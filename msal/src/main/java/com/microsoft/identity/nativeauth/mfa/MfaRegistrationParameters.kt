// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.identity.nativeauth.mfa

/**
 * Activation input for enrollment. Never log the input or its individual properties.
 *
 * Warning: This API is experimental. It may be changed in the future without notice.
 * Do not use in production applications.
 */
@ExperimentalMfaApi
sealed class MfaRegistrationParameters {
    /**
     * Activation details obtained by scanning a QR code.
     *
     * Warning: This API is experimental. It may be changed in the future without notice.
     * Do not use in production applications.
     * @property text Decoded QR text, not an image or a registered account identifier.
     */
    @ExperimentalMfaApi
    class QrCode(val text: String) : MfaRegistrationParameters() {
        override fun equals(other: Any?): Boolean = other is QrCode && text == other.text

        override fun hashCode(): Int = text.hashCode()

        /** Omits activation material. */
        override fun toString(): String = "QrCode(<redacted>)"
    }

    /**
     * Manually entered activation details.
     *
     * Warning: This API is experimental. It may be changed in the future without notice.
     * Do not use in production applications.
     * @property code Activation code.
     * @property url Activation URL, subject to SDK validation before use.
     */
    @ExperimentalMfaApi
    class Activation(val code: String, val url: String) : MfaRegistrationParameters() {
        override fun equals(other: Any?): Boolean =
            other is Activation && code == other.code && url == other.url

        override fun hashCode(): Int = 31 * code.hashCode() + url.hashCode()

        /** Omits activation material. */
        override fun toString(): String = "Activation(<redacted>)"
    }
}
