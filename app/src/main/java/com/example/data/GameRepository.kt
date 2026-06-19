package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.roundToInt

class GameRepository(private val gameDao: GameDao) {

    val userProgress: Flow<UserProgress?> = gameDao.getUserProgress()
    val leaderboard: Flow<List<LeaderboardEntry>> = gameDao.getLeaderboard()
    val missions: Flow<List<Mission>> = gameDao.getMissions()
    val achievements: Flow<List<Achievement>> = gameDao.getAchievements()

    /**
     * Pre-populates default game configuration if starting for the first time.
     */
    suspend fun initializeDefaultDataIfNeeded() {
        val progress = gameDao.getUserProgressDirect()
        if (progress == null) {
            // New player progress
            val defaultProgress = UserProgress(
                id = 1,
                coins = 150, // Starting bonus
                currentLevel = 1,
                xp = 0,
                currentStreak = 1,
                lastLoginTimestamp = System.currentTimeMillis() - 86400000L, // Yesterday
                unlockedTemples = "Temple of Ra",
                unlockedSkins = "Classic Explorer",
                equippedSkin = "Classic Explorer",
                soundOn = true,
                musicOn = true,
                totalScarabsTapped = 0,
                totalGamesPlayed = 0,
                highXpNeeded = 100
            )
            gameDao.saveUserProgress(defaultProgress)

            // Populate Initial Missions
            val initialMissions = listOf(
                Mission("m1", "Scarab Catcher I", "Tap 25 golden scarabs in total", 25, 0, 50),
                Mission("m2", "Fortune Hunter", "Accumulate 300 coins from challenges", 300, 0, 80),
                Mission("m3", "Temple Explorer", "Complete 5 temple challenge rounds", 5, 0, 100),
                Mission("m4", "Perfect Reflexes", "Tap 15 scarabs in a single game", 15, 0, 60),
                Mission("m5", "Undefeated", "Win a game without hitting any Cursed Black Stones", 1, 0, 120)
            )
            gameDao.saveMissions(initialMissions)

            // Populate Achievements
            val initialAchievements = listOf(
                Achievement("a1", "Novice Archaeologist", "Reach Player Level 3", 0, 3, false, "100 Coins", "stars"),
                Achievement("a2", "Scarab Magnate", "Tap 100 golden scarabs in total", 0, 100, false, "250 Coins", "emoji_events"),
                Achievement("a3", "Tomb Millionaire", "Collect 1,000 total coins in pouch", 0, 1000, false, "Classic Skin Unlock", "account_balance_wallet"),
                Achievement("a4", "Temple Conqueror", "Unlock all 5 Ancient Temples", 1, 5, false, "Pharaoh Crown Skin", "fort"),
                Achievement("a5", "Streak Master", "Reach a 5-day consecutive login streak", 1, 5, false, "Golden Chest", "local_fire_department")
            )
            gameDao.saveAchievements(initialAchievements)
        }
    }

    suspend fun addCoins(amount: Int) {
        val progress = gameDao.getUserProgressDirect() ?: return
        val newCoins = (progress.coins + amount).coerceAtLeast(0)
        gameDao.saveUserProgress(progress.copy(coins = newCoins))
        
        // Progress passive achievements
        incrementAchievementProgress("a3", amount)
    }

    suspend fun updateSoundSettings(sound: Boolean, music: Boolean) {
        val progress = gameDao.getUserProgressDirect() ?: return
        gameDao.saveUserProgress(progress.copy(soundOn = sound, musicOn = music))
    }

    suspend fun submitScore(playerName: String, score: Int, temple: String) {
        val entry = LeaderboardEntry(
            playerName = playerName.ifBlank { "Anonym" },
            score = score,
            temple = temple
        )
        gameDao.insertLeaderboard(entry)
    }

