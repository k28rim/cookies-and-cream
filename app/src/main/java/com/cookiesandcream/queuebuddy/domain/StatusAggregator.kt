package com.cookiesandcream.queuebuddy.domain

import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.Freshness
import com.cookiesandcream.queuebuddy.domain.model.LocationStatus
import com.cookiesandcream.queuebuddy.domain.model.NoiseLevel
import com.cookiesandcream.queuebuddy.domain.model.ResourceStatus
import com.cookiesandcream.queuebuddy.domain.model.SeatAvailability
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import com.cookiesandcream.queuebuddy.domain.model.WaitEstimate
import kotlin.math.pow
import kotlin.math.roundToInt

// Combines a location's recent reports into one current status. Newer reports
// count for more via exponential time-decay weighting.
class StatusAggregator(private val halfLifeMinutes: Double = 15.0) {

    fun aggregate(
        location: CampusLocation,
        reports: List<StatusReport>,
        nowMillis: Long = System.currentTimeMillis()
    ): LocationStatus {
        val policy = location.category.createStalenessPolicy()
        val locationReports = reports.filter { it.locationId == location.id }
        val usable = locationReports
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
                lastUpdatedMillis = locationReports.maxOfOrNull { it.timestampMillis },
                freshness = Freshness.NO_DATA,
                reportCount = 0
            )
        }

        val newest = usable.first()
        val freshness = policy.freshnessOf(ageMinutes(newest.timestampMillis, nowMillis))

        val crowd = weightedMode(usable, nowMillis) { it.crowdLevel }
        val seats = weightedMode(usable, nowMillis) { it.seatAvailability }
        val noise = weightedMode(usable, nowMillis) { it.noiseLevel }
        val resource = weightedMode(usable, nowMillis) { it.resourceStatus }
        val waitMinutes = weightedAverageWait(usable, nowMillis)

        return LocationStatus(
            locationId = location.id,
            crowdLevel = crowd,
            estimatedWaitMinutes = waitMinutes,
            waitLabel = waitMinutes?.let { waitLabelFor(it) },
            seatAvailability = seats,
            noiseLevel = noise,
            resourceStatus = resource,
            lastUpdatedMillis = newest.timestampMillis,
            freshness = freshness,
            reportCount = usable.size
        )
    }

    private fun ageMinutes(timestampMillis: Long, nowMillis: Long): Long =
        (nowMillis - timestampMillis) / 60_000

    private fun decayWeight(report: StatusReport, nowMillis: Long): Double {
        val age = (nowMillis - report.timestampMillis) / 60_000.0
        return 0.5.pow(age / halfLifeMinutes)
    }

    private fun <T : Any> weightedMode(
        reports: List<StatusReport>,
        nowMillis: Long,
        selector: (StatusReport) -> T?
    ): T? {
        val weights = HashMap<T, Double>()
        for (report in reports) {
            val value = selector(report) ?: continue
            weights[value] = (weights[value] ?: 0.0) + decayWeight(report, nowMillis)
        }
        return weights.maxByOrNull { it.value }?.key
    }

    private fun weightedAverageWait(reports: List<StatusReport>, nowMillis: Long): Int? {
        var totalWeight = 0.0
        var weightedSum = 0.0
        for (report in reports) {
            val wait = report.waitEstimate ?: continue
            val weight = decayWeight(report, nowMillis)
            totalWeight += weight
            weightedSum += wait.representativeMinutes * weight
        }
        if (totalWeight == 0.0) return null
        return (weightedSum / totalWeight).roundToInt()
    }

    private fun waitLabelFor(minutes: Int): String = when {
        minutes <= 1 -> "No wait"
        minutes < 15 -> "~$minutes min"
        else -> "15+ min"
    }
}

object StatusTags {
    const val LOW_CROWD = "Low crowd"
    const val SEATS_AVAILABLE = "Seats available"
    const val QUIET = "Quiet"
    const val WORKING_PRINTER = "Working printer"

    fun tagsFor(status: LocationStatus): List<String> = buildList {
        if (status.crowdLevel == CrowdLevel.LOW) add(LOW_CROWD)
        if (status.seatAvailability == SeatAvailability.AVAILABLE) add(SEATS_AVAILABLE)
        if (status.noiseLevel == NoiseLevel.QUIET) add(QUIET)
        if (status.resourceStatus == ResourceStatus.WORKING) add(WORKING_PRINTER)
        if (status.estimatedWaitMinutes != null && status.estimatedWaitMinutes >= WaitEstimate.LONG.representativeMinutes) {
            add("Long line")
        }
    }
}
