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

import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
    fun unifiedError_localErrorsHaveNoServiceDiagnostics() {
        val error = NativeAuthErrorV2(
            errorMessage = errorMessage, correlationId = correlationId,
            scenario = NativeAuthFlowScenarioV2.SIGN_IN
        )
        assertNull(error.traceId)
        assertNull(error.timestamp)
    }

    @Test
    fun unifiedError_credentialResponseNeedsPasswordSubmissionContext() {
        val error = NativeAuthErrorV2(
            error = "invalidGrant", subError = "invalidUserNameOrPassword",
            errorMessage = errorMessage, correlationId = correlationId,
            scenario = NativeAuthFlowScenarioV2.SIGN_IN
        )
        assertFalse(error.isInvalidPassword())
        assertTrue(error.isGeneralError())
    }

    @Test
    fun unifiedError_passwordDetailsCannotReplacePasswordSubmissionContext() {
        val error = NativeAuthErrorV2(
            error = "invalidGrant",
            subError = "invalidUserNameOrPassword",
            errorMessage = errorMessage,
            correlationId = correlationId,
            scenario = NativeAuthFlowScenarioV2.SIGN_IN,
            attributeValidationDetails = listOf(
                mapOf(
                    "code" to "passwordPolicyViolation",
                    "attributeIds" to listOf("password")
                )
            )
        )

        assertFalse(error.isInvalidPassword())
        assertTrue(error.isGeneralError())
    }

    @Test
    fun unifiedError_validatesAttributeDetailShapes() {
        fun error(details: Any?) = NativeAuthErrorV2(
            errorMessage = errorMessage, correlationId = correlationId,
            scenario = NativeAuthFlowScenarioV2.SIGN_UP,
            error = "invalidRequest", subError = "attributeValidationError",
            attributeValidationDetails = details
        )
        assertTrue(error(listOf(mapOf("code" to "attributeRequired"))).isMissingRequiredAttributes())
        assertTrue(error(listOf(mapOf("attributeIds" to listOf(42, "password")))).isInvalidPassword())
        listOf(null, "bad", listOf(null, 42, mapOf("code" to 42)),
            listOf(mapOf("code" to "passwordPolicyViolation", "attributeIds" to "password")),
            listOf(mapOf("attributeIds" to listOf("PASSWORD"))),
            listOf(mapOf("code" to "passwordPolicyViolation", "attributeIds" to listOf("other"))))
            .forEach { assertFalse(error(it).isInvalidPassword()) }
        assertTrue(error("bad").isInvalidAttributes())
    }

    @Test
    fun unifiedError_passwordValidationUsesStructuredFieldsNotDetailCodesOrMessages() {
        val details = listOf(mapOf("code" to "unrelated", "attributeIds" to listOf("password", "city")))
        for (scenario in listOf(NativeAuthFlowScenarioV2.SIGN_UP, NativeAuthFlowScenarioV2.RESET_PASSWORD)) {
            for (subError in listOf("passwordTooWeak", "passwordTooShort", "passwordTooLong",
                "passwordIsInvalid", "passwordRecentlyUsed", "passwordBanned", "attributeValidationError")) {
                val error = NativeAuthErrorV2(
                    error = "invalidRequest", subError = subError, attributeValidationDetails = details,
                    errorMessage = "Unrelated description", correlationId = correlationId, scenario = scenario
                )
                assertTrue(error.isInvalidPassword())
            }
        }
        for ((code, subError) in listOf(
            "invalidGrant" to "attributeValidationError",
            "invalidRequest" to "unrelated",
            "invalidRequest" to null
        )) {
            assertFalse(NativeAuthErrorV2(
                error = code, subError = subError, attributeValidationDetails = details,
                errorMessage = "passwordPolicyViolation", correlationId = correlationId,
                scenario = NativeAuthFlowScenarioV2.SIGN_UP
            ).isInvalidPassword())
        }
    }

    @Test
    fun unifiedError_usernameClassificationIsLocalOnly() {
        val serviceError = NativeAuthErrorV2(
            error = "invalidRequest", errorMessage = "AADSTS90100: invalid username",
            correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.SIGN_IN
        )
        assertFalse(serviceError.isInvalidUsername())
        assertTrue(serviceError.isGeneralError())
        assertTrue(NativeAuthErrorV2(
            errorType = ErrorTypes.INVALID_USERNAME, errorMessage = "Empty or blank username",
            correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.SIGN_IN
        ).isInvalidUsername())
    }

    @Test
    fun unifiedError_expiredSessionRemainsGeneralAcrossScenarios() {
        for (scenario in NativeAuthFlowScenarioV2.values()) {
            val error = NativeAuthErrorV2(
                error = "invalidRequest", subError = "sessionExpiredOrInvalid",
                errorMessage = "AADSTS400004: The session has expired or is invalid. Please try again.",
                correlationId = correlationId, scenario = scenario
            )
            assertTrue(error.isGeneralError())
        }
    }

    @Test
    fun unifiedError_userNotFoundRequiresStructuredCodes() {
        for ((code, subError, expected) in listOf(
            Triple("invalidRequest", "userNotFound", true),
            Triple("invalidGrant", "userNotFound", false),
            Triple("invalidRequest", null, false),
            Triple("invalidRequest", "unrelated", false)
        )) {
            val error = NativeAuthErrorV2(
                error = code, subError = subError, errorMessage = "AADSTS50034: user not found",
                correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.SIGN_IN
            )
            assertEquals(expected, error.isUserNotFound())
        }
    }

    @Test
    fun unifiedError_browserFallbackRequiresExactOAuthPair() {
        for ((code, subError, expected) in listOf(
            Triple("invalid_grant", "web_redirect_required", true),
            Triple("invalid_grant", null, false),
            Triple("invalidGrant", "web_redirect_required", false),
            Triple("invalid_request", "web_redirect_required", false),
            Triple("redirect_to_web", "web_redirect_required", false),
            Triple("webFallbackRequired", null, false)
        )) {
            val error = NativeAuthErrorV2(
                error = code, subError = subError, errorMessage = "web_redirect_required",
                errorCodes = listOf(50196), correlationId = correlationId,
                scenario = NativeAuthFlowScenarioV2.SIGN_IN,
                traceId = "trace", timestamp = "time"
            )
            assertEquals(expected, error.isBrowserRequired())
            assertEquals(!expected, error.isGeneralError())
            assertEquals(code, error.error)
            assertEquals(subError, error.subError)
            assertEquals("trace", error.traceId)
            assertEquals("time", error.timestamp)
        }
    }

    @Test
    fun unifiedError_challengeAndOrdinaryCodeClassificationsAreDistinct() {
        for (type in listOf(ErrorTypes.INVALID_CODE, ErrorTypes.INVALID_CHALLENGE)) {
            val error = NativeAuthErrorV2(
                errorType = type, error = "invalidGrant", subError = "invalidOneTimeCode",
                errorMessage = errorMessage, correlationId = correlationId,
                scenario = NativeAuthFlowScenarioV2.SIGN_IN
            )
            assertEquals(type == ErrorTypes.INVALID_CODE, error.isInvalidCode())
            assertEquals(type == ErrorTypes.INVALID_CHALLENGE, error.isInvalidChallenge())
        }
    }

    @Test
    fun testSignInErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "user_not_found", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isUserNotFound()
        )
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_username", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidUsername()
        )
        val error = NativeAuthErrorV2(
            errorType = "other",
            errorMessage = errorMessage,
            correlationId = correlationId,
            scenario = NativeAuthFlowScenarioV2.UNKNOWN,
            subError = "sub"
        )
        assertFalse(error.isUserNotFound())
        assertFalse(error.isInvalidUsername())
        assertEquals(NativeAuthFlowScenarioV2.UNKNOWN, error.scenario)
        assertEquals("sub", error.subError)
    }

    @Test
    fun testSubmitPasswordErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_password", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidPassword()
        )
        assertFalse(
            NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidPassword()
        )
    }

    @Test
    fun testSignUpErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_username", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidUsername()
        )
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_attributes", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidAttributes()
        )
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_password", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidPassword()
        )
        assertTrue(
            NativeAuthErrorV2(errorType = "auth_not_supported", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isAuthNotSupported()
        )
        val error = NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN)
        assertFalse(error.isInvalidUsername())
        assertFalse(error.isInvalidAttributes())
        assertFalse(error.isInvalidPassword())
        assertFalse(error.isAuthNotSupported())
        assertEquals(NativeAuthFlowScenarioV2.UNKNOWN, error.scenario)
    }

    @Test
    fun testSubmitAttributesErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_attributes", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidAttributes()
        )
        assertFalse(
            NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidAttributes()
        )
    }

    @Test
    fun testInvalidAttributesClassifierPreservesServiceValidation() {
        val errors = listOf(
            NativeAuthErrorV2(
                errorType = "invalid_attributes", errorMessage = errorMessage,
                correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.SIGN_UP),
            NativeAuthErrorV2(
                subError = "attributeValidationError", errorMessage = errorMessage,
                correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.SIGN_UP),
            NativeAuthErrorV2(
                attributeValidationDetails = listOf(mapOf("code" to "attributeRequired")),
                errorMessage = errorMessage, correlationId = correlationId,
                scenario = NativeAuthFlowScenarioV2.SIGN_UP)
        )
        errors.forEach {
            assertTrue(it.isInvalidAttributes())
            assertFalse(it.isGeneralError())
        }
        val unrelated = NativeAuthErrorV2(
            errorMessage = errorMessage, correlationId = correlationId,
            scenario = NativeAuthFlowScenarioV2.SIGN_UP,
            attributeValidationDetails = listOf("malformed", mapOf("code" to "unknown"))
        )
        assertFalse(unrelated.isInvalidAttributes())
        assertTrue(unrelated.isGeneralError())
    }

    @Test
    fun testResetPasswordErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "user_not_found", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isUserNotFound()
        )
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_username", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidUsername()
        )
        val error = NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN)
        assertFalse(error.isUserNotFound())
        assertFalse(error.isInvalidUsername())
        assertEquals(NativeAuthFlowScenarioV2.UNKNOWN, error.scenario)
    }

    @Test
    fun testSubmitNewPasswordErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_password", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidPassword()
        )
        assertTrue(
            NativeAuthErrorV2(errorType = "password_reset_failed", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isPasswordResetFailed()
        )
        val error = NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN, subError = "sub")
        assertFalse(error.isInvalidPassword())
        assertFalse(error.isPasswordResetFailed())
        assertEquals("sub", error.subError)
    }

    @Test
    fun testNativeAuthV2ErrorsDoNotExposeRetryState() {
        assertFalse(
            "NativeAuthErrorV2 must not expose a continuation or retry state",
            NativeAuthErrorV2::class.java.methods.any {
                it.name in setOf("getNextState", "getRetryState", "getContinuationState")
            }
        )
    }

    @Test
    fun testSubmitCodeErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_code", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidCode()
        )
        val error = NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN, subError = "sub")
        assertFalse(error.isInvalidCode())
        assertEquals("sub", error.subError)
    }

    @Test
    fun removedV2ClassifiersRemainGeneralErrorsWithoutChangingV1() {
        val removedMethods = setOf("isUserAlreadyExists", "isAuthMethodBlocked",
            "isVerificationContactBlocked", "isUserDoesNotHavePassword", "isInvalidCredentials")
        assertFalse(NativeAuthErrorV2::class.java.methods.any { it.name in removedMethods })
        assertTrue(com.microsoft.identity.nativeauth.statemachine.errors.SignUpError::class.java
            .methods.any { it.name == "isUserAlreadyExists" })
        assertTrue(com.microsoft.identity.nativeauth.statemachine.errors.SignInError(
            errorType = "invalid_credentials", errorMessage = errorMessage, correlationId = correlationId
        ).isInvalidCredentials())
        assertFalse(com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult::class.java
            .declaredClasses.any { it.simpleName == "InvalidCredentials" })
        for (type in listOf("user_already_exists", "auth_method_blocked", "verification_contact_blocked",
            "invalid_credentials")) {
            assertTrue(NativeAuthErrorV2(
                errorType = type, errorMessage = errorMessage, correlationId = correlationId,
                scenario = NativeAuthFlowScenarioV2.UNKNOWN
            ).isGeneralError())
        }
    }

    @Test
    fun testMFASubmitChallengeErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = ErrorTypes.INVALID_CHALLENGE, errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidChallenge()
        )
        val error = NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN, subError = "sub")
        assertFalse(error.isInvalidChallenge())
        assertEquals("sub", error.subError)
    }

    @Test
    fun testRegisterStrongAuthChallengeErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_input", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidInput()
        )
        val error = NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN)
        assertFalse(error.isInvalidInput())
    }

    @Test
    fun testRegisterStrongAuthSubmitChallengeErrorV2UtilityMethods() {
        assertTrue(
            NativeAuthErrorV2(errorType = "invalid_challenge", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidChallenge()
        )
        assertFalse(
            NativeAuthErrorV2(errorType = "other", errorMessage = errorMessage, correlationId = correlationId, scenario = NativeAuthFlowScenarioV2.UNKNOWN).isInvalidChallenge()
        )
    }

    /**
     * Constructs every V2 error type through its full (all-arguments) primary constructor and
     * verifies the values passed to the shared [NativeAuthErrorV2] base are exposed. This exercises
     * the constructor path that omits Kotlin's synthesized default-argument handling, which the
     * other tests (relying on defaults) do not cover.
     */
    @Test
    fun testErrorsExposeAllConstructorFields() {
        val error = "server_error"
        val errorCodes = listOf(1, 2, 3)
        val exception = MsalClientException("client_error", "boom")
        val scenario = NativeAuthFlowScenarioV2.SIGN_IN

        val allErrors = listOf(
            NativeAuthErrorV2("type", error, errorMessage, correlationId, scenario, errorCodes, exception, "sub")
        )

        for (built in allErrors) {
            assertEquals(error, built.error)
            assertEquals(errorMessage, built.errorMessage)
            assertEquals(correlationId, built.correlationId)
            assertEquals(errorCodes, built.errorCodes)
            assertSame(exception, built.exception)
            assertEquals(scenario, built.scenario)
            assertFalse(built.isNotImplemented())
        }
    }

    /**
     * Constructs every error type while omitting the optional [errorType] argument so the
     * default-value (errorType = null) branch of the synthesized constructor is exercised.
     */
    @Test
    fun testErrorsDefaultErrorTypeToNull() {
        val scenario = NativeAuthFlowScenarioV2.UNKNOWN

        val allErrors = listOf<NativeAuthErrorV2>(
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario),
            NativeAuthErrorV2(errorMessage = errorMessage, correlationId = correlationId, scenario = scenario)
        )

        for (built in allErrors) {
            assertEquals(errorMessage, built.errorMessage)
            assertEquals(correlationId, built.correlationId)
            assertEquals(scenario, built.scenario)
        }
    }
}
