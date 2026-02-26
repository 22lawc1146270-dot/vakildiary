package com.vakildiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vakildiary.app.data.local.entities.ECourtTrackedCaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ECourtTrackedCaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(caseEntity: ECourtTrackedCaseEntity)

    @Query("SELECT * FROM ecourt_tracked_cases ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ECourtTrackedCaseEntity>>

    @Query("DELETE FROM ecourt_tracked_cases WHERE trackId = :trackId")
    suspend fun deleteById(trackId: String)
}
