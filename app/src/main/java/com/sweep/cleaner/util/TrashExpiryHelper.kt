package com.sweep.cleaner.util

object TrashExpiryHelper {

    const val DEFAULT_TRASH_RETENTION_DAYS = 30
    private const val MS_IN_A_DAY = 24L * 60L * 60L * 1000L

    /**
     * Calculates the exact 30-day expiry timestamp from the deletion time.
     */
    fun calculateExpiryTimestamp(
        deletedTimestampMs: Long,
        retentionDays: Int = DEFAULT_TRASH_RETENTION_DAYS
    ): Long {
        return deletedTimestampMs + (retentionDays.toLong() * MS_IN_A_DAY)
    }

    /**
     * Computes the whole days remaining before an item is permanently purged.
     * Minimum 0 days.
     */
    fun getDaysRemaining(
        expiryTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Int {
        val diffMs = expiryTimestampMs - currentTimeMs
        return if (diffMs <= 0) 0 else ((diffMs + MS_IN_A_DAY - 1) / MS_IN_A_DAY).toInt()
    }

    fun calculateDaysRemaining(
        deletedTimestampMs: Long,
        retentionDays: Int = DEFAULT_TRASH_RETENTION_DAYS,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Int {
        val expiry = calculateExpiryTimestamp(deletedTimestampMs, retentionDays)
        return getDaysRemaining(expiry, currentTimeMs)
    }

    /**
     * Checks if the item is expired and due for automatic purge.
     */
    fun isExpired(
        expiryTimestampMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        return currentTimeMs >= expiryTimestampMs
    }

    /**
     * Formats days remaining into friendly display text.
     */
    fun formatExpiryLabel(daysRemaining: Int): String {
        return when {
            daysRemaining <= 0 -> "Purging soon"
            daysRemaining == 1 -> "1 day left"
            else -> "$daysRemaining days left"
        }
    }
}
