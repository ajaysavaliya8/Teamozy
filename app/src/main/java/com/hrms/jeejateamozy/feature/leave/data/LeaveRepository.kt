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
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

sealed class LeaveTypesOutcome {
    data class Success(val leaveTypes: List<LeaveType>) : LeaveTypesOutcome()
    data class Error(val message: String) : LeaveTypesOutcome()
}

sealed class ApplyLeaveOutcome {
    data class Success(
        val message: String,
        val applicationId: Int,
        val appliedAt: String,
        val numDays: Int
    ) : ApplyLeaveOutcome()
    data class Error(val message: String) : ApplyLeaveOutcome()
}

sealed class LeaveApplicationsOutcome {
    data class Success(
        val applications: List<LeaveApplication>,
        val pagination: PaginationInfo
    ) : LeaveApplicationsOutcome()
    data class Error(val message: String) : LeaveApplicationsOutcome()
}

sealed class LeaveSummaryOutcome {
    data class Success(val summary: LeaveSummary) : LeaveSummaryOutcome()
    data class Error(val message: String) : LeaveSummaryOutcome()
}

sealed class WithdrawLeaveOutcome {
    data class Success(val message: String) : WithdrawLeaveOutcome()
    data class Error(val message: String) : WithdrawLeaveOutcome()
}

/**
 * ✅ UPDATED: Now using NetworkErrorHandler
 */
class LeaveRepository(private val context: Context) {

    private val api = NetworkModule.apiService

    /**
     * Get all available leave types
     */
    suspend fun getLeaveTypes(): LeaveTypesOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("LEAVE", "Fetching leave types")

            val response = api.getLeaveTypes()

            Log.d("LEAVE", "Get leave types response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("LEAVE", "Successfully fetched ${responseBody.data.size} leave types")

                        val leaveTypes = responseBody.data.map { it.toDomain() }

                        LeaveTypesOutcome.Success(leaveTypes = leaveTypes)
                    } else {
                        LeaveTypesOutcome.Error("Failed to fetch leave types")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    LeaveTypesOutcome.Error("Unauthorized. Please login again.")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch leave types")
                    LeaveTypesOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("LEAVE", "Error fetching leave types", e)
            LeaveTypesOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Apply for leave
     */
    suspend fun applyLeave(
        leaveTypeId: Int,
        startDate: String,
        endDate: String,
        leaveReason: String,
        alternateContact: String?,
        taskDependedOnYou: Boolean,
        dependencyHandledBy: String?,
        supportingDocumentFile: File?
    ): ApplyLeaveOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("LEAVE", "Applying for leave: type=$leaveTypeId, dates=$startDate to $endDate")

            // Create request bodies
            val startDateBody = startDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val endDateBody = endDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val leaveReasonBody = leaveReason.toRequestBody("text/plain".toMediaTypeOrNull())
            val alternateContactBody = alternateContact?.toRequestBody("text/plain".toMediaTypeOrNull())
            val dependencyHandledByBody = dependencyHandledBy?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Create multipart file if provided
            val filePart = supportingDocumentFile?.let {
                val requestFile = it.readBytes().toRequestBody("application/octet-stream".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("supporting_document", it.name, requestFile)
            }

            val response = api.applyLeave(
                leaveTypeId = leaveTypeId,
                startDate = startDateBody,
                endDate = endDateBody,
                leaveReason = leaveReasonBody,
                alternateContact = alternateContactBody,
                taskDependedOnYou = taskDependedOnYou,
                dependencyHandledBy = dependencyHandledByBody,
                supportingDocument = filePart
            )

            Log.d("LEAVE", "Apply leave response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 201 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success" && responseBody.data != null) {
                        Log.d("LEAVE", "Leave applied successfully: ID=${responseBody.data.application_id}")

                        ApplyLeaveOutcome.Success(
                            message = responseBody.message,
                            applicationId = responseBody.data.application_id,
                            appliedAt = responseBody.data.applied_at,
                            numDays = responseBody.data.num_days
                        )
                    } else {
                        ApplyLeaveOutcome.Error("Failed to apply leave")
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
                    ApplyLeaveOutcome.Error("Leave type not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to apply leave")
                    ApplyLeaveOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("LEAVE", "Error applying leave", e)
            ApplyLeaveOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Get leave applications history
     */
    suspend fun getLeaveApplications(
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): LeaveApplicationsOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("LEAVE", "Fetching leave applications: status=$status, page=$page")

            val response = api.getLeaveApplications(
                status = status,
                startDate = startDate,
                endDate = endDate,
                page = page,
                pageSize = pageSize
            )

            Log.d("LEAVE", "Get leave applications response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("LEAVE", "Successfully fetched ${responseBody.data.applications.size} applications")

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

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch leave applications")
                    LeaveApplicationsOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("LEAVE", "Error fetching leave applications", e)
            LeaveApplicationsOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Get leave summary
     */
    suspend fun getLeaveSummary(year: Int? = null): LeaveSummaryOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("LEAVE", "Fetching leave summary for year: $year")

            val response = api.getLeaveSummary(year = year)

            Log.d("LEAVE", "Get leave summary response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("LEAVE", "Successfully fetched leave summary")

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

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch leave summary")
                    LeaveSummaryOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("LEAVE", "Error fetching leave summary", e)
            LeaveSummaryOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Withdraw leave application
     */
    suspend fun withdrawLeave(
        applicationId: Int,
        withdrawalReason: String
    ): WithdrawLeaveOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("LEAVE", "Withdrawing leave application: ID=$applicationId")

            val response = api.withdrawLeave(
                applicationId = applicationId,
                withdrawalReason = withdrawalReason
            )

            Log.d("LEAVE", "Withdraw leave response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("LEAVE", "Leave withdrawn successfully")

                        WithdrawLeaveOutcome.Success(message = responseBody.message)
                    } else {
                        WithdrawLeaveOutcome.Error("Failed to withdraw leave")
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
            Log.e("LEAVE", "Error withdrawing leave", e)
            WithdrawLeaveOutcome.Error(e.message ?: "Network error occurred")
        }
    }
}