package com.cookiesandcream.queuebuddy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cookiesandcream.queuebuddy.AppContainer
import com.cookiesandcream.queuebuddy.domain.LocationSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// MVVM: holds the home list state; the screen just renders it.
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _summaries = MutableStateFlow<List<LocationSummary>>(emptyList())
    val summaries: StateFlow<List<LocationSummary>> = _summaries.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _summaries.value = container.facade.locationSummaries()
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(container) as T
        }
    }
}
