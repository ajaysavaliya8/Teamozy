package com.example.teamozy.feature.auth.domain.usecase

import com.example.teamozy.feature.auth.data.AuthOutcome
import com.example.teamozy.feature.auth.data.AuthRepository

class LoginUseCase(private val repo: AuthRepository) {
    suspend fun sendOtp(phone: String): AuthOutcome = repo.sendOtp(phone)

    suspend fun loginWithPassword(phone: String, password: String): AuthOutcome =
        repo.loginWithPassword(phone, password)

    suspend fun loginWithOtp(phone: String, otp: String): AuthOutcome =
        repo.loginWithOtp(phone, otp)

    suspend fun sendChangeDeviceOtp(mobileNumber: String): AuthOutcome =
        repo.sendChangeDeviceOtp(mobileNumber)

    suspend fun requestDeviceChange(mobileNumber: String, otp: String, reason: String = ""): AuthOutcome =
        repo.requestDeviceChange(mobileNumber, otp, reason)
}