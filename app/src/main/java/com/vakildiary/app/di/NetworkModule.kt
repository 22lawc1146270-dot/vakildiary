package com.vakildiary.app.di

import com.vakildiary.app.BuildConfig
import com.vakildiary.app.data.remote.ECourtApiService
import com.vakildiary.app.data.remote.judgments.SCJudgmentService
import com.vakildiary.app.data.remote.reportable.ReportableBackendService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val ECOURT_BASE_URL = "https://services.ecourts.gov.in/ecourtindia_v6/"
    private const val SC_JUDGMENT_BASE_URL = "https://indian-supreme-court-judgments.s3.amazonaws.com/"
    private const val ECOURT_HOST = "services.ecourts.gov.in"
    private const val ECOURT_REFERER = "https://services.ecourts.gov.in/ecourtindia_v6/?p=casestatus/index"
    private const val ECOURT_ORIGIN = "https://services.ecourts.gov.in"
    private const val SCI_HOST_SUFFIX = "sci.gov.in"
    private const val SCI_REFERER = "https://www.sci.gov.in/judgements-case-no/"
    private const val SCI_ORIGIN = "https://www.sci.gov.in"
    private const val ECOURT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
    private const val SCI_USER_AGENT = ECOURT_USER_AGENT

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
        return OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.host == ECOURT_HOST) {
                    val updated = request.newBuilder()
                        .header("User-Agent", ECOURT_USER_AGENT)
                        .header("Accept", "text/html,application/json;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-IN,en;q=0.9")
                        .header("Origin", ECOURT_ORIGIN)
                        .header("Referer", ECOURT_REFERER)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .build()
                    chain.proceed(updated)
                } else if (request.url.host.endsWith(SCI_HOST_SUFFIX, ignoreCase = true)) {
                    val updated = request.newBuilder()
                        .header("User-Agent", SCI_USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-IN,en;q=0.9")
                        .header("Origin", SCI_ORIGIN)
                        .header("Referer", SCI_REFERER)
                        .header("Upgrade-Insecure-Requests", "1")
                        .build()
                    chain.proceed(updated)
                } else {
                    chain.proceed(request)
                }
            }
            .build()
    }

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

    @Provides
    @Singleton
    @Named("reportable_backend")
    fun provideReportableBackendRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.REPORTABLE_BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideReportableBackendService(
        @Named("reportable_backend") retrofit: Retrofit
    ): ReportableBackendService {
        return retrofit.create(ReportableBackendService::class.java)
    }
}
