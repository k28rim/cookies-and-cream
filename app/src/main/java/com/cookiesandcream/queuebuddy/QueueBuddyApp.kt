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

    private val prefs = context.getSharedPreferences("queue_buddy", Context.MODE_PRIVATE)
    private val reportStore = ReportStore(File(context.filesDir, "reports.json"))
    val locationRepository = LocationRepository()
    val reportRepository = ReportRepository(eventBus, reportStore)
    val facade = QueueBuddyFacade(locationRepository, reportRepository)

    val reporterId: String by lazy {
        prefs.getString("reporter_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("reporter_id", it).apply()
        }
    }

    val moderatorMode = kotlinx.coroutines.flow.MutableStateFlow(false)

    // Off switch for the "Suggested" card on Home; the choice sticks across launches.
    val suggestionsEnabled = kotlinx.coroutines.flow.MutableStateFlow(
        prefs.getBoolean("suggestions_enabled", true)
    )

    fun setSuggestionsEnabled(enabled: Boolean) {
        suggestionsEnabled.value = enabled
        prefs.edit().putBoolean("suggestions_enabled", enabled).apply()
    }

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
