package com.hrms.jeejateamozy.di

import com.hrms.jeejateamozy.feature.notification.data.NotificationRepository
import com.hrms.jeejateamozy.feature.notification.presentation.NotificationViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for Notification feature
 */
val notificationModule = module {

    // Repository
    single { NotificationRepository.getInstance(androidContext()) }

    // ViewModel
    viewModel { NotificationViewModel(get()) }
}