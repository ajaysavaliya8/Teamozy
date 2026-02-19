package com.hrms.jeejateamozy.feature.attendance.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hrms.jeejateamozy.core.network.*
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.NetworkErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream

sealed class CorrectionRequestOptionsOutcome {
    data class Success(val options: CorrectionRequestOptionsData) : CorrectionRequestOptionsOutcome()
    data class Error(val message: String) : CorrectionRequestOptionsOutcome()
}

sealed class SubmitCorrectionRequestOutcome {
    data class Success(val message: String) : SubmitCorrectionRequestOutcome()
    data class Error(val message: String) : SubmitCorrectionRequestOutcome()
}

sealed class WithdrawCorrectionRequestOutcome {
    data class Success(val message: String) : WithdrawCorrectionRequestOutcome()
    data class Error(val message: String) : WithdrawCorrectionRequestOutcome()
}

sealed class CorrectionRequestListOutcome {
    data class Success(
        val requests: List<CorrectionRequestListItem>,
        val pagination: PaginationInfo
    ) : CorrectionRequestListOutcome()
    data class Error(val message: String) : CorrectionRequestListOutcome()
}

sealed class CorrectionRequestDetailOutcome {
    data class Success(val detail: CorrectionRequestDetail) : CorrectionRequestDetailOutcome()
    data class Error(val message: String) : CorrectionRequestDetailOutcome()
}

sealed class DownloadAttachmentOutcome {
    data class Success(val file: File) : DownloadAttachmentOutcome()
    data class Error(val message: String) : DownloadAttachmentOutcome()
}

class CorrectionRequestRepository(private val context: Context) {

    private val api = NetworkModule.apiService
    private val TAG = "CorrectionRequestRepo"

