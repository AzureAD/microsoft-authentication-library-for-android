// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.microsoft.identity.client.*
import com.microsoft.identity.client.claims.ClaimsRequest
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import com.microsoft.identity.mfa.*
import java.lang.ref.WeakReference
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class BrowserDeletionTokenProvider private constructor(
    private val pca: IMultipleAccountPublicClientApplication
) : MfaDeletionTokenProvider {
    private var activity = WeakReference<Activity>(null)
    private val bridge = DeletionCallbackBridge()
    var checkpoint: (String) -> Unit = {}

    fun attach(value: Activity) { activity = WeakReference(value) }
    fun detach(value: Activity) { if (activity.get() === value) activity.clear() }

    override suspend fun acquireToken(request: MfaDeletionTokenRequest): MfaDeletionToken =
        withContext(Dispatchers.Main.immediate) {
            val authority = pca.configuration.defaultAuthority.authorityURL.toString().trimEnd('/')
            if (!authority.equals(request.authority, true)) {
                throw MfaDeletionException(MfaDeletionError.CONFIGURATION_REQUIRED)
            }
            val claims = ClaimsRequest.getClaimsRequestFromJsonString(request.claims)
            acquireDeletionToken(request.allowsInteraction, {
                checkpoint("SILENT_ACQUISITION")
                val accounts = bridge.await<List<IAccount>> { complete ->
                    pca.getAccounts(object : IPublicClientApplication.LoadAccountsCallback {
                        override fun onTaskCompleted(result: List<IAccount>) = complete(Result.success(result))
                        override fun onError(exception: MsalException) = complete(Result.failure(mapError(exception)))
                    })
                }
                val matches = accounts.filter { identity(it, request.tenantId)?.second
                    .equals(request.objectId, true) }
                if (matches.size > 1) throw MfaDeletionException(MfaDeletionError.INVALID_TOKEN)
                val account = matches.singleOrNull()
                    ?: throw MfaDeletionException(MfaDeletionError.INTERACTION_REQUIRED)
                val result = bridge.await<IAuthenticationResult> { complete ->
                    pca.acquireTokenSilentAsync(AcquireTokenSilentParameters.Builder()
                        .forAccount(account).fromAuthority(authority).withScopes(request.scopes)
                        .withClaims(claims).withCorrelationId(request.correlationId)
                        .withCallback(callback(complete)).build())
                }
                mapResult(result, authority).also { checkpoint("SILENT_TOKEN_RECEIVED") }
            }, {
                val host = activity.get()?.takeUnless { it.isFinishing || it.isDestroyed }
                    ?: throw MfaDeletionException(MfaDeletionError.USER_CANCELLED)
                checkpoint("INTERACTIVE_ACQUISITION")
                val result = bridge.await<IAuthenticationResult> { complete ->
                    pca.acquireToken(AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(host).fromAuthority(authority)
                        .withScopes(request.scopes).withClaims(claims)
                        .withCorrelationId(request.correlationId).withLoginHint(request.loginHint)
                        .withPrompt(Prompt.LOGIN).withCallback(callback(complete)).build())
                }
                mapResult(result, authority).also { checkpoint("INTERACTIVE_TOKEN_RECEIVED") }
            })
        }

    private fun callback(complete: (Result<IAuthenticationResult>) -> Unit) =
        object : AuthenticationCallback {
            override fun onSuccess(result: IAuthenticationResult) = complete(Result.success(result))
            override fun onError(exception: MsalException) = complete(Result.failure(mapError(exception)))
            override fun onCancel() = complete(Result.failure(MfaDeletionException(MfaDeletionError.USER_CANCELLED)))
        }

    private fun mapResult(result: IAuthenticationResult, authority: String): MfaDeletionToken {
        val tenant = result.tenantId ?: throw MfaDeletionException(MfaDeletionError.INVALID_TOKEN)
        val actual = identity(result.account, tenant)
            ?: throw MfaDeletionException(MfaDeletionError.INVALID_TOKEN)
        return MfaDeletionToken(result.accessToken, actual.first, actual.second, authority,
            result.expiresOn.time, result.authenticationScheme, result.scope.toList())
    }

    companion object {
        internal fun identity(account: IAccount, tenant: String): Pair<String, String>? {
            val profiles = listOf(account) + ((account as? IMultiTenantAccount)?.tenantProfiles?.values ?: emptyList())
            val identities = profiles.mapNotNull { profile ->
                val claims = profile.claims ?: return@mapNotNull null
                val tid = claims["tid"] as? String ?: return@mapNotNull null
                val oid = claims["oid"] as? String ?: return@mapNotNull null
                if (tid.equals(tenant, true)) tid to oid else null
            }.distinct()
            return identities.singleOrNull()
        }

        private fun mapError(error: MsalException): MfaDeletionException = MfaDeletionException(when {
            error is MsalUiRequiredException -> MfaDeletionError.INTERACTION_REQUIRED
            error.errorCode in setOf(MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE, MsalClientException.IO_ERROR) ->
                MfaDeletionError.PROVIDER_NETWORK
            else -> MfaDeletionError.PROVIDER_FAILED
        })

        suspend fun create(context: Context): Pair<BrowserDeletionTokenProvider?, String> {
            val resource = context.resources.getIdentifier("auth_config", "raw", context.packageName)
            if (resource == 0) return null to "DELETION SETUP REQUIRED: add local src/live/res/raw/auth_config.json; see TESTING-deletion.md."
            val problem = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val signatures = context.packageManager.getPackageInfo(context.packageName,
                    PackageManager.GET_SIGNATURES).signatures.orEmpty()
                val hashes = signatures.map { Base64.encodeToString(
                    MessageDigest.getInstance("SHA-1").digest(it.toByteArray()), Base64.NO_WRAP) }.toSet()
                val json = context.resources.openRawResource(resource).bufferedReader().use { it.readText() }
                deletionConfigurationProblem(json, context.packageName, hashes)
            }
            if (problem != null) return null to "DELETION SETUP REQUIRED: $problem"
            return try {
                val application = suspendCancellableCoroutine<IMultipleAccountPublicClientApplication> { continuation ->
                    PublicClientApplication.createMultipleAccountPublicClientApplication(context, resource,
                        object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                            override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                                if (continuation.isActive) continuation.resume(application)
                            }
                            override fun onError(exception: MsalException) {
                                if (continuation.isActive) continuation.resumeWithException(mapError(exception))
                            }
                        })
                }
                BrowserDeletionTokenProvider(application) to "DELETION CONFIGURED — tenant consent and service support still require live verification."
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                null to "DELETION SETUP REQUIRED: MSAL initialization failed; check redirect, browser and account mode."
            }
        }
    }
}
