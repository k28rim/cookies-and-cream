package com.cookiesandcream.queuebuddy.data

import com.cookiesandcream.queuebuddy.domain.event.ReportEvent
import com.cookiesandcream.queuebuddy.domain.event.ReportEventBus
import com.cookiesandcream.queuebuddy.domain.model.StatusReport

// Stores reports in memory + on disk, and announces each new report on the event bus.
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

    fun add(report: StatusReport) {
        reports[report.id] = report
        store?.save(reports.values.toList())
        eventBus.publish(ReportEvent.ReportSubmitted(report))
    }
}