    suspend fun getCorrectionRequestOptions(): CorrectionRequestOptionsOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val response = api.getCorrectionRequestOptions()
                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody?.success == true) {
                            CorrectionRequestOptionsOutcome.Success(
                                options = responseBody.data.toDomain()
                            )
                        } else {
                            CorrectionRequestOptionsOutcome.Error("Failed to fetch options")
                        }
                    }
                    response.code() == 401 -> {
                        AppStateManager.emitUnauthorized()
                        CorrectionRequestOptionsOutcome.Error("Unauthorized. Please login again.")
                    }
                    else -> {
                        val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch options")
                        CorrectionRequestOptionsOutcome.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching options", e)
                CorrectionRequestOptionsOutcome.Error(e.message ?: "Network error occurred")
            }
        }

    suspend fun submitCorrectionRequest(
        requestType: String,
        attendanceDate: String,
        reason: String,
        attendanceRecordId: Int? = null,
        leaveTypeId: Int? = null,
        requestedStatus: String? = null,
        requestedCheckIn: String? = null,
        requestedCheckOut: String? = null,
        priority: String = "normal",
        attachmentUri: Uri? = null
    ): SubmitCorrectionRequestOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val requestTypeBody = requestType.toRequestBody("text/plain".toMediaTypeOrNull())
            val attendanceDateBody = attendanceDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val reasonBody = reason.toRequestBody("text/plain".toMediaTypeOrNull())
            val priorityBody = priority.toRequestBody("text/plain".toMediaTypeOrNull())

            val attendanceRecordIdBody = attendanceRecordId?.toString()
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val leaveTypeIdBody = leaveTypeId?.toString()
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestedStatusBody = requestedStatus
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestedCheckInBody = requestedCheckIn
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestedCheckOutBody = requestedCheckOut
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            var attachmentPart: MultipartBody.Part? = null
            attachmentUri?.let { uri ->
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val fileBytes = inputStream?.readBytes()
                    inputStream?.close()

                    fileBytes?.let { bytes ->
                        val fileSizeMB = bytes.size / (1024.0 * 1024.0)
                        if (fileSizeMB > 5.0) {
                            return@withContext SubmitCorrectionRequestOutcome.Error(
                                "File size exceeds 5MB limit"
                            )
                        }

                        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                        val fileName = getFileName(uri) ?: "attachment"

                        val tempFile = File(context.cacheDir, fileName)
                        FileOutputStream(tempFile).use { it.write(bytes) }

                        val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                        attachmentPart = MultipartBody.Part.createFormData(
                            "supporting_document",
                            fileName,
                            requestBody
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error preparing attachment", e)
                    return@withContext SubmitCorrectionRequestOutcome.Error(
                        "Failed to prepare attachment: ${e.message}"
                    )
                }
            }

            val response = api.submitCorrectionRequest(
                requestType = requestTypeBody,
                attendanceDate = attendanceDateBody,
                reason = reasonBody,
                attendanceRecordId = attendanceRecordIdBody,
                leaveTypeId = leaveTypeIdBody,
                requestedStatus = requestedStatusBody,
                requestedCheckIn = requestedCheckInBody,
                requestedCheckOut = requestedCheckOutBody,
                priority = priorityBody,
                supporting_document = attachmentPart
            )

            when {
                response.isSuccessful && (response.code() == 200 || response.code() == 201) -> {
                    val responseBody = response.body()
                    if (responseBody?.success == true) {
                        SubmitCorrectionRequestOutcome.Success(
                            message = responseBody.message
                        )
                    } else {
                        SubmitCorrectionRequestOutcome.Error(
                            responseBody?.message ?: "Failed to submit correction request"
                        )
                    }
                }
                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    SubmitCorrectionRequestOutcome.Error("Unauthorized. Please login again.")
                }
                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to submit correction request")
                    SubmitCorrectionRequestOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting correction request", e)
            SubmitCorrectionRequestOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    suspend fun withdrawCorrectionRequest(
        requestId: Int,
        withdrawalReason: String
    ): WithdrawCorrectionRequestOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val response = api.withdrawCorrectionRequest(
                    requestId = requestId,
                    withdrawalReason = withdrawalReason
                )
                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody?.success == true) {
                            WithdrawCorrectionRequestOutcome.Success(
                                message = responseBody.message
                            )
                        } else {
                            WithdrawCorrectionRequestOutcome.Error(
                                responseBody?.message ?: "Failed to withdraw correction request"
                            )
                        }
                    }
                    response.code() == 401 -> {
                        AppStateManager.emitUnauthorized()
                        WithdrawCorrectionRequestOutcome.Error("Unauthorized. Please login again.")
                    }
                    else -> {
                        val errorMsg = NetworkErrorHandler.extract(response, "Failed to withdraw correction request")
                        WithdrawCorrectionRequestOutcome.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error withdrawing correction request", e)
                WithdrawCorrectionRequestOutcome.Error(e.message ?: "Network error occurred")
            }
        }

    suspend fun getCorrectionRequests(
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): CorrectionRequestListOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = api.getCorrectionRequests(
                status = status,
                startDate = startDate,
                endDate = endDate,
                page = page,
                pageSize = pageSize
            )
            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.success == true) {
                        CorrectionRequestListOutcome.Success(
                            requests = responseBody.data.requests.map { it.toDomain() },
                            pagination = responseBody.data.pagination.toDomain()
                        )
                    } else {
                        CorrectionRequestListOutcome.Error("Failed to fetch correction requests")
                    }
                }
                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    CorrectionRequestListOutcome.Error("Unauthorized. Please login again.")
                }
                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch correction requests")
                    CorrectionRequestListOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching correction requests", e)
            CorrectionRequestListOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    suspend fun getCorrectionRequestDetail(requestId: Int): CorrectionRequestDetailOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val response = api.getCorrectionRequestDetail(requestId = requestId)
                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody?.success == true) {
                            CorrectionRequestDetailOutcome.Success(
                                detail = responseBody.data.toDomain()
                            )
                        } else {
                            CorrectionRequestDetailOutcome.Error("Failed to fetch correction request detail")
                        }
                    }
                    response.code() == 401 -> {
                        AppStateManager.emitUnauthorized()
                        CorrectionRequestDetailOutcome.Error("Unauthorized. Please login again.")
                    }
                    else -> {
                        val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch correction request detail")
                        CorrectionRequestDetailOutcome.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching correction request detail", e)
                CorrectionRequestDetailOutcome.Error(e.message ?: "Network error occurred")
            }
        }

    suspend fun downloadCorrectionAttachment(requestId: Int): DownloadAttachmentOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val response = api.downloadCorrectionAttachment(requestId = requestId)
                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            val fileName = extractFileName(response) ?: "attachment_$requestId"
                            val file = saveAttachment(responseBody, fileName)
                            if (file != null) {
                                DownloadAttachmentOutcome.Success(file = file)
                            } else {
                                DownloadAttachmentOutcome.Error("Failed to save attachment")
                            }
                        } else {
                            DownloadAttachmentOutcome.Error("Empty response body")
                        }
                    }
                    response.code() == 401 -> {
                        AppStateManager.emitUnauthorized()
                        DownloadAttachmentOutcome.Error("Unauthorized. Please login again.")
                    }
                    response.code() == 404 -> {
                        DownloadAttachmentOutcome.Error("Attachment not found")
                    }
                    else -> {
                        DownloadAttachmentOutcome.Error("Failed to download attachment")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading attachment", e)
                DownloadAttachmentOutcome.Error(e.message ?: "Network error occurred")
            }
        }

    private fun getFileName(uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) it.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name", e)
            null
        }
    }

    private fun <T> extractFileName(response: retrofit2.Response<T>): String? {
        val contentDisposition = response.headers()["Content-Disposition"]
        return contentDisposition?.let {
            val startIndex = it.indexOf("filename=")
            if (startIndex != -1) it.substring(startIndex + 9).replace("\"", "") else null
        }
    }

    private fun saveAttachment(responseBody: ResponseBody, fileName: String): File? {
        return try {
            val downloadDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val file = File(downloadDir, fileName)
            responseBody.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving attachment", e)
            null
        }
    }
}
