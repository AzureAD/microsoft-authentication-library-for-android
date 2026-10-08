// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.tests.network.nativeauth

import com.microsoft.identity.client.e2e.utils.assertResult as assertResultType
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.parameters.NativeAuthGetAccessTokenParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthResetPasswordParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitNewPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Enabled iOS V2 SSPR parity, using disposable accounts so password changes cannot
 * invalidate shared lab credentials or race another pipeline's password reset.
 */
class SSPRV2Test : NativeAuthPublicClientApplicationAbstractTest() {
    private val tempEmailApi = TemporaryEmailService()
    private lateinit var application: INativeAuthPublicClientApplication

    override fun setup() {
        super.setup()
        application = setupPCA(
            getConfig(ConfigType.SSPR),
            listOf("password", "oob"),
            listOf("mfa_required", "registration_required")
        )
    }

    @Test
    fun iosParity_resetPasswordAndExplicitSignInSucceeds() {
        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = createPasswordAccount()
                val codeRequired = startReset(email)
                val passwordRequired = submitEmailCode(email, codeRequired)
                submitNewPasswordAndSignIn(email, passwordRequired)
            }
        }
    }

    @Test
    fun iosParity_invalidThenValidCodeCompletesReset() {
        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = createPasswordAccount()
                val codeRequired = startReset(email)
                val invalid = codeRequired.nextState.submitCode("1")
                assertResult<SubmitCodeErrorV2>(invalid)
                assertTrue((invalid as SubmitCodeErrorV2).isInvalidCode())
                val passwordRequired = submitEmailCode(email, codeRequired)
                submitNewPasswordAndSignIn(email, passwordRequired)
            }
        }
    }

    @Test
    fun iosParity_newPasswordComplexityIsEnforced() {
        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = createPasswordAccount()
                val passwordRequired = submitEmailCode(email, startReset(email))
                val password = "1".toCharArray()
                val result = try {
                    passwordRequired.nextState.submitNewPassword(password)
                } finally {
                    password.fill('\u0000')
                }
                assertResult<SubmitNewPasswordErrorV2>(result)
                assertTrue((result as SubmitNewPasswordErrorV2).isInvalidPassword())
            }
        }
    }

    @Test
    fun iosParity_resendCodeReachesNewPasswordRequired() {
        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = createPasswordAccount()
                val initial = startReset(email)
                tempEmailApi.retrieveCodeFromInbox(email)
                tempEmailApi.markCheckpoint(email)
                val resent = initial.nextState.resendCode()
                assertResult<NativeAuthResultV2.CodeRequired>(resent)
                submitEmailCode(email, resent as NativeAuthResultV2.CodeRequired)
            }
        }
    }

    @Test
    fun iosParity_unregisteredEmailIsUserNotFound() {
        runBlocking {
            val result = application.resetPasswordV2(
                NativeAuthResetPasswordParameters(
                    tempEmailApi.generateRandomUnregisteredEmailAddress()
                )
            )
            assertResult<ResetPasswordErrorV2>(result)
            assertTrue((result as ResetPasswordErrorV2).isUserNotFound())
        }
    }

    private suspend fun createPasswordAccount(): String {
        val signUp = setupPCA(
            getConfig(ConfigType.SIGN_UP_PASSWORD),
            listOf("password", "oob"),
            listOf("mfa_required", "registration_required")
        )
        val email = tempEmailApi.createRandomEmailAddress()
        tempEmailApi.markCheckpoint(email)
        val password = newPassword()
        try {
            val parameters = NativeAuthSignUpParameters(email)
            parameters.password = password
            val started = signUp.signUpV2(parameters)
            assertResultType<NativeAuthResultV2.CodeRequired>(started)
            val completed = (started as NativeAuthResultV2.CodeRequired).nextState.submitCode(
                tempEmailApi.retrieveCodeFromInbox(email)
            )
            assertResultType<NativeAuthResultV2.SignInAfterSignUpRequired>(completed)
            assertResultType<GetAccountResult.NoAccountFound>(signUp.getCurrentAccount())
        } finally {
            password.fill('\u0000')
        }
        return email
    }

    private suspend fun startReset(email: String): NativeAuthResultV2.CodeRequired {
        tempEmailApi.markCheckpoint(email)
        val started = application.resetPasswordV2(NativeAuthResetPasswordParameters(email))
        assertResult<NativeAuthResultV2.CodeRequired>(started)
        val required = started as NativeAuthResultV2.CodeRequired
        assertEquals("email", required.channel.lowercase())
        assertFalse(required.sentTo.isNullOrBlank())
        assertTrue(required.codeLength > 0)
        return required
    }

    private suspend fun submitEmailCode(
        email: String,
        required: NativeAuthResultV2.CodeRequired
    ): NativeAuthResultV2.NewPasswordRequired {
        val result = required.nextState.submitCode(tempEmailApi.retrieveCodeFromInbox(email))
        assertResult<NativeAuthResultV2.NewPasswordRequired>(result)
        return result as NativeAuthResultV2.NewPasswordRequired
    }

    private suspend fun submitNewPasswordAndSignIn(
        email: String,
        required: NativeAuthResultV2.NewPasswordRequired
    ) {
        val password = newPassword()
        val result = try {
            required.nextState.submitNewPassword(password)
        } finally {
            password.fill('\u0000')
        }
        assertResult<NativeAuthResultV2.SignInAfterResetPasswordRequired>(result)
        assertResultType<GetAccountResult.NoAccountFound>(application.getCurrentAccount())
        val signedIn = (result as NativeAuthResultV2.SignInAfterResetPasswordRequired)
            .nextState.signIn()
        assertResult<NativeAuthResultV2.Complete>(signedIn)
        val account = (signedIn as NativeAuthResultV2.Complete).resultValue
        assertTrue(account.getAccount().username.equals(email, ignoreCase = true))
        assertFalse(account.getIdToken().isNullOrBlank())
        val token = account.getAccessToken(NativeAuthGetAccessTokenParameters())
        assertResultType<GetAccessTokenResult.Complete>(token)
        val authentication = (token as GetAccessTokenResult.Complete).resultValue
        assertFalse(authentication.accessToken.isNullOrBlank())
        assertNotNull(authentication.account)
    }

    private inline fun <reified ExpectedType> assertResult(actual: NativeAuthResultV2) {
        assertResultType<ExpectedType>(actual)
        assertEquals(NativeAuthFlowScenarioV2.RESET_PASSWORD, actual.scenario)
    }

    private fun newPassword(): CharArray =
        "Aa1!${UUID.randomUUID().toString().replace("-", "")}".toCharArray()
}
