package com.example.ui.board

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PlayerColor

@Composable
fun DiceComposable(
    value: Int?,
    isRolling: Boolean,
    canRoll: Boolean,
    playerColor: PlayerColor,
    onRollClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dice_anim")

    // Rotation when rolling
    val rollingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dice_rotation"
    )

    // Gentle pulse when ready to roll
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dice_pulse"
    )

    val currentScale = if (isRolling) 1.15f else if (canRoll) pulseScale else 1f
    val currentRotation = if (isRolling) rollingRotation else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .scale(currentScale)
                .rotate(currentRotation)
                .shadow(
                    elevation = if (canRoll) 12.dp else 4.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = playerColor.primaryColor,
                    spotColor = playerColor.primaryColor
                )
                .clip(RoundedCornerShape(18.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF0F0F0), Color(0xFFE2E2E2))
                    )
                )
                .border(
                    width = if (canRoll) 3.dp else 1.5.dp,
                    color = if (canRoll) playerColor.primaryColor else Color(0xFFBDBDBD),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(enabled = canRoll && !isRolling) { onRollClick() }
                .testTag("dice_cube")
        ) {
            if (isRolling) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Rolling dice",
                    tint = playerColor.primaryColor,
                    modifier = Modifier.size(40.dp)
                )
            } else {
                DiceFacePips(value = value ?: 6, dotColor = playerColor.primaryColor)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (canRoll && !isRolling) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = playerColor.primaryColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "TAP TO ROLL",
                    color = playerColor.primaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        } else if (value != null && !isRolling) {
            Text(
                text = "Rolled $value",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DiceFacePips(value: Int, dotColor: Color) {
    val coercedVal = value.coerceIn(1, 6)

    Canvas(modifier = Modifier.size(48.dp)) {
        val w = size.width
        val h = size.height
        val r = w * 0.11f

        val left = w * 0.25f
        val center = w * 0.5f
        val right = w * 0.75f
        val top = h * 0.25f
        val middle = h * 0.5f
        val bottom = h * 0.75f

        fun drawDot(x: Float, y: Float) {
            // Draw subtle dot shadow
            drawCircle(
                color = Color(0x33000000),
                radius = r * 1.1f,
                center = Offset(x, y + 1.5f)
            )
            // Draw main pip dot
            drawCircle(
                color = dotColor,
                radius = r,
                center = Offset(x, y)
            )
        }

        when (coercedVal) {
            1 -> {
                drawDot(center, middle)
            }
            2 -> {
                drawDot(left, top)
                drawDot(right, bottom)
            }
            3 -> {
                drawDot(left, top)
                drawDot(center, middle)
                drawDot(right, bottom)
            }
            4 -> {
                drawDot(left, top)
                drawDot(right, top)
                drawDot(left, bottom)
                drawDot(right, bottom)
            }
            5 -> {
                drawDot(left, top)
                drawDot(right, top)
                drawDot(center, middle)
                drawDot(left, bottom)
                drawDot(right, bottom)
            }
            6 -> {
                drawDot(left, top)
                drawDot(right, top)
                drawDot(left, middle)
                drawDot(right, middle)
                drawDot(left, bottom)
                drawDot(right, bottom)
            }
        }
    }
}