    /**
     * Handles game completion progress (XP, Coins, Stats, levels up)
     */
    suspend fun finishTempleChallenge(
        scarabsTapped: Int,
        coinsEarned: Int,
        levelCompleted: Boolean,
        templeName: String
    ): LevelUpResult {
        val progress = gameDao.getUserProgressDirect() ?: return LevelUpResult(false, 1, 0, false)
        
        val newTotalScarabs = progress.totalScarabsTapped + scarabsTapped
        val newTotalGames = progress.totalGamesPlayed + 1
        
        // XP Formula: earned based on score/scarabs
        val xpGained = scarabsTapped * 5 + 10
        var newXp = progress.xp + xpGained
        var level = progress.currentLevel
        var xpNeeded = progress.highXpNeeded
        var leveledUp = false
        var awardedMysteryChest = false

        while (newXp >= xpNeeded) {
            newXp -= xpNeeded
            level++
            xpNeeded = (xpNeeded * 1.3).roundToInt()
            leveledUp = true
            // Mystery chest awarded every 5 levels
            if (level % 5 == 0) {
                awardedMysteryChest = true
            }
        }

        val updatedProgress = progress.copy(
            coins = progress.coins + coinsEarned,
            currentLevel = level,
            xp = newXp,
            highXpNeeded = xpNeeded,
            totalGamesPlayed = newTotalGames,
            totalScarabsTapped = newTotalScarabs
        )
        gameDao.saveUserProgress(updatedProgress)

        // Update active missions
        updateMissionProgress("m1", scarabsTapped)
        updateMissionProgress("m2", coinsEarned)
        updateMissionProgress("m3", 1)
        updateLimitMissions("m4", scarabsTapped)
        
        // Update achievements
        incrementAchievementProgress("a1", level - progress.currentLevel)
        incrementAchievementProgress("a2", scarabsTapped)
        incrementAchievementProgress("a3", coinsEarned)

        return LevelUpResult(
            leveledUp = leveledUp,
            newLevel = level,
            xpGained = xpGained,
            awardedMysteryChest = awardedMysteryChest
        )
    }

    /**
     * Updates ongoing missions with absolute count caps.
     */
    private suspend fun updateLimitMissions(missionId: String, currentMax: Int) {
        val missions = gameDao.getMissionsDirect()
        val m = missions.find { it.id == missionId } ?: return
        if (!m.isCompleted && currentMax >= m.targetCount) {
            gameDao.updateMission(m.copy(currentCount = currentMax.coerceAtLeast(m.currentCount), isCompleted = true))
        } else if (!m.isCompleted && currentMax > m.currentCount) {
            gameDao.updateMission(m.copy(currentCount = currentMax))
        }
    }

    /**
     * Increments simple incremental missions.
     */
    suspend fun updateMissionProgress(missionId: String, increment: Int) {
        if (increment <= 0) return
        val missions = gameDao.getMissionsDirect()
        val m = missions.find { it.id == missionId } ?: return
        if (m.isCompleted) return

        val newCount = (m.currentCount + increment).coerceAtMost(m.targetCount)
        val completed = newCount >= m.targetCount
        gameDao.updateMission(m.copy(currentCount = newCount, isCompleted = completed))
    }

    suspend fun claimMissionReward(missionId: String): Int {
        val missions = gameDao.getMissionsDirect()
        val m = missions.find { it.id == missionId } ?: return 0
        if (!m.isCompleted || m.isClaimed) return 0
        
        // Update mission
        gameDao.updateMission(m.copy(isClaimed = true))
        
        // Reward coin
        addCoins(m.rewardCoins)
        return m.rewardCoins
    }

    suspend fun incrementAchievementProgress(achievementId: String, increment: Int) {
        if (increment <= 0) return
        val achievements = gameDao.getAchievementsDirect()
        val a = achievements.find { it.id == achievementId } ?: return
        if (a.isCompleted) return

        val newProg = (a.progress + increment).coerceAtMost(a.maxProgress)
        val completed = newProg >= a.maxProgress
        gameDao.updateAchievement(a.copy(progress = newProg, isCompleted = completed))
    }

