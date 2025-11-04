package com.hrms.jeejateamozy.feature.profile.presentation.components

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.core.utils.PreferencesManager
import com.hrms.jeejateamozy.feature.profile.presentation.utils.dialPhoneNumber
import com.hrms.jeejateamozy.feature.profile.presentation.utils.sendEmail

/**
 * Section Card Wrapper
 * Provides consistent styling for all profile sections
 */
@Composable
fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * HR Contact Section
 */
@Composable
fun HRContactSection(
    context: Context,
    prefs: PreferencesManager
) {
    val hasHRInfo = !prefs.hrEmail.isNullOrBlank() || !prefs.companyContact.isNullOrBlank()

    if (hasHRInfo) {
        SectionCard(title = "HR Contact") {
            if (!prefs.companyContact.isNullOrBlank()) {
                ContactItem(
                    icon = Icons.Default.Phone,
                    title = "HR Mobile",
                    value = prefs.companyContact!!,
                    onClick = { dialPhoneNumber(context, prefs.companyContact!!) }
                )
                if (!prefs.hrEmail.isNullOrBlank()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
            if (!prefs.hrEmail.isNullOrBlank()) {
                ContactItem(
                    icon = Icons.Default.Email,
                    title = "HR Email",
                    value = prefs.hrEmail!!,
                    onClick = { sendEmail(context, prefs.hrEmail!!) }
                )
            }
        }
    }
}

/**
 * Face Recognition Section
 */
@Composable
fun FaceRecognitionSection(
    onUpdateFaceClick: () -> Unit
) {
    SectionCard(title = "Face Recognition") {
        ProfileMenuItem(
            icon = Icons.Outlined.Face,
            title = "Update Face",
            subtitle = "Update your face for recognition",
            onClick = onUpdateFaceClick,
            iconTint = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * Account Section
 */
@Composable
fun AccountSection() {
    SectionCard(title = "Account") {
        ProfileMenuItem(
            icon = Icons.Outlined.Person,
            title = "Personal Information",
            subtitle = "View and edit your personal details",
            onClick = { /* TODO */ }
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        ProfileMenuItem(
            icon = Icons.Outlined.Lock,
            title = "Privacy & Security",
            subtitle = "Manage your privacy settings",
            onClick = { /* TODO */ }
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        ProfileMenuItem(
            icon = Icons.Outlined.Notifications,
            title = "Notifications",
            subtitle = "Configure notification preferences",
            onClick = { /* TODO */ }
        )
    }
}

/**
 * Support Section
 */
@Composable
fun SupportSection(
    context: Context,
    prefs: PreferencesManager
) {
    SectionCard(title = "Support") {
        val hasTechnicalInfo = !prefs.technicalSupportNumber.isNullOrBlank() ||
                !prefs.technicalSupportEmail.isNullOrBlank()

        if (hasTechnicalInfo) {
            if (!prefs.technicalSupportNumber.isNullOrBlank()) {
                ContactItem(
                    icon = Icons.Default.Phone,
                    title = "Technical Support",
                    value = prefs.technicalSupportNumber!!,
                    onClick = { dialPhoneNumber(context, prefs.technicalSupportNumber!!) }
                )
                if (!prefs.technicalSupportEmail.isNullOrBlank()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
            if (!prefs.technicalSupportEmail.isNullOrBlank()) {
                ContactItem(
                    icon = Icons.Default.Email,
                    title = "Technical Support",
                    value = prefs.technicalSupportEmail!!,
                    onClick = { sendEmail(context, prefs.technicalSupportEmail!!) }
                )
            }
        }
    }
}