package com.example.teamozy.splash.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    durationMillis: Int = 1500,
    onComplete: () -> Unit
) {
    var start by remember { mutableStateOf(false) }
    val alpha = animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis),
        label = "splash-alpha"
    )

    LaunchedEffect(Unit) {
        start = true
        delay(durationMillis.toLong())
        onComplete()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(110.dp).alpha(alpha.value)) {
                    Text("T", fontSize = 72.sp, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Teamozy",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.value)
                )
            }
        }
    }
}
