package com.cookiesandcream.queuebuddy.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cookiesandcream.queuebuddy.AppContainer
import com.cookiesandcream.queuebuddy.domain.LocationDetail
import com.cookiesandcream.queuebuddy.domain.SubmitResult
import com.cookiesandcream.queuebuddy.domain.event.ReportEventListener
import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.NoiseLevel
import com.cookiesandcream.queuebuddy.domain.model.ResourceStatus
import com.cookiesandcream.queuebuddy.domain.model.SeatAvailability
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import com.cookiesandcream.queuebuddy.domain.model.WaitEstimate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// The report being composed in the bottom sheet.
data class ReportDraft(
    val crowdLevel: CrowdLevel? = null,
    val waitEstimate: WaitEstimate? = null,
    val seatAvailability: SeatAvailability? = null,
    val noiseLevel: NoiseLevel? = null,
    val resourceStatus: ResourceStatus? = null,
    val note: String = ""
)

data class DetailUiState(
    val detail: LocationDetail? = null,
    val sheetOpen: Boolean = false,
    val draft: ReportDraft = ReportDraft(),
    val message: String? = null,
    val submitSucceeded: Boolean = false
)

class LocationDetailViewModel(
    private val container: AppContainer,
    private val locationId: String
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    // Observer: refresh when a report for THIS location changes.
    private val listener = ReportEventListener { event ->
        if (event.locationId == locationId) refresh()
    }

    init {
        container.eventBus.subscribe(listener)
        viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                refresh()
            }
        }
        refresh()
    }

    override fun onCleared() {
        container.eventBus.unsubscribe(listener)
    }

    private fun refresh() {
        _state.value = _state.value.copy(detail = container.facade.locationDetail(locationId))
    }

    fun openSheet() {
        _state.value = _state.value.copy(sheetOpen = true, draft = ReportDraft(), submitSucceeded = false)
    }

    fun closeSheet() {
        _state.value = _state.value.copy(sheetOpen = false)
    }

    fun updateDraft(transform: (ReportDraft) -> ReportDraft) {
        _state.value = _state.value.copy(draft = transform(_state.value.draft))
    }

    // Builds the report (Builder pattern) and submits it through the validation chain.
    // The facade tells us whether it was accepted or why it was rejected.
    fun submit() {
        val draft = _state.value.draft
        val report = StatusReport.Builder(locationId, container.reporterId)
            .crowdLevel(draft.crowdLevel)
            .waitEstimate(draft.waitEstimate)
            .seatAvailability(draft.seatAvailability)
            .noiseLevel(draft.noiseLevel)
            .resourceStatus(draft.resourceStatus)
            .note(draft.note)
            .build()
        when (val result = container.facade.submitReport(report)) {
            is SubmitResult.Success ->
                _state.value = _state.value.copy(
                    sheetOpen = false,
                    message = "Thanks! Your report is live.",
                    submitSucceeded = true
                )
            is SubmitResult.Rejected ->
                _state.value = _state.value.copy(message = result.reason, submitSucceeded = false)
        }
    }

    fun toggleFlag(report: StatusReport) {
        if (alreadyFlaggedByMe(report)) {
            container.facade.unflagReport(report.id, container.reporterId)
            _state.value = _state.value.copy(message = "Your flag was removed.")
        } else {
            container.facade.flagReport(report.id, container.reporterId)
            _state.value = _state.value.copy(message = "Report flagged for moderators. Thanks!")
        }
        refresh()
    }

    fun alreadyFlaggedByMe(report: StatusReport): Boolean =
        container.reporterId in report.flaggedByReporterIds

    fun consumeMessage() {
        _state.value = _state.value.copy(message = null)
    }

    companion object {
        fun factory(container: AppContainer, locationId: String) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LocationDetailViewModel(container, locationId) as T
            }
    }
}
