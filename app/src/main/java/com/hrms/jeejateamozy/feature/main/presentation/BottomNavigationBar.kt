package com.hrms.jeejateamozy.feature.main.presentation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hrms.jeejateamozy.core.designsystem.TeamozyColors

enum class NavigationScreen {
    HOME,
    ATTENDANCE,
    PROFILE
}

@Composable
fun BottomNavigationBar(
    currentScreen: NavigationScreen,
    onScreenSelected: (NavigationScreen) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentScreen == NavigationScreen.HOME,
            onClick = { onScreenSelected(NavigationScreen.HOME) },
            icon = {
                Icon(
                    if (currentScreen == NavigationScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TeamozyColors.Primary,
                selectedTextColor = TeamozyColors.Primary,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TeamozyColors.Secondary,
                unselectedTextColor = TeamozyColors.Secondary
            )
        )

        NavigationBarItem(
            selected = currentScreen == NavigationScreen.ATTENDANCE,
            onClick = { onScreenSelected(NavigationScreen.ATTENDANCE) },
            icon = {
                Icon(
                    if (currentScreen == NavigationScreen.ATTENDANCE) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                    contentDescription = "Attendance"
                )
            },
            label = { Text("Attendance") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TeamozyColors.Primary,
                selectedTextColor = TeamozyColors.Primary,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TeamozyColors.Secondary,
                unselectedTextColor = TeamozyColors.Secondary
            )
        )

        NavigationBarItem(
            selected = currentScreen == NavigationScreen.PROFILE,
            onClick = { onScreenSelected(NavigationScreen.PROFILE) },
            icon = {
                Icon(
                    if (currentScreen == NavigationScreen.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                    contentDescription = "Profile"
                )
            },
            label = { Text("Profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TeamozyColors.Primary,
                selectedTextColor = TeamozyColors.Primary,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TeamozyColors.Secondary,
                unselectedTextColor = TeamozyColors.Secondary
            )
        )
    }
}
