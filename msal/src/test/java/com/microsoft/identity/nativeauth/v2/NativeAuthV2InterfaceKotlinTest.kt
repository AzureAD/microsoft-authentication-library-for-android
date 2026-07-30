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
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.nativeauth.parameters.NativeAuthResetPasswordParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import com.microsoft.identity.nativeauth.statemachine.states.NativeAuthFlowStateV2
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
        val error = assertNotImplemented(result)
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, error.scenario)
    }

    @Test
    fun resetPasswordV2ReturnsNotImplemented() = runTest {
        val result = application.resetPasswordV2(NativeAuthResetPasswordParameters(username = username))
        val error = assertNotImplemented(result)
        assertEquals(NativeAuthFlowScenarioV2.RESET_PASSWORD, error.scenario)
    }

    @Test
    fun flowStateStepsReturnNotImplemented() = runTest {
        val state = NativeAuthFlowStateV2(
            continuationToken = "continuation-token",
            correlationId = "correlation-id",
            scenario = NativeAuthFlowScenarioV2.SIGN_IN,
            config = NativeAuthPublicClientApplicationConfiguration()
        )

        assertNotImplemented(state.submitCode("1234"))
        assertNotImplemented(state.submitPassword("password".toCharArray()))
        assertNotImplemented(state.submitNewPassword("password".toCharArray()))
        assertNotImplemented(state.resendCode())
        assertNotImplemented(state.submitChallenge("challenge"))
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

    private fun assertNotImplemented(result: NativeAuthResultV2): NativeAuthErrorV2 {
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
