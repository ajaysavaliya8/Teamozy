package com.hrms.jeejateamozy.feature.attendance.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.network.PendingMessage
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.feature.location.model.LocationData
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
        val isComplete: Boolean?,
        val checkOutTime: String? = null
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
        val message: String,
        val pendingMessage: PendingMessage? = null
    ) : CheckInOutcome()

    data class RequiresReasons(
        val tToken: String,
        val isLate: Boolean,
        val isOutOfRange: Boolean,
        val lateReasonRequired: Boolean,
        val outOfRangeReasonRequired: Boolean,
        val message: String,
        val pendingMessage: PendingMessage? = null
    ) : CheckInOutcome()

    data class Success(val message: String) : CheckInOutcome()
    data class Error(val message: String) : CheckInOutcome()
}

sealed class CheckOutOutcome {
    data class RequiresFaceVerification(
        val tToken: String,
        val faceVector: FloatArray?,
        val minimumQualityScore: Float,
        val workMinutes: Int,
        val isEarly: Boolean,
        val isOutOfRange: Boolean,
        val earlyReasonRequired: Boolean,
        val outOfRangeReasonRequired: Boolean,
        val workReportRequired: Boolean,
        val message: String,
        val pendingMessage: PendingMessage? = null
    ) : CheckOutOutcome()

    data class RequiresReasons(
        val tToken: String,
        val workMinutes: Int,
        val isEarly: Boolean,
        val isOutOfRange: Boolean,
        val earlyReasonRequired: Boolean,
        val outOfRangeReasonRequired: Boolean,
        val workReportRequired: Boolean,
        val message: String,
        val pendingMessage: PendingMessage? = null
    ) : CheckOutOutcome()

    data class Success(val message: String) : CheckOutOutcome()
    data class Error(val message: String) : CheckOutOutcome()
}

sealed class LocationReverifyOutcome {
    data class Success(
        val newTToken: String?,
        val message: String,
        val isOutOfRange: Boolean
    ) : LocationReverifyOutcome()

    data class Error(val message: String) : LocationReverifyOutcome()
}

