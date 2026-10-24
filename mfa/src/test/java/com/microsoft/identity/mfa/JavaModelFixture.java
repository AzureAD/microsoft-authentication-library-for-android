// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Compile fixture for the host-facing Java getters and immutable collections. */
final class JavaModelFixture {
    static void verify(final MfaAuthSession session, final MfaDisplayMetadata display) {
        final MfaRegistrationParameters.QrCode qr = new MfaRegistrationParameters.QrCode("synthetic");
        final MfaRegistrationParameters.Activation activation =
                new MfaRegistrationParameters.Activation("synthetic", "https://example.invalid");
        final MfaSdkStatus status =
                new MfaSdkStatus(MfaEnrollmentState.NOT_ENROLLED, MfaPushTokenState.AVAILABLE);
        assertEquals("synthetic", qr.getText());
        assertEquals("synthetic", activation.getCode());
        assertEquals("https://example.invalid", activation.getUrl());
        assertEquals(MfaPushTokenState.AVAILABLE, status.getPushTokenState());
        assertEquals(MfaEnrollmentState.NOT_ENROLLED, status.getEnrollmentState());
        assertEquals(display, session.getDisplayMetadata());
        assertEquals(display.getAccountName(), display.getUsername());
        assertEquals(session.getRegistrationId(), session.getSessionHandle());
        assertEquals(1234L, session.getExpiresAtEpochMillis());
        assertTrue(session.getRequiresNumberMatching());
        assertThrows(UnsupportedOperationException.class, () -> session.getApprovalBlockedReasons().clear());
        assertEquals(MfaErrorCategory.UNAVAILABLE,
                new MfaSdkException(MfaErrorCategory.UNAVAILABLE).getCategory());
    }
}
