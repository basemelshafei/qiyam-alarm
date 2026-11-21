package com.basem.qiyam

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/**
 * Reads your Fitbit Air data (synced by the Google Health app into Health Connect) and any
 * workouts other apps post there, such as Lyfta. Read only: nothing is ever written back.
 *
 * Each sync stores a small daily summary in [DayLog] ("hc_*" items), so screens and the daily
 * score can use it instantly without waiting on Health Connect.
 */
object Health {

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    enum class Status { NOT_INSTALLED, NEEDS_PERMISSION, CONNECTED }

    fun installed(ctx: Context): Boolean = runCatching {
        HealthConnectClient.getSdkStatus(ctx) == HealthConnectClient.SDK_AVAILABLE
    }.getOrDefault(false)

    suspend fun status(ctx: Context): Status {
        if (!installed(ctx)) return Status.NOT_INSTALLED
        val granted = runCatching { HealthConnectClient.getOrCreate(ctx).permissionController.getGrantedPermissions() }
            .getOrDefault(emptySet())
        // Even partly granted is useful: we read whatever you allowed.
        return if (granted.intersect(permissions).isNotEmpty()) Status.CONNECTED else Status.NEEDS_PERMISSION
    }

    /** Pulls the last [days] days into the day log. Safe to call often; returns false if unavailable. */
    suspend fun sync(ctx: Context, days: Int = 7): Boolean {
        if (status(ctx) != Status.CONNECTED) return false
        val hc = HealthConnectClient.getOrCreate(ctx)
        val log = DayLog(ctx)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        for (i in 0 until days) {
            val day = today.minusDays(i.toLong())
            val start = day.atStartOfDay(zone).toInstant()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant()
            val range = TimeRangeFilter.between(start, end)

            runCatching {
                val agg = hc.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), range))
                agg[StepsRecord.COUNT_TOTAL]?.let { log.setText(day, "hc_steps", it.toString()) }
            }

            // Sleep that ended this morning: from 18:00 the evening before to 14:00 today.
            runCatching {
                val night = TimeRangeFilter.between(day.minusDays(1).atTime(18, 0).atZone(zone).toInstant(), day.atTime(14, 0).atZone(zone).toInstant())
                val sessions = hc.readRecords(ReadRecordsRequest(SleepSessionRecord::class, night)).records
                if (sessions.isNotEmpty()) {
                    val minutes = sessions.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                    log.setText(day, "hc_sleep_min", minutes.toString())
                    log.setText(day, "hc_sleep_span", "${sessions.minOf { it.startTime }.toEpochMilli()}-${sessions.maxOf { it.endTime }.toEpochMilli()}")
                }
            }

            runCatching {
                val rhr = hc.readRecords(ReadRecordsRequest(RestingHeartRateRecord::class, range)).records
                rhr.lastOrNull()?.let { log.setText(day, "hc_rhr", it.beatsPerMinute.toString()) }
            }

            runCatching {
                val sessions = hc.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range)).records
                if (sessions.isNotEmpty()) {
                    val names = sessions.joinToString(", ") { s ->
                        s.title?.takeIf { it.isNotBlank() } ?: exerciseName(s.exerciseType)
                    }
                    val minutes = sessions.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() }
                    log.setText(day, "hc_workout", names)
                    log.setText(day, "hc_workout_min", minutes.toString())
                }
            }

            runCatching {
                val w = hc.readRecords(ReadRecordsRequest(WeightRecord::class, range)).records.lastOrNull()
                if (w != null && log.weight(day) == null) log.setWeight(day, w.weight.inKilograms.toFloat())
            }
        }
        Prefs(ctx).healthSyncedAt = System.currentTimeMillis()
        return true
    }

    private fun exerciseName(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Strength"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Run"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walk"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING, ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Cycle"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swim"
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        else -> "Workout"
    }
}

/** What Health Connect told us about one day, read back from the day log. */
data class HealthDay(val steps: Long?, val sleepMinutes: Long?, val restingHr: Long?, val workout: String?, val workoutMinutes: Long?)

fun DayLog.health(day: LocalDate) = HealthDay(
    steps = text(day, "hc_steps").toLongOrNull(),
    sleepMinutes = text(day, "hc_sleep_min").toLongOrNull(),
    restingHr = text(day, "hc_rhr").toLongOrNull(),
    workout = text(day, "hc_workout").ifBlank { null },
    workoutMinutes = text(day, "hc_workout_min").toLongOrNull(),
)
