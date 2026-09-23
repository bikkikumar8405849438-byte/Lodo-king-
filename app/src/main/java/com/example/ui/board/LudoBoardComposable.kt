package com.example.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.LudoBoardCoordinates
import com.example.model.BoardTheme
import com.example.model.GameState
import com.example.model.PlayerColor
import com.example.model.Token
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun LudoBoardComposable(
    gameState: GameState,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = gameState.selectedTheme
    val activePlayer = gameState.activePlayer

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(theme.boardBackground)
            .border(4.dp, Color(0xFFC5A059), RoundedCornerShape(20.dp))
            .testTag("ludo_board_container")
    ) {
        val boardWidth = maxWidth
        val cellWidth = boardWidth / 15f

        // 1. Draw Canvas background, yards, tracks, stars, center
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / 15f
            val stepY = size.height / 15f

            // Draw full grid background
            drawRect(
                color = theme.boardBackground,
                size = size
            )

            // Draw 4 Corner Yards (6x6 cells each)
            drawYard(PlayerColor.GREEN, 0, 0, stepX, stepY, theme)
            drawYard(PlayerColor.YELLOW, 0, 9, stepX, stepY, theme)
            drawYard(PlayerColor.RED, 9, 0, stepX, stepY, theme)
            drawYard(PlayerColor.BLUE, 9, 9, stepX, stepY, theme)

            // Draw Outer Track Cells & Home Columns
            drawTracksAndPaths(stepX, stepY, theme)

            // Draw Safe Star Badges on outer track
            drawSafeStars(stepX, stepY, theme)

            // Draw Center 4-way Triangles (rows 6..8, cols 6..8)
            drawCenterTriangles(stepX, stepY, theme)

            // Outer and inner borders
            drawRect(
                color = theme.gridBorderColor,
                style = Stroke(width = 2.5f)
            )
        }

        // 2. Overlay Interactive Tokens
        val allTokens = remember(gameState.players) {
            gameState.players.flatMap { it.tokens }
        }

        // Group tokens by cell position to calculate clustering offsets
        val tokenClusters = remember(allTokens) {
            val map = mutableMapOf<String, MutableList<Token>>()
            for (t in allTokens) {
                val pos = LudoBoardCoordinates.getTokenBoardPosition(t)
                val key = "${pos.x.toInt()}_${pos.y.toInt()}_${t.step}"
                map.getOrPut(key) { mutableListOf() }.add(t)
            }
            map
        }

        for (token in allTokens) {
            val basePos = LudoBoardCoordinates.getTokenBoardPosition(token)
            val clusterKey = "${basePos.x.toInt()}_${basePos.y.toInt()}_${token.step}"
            val cluster = tokenClusters[clusterKey] ?: listOf(token)
            val clusterIndex = cluster.indexOf(token)
            val totalInCluster = cluster.size

            // Compute offset spread if multiple tokens share the cell
            val (offsetX, offsetY) = if (totalInCluster > 1 && !token.inYard) {
                val angle = (2 * PI * clusterIndex / totalInCluster).toFloat()
                val spreadRadius = (cellWidth * 0.22f).value
                Pair(spreadRadius * cos(angle), spreadRadius * sin(angle))
            } else {
                Pair(0f, 0f)
            }

            val isMovable = activePlayer != null &&
                    activePlayer.color == token.color &&
                    token.id in gameState.movableTokenIds

            val tokenX = (basePos.y * cellWidth.value + cellWidth.value * 0.5f + offsetX - (cellWidth.value * 0.44f)).dp
            val tokenY = (basePos.x * cellWidth.value + cellWidth.value * 0.5f + offsetY - (cellWidth.value * 0.44f)).dp

            TokenComposable(
                token = token,
                isMovable = isMovable,
                onTokenClick = { onTokenClick(token.id) },
                size = (cellWidth.value * 0.88f).dp,
                modifier = Modifier.offset(x = tokenX, y = tokenY)
            )
        }
    }
}

