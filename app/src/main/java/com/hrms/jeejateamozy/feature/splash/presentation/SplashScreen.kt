package com.hrms.jeejateamozy.feature.splash.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrms.jeejateamozy.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    durationMillis: Int = 1800,
    onComplete: () -> Unit
) {
    var start by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "splash-alpha"
    )

    LaunchedEffect(Unit) {
        start = true
        delay(durationMillis.toLong())
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF062E1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_teamozy_logo),
                contentDescription = "Teamozy",
                modifier = Modifier.size(130.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Teamozy",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "HR Management System",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
