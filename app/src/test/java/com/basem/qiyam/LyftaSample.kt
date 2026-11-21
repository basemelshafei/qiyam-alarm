package com.basem.qiyam

import java.time.LocalDate

/** Workouts shaped exactly like Lyfta's API response (including its "excercise_name" spelling). */
object LyftaSample {
    private fun set(w: String, r: String, pr: Boolean = false) =
        """{"id":"s","weight":"$w","reps":"$r","rir":null,"duration":null,"distance":null,"set_type_id":"1","is_completed":true,
           "record_type":${if (pr) "\"weight\"" else "null"},"record_level":null,"record_value":null}"""

    private fun ex(name: String, vararg sets: String) =
        """{"exercise_id":1,"excercise_name":"$name","exercise_type":"weight_reps","exercise_image":"","exercise_rest_time":90,
           "sets":[${sets.joinToString(",")}]}"""

    private fun workout(id: Int, title: String, date: LocalDate, volume: Int, vararg exercises: String) =
        """{"id":$id,"user_id":7,"title":"$title","body_weight":80,"weight_unit":"kg","workout_duration":"01:05:00",
           "workout_perform_date":"${date}T18:10:00Z","total_volume":"$volume","totalLiftedWeight":$volume,
           "user":{"username":"basem"},"exercises":[${exercises.joinToString(",")}]}"""

    fun json(today: LocalDate): String {
        val list = mutableListOf<String>()
        var id = 100
        for (i in 1L..50L) {
            val d = today.minusDays(i)
            if (d.dayOfWeek.value !in setOf(1, 2, 4, 5) || i % 9 == 4L) continue
            val kind = (i % 3).toInt()
            val base = 60 + (50 - i).toInt() / 4
            list += when (kind) {
                0 -> workout(id++, "Push Day", d, 5200 + base * 10,
                    ex("Bench Press (Barbell)", set("$base", "8"), set("$base", "8"), set("${base + 2.5}", "6", pr = i == 1L)),
                    ex("Overhead Press", set("40", "10"), set("40", "9"), set("40", "8")),
                    ex("Tricep Pushdown", set("25", "12"), set("25", "12")))
                1 -> workout(id++, "Pull Day", d, 6100 + base * 10,
                    ex("Deadlift", set("${base + 60}", "5"), set("${base + 60}", "5"), set("${base + 60}", "5")),
                    ex("Lat Pulldown", set("55", "10"), set("55", "10"), set("60", "8")),
                    ex("Barbell Row", set("60", "8"), set("60", "8")))
                else -> workout(id++, "Leg Day", d, 7400 + base * 10,
                    ex("Back Squat", set("${base + 30}", "6"), set("${base + 30}", "6"), set("${base + 30}", "6")),
                    ex("Romanian Deadlift", set("80", "8"), set("80", "8")),
                    ex("Leg Press", set("140", "12"), set("140", "12")))
            }
        }
        return "[" + list.joinToString(",") + "]"
    }
}
