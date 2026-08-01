package com.cookiesandcream.queuebuddy.ui.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cookiesandcream.queuebuddy.AppContainer
import com.cookiesandcream.queuebuddy.domain.event.ReportEventListener
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FlaggedItem(
    val report: StatusReport,
    val locationName: String
)

data class ModerationUiState(
    val flagged: List<FlaggedItem> = emptyList(),
    val message: String? = null
)

class ModerationViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(ModerationUiState())
    val uiState: StateFlow<ModerationUiState> = _uiState.asStateFlow()

    private val listener = ReportEventListener { refresh() }

    init {
        container.eventBus.subscribe(listener)
        refresh()
    }

    override fun onCleared() {
        container.eventBus.unsubscribe(listener)
    }

    fun remove(reportId: String) {
        container.facade.removeReport(reportId)
        _uiState.value = _uiState.value.copy(message = "Report removed.")
        refresh()
    }

    fun dismissFlags(reportId: String) {
        container.facade.clearFlags(reportId)
        _uiState.value = _uiState.value.copy(message = "Flags dismissed; report kept.")
        refresh()
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun refresh() {
        _uiState.value = _uiState.value.copy(
            flagged = container.facade.flaggedReports().map { report ->
                FlaggedItem(
                    report = report,
                    locationName = container.facade.locationById(report.locationId)?.name
                        ?: "Unknown location"
                )
            }
        )
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ModerationViewModel(container) as T
        }
    }
}
