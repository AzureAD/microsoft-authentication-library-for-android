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

import com.microsoft.identity.client.e2e.utils.assertResult
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import com.microsoft.identity.nativeauth.statemachine.errors.MFASubmitChallengeError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.MFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.lang.Thread.sleep

class SignInMFATest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    override var defaultConfigType = ConfigType.SIGN_IN_MFA_SINGLE_AUTH

    private lateinit var resources: List<NativeAuthTestConfig.Resource>

    override fun setup() {
        super.setup()
        resources = config.resources
    }

    /**
     * Full flow:
     * - Receive MFA required error from API.
     * - Request default challenge.
     * - Challenge sent successfully, SelectionRequired is returned.
     * - Submit invalid challenge and assert error.
     * - Request new challenge.
     * - Submit correct challenge.
     * - Complete MFA flow and complete sign in.
     *
     * Note: this test also asserts whether the scopes requested at sign in are present in the token that's received at the end of the flow
     */
    @Test
    @Ignore("Skip until MFA becomes available on production")
    fun `test submit invalid challenge, request new challenge, submit correct challenge and complete MFA flow`()  {
        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val username = config.email

                val scopeA = resources[0].scopes[0]
                val scopeB = resources[0].scopes[1]

                val password = getSafePassword()
                val result = application.signIn(
                    username = username,
                    password = password.toCharArray(),
                    scopes = listOf(scopeA, scopeB)
                )
                assertResult<SignInResult.MFARequired>(result)

                // Initiate challenge, send code to email
                val sendChallengeResult =
                    (result as SignInResult.MFARequired).nextState.requestChallenge()
                assertResult<MFARequiredResult.VerificationRequired>(sendChallengeResult)
                (sendChallengeResult as MFARequiredResult.VerificationRequired)
                assertNotNull(sendChallengeResult.sentTo)
                assertNotNull(sendChallengeResult.codeLength)
                assertNotNull(sendChallengeResult.channel)

                // Submit incorrect challenge
                val submitIncorrectChallengeResult = sendChallengeResult.nextState.submitChallenge("invalid")
                assertResult<MFASubmitChallengeError>(submitIncorrectChallengeResult)
                assertTrue((submitIncorrectChallengeResult as MFASubmitChallengeError).isInvalidChallenge())

                // Request new challenge
                val requestNewChallengeResult = sendChallengeResult.nextState.requestChallenge()
                assertResult<MFARequiredResult.VerificationRequired>(requestNewChallengeResult)
                (requestNewChallengeResult as MFARequiredResult.VerificationRequired)
                assertNotNull(requestNewChallengeResult.sentTo)
                assertNotNull(requestNewChallengeResult.codeLength)
                assertNotNull(requestNewChallengeResult.channel)

                // Retrieve challenge from mailbox and submit
                val otp = tempEmailApi.retrieveCodeFromInbox(username)
                val submitCorrectChallengeResult = requestNewChallengeResult.nextState.submitChallenge(otp)
                assertResult<SignInResult.Complete>(submitCorrectChallengeResult)

                val accountState = (submitCorrectChallengeResult as SignInResult.Complete).resultValue
                val getAccessTokenResult = accountState.getAccessToken()
                assertResult<GetAccessTokenResult.Complete>(getAccessTokenResult)
                val authResult = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue
                assertTrue(authResult.scope.contains(scopeA))
                assertTrue(authResult.scope.contains(scopeB))
            }
        }
    }

    /**
     * Full flow:
     * - Receive MFA required error from API.
     * - Request default challenge.
     * - Challenge sent successfully, SelectionRequired is returned.
     * - Call getAuthMethods to retrieve all auth methods available.
     * - Request new challenge.
     * - Submit correct challenge.
     * - Complete MFA flow and complete sign in.
     *
     * Note: this test also asserts whether the scopes requested at sign in are present in the token that's received at the end of the flow
     */
    @Test
    @Ignore("Skip until MFA becomes available on production")
    fun `test get other auth methods, request challenge on specific auth method and complete MFA flow`() {
        retryOperation {
            runBlocking {
                val scopeA = resources[0].scopes[0]
                val scopeB = resources[0].scopes[1]

                val username = config.email
                val password = getSafePassword()
                val result = application.signIn(
                    username,
                    password.toCharArray(),
                    listOf(scopeA, scopeB)
                )
                assertResult<SignInResult.MFARequired>(result)

                // Initiate challenge, send code to email
                val sendChallengeResult =
                    (result as SignInResult.MFARequired).nextState.requestChallenge()
                assertResult<MFARequiredResult.VerificationRequired>(sendChallengeResult)
                (sendChallengeResult as MFARequiredResult.VerificationRequired)
                assertNotNull(sendChallengeResult.sentTo)
                assertNotNull(sendChallengeResult.codeLength)
                assertNotNull(sendChallengeResult.channel)

                // Retrieve other auth methods
                val getAuthMethodsResult = sendChallengeResult.nextState.getAuthMethods()
                assertResult<MFARequiredResult.SelectionRequired>(getAuthMethodsResult)
                (getAuthMethodsResult as MFARequiredResult.SelectionRequired)
                assertTrue(getAuthMethodsResult.authMethods.size == 1)
                assertEquals("email", getAuthMethodsResult.authMethods[0].challengeChannel)

                // Request challenge for specific auth method
                val requestNewChallengeResult =
                    sendChallengeResult.nextState.requestChallenge(getAuthMethodsResult.authMethods[0])
                assertResult<MFARequiredResult.VerificationRequired>(requestNewChallengeResult)
                (requestNewChallengeResult as MFARequiredResult.VerificationRequired)
                assertNotNull(requestNewChallengeResult.sentTo)
                assertNotNull(requestNewChallengeResult.codeLength)
                assertNotNull(requestNewChallengeResult.channel)

                // Retrieve challenge from mailbox and submit
                val otp = tempEmailApi.retrieveCodeFromInbox(username)
                val submitCorrectChallengeResult = requestNewChallengeResult.nextState.submitChallenge(otp)
                assertResult<SignInResult.Complete>(submitCorrectChallengeResult)

                val accountState = (submitCorrectChallengeResult as SignInResult.Complete).resultValue
                val getAccessTokenResult = accountState.getAccessToken()
                assertResult<GetAccessTokenResult.Complete>(getAccessTokenResult)
                val authResult = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue
                assertTrue(authResult.scope.contains(scopeA))
                assertTrue(authResult.scope.contains(scopeB))
            }
        }
    }

    /**
     * Full flow:
     * - Receive MFA required error from API.
     * - Request default challenge.
     * - No default auth method available, so SelectionRequired is returned.
     * - Request new challenge on specific auth method.
     * - Submit correct challenge.
     * - Complete MFA flow and complete sign in.
     *
     * Note: this test also asserts whether the scopes requested at sign in are present in the token that's received at the end of the flow
     */
    @Test
    @Ignore("Skip until MFA becomes available on production")
    fun `test selection required, request challenge on specific auth method and complete MFA flow`() {
        retryOperation {
            runBlocking {
                val configType = ConfigType.SIGN_IN_MFA_MULTI_AUTH
                setupPCA(configType)

                val scopeA = resources[0].scopes[0]
                val scopeB = resources[0].scopes[1]

                val username = config.email
                val password = getSafePassword()
                val result = application.signIn(
                    username,
                    password.toCharArray(),
                    listOf(scopeA, scopeB)
                )
                assertResult<SignInResult.MFARequired>(result)

                // Initiate challenge, send code to email
                val sendChallengeResult =
                    (result as SignInResult.MFARequired).nextState.requestChallenge()
                assertResult<MFARequiredResult.SelectionRequired>(sendChallengeResult)
                (sendChallengeResult as MFARequiredResult.SelectionRequired)
                assertTrue(sendChallengeResult.authMethods.size == 1)
                assertEquals("email", sendChallengeResult.authMethods[0].challengeChannel)

                // Request challenge for specific auth method
                val requestNewChallengeResult =
                    sendChallengeResult.nextState.requestChallenge(sendChallengeResult.authMethods[0])
                assertResult<MFARequiredResult.VerificationRequired>(requestNewChallengeResult)
                (requestNewChallengeResult as MFARequiredResult.VerificationRequired)
                assertNotNull(requestNewChallengeResult.sentTo)
                assertNotNull(requestNewChallengeResult.codeLength)
                assertNotNull(requestNewChallengeResult.channel)

                // Retrieve challenge from mailbox and submit
                val otp = tempEmailApi.retrieveCodeFromInbox(username)
                val submitCorrectChallengeResult = requestNewChallengeResult.nextState.submitChallenge(otp)
                assertResult<SignInResult.Complete>(submitCorrectChallengeResult)

                val accountState = (submitCorrectChallengeResult as SignInResult.Complete).resultValue
                val getAccessTokenResult = accountState.getAccessToken()
                assertResult<GetAccessTokenResult.Complete>(getAccessTokenResult)
                val authResult = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue
                assertTrue(authResult.scope.contains(scopeA))
                assertTrue(authResult.scope.contains(scopeB))
            }
        }
    }
}
