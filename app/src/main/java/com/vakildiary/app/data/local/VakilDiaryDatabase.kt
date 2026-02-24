package com.vakildiary.app.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.dao.DocumentDao
import com.vakildiary.app.data.local.dao.HearingHistoryDao
import com.vakildiary.app.data.local.dao.JudgmentMetadataDao
import com.vakildiary.app.data.local.dao.MeetingDao
import com.vakildiary.app.data.local.dao.PaymentDao
import com.vakildiary.app.data.local.dao.TaskDao
import com.vakildiary.app.data.local.entities.CaseEntity
import com.vakildiary.app.data.local.entities.DocumentEntity
import com.vakildiary.app.data.local.entities.HearingHistoryEntity
import com.vakildiary.app.data.local.entities.JudgmentMetadataEntity
import com.vakildiary.app.data.local.entities.MeetingEntity
import com.vakildiary.app.data.local.entities.PaymentEntity
import com.vakildiary.app.data.local.entities.TaskEntity

@Database(
    entities = [
        CaseEntity::class,
        HearingHistoryEntity::class,
        TaskEntity::class,
        PaymentEntity::class,
        DocumentEntity::class,
        MeetingEntity::class,
        JudgmentMetadataEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VakilDiaryDatabase : RoomDatabase() {
    abstract fun caseDao(): CaseDao
    abstract fun hearingHistoryDao(): HearingHistoryDao
    abstract fun taskDao(): TaskDao
    abstract fun paymentDao(): PaymentDao
    abstract fun documentDao(): DocumentDao
    abstract fun meetingDao(): MeetingDao
    abstract fun judgmentMetadataDao(): JudgmentMetadataDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE hearing_history ADD COLUMN voiceNotePath TEXT"
                )
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS judgment_metadata (
                        judgmentId TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        citation TEXT,
                        bench TEXT,
                        coram TEXT,
                        caseNumber TEXT,
                        year INTEGER NOT NULL,
                        archiveName TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        dateOfJudgment INTEGER,
                        searchText TEXT NOT NULL,
                        syncedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
