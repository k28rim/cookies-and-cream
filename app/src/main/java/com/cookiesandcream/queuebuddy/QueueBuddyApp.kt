package com.cookiesandcream.queuebuddy

import android.app.Application
import android.content.Context
import com.cookiesandcream.queuebuddy.data.LocationRepository
import com.cookiesandcream.queuebuddy.data.ReportRepository
import com.cookiesandcream.queuebuddy.data.ReportStore
import com.cookiesandcream.queuebuddy.domain.QueueBuddyFacade
import com.cookiesandcream.queuebuddy.domain.event.ReportEventBus
import java.io.File
import java.util.UUID

// Wires the app's pieces together: a shared event bus, the repositories (data),
// and the facade (domain) the UI talks to.
class AppContainer(context: Context) {
    val eventBus = ReportEventBus()

    private val reportStore = ReportStore(File(context.filesDir, "reports.json"))
    val locationRepository = LocationRepository()
    val reportRepository = ReportRepository(eventBus, reportStore)
    val facade = QueueBuddyFacade(locationRepository, reportRepository)

    // Anonymous id for the current user, used to tag reports.
    val reporterId: String = UUID.randomUUID().toString()

    fun start() {
        reportRepository.loadPersisted()
    }
}

class QueueBuddyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
