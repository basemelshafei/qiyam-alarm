package com.basem.qiyam

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset

data class LyftaSet(val weight: Double?, val reps: Int?, val completed: Boolean, val record: Boolean)

data class LyftaExercise(val name: String, val sets: List<LyftaSet>) {
    /** Heaviest completed set, e.g. "80 kg × 8". */
    val topSet: LyftaSet? get() = sets.filter { it.completed && it.weight != null }.maxByOrNull { it.weight!! }
    val hasRecord get() = sets.any { it.record }
}

data class LyftaWorkout(
    val id: String,
    val title: String,
    val date: LocalDate,
    val duration: String,
    val volume: Double,
    val unit: String,
    val exercises: List<LyftaExercise>,
) {
    val setCount get() = exercises.sumOf { e -> e.sets.count { it.completed } }
    val records get() = exercises.count { it.hasRecord }
}

/**
 * Reads your workouts from Lyfta's API (https://my.lyfta.app/api/v1/workouts) with a personal
 * key you generate on Lyfta's website. Read only. Workouts are cached on the phone, and each
 * workout day ticks off the gym on that date.
 */
object Lyfta {
    private const val BASE = "https://my.lyfta.app"

    private fun cache(ctx: Context) = File(ctx.filesDir, "lyfta_workouts.json")

    /** Fetches recent workouts (blocking: call off the main thread). Returns a short status message. */
    fun sync(ctx: Context, days: Int = 120): String {
        val prefs = Prefs(ctx)
        val key = prefs.lyftaKey
        if (key.isBlank()) return "Add your Lyfta API key first"
        val to = LocalDate.now(ZoneOffset.UTC)
        val from = to.minusDays(days.toLong())
        val all = JSONArray()
        var page = 1
        while (page <= 5) {
            val (code, body) = get("$BASE/api/v1/workouts?limit=100&page=$page&from=$from&to=$to", key)
            when (code) {
                401, 403 -> return "Lyfta didn't accept that key. Generate a new one and paste it again."
                429 -> return "Lyfta says too many requests. Try again in a minute."
                !in 200..299 -> return "Lyfta returned an error ($code)"
            }
            val json = JSONObject(body)
            val arr = json.optJSONArray("workouts") ?: JSONArray()
            for (i in 0 until arr.length()) all.put(arr.getJSONObject(i))
            val pages = json.optInt("total_pages", 1)
            if (page >= pages || arr.length() == 0) break
            page++
        }
        save(ctx, all.toString())
        prefs.lyftaSyncedAt = System.currentTimeMillis()
        return "Synced ${all.length()} workout${if (all.length() == 1) "" else "s"} from Lyfta"
    }

    /** Stores raw workouts and marks each workout day in the log. */
    fun save(ctx: Context, workoutsJson: String) {
        cache(ctx).writeText(workoutsJson)
        val log = DayLog(ctx)
        parse(workoutsJson).groupBy { it.date }.forEach { (day, ws) ->
            log.setText(day, "lyfta", ws.joinToString(" + ") { it.title })
        }
    }

    fun workouts(ctx: Context): List<LyftaWorkout> {
        Changes.watch()
        val f = cache(ctx)
        return if (f.exists()) runCatching { parse(f.readText()) }.getOrDefault(emptyList()) else emptyList()
    }

    fun parse(workoutsJson: String): List<LyftaWorkout> {
        val arr = JSONArray(workoutsJson)
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val w = arr.getJSONObject(i)
                val ex = w.optJSONArray("exercises") ?: JSONArray()
                LyftaWorkout(
                    id = w.opt("id")?.toString() ?: "$i",
                    title = w.optString("title").ifBlank { "Workout" },
                    date = LocalDate.parse(w.optString("workout_perform_date").take(10)),
                    duration = w.optString("workout_duration"),
                    volume = w.optString("total_volume").toDoubleOrNull() ?: w.optDouble("total_volume", 0.0),
                    unit = w.optString("weight_unit").takeIf { it == "kg" || it == "lb" } ?: "kg",
                    exercises = (0 until ex.length()).map { j ->
                        val e = ex.getJSONObject(j)
                        val sets = e.optJSONArray("sets") ?: JSONArray()
                        LyftaExercise(
                            // Lyfta's field really is spelled "excercise_name".
                            name = e.optString("excercise_name").ifBlank { e.optString("exercise_name", "Exercise") },
                            sets = (0 until sets.length()).map { k ->
                                val st = sets.getJSONObject(k)
                                LyftaSet(
                                    weight = st.optString("weight").toDoubleOrNull(),
                                    reps = st.optString("reps").toDoubleOrNull()?.toInt(),
                                    completed = st.optBoolean("is_completed", true),
                                    record = st.optString("record_type").let { it.isNotBlank() && it != "null" && it != "0" },
                                )
                            },
                        )
                    },
                )
            }.getOrNull()
        }.sortedByDescending { it.date }
    }

    private fun get(url: String, key: String): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        return try {
            c.connectTimeout = 10_000; c.readTimeout = 20_000
            c.setRequestProperty("Authorization", "Bearer $key")
            c.setRequestProperty("Accept", "application/json")
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            code to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            c.disconnect()
        }
    }
}

fun fmtWeight(w: Double?): String = when {
    w == null -> "—"
    w % 1.0 == 0.0 -> w.toInt().toString()
    else -> String.format(java.util.Locale.US, "%.1f", w)
}
