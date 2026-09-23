package com.example.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val speedX: Float,
    val speedY: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float
)

@Composable
fun ConfettiView(
    modifier: Modifier = Modifier,
    particleCount: Int = 75
) {
    var animStarted by remember { mutableStateOf(false) }

    val progress by animateFloatAsState(
        targetValue = if (animStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 2800, easing = FastOutLinearInEasing),
        label = "confetti_progress"
    )

    val particles = remember {
        val colors = listOf(
            Color(0xFFFFD700), Color(0xFFE53935), Color(0xFF1B9E4B),
            Color(0xFF1E88E5), Color(0xFFFF9800), Color(0xFFAB47BC), Color(0xFF00E5FF)
        )
        List(particleCount) {
            ConfettiParticle(
                initialX = Random.nextFloat(),
                initialY = -0.1f - Random.nextFloat() * 0.2f,
                speedX = (Random.nextFloat() - 0.5f) * 0.4f,
                speedY = 0.8f + Random.nextFloat() * 0.6f,
                color = colors.random(),
                size = 14f + Random.nextFloat() * 16f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f
            )
        }
    }

    LaunchedEffect(Unit) {
        animStarted = true
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            val currentX = (p.initialX + p.speedX * progress) * w
            val currentY = (p.initialY + p.speedY * progress) * h

            if (currentY in 0f..h && currentX in 0f..w) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(currentX, currentY),
                    size = Size(p.size, p.size * 0.5f)
                )
            }
        }
    }
}
