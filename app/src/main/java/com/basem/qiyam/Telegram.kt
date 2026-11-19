package com.basem.qiyam

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Telegram Bot API client. All calls block: call them off the main thread.
 * The bot token stays on the phone; nothing goes through any other server.
 */
class Telegram(private val token: String) {

    data class Update(val updateId: Long, val chatId: Long?, val callbackId: String?, val data: String?, val text: String?)

    private fun call(method: String, body: JSONObject, readTimeoutMs: Int = 15_000): JSONObject {
        val conn = URL("https://api.telegram.org/bot$token/$method").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = readTimeoutMs
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            val json = JSONObject(text)
            if (!json.optBoolean("ok")) {
                throw IllegalStateException(json.optString("description", "Telegram error ${conn.responseCode}"))
            }
            return json
        } finally {
            conn.disconnect()
        }
    }

    /** Returns the bot's @username; throws if the token is wrong. */
    fun botName(): String = call("getMe", JSONObject()).getJSONObject("result").optString("username")

    fun send(chatId: Long, html: String, buttons: List<Pair<String, String>> = emptyList()): Long {
        val body = JSONObject()
            .put("chat_id", chatId)
            .put("text", html)
            .put("parse_mode", "HTML")
            .put("disable_web_page_preview", true)
        if (buttons.isNotEmpty()) body.put("reply_markup", keyboard(buttons))
        return call("sendMessage", body).getJSONObject("result").getLong("message_id")
    }

    fun edit(chatId: Long, messageId: Long, html: String) {
        runCatching {
            call(
                "editMessageText",
                JSONObject().put("chat_id", chatId).put("message_id", messageId)
                    .put("text", html).put("parse_mode", "HTML")
                    .put("reply_markup", JSONObject().put("inline_keyboard", JSONArray())),
            )
        }
    }

    fun answerCallback(id: String, text: String) {
        runCatching { call("answerCallbackQuery", JSONObject().put("callback_query_id", id).put("text", text)) }
    }

    /** Long-polls for new updates (button taps, /start messages). */
    fun updates(offset: Long, timeoutSec: Int): List<Update> {
        val body = JSONObject().put("offset", offset).put("timeout", timeoutSec)
            .put("allowed_updates", JSONArray().put("message").put("callback_query"))
        val arr = call("getUpdates", body, readTimeoutMs = (timeoutSec + 10) * 1000).getJSONArray("result")
        return (0 until arr.length()).map { i ->
            val u = arr.getJSONObject(i)
            val cb = u.optJSONObject("callback_query")
            val msg = u.optJSONObject("message")
            when {
                cb != null -> Update(
                    u.getLong("update_id"),
                    cb.optJSONObject("message")?.optJSONObject("chat")?.optLong("id"),
                    cb.optString("id"), cb.optString("data"), null,
                )
                msg != null -> Update(
                    u.getLong("update_id"),
                    msg.optJSONObject("chat")?.optLong("id"),
                    null, null, msg.optString("text"),
                )
                else -> Update(u.getLong("update_id"), null, null, null, null)
            }
        }
    }

    private fun keyboard(buttons: List<Pair<String, String>>): JSONObject {
        val row = JSONArray()
        buttons.forEach { (label, data) -> row.put(JSONObject().put("text", label).put("callback_data", data)) }
        return JSONObject().put("inline_keyboard", JSONArray().put(row))
    }

    companion object {
        fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    }
}
