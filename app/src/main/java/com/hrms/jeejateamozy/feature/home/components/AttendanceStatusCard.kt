package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Redesigned Attendance Status Card Component
 * Modern, centered layout with prominent punch button
 */
@Composable
fun AttendanceStatusCard(
    currentState: String,
    isTimerRunning: Boolean,
    elapsedSeconds: Int,
    isLoading: Boolean,
    isFaceVerifyBusy: Boolean,
    loadingMessage: String? = null,
    checkOutTime: String? = null,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCheckInNeeded = currentState == "CHECK_IN_NEEDED"
    val isCheckOutNeeded = currentState == "CHECK_OUT_NEEDED"
    val isCompleted = !isCheckInNeeded && !isCheckOutNeeded

    // Calculate shift total and remaining seconds from checkOutTime
    val shiftInfo = remember(checkOutTime, elapsedSeconds) {
        calculateShiftInfo(checkOutTime, elapsedSeconds)
    }

    // Determine if overtime
    val isOvertime = shiftInfo != null && shiftInfo.remainingSeconds < 0

    // Pulsing animation for the punch button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Colors based on state
    val primaryColor = when {
        isCheckInNeeded -> Color(0xFF00C896)  // Green for check-in
        isCheckOutNeeded && isOvertime -> Color(0xFFF59E0B)  // Amber for overtime
        isCheckOutNeeded -> Color(0xFFEF4444)  // Red for check-out
        else -> Color(0xFF10B981)  // Green for completed
    }

    val gradientColors = when {
        isCheckInNeeded -> listOf(Color(0xFF00C896), Color(0xFF00A67E))
        isCheckOutNeeded && isOvertime -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        isCheckOutNeeded -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        else -> listOf(Color(0xFF10B981), Color(0xFF059669))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Text
            Text(
                text = when {
                    isCheckInNeeded -> "Ready to Start Your Day"
                    isCheckOutNeeded -> "Working Time"
                    else -> "Day Complete"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Large Circular Timer/Button Area
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Progress calculation
                val progress = if (isTimerRunning && shiftInfo != null && shiftInfo.totalShiftSeconds > 0) {
                    (elapsedSeconds.toFloat() / shiftInfo.totalShiftSeconds).coerceIn(0f, 1f)
                } else if (isTimerRunning) {
                    // Fallback to 8hr if no checkOutTime
                    val workdaySeconds = 8 * 60 * 60
                    (elapsedSeconds.toFloat() / workdaySeconds).coerceIn(0f, 1f)
                } else {
                    0f
                }

                // Background track
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(160.dp),
                    color = primaryColor.copy(alpha = 0.15f),
                    strokeWidth = 10.dp,
                )

                // Progress indicator
                if (isTimerRunning) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(160.dp),
                        color = primaryColor,
                        strokeWidth = 10.dp,
                    )
                }

                // Center Punch Button - always show (supports multiple check-ins)
                Button(
                    onClick = if (isCheckOutNeeded) onCheckOutClick else onCheckInClick,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(if (!isLoading && !isFaceVerifyBusy) pulseScale else 1f)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = primaryColor.copy(alpha = 0.3f),
                            spotColor = primaryColor.copy(alpha = 0.3f)
                        ),
                    enabled = !isLoading && !isFaceVerifyBusy,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(gradientColors),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isCheckOutNeeded) Icons.Default.Logout else Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isCheckOutNeeded) "CHECK OUT" else "CHECK IN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Timer Display
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60

            if (isTimerRunning || isCompleted) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = if (isTimerRunning) "Time Elapsed" else "Total Hours",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Remaining / Overtime indicator
                    if (isTimerRunning && shiftInfo != null) {
                        Spacer(Modifier.height(8.dp))
                        if (isOvertime) {
                            val overtimeSecs = -shiftInfo.remainingSeconds
                            val otH = overtimeSecs / 3600
                            val otM = (overtimeSecs % 3600) / 60
                            Text(
                                text = "Overtime: ${otH}h ${otM}m",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF59E0B)
                            )
                        } else {
                            val remH = shiftInfo.remainingSeconds / 3600
                            val remM = (shiftInfo.remainingSeconds % 3600) / 60
                            Text(
                                text = "${remH}h ${remM}m remaining",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Tap to start your workday",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Loading indicator
            if (isLoading || isFaceVerifyBusy) {
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = primaryColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isFaceVerifyBusy) "Verifying face..." else (loadingMessage ?: "Processing..."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class ShiftInfo(
    val totalShiftSeconds: Int,
    val remainingSeconds: Int
)

/**
 * Parse checkOutTime (HH:mm or HH:mm:ss format) and compute shift info.
 * Returns null if checkOutTime is not available or unparseable.
 */
private fun calculateShiftInfo(checkOutTime: String?, elapsedSeconds: Int): ShiftInfo? {
    if (checkOutTime.isNullOrBlank()) return null

    return try {
        val now = System.currentTimeMillis()

        // Try full datetime first (yyyy-MM-dd HH:mm:ss), then time-only formats
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "HH:mm:ss",
            "HH:mm"
        )

        var checkOutMillis: Long? = null

        for (pattern in formats) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.getDefault())
                fmt.timeZone = TimeZone.getDefault()
                val parsed = fmt.parse(checkOutTime) ?: continue

                if (pattern.startsWith("yyyy")) {
                    // Full datetime — use directly
                    checkOutMillis = parsed.time
                } else {
                    // Time-only — set on today's date
                    val cal = java.util.Calendar.getInstance()
                    val todayCal = java.util.Calendar.getInstance()
                    cal.time = parsed
                    todayCal.set(java.util.Calendar.HOUR_OF_DAY, cal.get(java.util.Calendar.HOUR_OF_DAY))
                    todayCal.set(java.util.Calendar.MINUTE, cal.get(java.util.Calendar.MINUTE))
                    todayCal.set(java.util.Calendar.SECOND, cal.get(java.util.Calendar.SECOND))
                    checkOutMillis = todayCal.timeInMillis
                }
                break
            } catch (_: Exception) {
                continue
            }
        }

        if (checkOutMillis == null) return null

        val remainingSecs = ((checkOutMillis - now) / 1000).toInt()
        val totalShiftSecs = elapsedSeconds + remainingSecs

        ShiftInfo(
            totalShiftSeconds = if (totalShiftSecs > 0) totalShiftSecs else elapsedSeconds,
            remainingSeconds = remainingSecs
        )
    } catch (e: Exception) {
        null
    }
}
