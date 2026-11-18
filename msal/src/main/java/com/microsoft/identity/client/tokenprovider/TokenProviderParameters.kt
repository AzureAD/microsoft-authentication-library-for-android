// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.client.tokenprovider

import java.util.Collections
import java.util.UUID

/**
 * Immutable token requirements supplied by the consuming feature, not browser presentation options.
 *
 * Providers must honor the supplied requirements rather than hardcoding one feature's permissions.
 * Never log these properties; claims and account hints may contain personal information.
 * @property authorityUrl Absolute authority URL to use for acquisition.
 * @property claimsRequestJson Optional OIDC claims JSON to honor during acquisition, not to inject
 * into or manufacture in a returned token.
 * @property tenantId Expected tenant identity when specified.
 * @property objectId Expected object identity in that tenant when specified.
 * @property loginHint Optional hint for sign-in UI, not proof of identity.
 * @property allowsInteraction Whether acquisition may present UI. False requires an
 * interaction-required failure when silent acquisition cannot meet the requirements.
 * @property correlationId Operation correlation ID to pass through token acquisition.
 */
class TokenProviderParameters(
    val authorityUrl: String,
    scopes: List<String>,
    val claimsRequestJson: String? = null,
    val tenantId: String? = null,
    val objectId: String? = null,
    val loginHint: String? = null,
    val allowsInteraction: Boolean = true,
    val correlationId: UUID = UUID.randomUUID()
) {
    /** Immutable snapshot of the requested resource permissions. */
    val scopes: List<String> = Collections.unmodifiableList(ArrayList(scopes))

    /** Omits claims, authority, scopes and account information. */
    override fun toString(): String = "TokenProviderParameters(<redacted>)"
}
