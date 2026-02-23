package com.vakildiary.app.di

import com.vakildiary.app.data.backup.DriveBackupManager
import com.vakildiary.app.data.backup.ManualBackupManager
import com.vakildiary.app.data.backup.RestoreManager
import com.vakildiary.app.data.backup.DeltaSyncManager
import com.vakildiary.app.data.backup.ChecksumStore
import com.vakildiary.app.data.backup.DriveAuthManager
import com.vakildiary.app.data.preferences.UserPreferencesRepository
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
    fun provideDriveAuthManager(
        @ApplicationContext context: Context,
        userPreferencesRepository: UserPreferencesRepository,
        driveBackupManager: DriveBackupManager
    ): DriveAuthManager = DriveAuthManager(context, userPreferencesRepository, driveBackupManager)

    @Provides
    @Singleton
    fun provideManualBackupManager(
        @ApplicationContext context: Context,
        driveBackupManager: DriveBackupManager,
        driveAuthManager: DriveAuthManager,
        userPreferencesRepository: UserPreferencesRepository
    ): ManualBackupManager = ManualBackupManager(context, driveBackupManager, driveAuthManager, userPreferencesRepository)

    @Provides
    @Singleton
    fun provideRestoreManager(
        @ApplicationContext context: Context,
        driveBackupManager: DriveBackupManager,
        driveAuthManager: DriveAuthManager
    ): RestoreManager = RestoreManager(context, driveBackupManager, driveAuthManager)

    @Provides
    @Singleton
    fun provideDeltaSyncManager(
        @ApplicationContext context: Context,
        driveBackupManager: DriveBackupManager,
        checksumStore: ChecksumStore,
        driveAuthManager: DriveAuthManager
    ): DeltaSyncManager = DeltaSyncManager(context, driveBackupManager, checksumStore, driveAuthManager)
}
