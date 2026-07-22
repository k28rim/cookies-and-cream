package com.cookiesandcream.queuebuddy

import com.cookiesandcream.queuebuddy.domain.StatusAggregator
import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.Freshness
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import org.junit.Assert.assertEquals
import org.junit.Test

class StatusAggregatorTest {

    private val aggregator = StatusAggregator()
    private val now = 10_000_000L
    private val location = CampusLocation(
        id = "loc",
        name = "Test Spot",
        building = "Test Building",
        category = LocationCategory.FOOD,
        description = "",
        latitude = 0.0,
        longitude = 0.0
    )

    private fun reportAgo(minutes: Long, crowd: CrowdLevel) =
        StatusReport.Builder(location.id, "rep").crowdLevel(crowd).build(now - minutes * 60_000)

    @Test
    fun recentReportDrivesALiveStatus() {
        val status = aggregator.aggregate(location, listOf(reportAgo(5, CrowdLevel.LOW)), now)
        assertEquals(Freshness.LIVE, status.freshness)
        assertEquals(CrowdLevel.LOW, status.crowdLevel)
        assertEquals(1, status.reportCount)
    }

    @Test
    fun reportPastHardCapCountsAsNoData() {
        val status = aggregator.aggregate(location, listOf(reportAgo(90, CrowdLevel.HIGH)), now)
        assertEquals(Freshness.NO_DATA, status.freshness)
        assertEquals(0, status.reportCount)
    }

    @Test
    fun mostRecentReportWinsForEachField() {
        val reports = listOf(reportAgo(2, CrowdLevel.HIGH), reportAgo(8, CrowdLevel.LOW))
        val status = aggregator.aggregate(location, reports, now)
        assertEquals(CrowdLevel.HIGH, status.crowdLevel)
    }
}
