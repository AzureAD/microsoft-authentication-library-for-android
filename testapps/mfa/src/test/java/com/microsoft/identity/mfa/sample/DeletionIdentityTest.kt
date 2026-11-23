// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.client.IMultiTenantAccount
import com.microsoft.identity.client.ITenantProfile
import org.junit.Assert.*
import org.junit.Test

class DeletionIdentityTest {
    private open class Profile(private val values: Map<String, Any>) : ITenantProfile {
        override fun getId() = "home-id-must-not-be-used"
        override fun getAuthority() = "unused"
        override fun getIdToken(): String? = null
        override fun getClaims(): Map<String, Any> = values
        override fun getUsername() = "test@example.invalid"
        override fun getTenantId() = "do-not-use-requested-or-home-tenant"
    }

    @Test fun resultTenantSelectsGuestProfileNotHomeAccountId() {
        val account = object : Profile(mapOf("tid" to "home", "oid" to "home-object")),
            IMultiTenantAccount {
            override fun getTenantProfiles() = mapOf("guest" to
                Profile(mapOf("tid" to "guest", "oid" to "guest-object")))
        }
        assertEquals("guest" to "guest-object", BrowserDeletionTokenProvider.identity(account, "guest"))
        assertNull(BrowserDeletionTokenProvider.identity(account, "unknown"))
    }

    @Test fun absentOrAmbiguousProfileClaimsDoNotFabricateIdentity() {
        assertNull(BrowserDeletionTokenProvider.identity(Profile(mapOf("tid" to "tenant")), "tenant"))
        val account = object : Profile(mapOf("tid" to "tenant", "oid" to "one")),
            IMultiTenantAccount {
            override fun getTenantProfiles() = mapOf("tenant" to
                Profile(mapOf("tid" to "tenant", "oid" to "another")))
        }
        assertNull(BrowserDeletionTokenProvider.identity(account, "tenant"))
    }
}
