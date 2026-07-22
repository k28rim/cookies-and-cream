package com.cookiesandcream.queuebuddy

import com.cookiesandcream.queuebuddy.domain.model.Freshness
import com.cookiesandcream.queuebuddy.domain.staleness.QueueStalenessPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StalenessPolicyTest {

    private val policy = QueueStalenessPolicy()

    @Test
    fun freshnessFollowsTheWindows() {
        assertEquals(Freshness.LIVE, policy.freshnessOf(5))
        assertEquals(Freshness.RECENT, policy.freshnessOf(20))
        assertEquals(Freshness.STALE, policy.freshnessOf(45))
    }

    @Test
    fun reportsAreUsableUntilTheHardCap() {
        assertTrue(policy.isUsable(40))
        assertFalse(policy.isUsable(70))
    }
}
