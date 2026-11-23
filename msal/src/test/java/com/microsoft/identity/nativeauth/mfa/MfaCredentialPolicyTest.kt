// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.mfa

import com.microsoft.identity.nativeauth.mfa.internal.MfaApprovalBlockReason.*
import com.microsoft.identity.nativeauth.mfa.internal.MfaCredentialPolicy
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic protobuf fixtures reused from the public PoC, with conservative third-party policy. */
class MfaCredentialPolicyTest {
    private fun bytes(vararg values: Int) = values.map { it.toByte() }.toByteArray()
    private fun encode(value: ByteArray) = Base64.getUrlEncoder().encodeToString(value)
    private fun policy(vararg fields: Int) = encode(bytes(0x12, fields.size) + bytes(*fields))
    private fun reasons(value: String?, number: Boolean = true) =
        MfaCredentialPolicy.blockedReasons(value, number)

    @Test fun acceptsSupportedPushPoliciesAndBindsNumberRequirement() {
        for (mode in listOf(1, 2)) {
            assertTrue(reasons(policy(0x10, mode, 0x28, 2)).isEmpty())
            assertEquals(setOf(NUMBER_MATCHING_DATA_MISSING),
                reasons(policy(0x10, mode, 0x28, 2), number = false))
        }
        assertEquals(setOf(UNSUPPORTED_MODE), reasons(policy()))
        assertEquals(setOf(UNSUPPORTED_MODE), reasons(policy(0x10, 3)))
        assertEquals(setOf(MFA_DISABLED), reasons(policy(0x10, 1, 0x40, 1)))
        assertEquals(setOf(MFA_DISABLED), reasons(policy(0x10, 1, 0x40, 2)))
    }

    @Test fun requiredCapabilitiesAndServiceDefaultsDoNotAuthorizeApproval() {
        val fields = mapOf(0x18 to APP_CONTEXT_REQUIRED, 0x20 to LOCATION_CONTEXT_REQUIRED,
            0x30 to APP_LOCK_REQUIRED)
        for ((tag, reason) in fields) {
            assertEquals(setOf(reason), reasons(policy(0x10, 1, tag, 2)))
            assertEquals(setOf(UNKNOWN_POLICY), reasons(policy(0x10, 1, tag, 1)))
            assertTrue(reasons(policy(0x10, 1, tag, 0)).isEmpty())
        }
        assertEquals(setOf(APPROVAL_PIN_REQUIRED),
            reasons(encode(bytes(0x12, 2, 0x10, 1, 0x2a, 2, 0x08, 1))))
        assertEquals(setOf(APP_CONTEXT_REQUIRED, LOCATION_CONTEXT_REQUIRED, APP_LOCK_REQUIRED),
            reasons(policy(0x10, 1, 0x18, 2, 0x20, 2, 0x30, 2)))
    }

    @Test fun companionPolicyMustAlsoPermitTheDecision() {
        val primary = bytes(0x12, 6, 0x10, 2, 0x28, 2, 0x38, 2)
        fun combined(vararg companion: Int) =
            encode(primary + bytes(0x22, companion.size) + bytes(*companion))
        assertTrue(reasons(combined(0x10, 1, 0x28, 2)).isEmpty())
        assertEquals(setOf(NUMBER_MATCHING_DATA_MISSING),
            reasons(combined(0x10, 1, 0x28, 2), number = false))
        assertEquals(setOf(APP_LOCK_REQUIRED), reasons(combined(0x10, 1, 0x30, 2)))
        assertEquals(setOf(MFA_DISABLED), reasons(combined(0x10, 1, 0x40, 1)))
        assertEquals(setOf(UNKNOWN_POLICY), reasons(combined(0x10, 1, 0x48, 0)))
        assertEquals(setOf(UNSUPPORTED_MODE), reasons(combined()))
    }

    @Test fun failsClosedOnAmbiguousMalformedUnknownAndOversizedData() {
        val malformed = listOf(
            "not a policy", "====", "!", encode(bytes(0x12, 8, 0x10, 1)),
            encode(bytes(0x12, 2, 0x10, 1, 0x12, 2, 0x10, 2)),
            policy(0x10, 1, 0x10, 2), policy(0x10, 1, 0x32, 0),
            encode(bytes(0)), encode(ByteArray(16 * 1024 + 1)),
            policy(0x10, 0x80), policy(0x10, 1, 0x33, 0x34)
        )
        malformed.forEach { assertEquals(setOf(MALFORMED_POLICY), reasons(it)) }
        val unknown = listOf(policy(0x10, 4), policy(0x10, 1, 0x30, 3),
            policy(0x10, 1, 0x48, 0), encode(bytes(0x32, 0)),
            encode(bytes(0x0a, 0)), policy(0x10, 1, 0x28, 1))
        unknown.forEach { assertEquals(setOf(UNKNOWN_POLICY), reasons(it)) }
    }
}
