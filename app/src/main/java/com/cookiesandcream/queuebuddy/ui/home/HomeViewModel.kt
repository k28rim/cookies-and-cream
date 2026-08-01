package com.cookiesandcream.queuebuddy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cookiesandcream.queuebuddy.AppContainer
import com.cookiesandcream.queuebuddy.domain.ConditionFilter
import com.cookiesandcream.queuebuddy.domain.LocationSummary
import com.cookiesandcream.queuebuddy.domain.SortOption
import com.cookiesandcream.queuebuddy.domain.event.ReportEventListener
import com.cookiesandcream.queuebuddy.domain.model.LocationCategory
import com.cookiesandcream.queuebuddy.domain.model.University
import com.cookiesandcream.queuebuddy.domain.suggestion.LocationSuggestion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// The current search text plus the resulting list.
data class HomeUiState(
    val query: String = "",
    val university: University = University.WATERLOO,
    val category: LocationCategory? = null,
    val conditions: Set<ConditionFilter> = emptySet(),
    val sort: SortOption = SortOption.RECENTLY_UPDATED,
    val summaries: List<LocationSummary> = emptyList(),
    val suggestion: LocationSuggestion? = null,
    val suggestionsEnabled: Boolean = true,
    val suggestionDismissed: Boolean = false,
    val moderatorMode: Boolean = false,
    val hasUserLocation: Boolean = false
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || category != null || conditions.isNotEmpty()
}

// MVVM: owns the home screen state. Subscribes to the event bus (Observer) so a
// new report refreshes the list at once.
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var userLatitude: Double? = null
    private var userLongitude: Double? = null

    private val listener = ReportEventListener { refresh() }

    init {
        container.eventBus.subscribe(listener)
        viewModelScope.launch {
            container.moderatorMode.collect { isModerator ->
                _uiState.value = _uiState.value.copy(moderatorMode = isModerator)
            }
        }

        viewModelScope.launch {
            container.suggestionsEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(suggestionsEnabled = enabled)
                refresh()
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

    fun setQuery(query: String) = update { it.copy(query = query) }

    // Changing any filter also un-dismisses the suggestion, so it pops back up
    // for the new filter after being closed with the X.
    fun setCategory(category: LocationCategory?) = update {
        it.copy(category = category, suggestionDismissed = false)
    }

    fun setUniversity(university: University) = update {
        it.copy(
            university = university, query = "", category = null,
            conditions = emptySet(), suggestionDismissed = false
        )
    }

    fun toggleCondition(condition: ConditionFilter) = update {
        val newConditions =
            if (condition in it.conditions) it.conditions - condition else it.conditions + condition
        it.copy(conditions = newConditions, suggestionDismissed = false)
    }

    fun setSort(sort: SortOption) = update { it.copy(sort = sort) }

    fun resetFilters() = update {
        it.copy(query = "", category = null, conditions = emptySet(), suggestionDismissed = false)
    }

    fun dismissSuggestion() = update { it.copy(suggestionDismissed = true) }

    fun setModeratorMode(enabled: Boolean) {
        container.moderatorMode.value = enabled
    }

    fun setSuggestionsEnabled(enabled: Boolean) = container.setSuggestionsEnabled(enabled)

    fun setUserLocation(latitude: Double, longitude: Double) {
        userLatitude = latitude
        userLongitude = longitude
        update { it.copy(hasUserLocation = true) }
    }

    private fun update(transform: (HomeUiState) -> HomeUiState) {
        _uiState.value = transform(_uiState.value)
        refresh()
    }

    private fun refresh() {
        val state = _uiState.value
        val summaries = container.facade.locationSummaries(
            query = state.query,
            category = state.category,
            university = state.university,
            conditions = state.conditions,
            sort = state.sort,
            userLatitude = userLatitude,
            userLongitude = userLongitude
        )
        // Suggest only while a category or condition filter is narrowing the list.
        val suggestion = if (state.suggestionsEnabled && !state.suggestionDismissed &&
            (state.category != null || state.conditions.isNotEmpty())
        ) {
            container.facade.suggestedLocation(summaries)
        } else {
            null
        }
        _uiState.value = state.copy(summaries = summaries, suggestion = suggestion)
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(container) as T
        }
    }
}
