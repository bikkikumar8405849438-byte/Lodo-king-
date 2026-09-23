package com.example.data.repository

import com.example.data.dao.LudoDao
import com.example.data.entity.GameAchievement
import com.example.data.entity.MatchHistory
import com.example.data.entity.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class LudoRepository(private val dao: LudoDao) {

    val playerProfile: Flow<PlayerProfile?> = dao.getPlayerProfile()
    val recentMatches: Flow<List<MatchHistory>> = dao.getRecentMatches()
    val achievements: Flow<List<GameAchievement>> = dao.getAllAchievements()

    suspend fun initializeDefaultsIfEmpty() {
        val existing = dao.getPlayerProfileDirect()
        if (existing == null) {
            dao.insertOrUpdateProfile(PlayerProfile())
        }

        val existingAchievements = dao.getAllAchievementsDirect()
        if (existingAchievements.isEmpty()) {
            val defaults = listOf(
                GameAchievement("first_win", "Royal Debut", "Win your first Ludo match", "🏆", 0, 1, false, 250),
                GameAchievement("captures_5", "Token Hunter", "Capture 5 opponent tokens", "⚔️", 0, 5, false, 300),
                GameAchievement("sixes_10", "Lucky Sixes", "Roll a 6 ten times in games", "🎲", 0, 10, false, 200),
                GameAchievement("win_3_streak", "Grand Monarch", "Win 3 matches in total", "👑", 0, 3, false, 500),
                GameAchievement("rich_5000", "Treasury Gold", "Accumulate 3,000 coins", "💰", 0, 3000, false, 400),
                GameAchievement("online_warrior", "Room Champ", "Complete an online private room game", "🌐", 0, 1, false, 350)
            )
            dao.insertDefaultAchievements(defaults)
        }
    }

    suspend fun updateProfile(profile: PlayerProfile) {
        dao.insertOrUpdateProfile(profile)
    }

    suspend fun recordMatch(
        mode: String,
        playerCount: Int,
        userColor: String,
        resultWon: Boolean,
        winnerName: String,
        coinsDelta: Int,
        turnsCount: Int,
        tokensCapturedInMatch: Int,
        sixesRolledInMatch: Int
    ) {
        val match = MatchHistory(
            mode = mode,
            playerCount = playerCount,
            userColor = userColor,
            resultWon = resultWon,
            winnerName = winnerName,
            coinsEarned = coinsDelta,
            turnsCount = turnsCount,
            tokensCaptured = tokensCapturedInMatch
        )
        dao.insertMatchHistory(match)

        val currentProfile = dao.getPlayerProfileDirect() ?: PlayerProfile()
        val newCoins = (currentProfile.coins + coinsDelta).coerceAtLeast(0)
        val newTrophies = (currentProfile.trophies + if (resultWon) 25 else -10).coerceAtLeast(0)
        val updatedProfile = currentProfile.copy(
            coins = newCoins,
            trophies = newTrophies,
            gamesPlayed = currentProfile.gamesPlayed + 1,
            gamesWon = currentProfile.gamesWon + (if (resultWon) 1 else 0),
            tokensCaptured = currentProfile.tokensCaptured + tokensCapturedInMatch,
            sixesRolled = currentProfile.sixesRolled + sixesRolledInMatch
        )
        dao.insertOrUpdateProfile(updatedProfile)

        // Update achievements progress
        val allAch = dao.getAllAchievementsDirect()
        allAch.forEach { ach ->
            var newProg = ach.progress
            when (ach.id) {
                "first_win" -> if (resultWon) newProg = 1
                "captures_5" -> newProg = (ach.progress + tokensCapturedInMatch).coerceAtMost(ach.maxProgress)
                "sixes_10" -> newProg = (ach.progress + sixesRolledInMatch).coerceAtMost(ach.maxProgress)
                "win_3_streak" -> if (resultWon) newProg = (ach.progress + 1).coerceAtMost(ach.maxProgress)
                "rich_5000" -> newProg = newCoins.coerceAtMost(ach.maxProgress)
                "online_warrior" -> if (mode.contains("ONLINE", ignoreCase = true)) newProg = 1
            }
            val unlocked = newProg >= ach.maxProgress
            if (newProg != ach.progress || unlocked != ach.isUnlocked) {
                dao.updateAchievement(ach.copy(progress = newProg, isUnlocked = unlocked))
            }
        }
    }

    suspend fun claimDailyReward(): Int {
        val profile = dao.getPlayerProfileDirect() ?: return 0
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        if (now - profile.lastDailyRewardTimestamp < oneDayMillis) {
            return 0 // Already claimed today
        }
        val nextStreak = if (now - profile.lastDailyRewardTimestamp < 2 * oneDayMillis) {
            (profile.streakDays % 7) + 1
        } else {
            1
        }
        val rewardAmount = 150 * nextStreak
        dao.insertOrUpdateProfile(
            profile.copy(
                coins = profile.coins + rewardAmount,
                streakDays = nextStreak,
                lastDailyRewardTimestamp = now
            )
        )
        return rewardAmount
    }

    suspend fun claimAchievement(achievementId: String): Int {
        val list = dao.getAllAchievementsDirect()
        val ach = list.find { it.id == achievementId } ?: return 0
        if (ach.isUnlocked && !ach.isClaimed) {
            val profile = dao.getPlayerProfileDirect() ?: return 0
            dao.updateAchievement(ach.copy(isClaimed = true))
            dao.insertOrUpdateProfile(profile.copy(coins = profile.coins + ach.rewardCoins))
            return ach.rewardCoins
        }
        return 0
    }
}
