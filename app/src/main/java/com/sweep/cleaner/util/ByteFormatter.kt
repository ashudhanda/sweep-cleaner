package com.sweep.cleaner.util

import java.util.Locale

object ByteFormatter {
    private const val ONE_KB = 1_000L
    private const val ONE_MB = 1_000_000L
    private const val ONE_GB = 1_000_000_000L
    private const val ONE_TB = 1_000_000_000_000L

    /**
     * Formats bytes to standard decimal units (1 GB = 1,000,000,000 bytes)
     * as required by Android Storage specifications.
     */
    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "0 B"
        return when {
            bytes >= ONE_TB -> String.format(Locale.US, "%.1f TB", bytes.toDouble() / ONE_TB)
            bytes >= ONE_GB -> String.format(Locale.US, "%.1f GB", bytes.toDouble() / ONE_GB)
            bytes >= ONE_MB -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / ONE_MB)
            bytes >= ONE_KB -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / ONE_KB)
            else -> "$bytes B"
        }
    }

    /**
     * Returns split value and unit for large dashboard displays, e.g. "98.6" and "GB used"
     */
    fun formatBytesParts(bytes: Long): Pair<String, String> {
        if (bytes < 0) return Pair("0", "B")
        return when {
            bytes >= ONE_TB -> Pair(String.format(Locale.US, "%.1f", bytes.toDouble() / ONE_TB), "TB")
            bytes >= ONE_GB -> Pair(String.format(Locale.US, "%.1f", bytes.toDouble() / ONE_GB), "GB")
            bytes >= ONE_MB -> Pair(String.format(Locale.US, "%.1f", bytes.toDouble() / ONE_MB), "MB")
            bytes >= ONE_KB -> Pair(String.format(Locale.US, "%.1f", bytes.toDouble() / ONE_KB), "KB")
            else -> Pair(bytes.toString(), "B")
        }
    }

    /**
     * Formats duration in milliseconds to "m:ss" or "h:mm:ss"
     */
    fun formatDuration(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats item counts, e.g. "1,240 items"
     */
    fun formatItemCount(count: Int, singular: String, plural: String): String {
        return if (count == 1) "1 $singular" else "$count $plural"
    }
}