    suspend fun claimDailyReward(): Int {
        val progress = gameDao.getUserProgressDirect() ?: return 0
        val now = System.currentTimeMillis()
        val diff = now - progress.lastLoginTimestamp
        
        // Check if claimed today already (less than 24h & same calendar day is approximate)
        // If diff < 20 hours, do not claim (avoid duplicate taps). Let's use 20 hours to be safe
        if (diff < 72000000L) {
            return -1 // Too early!
        }

        var streak = progress.currentStreak
        if (diff in 72000000L..172800000L) {
            // Logged in on consecutive day (between 20 hours and 48 hours)
            streak = (streak + 1).coerceAtMost(7)
        } else {
            // Missed a day, reset streak
            streak = 1
        }

        val baseReward = 50
        val streakMultiplier = streak
        val coinsToAward = baseReward * streakMultiplier

        val updated = progress.copy(
            coins = progress.coins + coinsToAward,
            currentStreak = streak,
            lastLoginTimestamp = now
        )
        gameDao.saveUserProgress(updated)

        // Increment cumulative achievements
        incrementAchievementProgress("a5", 1)
        incrementAchievementProgress("a3", coinsToAward)

        return coinsToAward
    }

    suspend fun claimStreakReward(coins: Int) {
        addCoins(coins)
    }

    suspend fun unlockTemple(templeName: String, cost: Int): Boolean {
        val progress = gameDao.getUserProgressDirect() ?: return false
        if (progress.coins < cost) return false

        val currentList = progress.unlockedTemples.split(",").map { it.trim() }.toMutableSet()
        if (currentList.contains(templeName)) return true // already unlocked

        currentList.add(templeName)
        val newListString = currentList.joinToString(", ")
        
        val updated = progress.copy(
            coins = progress.coins - cost,
            unlockedTemples = newListString
        )
        gameDao.saveUserProgress(updated)

        // Check temple achievement
        incrementAchievementProgress("a4", 1)

        return true
    }

    suspend fun unlockSkin(skinName: String, cost: Int): Boolean {
        val progress = gameDao.getUserProgressDirect() ?: return false
        if (progress.coins < cost) return false

        val currentList = progress.unlockedSkins.split(",").map { it.trim() }.toMutableSet()
        if (currentList.contains(skinName)) return true

        currentList.add(skinName)
        val newListString = currentList.joinToString(", ")

        val updated = progress.copy(
            coins = progress.coins - cost,
            unlockedSkins = newListString
        )
        gameDao.saveUserProgress(updated)
        return true
    }

    suspend fun equipSkin(skinName: String): Boolean {
        val progress = gameDao.getUserProgressDirect() ?: return false
        val currentList = progress.unlockedSkins.split(",").map { it.trim() }
        if (!currentList.contains(skinName)) return false

        gameDao.saveUserProgress(progress.copy(equippedSkin = skinName))
        return true
    }

    suspend fun updateSelectedLanguage(lang: String) {
        val progress = gameDao.getUserProgressDirect() ?: return
        gameDao.saveUserProgress(progress.copy(selectedLanguage = lang))
    }

    suspend fun updateVibrationOn(enabled: Boolean) {
        val progress = gameDao.getUserProgressDirect() ?: return
        gameDao.saveUserProgress(progress.copy(vibrationOn = enabled))
    }

    suspend fun updateDifficulty(diff: String) {
        val progress = gameDao.getUserProgressDirect() ?: return
        gameDao.saveUserProgress(progress.copy(difficulty = diff))
    }

    suspend fun completeOnboarding() {
        val progress = gameDao.getUserProgressDirect() ?: return
        gameDao.saveUserProgress(progress.copy(onboardingCompleted = true))
    }

    suspend fun triggerAdsRemoved() {
        val progress = gameDao.getUserProgressDirect() ?: return
        gameDao.saveUserProgress(progress.copy(adsRemoved = true))
    }
}

data class LevelUpResult(
    val leveledUp: Boolean,
    val newLevel: Int,
    val xpGained: Int,
    val awardedMysteryChest: Boolean
)
