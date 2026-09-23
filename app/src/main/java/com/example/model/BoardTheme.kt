package com.example.model

import androidx.compose.ui.graphics.Color

enum class BoardTheme(
    val title: String,
    val boardBackground: Color,
    val cellBackground: Color,
    val gridBorderColor: Color,
    val starColor: Color,
    val centerHubColor: Color,
    val isDark: Boolean
) {
    ROYAL_CLASSIC(
        title = "Royal Classic",
        boardBackground = Color(0xFFFBF8EE),
        cellBackground = Color(0xFFFFFFFF),
        gridBorderColor = Color(0xFFD7CCC8),
        starColor = Color(0xFFFFB300),
        centerHubColor = Color(0xFFFFF8E1),
        isDark = false
    ),
    MAHARAJA_WOOD(
        title = "Maharaja Heritage",
        boardBackground = Color(0xFF4E342E),
        cellBackground = Color(0xFF795548),
        gridBorderColor = Color(0xFF3E2723),
        starColor = Color(0xFFFFD54F),
        centerHubColor = Color(0xFF5D4037),
        isDark = true
    ),
    MIDNIGHT_NEON(
        title = "Midnight Neon",
        boardBackground = Color(0xFF101426),
        cellBackground = Color(0xFF1B223E),
        gridBorderColor = Color(0xFF2C386B),
        starColor = Color(0xFF00E5FF),
        centerHubColor = Color(0xFF161C33),
        isDark = true
    ),
    EMERALD_GARDEN(
        title = "Emerald Court",
        boardBackground = Color(0xFFE8F5E9),
        cellBackground = Color(0xFFFFFFFF),
        gridBorderColor = Color(0xFFC8E6C9),
        starColor = Color(0xFFFFA000),
        centerHubColor = Color(0xFFF1F8E9),
        isDark = false
    );
}
