package com.microsoft.identity.client.ktx

import android.app.Activity
import android.text.TextUtils
import com.microsoft.identity.client.AadAuthorityAudience
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.AzureCloudInstance
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.microsoft.identity.common.java.ui.PreferredAuthMethod
import java.util.UUID
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationFragment
import androidx.fragment.app.Fragment;
import com.microsoft.identity.client.AuthenticationScheme
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.claims.ClaimsRequest
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
 * Perform acquire token silent call.
 * If there is a valid access token in the cache, the SDK will return the access token;
 * If no valid access token exists, the SDK will try to find a refresh token and
 * use the refresh token to get a new access token.
 * If a refresh token does not exist or it fails the refresh, an exception will be thrown.
 *
 * @param scopes The list of scopes to be consented to during sign in.
 * MSAL always sends the scopes 'openid profile offline_access'. Do not include any of these scopes
 * in the scope parameter.
 *
 * @param authority Can be passed to override the default authority.
 *
 * @param account If provided, will be used to force the session continuation.
 * If the user tries to sign in with a different account, an error with be thrown.
 *
 * @param authenticationScheme The authentication scheme.
 *
 * @param claimsRequest Request-specific claims for the id_token and access_token.
 *
 * @param correlationId The correlation id passed to Token Parameters.
 * If specified, MSAL will use this correlation id for the request instead of generating a new one.
 *
 * @param forceRefresh Indicates whether MSAL should refresh the access token.
 * Default is false and unless you have good reason to, you should not use this parameter.
 *
 * @see IPublicClientApplication.acquireTokenSilentAsync
 */
suspend fun IPublicClientApplication.acquireTokenSilentSuspend(
    scopes: List<String>,
    authority: Authority,
    account: IAccount? = null,
    authenticationScheme: AuthenticationScheme,
    claimsRequest: ClaimsRequest? = null,
    correlationId: String? = null,
    forceRefresh: Boolean? = null,
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

        val builder = AcquireTokenSilentParameters.Builder()
            .fromAuthority(authority.authorityUrl)
            .forAccount(account)
            .withScopes(scopes)
            .withAuthenticationScheme(authenticationScheme)
            .withClaims(claimsRequest)
            .withCorrelationId(UUID.fromString(correlationId))
            .withCallback(callback)

        if (correlationId != null) {
            builder.withCorrelationId(UUID.fromString(correlationId))
        }

        if (forceRefresh != null) {
            builder.forceRefresh(forceRefresh)
        }

        val acquireTokenSilentParams = builder.build()

        this.acquireTokenSilentAsync(acquireTokenSilentParams)
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

/**
 * Acquire token interactively. Will pop-up web UI. Interactive flow will skip the cache lookup.
 * Default value for [Prompt] is [Prompt.SELECT_ACCOUNT].
 *
 * Returns null if the user cancels the process.
 *
 * @param activity The [Activity] that will be used as the parent activity for launching
 * the [AuthorizationActivity].
 *
 * @param fragment The [Fragment] that will be replaced by [AuthorizationFragment]
 *
 * @param loginHint The login hint sent along with the authorization request.
 *
 * @param preferredAuthMethod The preferred authentication method sent along with
 * the authorization request
 *
 * @param prompt The prompt parameter sent along with the authorization request.
 *
 * @param scopes The list of scopes to be consented to during sign in.
 * MSAL always sends the scopes 'openid profile offline_access'. Do not include any of these scopes
 * in the scope parameter.
 *
 * @param extraScopesToConsent Additional scopes (of other resources) that you would like
 * the user to authorize up front.
 * The scopes parameter should only contain scopes for a single resource.
 *
 * @param extraQueryStringParameters If you've been instructed to pass additional
 * query string parameters to the authorization endpoint, you can add them here.
 * Otherwise, would recommend not touching.
 *
 * @param authority Can be passed to override the default authority.
 *
 * @param account If provided, will be used to force the session continuation.
 * If the user tries to sign in with a different account, an error with be thrown.
 *
 * @param authenticationScheme The authentication scheme.
 *
 * @param claimsRequest Request-specific claims for the id_token and access_token.
 *
 * @param correlationId The correlation id passed to Token Parameters.
 * If specified, MSAL will use this correlation id for the request instead of generating a new one.
 *
 * @see IPublicClientApplication.acquireToken
 */
suspend fun IPublicClientApplication.acquireTokenSuspend(
    activity: Activity,
    fragment: Fragment? = null,
    loginHint: String? = null,
    preferredAuthMethod: PreferredAuthMethod? = null,
    prompt: Prompt? = null,
    scopes: List<String>,
    extraScopesToConsent: List<String>? = null,
    extraQueryStringParameters: List<Map.Entry<String, String>>? = null,
    authority: Authority? = null,
    account: IAccount? = null,
    authenticationScheme: AuthenticationScheme,
    claimsRequest: ClaimsRequest? = null,
    correlationId: String? = null,
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

        val builder = AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withFragment(fragment)
            .withLoginHint(loginHint)
            .withPreferredAuthMethod(preferredAuthMethod)!!  // Accidental @Nullable annotation?
            .withPrompt(prompt)
            .withOtherScopesToAuthorize(extraScopesToConsent)
            .withAuthorizationQueryStringParameters(extraQueryStringParameters)
            .fromAuthority(authority?.authorityUrl)
            .forAccount(account)
            .withScopes(scopes)
            .withAuthenticationScheme(authenticationScheme)
            .withClaims(claimsRequest)
            .withCallback(callback)

        if (correlationId != null) {
            builder.withCorrelationId(UUID.fromString(correlationId))
        }

        val acquireTokenParams = builder.build()

        this.acquireToken(acquireTokenParams)
    }

data class Authority(
    val authorityUrl: String,
) {
    companion object {
        fun from(authorityUrl: String): Authority {
            return Authority(authorityUrl)
        }

        fun from(
            cloudInstance: AzureCloudInstance,
            audience: AadAuthorityAudience,
            tenant: String?
        ): Authority {
            return if (!TextUtils.isEmpty(tenant)) {
                if (audience != AadAuthorityAudience.AzureAdMyOrg) {
                    throw IllegalArgumentException(
                        "Audience must be " + AadAuthorityAudience.AzureAdMyOrg + " when tenant is specified"
                    )
                } else {
                    from(cloudInstance, tenant!!)
                }
            } else if (audience == AadAuthorityAudience.AzureAdMyOrg) {
                if (TextUtils.isEmpty(tenant)) {
                    throw IllegalArgumentException(
                        "Tenant must be specified when the audience is $audience"
                    )
                } else {
                    from(cloudInstance.cloudInstanceUri + "/" + tenant)
                }
            } else {
                from(cloudInstance.cloudInstanceUri + "/" + audience.audienceValue)
            }
        }

        fun from(
            cloudInstance: AzureCloudInstance,
            audience: AadAuthorityAudience
        ): Authority =
            from(cloudInstance, audience, null)

        fun from(
            cloudInstance: AzureCloudInstance,
            tenant: String
        ): Authority =
            from(cloudInstance.cloudInstanceUri + "/" + tenant)
    }
}
