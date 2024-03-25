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
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity

/**
 * Gets the current account.
 * This method must be called whenever the application is resumed or prior to running
 * a scheduled background operation.
 *
 * @see ISingleAccountPublicClientApplication.getCurrentAccountAsync
 */
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

/**
 * Allows a user to sign in to your application with one of their accounts.
 * This method may only be called once: once a user is signed in, they must first be signed out
 * before another user may sign in.
 * If you wish to prompt the existing user for credentials
 * use [ISingleAccountPublicClientApplication.signInAgain]
 * or [ISingleAccountPublicClientApplication.acquireToken].
 *
 * Note: The authority used to make the sign in request will be either
 * the MSAL default (https://login.microsoftonline.com/common)
 * or the default authority specified by you in your configuration.
 *
 * @param activity The [Activity] that is used as the parent activity for launching
 * the [AuthorizationActivity].
 *
 * @param loginHint If provided, will be used as the query parameter sent for
 * authenticating the user, which will have the UPN pre-populated.
 *
 * @param scopes The list of scopes to be consented to during sign in.
 * MSAL always sends the scopes 'openid profile offline_access'. Do not include any of these scopes
 * in the scope parameter.
 * The access token returned is for MS Graph and will allow you to query for additional information
 * about the signed in account.
 *
 * @param prompt Indicates the type of user interaction that is required.
 * If no argument is supplied the default behavior will be used ([Prompt.SELECT_ACCOUNT]).
 *
 * @param callback [AuthenticationCallback] that is used to send the result back.
 * The success result will be sent back via [AuthenticationCallback.onSuccess].
 * Failure case will be sent back via [AuthenticationCallback.onError].
 */
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

/**
 * Allows a user to sign in to your application with one of their accounts.
 * This method may only be called once: once a user is signed in, they must first be signed out
 * before another user may sign in.
 * If you wish to prompt the existing user for credentials
 * use [ISingleAccountPublicClientApplication.signInAgain]
 * or [ISingleAccountPublicClientApplication.acquireToken].
 *
 * Note: The authority used to make the sign in request will be either
 * the MSAL default (https://login.microsoftonline.com/common)
 * or the default authority specified by you in your configuration.
 *
 * @param activity The [Activity] that is used as the parent activity for launching
 * the [AuthorizationActivity].
 *
 * @param loginHint If provided, will be used as the query parameter sent for
 * authenticating the user, which will have the UPN pre-populated.
 *
 * @param scopes The list of scopes to be consented to during sign in.
 * MSAL always sends the scopes 'openid profile offline_access'. Do not include any of these scopes
 * in the scope parameter.
 * The access token returned is for MS Graph and will allow you to query for additional information
 * about the signed in account.
 *
 * @param prompt Indicates the type of user interaction that is required.
 * If no argument is supplied the default behavior will be used ([Prompt.SELECT_ACCOUNT]).
 *
 * @return [IAuthenticationResult] or null if the user cancels the process.
 */
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

/**
 * Signs out the current the Account and Credentials (tokens).
 *
 * Note: If a device is marked as a shared device within the broker,
 * the sign-out will be device-wide.
 *
 * @see ISingleAccountPublicClientApplication.signOut
 */
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
