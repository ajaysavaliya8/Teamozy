package com.example.teamozy.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.teamozy.repository.AttendanceRepository
import com.example.teamozy.repository.AttendanceResult
import com.example.teamozy.repository.AttendanceStatusResult
import com.example.teamozy.utils.LocationHelper
import com.example.teamozy.utils.LocationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(application)
    private val locationHelper = LocationHelper(application)

    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    private val _showViolationDialog = MutableStateFlow(false)
    val showViolationDialog: StateFlow<Boolean> = _showViolationDialog.asStateFlow()

    private var currentViolation: ViolationData? = null

    init {
        checkStatus()
    }

    private fun checkStatus() {
        viewModelScope.launch {
            println("DEBUG: Checking current status...")
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = repository.checkStatus()) {
                is AttendanceStatusResult.Success -> {
                    println("DEBUG: Status check successful - canCheckIn: ${result.canCheckIn}, buttonText: ${result.buttonText}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        canCheckIn = result.canCheckIn,
                        buttonText = result.buttonText,
                        errorMessage = null
                    )
                }
                is AttendanceStatusResult.Error -> {
                    println("DEBUG: Status check failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        canCheckIn = true,
                        buttonText = "Check In",
                        errorMessage = "Failed to get current status: ${result.message}"
                    )
                }
            }
        }
    }

    fun onAttendanceButtonClick() {
        viewModelScope.launch {
            println("DEBUG: Button clicked - current state: canCheckIn=${_uiState.value.canCheckIn}")

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )

            println("DEBUG: State reset to loading")

            val locationResult = locationHelper.getCurrentLocation()

            when (locationResult) {
                is LocationResult.Success -> {
                    println("DEBUG: Location obtained - lat: ${locationResult.latitude}, lng: ${locationResult.longitude}, accuracy: ${locationResult.accuracy}m")

                    if (locationResult.warning != null) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = locationResult.warning
                        )
                    }

                    val result = if (_uiState.value.canCheckIn) {
                        println("DEBUG: Calling check-in API with real coordinates")
                        repository.checkIn(longitude = locationResult.longitude, latitude = locationResult.latitude)
                    } else {
                        println("DEBUG: Calling check-out API with real coordinates")
                        repository.checkOut(longitude = locationResult.longitude, latitude = locationResult.latitude)
                    }

                    handleAttendanceResult(result)
                }
                is LocationResult.Error -> {
                    println("DEBUG: Location error: ${locationResult.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Location error: ${locationResult.message}",
                        successMessage = null
                    )
                }
            }
        }
    }

    private fun handleAttendanceResult(result: AttendanceResult) {
        println("DEBUG: Got result: $result")

        when (result) {
            is AttendanceResult.Success -> {
                println("DEBUG: Processing success")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    canCheckIn = !_uiState.value.canCheckIn,
                    buttonText = if (_uiState.value.canCheckIn) "Check Out" else "Check In",
                    successMessage = result.message,
                    errorMessage = null
                )
                println("DEBUG: Success state set: ${_uiState.value}")
            }
            is AttendanceResult.ViolationRequired -> {
                println("DEBUG: Processing violation with t_token: '${result.tToken}'")

                if (result.tToken.isNotEmpty()) {
                    println("DEBUG: Valid t_token found, showing violation dialog")
                    _uiState.value = _uiState.value.copy(isLoading = false)

                    val violationTitle = when {
                        result.isLate && result.isLocationViolation -> "Late & Location Issue"
                        result.isLate -> "Late Arrival"
                        result.isLocationViolation -> "Location Issue"
                        else -> "Attendance Violation"
                    }

                    currentViolation = ViolationData(
                        tToken = result.tToken,
                        isCheckIn = _uiState.value.canCheckIn,
                        title = violationTitle,
                        isLate = result.isLate,
                        isLocationViolation = result.isLocationViolation
                    )
                    _showViolationDialog.value = true
                } else {
                    println("DEBUG: Empty t_token, treating as error")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                        successMessage = null
                    )
                }
            }
            is AttendanceResult.Error -> {
                println("DEBUG: Processing error: ${result.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.message,
                    successMessage = null
                )
                println("DEBUG: Error state set: ${_uiState.value}")
            }
        }
    }

    fun submitViolation(reason: String) {
        val violation = currentViolation ?: return

        println("DEBUG: Submitting violation with t_token: '${violation.tToken}' and reason: '$reason'")
        println("DEBUG: isLate: ${violation.isLate}, isLocationViolation: ${violation.isLocationViolation}")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = if (violation.isCheckIn) {
                repository.submitCheckInViolation(violation.tToken, reason)
            } else {
                repository.submitCheckOutViolation(violation.tToken, reason)
            }

            when (result) {
                is AttendanceResult.Success -> {
                    println("DEBUG: Violation submission successful")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        canCheckIn = !violation.isCheckIn,
                        buttonText = if (violation.isCheckIn) "Check Out" else "Check In",
                        successMessage = result.message,
                        errorMessage = null
                    )
                    dismissViolationDialog()
                }
                is AttendanceResult.Error -> {
                    println("DEBUG: Violation submission failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message,
                        successMessage = null
                    )
                    dismissViolationDialog()
                }
                else -> {
                    println("DEBUG: Unexpected violation result")
                    dismissViolationDialog()
                }
            }
        }
    }

    fun dismissViolationDialog() {
        println("DEBUG: Dismissing violation dialog")
        _showViolationDialog.value = false
        currentViolation = null
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun getViolationTitle(): String = currentViolation?.title ?: "Reason Required"

    fun refreshStatus() {
        println("DEBUG: Manually refreshing status")
        checkStatus()
    }
}

data class AttendanceUiState(
    val isLoading: Boolean = false,
    val canCheckIn: Boolean = true,
    val buttonText: String = "Check In",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class ViolationData(
    val tToken: String,
    val isCheckIn: Boolean,
    val title: String,
    val isLate: Boolean = false,
    val isLocationViolation: Boolean = false
)