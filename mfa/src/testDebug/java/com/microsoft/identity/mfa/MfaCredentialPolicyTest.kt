// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import com.microsoft.identity.mfa.MfaApprovalBlockReason.*
import com.microsoft.identity.mfa.internal.MfaCredentialPolicy
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Only synthetic wire values; no captured policy data or private generated classes. */
class MfaCredentialPolicyTest {
    private fun bytes(vararg values: Int) = values.map { it.toByte() }.toByteArray()
    private fun encode(value: ByteArray) = Base64.getUrlEncoder().encodeToString(value)
    private fun policy(vararg fields: Int) = encode(bytes(0x12, fields.size) + bytes(*fields))
    private fun reasons(value: String?, number: Boolean = true) =
        MfaCredentialPolicy.blockedReasons(value, number)

    @Test fun acceptsOnlySupportedPushPoliciesAndBindsNumberRequirement() {
        for (mode in listOf(1, 2)) {
            assertTrue(reasons(policy(0x10, mode, 0x28, 2)).isEmpty())
            assertEquals(setOf(NUMBER_MATCHING_DATA_MISSING),
                reasons(policy(0x10, mode, 0x28, 2), number = false))
            assertTrue(reasons(policy(0x10, mode, 0x28, 0), number = false).isEmpty())
        }
        assertTrue(reasons(null).isEmpty())
        assertTrue(reasons("").isEmpty())
        assertEquals(setOf(UNSUPPORTED_MODE), reasons(policy()))
        assertEquals(setOf(UNSUPPORTED_MODE), reasons(policy(0x10, 3)))
        assertEquals(setOf(MFA_DISABLED), reasons(policy(0x10, 1, 0x40, 1)))
        assertEquals(setOf(MFA_DISABLED), reasons(policy(0x10, 1, 0x40, 2)))
    }

    @Test fun distinguishesRequirementsInsteadOfIgnoringEncodedPolicy() {
        val fields = mapOf(0x18 to APP_CONTEXT_REQUIRED, 0x20 to LOCATION_CONTEXT_REQUIRED,
            0x30 to APP_LOCK_REQUIRED)
        for ((tag, reason) in fields) {
            assertEquals(setOf(reason), reasons(policy(0x10, 1, tag, 2)))
            assertEquals(setOf(UNKNOWN_POLICY), reasons(policy(0x10, 1, tag, 1)))
            assertTrue(reasons(policy(0x10, 1, tag, 0)).isEmpty())
        }
        assertEquals(setOf(APPROVAL_PIN_REQUIRED),
            reasons(encode(bytes(0x12, 2, 0x10, 1, 0x2a, 2, 0x08, 1))))
        assertTrue(reasons(encode(bytes(0x12, 2, 0x10, 1, 0x2a, 2, 0x08, 0))).isEmpty())
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
        val primaryRequiresLock = bytes(0x12, 4, 0x10, 1, 0x30, 2)
        assertEquals(setOf(APP_LOCK_REQUIRED),
            reasons(encode(primaryRequiresLock + bytes(0x22, 2, 0x10, 1))))
    }

    @Test fun authenticatorProfileUsesPrimaryPolicyAndRetainsApplicableRestrictions() {
        // Companion mode is omitted: its proto default must not override the primary ANY mode.
        val companion = bytes(0x22, 4, 0x28, 2, 0x38, 2)
        val primary = bytes(0x12, 4, 0x10, 2, 0x28, 2)
        fun selected(value: ByteArray) = MfaCredentialPolicy.blockedReasons(
            encode(value), true, MfaPolicyProfile.AUTHENTICATOR)
        assertTrue(selected(primary + companion).isEmpty())
        assertEquals(setOf(UNSUPPORTED_MODE), reasons(encode(primary + companion)))
        assertEquals(setOf(APP_LOCK_REQUIRED),
            selected(bytes(0x12, 4, 0x10, 2, 0x30, 2) + companion))
        assertEquals(setOf(APPROVAL_PIN_REQUIRED),
            selected(primary + companion + bytes(0x2a, 2, 0x08, 1)))
        assertEquals(setOf(MFA_DISABLED),
            selected(bytes(0x12, 4, 0x10, 2, 0x40, 1) + companion))
        assertEquals(setOf(UNKNOWN_POLICY), selected(companion))
        assertEquals(setOf(UNKNOWN_POLICY), selected(primary + bytes(0x22, 2, 0x48, 0)))
        assertEquals(setOf(MALFORMED_POLICY), selected(primary + bytes(0x22, 9, 0x28, 2)))
    }

    @Test fun authenticatorProfileRequiresMatchingHostIdentity() {
        MfaPolicyProfile.AUTHENTICATOR.validateHost("com.azure.authenticator")
        MfaPolicyProfile.CONSERVATIVE.validateHost("example.poc")
        for (host in listOf("example.poc", "com.azure.authenticator.other")) {
            try {
                MfaPolicyProfile.AUTHENTICATOR.validateHost(host)
                throw AssertionError("Mismatched policy profile must be rejected")
            } catch (error: MfaSdkException) {
                assertEquals(MfaErrorCode.INVALID_CONFIGURATION, error.code)
            }
        }
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

    @Test fun recognizedMetadataAndNonApprovalSettingsDoNotExposeOrBlock() {
        // Empty version, timestamp, disabled reporting, and software TOTP enabled.
        val value = bytes(0x0a, 4, 0x0a, 0, 0x12, 0,
            0x12, 6, 0x08, 1, 0x10, 1, 0x38, 2,
            0x1a, 2, 0x08, 0)
        assertTrue(reasons(encode(value)).isEmpty())
    }

    @Test fun sessionBlockReasonsAreImmutableAndDetachedFromCaller() {
        val source = mutableSetOf(APP_LOCK_REQUIRED)
        val session = MfaAuthSession("synthetic", "synthetic", "synthetic", true, source)
        source.clear()
        assertFalse(session.canApprove)
        assertEquals(setOf(APP_LOCK_REQUIRED), session.approvalBlockedReasons)
        try {
            (session.approvalBlockedReasons as MutableSet).clear()
            throw AssertionError("Block reasons must not be mutable")
        } catch (_: UnsupportedOperationException) {
            assertFalse(session.canApprove)
        }
    }
}
