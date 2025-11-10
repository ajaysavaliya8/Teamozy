package com.hrms.jeejateamozy.feature.workreport.domain.usecase

import android.net.Uri
import com.hrms.jeejateamozy.feature.workreport.data.CreateWorkReportOutcome
import com.hrms.jeejateamozy.feature.workreport.data.WorkReportListOutcome
import com.hrms.jeejateamozy.feature.workreport.data.WorkReportRepository

/**
 * Use cases for Work Report feature
 * Encapsulates business logic and repository calls
 */
class WorkReportUseCase(private val repository: WorkReportRepository) {

    /**
     * Get work reports for a specific month and year
     * @param month Month (1-12)
     * @param year Year
     * @return WorkReportListOutcome
     */
    suspend fun getWorkReports(month: Int, year: Int): WorkReportListOutcome {
        // Validate input
        if (month < 1 || month > 12) {
            return WorkReportListOutcome.Error("Invalid month. Must be between 1 and 12.")
        }

        if (year < 2020 || year > 2100) {
            return WorkReportListOutcome.Error("Invalid year. Must be between 2020 and 2100.")
        }

        return repository.getWorkReports(month, year)
    }

    /**
     * Create a new work report
     * @param workDescription Text description of work
     * @param attachmentUris List of attachment URIs
     * @return CreateWorkReportOutcome
     */
    suspend fun createWorkReport(
        workDescription: String,
        attachmentUris: List<Uri>
    ): CreateWorkReportOutcome {
        // Validate input
        val trimmedDescription = workDescription.trim()

        if (trimmedDescription.isEmpty()) {
            return CreateWorkReportOutcome.Error("Work description cannot be empty")
        }

        if (trimmedDescription.length < 10) {
            return CreateWorkReportOutcome.Error("Work description must be at least 10 characters")
        }

        if (trimmedDescription.length > 5000) {
            return CreateWorkReportOutcome.Error("Work description must not exceed 5000 characters")
        }

        if (attachmentUris.size > 5) {
            return CreateWorkReportOutcome.Error("Maximum 5 attachments allowed")
        }

        return repository.createWorkReport(trimmedDescription, attachmentUris)
    }
}