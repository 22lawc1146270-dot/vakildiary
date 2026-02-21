package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.local.dao.MeetingDao
import com.vakildiary.app.data.local.entities.MeetingEntity
import com.vakildiary.app.domain.model.Meeting
import com.vakildiary.app.domain.repository.MeetingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MeetingRepositoryImpl @Inject constructor(
    private val meetingDao: MeetingDao
) : MeetingRepository {
    override suspend fun insertMeeting(meeting: Meeting): Result<Unit> {
        return try {
            meetingDao.insertMeeting(meeting.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to insert meeting", throwable = t)
        }
    }

    override fun getMeetingsByCaseId(caseId: String): Flow<Result<List<Meeting>>> {
        return meetingDao.getMeetingsByCaseId(caseId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override suspend fun deleteMeeting(meeting: Meeting): Result<Unit> {
        return try {
            meetingDao.deleteMeeting(meeting.toEntity())
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(message = "Failed to delete meeting", throwable = t)
        }
    }

    private fun Meeting.toEntity(): MeetingEntity =
        MeetingEntity(
            meetingId = meetingId,
            caseId = caseId,
            clientName = clientName,
            meetingDate = meetingDate,
            location = location,
            agenda = agenda,
            notes = notes,
            reminderMinutesBefore = reminderMinutesBefore,
            createdAt = createdAt
        )

    private fun MeetingEntity.toDomain(): Meeting =
        Meeting(
            meetingId = meetingId,
            caseId = caseId,
            clientName = clientName,
            meetingDate = meetingDate,
            location = location,
            agenda = agenda,
            notes = notes,
            reminderMinutesBefore = reminderMinutesBefore,
            createdAt = createdAt
        )
}
