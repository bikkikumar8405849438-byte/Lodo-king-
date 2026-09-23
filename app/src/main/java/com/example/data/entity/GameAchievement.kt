package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class GameAchievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val progress: Int = 0,
    val maxProgress: Int = 1,
    val isUnlocked: Boolean = false,
    val rewardCoins: Int = 200,
    val isClaimed: Boolean = false
)
