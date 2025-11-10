package com.hrms.jeejateamozy.di

import com.hrms.jeejateamozy.feature.auth.data.AuthRepository
import com.hrms.jeejateamozy.feature.attendance.data.AttendanceRepository
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceViewModel
import com.hrms.jeejateamozy.feature.auth.domain.usecase.LoginUseCase
import com.hrms.jeejateamozy.feature.workreport.data.WorkReportRepository
import com.hrms.jeejateamozy.feature.workreport.domain.usecase.WorkReportUseCase
import com.hrms.jeejateamozy.feature.workreport.presentation.WorkReportViewModel
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
val homeModule = module {
    single { WorkReportRepository(androidContext()) }
    factory { WorkReportUseCase(get()) }
    viewModel { WorkReportViewModel(get()) }
}
