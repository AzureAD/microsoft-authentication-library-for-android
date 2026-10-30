// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MfaModelsTest {
    private val marker = "synthetic-sensitive-marker"
    private val display = MfaDisplayMetadata(marker, marker)

    @Test
    fun sensitiveInputsAndResultsDoNotRenderValuesOrGenerateCopies() {
        val qr = MfaRegistrationParameters.QrCode(marker)
        val activation = MfaRegistrationParameters.Activation(marker, "https://example.invalid/$marker")
        val result = MfaRegistrationResult(marker, display)
        val session = session(emptySet())
        val values = listOf(qr, activation, display, result, session)

        values.forEach { value ->
            assertFalse(value.toString().contains(marker))
            assertFalse(value.javaClass.methods.any { it.name == "copy" || it.name == "component1" })
        }
        assertEquals(marker, qr.text)
        assertEquals(marker, activation.code)
        assertEquals("https://example.invalid/$marker", activation.url)
        assertEquals(marker, result.registrationId)
    }

    @Test
    fun errorsOnlyAcceptBoundedCategories() {
        MfaErrorCategory.values().forEach { category ->
            val error = MfaSdkException(category)
            assertEquals(category.name, error.message)
            assertEquals(category, error.category)
            assertNull(error.cause)
            assertFalse(error.toString().contains(marker))
            assertThrows(IllegalStateException::class.java) {
                error.initCause(IllegalArgumentException(marker))
            }
        }
    }

    @Test
    fun sessionSnapshotsBlockReasonsAndExposesOnlySafeMetadata() {
        val source = mutableSetOf(MfaApprovalBlockReason.UNKNOWN_POLICY)
        val session = session(source)
        source.clear()
        assertEquals(setOf(MfaApprovalBlockReason.UNKNOWN_POLICY), session.approvalBlockedReasons)
        assertThrows(UnsupportedOperationException::class.java) {
            (session.approvalBlockedReasons as MutableSet<MfaApprovalBlockReason>).clear()
        }
        assertEquals(marker, session.sessionHandle)
        assertEquals(marker, session.registrationId)
        assertEquals(1234L, session.expiresAtEpochMillis)
        assertTrue(session.requiresNumberMatching)
        assertEquals(marker, session.displayMetadata.username)
    }

    @Test
    fun sessionHasNoExpectedAnswerCredentialOrAuthenticationResult() {
        assertEquals(
            setOf(
                "sessionHandle", "registrationId", "displayMetadata", "expiresAtEpochMillis",
                "requiresNumberMatching", "approvalBlockedReasons"
            ),
            MfaAuthSession::class.java.declaredFields.filterNot { it.isSynthetic }.map { it.name }.toSet()
        )
        assertEquals(
            setOf("registrationId", "displayMetadata"),
            MfaRegistrationResult::class.java.declaredFields.filterNot { it.isSynthetic }
                .map { it.name }.toSet()
        )
        assertFalse(MfaAuthSession::class.java.methods.any { it.name.startsWith("set") })
        assertFalse(MfaDisplayMetadata::class.java.methods.any { it.name.startsWith("set") })
    }

    @Test
    fun publicModelsAreUsableFromJava() {
        val metadata = MfaDisplayMetadata("Account label", "User label")
        val session = MfaAuthSession(
            "session-handle", "registration-id", metadata, 1234L, true,
            setOf(MfaApprovalBlockReason.UNKNOWN_POLICY)
        )
        JavaModelFixture.verify(session, MfaRegistrationResult("registration-id", metadata))
    }

    @Test
    fun registrationInputsRemainUnparsedUntilAnEnrollmentImplementationConsumesThem() {
        val qr = MfaRegistrationParameters.QrCode("")
        val activation = MfaRegistrationParameters.Activation("", "not-a-url")
        assertEquals("", qr.text)
        assertEquals("", activation.code)
        assertEquals("not-a-url", activation.url)
    }

    private fun session(reasons: Set<MfaApprovalBlockReason>) = MfaAuthSession(
        marker, marker, display, 1234L, true, reasons
    )
}
