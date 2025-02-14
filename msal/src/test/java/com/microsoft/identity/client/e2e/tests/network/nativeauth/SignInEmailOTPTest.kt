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
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeError
import com.microsoft.identity.nativeauth.statemachine.results.SignInResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class SignInEmailOTPTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    lateinit var application: INativeAuthPublicClientApplication
    lateinit var config: NativeAuthTestConfig.Config

    private val defaultConfigType = ConfigType.SIGN_IN_OTP
    private val defaultChallengeTypes = listOf("password", "oob")

    override fun setup() {
        super.setup()
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)
    }

    /**
     * Use valid email and OTP to get token and sign in.
     * (hero scenario 6, use case 2.2.1)
     */
    @Ignore("Retrieving OTP code failure.")
    @Test
    fun testSuccess() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = config.email
                val signInResult = application.signIn(user)
                assertResult<SignInResult.CodeRequired>(signInResult)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signInResult as SignInResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignInResult.Complete>(submitCodeResult)
            }
        }
    }

    /**
     * Use invalid email address to receive a "user not found" error.
     * (use case 2.2.2)
     */
    @Test
    fun testErrorIsUserNotFound() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking {
                val user = config.email
                // Turn correct username into an incorrect one
                val invalidUser = user + "x"
                val signInResult = application.signIn(invalidUser)
                Assert.assertTrue(signInResult is SignInError)
                Assert.assertTrue((signInResult as SignInError).isUserNotFound())
            }
        }
    }

    /**
     * User email is registered with password method, which is not supported by client (aka redirect flow).
     * (use case 2.2.3)
     */
    @Test
    fun testErrorPasswordConfigBrowserRequired() {
        config = getConfig(ConfigType.SIGN_IN_PASSWORD)
        application = setupPCA(config, listOf("oob"))

        runBlocking {
            val user = config.email
            val password = getSafePassword()
            val signInResult = application.signIn(user, password.toCharArray())
            Assert.assertTrue(signInResult is SignInError)
            Assert.assertTrue((signInResult as SignInError).isBrowserRequired())
        }
    }

    /**
     * User email is registered with password method, which is supported by client.
     * (use case 2.2.4)
     */
    @Test
    fun testSuccessConfigPasswordRequired() {
        config = getConfig(ConfigType.SIGN_IN_PASSWORD)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val user = config.email
            val signInResult = application.signIn(user)
            assertResult<SignInResult.PasswordRequired>(signInResult)

            val password = getSafePassword()
            val passwordRequiredState = (signInResult as SignInResult.PasswordRequired).nextState
            val submitPasswordResult = passwordRequiredState.submitPassword(password.toCharArray())
            assertResult<SignInResult.Complete>(submitPasswordResult)
        }
    }

    /**
     * Resend email OTP.
     * (use case 2.2.5)
     */
    @Test
    fun testResendCode() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking {
                val user = config.email
                val signInResult = application.signIn(user)
                assertResult<SignInResult.CodeRequired>(signInResult)
                val otp1 = tempEmailApi.retrieveCodeFromInbox(user)
                val codeRequiredState = (signInResult as SignInResult.CodeRequired).nextState
                val resendCodeResult = codeRequiredState.resendCode()
                assertResult<SignInResendCodeResult.Success>(resendCodeResult)
                val otp2 = tempEmailApi.retrieveCodeFromInbox(user)
                Assert.assertNotEquals(otp1, otp2)
            }
        }
    }

    /**
     * Ability to provide scope to control auth strength of the token.
     * (use case 2.2.6)
     * Please refer to GetTokenTests.kt (testGetAccessTokenFromCache) for the test.
     */


    /**
     * Use valid email address, but invalid OTP to receive "invalid code" error.
     * (use case 2.2.7)
     */
    @Ignore("Username used for this test is currently blocked in lab tenant.")
    @Test
    fun testErrorIsInvalidCode() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking {// Running with runBlocking to avoid default 10 second execution timeout.
                val user = config.email
                val signInResult = application.signIn(user)
                assertResult<SignInResult.CodeRequired>(signInResult)

                val incorrectOtp = "1234"
                val submitCodeResult = (signInResult as SignInResult.CodeRequired).nextState.submitCode(incorrectOtp)
                Assert.assertTrue(submitCodeResult is SubmitCodeError)
                Assert.assertTrue((submitCodeResult as SubmitCodeError).isInvalidCode())
            }
        }
    }
}