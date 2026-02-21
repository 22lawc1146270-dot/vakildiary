package com.vakildiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vakildiary.app.data.local.entities.CaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(caseEntity: CaseEntity)

    @Update
    suspend fun updateCase(caseEntity: CaseEntity)

    @Query("SELECT * FROM cases WHERE caseId = :caseId LIMIT 1")
    fun getCaseById(caseId: String): Flow<CaseEntity?>

    @Query("SELECT * FROM cases WHERE isArchived = 0 ORDER BY nextHearingDate ASC")
    fun getAllActiveCases(): Flow<List<CaseEntity>>

    @Query(
        """
        SELECT * FROM cases
        WHERE isArchived = 0 AND (
            caseName LIKE '%' || :query || '%' OR
            caseNumber LIKE '%' || :query || '%' OR
            clientName LIKE '%' || :query || '%' OR
            oppositeParty LIKE '%' || :query || '%'
        )
        ORDER BY nextHearingDate ASC
        """
    )
    fun searchCases(query: String): Flow<List<CaseEntity>>

    @Query("UPDATE cases SET isArchived = 1 WHERE caseId = :caseId")
    suspend fun archiveCase(caseId: String)
}
