package com.vakildiary.app.di

import com.vakildiary.app.data.remote.ECourtApiService
import com.vakildiary.app.data.remote.judgments.SCJudgmentService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val ECOURT_BASE_URL = "https://hcservices.ecourts.gov.in/ecourtindiaHC/"
    private const val SC_JUDGMENT_BASE_URL = "https://indian-supreme-court-judgments.s3.amazonaws.com/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    @Named("ecourt")
    fun provideEcourtRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ECOURT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideECourtApiService(@Named("ecourt") retrofit: Retrofit): ECourtApiService {
        return retrofit.create(ECourtApiService::class.java)
    }

    @Provides
    @Singleton
    @Named("sc_judgment")
    fun provideScJudgmentRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(SC_JUDGMENT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideScJudgmentService(@Named("sc_judgment") retrofit: Retrofit): SCJudgmentService {
        return retrofit.create(SCJudgmentService::class.java)
    }
}
