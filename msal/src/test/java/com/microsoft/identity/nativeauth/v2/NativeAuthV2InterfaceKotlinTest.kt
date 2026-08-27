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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.parameters.NativeAuthResetPasswordParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesInvalidStateV2
import com.microsoft.identity.nativeauth.statemachine.states.AttributesRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.CodeRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFARequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.MFAVerificationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.NewPasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.PasswordRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthRegistrationRequiredStateV2
import com.microsoft.identity.nativeauth.statemachine.states.StrongAuthVerificationRequiredStateV2
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
class NativeAuthV2InterfaceKotlinTest : PublicClientApplicationAbstractTest() {

    private lateinit var context: Context
    private lateinit var application: INativeAuthPublicClientApplication
    private val username = "user@email.com"

    override fun getConfigFilePath() = "src/test/res/raw/native_auth_native_only_test_config.json"

    @Before
    override fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val configFile = File(getConfigFilePath())
        try {
            application = PublicClientApplication.createNativeAuthPublicClientApplication(context, configFile)
        } catch (e: MsalException) {
            fail(e.message)
        }
    }

    @Test
    fun signInV2ReturnsNotImplemented() = runTest {
        val result = application.signInV2(NativeAuthSignInParameters(username = username))
        val error = assertNotImplemented(result)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_IN, error.scenario)
    }

    @Test
    fun signUpV2ReturnsNotImplemented() = runTest {
        val result = application.signUpV2(NativeAuthSignUpParameters(username = username))
        val error = assertNotImplemented(result, NativeAuthFlowScenarioV2.SIGN_UP)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
    }

    @Test
    fun resetPasswordV2ReturnsNotImplemented() = runTest {
        val result = application.resetPasswordV2(NativeAuthResetPasswordParameters(username = username))
        val error = assertNotImplemented(result, NativeAuthFlowScenarioV2.RESET_PASSWORD)
        assertEquals(NativeAuthFlowScenarioV2.RESET_PASSWORD, error.scenario)
    }

    @Test
    fun signInV2RoutesExceptionToOnError() {
        assertRoutesToOnError { future ->
            application.signInV2(NativeAuthSignInParameters(username = username), throwingCallback(future))
        }
    }

    @Test
    fun signUpV2RoutesExceptionToOnError() {
        assertRoutesToOnError { future ->
            application.signUpV2(NativeAuthSignUpParameters(username = username), throwingCallback(future))
        }
    }

    @Test
    fun resetPasswordV2RoutesExceptionToOnError() {
        assertRoutesToOnError { future ->
            application.resetPasswordV2(NativeAuthResetPasswordParameters(username = username), throwingCallback(future))
        }
    }

    /**
     * Drives the error path of the application-level V2 callback overloads: when result delivery
     * throws an [MsalException], the `catch` block must route it to the callback's `onError`. This
     * covers the `catch`/`onError` branch of the callback overloads.
     */
    private fun assertRoutesToOnError(action: (ResultFuture<NativeAuthResultV2>) -> Unit) {
        val future = ResultFuture<NativeAuthResultV2>()
        action(future)
        try {
            future.get(30, TimeUnit.SECONDS)
            fail("Expected the exception to be routed to onError")
        } catch (e: ExecutionException) {
            assertTrue(e.cause is MsalClientException)
        }
    }

    private fun throwingCallback(future: ResultFuture<NativeAuthResultV2>): NativeAuthPublicClientApplication.NativeAuthV2Callback {
        return object : NativeAuthPublicClientApplication.NativeAuthV2Callback {
            override fun onResult(result: NativeAuthResultV2): Unit = throw MsalClientException("test_error", "boom")
            override fun onError(exception: BaseException) = future.setException(exception)
        }
    }

    @Test
    fun flowStateStepsReturnNotImplemented() = runTest {
        val config = NativeAuthPublicClientApplicationConfiguration()
        val scenario = NativeAuthFlowScenarioV2.SIGN_IN
        val attributes = com.microsoft.identity.nativeauth.UserAttributes.Builder().city("city").build()
        val authMethod = com.microsoft.identity.nativeauth.AuthMethod("id", "oob", null, "email")

        val codeRequired = CodeRequiredStateV2("continuation-token", "correlation-id", scenario, config)
        assertNotImplemented(codeRequired.submitCode("1234"))
        assertNotImplemented(codeRequired.resendCode())

        assertNotImplemented(PasswordRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitPassword("password".toCharArray()))
        assertNotImplemented(NewPasswordRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitNewPassword("password".toCharArray()))
        assertNotImplemented(AttributesRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitAttributes(attributes))
        assertNotImplemented(AttributesInvalidStateV2("continuation-token", "correlation-id", scenario, config).submitAttributes(attributes))
        assertNotImplemented(MFARequiredStateV2("continuation-token", "correlation-id", scenario, config).selectAuthMethod(authMethod))
        assertNotImplemented(MFAVerificationRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitChallenge("challenge"))
        assertNotImplemented(StrongAuthRegistrationRequiredStateV2("continuation-token", "correlation-id", scenario, config).selectAuthMethod(authMethod))
        assertNotImplemented(StrongAuthVerificationRequiredStateV2("continuation-token", "correlation-id", scenario, config).submitChallenge("challenge"))
    }

    @Test
    fun errorIsResultAndNotBrowserRequired() {
        val error = NativeAuthErrorV2(
            errorType = "not_implemented",
            errorMessage = "This is not implemented yet",
            correlationId = "correlation-id",
            scenario = NativeAuthFlowScenarioV2.UNKNOWN
        )
        assertTrue(error is NativeAuthResultV2)
        assertTrue(error.isNotImplemented())
        assertFalse(error.isBrowserRequired())
    }

    private fun assertNotImplemented(
        result: NativeAuthResultV2,
        expectedScenario: NativeAuthFlowScenarioV2 = NativeAuthFlowScenarioV2.SIGN_IN
    ): NativeAuthErrorV2 {
        assertEquals(expectedScenario, result.scenario)
        assertTrue(result is NativeAuthErrorV2)
        val error = result as NativeAuthErrorV2
        assertTrue(error.isNotImplemented())
        return error
    }

    @Suppress("unused")
    private fun exhaustiveWhen(result: NativeAuthResultV2): String = when (result) {
        is NativeAuthResultV2.Complete -> "complete"
        is NativeAuthResultV2.CodeRequired -> "code"
        is NativeAuthResultV2.PasswordRequired -> "password"
        is NativeAuthResultV2.NewPasswordRequired -> "newPassword"
        is NativeAuthResultV2.AttributesRequired -> "attributes"
        is NativeAuthResultV2.AttributesInvalid -> "attributesInvalid"
        is NativeAuthResultV2.MFARequired -> "mfa"
        is NativeAuthResultV2.MFAVerificationRequired -> "mfaVerification"
        is NativeAuthResultV2.StrongAuthRegistrationRequired -> "strongAuthRegistration"
        is NativeAuthResultV2.StrongAuthVerificationRequired -> "strongAuthVerification"
        is NativeAuthErrorV2 -> "error"
        else -> "unknown"
    }
}
