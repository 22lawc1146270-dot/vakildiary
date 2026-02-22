package com.vakildiary.app.di

import com.vakildiary.app.data.repository.CaseRepositoryImpl
import com.vakildiary.app.data.repository.DocumentRepositoryImpl
import com.vakildiary.app.data.repository.ECourtRepositoryImpl
import com.vakildiary.app.data.repository.HearingRepositoryImpl
import com.vakildiary.app.data.repository.MeetingRepositoryImpl
import com.vakildiary.app.data.repository.PaymentRepositoryImpl
import com.vakildiary.app.data.repository.SCJudgmentRepositoryImpl
import com.vakildiary.app.data.repository.TaskRepositoryImpl
import com.vakildiary.app.domain.repository.CaseRepository
import com.vakildiary.app.domain.repository.DocumentRepository
import com.vakildiary.app.domain.repository.ECourtRepository
import com.vakildiary.app.domain.repository.HearingRepository
import com.vakildiary.app.domain.repository.SCJudgmentRepository
import com.vakildiary.app.domain.repository.MeetingRepository
import com.vakildiary.app.domain.repository.PaymentRepository
import com.vakildiary.app.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCaseRepository(
        impl: CaseRepositoryImpl
    ): CaseRepository

    @Binds
    @Singleton
    abstract fun bindHearingRepository(
        impl: HearingRepositoryImpl
    ): HearingRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        impl: PaymentRepositoryImpl
    ): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        impl: DocumentRepositoryImpl
    ): DocumentRepository

    @Binds
    @Singleton
    abstract fun bindMeetingRepository(
        impl: MeetingRepositoryImpl
    ): MeetingRepository

    @Binds
    @Singleton
    abstract fun bindECourtRepository(
        impl: ECourtRepositoryImpl
    ): ECourtRepository

    @Binds
    @Singleton
    abstract fun bindSCJudgmentRepository(
        impl: SCJudgmentRepositoryImpl
    ): SCJudgmentRepository
}
