package com.vakildiary.app.domain.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Meeting
import kotlinx.coroutines.flow.Flow

interface MeetingRepository {
    suspend fun insertMeeting(meeting: Meeting): Result<Unit>
    fun getMeetingsByCaseId(caseId: String): Flow<Result<List<Meeting>>>
    suspend fun deleteMeeting(meeting: Meeting): Result<Unit>
}
