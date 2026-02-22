package com.vakildiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vakildiary.app.data.local.entities.MeetingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity)

    @Query("SELECT * FROM meetings WHERE caseId = :caseId ORDER BY meetingDate ASC")
    fun getMeetingsByCaseId(caseId: String): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE meetingDate BETWEEN :start AND :end ORDER BY meetingDate ASC")
    suspend fun getMeetingsBetween(start: Long, end: Long): List<MeetingEntity>

    @Delete
    suspend fun deleteMeeting(meeting: MeetingEntity)
}
