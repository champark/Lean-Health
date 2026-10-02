package com.cpkr.leanpedometer

import android.content.Context
import android.os.SystemClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StepStore(context: Context) {
    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun recordSensorValue(
        rawSensorValue: Long,
        nowMillis: Long = System.currentTimeMillis(),
        elapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ): Long {
        val safeRaw = rawSensorValue.coerceAtLeast(0L)
        val zone = ZoneId.systemDefault()
        val today = dateAt(nowMillis, zone)
        val bootDate =
            dateAt(
                (nowMillis - elapsedRealtimeMillis)
                    .coerceAtLeast(0L),
                zone,
            )

        val previousRaw =
            if (prefs.contains(KEY_LAST_RAW)) {
                prefs.getLong(KEY_LAST_RAW, 0L)
            } else {
                null
            }
        val skipDelta =
            prefs.getBoolean(
                KEY_SKIP_NEXT_DELTA,
                false,
            )

        val delta = when {
            skipDelta -> 0L

            previousRaw == null -> {
                if (bootDate == today) safeRaw else 0L
            }

            safeRaw >= previousRaw -> {
                safeRaw - previousRaw
            }

            bootDate == today -> {
                // TYPE_STEP_COUNTER resets after a reboot.
                safeRaw
            }

            else -> {
                // A reset happened before today, so the portion belonging
                // to the current day cannot be reconstructed safely.
                0L
            }
        }

        val next =
            (getSteps(today) + delta)
                .coerceAtLeast(0L)

        prefs.edit()
            .putLong(dayKey(today), next)
            .putLong(updatedKey(today), nowMillis)
            .putLong(KEY_LAST_RAW, safeRaw)
            .putString(KEY_LAST_DATE, today.toString())
            .putLong(KEY_LAST_UPDATED_AT, nowMillis)
            .putBoolean(KEY_SKIP_NEXT_DELTA, false)
            .apply()

        pruneOldDays(today)
        return next
    }

    fun getTodaySteps(): Long =
        getSteps(LocalDate.now())

    fun getSteps(date: LocalDate): Long =
        prefs.getLong(dayKey(date), 0L)

    fun getUpdatedAt(date: LocalDate): Long =
        prefs.getLong(updatedKey(date), 0L)

    fun getRecentDays(days: Int): List<DailySteps> {
        val safeDays = days.coerceAtLeast(0)
        val today = LocalDate.now()
        return (0 until safeDays).map { offset ->
            val date = today.minusDays(offset.toLong())
            DailySteps(
                date = date,
                steps = getSteps(date),
                updatedAtEpochMillis = getUpdatedAt(date),
            )
        }
    }

    fun setTrackingEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_TRACKING_ENABLED, enabled)
            .apply()
    }

    fun markManualStop() {
        prefs.edit()
            .putBoolean(KEY_TRACKING_ENABLED, false)
            .putBoolean(KEY_SKIP_NEXT_DELTA, true)
            .apply()
    }

    fun isTrackingEnabled(): Boolean =
        prefs.getBoolean(KEY_TRACKING_ENABLED, false)

    fun lastUpdatedAt(): Long =
        prefs.getLong(KEY_LAST_UPDATED_AT, 0L)

    @Synchronized
    private fun pruneOldDays(today: LocalDate) {
        if (
            prefs.getString(KEY_LAST_PRUNE_DATE, null) ==
            today.toString()
        ) {
            return
        }

        val cutoff = today.minusDays(RETENTION_DAYS)
        val editor = prefs.edit()

        prefs.all.keys.forEach { key ->
            val prefix = when {
                key.startsWith(DAY_PREFIX) -> DAY_PREFIX
                key.startsWith(UPDATED_PREFIX) -> UPDATED_PREFIX
                else -> null
            } ?: return@forEach

            val date =
                runCatching {
                    LocalDate.parse(key.removePrefix(prefix))
                }.getOrNull() ?: return@forEach

            if (date.isBefore(cutoff)) {
                editor.remove(key)
            }
        }

        editor
            .putString(KEY_LAST_PRUNE_DATE, today.toString())
            .apply()
    }

    private fun dateAt(
        epochMillis: Long,
        zone: ZoneId,
    ): LocalDate =
        Instant.ofEpochMilli(epochMillis)
            .atZone(zone)
            .toLocalDate()

    private fun dayKey(date: LocalDate): String =
        DAY_PREFIX + date

    private fun updatedKey(date: LocalDate): String =
        UPDATED_PREFIX + date

    data class DailySteps(
        val date: LocalDate,
        val steps: Long,
        val updatedAtEpochMillis: Long,
    )

    companion object {
        private const val PREFS_NAME = "lean_pedometer_steps"
        private const val KEY_LAST_RAW = "last_raw"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_LAST_UPDATED_AT = "last_updated_at"
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
        private const val KEY_SKIP_NEXT_DELTA = "skip_next_delta"
        private const val KEY_LAST_PRUNE_DATE = "last_prune_date"
        private const val DAY_PREFIX = "day:"
        private const val UPDATED_PREFIX = "updated:"
        private const val RETENTION_DAYS = 400L
    }
}
