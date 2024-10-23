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
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpError
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class SignUpEmailOTPTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    lateinit var application: INativeAuthPublicClientApplication
    lateinit var config: NativeAuthTestConfig.Config

    private val defaultConfigType = ConfigType.SIGN_UP_OTP
    private val defaultChallengeTypes = listOf("password", "oob")

    /**
     * Sign up with email + OTP. Verify email address using email OTP and sign up.
     * (hero scenario 1, use case 2.1.1, Test case 1)
     */
    @Ignore("Fetching OTP code is unstable")
    @Test
    fun testSuccess() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Sign up with email + OTP. Resend email OTP.
     * (hero scenario 1, use case 2.1.5, Test case 11)
     */
    @Test
    fun testResendCode() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = tempEmailApi.generateRandomEmailAddress()
            val signUpResult = application.signUp(user)
            assertResult<SignUpResult.CodeRequired>(signUpResult)
            val codeRequiredState = (signUpResult as SignUpResult.CodeRequired).nextState
            val resendCodeResult = codeRequiredState.resendCode()
            assertResult<SignUpResendCodeResult.Success>(resendCodeResult)
        }
    }

    /**
     * Sign up with email + OTP. User already exists with given email as email-otp account.
     * (hero scenario 1, use case 2.1.6, Test case 12)
     */
    @Test
    fun testErrorUserExistAsOTP() {
        config = getConfig(ConfigType.SIGN_IN_OTP)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = config.email
            val signUpResult = application.signUp(user)
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isUserAlreadyExists())
        }
    }

    /**
     * Sign up with email + OTP. User already exists with given email as email-pw account.
     * (Test case 13)
     */
    @Test
    fun testErrorUserExistAsPassword() {
        config = getConfig(ConfigType.SIGN_IN_PASSWORD)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = config.email
            val signUpResult = application.signUp(user)
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isUserAlreadyExists())
        }
    }

    /**
     * Sign up with email + OTP. User already exists with given email as social account.
     * (use case 2.1.7, Test case 14)
     */
    @Ignore("TODO: Add social account in the tenant.")
    @Test
    fun testErrorUserExistAsSocial() {
        config = getConfig(ConfigType.SIGN_IN_PASSWORD)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = config.email
            val signUpResult = application.signUp(user)
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isUserAlreadyExists())
        }
    }

    /**
     * Sign up with email + OTP. Developer makes a request with invalid format email address.
     * (use case 2.1.8, Test case 15)
     */
    @Test
    fun testErrorInvalidEmailFormat() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = INVALID_EMAIl
            val signUpResult = application.signUp(user)
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isInvalidUsername())
        }
    }

    /**
     * Sign up with email + OTP. Developer can opt to get AT and/or ID token (aka sign in after signup).
     * (use case 2.1.9, Test case 16)
     */
    @Test
    fun testSignInAfterSignUp() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.Complete)
                val signWithContinuationResult = (submitCodeResult as SignUpResult.Complete).nextState.signIn()
                assertResult<SignInResult.Complete>(signWithContinuationResult)
            }
        }
    }

    /**
     * Sign up with email + OTP. Server requires password authentication, which is not supported by the developer (aka redirect flow).
     * (use case 2.1.10, Test case 17)
     */
    @Test
    fun testErrorRedirect() {
        config = getConfig(ConfigType.SIGN_UP_PASSWORD)
        application = setupPCA(config, listOf("oob"))

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = tempEmailApi.generateRandomEmailAddress()
            val signUpResult = application.signUp(user)
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isBrowserRequired())
        }
    }

    /**
     * Sign up with email + OTP. Server requires password authentication, which is supported by the developer.
     * (hero scenario 11, use case 2.1.11, Test case 17)
     */
    @Test
    fun testPasswordRequired() {
        config = getConfig(ConfigType.SIGN_UP_PASSWORD)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignUpResult.PasswordRequired>(submitCodeResult)
            }
        }
    }
}