package com.example.mywork.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "work_sessions")
data class WorkSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryTime: LocalDateTime,
    val exitTime: LocalDateTime? = null,
    val plannedExitTime: LocalDateTime? = null,
    val commuteTimeMinutes: Int,
    val morningAlarmTime: LocalDateTime
) 