//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.e2e.utils

/**
 * Test-scope classifier for the Entra/CIAM email one-time-passcode throttle error.
 *
 * This is the Android port of the iOS classifier
 * `MSAL/test/unit/native_auth/utils/MSALNativeAuthEmailOTPErrorClassifier.swift`
 * (microsoft-authentication-library-for-objc), and is kept intentionally behaviour-compatible
 * with it: prefer the typed error codes, fall back to a description substring match.
 */
object NativeAuthEmailOTPErrorClassifier {

    /**
     * AADSTS701014 - the service could not generate another email one-time passcode yet, because
     * OTP issuance for this identity/tenant is currently being throttled.
     *
     * iOS phrases the equivalent skip message as
     * "AADSTS701014: CIAM could not generate another email OTP."
     *
     * It is a transient, service-side condition: the correct reaction in an E2E test is to back
     * off and retry rather than to fail the test.
     */
    const val THROTTLE_ERROR_CODE = 701014

    /**
     * Substring fallback marker, matched against an error's description/assertion text when the
     * typed [com.microsoft.identity.nativeauth.statemachine.errors.Error.errorCodes] list does not
     * itself carry [THROTTLE_ERROR_CODE].
     */
    const val THROTTLE_ERROR_DESCRIPTION_MARKER = "AADSTS$THROTTLE_ERROR_CODE"

    /**
     * Returns true when the supplied error represents the email OTP throttle.
     *
     * The typed [errorCodes] list is the primary, contract-based signal and is checked first, so
     * that classification does not depend on how an error was formatted into a string. The
     * [errorDescription] substring match is the fallback for the cases the typed list cannot cover.
     *
     * @param errorCodes the error codes returned by the authentication server, if available.
     * @param errorDescription the error message/description text, if available.
     */
    @JvmStatic
    fun isThrottleError(errorCodes: List<Int>?, errorDescription: String?): Boolean {
        if (errorCodes?.contains(THROTTLE_ERROR_CODE) == true) {
            return true
        }
        return errorDescription?.contains(THROTTLE_ERROR_DESCRIPTION_MARKER) ?: false
    }

    /**
     * Returns true when the supplied assertion failure represents the email OTP throttle.
     *
     * The typed [com.microsoft.identity.nativeauth.statemachine.errors.Error.errorCodes] list
     * carried by a [NativeAuthAssertionError] is the primary, contract-based signal.
     *
     * [Throwable.message] is the fallback and is consulted unconditionally rather than only when
     * the typed error is absent: [assertResult] embeds both the server's `error` and `errorMessage`
     * into the assertion message, so the message text is a strict superset of `errorMessage` for
     * the typed case and is the only available text for the untyped case. Using it unconditionally
     * keeps this fallback from ever being narrower than the plain string match it replaced.
     *
     * @param error the assertion failure thrown by the auth flow under test.
     */
    @JvmStatic
    fun isThrottleError(error: AssertionError): Boolean {
        val nativeAuthError = (error as? NativeAuthAssertionError)?.nativeAuthError
        return isThrottleError(
            errorCodes = nativeAuthError?.errorCodes,
            errorDescription = error.message
        )
    }
}
