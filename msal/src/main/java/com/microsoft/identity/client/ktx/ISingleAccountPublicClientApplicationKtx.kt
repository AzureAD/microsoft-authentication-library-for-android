package com.microsoft.identity.client.ktx

import android.app.Activity
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.CurrentAccountCallback
import com.microsoft.identity.client.ISingleAccountPublicClientApplication.SignOutCallback
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.SignInParameters
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


suspend fun ISingleAccountPublicClientApplication.getCurrentAccountSuspend(): IAccount? =
    suspendCancellableCoroutine { continuation ->
        this.getCurrentAccountAsync(object : CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                continuation.resume(activeAccount)
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }
        })
    }

fun ISingleAccountPublicClientApplication.signIn(
    activity: Activity,
    scopes: List<String>,
    loginHint: String? = null,
    prompt: Prompt? = null,
    callback: AuthenticationCallback,
) {
    val signInParameters = SignInParameters.builder()
        .withActivity(activity)
        .withScopes(scopes)
        .withLoginHint(loginHint)
        .withPrompt(prompt)
        .withCallback(callback)
        .build()

    this.signIn(signInParameters)
}

suspend fun ISingleAccountPublicClientApplication.signIn(
    activity: Activity,
    scopes: List<String>,
    loginHint: String? = null,
    prompt: Prompt? = null,
): IAuthenticationResult? =
    suspendCancellableCoroutine { continuation ->
        val callback = object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                continuation.resume(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }

            override fun onCancel() {
                continuation.resume(null)
            }
        }

        this.signIn(
            activity = activity,
            scopes = scopes,
            loginHint = loginHint,
            prompt = prompt,
            callback = callback
        )
    }

suspend fun ISingleAccountPublicClientApplication.signOutSuspend() =
    suspendCancellableCoroutine { continuation ->
        this.signOut(object : SignOutCallback {
            override fun onSignOut() {
                continuation.resume(Unit)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }
        })
    }