sealed class SignatureOutcome {
    data class Success(
        val message: String,
        val attendanceRecordId: Int?,
        val checkInTime: String?,
        val checkOutTime: String? = null
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
                            isComplete = data.is_complete,
                            checkOutTime = data.check_out_time
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
                    val data = body?.data

                    if (body != null && data?.t_token != null) {
                        val faceVerificationRequired = data.face_verification_required ?: false
                        val minimumQualityScore = data.minimum_quality_score ?: 0.57f
                        val isLate = data.is_late ?: false
                        val isOutOfRange = data.is_out_of_range
                            ?: data.is_check_in_valid_location?.let { !it }
                            ?: false
                        val lateReasonRequired = data.late_reason_required ?: false
                        val outOfRangeReasonRequired = data.out_of_range_reason_required ?: false
                        val message = body.message ?: "Ready for check-in"
                        val pendingMessage = data.pending_message

                        Log.d("NET", "Check-in initial success:")
                        Log.d("NET", "  face_verification_required: $faceVerificationRequired")
                        Log.d("NET", "  minimum_quality_score: $minimumQualityScore")
                        Log.d("NET", "  is_late: $isLate")
                        Log.d("NET", "  is_out_of_range: $isOutOfRange")
                        Log.d("NET", "  late_reason_required: $lateReasonRequired")
                        Log.d("NET", "  out_of_range_reason_required: $outOfRangeReasonRequired")
                        Log.d("NET", "  pending_message: ${if (pendingMessage != null) "ID=${pendingMessage.id}, Type=${pendingMessage.type}" else "null"}")

                        val faceVector = data.face_vector?.let { faceVectorString ->
                            FaceVectorUtil.parseFaceVector(faceVectorString)
                        }

                        if (faceVerificationRequired) {
                            Log.d("NET", "  face_vector present: ${data.face_vector != null}")
                            Log.d("NET", "  face_vector parsed: ${faceVector != null}")
                            if (faceVector != null) {
                                Log.d("NET", "  face_vector size: ${faceVector.size}")
                            }

                            CheckInOutcome.RequiresFaceVerification(
                                tToken = data.t_token,
                                faceVector = faceVector,
                                minimumQualityScore = minimumQualityScore,
                                isLate = isLate,
                                isOutOfRange = isOutOfRange,
                                lateReasonRequired = lateReasonRequired,
                                outOfRangeReasonRequired = outOfRangeReasonRequired,
                                message = message,
                                pendingMessage = pendingMessage
                            )
                        } else {
                            CheckInOutcome.RequiresReasons(
                                tToken = data.t_token,
                                isLate = isLate,
                                isOutOfRange = isOutOfRange,
                                lateReasonRequired = lateReasonRequired,
                                outOfRangeReasonRequired = outOfRangeReasonRequired,
                                message = message,
                                pendingMessage = pendingMessage
                            )
                        }
                    } else {
                        CheckInOutcome.Error(body?.message ?: "Invalid response from server")
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
     * Complete check-in with signature and first location data
     * NEW: Added firstLocation parameter for initial location tracking point
     */
    suspend fun checkInSignature(
        tToken: String,
        faceRecognitionQualityScore: Float? = null,
        faceVerify: Boolean = false,
        lateReason: String? = null,
        outOfRangeReason: String? = null,
        acknowledgmentNote: String? = null,
        firstLocation: LocationData? = null  // NEW: First location data
    ): SignatureOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("NET", "checkInSignature called:")
            Log.d("NET", "  t_token: ${tToken.take(20)}...")
            Log.d("NET", "  face_recognition_quality_score: $faceRecognitionQualityScore")
            Log.d("NET", "  face_verify: $faceVerify")
            Log.d("NET", "  late_reason: ${lateReason?.take(50)}")
            Log.d("NET", "  out_of_range_reason: ${outOfRangeReason?.take(50)}")
            Log.d("NET", "  acknowledgment_note: ${acknowledgmentNote?.take(50)}")
            Log.d("NET", "  first_location: ${if (firstLocation != null) "lat=${firstLocation.latitude}, lng=${firstLocation.longitude}" else "null"}")

            val res = api.checkInSignature(
                tToken = tToken,
                faceRecognitionQualityScore = faceRecognitionQualityScore,
                faceVerify = faceVerify,
                lateReason = lateReason,
                outOfRangeReason = outOfRangeReason,
                acknowledgmentNote = acknowledgmentNote,
                // NEW: First location tracking data
                firstLocationRecordedAt = firstLocation?.recordedAt,
                firstLocationLatitude = firstLocation?.latitude,
                firstLocationLongitude = firstLocation?.longitude,
                firstLocationAccuracy = firstLocation?.locationAccuracy,
                firstLocationAltitude = firstLocation?.altitude,
                firstLocationVerticalAccuracy = firstLocation?.verticalAccuracy,
                firstLocationSpeed = firstLocation?.speed,
                firstLocationHeading = firstLocation?.heading,
                firstLocationAppVersion = firstLocation?.appVersion,
                firstLocationNetworkType = firstLocation?.networkType,
                firstLocationWifiName = firstLocation?.wifiName,
                firstLocationWifiMacAddress = firstLocation?.wifiMacAddress,
                firstLocationBatteryLevel = firstLocation?.batteryLevel,
                token = token()
            )

            Log.d("NET", "checkInSignature -> code=${res.code()}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()

                    if (body != null && body.success) {
                        Log.d("NET", "Check-in signature success: ${body.message}")
                        SignatureOutcome.Success(
                            message = body.message,
                            attendanceRecordId = body.data?.attendance_record_id,
                            checkInTime = body.data?.check_in_time,
                            checkOutTime = body.data?.check_out_time
                        )
                    } else {
                        SignatureOutcome.Error(body?.message ?: "Invalid response from server")
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
                    val data = body?.data

                    if (body != null && data?.t_token != null) {
                        val faceVerificationRequired = data.face_verification_required ?: false
                        val minimumQualityScore = data.minimum_quality_score ?: 0.57f
                        val workMinutes = data.work_minutes ?: 0
                        val isEarly = data.is_early ?: false
                        val isOutOfRange = data.is_out_of_range
                            ?: data.is_check_out_valid_location?.let { !it }
                            ?: false
                        val earlyReasonRequired = data.early_reason_required ?: false
                        val outOfRangeReasonRequired = data.out_of_range_reason_required ?: false
                        val workReportRequired = data.work_report_require ?: false
                        val message = body.message ?: "Ready for check-out"
                        val pendingMessage = data.pending_message

                        Log.d("NET", "Check-out initial success:")
                        Log.d("NET", "  face_verification_required: $faceVerificationRequired")
                        Log.d("NET", "  minimum_quality_score: $minimumQualityScore")
                        Log.d("NET", "  work_minutes: $workMinutes")
                        Log.d("NET", "  is_early: $isEarly")
                        Log.d("NET", "  is_out_of_range: $isOutOfRange")
                        Log.d("NET", "  early_reason_required: $earlyReasonRequired")
                        Log.d("NET", "  out_of_range_reason_required: $outOfRangeReasonRequired")
                        Log.d("NET", "  work_report_require: $workReportRequired")
                        Log.d("NET", "  pending_message: ${if (pendingMessage != null) "ID=${pendingMessage.id}, Type=${pendingMessage.type}" else "null"}")

                        val faceVector = data.face_vector?.let { faceVectorString ->
                            FaceVectorUtil.parseFaceVector(faceVectorString)
                        }

                        if (faceVerificationRequired) {
                            Log.d("NET", "  face_vector present: ${data.face_vector != null}")
                            Log.d("NET", "  face_vector parsed: ${faceVector != null}")

                            CheckOutOutcome.RequiresFaceVerification(
                                tToken = data.t_token,
                                faceVector = faceVector,
                                minimumQualityScore = minimumQualityScore,
                                workMinutes = workMinutes,
                                isEarly = isEarly,
                                isOutOfRange = isOutOfRange,
                                earlyReasonRequired = earlyReasonRequired,
                                outOfRangeReasonRequired = outOfRangeReasonRequired,
                                workReportRequired = workReportRequired,
                                message = message,
                                pendingMessage = pendingMessage
                            )
                        } else {
                            CheckOutOutcome.RequiresReasons(
                                tToken = data.t_token,
                                workMinutes = workMinutes,
                                isEarly = isEarly,
                                isOutOfRange = isOutOfRange,
                                earlyReasonRequired = earlyReasonRequired,
                                outOfRangeReasonRequired = outOfRangeReasonRequired,
                                workReportRequired = workReportRequired,
                                message = message,
                                pendingMessage = pendingMessage
                            )
                        }
                    } else {
                        CheckOutOutcome.Error(body?.message ?: "Invalid response from server")
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
     * Complete check-out with signature, optional work report, and last location data
     * NEW: Added lastLocation parameter for final location tracking point
     */
    suspend fun checkOutSignature(
        tToken: String,
        faceRecognitionQualityScore: Float? = null,
        faceVerify: Boolean = false,
        earlyReason: String? = null,
        outOfRangeReason: String? = null,
        workReport: String? = null,
        workReportFileUri: Uri? = null,
        lastLocation: LocationData? = null,
        acknowledgmentNote: String? = null
    ): SignatureOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("NET", "checkOutSignature called:")
            Log.d("NET", "  t_token: ${tToken.take(20)}...")
            Log.d("NET", "  face_recognition_quality_score: $faceRecognitionQualityScore")
            Log.d("NET", "  face_verify: $faceVerify")
            Log.d("NET", "  early_reason: ${earlyReason?.take(50)}")
            Log.d("NET", "  out_of_range_reason: ${outOfRangeReason?.take(50)}")
            Log.d("NET", "  work_report: ${workReport?.take(50)}")
            Log.d("NET", "  last_location: ${if (lastLocation != null) "lat=${lastLocation.latitude}, lng=${lastLocation.longitude}" else "null"}")
            Log.d("NET", "  acknowledgment_note: ${acknowledgmentNote?.take(50)}")

            // Prepare multipart request bodies
            val tTokenBody = tToken.toRequestBody("text/plain".toMediaTypeOrNull())
            val faceScoreBody = faceRecognitionQualityScore?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val faceVerifyBody = faceVerify.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val earlyReasonBody = earlyReason?.toRequestBody("text/plain".toMediaTypeOrNull())
            val outOfRangeReasonBody = outOfRangeReason?.toRequestBody("text/plain".toMediaTypeOrNull())
            val workReportBody = workReport?.toRequestBody("text/plain".toMediaTypeOrNull())

            // NEW: Prepare last location request bodies
            val lastLocationRecordedAtBody = lastLocation?.recordedAt?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationLatitudeBody = lastLocation?.latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationLongitudeBody = lastLocation?.longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationAccuracyBody = lastLocation?.locationAccuracy?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationAltitudeBody = lastLocation?.altitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationVerticalAccuracyBody = lastLocation?.verticalAccuracy?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationSpeedBody = lastLocation?.speed?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationHeadingBody = lastLocation?.heading?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationAppVersionBody = lastLocation?.appVersion?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationNetworkTypeBody = lastLocation?.networkType?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationWifiNameBody = lastLocation?.wifiName?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationWifiMacAddressBody = lastLocation?.wifiMacAddress?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lastLocationBatteryLevelBody = lastLocation?.batteryLevel?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val acknowledgmentNoteBody = acknowledgmentNote?.toRequestBody("text/plain".toMediaTypeOrNull())

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
                        val fileName = getFileName(uri) ?: "work_report_attachment"

                        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                        filePart = MultipartBody.Part.createFormData(
                            "work_report_file",
                            fileName,
                            requestBody
                        )
                        Log.d("NET", "  work_report_file prepared: $fileName (${bytes.size} bytes)")
                    }
                } catch (e: Exception) {
                    Log.e("NET", "Error preparing work report file", e)
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
                // NEW: Last location tracking data
                lastLocationRecordedAt = lastLocationRecordedAtBody,
                lastLocationLatitude = lastLocationLatitudeBody,
                lastLocationLongitude = lastLocationLongitudeBody,
                lastLocationAccuracy = lastLocationAccuracyBody,
                lastLocationAltitude = lastLocationAltitudeBody,
                lastLocationVerticalAccuracy = lastLocationVerticalAccuracyBody,
                lastLocationSpeed = lastLocationSpeedBody,
                lastLocationHeading = lastLocationHeadingBody,
                lastLocationAppVersion = lastLocationAppVersionBody,
                lastLocationNetworkType = lastLocationNetworkTypeBody,
                lastLocationWifiName = lastLocationWifiNameBody,
                lastLocationWifiMacAddress = lastLocationWifiMacAddressBody,
                lastLocationBatteryLevel = lastLocationBatteryLevelBody,
                acknowledgmentNote = acknowledgmentNoteBody,
                token = token()
            )

            Log.d("NET", "checkOutSignature -> code=${res.code()}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()

                    if (body != null && body.success) {
                        Log.d("NET", "Check-out signature success: ${body.message}")
                        SignatureOutcome.Success(
                            message = body.message,
                            attendanceRecordId = body.data?.attendance_record_id,
                            checkInTime = body.data?.check_out_time
                        )
                    } else {
                        SignatureOutcome.Error(body?.message ?: "Invalid response from server")
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

    /**
     * Re-verify location for check-in when out of range
     */
    suspend fun locationReverify(
        tToken: String,
        latitude: Double,
        longitude: Double
    ): LocationReverifyOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("NET", "locationReverify called: t_token=${tToken.take(20)}..., lat=$latitude, lng=$longitude")

            val res = api.checkInLocationReverify(
                tToken = tToken,
                latitude = latitude,
                longitude = longitude,
                token = token()
            )

            Log.d("NET", "locationReverify -> code=${res.code()}")

            when {
                res.isSuccessful && res.code() == 200 -> {
                    val body = res.body()
                    if (body != null && body.success) {
                        Log.d("NET", "Location reverify success: ${body.message}")
                        LocationReverifyOutcome.Success(
                            newTToken = body.data?.t_token,
                            message = body.message ?: "Location verified",
                            isOutOfRange = body.data?.is_out_of_range ?: false
                        )
                    } else {
                        LocationReverifyOutcome.Error(body?.message ?: "Location reverification failed")
                    }
                }

                res.code() == 401 -> {
                    LocationReverifyOutcome.Error(extractError(res))
                }

                else -> LocationReverifyOutcome.Error(extractError(res))
            }
        } catch (e: Exception) {
            Log.e("NET", "Exception in locationReverify", e)
            LocationReverifyOutcome.Error(friendlyNetError(e))
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun extractError(res: retrofit2.Response<*>): String {
        return try {
            val errorBody = res.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = JSONObject(errorBody)
                json.optString("message", "Unknown error")
            } else {
                "Server error: ${res.code()}"
            }
        } catch (e: Exception) {
            "Server error: ${res.code()}"
        }
    }

    private fun friendlyNetError(e: Exception): String {
        return when {
            e.message?.contains("Unable to resolve host") == true ||
                    e.message?.contains("Failed to connect") == true -> "No internet connection"
            e.message?.contains("timeout") == true -> "Connection timeout. Please try again"
            else -> "Network error. Please try again"
        }
    }
}