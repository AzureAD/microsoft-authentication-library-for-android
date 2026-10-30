// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Compile fixture for the host-facing Java getters and immutable collections. */
final class JavaModelFixture {
    static void verify(final MfaAuthSession session, final MfaRegistrationResult result) {
        final MfaRegistrationParameters.QrCode qr = new MfaRegistrationParameters.QrCode("synthetic");
        final MfaRegistrationParameters.Activation activation =
                new MfaRegistrationParameters.Activation("synthetic", "https://example.invalid");
        assertEquals("synthetic", qr.getText());
        assertEquals("synthetic", activation.getCode());
        assertEquals("https://example.invalid", activation.getUrl());
        assertEquals("registration-id", result.getRegistrationId());
        assertEquals(result.getDisplayMetadata(), session.getDisplayMetadata());
        assertEquals("Account label", session.getDisplayMetadata().getAccountName());
        assertEquals("User label", session.getDisplayMetadata().getUsername());
        assertEquals("registration-id", session.getRegistrationId());
        assertEquals("session-handle", session.getSessionHandle());
        assertEquals(1234L, session.getExpiresAtEpochMillis());
        assertTrue(session.getRequiresNumberMatching());
        assertThrows(UnsupportedOperationException.class, () -> session.getApprovalBlockedReasons().clear());
        assertEquals(MfaDecision.APPROVED, MfaDecision.valueOf("APPROVED"));
        assertEquals(MfaDecision.DENIED, MfaDecision.valueOf("DENIED"));
        assertEquals(MfaErrorCategory.UNAVAILABLE,
                new MfaSdkException(MfaErrorCategory.UNAVAILABLE).getCategory());
    }
}
