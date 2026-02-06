package com.hrms.jeejateamozy.feature.leave.data

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.NetworkErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

// =============================================================================
// OUTCOME CLASSES
// =============================================================================

sealed class LeaveTypesOutcome {
    data class Success(val leaveTypes: List<LeaveType>) : LeaveTypesOutcome()
    data class Error(val message: String) : LeaveTypesOutcome()
}

sealed class ApplyLeaveOutcome {
    data class Success(val message: String) : ApplyLeaveOutcome()
    data class Error(val message: String) : ApplyLeaveOutcome()
}

sealed class LeaveApplicationsOutcome {
    data class Success(
        val applications: List<LeaveApplication>,
        val pagination: PaginationInfo
    ) : LeaveApplicationsOutcome()
    data class Error(val message: String) : LeaveApplicationsOutcome()
}

sealed class LeaveApplicationDetailOutcome {
    data class Success(val detail: LeaveApplicationDetail) : LeaveApplicationDetailOutcome()
    data class Error(val message: String) : LeaveApplicationDetailOutcome()
}

sealed class LeaveSummaryOutcome {
    data class Success(val summary: LeaveSummary) : LeaveSummaryOutcome()
    data class Error(val message: String) : LeaveSummaryOutcome()
}

sealed class WithdrawLeaveOutcome {
    data class Success(val message: String) : WithdrawLeaveOutcome()
    data class Error(val message: String) : WithdrawLeaveOutcome()
}

sealed class CancelLeaveOutcome {
    data class Success(val message: String) : CancelLeaveOutcome()
    data class Error(val message: String) : CancelLeaveOutcome()
}

// =============================================================================
// REPOSITORY
// =============================================================================

class LeaveRepository(private val context: Context) {

    private val api = NetworkModule.apiService

    companion object {
        private const val TAG = "LeaveRepository"
    }

    // =========================================================================
    // GET LEAVE TYPES
    // =========================================================================

    suspend fun getLeaveTypes(): LeaveTypesOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching leave types")

            val response = api.getLeaveTypes()
            Log.d(TAG, "Get leave types response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        val leaveTypes = responseBody.data.map { it.toDomain() }
                        Log.d(TAG, "Successfully fetched ${leaveTypes.size} leave types")
                        LeaveTypesOutcome.Success(leaveTypes = leaveTypes)
                    } else {
                        LeaveTypesOutcome.Error("Failed to fetch leave types")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    LeaveTypesOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    LeaveTypesOutcome.Error("Employee not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch leave types")
                    LeaveTypesOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching leave types", e)
            LeaveTypesOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    // =========================================================================
    // APPLY LEAVE
    // =========================================================================

    suspend fun applyLeave(
        leaveTypeId: Int,
        startDate: String,
        endDate: String,
        leaveReason: String,
        alternateContact: String?,
        emergencyContact: String?,
        taskDependedOnYou: Boolean,
        dependencyHandledBy: String?,
        handoverNotes: String?,
        priority: String,
        supportingDocumentFile: File?
    ): ApplyLeaveOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Applying for leave: type=$leaveTypeId, dates=$startDate to $endDate, priority=$priority")

            // Create request bodies
            val leaveTypeIdBody = leaveTypeId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val startDateBody = startDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val endDateBody = endDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val leaveReasonBody = leaveReason.toRequestBody("text/plain".toMediaTypeOrNull())
            val alternateContactBody = alternateContact?.toRequestBody("text/plain".toMediaTypeOrNull())
            val emergencyContactBody = emergencyContact?.toRequestBody("text/plain".toMediaTypeOrNull())
            val taskDependedOnYouBody = taskDependedOnYou.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val dependencyHandledByBody = dependencyHandledBy?.toRequestBody("text/plain".toMediaTypeOrNull())
            val handoverNotesBody = handoverNotes?.toRequestBody("text/plain".toMediaTypeOrNull())
            val priorityBody = priority.toRequestBody("text/plain".toMediaTypeOrNull())

            // Create multipart file if provided
            val filePart = supportingDocumentFile?.let {
                val requestFile = it.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("supporting_document", it.name, requestFile)
            }

            val response = api.applyLeave(
                leaveTypeId = leaveTypeIdBody,
                startDate = startDateBody,
                endDate = endDateBody,
                leaveReason = leaveReasonBody,
                alternateContact = alternateContactBody,
                emergencyContact = emergencyContactBody,
                taskDependedOnYou = taskDependedOnYouBody,
                dependencyHandledBy = dependencyHandledByBody,
                handoverNotes = handoverNotesBody,
                priority = priorityBody,
                supportingDocument = filePart
            )

            Log.d(TAG, "Apply leave response code: ${response.code()}")

            when {
                response.isSuccessful && (response.code() == 201 || response.code() == 200) -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d(TAG, "Leave applied successfully")
                        ApplyLeaveOutcome.Success(message = responseBody.message)
                    } else {
                        ApplyLeaveOutcome.Error(responseBody?.message ?: "Failed to apply leave")
                    }
                }

