package com.hrms.jeejateamozy.feature.profile.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.utils.NetworkErrorHandler
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

/**
 * ✅ UPDATED: Now using NetworkErrorHandler
 */
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
                    val errorMsg = NetworkErrorHandler.extract(response, "Invalid image file")
                    ProfilePictureOutcome.Error(errorMsg)
                }

                else -> {
                    compressedFile.delete()
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to update profile picture")
                    ProfilePictureOutcome.Error(errorMsg)
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
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to remove profile picture")
                    ProfilePictureOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error removing profile picture", e)
            ProfilePictureOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Get contact information
     */
    suspend fun getContactInfo(): ContactInfoOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Fetching contact information")

            val response = api.getContactInfo()

            Log.d("PROFILE", "Get contact info response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Contact info retrieved successfully")
                        ContactInfoOutcome.Success(
                            message = responseBody.message,
                            contactInfo = responseBody.data
                        )
                    } else {
                        ContactInfoOutcome.Error(responseBody?.message ?: "Failed to retrieve contact information")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    ContactInfoOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    ContactInfoOutcome.Error("Contact information not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to retrieve contact information")
                    ContactInfoOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error fetching contact info", e)
            ContactInfoOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Update contact information
     */
    suspend fun updateContactInfo(
        countryCode: Int?,
        alternatePhone: Long?,
        emergencyPhone: Long?,
        whatsappNumber: Long?,
        companyPhone: Long?,
        currentAddress: String?,
        permanentAddress: String?
    ): ContactInfoOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Updating contact information")

            val response = api.updateContactInfo(
                countryCode = countryCode,
                alternatePhoneNumber = alternatePhone,
                emergencyPhoneNumber = emergencyPhone,
                whatsappNumber = whatsappNumber,
                companyPhoneNumber = companyPhone,
                currentAddress = currentAddress,
                permanentAddress = permanentAddress
            )

            Log.d("PROFILE", "Update contact info response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Contact info updated successfully")
                        ContactInfoOutcome.Success(
                            message = responseBody.message,
                            contactInfo = responseBody.data
                        )
                    } else {
                        ContactInfoOutcome.Error(responseBody?.message ?: "Failed to update contact information")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    ContactInfoOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 400 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Invalid contact information")
                    ContactInfoOutcome.Error(errorMsg)
                }

                response.code() == 404 -> {
                    ContactInfoOutcome.Error("Contact information not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to update contact information")
                    ContactInfoOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error updating contact info", e)
            ContactInfoOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    suspend fun getPersonalInfo(): PersonalInfoOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Fetching personal information")

            val response = api.getPersonalInfo()

            Log.d("PROFILE", "Get personal info response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Personal info retrieved successfully")
                        PersonalInfoOutcome.Success(
                            message = responseBody.message,
                            personalInfo = responseBody.data
                        )
                    } else {
                        PersonalInfoOutcome.Error(responseBody?.message ?: "Failed to retrieve personal information")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    PersonalInfoOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    PersonalInfoOutcome.Error("Personal information not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to retrieve personal information")
                    PersonalInfoOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error fetching personal info", e)
            PersonalInfoOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Update personal information
     * Only these fields are editable: blood_group, marital_status, no_of_family_members, languages
     */
    suspend fun updatePersonalInfo(
        bloodGroup: String?,
        maritalStatus: String?,
        noOfFamilyMembers: Int?,
        languages: List<String>?
    ): PersonalInfoOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Updating personal information")

            val response = api.updatePersonalInfo(
                bloodGroup = bloodGroup,
                maritalStatus = maritalStatus,
                noOfFamilyMembers = noOfFamilyMembers,
                languages = languages
            )

            Log.d("PROFILE", "Update personal info response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Personal info updated successfully")
                        PersonalInfoOutcome.Success(
                            message = responseBody.message,
                            personalInfo = responseBody.data
                        )
                    } else {
                        PersonalInfoOutcome.Error(responseBody?.message ?: "Failed to update personal information")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    PersonalInfoOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 400 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Invalid personal information")
                    PersonalInfoOutcome.Error(errorMsg)
                }

                response.code() == 404 -> {
                    PersonalInfoOutcome.Error("Personal information not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to update personal information")
                    PersonalInfoOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error updating personal info", e)
            PersonalInfoOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    suspend fun getEmploymentDetails(): EmploymentDetailOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Fetching employment details")

            val response = api.getEmploymentDetails()

            Log.d("PROFILE", "Get employment details response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Employment details retrieved successfully")
                        EmploymentDetailOutcome.Success(
                            message = responseBody.message,
                            employmentDetail = responseBody.data
                        )
                    } else {
                        EmploymentDetailOutcome.Error(responseBody?.message ?: "Failed to retrieve employment details")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    EmploymentDetailOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    EmploymentDetailOutcome.Error("Employment details not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to retrieve employment details")
                    EmploymentDetailOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error fetching employment details", e)
            EmploymentDetailOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    suspend fun getBankingInfo(): BankingInfoOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Fetching banking information")

            val response = api.getBankingInfo()

            Log.d("PROFILE", "Get banking info response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Banking info retrieved successfully")
                        BankingInfoOutcome.Success(
                            message = responseBody.message,
                            bankingInfo = responseBody.data
                        )
                    } else {
                        BankingInfoOutcome.Error(responseBody?.message ?: "Failed to retrieve banking information")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    BankingInfoOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    BankingInfoOutcome.Error("Banking information not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to retrieve banking information")
                    BankingInfoOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error fetching banking info", e)
            BankingInfoOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Update banking information
     * All 5 fields are editable: account_holder_name, bank_name, bank_account_number, account_type, ifsc_code
     */
    suspend fun updateBankingInfo(
        accountHolderName: String?,
        bankName: String?,
        bankAccountNumber: String?,
        accountType: String?,
        ifscCode: String?
    ): BankingInfoOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Updating banking information")

            val response = api.updateBankingInfo(
                accountHolderName = accountHolderName,
                bankName = bankName,
                bankAccountNumber = bankAccountNumber,
                accountType = accountType,
                ifscCode = ifscCode
            )

            Log.d("PROFILE", "Update banking info response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Banking info updated successfully")
                        BankingInfoOutcome.Success(
                            message = responseBody.message,
                            bankingInfo = responseBody.data
                        )
                    } else {
                        BankingInfoOutcome.Error(responseBody?.message ?: "Failed to update banking information")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    BankingInfoOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 400 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Invalid banking information")
                    BankingInfoOutcome.Error(errorMsg)
                }

                response.code() == 404 -> {
                    BankingInfoOutcome.Error("Banking information not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to update banking information")
                    BankingInfoOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error updating banking info", e)
            BankingInfoOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    suspend fun getEmploymentIdentity(): EmploymentIdentityOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Fetching employment identity information")

            val response = api.getEmploymentIdentity()

            Log.d("PROFILE", "Get employment identity response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Employment identity retrieved successfully")
                        EmploymentIdentityOutcome.Success(
                            message = responseBody.message,
                            identityInfo = responseBody.data
                        )
                    } else {
                        EmploymentIdentityOutcome.Error(responseBody?.message ?: "Failed to retrieve identity information")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    EmploymentIdentityOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 403 -> {
                    EmploymentIdentityOutcome.Error("Only active employees can access identity information")
                }

                response.code() == 404 -> {
                    EmploymentIdentityOutcome.Error("Identity information not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to retrieve identity information")
                    EmploymentIdentityOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error fetching employment identity", e)
            EmploymentIdentityOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    suspend fun getShiftDetails(): ShiftDetailsOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("PROFILE", "Fetching shift details")

            val response = api.getEmploymentShift()

            Log.d("PROFILE", "Get shift details response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("PROFILE", "Shift details retrieved successfully")
                        ShiftDetailsOutcome.Success(
                            message = responseBody.message,
                            shiftDetails = responseBody.data
                        )
                    } else {
                        ShiftDetailsOutcome.Error(responseBody?.message ?: "Failed to retrieve shift details")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    ShiftDetailsOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 403 -> {
                    ShiftDetailsOutcome.Error("Only active employees can access shift information")
                }

                response.code() == 404 -> {
                    ShiftDetailsOutcome.Error("Shift information not found")
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to retrieve shift details")
                    ShiftDetailsOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Error fetching shift details", e)
            ShiftDetailsOutcome.Error(e.message ?: "Network error occurred")
        }
    }

    /**
     * Helper function to compress image
     */
    private fun compressImage(imageUri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                Log.e("PROFILE", "Failed to decode image")
                return null
            }

            // Calculate new dimensions (max 1024px on longest side)
            val maxSize = 1024
            val ratio = if (originalBitmap.width > originalBitmap.height) {
                maxSize.toFloat() / originalBitmap.width
            } else {
                maxSize.toFloat() / originalBitmap.height
            }

            val newWidth = (originalBitmap.width * ratio).toInt()
            val newHeight = (originalBitmap.height * ratio).toInt()

            // Resize bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

            // Save to temporary file
            val tempFile = File(context.cacheDir, "profile_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(tempFile)
            fos.write(outputStream.toByteArray())
            fos.close()

            // Clean up bitmaps
            originalBitmap.recycle()
            resizedBitmap.recycle()

            Log.d("PROFILE", "Image compressed: ${tempFile.length() / 1024}KB")
            tempFile
        } catch (e: Exception) {
            Log.e("PROFILE", "Error compressing image", e)
            null
        }
    }
}