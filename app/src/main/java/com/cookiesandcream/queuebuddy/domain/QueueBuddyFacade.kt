package com.cookiesandcream.queuebuddy.domain

import com.cookiesandcream.queuebuddy.data.LocationRepository
import com.cookiesandcream.queuebuddy.data.ReportRepository
import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory
import com.cookiesandcream.queuebuddy.domain.model.LocationStatus
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import com.cookiesandcream.queuebuddy.domain.validation.ReportValidationChain
import com.cookiesandcream.queuebuddy.domain.validation.ValidationContext
import com.cookiesandcream.queuebuddy.domain.validation.ValidationResult

// How the home list can be ordered.
enum class SortOption(val displayName: String) {
    RECENTLY_UPDATED("Recently updated"),
    SHORTEST_WAIT("Shortest wait"),
    LOWEST_CROWD("Lowest crowd"),
    NAME("Name (A-Z)")
}

data class LocationSummary(val location: CampusLocation, val status: LocationStatus)

data class LocationDetail(
    val location: CampusLocation,
    val status: LocationStatus,
    val reports: List<StatusReport>
)

// The outcome of a submit attempt, so the UI can show a thank-you or the reason it was rejected.
sealed interface SubmitResult {
    data class Success(val report: StatusReport) : SubmitResult
    data class Rejected(val reason: String) : SubmitResult
}

// Facade: the single entry point the UI uses, hiding the repositories, aggregator,
// and the validation chain.
class QueueBuddyFacade(
    private val locationRepository: LocationRepository,
    private val reportRepository: ReportRepository,
    private val aggregator: StatusAggregator = StatusAggregator()
) {
    private val validationChain = ReportValidationChain.default()

    fun locationSummaries(
        query: String = "",
        category: LocationCategory? = null,
        sort: SortOption = SortOption.RECENTLY_UPDATED
    ): List<LocationSummary> {
        val reports = reportRepository.allReports()
        var summaries = locationRepository.allLocations().map { location ->
            LocationSummary(location, aggregator.aggregate(location, reports))
        }
        if (query.isNotBlank()) {
            summaries = summaries.filter {
                it.location.name.contains(query, ignoreCase = true) ||
                    it.location.building.contains(query, ignoreCase = true)
            }
        }
        if (category != null) {
            summaries = summaries.filter { it.location.category == category }
        }
        return sortSummaries(summaries, sort)
    }

    fun locationDetail(locationId: String): LocationDetail? {
        val location = locationRepository.locationById(locationId) ?: return null
        val reports = reportRepository.reportsForLocation(locationId)
        return LocationDetail(location, aggregator.aggregate(location, reports), reports)
    }

    // Runs the report through the validation chain; only valid reports are stored.
    fun submitReport(report: StatusReport): SubmitResult {
        val context = ValidationContext(
            nowMillis = report.timestampMillis,
            lastReportFromReporterMillis =
                reportRepository.lastReportFrom(report.reporterId, report.locationId)?.timestampMillis
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

    private fun sortSummaries(summaries: List<LocationSummary>, sort: SortOption): List<LocationSummary> =
        when (sort) {
            SortOption.RECENTLY_UPDATED ->
                summaries.sortedByDescending { it.status.lastUpdatedMillis ?: 0L }
            SortOption.SHORTEST_WAIT ->
                summaries.sortedWith(compareBy(nullsLast()) { it.status.estimatedWaitMinutes })
            SortOption.LOWEST_CROWD ->
                summaries.sortedWith(compareBy(nullsLast()) { it.status.crowdLevel?.rank })
            SortOption.NAME ->
                summaries.sortedBy { it.location.name }
        }
}
