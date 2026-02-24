package com.vakildiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vakildiary.app.data.local.entities.JudgmentMetadataEntity

@Dao
interface JudgmentMetadataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<JudgmentMetadataEntity>)

    @Query("SELECT * FROM judgment_metadata WHERE year = :year ORDER BY dateOfJudgment DESC")
    suspend fun getByYear(year: Int): List<JudgmentMetadataEntity>

    @Query(
        "SELECT * FROM judgment_metadata " +
            "WHERE year = :year AND searchText LIKE '%' || :query || '%' " +
            "ORDER BY dateOfJudgment DESC"
    )
    suspend fun searchByYear(year: Int, query: String): List<JudgmentMetadataEntity>

    @Query("SELECT COUNT(*) FROM judgment_metadata WHERE year = :year")
    suspend fun countByYear(year: Int): Int

    @Query("SELECT * FROM judgment_metadata WHERE judgmentId = :judgmentId LIMIT 1")
    suspend fun getById(judgmentId: String): JudgmentMetadataEntity?

    @Query("DELETE FROM judgment_metadata WHERE year = :year")
    suspend fun deleteByYear(year: Int)
}
