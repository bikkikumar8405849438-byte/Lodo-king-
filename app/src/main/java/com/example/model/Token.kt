package com.example.model

data class Token(
    val id: Int, // 0..3
    val color: PlayerColor,
    val step: Int = -1 // -1: in Yard, 0..50: circular track, 51..55: home column, 56: Home/Finish
) {
    val inYard: Boolean get() = step == -1
    val isHome: Boolean get() = step == 56
    val inHomeColumn: Boolean get() = step in 51..55
    val onOuterTrack: Boolean get() = step in 0..50
}
