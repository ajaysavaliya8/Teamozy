package com.example.teamozy.di

import com.example.teamozy.feature.auth.data.AuthRepository
import com.example.teamozy.feature.auth.domain.usecase.LoginUseCase
import com.example.teamozy.feature.attendance.data.AttendanceRepository
import com.example.teamozy.feature.attendance.presentation.AttendanceViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val authModule = module {
    // Provide the repo from Context
    single { AuthRepository(androidContext()) }

    // ✅ Provide LoginUseCase with the repo (matches your compiler error expectation)
    factory { LoginUseCase(get<AuthRepository>()) }
}

val attendanceModule = module {
    single { AttendanceRepository(androidContext()) }
    viewModel { AttendanceViewModel(get()) }
}

val permissionsModule = module { }
val homeModule = module { }
