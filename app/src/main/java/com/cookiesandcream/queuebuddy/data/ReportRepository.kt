package com.cookiesandcream.queuebuddy.data

import com.cookiesandcream.queuebuddy.domain.event.ReportEvent
import com.cookiesandcream.queuebuddy.domain.event.ReportEventBus
import com.cookiesandcream.queuebuddy.domain.model.StatusReport

// Stores reports in memory + on disk, and announces each change on the event bus.
class ReportRepository(
    private val eventBus: ReportEventBus,
    private val store: ReportStore? = null
) {

    private val reports = LinkedHashMap<String, StatusReport>()

    fun loadPersisted() {
        store?.load()?.forEach { reports[it.id] = it }
    }

    fun allReports(): List<StatusReport> = reports.values.toList()

    fun reportsForLocation(locationId: String): List<StatusReport> =
        reports.values
            .filter { it.locationId == locationId }
            .sortedByDescending { it.timestampMillis }

    // Most recent report this reporter filed here, used for rate limiting.
    fun lastReportFrom(reporterId: String, locationId: String): StatusReport? =
        reports.values
            .filter { it.reporterId == reporterId && it.locationId == locationId }
            .maxByOrNull { it.timestampMillis }

    fun add(report: StatusReport) {
        reports[report.id] = report
        persist()
        eventBus.publish(ReportEvent.ReportSubmitted(report))
    }

    fun flag(reportId: String, flaggerId: String): StatusReport? {
        val current = reports[reportId] ?: return null
        if (flaggerId in current.flaggedByReporterIds) return current
        val flagged = current.copy(
            flagCount = current.flagCount + 1,
            flaggedByReporterIds = current.flaggedByReporterIds + flaggerId
        )
        reports[reportId] = flagged
        persist()
        eventBus.publish(ReportEvent.ReportFlagged(flagged))
        return flagged
    }

    fun unflag(reportId: String, flaggerId: String): StatusReport? {
        val current = reports[reportId] ?: return null
        if (flaggerId !in current.flaggedByReporterIds) return current
        val cleared = current.copy(
            flagCount = (current.flagCount - 1).coerceAtLeast(0),
            flaggedByReporterIds = current.flaggedByReporterIds - flaggerId
        )
        reports[reportId] = cleared
        persist()
        eventBus.publish(ReportEvent.ReportFlagged(cleared))
        return cleared
    }

    private fun persist() {
        store?.save(reports.values.toList())
    }
}
