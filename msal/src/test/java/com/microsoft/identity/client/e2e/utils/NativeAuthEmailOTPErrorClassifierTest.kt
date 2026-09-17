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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.microsoft.identity.nativeauth.statemachine.errors.Error as NativeAuthError

/**
 * Pure JVM tests for [NativeAuthEmailOTPErrorClassifier], mirroring the iOS
 * `MSALNativeAuthEmailOTPErrorClassifierTests` coverage.
 */
class NativeAuthEmailOTPErrorClassifierTest {

    @Test
    fun testThrottleCodePresentInErrorCodesReturnsTrue() {
        assertTrue(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                errorCodes = listOf(NativeAuthEmailOTPErrorClassifier.THROTTLE_ERROR_CODE),
                errorDescription = null
            )
        )
    }

    @Test
    fun testThrottleCodePresentAmongOtherErrorCodesReturnsTrue() {
        assertTrue(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                errorCodes = listOf(50034, NativeAuthEmailOTPErrorClassifier.THROTTLE_ERROR_CODE),
                errorDescription = "some unrelated description"
            )
        )
    }

    @Test
    fun testThrottleCodeAbsentFromErrorCodesReturnsFalse() {
        assertFalse(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                errorCodes = listOf(50034, 50126),
                errorDescription = "AADSTS50034: The user account does not exist."
            )
        )
    }

    @Test
    fun testDescriptionOnlyMatchReturnsTrue() {
        assertFalse(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                errorCodes = null,
                errorDescription = "AADSTS50034: The user account does not exist."
            )
        )
        assertTrue(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                errorCodes = null,
                errorDescription = "AADSTS701014: CIAM could not generate another email OTP."
            )
        )
    }

    @Test
    fun testEmptyErrorCodesFallsBackToDescription() {
        assertTrue(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                errorCodes = emptyList(),
                errorDescription = "AADSTS701014: CIAM could not generate another email OTP."
            )
        )
        assertFalse(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                errorCodes = emptyList(),
                errorDescription = "some unrelated description"
            )
        )
    }

    @Test
    fun testNullErrorCodesAndNullDescriptionReturnsFalse() {
        assertFalse(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                errorCodes = null,
                errorDescription = null
            )
        )
    }

    @Test
    fun testThrottleErrorCodeConstantMatchesServiceContract() {
        assertEquals(701014, NativeAuthEmailOTPErrorClassifier.THROTTLE_ERROR_CODE)
        assertEquals("AADSTS701014", NativeAuthEmailOTPErrorClassifier.THROTTLE_ERROR_DESCRIPTION_MARKER)
    }

    private fun nativeAuthError(
        error: String?,
        errorMessage: String?,
        errorCodes: List<Int>?
    ) = NativeAuthError(
        error = error,
        errorMessage = errorMessage,
        correlationId = "UNSET",
        errorCodes = errorCodes
    )

    @Test
    fun testAssertionErrorWithTypedThrottleCodeIsThrottled() {
        val error = nativeAuthError(
            error = "invalid_grant",
            errorMessage = "Something the marker does not appear in",
            errorCodes = listOf(NativeAuthEmailOTPErrorClassifier.THROTTLE_ERROR_CODE)
        )
        assertTrue(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                NativeAuthAssertionError("Type comparison failed.", error)
            )
        )
    }

    /**
     * Regression pin: when the carried error has no typed codes, classification must still fall
     * back to the assertion message. Reading only `Error.errorMessage` here would be narrower than
     * the plain `e.message.contains("AADSTS701014")` check this classifier replaced.
     */
    @Test
    fun testAssertionErrorWithoutTypedCodesFallsBackToAssertionMessage() {
        val error = nativeAuthError(error = null, errorMessage = null, errorCodes = null)
        assertTrue(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                NativeAuthAssertionError(
                    "Type comparison failed. Error: null - AADSTS701014: CIAM could not generate another email OTP.",
                    error
                )
            )
        )
    }

    /**
     * Regression pin: the marker may arrive in the server's `error` field rather than in
     * `errorMessage`. [assertResult] embeds both into the assertion message, so classifying off the
     * message keeps this case retryable.
     */
    @Test
    fun testAssertionErrorWithMarkerOnlyInErrorFieldIsThrottled() {
        val error = nativeAuthError(
            error = "AADSTS701014",
            errorMessage = "Request throttled.",
            errorCodes = listOf(50034)
        )
        assertTrue(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                NativeAuthAssertionError(
                    "Type comparison failed. Error: AADSTS701014 - Request throttled.",
                    error
                )
            )
        )
    }

    @Test
    fun testNonThrottleAssertionErrorIsNotThrottled() {
        val error = nativeAuthError(
            error = "invalid_grant",
            errorMessage = "AADSTS50034: The user account does not exist.",
            errorCodes = listOf(50034)
        )
        assertFalse(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                NativeAuthAssertionError(
                    "Type comparison failed. Error: invalid_grant - AADSTS50034: The user account does not exist.",
                    error
                )
            )
        )
    }

    @Test
    fun testPlainAssertionErrorIsClassifiedFromItsMessage() {
        assertTrue(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(
                AssertionError("AADSTS701014: CIAM could not generate another email OTP.")
            )
        )
        assertFalse(
            NativeAuthEmailOTPErrorClassifier.isThrottleError(AssertionError("expected:<1> but was:<2>"))
        )
        assertFalse(NativeAuthEmailOTPErrorClassifier.isThrottleError(AssertionError()))
    }
}
