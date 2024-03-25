package com.microsoft.identity.client.ktx

import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Perform acquire token silent call.
 * If there is a valid access token in the cache, the SDK will return the access token;
 * If no valid access token exists, the SDK will try to find a refresh token and
 * use the refresh token to get a new access token.
 * If a refresh token does not exist or it fails the refresh, an exception will be thrown.
 *
 * @param acquireTokenSilentParameters
 *
 * @see IPublicClientApplication.acquireTokenSilentAsync
 */
suspend fun IPublicClientApplication.acquireTokenSilentSuspend(
    acquireTokenSilentParameters: AcquireTokenSilentParameters,
): IAuthenticationResult =
    suspendCancellableCoroutine { continuation ->
        val callback = object : SilentAuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                continuation.resume(authenticationResult)
            }

            override fun onError(exception: MsalException) {
                continuation.cancel(exception)
            }
        }

        val paramsWithCallback = AcquireTokenSilentParameters.Builder()
            .fromAuthority(acquireTokenSilentParameters.authority)
            .forAccount(acquireTokenSilentParameters.account)
            .withScopes(acquireTokenSilentParameters.scopes)
            .forceRefresh(acquireTokenSilentParameters.forceRefresh)
            .withAuthenticationScheme(acquireTokenSilentParameters.authenticationScheme)
            .withClaims(acquireTokenSilentParameters.claimsRequest)
            .withCorrelationId(UUID.fromString(acquireTokenSilentParameters.correlationId))
            .withCallback(callback)
            .build()

        this.acquireTokenSilentAsync(paramsWithCallback)
    }

/**
 * Acquire token interactively. Will pop-up web UI. Interactive flow will skip the cache lookup.
 * Default value for [Prompt] is [Prompt.SELECT_ACCOUNT].
 *
 * Returns null if the user cancels the process.
 *
 * @param acquireTokenParameters
 *
 * @see IPublicClientApplication.acquireToken
 */
suspend fun IPublicClientApplication.acquireTokenSuspend(
    acquireTokenParameters: AcquireTokenParameters,
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

        val paramsWithCallback = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(acquireTokenParameters.activity)
            .withFragment(acquireTokenParameters.fragment)
            .withLoginHint(acquireTokenParameters.loginHint)
            .withPreferredAuthMethod(acquireTokenParameters.preferredAuthMethod)!!  // Accidental @Nullable annotation?
            .withPrompt(acquireTokenParameters.prompt)
            .withOtherScopesToAuthorize(acquireTokenParameters.extraScopesToConsent)
            .withAuthorizationQueryStringParameters(acquireTokenParameters.extraQueryStringParameters)
            .fromAuthority(acquireTokenParameters.authority)
            .forAccount(acquireTokenParameters.account)
            .withScopes(acquireTokenParameters.scopes)
            .withAuthenticationScheme(acquireTokenParameters.authenticationScheme)
            .withClaims(acquireTokenParameters.claimsRequest)
            .withCorrelationId(UUID.fromString(acquireTokenParameters.correlationId))
            .withCallback(callback)
            .build()

        this.acquireToken(paramsWithCallback)
    }
