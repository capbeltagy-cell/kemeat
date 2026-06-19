package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    // User Progress
    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgress(): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getUserProgressDirect(): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProgress(progress: UserProgress)

    // Leaderboard
    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 50")
    fun getLeaderboard(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboard(entry: LeaderboardEntry)

    // Missions
    @Query("SELECT * FROM missions")
    fun getMissions(): Flow<List<Mission>>

    @Query("SELECT * FROM missions")
    suspend fun getMissionsDirect(): List<Mission>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMissions(missions: List<Mission>)

    @Update
    suspend fun updateMission(mission: Mission)

    // Achievements
    @Query("SELECT * FROM achievements")
    fun getAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievementsDirect(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)
}
