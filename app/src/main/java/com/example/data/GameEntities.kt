package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1, // Single-row configuration to persist game progress
    val coins: Int = 100,
    val currentLevel: Int = 1,
    val xp: Int = 0,
    val currentStreak: Int = 1,
    val lastLoginTimestamp: Long = 0L,
    val unlockedTemples: String = "Temple of Ra", // Comma-separated list of unlocked temples
    val unlockedSkins: String = "Classic Explorer", // Comma-separated list of skins
    val equippedSkin: String = "Classic Explorer",
    val soundOn: Boolean = true,
    val musicOn: Boolean = true,
    val totalScarabsTapped: Int = 0,
    val totalGamesPlayed: Int = 0,
    val highXpNeeded: Int = 100, // XP needed for next level
    val selectedLanguage: String = "en",
    val vibrationOn: Boolean = true,
    val difficulty: String = "Normal",
    val onboardingCompleted: Boolean = false,
    val adsRemoved: Boolean = false
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val score: Int,
    val temple: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "missions")
data class Mission(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val targetCount: Int,
    val currentCount: Int,
    val rewardCoins: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val progress: Int = 0,
    val maxProgress: Int = 1,
    val isCompleted: Boolean = false,
    val rewardText: String = "",
    val iconName: String = "" // Material Icon key representing the badge
)
