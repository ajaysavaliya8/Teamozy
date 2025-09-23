package com.example.teamozy.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamozy.viewmodel.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage() {
    val viewModel: AttendanceViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val showViolationDialog by viewModel.showViolationDialog.collectAsState()

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Debug log
    LaunchedEffect(uiState) {
        println("DEBUG: HomePage UI state changed: $uiState")
    }

    Scaffold(
        bottomBar = { BottomNavigationBar() },
        containerColor = Color(0xFFF8F9FA)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F9FA))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Jeeja Fashion",
                            color = Color(0xFF1E293B),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF1E293B),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Single Line Greeting
            Text(
                text = "Hello, ajay second",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Grid Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureCard("Attendance", Icons.Default.Person, Modifier.weight(1f))
                    FeatureCard("Salary Overview", Icons.Default.List, Modifier.weight(1f))
                    FeatureCard("Salary Slips", Icons.Default.List, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureCard("Apply Leaves", Icons.Default.Add, Modifier.weight(1f))
                    FeatureCard("YTD Statement", Icons.Default.CheckCircle, Modifier.weight(1f))
                    FeatureCard("Roster Schedule", Icons.Default.AccountCircle, Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.canCheckIn) "Not Punched IN" else "Currently Working",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconButton(
                        onClick = { viewModel.refreshStatus() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Refresh Status",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Timer and Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Timer Circle
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(140.dp)) {
                        val strokeWidth = 10.dp.toPx()
                        val segments = 12
                        val segmentAngle = 360f / segments
                        val gapAngle = 16f

                        repeat(segments) { index ->
                            val startAngle = -90f + index * segmentAngle + gapAngle / 2
                            val sweepAngle = segmentAngle - gapAngle

                            drawArc(
                                color = androidx.compose.ui.graphics.Color(0xFF10B981),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeWidth,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - strokeWidth,
                                    size.height - strokeWidth
                                )
                            )
                        }
                    }

                    Text(
                        text = "00:00:00",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                // Check In Button
                Button(
                    onClick = { viewModel.onAttendanceButtonClick() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .width(170.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.canCheckIn) "Check In" else "Check Out",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Fill remaining space
            Spacer(modifier = Modifier.weight(1f))

            // Message Area at bottom
            if (uiState.errorMessage != null || uiState.successMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.errorMessage != null)
                            Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: uiState.successMessage ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.errorMessage != null)
                            Color(0xFFDC2626) else Color(0xFF059669)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Violation Bottom Sheet
    if (showViolationDialog) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissViolationDialog() },
            sheetState = bottomSheetState,
            containerColor = Color.White,
            dragHandle = {
                Surface(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .size(width = 36.dp, height = 4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = Color(0xFFE2E8F0)
                ) {}
            }
        ) {
            ViolationBottomSheetContent(
                title = viewModel.getViolationTitle(),
                onSubmit = { reason -> viewModel.submitViolation(reason) },
                onDismiss = { viewModel.dismissViolationDialog() },
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
fun ViolationBottomSheetContent(
    title: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    var reason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        // Title
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Please provide a reason for this violation",
            fontSize = 16.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Text Input
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Enter reason") },
            placeholder = { Text("Type your reason here...") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
            minLines = 3,
            enabled = !isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF10B981),
                focusedLabelColor = Color(0xFF10B981),
                cursorColor = Color(0xFF10B981)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel Button
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp,
                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF64748B)
                )
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (reason.isNotBlank()) {
                        onSubmit(reason.trim())
                    }
                },
                enabled = reason.isNotBlank() && !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Submit",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom spacing for safe area
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FeatureCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(75.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun BottomNavigationBar() {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = { },
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Home", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFF6B7280)
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Attendance",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Attendance", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(22.dp)
                )
            },
            label = { Text("Settings", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = Color(0xFF9CA3AF),
                unselectedTextColor = Color(0xFF9CA3AF)
            )
        )
    }
}