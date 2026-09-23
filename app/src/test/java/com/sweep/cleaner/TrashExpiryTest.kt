package com.sweep.cleaner

import com.sweep.cleaner.util.TrashExpiryHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TrashExpiryTest {

    @Test
    fun testDaysRemainingNewlyDeleted() {
        val now = System.currentTimeMillis()
        val expiry = TrashExpiryHelper.calculateExpiryTimestamp(now)
        val days = TrashExpiryHelper.calculateDaysRemaining(now)
        assertEquals(30, days)
        assertFalse(TrashExpiryHelper.isExpired(expiry))
    }

    @Test
    fun testDaysRemainingAfter10Days() {
        val tenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)
        val expiry = TrashExpiryHelper.calculateExpiryTimestamp(tenDaysAgo)
        val days = TrashExpiryHelper.calculateDaysRemaining(tenDaysAgo)
        assertEquals(20, days)
        assertFalse(TrashExpiryHelper.isExpired(expiry))
    }

    @Test
    fun testExpiryAfter31Days() {
        val thirtyOneDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31)
        val expiry = TrashExpiryHelper.calculateExpiryTimestamp(thirtyOneDaysAgo)
        val days = TrashExpiryHelper.calculateDaysRemaining(thirtyOneDaysAgo)
        assertEquals(0, days)
        assertTrue(TrashExpiryHelper.isExpired(expiry))
    }
}
