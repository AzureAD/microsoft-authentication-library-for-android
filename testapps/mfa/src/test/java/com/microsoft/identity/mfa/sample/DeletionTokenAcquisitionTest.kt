// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DeletionTokenAcquisitionTest {
    private val token = MfaDeletionToken("opaque", "tenant", "object", "authority",
        Long.MAX_VALUE, "Bearer", emptyList())

    @Test fun silentSuccessDoesNotOpenBrowser() = runBlocking {
        assertSame(token, acquireDeletionToken(true, { token }, { error("Unexpected browser") }))
    }

    @Test fun onlyInteractionRequiredCanFallBackWhenAllowed() = runBlocking {
        assertSame(token, acquireDeletionToken(true, {
            throw MfaDeletionException(MfaDeletionError.INTERACTION_REQUIRED)
        }, { token }))
        for (code in listOf(MfaDeletionError.PROVIDER_FAILED, MfaDeletionError.PROVIDER_NETWORK,
            MfaDeletionError.INVALID_TOKEN, MfaDeletionError.USER_CANCELLED)) {
            try {
                acquireDeletionToken(true, { throw MfaDeletionException(code) }, { error("Browser") })
                fail("Expected failure")
            } catch (e: MfaDeletionException) { assertEquals(code, e.code) }
        }
    }

    @Test fun silentOnlyNeverOpensBrowser() = runBlocking {
        try {
            acquireDeletionToken(false, {
                throw MfaDeletionException(MfaDeletionError.INTERACTION_REQUIRED)
            }, { error("Browser") })
            fail("Expected interaction required")
        } catch (e: MfaDeletionException) { assertEquals(MfaDeletionError.INTERACTION_REQUIRED, e.code) }
    }
}
