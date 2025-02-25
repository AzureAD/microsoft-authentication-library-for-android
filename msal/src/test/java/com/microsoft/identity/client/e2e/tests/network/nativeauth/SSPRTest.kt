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
import com.microsoft.identity.nativeauth.parameters.NativeAuthResetPasswordParameters
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordError
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordSubmitPasswordError
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordSubmitCodeResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class SSPRTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    lateinit var application: INativeAuthPublicClientApplication
    lateinit var config: NativeAuthTestConfig.Config

    private val defaultConfigType = ConfigType.SSPR
    private val defaultChallengeTypes = listOf("password", "oob")

    /**
     * Verify email with email OTP first and then reset password.
     * (hero scenario 8 & 17, use case 3.1.1)
     */
    @Ignore("Retrieving OTP code failure")
    @Test
    fun testSSPRSuccess() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        var result: ResetPasswordStartResult

        retryOperation {
            runBlocking {
                val user = config.email
                val param = NativeAuthResetPasswordParameters(username = user)
                result = application.resetPassword(param)
                assertResult<ResetPasswordStartResult.CodeRequired>(result)

                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (result as ResetPasswordStartResult.CodeRequired).nextState.submitCode(otp)
                assertResult<ResetPasswordSubmitCodeResult.PasswordRequired>(submitCodeResult)

                val password = getSafePassword()
                val submitPasswordResult = (submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState.submitPassword(password.toCharArray())
                Assert.assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
            }
        }
    }

    /**
     * New password being set doesn’t meet password complexity requirements set on portal
     * (use case 3.1.3)
     */
    @Ignore("Retrieving OTP code failure.")
    @Test
    fun testErrorInvalidPasswordFormat() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        var result: ResetPasswordStartResult

        retryOperation {
            runBlocking {
                val user = config.email
                val param = NativeAuthResetPasswordParameters(username = user)
                result = application.resetPassword(param)
                assertResult<ResetPasswordStartResult.CodeRequired>(result)

                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (result as ResetPasswordStartResult.CodeRequired).nextState.submitCode(otp)
                assertResult<ResetPasswordSubmitCodeResult.PasswordRequired>(submitCodeResult)

                val password = INVALID_PASSWORD
                val submitPasswordResult = (submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState.submitPassword(password.toCharArray())
                Assert.assertTrue(submitPasswordResult is ResetPasswordSubmitPasswordError)
                Assert.assertTrue((submitPasswordResult as ResetPasswordSubmitPasswordError).isInvalidPassword())
            }
        }
    }

    /**
     * Resend Code.
     * (use case 3.1.4)
     */
    @Ignore("Retrieving OTP code failure.")
    @Test
    fun testResendCode() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        var result: ResetPasswordStartResult

        retryOperation {
            runBlocking {
                val user = config.email
                val param = NativeAuthResetPasswordParameters(username = user)
                result = application.resetPassword(param)
                assertResult<ResetPasswordStartResult.CodeRequired>(result)

                val otp1 = tempEmailApi.retrieveCodeFromInbox(user)
                val codeRequiredState = (result as ResetPasswordStartResult.CodeRequired).nextState
                val resendCodeResult = codeRequiredState.resendCode()
                assertResult<ResetPasswordResendCodeResult.Success>(resendCodeResult)

                val otp2 = tempEmailApi.retrieveCodeFromInbox(user)
                Assert.assertNotEquals(otp1, otp2)

                val submitCodeResult = (result as ResetPasswordStartResult.CodeRequired).nextState.submitCode(otp2)
                assertResult<ResetPasswordSubmitCodeResult.PasswordRequired>(submitCodeResult)

                val password = getSafePassword()
                val submitPasswordResult = (submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState.submitPassword(password.toCharArray())
                Assert.assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
            }
        }
    }

    /**
     * Email is not found in records.
     * (use case 3.1.5)
     */
    @Test
    fun testErrorUserNotExist() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val username = tempEmailApi.generateRandomEmailAddressLocally()
            val param = NativeAuthResetPasswordParameters(username = username)
            val result = application.resetPassword(param)
            Assert.assertTrue(result is ResetPasswordError)
            Assert.assertTrue((result as ResetPasswordError).isUserNotFound())
        }
    }

    /**
     *  When SSPR requires a challenge type not supported by the client, redirect to web-fallback.
     * (use case 3.1.6)
     */
    @Test
    fun testErrorInsufficientChallengesBrowserRequired() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, listOf("password"))

        runBlocking {
            val username = config.email
            val param = NativeAuthResetPasswordParameters(username = username)
            val result = application.resetPassword(param)
            Assert.assertTrue(result is ResetPasswordError)
            Assert.assertTrue((result as ResetPasswordError).isBrowserRequired())
        }
    }

    /**
     * Email exists but not linked to any password.
     * (use case 3.1.8)
     */
    @Test
    fun testErrorNoPasswordLinked() {
        config = getConfig(ConfigType.SIGN_IN_OTP)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val username = config.email
            val param = NativeAuthResetPasswordParameters(username = username)
            val result = application.resetPassword(param)
            Assert.assertTrue(result is ResetPasswordError)
            Assert.assertTrue((result as ResetPasswordError).errorMessage!!.contains("The tenant or user does not support native credential recovery."))
        }
    }

    /**
     * Email exists but signup method was OTP, social, etc.
     * (use case 3.1.9)
     */
    @Ignore("TODO: Add social account in the tenant.")
    @Test
    fun testErrorUserExistAsSocial() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val username = INVALID_EMAIL // TODO: Use social accounts instead when ready
            val param = NativeAuthResetPasswordParameters(username = username)
            val result = application.resetPassword(param)
            Assert.assertTrue(result is ResetPasswordError)
            Assert.assertTrue((result as ResetPasswordError).isUserNotFound())
        }
    }
}