package com.vakildiary.app.presentation.screens.auth

import android.app.Activity
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vakildiary.app.R
import com.vakildiary.app.presentation.components.ButtonLabel
import com.vakildiary.app.presentation.theme.VakilTheme
import com.vakildiary.app.presentation.viewmodels.AuthViewModel
import com.vakildiary.app.presentation.viewmodels.state.AuthUiState
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SignInScreen(
    viewModel: AuthViewModel,
    uiState: AuthUiState
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val credentialManager = remember { CredentialManager.create(context) }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VakilTheme.colors.bgPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(VakilTheme.spacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "VakilDiary", 
                style = VakilTheme.typography.headlineLarge,
                color = VakilTheme.colors.accentPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(VakilTheme.spacing.xs))
            Text(
                text = "LITIGATION CONTROL CENTER", 
                style = VakilTheme.typography.labelSmall,
                color = VakilTheme.colors.textTertiary,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )
            
            Spacer(modifier = Modifier.height(VakilTheme.spacing.xl))
            
            Text(
                text = "Professional management for your legal practice.", 
                style = VakilTheme.typography.bodyLarge,
                color = VakilTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(VakilTheme.spacing.xl))

            if (uiState is AuthUiState.Error) {
                Surface(
                    color = VakilTheme.colors.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.message, 
                        color = VakilTheme.colors.error,
                        modifier = Modifier.padding(VakilTheme.spacing.sm),
                        style = VakilTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(VakilTheme.spacing.md))
            }

            Button(
                onClick = {
                    if (activity == null) {
                        viewModel.onSignInError("Unable to start sign-in")
                        return@Button
                    }
                    coroutineScope.launch {
                        viewModel.onLoading()
                        signInWithGoogle(
                            activity = activity,
                            credentialManager = credentialManager,
                            onSuccess = viewModel::onSignInSuccess,
                            onError = viewModel::onSignInError
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VakilTheme.colors.accentPrimary,
                    disabledContainerColor = VakilTheme.colors.bgSurfaceSoft
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(VakilTheme.spacing.md),
                enabled = uiState !is AuthUiState.Loading
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = VakilTheme.colors.onAccent, strokeWidth = 2.dp)
                } else {
                    ButtonLabel(
                        text = "Sign in with Google",
                        style = VakilTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(VakilTheme.spacing.md))

            TextButton(
                onClick = { viewModel.skipSignIn() },
                modifier = Modifier.fillMaxWidth()
            ) {
                ButtonLabel(
                    text = "Continue as Guest",
                    style = VakilTheme.typography.labelMedium.copy(color = VakilTheme.colors.textSecondary)
                )
            }
        }
    }
}

private suspend fun signInWithGoogle(
    activity: Activity,
    credentialManager: CredentialManager,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val serverClientId = activity.getString(R.string.google_web_client_id)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(serverClientId)
        .setFilterByAuthorizedAccounts(true)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(activity, request)
        val credential = result.credential
        if (credential is androidx.credentials.CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val email = extractEmailFromIdToken(googleCredential.idToken) ?: googleCredential.id
            onSuccess(email)
        } else {
            onError("Unsupported credential type")
        }
    } catch (e: NoCredentialException) {
        val retryOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val retryRequest = GetCredentialRequest.Builder()
            .addCredentialOption(retryOption)
            .build()
        try {
            val retryResult = credentialManager.getCredential(activity, retryRequest)
            val credential = retryResult.credential
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = extractEmailFromIdToken(googleCredential.idToken) ?: googleCredential.id
                onSuccess(email)
            } else {
                onError("Unsupported credential type")
            }
        } catch (e2: GetCredentialException) {
            onError("Sign-in failed")
        }
    } catch (e: GetCredentialException) {
        onError("Sign-in failed")
    }
}

private fun extractEmailFromIdToken(idToken: String): String? {
    return try {
        val parts = idToken.split(".")
        if (parts.size < 2) return null
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE), Charsets.UTF_8)
        val json = JSONObject(payload)
        json.optString("email", null)
    } catch (t: Throwable) {
        null
    }
}
