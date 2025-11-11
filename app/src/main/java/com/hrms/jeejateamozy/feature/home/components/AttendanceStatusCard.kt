package com.hrms.jeejateamozy.feature.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Attendance Status Card Component
 * Displays circular timer and check-in/check-out buttons
 */
@Composable
fun AttendanceStatusCard(
    currentState: String,
    isTimerRunning: Boolean,
    elapsedSeconds: Int,
    isLoading: Boolean,
    isFaceVerifyBusy: Boolean,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Timer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                val workdaySeconds = 8 * 60 * 60
                val progress = if (isTimerRunning) {
                    (elapsedSeconds.toFloat() / workdaySeconds).coerceIn(0f, 1f)
                } else {
                    0f
                }

                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(130.dp),
                    color = if (isTimerRunning) Color(0xFF00C896) else Color(0xFF4DD0B8),
                    strokeWidth = 10.dp,
                    trackColor = Color(0xFFE0F2F1),
                )

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60

                    Text(
                        text = "%02d:%02d:%02d".format(hours, minutes, seconds),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isTimerRunning) {
                            Color(0xFF00C896)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            // Check In/Out Button
            when (currentState) {
                "CHECK_IN_NEEDED" -> {
                    Button(
                        onClick = onCheckInClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = !isLoading && !isFaceVerifyBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C896),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "CHECK IN",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                "CHECK_OUT_NEEDED" -> {
                    Button(
                        onClick = onCheckOutClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = !isLoading && !isFaceVerifyBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF6B6B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "CHECK OUT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    // Completed state or other states
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Attendance Complete",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "See you tomorrow!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}