package com.vakildiary.app.di

import com.vakildiary.app.data.documents.DocumentStorageManager
import com.vakildiary.app.domain.storage.DocumentStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DocumentModule {
    @Binds
    @Singleton
    abstract fun bindDocumentStorage(
        impl: DocumentStorageManager
    ): DocumentStorage
}
