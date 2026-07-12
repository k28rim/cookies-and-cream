package com.cookiesandcream.queuebuddy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cookiesandcream.queuebuddy.AppContainer
import com.cookiesandcream.queuebuddy.domain.LocationSummary
import com.cookiesandcream.queuebuddy.domain.SortOption
import com.cookiesandcream.queuebuddy.domain.event.ReportEventListener
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// The current search/filter/sort choices plus the resulting list.
data class HomeUiState(
    val query: String = "",
    val category: LocationCategory? = null,
    val sort: SortOption = SortOption.RECENTLY_UPDATED,
    val summaries: List<LocationSummary> = emptyList()
) {
    val hasActiveFilters: Boolean get() = query.isNotBlank() || category != null
}

// MVVM: owns the home state. As an Observer it subscribes to the event bus so a new
// report refreshes the list at once, and it re-checks every 30s so the relative
// "updated X min ago" timestamps stay current.
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val listener = ReportEventListener { refresh() }

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

    fun setQuery(query: String) = update { it.copy(query = query) }

    fun setCategory(category: LocationCategory?) = update { it.copy(category = category) }

    fun setSort(sort: SortOption) = update { it.copy(sort = sort) }

    fun resetFilters() = update { it.copy(query = "", category = null) }

    private fun update(transform: (HomeUiState) -> HomeUiState) {
        _uiState.value = transform(_uiState.value)
        refresh()
    }

    private fun refresh() {
        val state = _uiState.value
        _uiState.value = state.copy(
            summaries = container.facade.locationSummaries(state.query, state.category, state.sort)
        )
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(container) as T
        }
    }
}
