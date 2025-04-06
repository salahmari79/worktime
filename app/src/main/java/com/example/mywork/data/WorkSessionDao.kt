package com.example.mywork.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface WorkSessionDao {
    @Query("SELECT * FROM work_sessions ORDER BY entryTime DESC")
    fun getAllSessions(): Flow<List<WorkSession>>

    @Query("SELECT * FROM work_sessions WHERE entryTime BETWEEN :startOfDay AND :endOfDay ORDER BY entryTime DESC")
    fun getSessionsForDay(startOfDay: LocalDateTime, endOfDay: LocalDateTime): Flow<List<WorkSession>>

    @Query("SELECT * FROM work_sessions WHERE id = :sessionId")
    fun getSession(sessionId: Long): Flow<WorkSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: WorkSession): Long

    @Update
    fun updateSession(session: WorkSession): Int

    @Query("DELETE FROM work_sessions")
    fun clearAllSessions(): Int

    @Query("UPDATE work_sessions SET plannedExitTime = :newExitTime WHERE id = :sessionId")
    fun updatePlannedExitTime(sessionId: Long, newExitTime: LocalDateTime): Int
} 