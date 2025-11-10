package com.hrms.jeejateamozy.feature.attendance.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.core.state.AppStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.hrms.jeejateamozy.feature.face.util.FaceVectorUtil
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

sealed class AttendanceOutcome {
    data class Success(
        val currentState: String,
        val message: String,
        val lastCheckInTime: String?,
        val attendanceStatus: String?,
        val isComplete: Boolean?
    ) : AttendanceOutcome()

    data class Error(val message: String) : AttendanceOutcome()
}

sealed class CheckInOutcome {
    data class RequiresFaceVerification(
        val tToken: String,
        val faceVector: FloatArray?,
        val minimumQualityScore: Float,
        val isLate: Boolean,
        val isOutOfRange: Boolean,
        val lateReasonRequired: Boolean,
        val outOfRangeReasonRequired: Boolean,
        val message: String
    ) : CheckInOutcome()

    data class RequiresReasons(
        val tToken: String,
        val isLate: Boolean,
        val isOutOfRange: Boolean,
        val lateReasonRequired: Boolean,
        val outOfRangeReasonRequired: Boolean,
        val message: String
    ) : CheckInOutcome()

    data class Success(val message: String) : CheckInOutcome()
    data class Error(val message: String) : CheckInOutcome()
}

sealed class CheckOutOutcome {
    data class RequiresFaceVerification(
        val tToken: String,
        val faceVector: FloatArray?,
        val minimumQualityScore: Float,
        val workHours: Float,
        val isEarly: Boolean,
        val isOutOfRange: Boolean,
        val earlyReasonRequired: Boolean,
        val outOfRangeReasonRequired: Boolean,
        val workReportRequired: Boolean,  // ✅ NEW
        val message: String
    ) : CheckOutOutcome()

    data class RequiresReasons(
        val tToken: String,
        val workHours: Float,
        val isEarly: Boolean,
        val isOutOfRange: Boolean,
        val earlyReasonRequired: Boolean,
        val outOfRangeReasonRequired: Boolean,
        val workReportRequired: Boolean,  // ✅ NEW
        val message: String
    ) : CheckOutOutcome()

    data class Success(val message: String) : CheckOutOutcome()
    data class Error(val message: String) : CheckOutOutcome()
}

sealed class SignatureOutcome {
    data class Success(
        val message: String,
        val attendanceRecordId: Int?,
        val checkInTime: String?
    ) : SignatureOutcome()

    data class Error(val message: String) : SignatureOutcome()
}

class AttendanceRepository(private val context: Context) {

    private val api = NetworkModule.apiService
    private val pm = PreferencesManager.getInstance(context)

    private fun deviceId(): String {
        val id = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: ""
        Log.d("ATTENDANCE", "deviceId() from system = '$id'")
        if (id.isBlank()) {
            Log.e("ATTENDANCE", "❌ Device ID from system is BLANK!")
        }
        return id
    }

    private fun token(): String {
        val t = pm.authToken.orEmpty()
        Log.d("ATTENDANCE", "token() called = ${if (t.isBlank()) "BLANK" else "EXISTS (${t.take(20)}...)"}")
        return t
    }

    /**
     * Get current attendance status
     */
    suspend fun getStatus(): AttendanceOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("ATTENDANCE", "getStatus() - deviceId: ${deviceId()}")

            val res = api.checkStatus(
                deviceId = deviceId(),
                token = token()
            )

