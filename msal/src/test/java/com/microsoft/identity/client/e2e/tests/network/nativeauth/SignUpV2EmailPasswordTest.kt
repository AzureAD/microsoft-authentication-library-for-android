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

import com.microsoft.identity.client.e2e.utils.NativeAuthEndpointCategory
import com.microsoft.identity.client.e2e.utils.NativeAuthRequestRecorder
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end coverage for Native Auth V2 sign-up with an email and password.
 */
class SignUpV2EmailPasswordTest : SignUpV2EmailAbstractTest() {

    private lateinit var application: INativeAuthPublicClientApplication

    companion object {
        private const val EMAIL_PASSWORD_CLIENT_ID = "456cf138-cb77-48e6-8a82-74f869d77e74"
    }

    private fun createApplication(
        recorder: NativeAuthRequestRecorder? = null
    ): INativeAuthPublicClientApplication =
        createApplication(ConfigType.SIGN_UP_PASSWORD, EMAIL_PASSWORD_CLIENT_ID, recorder)

    @Test
    fun hero2a_suppliedPasswordAndSignInAfterSignUp() {
        application = createApplication()

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val password = newValidPassword()
                try {
                    val codeRequired = startSignUp(application, email, password)
                    val otp = tempEmailApi.retrieveCodeFromInbox(email)
                    val submitCodeResult = codeRequired.nextState.submitCode(otp)

                    assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(submitCodeResult)
                    val complete = completeSignInAfterSignUp(
                        submitCodeResult as NativeAuthResultV2.SignInAfterSignUpRequired
                    )
                    assertAccountAndTokens(complete, email)
                } finally {
                    password.fill('\u0000')
                }
            }
        }
    }

    @Test
    fun hero2b_suppliedPasswordStopsBeforeSignIn() {
        val recorder = NativeAuthRequestRecorder()
        application = createApplication(recorder)

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val password = newValidPassword()
                try {
                    val codeRequired = startSignUp(application, email, password)
                    val otp = tempEmailApi.retrieveCodeFromInbox(email)
                    val beforeSubmitSnapshotSize = recorder.snapshot().size

                    val submitCodeResult = codeRequired.nextState.submitCode(otp)
                    assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(submitCodeResult)

                    val followUpCalls = recorder.callsAfter(beforeSubmitSnapshotSize)
                    assertEquals(
                        listOf(NativeAuthEndpointCategory.VERIFY),
                        followUpCalls
                    )
                    assertEquals(
                        0,
                        followUpCalls.count { it == NativeAuthEndpointCategory.TOKEN }
                    )

                    val getAccountResult = application.getCurrentAccount()
                    assertResult<GetAccountResult.NoAccountFound>(getAccountResult)
                } finally {
                    password.fill('\u0000')
                }
            }
        }
    }

    @Test
    fun hero2c_deferredPasswordAndSignInAfterSignUp() {
        application = createApplication()

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val codeRequired = startSignUp(application, email)
                val otp = tempEmailApi.retrieveCodeFromInbox(email)
                val submitCodeResult = codeRequired.nextState.submitCode(otp)

                assertResult<NativeAuthResultV2.PasswordRequired>(submitCodeResult)
                val passwordRequired = submitCodeResult as NativeAuthResultV2.PasswordRequired
                val password = newValidPassword()
                val submitPasswordResult = try {
                    passwordRequired.nextState.submitPassword(password)
                } finally {
                    password.fill('\u0000')
                }

                assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(submitPasswordResult)
                val complete = completeSignInAfterSignUp(
                    submitPasswordResult as NativeAuthResultV2.SignInAfterSignUpRequired
                )
                assertAccountAndTokens(complete, email)
            }
        }
    }

    @Test
    fun hero2d_deferredPasswordResendInvalidThenValidOtp() {
        application = createApplication()

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val firstResult = startSignUp(application, email)
                val firstState = firstResult.nextState
                val firstOtp = tempEmailApi.retrieveCodeFromInbox(email)

                tempEmailApi.markCheckpoint(email)
                val resendResult = firstState.resendCode()
                assertResult<NativeAuthResultV2.CodeRequired>(resendResult)
                val secondState = (resendResult as NativeAuthResultV2.CodeRequired).nextState
                assertNotSame(firstState, secondState)

                val secondOtp = tempEmailApi.retrieveCodeFromInbox(email)
                assertNotEquals(firstOtp, secondOtp)

                val invalidResult = secondState.submitCode(INCORRECT_CODE)
                assertSignUpScenario(invalidResult)
                assertTrue(invalidResult is SubmitCodeErrorV2)
                assertTrue((invalidResult as SubmitCodeErrorV2).isInvalidCode())

                val validResult = secondState.submitCode(secondOtp)
                assertResult<NativeAuthResultV2.PasswordRequired>(validResult)
                val passwordRequired = validResult as NativeAuthResultV2.PasswordRequired
                val password = newValidPassword()
                val submitPasswordResult = try {
                    passwordRequired.nextState.submitPassword(password)
                } finally {
                    password.fill('\u0000')
                }

                assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(submitPasswordResult)
                val complete = completeSignInAfterSignUp(
                    submitPasswordResult as NativeAuthResultV2.SignInAfterSignUpRequired
                )
                assertAccountAndTokens(complete, email)
            }
        }
    }
}
