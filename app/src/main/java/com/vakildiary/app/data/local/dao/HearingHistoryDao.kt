package com.vakildiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vakildiary.app.data.local.entities.HearingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HearingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHearing(hearing: HearingHistoryEntity)

    @Query("SELECT * FROM hearing_history WHERE case_id = :caseId ORDER BY hearingDate DESC")
    fun getHearingsByCaseId(caseId: String): Flow<List<HearingHistoryEntity>>

    @Query("SELECT * FROM hearing_history WHERE hearingId = :hearingId LIMIT 1")
    fun getHearingById(hearingId: String): Flow<HearingHistoryEntity?>

    @Query("SELECT DISTINCT case_id FROM hearing_history WHERE DATE(hearingDate/1000,'unixepoch') = DATE('now')")
    fun getCaseIdsWithHearingToday(): Flow<List<String>>

    @Query(
        "DELETE FROM hearing_history WHERE case_id = :caseId AND DATE(hearingDate/1000,'unixepoch') = DATE('now')"
    )
    suspend fun deleteHearingsForCaseToday(caseId: String)

    @Query("DELETE FROM hearing_history WHERE case_id = :caseId")
    suspend fun deleteHearingsByCaseId(caseId: String)
}
