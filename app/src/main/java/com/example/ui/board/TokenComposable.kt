package com.example.ui.board

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.PlayerColor
import com.example.model.Token

@Composable
fun TokenComposable(
    token: Token,
    isMovable: Boolean,
    onTokenClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "token_bounce")

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val currentOffset = if (isMovable) bounceOffset.dp else 0.dp
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset(y = currentOffset)
            .minimumInteractiveComponentSize()
            .clickable(
                enabled = isMovable,
                interactionSource = interactionSource,
                indication = null
            ) {
                onTokenClick()
            }
            .testTag("token_${token.color.name}_${token.id}")
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width / 2f * 0.88f

            // 1. If movable: pulsing glowing outer ring
            if (isMovable) {
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = glowAlpha),
                    radius = radius * 1.35f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = glowAlpha * 0.6f),
                    radius = radius * 1.2f,
                    center = center
                )
            }

            // 2. Drop shadow under token
            drawCircle(
                color = Color(0x55000000),
                radius = radius * 0.95f,
                center = Offset(center.x, center.y + radius * 0.15f)
            )

            // 3. Metallic bevel rim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFD54F), Color(0xFFB78103)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // 4. Token color body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(token.color.lightColor, token.color.primaryColor, token.color.darkColor),
                    center = Offset(center.x - radius * 0.2f, center.y - radius * 0.25f),
                    radius = radius * 0.82f
                ),
                radius = radius * 0.78f,
                center = center
            )

            // 5. Specular highlight jewel dome
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = radius * 0.32f,
                center = Offset(center.x - radius * 0.22f, center.y - radius * 0.25f)
            )

            // 6. Finished token crown symbol
            if (token.isHome) {
                val crownPath = Path().apply {
                    val cx = center.x
                    val cy = center.y
                    val w = radius * 0.7f
                    val h = radius * 0.6f
                    moveTo(cx - w / 2, cy + h / 2)
                    lineTo(cx - w / 2, cy - h / 4)
                    lineTo(cx - w / 4, cy + h / 6)
                    lineTo(cx, cy - h / 2)
                    lineTo(cx + w / 4, cy + h / 6)
                    lineTo(cx + w / 2, cy - h / 4)
                    lineTo(cx + w / 2, cy + h / 2)
                    close()
                }
                drawPath(crownPath, color = Color(0xFFFFD700))
                drawPath(crownPath, color = Color(0xFF5D4037), style = Stroke(width = 1.2f))
            }
        }
    }
}
