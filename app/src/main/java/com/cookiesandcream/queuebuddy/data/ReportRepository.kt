package com.cookiesandcream.queuebuddy.data

import com.cookiesandcream.queuebuddy.domain.model.StatusReport

// Stores status reports in memory and persists them to disk (Repository pattern).
class ReportRepository(private val store: ReportStore? = null) {

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
    }
}
