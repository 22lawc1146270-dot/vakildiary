package com.vakildiary.app.data.backup

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.services.drive.DriveScopes
import com.vakildiary.app.core.Result
import com.vakildiary.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val driveBackupManager: DriveBackupManager
) {
    private val mutex = Mutex()
    private var initializedFor: String? = null

    suspend fun ensureInitialized(): Result<String> = mutex.withLock {
        val email = userPreferencesRepository.userEmail.first()
        if (email.isNullOrBlank()) {
            return Result.Error("Google Sign-In required for Drive backup")
        }
        if (driveBackupManager.isInitialized() && initializedFor == email) {
            return Result.Success(email)
        }
        driveBackupManager.initialize(
            requestInitializer = DriveRequestInitializer(context, email),
            applicationName = APP_NAME
        )
        initializedFor = email
        Result.Success(email)
    }

    private class DriveRequestInitializer(
        private val context: Context,
        private val email: String
    ) : HttpRequestInitializer {
        override fun initialize(request: HttpRequest) {
            val token = try {
                val account = Account(email, GOOGLE_ACCOUNT_TYPE)
                val scope = "oauth2:${DriveScopes.DRIVE_FILE}"
                GoogleAuthUtil.getToken(context, account, scope)
            } catch (e: UserRecoverableAuthException) {
                throw IOException("Drive authorization required", e)
            } catch (e: GoogleAuthException) {
                throw IOException("Drive authorization failed", e)
            } catch (e: IOException) {
                throw IOException("Drive authorization failed", e)
            }
            request.headers.authorization = "Bearer $token"
        }
    }

    companion object {
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private const val APP_NAME = "VakilDiary"
    }
}
