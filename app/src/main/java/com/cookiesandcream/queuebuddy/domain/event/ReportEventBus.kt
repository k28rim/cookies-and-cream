package com.cookiesandcream.queuebuddy.domain.event

import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import java.util.concurrent.CopyOnWriteArrayList

// Event-driven style + Observer pattern. The data layer publishes a small event
// whenever a report is submitted; screens subscribe and refresh themselves, so the
// app updates on its own instead of each screen polling the repository.
sealed interface ReportEvent {
    val locationId: String

    data class ReportSubmitted(val report: StatusReport) : ReportEvent {
        override val locationId: String get() = report.locationId
    }
}

// A subscriber. fun interface lets a ViewModel pass a plain lambda.
fun interface ReportEventListener {
    fun onReportEvent(event: ReportEvent)
}

// The bus everyone shares. CopyOnWriteArrayList keeps subscribe/publish thread-safe.
class ReportEventBus {
    private val listeners = CopyOnWriteArrayList<ReportEventListener>()

    fun subscribe(listener: ReportEventListener) {
        listeners.add(listener)
    }

    fun unsubscribe(listener: ReportEventListener) {
        listeners.remove(listener)
    }

    fun publish(event: ReportEvent) {
        for (listener in listeners) listener.onReportEvent(event)
    }
}
