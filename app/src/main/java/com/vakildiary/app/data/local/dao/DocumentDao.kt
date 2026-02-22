package com.vakildiary.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vakildiary.app.data.local.entities.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query(
        """
        SELECT * FROM documents
        WHERE (:caseId IS NULL AND caseId IS NULL) OR caseId = :caseId
        ORDER BY createdAt DESC
        """
    )
    fun getDocumentsByCaseId(caseId: String?): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)
}
