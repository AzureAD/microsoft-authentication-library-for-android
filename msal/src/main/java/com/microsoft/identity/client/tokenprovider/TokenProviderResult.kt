// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.client.tokenprovider

import java.util.Collections
import java.util.Date

/**
 * Immutable token and account metadata mapped from the authentication library's result.
 *
 * Identity must describe the authenticated account for this acquisition, not copied request values
 * or identity inferred by decoding the access token. Native providers need not construct an
 * MSAL-specific result. Consumers validate required metadata; the resource validates authorization.
 * Do not log this result or persist it in MFA registration storage. No refresh token crosses this API.
 * @property accessToken Original access token. Do not modify or log it.
 * @property tenantId Actual authenticated tenant identifier.
 * @property objectId Actual authenticated object's identifier in that tenant.
 * @property authorityUrl Absolute authority URL used for acquisition.
 * @property authenticationScheme Returned token scheme, such as Bearer. Do not relabel an unknown
 * or different scheme as Bearer.
 */
class TokenProviderResult(
    val accessToken: String,
    expiresOn: Date?,
    scopes: List<String>,
    val tenantId: String,
    val objectId: String,
    val authorityUrl: String,
    val authenticationScheme: String
) {
    private val expiresOnMillis = expiresOn?.time

    /** Token expiry, or null when unavailable. Each read returns an independent snapshot. */
    val expiresOn: Date?
        get() = expiresOnMillis?.let { Date(it) }

    /** Immutable snapshot of granted scopes, not a copy of the requested permissions. */
    val scopes: List<String> = Collections.unmodifiableList(ArrayList(scopes))

    /** Omits the token, authority, scopes and account information. */
    override fun toString(): String = "TokenProviderResult(<redacted>)"
}
