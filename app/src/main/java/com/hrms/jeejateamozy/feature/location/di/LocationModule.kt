package com.hrms.jeejateamozy.feature.location.di

import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.feature.location.data.local.LocationDatabase
import com.hrms.jeejateamozy.feature.location.data.repository.LocationRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for location tracking feature
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