            Log.d("NET", "checkStatus -> code=${res.code()}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()
                    val data = body?.data

                    if (data != null) {
                        Log.d("NET", "Status: ${data.current_state}, Message: ${data.message}")

                        AttendanceOutcome.Success(
                            currentState = data.current_state,
                            message = data.message,
                            lastCheckInTime = data.last_check_in_time,
                            attendanceStatus = data.attendance_status,
                            isComplete = data.is_complete
                        )
                    } else {
                        AttendanceOutcome.Error("Invalid response from server")
                    }
                }

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    AttendanceOutcome.Error("Unauthorized. Please login again.")
                }

                else -> AttendanceOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            Log.e("NET", "Exception in getStatus", e)
            AttendanceOutcome.Error(friendlyNetError(e))
        }
    }

    /**
     * Initial check-in request
     */
    suspend fun checkIn(
        latitude: Double,
        longitude: Double
    ): CheckInOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("ATTENDANCE", "checkIn() - deviceId: ${deviceId()}, lat: $latitude, lon: $longitude")

            val res = api.checkIn(
                deviceId = deviceId(),
                longitude = longitude,
                latitude = latitude,
                token = token()
            )

            Log.d("NET", "checkIn -> code=${res.code()}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()

                    if (body != null && body.t_token != null) {
                        val faceVerificationRequired = body.face_verification_required ?: false
                        val minimumQualityScore = body.minimum_quality_score ?: 0.57f
                        val isLate = body.is_late ?: false
                        val isOutOfRange = body.is_out_of_range ?: false
                        val lateReasonRequired = body.late_reason_required ?: false
                        val outOfRangeReasonRequired = body.out_of_range_reason_required ?: false
                        val message = body.message ?: "Ready for check-in"

                        Log.d("NET", "Check-in initial success:")
                        Log.d("NET", "  face_verification_required: $faceVerificationRequired")
                        Log.d("NET", "  minimum_quality_score: $minimumQualityScore")
                        Log.d("NET", "  is_late: $isLate")
                        Log.d("NET", "  is_out_of_range: $isOutOfRange")
                        Log.d("NET", "  late_reason_required: $lateReasonRequired")
                        Log.d("NET", "  out_of_range_reason_required: $outOfRangeReasonRequired")

                        val faceVector = body.face_vector?.let { faceVectorString ->
                            FaceVectorUtil.parseFaceVector(faceVectorString)
                        }

                        if (faceVerificationRequired) {
                            Log.d("NET", "  face_vector present: ${body.face_vector != null}")
                            Log.d("NET", "  face_vector parsed: ${faceVector != null}")
                            if (faceVector != null) {
                                Log.d("NET", "  face_vector size: ${faceVector.size}")
                                Log.d("NET", "  face_vector valid: ${FaceVectorUtil.isValidFaceVector(faceVector)}")
                            }
                        }

                        when {
                            faceVerificationRequired -> {
                                CheckInOutcome.RequiresFaceVerification(
                                    tToken = body.t_token,
                                    faceVector = faceVector,
                                    minimumQualityScore = minimumQualityScore,
                                    isLate = isLate,
                                    isOutOfRange = isOutOfRange,
                                    lateReasonRequired = lateReasonRequired,
                                    outOfRangeReasonRequired = outOfRangeReasonRequired,
                                    message = message
                                )
                            }

                            lateReasonRequired || outOfRangeReasonRequired -> {
                                CheckInOutcome.RequiresReasons(
                                    tToken = body.t_token,
                                    isLate = isLate,
                                    isOutOfRange = isOutOfRange,
                                    lateReasonRequired = lateReasonRequired,
                                    outOfRangeReasonRequired = outOfRangeReasonRequired,
                                    message = message
                                )
                            }

                            else -> {
                                CheckInOutcome.RequiresReasons(
                                    tToken = body.t_token,
                                    isLate = false,
                                    isOutOfRange = false,
                                    lateReasonRequired = false,
                                    outOfRangeReasonRequired = false,
                                    message = message
                                )
                            }
                        }
                    } else {
                        CheckInOutcome.Error("Invalid response from server")
                    }
                }

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    CheckInOutcome.Error("Unauthorized. Please login again.")
                }

                else -> CheckInOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            Log.e("NET", "Exception in checkIn", e)
            CheckInOutcome.Error(friendlyNetError(e))
        }
    }

    /**
     * Complete check-in with signature
     */
    suspend fun checkInSignature(
        tToken: String,
        faceRecognitionQualityScore: Float? = null,
        faceVerify: Boolean = false,
        lateReason: String? = null,
        outOfRangeReason: String? = null
    ): SignatureOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("NET", "checkInSignature called:")
            Log.d("NET", "  t_token: ${tToken.take(20)}...")
            Log.d("NET", "  face_recognition_quality_score: $faceRecognitionQualityScore")
            Log.d("NET", "  face_verify: $faceVerify")
            Log.d("NET", "  late_reason: ${lateReason?.take(50)}")
            Log.d("NET", "  out_of_range_reason: ${outOfRangeReason?.take(50)}")

            val res = api.checkInSignature(
                tToken = tToken,
                faceRecognitionQualityScore = faceRecognitionQualityScore,
                faceVerify = faceVerify,
                lateReason = lateReason,
                outOfRangeReason = outOfRangeReason,
                token = token()
            )

            Log.d("NET", "checkInSignature -> code=${res.code()}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()

                    if (body != null) {
                        Log.d("NET", "Check-in signature success: ${body.message}")
                        SignatureOutcome.Success(
                            message = body.message,
                            attendanceRecordId = body.attendance_record_id,
                            checkInTime = body.check_in_time
                        )
                    } else {
                        SignatureOutcome.Error("Invalid response from server")
                    }
                }

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    SignatureOutcome.Error("Unauthorized. Please login again.")
                }

                res.code() == 403 -> {
                    SignatureOutcome.Error(extractError(res))
                }

                res.code() == 400 -> {
                    SignatureOutcome.Error(extractError(res))
                }

                else -> SignatureOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            Log.e("NET", "Exception in checkInSignature", e)
            SignatureOutcome.Error(friendlyNetError(e))
        }
    }

    /**
     * Initial check-out request
     */
    suspend fun checkOut(
        latitude: Double,
        longitude: Double
    ): CheckOutOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("ATTENDANCE", "checkOut() - deviceId: ${deviceId()}, lat: $latitude, lon: $longitude")

            val res = api.checkOut(
                deviceId = deviceId(),
                longitude = longitude,
                latitude = latitude,
                token = token()
            )

            Log.d("NET", "checkOut -> code=${res.code()}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()

                    if (body != null && body.t_token != null) {
                        val faceVerificationRequired = body.face_verification_required ?: false
                        val minimumQualityScore = body.minimum_quality_score ?: 0.57f
                        val workHours = body.work_hours ?: 0f
                        val isEarly = body.is_early ?: false
                        val isOutOfRange = body.is_out_of_range ?: false
                        val earlyReasonRequired = body.early_reason_required ?: false
                        val outOfRangeReasonRequired = body.out_of_range_reason_required ?: false
                        val workReportRequired = body.work_report_require ?: false  // ✅ NEW
                        val message = body.message ?: "Ready for check-out"

                        Log.d("NET", "Check-out initial success:")
                        Log.d("NET", "  face_verification_required: $faceVerificationRequired")
                        Log.d("NET", "  minimum_quality_score: $minimumQualityScore")
                        Log.d("NET", "  work_hours: $workHours")
                        Log.d("NET", "  is_early: $isEarly")
                        Log.d("NET", "  is_out_of_range: $isOutOfRange")
                        Log.d("NET", "  early_reason_required: $earlyReasonRequired")
                        Log.d("NET", "  out_of_range_reason_required: $outOfRangeReasonRequired")
                        Log.d("NET", "  work_report_required: $workReportRequired")  // ✅ NEW

                        val faceVector = body.face_vector?.let { faceVectorString ->
                            FaceVectorUtil.parseFaceVector(faceVectorString)
                        }

                        if (faceVerificationRequired) {
                            Log.d("NET", "  face_vector present: ${body.face_vector != null}")
                            Log.d("NET", "  face_vector parsed: ${faceVector != null}")
                            if (faceVector != null) {
                                Log.d("NET", "  face_vector size: ${faceVector.size}")
                                Log.d("NET", "  face_vector valid: ${FaceVectorUtil.isValidFaceVector(faceVector)}")
                            }
                        }

                        when {
                            faceVerificationRequired -> {
                                CheckOutOutcome.RequiresFaceVerification(
                                    tToken = body.t_token,
                                    faceVector = faceVector,
                                    minimumQualityScore = minimumQualityScore,
                                    workHours = workHours,
                                    isEarly = isEarly,
                                    isOutOfRange = isOutOfRange,
                                    earlyReasonRequired = earlyReasonRequired,
                                    outOfRangeReasonRequired = outOfRangeReasonRequired,
                                    workReportRequired = workReportRequired,  // ✅ NEW
                                    message = message
                                )
                            }

                            earlyReasonRequired || outOfRangeReasonRequired || workReportRequired -> {  // ✅ MODIFIED
                                CheckOutOutcome.RequiresReasons(
                                    tToken = body.t_token,
                                    workHours = workHours,
                                    isEarly = isEarly,
                                    isOutOfRange = isOutOfRange,
                                    earlyReasonRequired = earlyReasonRequired,
                                    outOfRangeReasonRequired = outOfRangeReasonRequired,
                                    workReportRequired = workReportRequired,  // ✅ NEW
                                    message = message
                                )
                            }

                            else -> {
                                CheckOutOutcome.RequiresReasons(
                                    tToken = body.t_token,
                                    workHours = workHours,
                                    isEarly = false,
                                    isOutOfRange = false,
                                    earlyReasonRequired = false,
                                    outOfRangeReasonRequired = false,
                                    workReportRequired = false,  // ✅ NEW
                                    message = message
                                )
                            }
                        }
                    } else {
                        CheckOutOutcome.Error("Invalid response from server")
                    }
                }

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    CheckOutOutcome.Error("Unauthorized. Please login again.")
                }

                res.code() == 400 -> {
                    CheckOutOutcome.Error(extractError(res))
                }

                else -> CheckOutOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            Log.e("NET", "Exception in checkOut", e)
            CheckOutOutcome.Error(friendlyNetError(e))
        }
    }

    /**
     * Complete check-out with signature and optional work report
     */
    suspend fun checkOutSignature(
        tToken: String,
        faceRecognitionQualityScore: Float? = null,
        faceVerify: Boolean = false,
        earlyReason: String? = null,
        outOfRangeReason: String? = null,
        workReport: String? = null,  // ✅ NEW
        workReportFileUri: Uri? = null  // ✅ NEW
    ): SignatureOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("NET", "checkOutSignature called:")
            Log.d("NET", "  t_token: ${tToken.take(20)}...")
            Log.d("NET", "  face_recognition_quality_score: $faceRecognitionQualityScore")
            Log.d("NET", "  face_verify: $faceVerify")
            Log.d("NET", "  early_reason: ${earlyReason?.take(50)}")
            Log.d("NET", "  out_of_range_reason: ${outOfRangeReason?.take(50)}")
            Log.d("NET", "  work_report: ${workReport?.take(50)}")  // ✅ NEW

            // Prepare multipart request bodies
            val tTokenBody = tToken.toRequestBody("text/plain".toMediaTypeOrNull())
            val faceScoreBody = faceRecognitionQualityScore?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val faceVerifyBody = faceVerify.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val earlyReasonBody = earlyReason?.toRequestBody("text/plain".toMediaTypeOrNull())
            val outOfRangeReasonBody = outOfRangeReason?.toRequestBody("text/plain".toMediaTypeOrNull())
            val workReportBody = workReport?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Prepare file part if provided
            var filePart: MultipartBody.Part? = null
            workReportFileUri?.let { uri ->
                try {
                    val contentResolver = context.contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    val fileBytes = inputStream?.readBytes()
                    inputStream?.close()

                    fileBytes?.let { bytes ->
                        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                        val fileName = getFileName(uri) ?: "work_report_${System.currentTimeMillis()}"

                        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                        filePart = MultipartBody.Part.createFormData(
                            "work_report_file",
                            fileName,
                            requestBody
                        )
                        Log.d("NET", "  work_report_file: $fileName (${bytes.size} bytes)")
                    }
                } catch (e: Exception) {
                    Log.e("NET", "Error preparing file: ${e.message}")
                }
            }

            val res = api.checkOutSignature(
                tToken = tTokenBody,
                faceRecognitionQualityScore = faceScoreBody,
                faceVerify = faceVerifyBody,
                earlyReason = earlyReasonBody,
                outOfRangeReason = outOfRangeReasonBody,
                workReport = workReportBody,
                work_report_file = filePart,
                token = token()
            )

            Log.d("NET", "checkOutSignature -> code=${res.code()}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()

                    if (body != null) {
                        Log.d("NET", "Check-out signature success: ${body.message}")
                        Log.d("NET", "  work_hours: ${body.work_hours}")
                        Log.d("NET", "  attendance_status: ${body.attendance_status}")

                        SignatureOutcome.Success(
                            message = body.message,
                            attendanceRecordId = null,
                            checkInTime = body.check_out_time
                        )
                    } else {
                        SignatureOutcome.Error("Invalid response from server")
                    }
                }

                res.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    SignatureOutcome.Error("Unauthorized. Please login again.")
                }

                res.code() == 403 -> {
                    SignatureOutcome.Error(extractError(res))
                }

                res.code() == 400 -> {
                    SignatureOutcome.Error(extractError(res))
                }

                else -> SignatureOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            Log.e("NET", "Exception in checkOutSignature", e)
            SignatureOutcome.Error(friendlyNetError(e))
        }
    }

    // Helper function to get file name from URI
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

    private fun extractError(res: retrofit2.Response<*>): String {
        return try {
            val errorBody = res.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = JSONObject(errorBody)

                // Check for FastAPI validation errors (detail array)
                if (json.has("detail")) {
                    val detail = json.get("detail")

                    // If detail is an array (FastAPI validation errors)
                    if (detail is org.json.JSONArray && detail.length() > 0) {
                        val firstError = detail.getJSONObject(0)
                        val msg = firstError.optString("msg", "")
                        val input = firstError.optString("input", "")

                        // Create a user-friendly message
                        return when {
                            msg.contains("at least 10 characters", ignoreCase = true) ->
                                "Work description must be at least 10 characters"
                            msg.contains("required", ignoreCase = true) -> {
                                val field = firstError.optJSONArray("loc")?.getString(1) ?: "Field"
                                "$field is required"
                            }
                            else -> msg.ifEmpty { "Validation error" }
                        }
                    }

                    // If detail is a string
                    if (detail is String) {
                        return detail
                    }
                }

                // Fallback to message field
                json.optString("message", "Unknown error")
            } else {
                "Server error (${res.code()})"
            }
        } catch (e: Exception) {
            Log.e("NET", "Error parsing error response", e)
            "Server error (${res.code()})"
        }
    }

    private fun friendlyNetError(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException -> "No internet connection"
            is java.net.SocketTimeoutException -> "Request timed out"
            is javax.net.ssl.SSLException -> "Secure connection failed"
            else -> e.message ?: "Network error occurred"
        }
    }
}