package com.sweep.cleaner

import com.sweep.cleaner.util.ByteFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ByteFormatterTest {

    @Test
    fun testByteFormattingZero() {
        assertEquals("0 B", ByteFormatter.formatBytes(0L))
    }

    @Test
    fun testByteFormattingKilobytes() {
        assertEquals("500 B", ByteFormatter.formatBytes(500L))
        assertEquals("1.5 KB", ByteFormatter.formatBytes(1500L))
    }

    @Test
    fun testByteFormattingMegabytes() {
        assertEquals("20.0 MB", ByteFormatter.formatBytes(20_000_000L))
    }

    @Test
    fun testByteFormattingGigabytes() {
        val (value, unit) = ByteFormatter.formatBytesParts(98_600_000_000L)
        assertEquals("98.6", value)
        assertEquals("GB", unit)
    }

    @Test
    fun testDurationFormatting() {
        assertEquals("0:45", ByteFormatter.formatDuration(45_000L))
        assertEquals("2:15", ByteFormatter.formatDuration(135_000L))
        assertEquals("1:05:00", ByteFormatter.formatDuration(3_900_000L))
    }
}
