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

/**
 * Sealed classes for correction request outcomes
 */
sealed class CorrectionRequestOptionsOutcome {
    data class Success(val options: CorrectionRequestOptionsData) : CorrectionRequestOptionsOutcome()
    data class Error(val message: String) : CorrectionRequestOptionsOutcome()
}

sealed class SubmitCorrectionRequestOutcome {
    data class Success(val data: SubmittedCorrectionRequestDataDto) : SubmitCorrectionRequestOutcome()
    data class Error(val message: String) : SubmitCorrectionRequestOutcome()
}

sealed class WithdrawCorrectionRequestOutcome {
    data class Success(val data: WithdrawnCorrectionRequestDataDto) : WithdrawCorrectionRequestOutcome()
    data class Error(val message: String) : WithdrawCorrectionRequestOutcome()
}

sealed class DownloadAttachmentOutcome {
    data class Success(val file: File) : DownloadAttachmentOutcome()
    data class Error(val message: String) : DownloadAttachmentOutcome()
}

/**
 * Repository for Correction Request operations
 * ✅ UPDATED: Now using NetworkErrorHandler
 */
class CorrectionRequestRepository(private val context: Context) {

    private val api = NetworkModule.apiService
    private val TAG = "CorrectionRequestRepo"

    /**
     * Get form options for correction request
     */
    suspend fun getCorrectionRequestOptions(): CorrectionRequestOptionsOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d(TAG, "Fetching correction request options")

                val response = api.getCorrectionRequestOptions()

                Log.d(TAG, "Options response code: ${response.code()}")

                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody?.status == "success") {
                            Log.d(TAG, "Successfully fetched options")
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

    /**
     * Submit correction request
     */
    suspend fun submitCorrectionRequest(
        requestType: String,
        attendanceDate: String,
        reason: String,
        attendanceRecordId: Int? = null,
        attendanceSessionId: Int? = null,
        leaveTypeId: Int? = null,
        requestedStatus: String? = null,
        requestedCheckIn: String? = null,
        requestedCheckOut: String? = null,
        priority: String = "NORMAL",
        attachmentUri: Uri? = null
    ): SubmitCorrectionRequestOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Submitting correction request")
            Log.d(TAG, "  request_type: $requestType")
            Log.d(TAG, "  attendance_date: $attendanceDate")
            Log.d(TAG, "  reason: ${reason.take(50)}")

            // Prepare multipart request bodies
            val requestTypeBody = requestType.toRequestBody("text/plain".toMediaTypeOrNull())
            val attendanceDateBody = attendanceDate.toRequestBody("text/plain".toMediaTypeOrNull())
            val reasonBody = reason.toRequestBody("text/plain".toMediaTypeOrNull())
            val priorityBody = priority.toRequestBody("text/plain".toMediaTypeOrNull())

            val attendanceRecordIdBody = attendanceRecordId?.toString()
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val attendanceSessionIdBody = attendanceSessionId?.toString()
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val leaveTypeIdBody = leaveTypeId?.toString()
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestedStatusBody = requestedStatus
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestedCheckInBody = requestedCheckIn
                ?.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestedCheckOutBody = requestedCheckOut
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Prepare attachment if provided
            var attachmentPart: MultipartBody.Part? = null
            attachmentUri?.let { uri ->
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val fileBytes = inputStream?.readBytes()
                    inputStream?.close()

                    fileBytes?.let { bytes ->
                        // Validate file size (max 5MB)
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
                            "attachment",
                            fileName,
                            requestBody
                        )

                        Log.d(TAG, "Attachment prepared: $fileName (${fileSizeMB}MB)")
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
                attendanceSessionId = attendanceSessionIdBody,
                leaveTypeId = leaveTypeIdBody,
                requestedStatus = requestedStatusBody,
                requestedCheckIn = requestedCheckInBody,
                requestedCheckOut = requestedCheckOutBody,
                priority = priorityBody,
                attachment = attachmentPart
            )

            Log.d(TAG, "Submit response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success" && responseBody.data != null) {
                        Log.d(TAG, "Successfully submitted correction request")
                        SubmitCorrectionRequestOutcome.Success(data = responseBody.data)
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

                response.code() == 400 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Invalid request data")
                    SubmitCorrectionRequestOutcome.Error(errorMsg)
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

    /**
     * Withdraw correction request
     */
    suspend fun withdrawCorrectionRequest(requestId: Int): WithdrawCorrectionRequestOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d(TAG, "Withdrawing correction request: $requestId")

                val response = api.withdrawCorrectionRequest(requestId = requestId)

                Log.d(TAG, "Withdraw response code: ${response.code()}")

                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody?.status == "success" && responseBody.data != null) {
                            Log.d(TAG, "Successfully withdrawn correction request")
                            WithdrawCorrectionRequestOutcome.Success(data = responseBody.data)
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

                    response.code() == 404 -> {
                        WithdrawCorrectionRequestOutcome.Error("Correction request not found")
                    }

                    response.code() == 400 -> {
                        val errorMsg = NetworkErrorHandler.extract(response, "Cannot withdraw this request")
                        WithdrawCorrectionRequestOutcome.Error(errorMsg)
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

    /**
     * Download active correction request attachment
     */
    suspend fun downloadCorrectionAttachment(requestId: Int): DownloadAttachmentOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d(TAG, "Downloading attachment for request: $requestId")

                val response = api.downloadCorrectionAttachment(requestId = requestId)

                Log.d(TAG, "Download response code: ${response.code()}")

                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            val fileName = extractFileName(response) ?: "attachment_$requestId"
                            val file = saveAttachment(responseBody, fileName)

                            if (file != null) {
                                Log.d(TAG, "Successfully downloaded attachment: ${file.name}")
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

    /**
     * Download settled correction request attachment
     */
    suspend fun downloadSettledCorrectionAttachment(settledId: Int): DownloadAttachmentOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d(TAG, "Downloading settled attachment: $settledId")

                val response = api.downloadSettledCorrectionAttachment(settledId = settledId)

                Log.d(TAG, "Download response code: ${response.code()}")

                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            val fileName = extractFileName(response) ?: "settled_attachment_$settledId"
                            val file = saveAttachment(responseBody, fileName)

                            if (file != null) {
                                Log.d(TAG, "Successfully downloaded settled attachment: ${file.name}")
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
                Log.e(TAG, "Error downloading settled attachment", e)
                DownloadAttachmentOutcome.Error(e.message ?: "Network error occurred")
            }
        }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private fun getFileName(uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        it.getString(nameIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
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
            if (startIndex != -1) {
                it.substring(startIndex + 9).replace("\"", "")
            } else {
                null
            }
        }
    }

    private fun saveAttachment(responseBody: ResponseBody, fileName: String): File? {
        return try {
            val downloadDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val file = File(downloadDir, fileName)
            val inputStream = responseBody.byteStream()
            val outputStream = FileOutputStream(file)

            inputStream.use { input ->
                outputStream.use { output ->
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