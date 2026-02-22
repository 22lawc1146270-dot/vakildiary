package com.vakildiary.app.presentation.screens.auth

import android.app.Activity
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vakildiary.app.R
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to VakilDiary", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Sign in with Google to continue", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState is AuthUiState.Error) {
            Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(12.dp))
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (uiState is AuthUiState.Loading) "Signing in..." else "Sign in with Google")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = { viewModel.skipSignIn() }) {
            Text(text = "Not now")
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
