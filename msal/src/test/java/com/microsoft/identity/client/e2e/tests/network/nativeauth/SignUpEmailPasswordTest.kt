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

class SignUpEmailPasswordTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    lateinit var application: INativeAuthPublicClientApplication
    lateinit var config: NativeAuthTestConfig.Config

    private val defaultConfigType = ConfigType.SIGN_UP_PASSWORD
    private val defaultChallengeTypes = listOf("password", "oob")


    @Ignore("1secmail service is down. Ignoring test for now.")
    @Test
    fun testSignUpErrorSimple() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking {
                val user = tempEmailApi.generateRandomEmailAddress()
                val result = application.signUp(user, "invalidpassword".toCharArray())
                Assert.assertTrue(result is SignUpError)
                Assert.assertTrue((result as SignUpError).isInvalidPassword())
            }
        }
    }

    /**
     * Sign up with email + password. Set email and password (mimicking one combined screen for email & password collection), and then verify email OTP as last step
     * (hero scenario 9, use case 1.1.1)
     */
    @Ignore("1secmail service is down. Ignoring test for now.")
    @Test
    fun testSuccessOTPLast() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val password = getSafePassword()
                val signUpResult = application.signUp(user, password.toCharArray())
                assertResult<SignUpResult.CodeRequired>(signUpResult)

                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Sign up with email + password. Resend email OOB.
     * (use case 1.1.2)
     */
    @Ignore("1secmail service is down. Ignoring test for now.")
    @Test
    fun testResendEmailOOB() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val password = getSafePassword()
                val signUpResult = application.signUp(user, password.toCharArray())
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp1 = tempEmailApi.retrieveCodeFromInbox(user)
                val codeRequiredState = (signUpResult as SignUpResult.CodeRequired).nextState
                val resendCodeResult = codeRequiredState.resendCode()
                assertResult<SignUpResendCodeResult.Success>(resendCodeResult)
                val otp2 = tempEmailApi.retrieveCodeFromInbox(user)
                Assert.assertNotEquals(otp1, otp2)
            }
        }
    }

    /**
     * Sign up with email + password. Verify email address using email OTP and then set password (mimicking email and password collection on separate screens).
     * (use case 1.1.4)
     */
    @Ignore("1secmail service is down. Ignoring test for now.")
    @Test
    fun testSuccessOTPFirst() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)

                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignUpResult.PasswordRequired>(submitCodeResult)

                val submitPasswordResult = (submitCodeResult as SignUpResult.PasswordRequired).nextState.submitPassword(getSafePassword().toCharArray())
                Assert.assertTrue(submitPasswordResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Sign up with email + password. Verify email address using email OTP, resend OTP and then set password.
     * (use case 1.1.5)
     */
    @Ignore("1secmail service is down. Ignoring test for now.")
    @Test
    fun testSuccessOTPResend() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val signUpResult = application.signUp(user)
                assertResult<SignUpResult.CodeRequired>(signUpResult)

                val resendCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.resendCode()
                assertResult<SignUpResendCodeResult.Success>(resendCodeResult)

                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (resendCodeResult as SignUpResendCodeResult.Success).nextState.submitCode(otp)
                assertResult<SignUpResult.PasswordRequired>(submitCodeResult)

                val submitPasswordResult = (submitCodeResult as SignUpResult.PasswordRequired).nextState.submitPassword(getSafePassword().toCharArray())
                Assert.assertTrue(submitPasswordResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Sign up with email + password. User already exists with given email as email-pw account.
     * (use case 1.1.10)
     */
    @Test
    fun testErrorUserExistAsPassword() {
        config = getConfig(ConfigType.SIGN_IN_PASSWORD)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = config.email
            val password = getSafePassword()
            val signUpResult = application.signUp(user, password.toCharArray())
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isUserAlreadyExists())
        }
    }

    /**
     * Sign up with email + password. User already exists with given email as social account.
     * (use case 1.1.11)
     */
    @Ignore("TODO: Add social account in the tenant.")
    @Test
    fun testErrorUserExistAsSocial() {
        config = getConfig(ConfigType.SIGN_IN_PASSWORD)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = config.email
            val password = getSafePassword()
            val signUpResult = application.signUp(user, password.toCharArray())
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isUserAlreadyExists())
        }
    }

    /**
     * Sign up with email + password. Developer makes a request with invalid format email address.
     * (use case 1.1.12)
     */
    @Test
    fun testErrorInvalidEmailFormat() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = INVALID_EMAIL
            val password = getSafePassword()
            val signUpResult = application.signUp(user, password.toCharArray())
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isInvalidUsername())
        }
    }

    /**
     * Sign up with email + password. Developer makes a request with password that does not match password complexity requirements set on portal.
     * (use case 1.1.13)
     */
    @Ignore("1secmail service is down. Ignoring test for now.")
    @Test
    fun testErrorInvalidPasswordFormat() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
            val user = tempEmailApi.generateRandomEmailAddress()
            val password = INVALID_PASSWORD
            val signUpResult = application.signUp(user, password.toCharArray())
            Assert.assertTrue(signUpResult is SignUpError)
            Assert.assertTrue((signUpResult as SignUpError).isInvalidPassword())
        }
    }

    /**
     * Sign up with email + password. Developer can opt to get AT and/or ID token (aka sign in after signup).
     * (use case 1.1.14)
     */
    @Ignore("1secmail service is down. Ignoring test for now.")
    @Test
    fun testSignInAfterSignUp() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val password = getSafePassword()
                val signUpResult = application.signUp(user, password.toCharArray())
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                assertResult<SignUpResult.Complete>(submitCodeResult)
                val signWithContinuationResult = (submitCodeResult as SignUpResult.Complete).nextState.signIn()
                assertResult<SignInResult.Complete>(signWithContinuationResult)
            }
        }
    }
}