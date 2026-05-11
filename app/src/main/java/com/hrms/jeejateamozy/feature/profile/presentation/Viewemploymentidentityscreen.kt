@file:OptIn(ExperimentalMaterial3Api::class)

package com.hrms.jeejateamozy.feature.profile.presentation

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.hrms.jeejateamozy.core.network.NetworkModule
import com.hrms.jeejateamozy.core.utils.PrivateFileUrl
import com.hrms.jeejateamozy.feature.profile.data.EmploymentIdentityOutcome
import com.hrms.jeejateamozy.feature.profile.data.IdentityDocument
import com.hrms.jeejateamozy.feature.profile.data.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ViewEmploymentIdentityScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val profileRepository = remember { ProfileRepository(context) }
    val scope = rememberCoroutineScope()

    var documents by remember { mutableStateOf<List<IdentityDocument>>(emptyList()) }
    var isFetching by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadingDocId by remember { mutableStateOf<Int?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isFetching = true
        when (val result = profileRepository.getEmploymentIdentity()) {
            is EmploymentIdentityOutcome.Success -> {
                documents = result.identityInfo?.documents ?: emptyList()
                isFetching = false
            }
            is EmploymentIdentityOutcome.Error -> {
                errorMessage = result.message
                isFetching = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Employment Identity", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isFetching -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = errorMessage ?: "Failed to load identity information",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                            .padding(bottom = 80.dp)
                    ) {
                        // Info banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "This information is read-only and managed by HR",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        downloadError?.let { err ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Text(
                                    text = err,
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        if (documents.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No identity documents on record",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            documents.forEach { doc ->
                                IdentityDocumentCard(
                                    doc = doc,
                                    isDownloading = downloadingDocId == doc.id,
                                    onViewDocument = {
                                        val filePath = doc.file_path ?: return@IdentityDocumentCard
                                        val url = PrivateFileUrl.build(NetworkModule.BASE_URL, filePath)
                                            ?: return@IdentityDocumentCard

                                        downloadingDocId = doc.id
                                        downloadError = null

                                        scope.launch {
                                            try {
                                                val bytes = withContext(Dispatchers.IO) {
                                                    val req = okhttp3.Request.Builder().url(url).build()
                                                    NetworkModule.okHttp.newCall(req).execute().use { response ->
                                                        if (!response.isSuccessful) error("HTTP ${response.code}")
                                                        response.body?.bytes() ?: error("Empty response")
                                                    }
                                                }
                                                val ext = when {
                                                    doc.file_type != null -> ".${doc.file_type.lowercase()}"
                                                    filePath.contains('.') -> ".${filePath.substringAfterLast('.')}"
                                                    else -> ".pdf"
                                                }
                                                val tmp = File(context.cacheDir, "doc_${doc.category_system_code ?: "file"}$ext")
                                                tmp.writeBytes(bytes)
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.provider",
                                                    tmp
                                                )
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Open document"))
                                            } catch (e: Exception) {
                                                downloadError = "Failed to open document: ${e.message}"
                                            } finally {
                                                downloadingDocId = null
                                            }
                                        }
                                    }
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentityDocumentCard(
    doc: IdentityDocument,
    isDownloading: Boolean,
    onViewDocument: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = doc.category_name ?: doc.category_system_code ?: "Document",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                doc.verification_status?.let { status ->
                    val (bg, fg) = when (status.uppercase()) {
                        "VERIFIED" -> Color(0xFF10B981) to Color.White
                        "PENDING" -> Color(0xFFF59E0B) to Color.White
                        "REJECTED" -> Color(0xFFEF4444) to Color.White
                        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = bg
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = fg
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            if (!doc.document_number.isNullOrBlank()) {
                DocInfoRow(label = "Document Number", value = doc.document_number)
                Spacer(Modifier.height(8.dp))
            }
            if (!doc.issue_date.isNullOrBlank()) {
                DocInfoRow(label = "Issue Date", value = doc.issue_date)
                Spacer(Modifier.height(8.dp))
            }
            if (!doc.expiry_date.isNullOrBlank()) {
                DocInfoRow(label = "Expiry Date", value = doc.expiry_date)
                Spacer(Modifier.height(8.dp))
            }

            if (doc.file_path != null) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onViewDocument,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Opening...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("View Document", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
