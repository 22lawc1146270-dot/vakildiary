package com.vakildiary.app.di

import com.vakildiary.app.data.backup.DriveBackupManager
import com.vakildiary.app.data.backup.ManualBackupManager
import com.vakildiary.app.data.backup.RestoreManager
import com.vakildiary.app.data.backup.DeltaSyncManager
import com.vakildiary.app.data.backup.ChecksumStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import android.content.Context
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DriveModule {
    @Provides
    @Singleton
    fun provideDriveBackupManager(): DriveBackupManager = DriveBackupManager()

    @Provides
    @Singleton
    fun provideManualBackupManager(
        @ApplicationContext context: Context,
        driveBackupManager: DriveBackupManager
    ): ManualBackupManager = ManualBackupManager(context, driveBackupManager)

    @Provides
    @Singleton
    fun provideRestoreManager(
        @ApplicationContext context: Context,
        driveBackupManager: DriveBackupManager
    ): RestoreManager = RestoreManager(context, driveBackupManager)

    @Provides
    @Singleton
    fun provideDeltaSyncManager(
        @ApplicationContext context: Context,
        driveBackupManager: DriveBackupManager,
        checksumStore: ChecksumStore
    ): DeltaSyncManager = DeltaSyncManager(context, driveBackupManager, checksumStore)
}
