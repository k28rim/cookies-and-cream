package com.cookiesandcream.queuebuddy.domain

import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.Freshness
import com.cookiesandcream.queuebuddy.domain.model.LocationStatus
import com.cookiesandcream.queuebuddy.domain.model.StatusReport

// Combines a location's reports into one current status. The most recent value for
// each field wins; reports past the category's hard age cap are dropped, and the
// newest usable report decides how fresh (Live / Recent / Stale) the status is.
class StatusAggregator {

    fun aggregate(
        location: CampusLocation,
        reports: List<StatusReport>,
        nowMillis: Long = System.currentTimeMillis()
    ): LocationStatus {
        val policy = location.category.createStalenessPolicy()
        val forLocation = reports.filter { it.locationId == location.id }
        val usable = forLocation
            .filter { policy.isUsable(ageMinutes(it.timestampMillis, nowMillis)) }
            .sortedByDescending { it.timestampMillis }

        if (usable.isEmpty()) {
            return LocationStatus(
                locationId = location.id,
                crowdLevel = null,
                estimatedWaitMinutes = null,
                waitLabel = null,
                seatAvailability = null,
                noiseLevel = null,
                resourceStatus = null,
                lastUpdatedMillis = forLocation.maxOfOrNull { it.timestampMillis },
                freshness = Freshness.NO_DATA,
                reportCount = 0
            )
        }

        val newest = usable.first()
        val wait = usable.firstNotNullOfOrNull { it.waitEstimate }
        return LocationStatus(
            locationId = location.id,
            crowdLevel = usable.firstNotNullOfOrNull { it.crowdLevel },
            estimatedWaitMinutes = wait?.representativeMinutes,
            waitLabel = wait?.displayName,
            seatAvailability = usable.firstNotNullOfOrNull { it.seatAvailability },
            noiseLevel = usable.firstNotNullOfOrNull { it.noiseLevel },
            resourceStatus = usable.firstNotNullOfOrNull { it.resourceStatus },
            lastUpdatedMillis = newest.timestampMillis,
            freshness = policy.freshnessOf(ageMinutes(newest.timestampMillis, nowMillis)),
            reportCount = usable.size
        )
    }

    private fun ageMinutes(timestampMillis: Long, nowMillis: Long): Long =
        (nowMillis - timestampMillis) / 60_000
}
