package com.microsoft.identity.client.ktx

import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

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