private fun DrawScope.drawYard(
    color: PlayerColor,
    row: Int,
    col: Int,
    stepX: Float,
    stepY: Float,
    theme: BoardTheme
) {
    val x = col * stepX
    val y = row * stepY
    val w = 6 * stepX
    val h = 6 * stepY

    // Yard base color with subtle bevel
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(color.primaryColor, color.darkColor),
            startY = y,
            endY = y + h
        ),
        topLeft = Offset(x, y),
        size = Size(w, h)
    )

    // Inner White Base Pad
    val padInsetX = stepX * 0.75f
    val padInsetY = stepY * 0.75f
    val padW = w - padInsetX * 2
    val padH = h - padInsetY * 2

    drawRoundRect(
        color = theme.cellBackground,
        topLeft = Offset(x + padInsetX, y + padInsetY),
        size = Size(padW, padH),
        cornerRadius = CornerRadius(14f, 14f)
    )

    // 4 Token Circles inside the pad
    val circleRadius = stepX * 0.65f
    val offsets = listOf(
        Offset(x + stepX * 2.0f, y + stepY * 2.0f),
        Offset(x + stepX * 4.0f, y + stepY * 2.0f),
        Offset(x + stepX * 2.0f, y + stepY * 4.0f),
        Offset(x + stepX * 4.0f, y + stepY * 4.0f)
    )

    for (c in offsets) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.lightColor.copy(alpha = 0.5f), color.primaryColor),
                center = c,
                radius = circleRadius
            ),
            radius = circleRadius,
            center = c
        )
        drawCircle(
            color = Color.White,
            radius = circleRadius * 0.8f,
            center = c
        )
        drawCircle(
            color = color.primaryColor.copy(alpha = 0.35f),
            radius = circleRadius * 0.5f,
            center = c
        )
    }

    // Yard border
    drawRect(
        color = theme.gridBorderColor,
        topLeft = Offset(x, y),
        size = Size(w, h),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawTracksAndPaths(stepX: Float, stepY: Float, theme: BoardTheme) {
    // 1. Draw outer cells grid borders and backgrounds
    for (r in 0 until 15) {
        for (c in 0 until 15) {
            val inTopLeftYard = r < 6 && c < 6
            val inTopRightYard = r < 6 && c >= 9
            val inBottomLeftYard = r >= 9 && c < 6
            val inBottomRightYard = r >= 9 && c >= 9
            val inCenter = r in 6..8 && c in 6..8

            if (inTopLeftYard || inTopRightYard || inBottomLeftYard || inBottomRightYard || inCenter) {
                continue
            }

            val x = c * stepX
            val y = r * stepY

            // Check if cell is in a Home Column
            val isGreenHome = c == 7 && r in 1..5
            val isRedHome = r == 7 && c in 1..5
            val isBlueHome = c == 7 && r in 9..13
            val isYellowHome = r == 7 && c in 9..13

            // Check if cell is a starting square
            val isGreenStart = r == 1 && c == 6
            val isRedStart = r == 8 && c == 1
            val isBlueStart = r == 13 && c == 8
            val isYellowStart = r == 6 && c == 13

            val cellColor = when {
                isGreenHome || isGreenStart -> PlayerColor.GREEN.primaryColor
                isRedHome || isRedStart -> PlayerColor.RED.primaryColor
                isBlueHome || isBlueStart -> PlayerColor.BLUE.primaryColor
                isYellowHome || isYellowStart -> PlayerColor.YELLOW.primaryColor
                else -> theme.cellBackground
            }

            drawRect(
                color = cellColor,
                topLeft = Offset(x, y),
                size = Size(stepX, stepY)
            )

            // Cell border
            drawRect(
                color = theme.gridBorderColor,
                topLeft = Offset(x, y),
                size = Size(stepX, stepY),
                style = Stroke(width = 1.2f)
            )

            // Draw directional arrow on starting squares
            if (isGreenStart || isRedStart || isBlueStart || isYellowStart) {
                drawStartArrow(r, c, stepX, stepY)
            }
        }
    }
}

private fun DrawScope.drawStartArrow(r: Int, c: Int, stepX: Float, stepY: Float) {
    val cx = c * stepX + stepX / 2f
    val cy = r * stepY + stepY / 2f
    val arrowSize = stepX * 0.32f

    val path = Path().apply {
        when {
            r == 1 && c == 6 -> { // Green: points down
                moveTo(cx - arrowSize, cy - arrowSize / 2)
                lineTo(cx + arrowSize, cy - arrowSize / 2)
                lineTo(cx, cy + arrowSize)
            }
            r == 8 && c == 1 -> { // Red: points right
                moveTo(cx - arrowSize / 2, cy - arrowSize)
                lineTo(cx - arrowSize / 2, cy + arrowSize)
                lineTo(cx + arrowSize, cy)
            }
            r == 13 && c == 8 -> { // Blue: points up
                moveTo(cx - arrowSize, cy + arrowSize / 2)
                lineTo(cx + arrowSize, cy + arrowSize / 2)
                lineTo(cx, cy - arrowSize)
            }
            else -> { // Yellow: points left
                moveTo(cx + arrowSize / 2, cy - arrowSize)
                lineTo(cx + arrowSize / 2, cy + arrowSize)
                lineTo(cx - arrowSize, cy)
            }
        }
        close()
    }
    drawPath(path, color = Color.White.copy(alpha = 0.9f))
}

private fun DrawScope.drawSafeStars(stepX: Float, stepY: Float, theme: BoardTheme) {
    for (outerIdx in LudoBoardCoordinates.safeCellOuterIndices) {
        val coord = LudoBoardCoordinates.outerTrackCells[outerIdx]
        val cx = coord.col * stepX + stepX / 2f
        val cy = coord.row * stepY + stepY / 2f
        val starRadius = stepX * 0.36f

        drawFivePointStar(
            center = Offset(cx, cy),
            radius = starRadius,
            fillColor = theme.starColor,
            strokeColor = Color(0xFF5D4037)
        )
    }
}

private fun DrawScope.drawFivePointStar(
    center: Offset,
    radius: Float,
    fillColor: Color,
    strokeColor: Color
) {
    val path = Path()
    val innerRadius = radius * 0.42f
    val points = 5

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = (i * PI / points - PI / 2).toFloat()
        val px = center.x + r * cos(angle)
        val py = center.y + r * sin(angle)
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()

    drawPath(path, color = fillColor, style = Fill)
    drawPath(path, color = strokeColor, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawCenterTriangles(stepX: Float, stepY: Float, theme: BoardTheme) {
    val startX = 6 * stepX
    val startY = 6 * stepY
    val endX = 9 * stepX
    val endY = 9 * stepY
    val centerX = 7.5f * stepX
    val centerY = 7.5f * stepY

    // Base background
    drawRect(
        color = theme.centerHubColor,
        topLeft = Offset(startX, startY),
        size = Size(endX - startX, endY - startY)
    )

    // 1. Green Triangle (Top)
    val greenPath = Path().apply {
        moveTo(startX, startY)
        lineTo(endX, startY)
        lineTo(centerX, centerY)
        close()
    }
    drawPath(greenPath, color = PlayerColor.GREEN.primaryColor)

    // 2. Yellow Triangle (Right)
    val yellowPath = Path().apply {
        moveTo(endX, startY)
        lineTo(endX, endY)
        lineTo(centerX, centerY)
        close()
    }
    drawPath(yellowPath, color = PlayerColor.YELLOW.primaryColor)

    // 3. Blue Triangle (Bottom)
    val bluePath = Path().apply {
        moveTo(endX, endY)
        lineTo(startX, endY)
        lineTo(centerX, centerY)
        close()
    }
    drawPath(bluePath, color = PlayerColor.BLUE.primaryColor)

    // 4. Red Triangle (Left)
    val redPath = Path().apply {
        moveTo(startX, endY)
        lineTo(startX, startY)
        lineTo(centerX, centerY)
        close()
    }
    drawPath(redPath, color = PlayerColor.RED.primaryColor)

    // Center Gold Medallion
    drawCircle(
        color = Color(0xFFFFD54F),
        radius = stepX * 0.6f,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = Color(0xFFC5A059),
        radius = stepX * 0.6f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.5f)
    )

    drawFivePointStar(
        center = Offset(centerX, centerY),
        radius = stepX * 0.42f,
        fillColor = Color(0xFFFFB300),
        strokeColor = Color(0xFF5D4037)
    )

    // Center borders
    drawRect(
        color = theme.gridBorderColor,
        topLeft = Offset(startX, startY),
        size = Size(endX - startX, endY - startY),
        style = Stroke(width = 2f)
    )
}
