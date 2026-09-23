package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_history")
data class MatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String,
    val playerCount: Int,
    val userColor: String,
    val resultWon: Boolean,
    val winnerName: String,
    val coinsEarned: Int,
    val turnsCount: Int = 0,
    val tokensCaptured: Int = 0
)
