package com.hrms.jeejateamozy.feature.circular.data

import android.content.Context
import android.util.Log
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.state.AppStateManager
import com.hrms.jeejateamozy.core.network.Circular
import com.hrms.jeejateamozy.core.network.CircularDetail
import com.hrms.jeejateamozy.core.network.CircularStats
import com.hrms.jeejateamozy.core.network.PaginationInfo
import com.hrms.jeejateamozy.core.utils.NetworkErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class CircularListOutcome {
    data class Success(
        val circulars: List<Circular>,
        val pagination: PaginationInfo
    ) : CircularListOutcome()

    data class Error(val message: String) : CircularListOutcome()
}

sealed class CircularDetailOutcome {
    data class Success(val circular: CircularDetail) : CircularDetailOutcome()
    data class Error(val message: String) : CircularDetailOutcome()
}

sealed class CircularStatsOutcome {
    data class Success(val stats: CircularStats) : CircularStatsOutcome()
    data class Error(val message: String) : CircularStatsOutcome()
}

/**
 * ✅ UPDATED: Now using NetworkErrorHandler
 */
class CircularRepository(private val context: Context) {

    private val api = NetworkModule.apiService

    /**
     * Get list of circulars with pagination and filters
     */
    suspend fun getCirculars(
        status: String? = null,
        circularType: String? = null,
        priority: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): CircularListOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("CIRCULAR", "Fetching circulars - page: $page, pageSize: $pageSize, status: $status, type: $circularType, priority: $priority")

            val response = api.getCirculars(
                status = status,
                circularType = circularType,
                priority = priority,
                page = page,
                pageSize = pageSize
            )

            Log.d("CIRCULAR", "Get circulars response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("CIRCULAR", "Successfully fetched ${responseBody.data.circulars.size} circulars")

                        val circulars = responseBody.data.circulars.map { it.toDomain() }
                        val pagination = responseBody.data.pagination.toDomain()

                        CircularListOutcome.Success(
                            circulars = circulars,
                            pagination = pagination
                        )
                    } else {
                        CircularListOutcome.Error("Failed to fetch circulars")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    CircularListOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Employee not found")
                    CircularListOutcome.Error(errorMsg)
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch circulars")
                    CircularListOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("CIRCULAR", "Error fetching circulars", e)
            CircularListOutcome.Error(
                e.message ?: "Network error occurred"
            )
        }
    }

    /**
     * Get circular detail by ID
     */
    suspend fun getCircularDetail(circularId: Int): CircularDetailOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("CIRCULAR", "Fetching circular detail for ID: $circularId")

            val response = api.getCircularDetail(circularId)

            Log.d("CIRCULAR", "Get circular detail response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("CIRCULAR", "Successfully fetched circular detail")

                        val circularDetail = responseBody.data.toDomain()

                        CircularDetailOutcome.Success(circular = circularDetail)
                    } else {
                        CircularDetailOutcome.Error("Failed to fetch circular detail")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    CircularDetailOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 403 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "You don't have access to this circular")
                    CircularDetailOutcome.Error(errorMsg)
                }

                response.code() == 404 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Circular not found")
                    CircularDetailOutcome.Error(errorMsg)
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch circular detail")
                    CircularDetailOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("CIRCULAR", "Error fetching circular detail", e)
            CircularDetailOutcome.Error(
                e.message ?: "Network error occurred"
            )
        }
    }

    /**
     * Get circular statistics
     */
    suspend fun getCircularStats(): CircularStatsOutcome = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("CIRCULAR", "Fetching circular statistics")

            val response = api.getCircularStats()

            Log.d("CIRCULAR", "Get circular stats response code: ${response.code()}")

            when {
                response.isSuccessful && response.code() == 200 -> {
                    val responseBody = response.body()
                    if (responseBody?.status == "success") {
                        Log.d("CIRCULAR", "Successfully fetched circular stats")

                        val stats = responseBody.data.toDomain()

                        CircularStatsOutcome.Success(stats = stats)
                    } else {
                        CircularStatsOutcome.Error("Failed to fetch statistics")
                    }
                }

                response.code() == 401 -> {
                    AppStateManager.emitUnauthorized()
                    CircularStatsOutcome.Error("Unauthorized. Please login again.")
                }

                response.code() == 404 -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Employee not found")
                    CircularStatsOutcome.Error(errorMsg)
                }

                else -> {
                    val errorMsg = NetworkErrorHandler.extract(response, "Failed to fetch statistics")
                    CircularStatsOutcome.Error(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("CIRCULAR", "Error fetching circular stats", e)
            CircularStatsOutcome.Error(
                e.message ?: "Network error occurred"
            )
        }
    }
}