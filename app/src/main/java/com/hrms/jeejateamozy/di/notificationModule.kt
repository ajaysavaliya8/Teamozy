package com.hrms.jeejateamozy.di

import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.notification.data.NotificationRepository
import com.hrms.jeejateamozy.feature.notification.presentation.NotificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for Notification feature
 */
val notificationModule = module {

    // Repository - with ApiService for server calls
    single { NotificationRepository.getInstance(androidContext(), NetworkModule.apiService) }

    // ViewModel
    viewModel { NotificationViewModel(get(), PreferencesManager.getInstance(androidContext())) }
}