package com.hrms.jeejateamozy.feature.circular.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hrms.jeejateamozy.feature.circular.data.*

import com.hrms.jeejateamozy.core.network.Circular
import com.hrms.jeejateamozy.core.network.CircularDetail
import com.hrms.jeejateamozy.core.network.CircularStats
import com.hrms.jeejateamozy.core.network.PaginationInfo

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


/**
 * UI State for Circular List Screen
 */
data class CircularUiState(
    val isLoading: Boolean = false,
    val circulars: List<Circular> = emptyList(),
    val pagination: PaginationInfo? = null,
    val stats: CircularStats? = null,
    val errorMessage: String? = null,
    val selectedFilters: CircularFilters = CircularFilters()
)

/**
 * Circular filters
 */
data class CircularFilters(
    val status: String? = null,
    val circularType: String? = null,
    val priority: String? = null,
    val currentPage: Int = 1,
    val pageSize: Int = 10
)

/**
 * UI State for Circular Detail Screen
 */
data class CircularDetailUiState(
    val isLoading: Boolean = false,
    val circular: CircularDetail? = null,
    val errorMessage: String? = null
)

sealed class CircularEvent {
    data class ShowError(val message: String) : CircularEvent()
    data class ShowSuccess(val message: String) : CircularEvent()
}

class CircularViewModel(
    private val repo: CircularRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CircularUiState())
    val uiState: StateFlow<CircularUiState> = _uiState.asStateFlow()

    private val _detailUiState = MutableStateFlow(CircularDetailUiState())
    val detailUiState: StateFlow<CircularDetailUiState> = _detailUiState.asStateFlow()

    private val _events = MutableSharedFlow<CircularEvent>()
    val events: SharedFlow<CircularEvent> = _events.asSharedFlow()

    init {
        // Load initial data
        loadCirculars()
        loadStats()
    }

    /**
     * Load circulars with current filters
     */
    fun loadCirculars() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }

                val filters = _uiState.value.selectedFilters

                when (val outcome = repo.getCirculars(
                    status = filters.status,
                    circularType = filters.circularType,
                    priority = filters.priority,
                    page = filters.currentPage,
                    pageSize = filters.pageSize
                )) {
                    is CircularListOutcome.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                circulars = outcome.circulars,
                                pagination = outcome.pagination,
                                errorMessage = null
                            )
                        }
                        Log.d("CircularViewModel", "Loaded ${outcome.circulars.size} circulars")
                    }

                    is CircularListOutcome.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(CircularEvent.ShowError(outcome.message))
                        Log.e("CircularViewModel", "Error loading circulars: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(CircularEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("CircularViewModel", "Exception loading circulars", e)
            }
        }
    }

    /**
     * Load circular statistics
     */
    fun loadStats() {
        viewModelScope.launch {
            try {
                when (val outcome = repo.getCircularStats()) {
                    is CircularStatsOutcome.Success -> {
                        _uiState.update { it.copy(stats = outcome.stats) }
                        Log.d("CircularViewModel", "Loaded stats: ${outcome.stats}")
                    }

                    is CircularStatsOutcome.Error -> {
                        Log.e("CircularViewModel", "Error loading stats: ${outcome.message}")
                        // Don't show error for stats - it's not critical
                    }
                }
            } catch (e: Exception) {
                Log.e("CircularViewModel", "Exception loading stats", e)
            }
        }
    }

    /**
     * Load circular detail by ID
     */
    fun loadCircularDetail(circularId: Int) {
        viewModelScope.launch {
            try {
                _detailUiState.update { it.copy(isLoading = true, errorMessage = null) }

                when (val outcome = repo.getCircularDetail(circularId)) {
                    is CircularDetailOutcome.Success -> {
                        _detailUiState.update {
                            it.copy(
                                isLoading = false,
                                circular = outcome.circular,
                                errorMessage = null
                            )
                        }
                        Log.d("CircularViewModel", "Loaded circular detail: ${outcome.circular.title}")
                    }

                    is CircularDetailOutcome.Error -> {
                        _detailUiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = outcome.message
                            )
                        }
                        _events.emit(CircularEvent.ShowError(outcome.message))
                        Log.e("CircularViewModel", "Error loading circular detail: ${outcome.message}")
                    }
                }
            } catch (e: Exception) {
                _detailUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error"
                    )
                }
                _events.emit(CircularEvent.ShowError(e.message ?: "Unknown error"))
                Log.e("CircularViewModel", "Exception loading circular detail", e)
            }
        }
    }

    /**
     * Apply filters and reload circulars
     */
    fun applyFilters(filters: CircularFilters) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedFilters = filters) }
            loadCirculars()
        }
    }

    /**
     * Load next page
     */
    fun loadNextPage() {
        val currentFilters = _uiState.value.selectedFilters
        val pagination = _uiState.value.pagination

        if (pagination != null && currentFilters.currentPage < pagination.totalPages) {
            val nextFilters = currentFilters.copy(currentPage = currentFilters.currentPage + 1)
            applyFilters(nextFilters)
        }
    }

    /**
     * Load previous page
     */
    fun loadPreviousPage() {
        val currentFilters = _uiState.value.selectedFilters

        if (currentFilters.currentPage > 1) {
            val previousFilters = currentFilters.copy(currentPage = currentFilters.currentPage - 1)
            applyFilters(previousFilters)
        }
    }

    /**
     * Refresh circulars (reset to page 1)
     */
    fun refresh() {
        val currentFilters = _uiState.value.selectedFilters
        val resetFilters = currentFilters.copy(currentPage = 1)
        _uiState.update { it.copy(selectedFilters = resetFilters) }
        loadCirculars()
        loadStats()
    }

    /**
     * Clear filters
     */
    fun clearFilters() {
        applyFilters(CircularFilters(currentPage = 1))
    }

    /**
     * Reset detail state when leaving detail screen
     */
    fun resetDetailState() {
        _detailUiState.update { CircularDetailUiState() }
    }
}