                response.code() == 400 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Invalid leave application data")
                    ApplyLeaveOutcome.Error(errorMsg)
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    ApplyLeaveOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Leave type not found")
                    ApplyLeaveOutcome.Error(errorMsg)
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to apply leave")
                    ApplyLeaveOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying leave", e)
            ApplyLeaveOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    // =========================================================================
    // GET LEAVE APPLICATIONS (History)
    // =========================================================================

    suspend fun getLeaveApplications(
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): LeaveApplicationsOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching leave applications: status=$status, page=$page")

            val response = api.getLeaveApplications(
                status = status,
                startDate = startDate,
                endDate = endDate,
                page = page,
                pageSize = pageSize
            )

            Log.d(TAG, "Get leave applications response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d(TAG, "Successfully fetched ${responseBody.data.applications.size} applications")

                        val applications = responseBody.data.applications.map { it.toDomain() }
                        val pagination = responseBody.data.pagination.toDomain()

                        LeaveApplicationsOutcome.Success(
                            applications = applications,
                            pagination = pagination
                        )
                    } else {
                        LeaveApplicationsOutcome.Error("Failed to fetch leave applications")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    LeaveApplicationsOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    LeaveApplicationsOutcome.Error("Employee record not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch leave applications")
                    LeaveApplicationsOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching leave applications", e)
            LeaveApplicationsOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    // =========================================================================
    // GET LEAVE APPLICATION DETAIL
    // =========================================================================

    suspend fun getLeaveApplicationDetail(
        applicationId: Int
    ): LeaveApplicationDetailOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching leave application detail: ID=$applicationId")

            val response = api.getLeaveApplicationDetail(applicationId)

            Log.d(TAG, "Get leave detail response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d(TAG, "Successfully fetched leave application detail")
                        LeaveApplicationDetailOutcome.Success(detail = responseBody.data.toDomain())
                    } else {
                        LeaveApplicationDetailOutcome.Error("Failed to fetch leave application detail")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    LeaveApplicationDetailOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    LeaveApplicationDetailOutcome.Error("Leave application not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch leave application detail")
                    LeaveApplicationDetailOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching leave application detail", e)
            LeaveApplicationDetailOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    // =========================================================================
    // WITHDRAW LEAVE
    // =========================================================================

    suspend fun withdrawLeave(
        applicationId: Int,
        withdrawalReason: String
    ): WithdrawLeaveOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Withdrawing leave application: ID=$applicationId")

            val response = api.withdrawLeave(
                applicationId = applicationId,
                withdrawalReason = withdrawalReason
            )

            Log.d(TAG, "Withdraw leave response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d(TAG, "Leave withdrawn successfully")
                        WithdrawLeaveOutcome.Success(message = responseBody.message)
                    } else {
                        WithdrawLeaveOutcome.Error(responseBody?.message ?: "Failed to withdraw leave")
                    }
                }

                response.code() == 400 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Cannot withdraw this leave application")
                    WithdrawLeaveOutcome.Error(errorMsg)
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    WithdrawLeaveOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    WithdrawLeaveOutcome.Error("Leave application not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to withdraw leave")
                    WithdrawLeaveOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error withdrawing leave", e)
            WithdrawLeaveOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    // =========================================================================
    // CANCEL LEAVE
    // =========================================================================

    suspend fun cancelLeave(
        applicationId: Int,
        cancellationReason: String
    ): CancelLeaveOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Cancelling leave application: ID=$applicationId")

            val response = api.cancelLeave(
                applicationId = applicationId,
                cancellationReason = cancellationReason
            )

            Log.d(TAG, "Cancel leave response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d(TAG, "Leave cancelled successfully")
                        CancelLeaveOutcome.Success(message = responseBody.message)
                    } else {
                        CancelLeaveOutcome.Error(responseBody?.message ?: "Failed to cancel leave")
                    }
                }

                response.code() == 400 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Cannot cancel this leave application")
                    CancelLeaveOutcome.Error(errorMsg)
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    CancelLeaveOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    CancelLeaveOutcome.Error("Leave application not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to cancel leave")
                    CancelLeaveOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling leave", e)
            CancelLeaveOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    // =========================================================================
    // GET LEAVE SUMMARY
    // =========================================================================

    suspend fun getLeaveSummary(year: Int? = null): LeaveSummaryOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Fetching leave summary for year: $year")

            val response = api.getLeaveSummary(year = year)

            Log.d(TAG, "Get leave summary response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d(TAG, "Successfully fetched leave summary")
                        val summary = responseBody.data.toDomain()
                        LeaveSummaryOutcome.Success(summary = summary)
                    } else {
                        LeaveSummaryOutcome.Error("Failed to fetch leave summary")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    LeaveSummaryOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    LeaveSummaryOutcome.Error("Employee record not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch leave summary")
                    LeaveSummaryOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching leave summary", e)
            LeaveSummaryOutcome.Error(e.message ?: "Network error occurred")
        }
    }

}