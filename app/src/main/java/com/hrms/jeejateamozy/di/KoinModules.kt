package com.hrms.jeejateamozy.di

import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.attendance.data.AttendanceHistoryRepository
import com.hrms.jeejateamozy.feature.auth.data.AuthRepository
import com.hrms.jeejateamozy.feature.attendance.data.AttendanceRepository
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceHistoryViewModel
import com.hrms.jeejateamozy.feature.attendance.presentation.AttendanceViewModel
import com.hrms.jeejateamozy.feature.auth.domain.usecase.LoginUseCase
import com.hrms.jeejateamozy.feature.workreport.data.WorkReportRepository
import com.hrms.jeejateamozy.feature.workreport.domain.usecase.WorkReportUseCase
import com.hrms.jeejateamozy.feature.workreport.presentation.WorkReportViewModel
import com.hrms.jeejateamozy.feature.circular.data.CircularRepository
import com.hrms.jeejateamozy.feature.circular.presentation.CircularViewModel
import com.hrms.jeejateamozy.feature.leave.data.LeaveRepository
import com.hrms.jeejateamozy.feature.leave.presentation.LeaveViewModel
import com.hrms.jeejateamozy.feature.attendance.data.CorrectionRequestRepository
import com.hrms.jeejateamozy.feature.location.data.local.LocationDatabase
import com.hrms.jeejateamozy.feature.location.data.repository.LocationRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val authModule = module {
    // Provide AuthRepository with correct parameter order: (api, publicApi, pm, context)
    single {
        AuthRepository(
            api = NetworkModule.apiService,
            publicApi = NetworkModule.publicApiService,
            pm = PreferencesManager.getInstance(androidContext()),
            ctx = androidContext()
        )
    }

    // Provide LoginUseCase with the repo
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

val circularModule = module {
    single { CircularRepository(androidContext()) }
    viewModel { CircularViewModel(get()) }
}

val leaveModule = module {
    single { LeaveRepository(androidContext()) }
    viewModel { LeaveViewModel(get()) }
}

val attendanceHistoryModule = module {
    single { AttendanceHistoryRepository(androidContext()) }
    single { CorrectionRequestRepository(androidContext()) }
    viewModel { AttendanceHistoryViewModel(get(), get()) }
}

/**
 * Location tracking module
 * Provides LocationDatabase, DAO, and LocationRepository
 */
val locationModule = module {
    // Database
    single { LocationDatabase.getInstance(androidContext()) }

    // DAO
    single { get<LocationDatabase>().pendingLocationDao() }

    // Repository
    single {
        LocationRepository(
            context = androidContext(),
            apiService = NetworkModule.apiService
        )
    }
}