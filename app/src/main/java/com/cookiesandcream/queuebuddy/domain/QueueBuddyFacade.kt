package com.cookiesandcream.queuebuddy.domain

import com.cookiesandcream.queuebuddy.data.LocationRepository
import com.cookiesandcream.queuebuddy.data.ReportRepository
import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory
import com.cookiesandcream.queuebuddy.domain.model.LocationStatus
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import com.cookiesandcream.queuebuddy.domain.model.University
import com.cookiesandcream.queuebuddy.domain.suggestion.LocationSuggester
import com.cookiesandcream.queuebuddy.domain.suggestion.LocationSuggestion
import com.cookiesandcream.queuebuddy.domain.validation.ReportValidationChain
import com.cookiesandcream.queuebuddy.domain.validation.ValidationContext
import com.cookiesandcream.queuebuddy.domain.validation.ValidationResult
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// How the home list can be ordered.
enum class SortOption(val displayName: String) {
    WAIT_TIME("Shortest wait"),
    CROWD_LEVEL("Lowest crowd"),
    RECENTLY_UPDATED("Recently updated"),
    NEAREST("Nearest first")
}

enum class ConditionFilter(val displayName: String) {
    LOW_CROWD("Low crowd"),
    SEATS_AVAILABLE("Seats available"),
    QUIET("Quiet"),
    WORKING_PRINTER("Working printer")
}

data class LocationSummary(
    val location: CampusLocation,
    val status: LocationStatus,
    val tags: List<String>,
    val distanceMeters: Double?
)

data class LocationDetail(
    val location: CampusLocation,
    val status: LocationStatus,
    val tags: List<String>,
    val reports: List<StatusReport>
)

// The outcome of a submit attempt, so the UI can show a thank-you or the reason it was rejected.
sealed interface SubmitResult {
    data class Success(val report: StatusReport) : SubmitResult
    data class Rejected(val reason: String) : SubmitResult
}

// Facade: the single entry point the UI uses, hiding the repositories, the
// aggregator, and the validation chain.
class QueueBuddyFacade(
    private val locationRepository: LocationRepository,
    private val reportRepository: ReportRepository,
    private val aggregator: StatusAggregator = StatusAggregator()
) {
    private val validationChain = ReportValidationChain.default()

    fun locationSummaries(
        query: String = "",
        category: LocationCategory? = null,
        university: University = University.WATERLOO,
        conditions: Set<ConditionFilter> = emptySet(),
        sort: SortOption = SortOption.RECENTLY_UPDATED,
        userLatitude: Double? = null,
        userLongitude: Double? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): List<LocationSummary> {
        val allReports = reportRepository.allReports()
        var summaries = locationRepository.locationsForUniversity(university).map { location ->
            val status = aggregator.aggregate(location, allReports, nowMillis)
            LocationSummary(
                location = location,
                status = status,
                tags = StatusTags.tagsFor(status),
                distanceMeters = if (userLatitude != null && userLongitude != null) {
                    haversineMeters(userLatitude, userLongitude, location.latitude, location.longitude)
                } else null
            )
        }

        if (query.isNotBlank()) {
            summaries = summaries.filter {
                it.location.name.contains(query, ignoreCase = true) ||
                    it.location.building.contains(query, ignoreCase = true)
            }
        }
        if (category != null) {
            val categories = categoriesFor(category)
            summaries = summaries.filter { it.location.category in categories }
        }
        for (condition in conditions) {
            summaries = summaries.filter { matchesCondition(it, condition) }
        }

        return when (sort) {
            SortOption.WAIT_TIME -> summaries.sortedWith(
                compareBy(nullsLast()) { it.status.estimatedWaitMinutes }
            )
            SortOption.CROWD_LEVEL -> summaries.sortedWith(
                compareBy(nullsLast()) { it.status.crowdLevel?.rank }
            )
            SortOption.RECENTLY_UPDATED -> summaries.sortedByDescending {
                it.status.lastUpdatedMillis ?: 0L
            }
            SortOption.NEAREST -> summaries.sortedWith(
                compareBy(nullsLast()) { it.distanceMeters }
            )
        }
    }

    // Best pick among the currently listed locations, shown highlighted on Home.
    fun suggestedLocation(summaries: List<LocationSummary>): LocationSuggestion? =
        LocationSuggester.suggest(summaries)

    fun locationDetail(locationId: String, nowMillis: Long = System.currentTimeMillis()): LocationDetail? {
        val location = locationRepository.locationById(locationId) ?: return null
        val reports = reportRepository.reportsForLocation(locationId)
        val status = aggregator.aggregate(location, reports, nowMillis)
        return LocationDetail(
            location = location,
            status = status,
            tags = StatusTags.tagsFor(status),
            reports = reports
        )
    }

    fun submitReport(report: StatusReport): SubmitResult {
        val context = ValidationContext(
            nowMillis = report.timestampMillis,
            lastReportFromReporterMillis = reportRepository
                .lastReportFrom(report.reporterId, report.locationId)?.timestampMillis
        )
        return when (val result = validationChain.validate(report, context)) {
            is ValidationResult.Valid -> {
                reportRepository.add(report)
                SubmitResult.Success(report)
            }
            is ValidationResult.Invalid -> SubmitResult.Rejected(result.reason)
        }
    }

    fun flagReport(reportId: String, flaggerId: String): StatusReport? =
        reportRepository.flag(reportId, flaggerId)

    fun unflagReport(reportId: String, flaggerId: String): StatusReport? =
        reportRepository.unflag(reportId, flaggerId)

    fun removeReport(reportId: String): StatusReport? = reportRepository.remove(reportId)

    fun clearFlags(reportId: String): StatusReport? = reportRepository.clearFlags(reportId)

    fun flaggedReports(): List<StatusReport> = reportRepository.flaggedReports()

    fun locationById(id: String): CampusLocation? = locationRepository.locationById(id)

    // Libraries are also study spaces (they contain study spots), so the Study Spaces
    // filter includes libraries. Every other category matches exactly.
    private fun categoriesFor(category: LocationCategory): Set<LocationCategory> =
        when (category) {
            LocationCategory.STUDY -> setOf(LocationCategory.STUDY, LocationCategory.LIBRARY)
            else -> setOf(category)
        }

    private fun matchesCondition(summary: LocationSummary, condition: ConditionFilter): Boolean =
        when (condition) {
            ConditionFilter.LOW_CROWD -> summary.status.crowdLevel == CrowdLevel.LOW
            ConditionFilter.SEATS_AVAILABLE -> StatusTags.SEATS_AVAILABLE in summary.tags
            ConditionFilter.QUIET -> StatusTags.QUIET in summary.tags
            ConditionFilter.WORKING_PRINTER -> StatusTags.WORKING_PRINTER in summary.tags
        }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
