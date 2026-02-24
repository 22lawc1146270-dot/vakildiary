package com.vakildiary.app.di

import android.content.Context
import androidx.room.Room
import com.vakildiary.app.data.local.VakilDiaryDatabase
import com.vakildiary.app.data.local.dao.CaseDao
import com.vakildiary.app.data.local.dao.DocumentDao
import com.vakildiary.app.data.local.dao.HearingHistoryDao
import com.vakildiary.app.data.local.dao.JudgmentMetadataDao
import com.vakildiary.app.data.local.dao.MeetingDao
import com.vakildiary.app.data.local.dao.PaymentDao
import com.vakildiary.app.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private const val DATABASE_NAME = "vakildiary.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): VakilDiaryDatabase {
        return Room.databaseBuilder(
            context,
            VakilDiaryDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(VakilDiaryDatabase.MIGRATION_1_2, VakilDiaryDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    fun provideCaseDao(database: VakilDiaryDatabase): CaseDao = database.caseDao()

    @Provides
    fun provideHearingHistoryDao(database: VakilDiaryDatabase): HearingHistoryDao =
        database.hearingHistoryDao()

    @Provides
    fun provideTaskDao(database: VakilDiaryDatabase): TaskDao = database.taskDao()

    @Provides
    fun providePaymentDao(database: VakilDiaryDatabase): PaymentDao = database.paymentDao()

    @Provides
    fun provideDocumentDao(database: VakilDiaryDatabase): DocumentDao = database.documentDao()

    @Provides
    fun provideMeetingDao(database: VakilDiaryDatabase): MeetingDao = database.meetingDao()

    @Provides
    fun provideJudgmentMetadataDao(database: VakilDiaryDatabase): JudgmentMetadataDao =
        database.judgmentMetadataDao()
}
