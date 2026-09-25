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
package com.microsoft.identity.client.e2e.tests.network.nativeauth

import com.microsoft.identity.client.e2e.utils.NativeAuthRequestRecorder
import com.microsoft.identity.client.e2e.utils.NativeAuthEndpointCategory
import com.microsoft.identity.client.e2e.utils.assertResult
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.parameters.NativeAuthGetAccessTokenParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.statemachine.NativeAuthFlowScenarioV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end coverage for Native Auth V2 sign-up with an email one-time code.
 */
class SignUpV2EmailOTPTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()
    private lateinit var application: INativeAuthPublicClientApplication
    private lateinit var config: NativeAuthTestConfig.Config

    private val challengeTypes = listOf("password", "oob")
    private val capabilities = listOf("mfa_required", "registration_required")

    companion object {
        private const val EMAIL_OTP_CLIENT_ID = "eb5a6da5-79fc-4d81-8d45-0ee1e8d4bd16"
    }

    override fun setup() {
        super.setup()
        config = getConfig(ConfigType.SIGN_UP_OTP)
        assertEquals(EMAIL_OTP_CLIENT_ID, config.clientId)
    }

    private fun createApplication(
        recorder: NativeAuthRequestRecorder? = null
    ): INativeAuthPublicClientApplication =
        setupPCA(config, challengeTypes, capabilities, recorder)

    private suspend fun startSignUp(email: String): NativeAuthResultV2.CodeRequired {
        val result = application.signUpV2(NativeAuthSignUpParameters(username = email))
        assertResult<NativeAuthResultV2.CodeRequired>(result)
        val codeRequired = result as NativeAuthResultV2.CodeRequired
        assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, codeRequired.scenario)
        assertEquals("email", codeRequired.channel.lowercase())
        assertTrue(codeRequired.codeLength > 0)
        return codeRequired
    }

    private suspend fun completeSignInAfterSignUp(
        result: NativeAuthResultV2.SignInAfterSignUpRequired
    ): NativeAuthResultV2.Complete {
        val complete = result.nextState.signIn()
        assertResult<NativeAuthResultV2.Complete>(complete)
        return complete as NativeAuthResultV2.Complete
    }

    private suspend fun assertAccountAndTokens(
        result: NativeAuthResultV2.Complete,
        email: String
    ) {
        val accountState = result.resultValue
        val account = accountState.getAccount()
        assertNotNull(account)
        assertTrue(account.username.equals(email, ignoreCase = true))
        assertFalse(accountState.getIdToken().isNullOrBlank())

        val tokenResult = accountState.getAccessToken(NativeAuthGetAccessTokenParameters())
        assertResult<GetAccessTokenResult.Complete>(tokenResult)
        val authenticationResult = (tokenResult as GetAccessTokenResult.Complete).resultValue
        assertFalse(authenticationResult.accessToken.isNullOrBlank())
        assertNotNull(authenticationResult.account)
    }

    @Test
    fun hero1a_signUpEmailOtpAndSignInAfterSignUp() {
        val recorder = NativeAuthRequestRecorder()
        application = createApplication(recorder)

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val codeRequired = startSignUp(email)
                val otp = tempEmailApi.retrieveCodeFromInbox(email)
                val submitResult = codeRequired.nextState.submitCode(otp)

                assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(submitResult)
                val beforeSignInSnapshotSize = recorder.snapshot().size
                val complete = completeSignInAfterSignUp(
                    submitResult as NativeAuthResultV2.SignInAfterSignUpRequired
                )
                val signInCalls = recorder.callsAfter(beforeSignInSnapshotSize)
                val authorizeChallengeIndex =
                    signInCalls.indexOf(NativeAuthEndpointCategory.AUTHORIZE_CHALLENGE)
                val tokenIndex = signInCalls.indexOf(NativeAuthEndpointCategory.TOKEN)
                assertTrue(authorizeChallengeIndex >= 0)
                assertTrue(tokenIndex > authorizeChallengeIndex)

                assertAccountAndTokens(complete, email)
            }
        }
    }

    @Test
    fun hero1b_invalidThenValidOtpReusesOriginalState() {
        application = createApplication()

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val codeRequired = startSignUp(email)
                val originalState = codeRequired.nextState
                val validOtp = tempEmailApi.retrieveCodeFromInbox(email)

                val invalidResult = originalState.submitCode(INCORRECT_CODE)
                assertTrue(invalidResult is SubmitCodeErrorV2)
                assertTrue((invalidResult as SubmitCodeErrorV2).isInvalidCode())
                assertEquals(NativeAuthFlowScenarioV2.SIGN_UP, invalidResult.scenario)

                val validResult = originalState.submitCode(validOtp)
                assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(validResult)
                val complete = completeSignInAfterSignUp(
                    validResult as NativeAuthResultV2.SignInAfterSignUpRequired
                )
                assertAccountAndTokens(complete, email)
            }
        }
    }

    @Test
    fun hero1c_resendAndSubmitSecondOtpWithSecondState() {
        application = createApplication()

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val firstResult = startSignUp(email)
                val firstState = firstResult.nextState
                val firstOtp = tempEmailApi.retrieveCodeFromInbox(email)

                tempEmailApi.markCheckpoint(email)
                val resendResult = firstState.resendCode()
                assertResult<NativeAuthResultV2.CodeRequired>(resendResult)
                val secondResult = resendResult as NativeAuthResultV2.CodeRequired
                val secondState = secondResult.nextState
                assertNotSame(firstState, secondState)

                val secondOtp = tempEmailApi.retrieveCodeFromInbox(email)
                assertNotEquals(firstOtp, secondOtp)

                val submitResult = secondState.submitCode(secondOtp)
                assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(submitResult)
                val complete = completeSignInAfterSignUp(
                    submitResult as NativeAuthResultV2.SignInAfterSignUpRequired
                )
                assertAccountAndTokens(complete, email)
            }
        }
    }

    @Test
    fun hero1d_resendAndSubmitFirstOtpWithFirstState() {
        application = createApplication()

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val firstResult = startSignUp(email)
                val firstState = firstResult.nextState
                val firstOtp = tempEmailApi.retrieveCodeFromInbox(email)

                tempEmailApi.markCheckpoint(email)
                val resendResult = firstState.resendCode()
                assertResult<NativeAuthResultV2.CodeRequired>(resendResult)
                val secondState = (resendResult as NativeAuthResultV2.CodeRequired).nextState
                assertNotSame(firstState, secondState)

                val secondOtp = tempEmailApi.retrieveCodeFromInbox(email)
                assertNotEquals(firstOtp, secondOtp)

                val submitResult = firstState.submitCode(firstOtp)
                assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(submitResult)
                val complete = completeSignInAfterSignUp(
                    submitResult as NativeAuthResultV2.SignInAfterSignUpRequired
                )
                assertAccountAndTokens(complete, email)
            }
        }
    }

    @Test
    fun hero1e_resendInvalidThenValidOtpReusesSecondState() {
        application = createApplication()

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val firstResult = startSignUp(email)
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
                assertTrue(invalidResult is SubmitCodeErrorV2)
                assertTrue((invalidResult as SubmitCodeErrorV2).isInvalidCode())

                val validResult = secondState.submitCode(secondOtp)
                assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(validResult)
                val complete = completeSignInAfterSignUp(
                    validResult as NativeAuthResultV2.SignInAfterSignUpRequired
                )
                assertAccountAndTokens(complete, email)
            }
        }
    }

    @Test
    fun hero1f_submitOtpDoesNotSignInAutomatically() {
        val recorder = NativeAuthRequestRecorder()
        application = createApplication(recorder)

        retryOperation(maxRetries = 1) {
            runBlocking {
                val email = tempEmailApi.createRandomEmailAddress()
                tempEmailApi.markCheckpoint(email)

                val codeRequired = startSignUp(email)
                val otp = tempEmailApi.retrieveCodeFromInbox(email)
                val beforeSubmitSnapshotSize = recorder.snapshot().size

                val submitResult = codeRequired.nextState.submitCode(otp)
                assertResult<NativeAuthResultV2.SignInAfterSignUpRequired>(submitResult)

                val followUpCalls = recorder.callsAfter(beforeSubmitSnapshotSize)
                assertEquals(
                    listOf(NativeAuthEndpointCategory.VERIFY),
                    followUpCalls
                )
                assertEquals(0, followUpCalls.count { it == NativeAuthEndpointCategory.TOKEN })

                val getAccountResult = application.getCurrentAccount()
                assertResult<GetAccountResult.NoAccountFound>(getAccountResult)
            }
        }
    }
}
