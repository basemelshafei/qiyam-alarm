package com.basem.qiyam

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API keys and tokens, encrypted with a key that lives in the phone's secure hardware
 * (Android Keystore) and can never be exported. Stored in their own file, which is left out of
 * Google backups (see res/xml/backup_rules.xml), so keys never leave the phone.
 */
object Secrets {
    /** Settings names that are secrets. Everything else stays in ordinary settings. */
    val NAMES = setOf("bot")

    private const val ALIAS = "qiyam-secrets"
    private const val FILE = "secrets"

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    @Volatile private var cachedKey: SecretKey? = null

    private fun key(): SecretKey? = cachedKey ?: runCatching {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }.getOrNull()?.also { cachedKey = it }

    fun get(ctx: Context, name: String): String {
        Changes.watch()
        val stored = sp(ctx).getString(name, null) ?: return ""
        if (!stored.startsWith("v1:")) return stored // no secure hardware available (test phone)
        return runCatching {
            val bytes = Base64.decode(stored.removePrefix("v1:"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, 12))
            String(cipher.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun put(ctx: Context, name: String, value: String) {
        val v = value.trim()
        val e = sp(ctx).edit()
        if (v.isEmpty()) e.remove(name)
        else {
            val k = key()
            if (k == null) e.putString(name, v)
            else {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, k)
                val out = cipher.iv + cipher.doFinal(v.toByteArray(Charsets.UTF_8))
                e.putString(name, "v1:" + Base64.encodeToString(out, Base64.NO_WRAP))
            }
        }
        e.apply()
        Changes.bump()
    }

    /** One-time move of keys saved by older versions out of ordinary settings. */
    fun migrate(ctx: Context) {
        val old = ctx.applicationContext.getSharedPreferences("qiyam", Context.MODE_PRIVATE)
        val found = NAMES.filter { !old.getString(it, null).isNullOrBlank() }
        if (found.isEmpty()) return
        found.forEach { put(ctx, it, old.getString(it, "")!!) }
        old.edit().apply { found.forEach { remove(it) } }.apply()
    }
}
