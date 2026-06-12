package com.hrms.jeejateamozy.feature.circular.presentation
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors
import com.hrms.jeejateamozy.core.network.CircularDetail
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Circular Detail Screen
 * Shows detailed information about a specific circular
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircularDetailScreen(
    viewModel: CircularViewModel,
    circularId: Int,
    onNavigateBack: () -> Unit
) {
    val detailState by viewModel.detailUiState.collectAsState()

    // ✅ FIX: Handle gesture back navigation
    BackHandler {
        onNavigateBack()
    }

    // Load circular detail
    LaunchedEffect(circularId) {
        viewModel.loadCircularDetail(circularId)
    }

    // Reset state when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetDetailState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Circular Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TeamozyColors.AppBar,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TeamozyColors.Background)
                .padding(paddingValues)
        ) {
            when {
                detailState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                detailState.errorMessage != null -> {
                    ErrorState(
                        message = detailState.errorMessage!!,
                        onRetry = { viewModel.loadCircularDetail(circularId) }
                    )
                }

                detailState.circular != null -> {
                    CircularDetailContent(circular = detailState.circular!!)
                }
            }
        }
    }
}

/**
 * Circular Detail Content
 */
@Composable
private fun CircularDetailContent(circular: CircularDetail) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            HeaderSection(circular = circular)
        }

        // Description Section
        item {
            DescriptionSection(description = circular.description)
        }

        // Dates Section
        item {
            DatesSection(circular = circular)
        }

        // Attachments Section
        if (circular.attachments.isNotEmpty()) {
            item {
                AttachmentsSection(attachments = circular.attachments)
            }
        }

        // ✅ REMOVED: Metadata Section (fields no longer in API)
        // circular.approvedBy?.let {
        //     item {
        //         ApprovalSection(circular = circular)
        //     }
        // }

        // ✅ ADDED: Bottom spacing
        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

/**
 * Header Section Component
 */
@Composable
private fun HeaderSection(circular: CircularDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TeamozyColors.Primary.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PriorityBadge(priority = circular.priority)

                // Type Badge
                TypeBadge(type = circular.circularType)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = circular.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TeamozyColors.Heading
            )
        }
    }
}

/**
 * Description Section Component
 */
@Composable
private fun DescriptionSection(description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TeamozyColors.Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Description",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Dates Section Component
 */
@Composable
private fun DatesSection(circular: CircularDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TeamozyColors.Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Important Dates",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Published Date
            circular.publishedDate?.let {
                DateRow(label = "Published", date = it)
            }

            // Effective Date
            circular.effectiveDate?.let {
                Spacer(modifier = Modifier.height(8.dp))
                DateRow(label = "Effective", date = it)
            }

            // Expiry Date
            circular.expiryDate?.let {
                Spacer(modifier = Modifier.height(8.dp))
                DateRow(label = "Expires", date = it)
            }
        }
    }
}

@Composable
private fun DateRow(label: String, date: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = formatDetailDate(date),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * Attachments Section Component
 */
@Composable
private fun AttachmentsSection(attachments: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AttachFile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = TeamozyColors.Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Attachments (${attachments.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            attachments.forEach { attachment ->
                AttachmentItem(attachment = attachment)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AttachmentItem(attachment: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = attachment.substringAfterLast("/"),
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Download,
                contentDescription = "Download",
                modifier = Modifier.size(20.dp),
                tint = TeamozyColors.Primary
            )
        }
    }
}

/**
 * Type Badge Component
 */
@Composable
private fun TypeBadge(type: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = type,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondary
        )
    }
}

/**
 * Priority Badge Component
 */
@Composable
private fun PriorityBadge(priority: String) {
    val (backgroundColor, textColor) = when (priority.uppercase()) {
        "HIGH" -> Pair(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
        "MEDIUM" -> Pair(Color(0xFFF59E0B), Color.White)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = priority.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Error State Component
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Error",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

/**
 * Helper function to format date for detail view
 */
private fun formatDetailDate(dateString: String): String {
    return try {
        // Parse ISO date-time format
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        // Try parsing date only format
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e2: Exception) {
            dateString
        }
    }
}