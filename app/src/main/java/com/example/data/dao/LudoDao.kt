package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.GameAchievement
import com.example.data.entity.MatchHistory
import com.example.data.entity.PlayerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface LudoDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getPlayerProfileDirect(): PlayerProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: PlayerProfile)

    @Query("SELECT * FROM match_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentMatches(): Flow<List<MatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchHistory(match: MatchHistory)

    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<GameAchievement>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsDirect(): List<GameAchievement>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultAchievements(achievements: List<GameAchievement>)

    @Update
    suspend fun updateAchievement(achievement: GameAchievement)
}
