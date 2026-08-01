package com.cookiesandcream.queuebuddy.data

import com.cookiesandcream.queuebuddy.domain.event.ReportEvent
import com.cookiesandcream.queuebuddy.domain.event.ReportEventBus
import com.cookiesandcream.queuebuddy.domain.model.StatusReport

// Stores all reports, persists them to disk, and announces every change on the event bus.
class ReportRepository(
    private val eventBus: ReportEventBus,
    private val store: ReportStore? = null
) {

    private val reports = LinkedHashMap<String, StatusReport>()
    private val lock = Any()

    fun loadPersisted() {
        val persisted = store?.load() ?: return
        synchronized(lock) {
            for (report in persisted) {
                reports[report.id] = report
            }
        }
    }

    fun allReports(): List<StatusReport> = synchronized(lock) { reports.values.toList() }

    fun reportsForLocation(locationId: String): List<StatusReport> = synchronized(lock) {
        reports.values.filter { it.locationId == locationId }
            .sortedByDescending { it.timestampMillis }
    }

    fun flaggedReports(): List<StatusReport> = synchronized(lock) {
        reports.values.filter { it.isFlagged }.sortedByDescending { it.flagCount }
    }

    fun lastReportFrom(reporterId: String, locationId: String): StatusReport? = synchronized(lock) {
        reports.values
            .filter { it.reporterId == reporterId && it.locationId == locationId }
            .maxByOrNull { it.timestampMillis }
    }

    fun add(report: StatusReport) {
        synchronized(lock) {
            reports[report.id] = report
        }
        persist()
        eventBus.publish(ReportEvent.ReportSubmitted(report))
    }

    fun flag(reportId: String, flaggerId: String): StatusReport? {
        val updated = synchronized(lock) {
            val current = reports[reportId] ?: return null
            if (flaggerId in current.flaggedByReporterIds) return current
            val flagged = current.copy(
                flagCount = current.flagCount + 1,
                flaggedByReporterIds = current.flaggedByReporterIds + flaggerId
            )
            reports[reportId] = flagged
            flagged
        }
        persist()
        eventBus.publish(ReportEvent.ReportFlagged(updated))
        return updated
    }

    fun unflag(reportId: String, flaggerId: String): StatusReport? {
        val updated = synchronized(lock) {
            val current = reports[reportId] ?: return null
            if (flaggerId !in current.flaggedByReporterIds) return current
            val cleared = current.copy(
                flagCount = (current.flagCount - 1).coerceAtLeast(0),
                flaggedByReporterIds = current.flaggedByReporterIds - flaggerId
            )
            reports[reportId] = cleared
            cleared
        }
        persist()
        eventBus.publish(ReportEvent.ReportFlagged(updated))
        return updated
    }

    fun remove(reportId: String): StatusReport? {
        val removed = synchronized(lock) { reports.remove(reportId) } ?: return null
        persist()
        eventBus.publish(ReportEvent.ReportRemoved(removed))
        return removed
    }

    fun clearFlags(reportId: String): StatusReport? {
        val cleared = synchronized(lock) {
            val current = reports[reportId] ?: return null
            val updated = current.copy(flagCount = 0, flaggedByReporterIds = emptyList())
            reports[reportId] = updated
            updated
        }
        persist()
        return cleared
    }

    private fun persist() {
        val snapshot = synchronized(lock) { reports.values.toList() }
        store?.save(snapshot)
    }
}
