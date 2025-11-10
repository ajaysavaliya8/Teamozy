package com.hrms.jeejateamozy.feature.workreport.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.network.WorkReport
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

sealed class WorkReportListOutcome {
    data class Success(
        val message: String,
        val month: Int,
        val year: Int,
        val totalReports: Int,
        val reports: List<WorkReport>
    ) : WorkReportListOutcome()

    data class Error(val message: String) : WorkReportListOutcome()
}

sealed class CreateWorkReportOutcome {
    data class Success(
        val message: String,
        val workReportId: Int?,
        val reportsToday: Int?,
        val remainingReportsToday: Int?
    ) : CreateWorkReportOutcome()

    data class Error(val message: String) : CreateWorkReportOutcome()
}

class WorkReportRepository(private val context: Context) {

    private val api = NetworkModule.apiService
    private val pm = PreferencesManager.getInstance(context)

    /**
     * Get work reports for a specific month and year
     */
    suspend fun getWorkReports(month: Int, year: Int): WorkReportListOutcome =
        withContext(Dispatchers.IO) {
            return@withContext try {
                Log.d("WORK_REPORT", "Fetching work reports for month: $month, year: $year")

                val response = api.getWorkReports(month = month, year = year)

                Log.d("WORK_REPORT", "Get work reports response code: ${response.code()}")

                when {
                    response.isSuccessful && response.code() == 200 -> {
                        val responseBody = response.body()
                        if (responseBody?.status == "success") {
                            Log.d("WORK_REPORT", "Successfully fetched ${responseBody.total_reports} reports")

                            val reports = responseBody.reports.map { it.toDomain() }

                            WorkReportListOutcome.Success(
                                message = responseBody.message,
                                month = responseBody.month,
                                year = responseBody.year,
                                totalReports = responseBody.total_reports,
                                reports = reports
                            )
                        } else {
                            WorkReportListOutcome.Error(
                                responseBody?.message ?: "Failed to fetch work reports"
                            )
                        }
                    }

                    response.code() == 401 -> {
                        AppStateManager.emitUnauthorized()
                        WorkReportListOutcome.Error("Unauthorized. Please login again.")
                    }

                    response.code() == 403 -> {
                        val errorMsg = extractErrorMessage(response)
                        WorkReportListOutcome.Error(
                            errorMsg ?: "Only active employees can view work reports"
                        )
                    }

                    response.code() == 404 -> {
                        WorkReportListOutcome.Error("Employee not found")
                    }

                    else -> {
                        val errorMsg = extractErrorMessage(response)
                        WorkReportListOutcome.Error(
                            errorMsg ?: "Failed to fetch work reports"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("WORK_REPORT", "Error fetching work reports", e)
                WorkReportListOutcome.Error(
                    e.message ?: "Network error occurred"
                )
            }
        }

    /**
     * Create a new work report
     */
    suspend fun createWorkReport(
        workDescription: String,
        attachmentUris: List<Uri>
    ): CreateWorkReportOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("WORK_REPORT", "Creating work report with ${attachmentUris.size} attachments")

            // Validate input
            if (workDescription.length < 10) {
                return@withContext CreateWorkReportOutcome.Error(
                    "Work description must be at least 10 characters"
                )
            }

            if (workDescription.length > 5000) {
                return@withContext CreateWorkReportOutcome.Error(
                    "Work description must not exceed 5000 characters"
                )
            }

            if (attachmentUris.size > 5) {
                return@withContext CreateWorkReportOutcome.Error(
                    "Maximum 5 attachments allowed per work report"
                )
            }

            // Create multipart request
            val workDescriptionPart = workDescription.toRequestBody("text/plain".toMediaTypeOrNull())

            // Process attachments
            val attachmentParts = mutableListOf<MultipartBody.Part>()

            for ((index, uri) in attachmentUris.withIndex()) {
                try {
                    val file = copyUriToTempFile(uri) ?: continue

                    // Validate file size (max 10MB)
                    if (file.length() > 10 * 1024 * 1024) {
                        file.delete()
                        return@withContext CreateWorkReportOutcome.Error(
                            "File ${file.name} is too large. Maximum size is 10MB per file."
                        )
                    }

                    // Determine media type
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData(
                        "attachments",
                        file.name,
                        requestFile
                    )
                    attachmentParts.add(part)

                    Log.d("WORK_REPORT", "Prepared attachment $index: ${file.name} (${file.length()} bytes)")
                } catch (e: Exception) {
                    Log.e("WORK_REPORT", "Error processing attachment $index", e)
                    return@withContext CreateWorkReportOutcome.Error(
                        "Failed to process attachment: ${e.message}"
                    )
                }
            }

            // Make API call
            val response = api.createWorkReport(
                workDescription = workDescriptionPart,
                attachments = if (attachmentParts.isNotEmpty()) attachmentParts else null
            )

            // Clean up temp files
            attachmentParts.forEach { part ->
                try {
                    // Extract file from part and delete
                    // This is a best effort cleanup
                } catch (e: Exception) {
                    Log.w("WORK_REPORT", "Failed to cleanup temp file", e)
                }
            }

            Log.d("WORK_REPORT", "Create work report response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 201 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("WORK_REPORT", "Work report created successfully")

                        CreateWorkReportOutcome.Success(
                            message = responseBody.message,
                            workReportId = responseBody.work_report?.id,
                            reportsToday = responseBody.reports_today,
                            remainingReportsToday = responseBody.remaining_reports_today
                        )
                    } else {
                        CreateWorkReportOutcome.Error(
                            responseBody?.message ?: "Failed to create work report"
                        )
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    CreateWorkReportOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 403 -> {
                    val errorMsg = extractErrorMessage(response)
                    CreateWorkReportOutcome.Error(
                        errorMsg ?: "Only active employees can create work reports"
                    )
                }

                response.code() == 400 -> {
                    val errorMsg = extractErrorMessage(response)
                    CreateWorkReportOutcome.Error(
                        errorMsg ?: "Invalid work report data"
                    )
                }

                else -> {
                    val errorMsg = extractErrorMessage(response)
                    CreateWorkReportOutcome.Error(
                        errorMsg ?: "Failed to create work report"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("WORK_REPORT", "Error creating work report", e)
            CreateWorkReportOutcome.Error(
                e.message ?: "Network error occurred"
            )
        }
    }

    /**
     * Helper function to copy URI to temporary file
     */
    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            val fileName = getFileName(uri) ?: "temp_${System.currentTimeMillis()}"
            val tempFile = File(context.cacheDir, fileName)

            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()

            tempFile
        } catch (e: Exception) {
            Log.e("WORK_REPORT", "Error copying URI to temp file", e)
            null
        }
    }

    /**
     * Helper function to get file name from URI
     */
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }
        if (fileName == null) {
            fileName = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return fileName
    }

    /**
     * Helper function to extract error message from response
     */
    private fun extractErrorMessage(response: retrofit2.Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = JSONObject(errorBody)

                // Check for standard message field
                if (json.has("message")) {
                    return json.getString("message")
                }

                // Check for detail field (FastAPI style)
                if (json.has("detail")) {
                    val detail = json.get("detail")

                    // If detail is a string
                    if (detail is String) {
                        return detail
                    }

                    // If detail is an array (validation errors)
                    if (detail is org.json.JSONArray && detail.length() > 0) {
                        val firstError = detail.getJSONObject(0)
                        val msg = firstError.optString("msg", "")

                        return when {
                            msg.contains("at least 10 characters", ignoreCase = true) ->
                                "Work description must be at least 10 characters"
                            msg.contains("required", ignoreCase = true) -> {
                                val field = firstError.optJSONArray("loc")?.getString(1) ?: "Field"
                                "$field is required"
                            }
                            else -> msg.ifBlank { "Invalid input" }
                        }
                    }
                }

                errorBody
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("WORK_REPORT", "Error parsing error response", e)
            null
        }
    }
}