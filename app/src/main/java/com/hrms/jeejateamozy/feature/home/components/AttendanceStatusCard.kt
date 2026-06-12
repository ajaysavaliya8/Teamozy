package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    val shiftInfo = remember(checkOutTime, elapsedSeconds) {
        calculateShiftInfo(checkOutTime, elapsedSeconds)
    }
    val isOvertime = shiftInfo != null && shiftInfo.remainingSeconds < 0

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Figma colors: check-in = green, check-out = warning/red
    val accentColor = when {
        isCheckInNeeded -> TeamozyColors.Primary
        isCheckOutNeeded && isOvertime -> TeamozyColors.Warning
        isCheckOutNeeded -> TeamozyColors.Error
        else -> TeamozyColors.Primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status label
            Text(
                text = when {
                    isCheckInNeeded -> "Ready to Start Your Day"
                    isCheckOutNeeded -> "Working Time"
                    else -> "Day Complete"
                },
                fontSize = 14.sp,
                color = TeamozyColors.Secondary
            )

            Spacer(Modifier.height(16.dp))

            // Ring + button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                // Outer ring — solid filled circle (Figma: #d7f0e6 ring) for check-in
                // Progress ring for check-out state
                if (isCheckInNeeded) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(TeamozyColors.CheckInRing)
                    )
                } else {
                    val progress = if (isTimerRunning && shiftInfo != null && shiftInfo.totalShiftSeconds > 0) {
                        (elapsedSeconds.toFloat() / shiftInfo.totalShiftSeconds).coerceIn(0f, 1f)
                    } else if (isTimerRunning) {
                        (elapsedSeconds.toFloat() / (8 * 3600)).coerceIn(0f, 1f)
                    } else 0f

                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(160.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        strokeWidth = 10.dp
                    )
                    if (isTimerRunning) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(160.dp),
                            color = accentColor,
                            strokeWidth = 10.dp
                        )
                    }
                }

                // Center punch button
                Button(
                    onClick = if (isCheckOutNeeded) onCheckOutClick else onCheckInClick,
                    modifier = Modifier
                        .size(120.dp)
                        .scale(if (!isLoading && !isFaceVerifyBusy) pulseScale else 1f),
                    enabled = !isLoading && !isFaceVerifyBusy,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TeamozyColors.Primary,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    contentPadding = PaddingValues(0.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isCheckOutNeeded) Icons.Default.Logout else Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
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

            Spacer(Modifier.height(16.dp))

            // Timer / CTA
            val h = elapsedSeconds / 3600
            val m = (elapsedSeconds % 3600) / 60
            val s = elapsedSeconds % 60

            if (isTimerRunning || isCompleted) {
                Text(
                    text = "%02d:%02d:%02d".format(h, m, s),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isTimerRunning) "Time Elapsed" else "Total Hours",
                    fontSize = 13.sp,
                    color = TeamozyColors.Secondary
                )
                if (isTimerRunning && shiftInfo != null) {
                    Spacer(Modifier.height(6.dp))
                    if (isOvertime) {
                        val otH = -shiftInfo.remainingSeconds / 3600
                        val otM = (-shiftInfo.remainingSeconds % 3600) / 60
                        Text("Overtime: ${otH}h ${otM}m", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TeamozyColors.Warning)
                    } else {
                        val rH = shiftInfo.remainingSeconds / 3600
                        val rM = (shiftInfo.remainingSeconds % 3600) / 60
                        Text("${rH}h ${rM}m remaining", fontSize = 13.sp, color = TeamozyColors.Secondary)
                    }
                }
            } else {
                Text(
                    text = "00:00:00",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = TeamozyColors.Primary
                )
                Spacer(Modifier.height(4.dp))
                Text("Tap to start your workday", fontSize = 13.sp, color = TeamozyColors.Secondary)
            }

            if (isLoading || isFaceVerifyBusy) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accentColor)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isFaceVerifyBusy) "Verifying face..." else (loadingMessage ?: "Processing..."),
                        fontSize = 12.sp,
                        color = TeamozyColors.Secondary
                    )
                }
            }
        }
    }
}

private data class ShiftInfo(val totalShiftSeconds: Int, val remainingSeconds: Int)

private fun calculateShiftInfo(checkOutTime: String?, elapsedSeconds: Int): ShiftInfo? {
    if (checkOutTime.isNullOrBlank()) return null
    return try {
        val now = System.currentTimeMillis()
        val formats = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "HH:mm:ss", "HH:mm")
        var checkOutMillis: Long? = null
        for (pattern in formats) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.getDefault())
                fmt.timeZone = TimeZone.getDefault()
                val parsed = fmt.parse(checkOutTime) ?: continue
                checkOutMillis = if (pattern.startsWith("yyyy")) {
                    parsed.time
                } else {
                    val cal = java.util.Calendar.getInstance()
                    val todayCal = java.util.Calendar.getInstance()
                    cal.time = parsed
                    todayCal.set(java.util.Calendar.HOUR_OF_DAY, cal.get(java.util.Calendar.HOUR_OF_DAY))
                    todayCal.set(java.util.Calendar.MINUTE, cal.get(java.util.Calendar.MINUTE))
                    todayCal.set(java.util.Calendar.SECOND, cal.get(java.util.Calendar.SECOND))
                    todayCal.timeInMillis
                }
                break
            } catch (_: Exception) { continue }
        }
        if (checkOutMillis == null) return null
        val remainingSecs = ((checkOutMillis - now) / 1000).toInt()
        val totalShiftSecs = elapsedSeconds + remainingSecs
        ShiftInfo(
            totalShiftSeconds = if (totalShiftSecs > 0) totalShiftSecs else elapsedSeconds,
            remainingSeconds = remainingSecs
        )
    } catch (e: Exception) { null }
}
