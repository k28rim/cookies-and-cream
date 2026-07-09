package com.cookiesandcream.queuebuddy.domain

import com.cookiesandcream.queuebuddy.data.LocationRepository
import com.cookiesandcream.queuebuddy.data.ReportRepository
import com.cookiesandcream.queuebuddy.domain.model.CampusLocation
import com.cookiesandcream.queuebuddy.domain.model.LocationStatus
import com.cookiesandcream.queuebuddy.domain.model.StatusReport

data class LocationSummary(val location: CampusLocation, val status: LocationStatus)

data class LocationDetail(
    val location: CampusLocation,
    val status: LocationStatus,
    val reports: List<StatusReport>
)

// Facade: the single entry point the UI uses, hiding the repositories and aggregator.
// This week it also applies the home screen's search.
class QueueBuddyFacade(
    private val locationRepository: LocationRepository,
    private val reportRepository: ReportRepository,
    private val aggregator: StatusAggregator = StatusAggregator()
) {

    fun locationSummaries(query: String = ""): List<LocationSummary> {
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
        return summaries
    }

    fun locationDetail(locationId: String): LocationDetail? {
        val location = locationRepository.locationById(locationId) ?: return null
        val reports = reportRepository.reportsForLocation(locationId)
        return LocationDetail(location, aggregator.aggregate(location, reports), reports)
    }

    // Stores a report if it has at least one field set. Returns true on success.
    fun submitReport(report: StatusReport): Boolean {
        if (!report.hasAnyField()) return false
        reportRepository.add(report)
        return true
    }
}
