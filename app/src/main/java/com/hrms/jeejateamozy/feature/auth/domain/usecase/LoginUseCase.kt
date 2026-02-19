package com.hrms.jeejateamozy.feature.auth.domain.usecase

import com.hrms.jeejateamozy.feature.auth.data.AuthOutcome
import com.hrms.jeejateamozy.feature.auth.data.AuthRepository
import com.hrms.jeejateamozy.feature.auth.data.ResetOutcome

class LoginUseCase(private val repo: AuthRepository) {
    suspend fun sendOtp(phone: String): AuthOutcome = repo.sendOtp(phone)

    suspend fun loginWithPassword(phone: String, password: String): AuthOutcome =
        repo.loginWithPassword(phone, password)

    suspend fun loginWithOtp(phone: String, otp: String): AuthOutcome =
        repo.loginWithOtp(phone, otp)

    suspend fun sendChangeDeviceOtp(mobileNumber: String): AuthOutcome =
        repo.sendChangeDeviceOtp(mobileNumber)

    /**
     * Request device change with OTP verification
     * Note: Method name changed from requestDeviceChange to requestChangeDevice
     * Note: Reason parameter removed as it's not used in the current API implementation
     */
    suspend fun requestChangeDevice(mobileNumber: String, otp: String): AuthOutcome =
        repo.requestChangeDevice(mobileNumber, otp)

    suspend fun forgotPassword(phone: String): AuthOutcome =
        repo.forgotPassword(phone)

    suspend fun verifyResetOtp(phone: String, otp: String): ResetOutcome =
        repo.verifyResetOtp(phone, otp)

    suspend fun resetPassword(resetToken: String, newPassword: String, confirmPassword: String): AuthOutcome =
        repo.resetPassword(resetToken, newPassword, confirmPassword)
}