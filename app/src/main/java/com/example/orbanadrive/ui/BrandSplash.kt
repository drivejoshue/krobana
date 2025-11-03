package com.example.orbanadrive.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.orbanadrive.R
import kotlinx.coroutines.delay

@Composable
fun BrandSplash(
    durationMs: Long = 1000,
    onFinished: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "spin")
    val angle = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    LaunchedEffect(Unit) {
        delay(durationMs)
        onFinished()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Usamos el ic_launcher del mipmap
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher),
            contentDescription = "Orbana",
            modifier = Modifier.size(120.dp).rotate(angle.value)
        )
    }
}
