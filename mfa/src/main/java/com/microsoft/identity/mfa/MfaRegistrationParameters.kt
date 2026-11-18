// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.mfa

/** Activation input for enrollment. Never log the input or its individual properties. */
sealed class MfaRegistrationParameters {
    /**
     * Activation details obtained by scanning a QR code.
     * @property text Decoded QR text, not an image or a registered account identifier.
     */
    class QrCode(val text: String) : MfaRegistrationParameters() {
        /** Omits activation material. */
        override fun toString(): String = "QrCode(<redacted>)"
    }

    /**
     * Manually entered activation details.
     * @property code Activation code.
     * @property url Activation URL, subject to SDK validation before use.
     */
    class Activation(val code: String, val url: String) : MfaRegistrationParameters() {
        /** Omits activation material. */
        override fun toString(): String = "Activation(<redacted>)"
    }
}
