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
    val moderatorMode: Boolean = false,
    val reportSheetOpen: Boolean = false,
    val draft: ReportDraft = ReportDraft(),
    val message: String? = null,
    val submitSucceeded: Boolean = false
)

class LocationDetailViewModel(
    private val container: AppContainer,
    private val locationId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // Observer: refresh when a report for THIS location changes.
    private val listener = ReportEventListener { event ->
        if (event.locationId == locationId) refresh()
    }

    init {
        container.eventBus.subscribe(listener)
        viewModelScope.launch {
            container.moderatorMode.collect { isModerator ->
                _uiState.value = _uiState.value.copy(moderatorMode = isModerator)
            }
        }
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

    fun openReportSheet() = _uiState.value.let {
        _uiState.value = it.copy(reportSheetOpen = true, draft = ReportDraft(), submitSucceeded = false)
    }

    fun closeReportSheet() {
        _uiState.value = _uiState.value.copy(reportSheetOpen = false)
    }

    fun updateDraft(transform: (ReportDraft) -> ReportDraft) {
        _uiState.value = _uiState.value.copy(draft = transform(_uiState.value.draft))
    }

    fun submitReport() {
        val draft = _uiState.value.draft
        val report = StatusReport.Builder(locationId, container.reporterId)
            .crowdLevel(draft.crowdLevel)
            .waitEstimate(draft.waitEstimate)
            .seatAvailability(draft.seatAvailability)
            .noiseLevel(draft.noiseLevel)
            .resourceStatus(draft.resourceStatus)
            .note(draft.note)
            .build()

        when (val result = container.facade.submitReport(report)) {
            is SubmitResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    reportSheetOpen = false,
                    message = "Thanks! Your report is live.",
                    submitSucceeded = true
                )
            }
            is SubmitResult.Rejected -> {

                _uiState.value = _uiState.value.copy(message = result.reason, submitSucceeded = false)
            }
        }
    }

    fun toggleFlag(report: StatusReport) {
        if (alreadyFlaggedByMe(report)) {
            container.facade.unflagReport(report.id, container.reporterId)
            _uiState.value = _uiState.value.copy(message = "Your flag was removed.")
        } else {
            container.facade.flagReport(report.id, container.reporterId)
            _uiState.value = _uiState.value.copy(
                message = "Report flagged for moderators. Thanks for keeping data honest."
            )
        }
        refresh()
    }

    fun removeReport(reportId: String) {
        container.facade.removeReport(reportId)
        _uiState.value = _uiState.value.copy(message = "Report removed.")
        refresh()
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun alreadyFlaggedByMe(report: StatusReport): Boolean =
        container.reporterId in report.flaggedByReporterIds

    private fun refresh() {
        _uiState.value = _uiState.value.copy(detail = container.facade.locationDetail(locationId))
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
