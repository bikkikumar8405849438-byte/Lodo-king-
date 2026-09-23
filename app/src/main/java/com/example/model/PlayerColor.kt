package com.example.model

import androidx.compose.ui.graphics.Color

enum class PlayerColor(
    val title: String,
    val startIndex: Int,
    val homeEntryIndex: Int,
    val primaryColor: Color,
    val lightColor: Color,
    val darkColor: Color
) {
    GREEN(
        title = "Green",
        startIndex = 0,
        homeEntryIndex = 51,
        primaryColor = Color(0xFF1B9E4B),
        lightColor = Color(0xFF81C784),
        darkColor = Color(0xFF0F5A2A)
    ),
    RED(
        title = "Red",
        startIndex = 13,
        homeEntryIndex = 12,
        primaryColor = Color(0xFFE53935),
        lightColor = Color(0xFFEF9A9A),
        darkColor = Color(0xFF8E1B1B)
    ),
    BLUE(
        title = "Blue",
        startIndex = 26,
        homeEntryIndex = 25,
        primaryColor = Color(0xFF1E88E5),
        lightColor = Color(0xFF90CAF9),
        darkColor = Color(0xFF0D47A1)
    ),
    YELLOW(
        title = "Yellow",
        startIndex = 39,
        homeEntryIndex = 38,
        primaryColor = Color(0xFFFBC02D),
        lightColor = Color(0xFFFFF59D),
        darkColor = Color(0xFFF57F17)
    );
}
