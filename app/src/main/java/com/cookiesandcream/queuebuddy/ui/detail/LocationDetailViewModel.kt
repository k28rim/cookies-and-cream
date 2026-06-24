package com.cookiesandcream.queuebuddy.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cookiesandcream.queuebuddy.AppContainer
import com.cookiesandcream.queuebuddy.domain.LocationDetail
import com.cookiesandcream.queuebuddy.domain.model.CrowdLevel
import com.cookiesandcream.queuebuddy.domain.model.NoiseLevel
import com.cookiesandcream.queuebuddy.domain.model.SeatAvailability
import com.cookiesandcream.queuebuddy.domain.model.StatusReport
import com.cookiesandcream.queuebuddy.domain.model.WaitEstimate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The report being composed in the bottom sheet.
data class ReportDraft(
    val crowdLevel: CrowdLevel? = null,
    val waitEstimate: WaitEstimate? = null,
    val seatAvailability: SeatAvailability? = null,
    val noiseLevel: NoiseLevel? = null,
    val note: String = ""
)

data class DetailUiState(
    val detail: LocationDetail? = null,
    val sheetOpen: Boolean = false,
    val draft: ReportDraft = ReportDraft()
)

class LocationDetailViewModel(
    private val container: AppContainer,
    private val locationId: String
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        _state.value = _state.value.copy(detail = container.facade.locationDetail(locationId))
    }

    fun openSheet() {
        _state.value = _state.value.copy(sheetOpen = true, draft = ReportDraft())
    }

    fun closeSheet() {
        _state.value = _state.value.copy(sheetOpen = false)
    }

    fun updateDraft(transform: (ReportDraft) -> ReportDraft) {
        _state.value = _state.value.copy(draft = transform(_state.value.draft))
    }

    // Builds the report from the draft (Builder pattern) and stores it.
    fun submit() {
        val draft = _state.value.draft
        val report = StatusReport.Builder(locationId, container.reporterId)
            .crowdLevel(draft.crowdLevel)
            .waitEstimate(draft.waitEstimate)
            .seatAvailability(draft.seatAvailability)
            .noiseLevel(draft.noiseLevel)
            .note(draft.note)
            .build()
        container.facade.submitReport(report)
        _state.value = _state.value.copy(sheetOpen = false)
        refresh()
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
