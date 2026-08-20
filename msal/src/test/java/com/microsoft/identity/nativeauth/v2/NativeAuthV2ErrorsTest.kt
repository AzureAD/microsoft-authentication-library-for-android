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
package com.microsoft.identity.nativeauth.v2

import com.microsoft.identity.nativeauth.statemachine.errors.MFARequestChallengeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.MFASubmitChallengeErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.RegisterStrongAuthChallengeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.RegisterStrongAuthSubmitChallengeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SignInErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitAttributesErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitNewPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitPasswordErrorV2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for the Native Auth V2 error types and their utility methods.
 */
@RunWith(RobolectricTestRunner::class)
class NativeAuthV2ErrorsTest {

    private val correlationId = "correlation-id"
    private val errorMessage = "error message"

    @Test
    fun testSignInErrorV2UtilityMethods() {
        assertTrue(
            SignInErrorV2(errorType = "user_not_found", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isUserNotFound()
        )
        assertTrue(
            SignInErrorV2(errorType = "invalid_credentials", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidCredentials()
        )
        assertTrue(
            SignInErrorV2(errorType = "invalid_username", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidUsername()
        )
        val error = SignInErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN)
        assertFalse(error.isUserNotFound())
        assertFalse(error.isInvalidCredentials())
        assertFalse(error.isInvalidUsername())
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, error.scenario)
    }

    @Test
    fun testSubmitPasswordErrorV2UtilityMethods() {
        assertTrue(
            SubmitPasswordErrorV2(errorType = "invalid_credentials", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidCredentials()
        )
        assertFalse(
            SubmitPasswordErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidCredentials()
        )
    }

    @Test
    fun testSignUpErrorV2UtilityMethods() {
        assertTrue(
            SignUpErrorV2(errorType = "user_already_exists", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isUserAlreadyExists()
        )
        assertTrue(
            SignUpErrorV2(errorType = "invalid_username", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidUsername()
        )
        assertTrue(
            SignUpErrorV2(errorType = "invalid_attributes", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidAttributes()
        )
        assertTrue(
            SignUpErrorV2(errorType = "invalid_password", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidPassword()
        )
        assertTrue(
            SignUpErrorV2(errorType = "auth_not_supported", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isAuthNotSupported()
        )
        val error = SignUpErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN)
        assertFalse(error.isUserAlreadyExists())
        assertFalse(error.isInvalidUsername())
        assertFalse(error.isInvalidAttributes())
        assertFalse(error.isInvalidPassword())
        assertFalse(error.isAuthNotSupported())
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
    }

    @Test
    fun testSubmitAttributesErrorV2UtilityMethods() {
        assertTrue(
            SubmitAttributesErrorV2(errorType = "invalid_attributes", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidAttributes()
        )
        assertFalse(
            SubmitAttributesErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidAttributes()
        )
    }

    @Test
    fun testResetPasswordErrorV2UtilityMethods() {
        assertTrue(
            ResetPasswordErrorV2(errorType = "user_not_found", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isUserNotFound()
        )
        assertTrue(
            ResetPasswordErrorV2(errorType = "invalid_username", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidUsername()
        )
        val error = ResetPasswordErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN)
        assertFalse(error.isUserNotFound())
        assertFalse(error.isInvalidUsername())
        assertEquals(NativeAuthFlowScenarioV2.RESET_PASSWORD, error.scenario)
    }

    @Test
    fun testSubmitNewPasswordErrorV2UtilityMethods() {
        assertTrue(
            SubmitNewPasswordErrorV2(errorType = "invalid_password", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidPassword()
        )
        assertTrue(
            SubmitNewPasswordErrorV2(errorType = "password_reset_failed", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isPasswordResetFailed()
        )
        val error = SubmitNewPasswordErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN, subError = "sub")
        assertFalse(error.isInvalidPassword())
        assertFalse(error.isPasswordResetFailed())
        assertEquals("sub", error.subError)
    }

    @Test
    fun testSubmitCodeErrorV2UtilityMethods() {
        assertTrue(
            SubmitCodeErrorV2(errorType = "invalid_code", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidCode()
        )
        val error = SubmitCodeErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN, subError = "sub")
        assertFalse(error.isInvalidCode())
        assertEquals("sub", error.subError)
    }

    @Test
    fun testMFARequestChallengeErrorV2UtilityMethods() {
        assertTrue(
            MFARequestChallengeErrorV2(errorType = "auth_method_blocked", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isAuthMethodBlocked()
        )
        val error = MFARequestChallengeErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN, subError = "sub")
        assertFalse(error.isAuthMethodBlocked())
        assertEquals("sub", error.subError)
    }

    @Test
    fun testMFASubmitChallengeErrorV2UtilityMethods() {
        assertTrue(
            MFASubmitChallengeErrorV2(errorType = "invalid_challenge", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidChallenge()
        )
        val error = MFASubmitChallengeErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN, subError = "sub")
        assertFalse(error.isInvalidChallenge())
        assertEquals("sub", error.subError)
    }

    @Test
    fun testRegisterStrongAuthChallengeErrorV2UtilityMethods() {
        assertTrue(
            RegisterStrongAuthChallengeErrorV2(errorType = "invalid_input", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidInput()
        )
        assertTrue(
            RegisterStrongAuthChallengeErrorV2(errorType = "verification_contact_blocked", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isVerificationContactBlocked()
        )
        val error = RegisterStrongAuthChallengeErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN)
        assertFalse(error.isInvalidInput())
        assertFalse(error.isVerificationContactBlocked())
    }

    @Test
    fun testRegisterStrongAuthSubmitChallengeErrorV2UtilityMethods() {
        assertTrue(
            RegisterStrongAuthSubmitChallengeErrorV2(errorType = "invalid_challenge", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidChallenge()
        )
        assertFalse(
            RegisterStrongAuthSubmitChallengeErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidChallenge()
        )
    }
}
