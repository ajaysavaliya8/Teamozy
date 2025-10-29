package com.hrms.jeejateamozy.feature.profile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.core.state.AppStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

sealed class ProfilePictureOutcome {
    data class Success(val message: String, val profileUrl: String? = null) : ProfilePictureOutcome()
    data class Error(val message: String) : ProfilePictureOutcome()
}

class ProfileRepository(private val context: Context) {

    private val api = NetworkModule.apiService
    private val pm = PreferencesManager.getInstance(context)

    /**
     * Update profile picture
     * @param imageUri URI of the selected image
     */
    suspend fun updateProfilePicture(imageUri: Uri): ProfilePictureOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Updating profile picture from URI: $imageUri")

            // Read and compress image
            val compressedFile = compressImage(imageUri)
            if (compressedFile == null) {
                return@withContext ProfilePictureOutcome.Error("Failed to process image")
            }

            // Create multipart body
            val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("profile_image", compressedFile.name, requestFile)

            // Make API call
            val response = api.updateProfilePicture(body)

            Log.d("PROFILE", "Update profile picture response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        // Update PreferencesManager with new profile URL
                        responseBody.profile_url?.let { url ->
                            pm.profileUrl = url
                            Log.d("PROFILE", "Profile URL updated: $url")
                        }

                        // Clean up temporary file
                        compressedFile.delete()

                        ProfilePictureOutcome.Success(
                            message = responseBody.message ?: "Profile picture updated successfully",
                            profileUrl = responseBody.profile_url
                        )
                    } else {
                        compressedFile.delete()
                        ProfilePictureOutcome.Error(responseBody?.message ?: "Failed to update profile picture")
                    }
                }

                response.code() == 401 -> {
                    compressedFile.delete()
                    AppStateManager.emitUnauthorized()
                    ProfilePictureOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 400 -> {
                    compressedFile.delete()
                    val errorMsg = extractErrorMessage(response)
                    ProfilePictureOutcome.Error(errorMsg ?: "Invalid image file")
                }

                else -> {
                    compressedFile.delete()
                    val errorMsg = extractErrorMessage(response)
                    ProfilePictureOutcome.Error(errorMsg ?: "Failed to update profile picture")
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error updating profile picture", e)
            ProfilePictureOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Remove profile picture
     */
    suspend fun removeProfilePicture(): ProfilePictureOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Removing profile picture")

            val response = api.removeProfilePicture()

            Log.d("PROFILE", "Remove profile picture response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        // Clear profile URL from PreferencesManager
                        pm.profileUrl = null
                        Log.d("PROFILE", "Profile URL cleared")

                        ProfilePictureOutcome.Success(
                            message = responseBody.message ?: "Profile picture removed successfully"
                        )
                    } else {
                        ProfilePictureOutcome.Error(responseBody?.message ?: "Failed to remove profile picture")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    ProfilePictureOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    ProfilePictureOutcome.Error("No profile picture found")
                }

                else -> {
                    val errorMsg = extractErrorMessage(response)
                    ProfilePictureOutcome.Error(errorMsg ?: "Failed to remove profile picture")
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error removing profile picture", e)
            ProfilePictureOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Compress image to reduce file size
     * Converts to JPEG and resizes if necessary
     */
    private fun compressImage(imageUri: Uri): File? {
        return try {
            // Read bitmap from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.e("PROFILE", "Failed to decode bitmap from URI")
                return null
            }

            // Resize if too large (max 2048x2048)
            val maxSize = 2048
            if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val ratio = Math.min(
                    maxSize.toFloat() / bitmap.width,
                    maxSize.toFloat() / bitmap.height
                )
                val width = (bitmap.width * ratio).toInt()
                val height = (bitmap.height * ratio).toInt()
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
                Log.d("PROFILE", "Image resized to ${width}x${height}")
            }

            // Rotate if needed based on EXIF data
            bitmap = correctImageOrientation(imageUri, bitmap)

            // Create temporary file
            val tempFile = File(context.cacheDir, "profile_picture_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)

            // Compress to JPEG with 85% quality
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()

            // Recycle bitmap
            bitmap.recycle()

            Log.d("PROFILE", "Image compressed to ${tempFile.length() / 1024}KB")
            tempFile
        } catch (e: Exception) {
            Log.e("PROFILE", "Error compressing image", e)
            null
        }
    }

    /**
     * Correct image orientation based on EXIF data
     */
    private fun correctImageOrientation(imageUri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val exif = androidx.exifinterface.media.ExifInterface(inputStream!!)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            if (!matrix.isIdentity) {
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                return rotatedBitmap
            }

            bitmap
        } catch (e: Exception) {
            Log.e("PROFILE", "Error correcting image orientation", e)
            bitmap
        }
    }

    /**
     * Extract error message from response
     */
    private fun extractErrorMessage(response: retrofit2.Response<*>): String? {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val json = org.json.JSONObject(errorBody)
                json.optString("message", null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}