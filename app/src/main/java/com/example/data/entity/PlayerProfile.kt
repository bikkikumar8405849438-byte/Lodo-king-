package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Royal Player",
    val avatarId: Int = 0,
    val coins: Int = 1500,
    val trophies: Int = 100,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val tokensCaptured: Int = 0,
    val tokensLost: Int = 0,
    val sixesRolled: Int = 0,
    val streakDays: Int = 1,
    val lastDailyRewardTimestamp: Long = 0L,
    val preferredTheme: String = "ROYAL_CLASSIC",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val darkThemeEnabled: Boolean = false
) {
    val winRate: Float
        get() = if (gamesPlayed > 0) (gamesWon.toFloat() / gamesPlayed.toFloat()) * 100f else 0f
}
