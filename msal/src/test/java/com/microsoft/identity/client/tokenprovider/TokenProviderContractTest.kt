// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.client.tokenprovider

import java.util.Date
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class TokenProviderContractTest {
    @Test
    fun `request scopes cannot change after construction or through a returned list`() {
        val scopes = mutableListOf("requested-scope")
        val parameters = TokenProviderParameters("https://example.invalid/tenant", scopes)
        scopes.clear()

        assertEquals(listOf("requested-scope"), parameters.scopes)
        assertThrows(UnsupportedOperationException::class.java) {
            (parameters.scopes as MutableList<String>).clear()
        }
    }

    @Test
    fun `result snapshots scopes and expiry instead of exposing mutable authentication metadata`() {
        val scopes = mutableListOf("granted-scope")
        val expiresOn = Date(1000L)
        val result = TokenProviderResult(
            accessToken = "test-token",
            expiresOn = expiresOn,
            scopes = scopes,
            tenantId = "tenant-id",
            objectId = "object-id",
            authorityUrl = "https://example.invalid/tenant",
            authenticationScheme = "Bearer"
        )
        scopes.clear()
        expiresOn.time = 2000L

        assertEquals(listOf("granted-scope"), result.scopes)
        assertThrows(UnsupportedOperationException::class.java) {
            (result.scopes as MutableList<String>).clear()
        }
        assertEquals(Date(1000L), result.expiresOn)
        result.expiresOn?.time = 3000L
        assertEquals(Date(1000L), result.expiresOn)
    }

    @Test
    fun `request and result strings do not disclose tokens claims or account metadata`() {
        val sensitive = "sensitive-test-value"
        val parameters = TokenProviderParameters(
            authorityUrl = sensitive,
            scopes = listOf(sensitive),
            claimsRequestJson = sensitive,
            tenantId = sensitive,
            objectId = sensitive,
            loginHint = sensitive,
            allowsInteraction = true,
            correlationId = UUID.randomUUID()
        )
        val result = TokenProviderResult(
            accessToken = sensitive,
            expiresOn = null,
            scopes = listOf(sensitive),
            tenantId = sensitive,
            objectId = sensitive,
            authorityUrl = sensitive,
            authenticationScheme = sensitive
        )

        assertFalse(parameters.toString().contains(sensitive))
        assertFalse(result.toString().contains(sensitive))
    }
}
