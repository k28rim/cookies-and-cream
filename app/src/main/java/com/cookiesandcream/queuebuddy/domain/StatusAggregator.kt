package com.cookiesandcream.queuebuddy.domain

import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.LocationStatus
import com.cookiesandcream.queuebuddy.domain.model.StatusReport

// Combines a location's reports into one current status. For now the most recent
// value for each field wins; later weeks add time-decay weighting and staleness.
class StatusAggregator {

    fun aggregate(location: CampusLocation, reports: List<StatusReport>): LocationStatus {
        val forLocation = reports
            .filter { it.locationId == location.id }
            .sortedByDescending { it.timestampMillis }
        val wait = forLocation.firstNotNullOfOrNull { it.waitEstimate }
        return LocationStatus(
            locationId = location.id,
            crowdLevel = forLocation.firstNotNullOfOrNull { it.crowdLevel },
            estimatedWaitMinutes = wait?.representativeMinutes,
            waitLabel = wait?.displayName,
            seatAvailability = forLocation.firstNotNullOfOrNull { it.seatAvailability },
            noiseLevel = forLocation.firstNotNullOfOrNull { it.noiseLevel },
            resourceStatus = forLocation.firstNotNullOfOrNull { it.resourceStatus },
            lastUpdatedMillis = forLocation.firstOrNull()?.timestampMillis,
            reportCount = forLocation.size
        )
    }
}
