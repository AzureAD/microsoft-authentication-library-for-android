// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import java.util.UUID

/** SDK-owned requirements for deleting one stored registration. Never log this object. */
class MfaDeletionTokenRequest internal constructor(
    val tenantId: String,
    val objectId: String,
    val loginHint: String,
    val allowsInteraction: Boolean
) {
    val authority: String = "https://login.microsoftonline.com/$tenantId"
    val scopes: List<String> = listOf("https://graph.microsoft.com/UserAuthenticationMethod.ReadWrite")
    val claims: String = """{"access_token":{"amr":{"essential":true,"values":["ngcmfa"]}}}"""
    val correlationId: UUID = UUID.randomUUID()
    override fun toString(): String = "MfaDeletionTokenRequest(redacted)"
}

/**
 * Actual authentication-library result metadata, not values copied from the request.
 * Authority is the trusted acquisition context, bound to the returned tenant profile.
 * Expiry is UTC epoch milliseconds. Access tokens are opaque and must never be decoded.
 */
data class MfaDeletionToken(
    val accessToken: String,
    val tenantId: String,
    val objectId: String,
    val authority: String,
    val expiresOnMillis: Long,
    val authenticationScheme: String,
    val grantedScopes: List<String>
) {
    override fun toString(): String = "MfaDeletionToken(redacted)"
}

/** Host-owned acquisition; silent first, interactive only when explicitly permitted and required. */
fun interface MfaDeletionTokenProvider {
    suspend fun acquireToken(request: MfaDeletionTokenRequest): MfaDeletionToken
}

/** Observable deletion checkpoints. SUCCEEDED and LOCAL_CLEANUP_REQUIRED survive cancellation. */
enum class MfaDeletionStage {
    ACQUIRING_TOKEN, WAITING_FOR_APPROVAL, DELETING, SUCCEEDED, LOCAL_CLEANUP_REQUIRED
}

/** Bounded, non-sensitive deletion failures; no raw provider/Graph payloads are retained. */
enum class MfaDeletionError {
    UNKNOWN_REGISTRATION, OPERATION_IN_PROGRESS, INVALID_REGISTRATION, INVALID_TOKEN,
    INTERACTION_REQUIRED, USER_CANCELLED, PROVIDER_FAILED, PROVIDER_NETWORK, CONFIGURATION_REQUIRED,
    GRAPH_REJECTED, OUTCOME_UNKNOWN, LOCAL_CLEANUP_REQUIRED
}

/** HTTP status is available only when an actual Graph response was received. */
class MfaDeletionException(
    val code: MfaDeletionError,
    val httpStatus: Int? = null
) : Exception(code.name)
