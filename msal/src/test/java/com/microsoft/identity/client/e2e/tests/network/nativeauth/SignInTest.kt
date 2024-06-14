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

import com.microsoft.identity.internal.testutils.nativeauth.NativeAuthCredentialHelper
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeError
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment.application

class SignInTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    // Remove default Coroutine test timeout of 10 seconds.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    override fun setup() {
        super.setup()
        Dispatchers.setMain(testDispatcher)
    }

    private fun <T> retryOperation(
        maxRetries: Int = 3,
        onFailure: () -> Unit = { Assert.fail() },
        block: () -> T
    ): T? {
        var retryCount = 0
        var shouldRetry = true

        while (shouldRetry) {
            try {
                return block()
            } catch (e: IllegalStateException) {
                if (retryCount >= maxRetries) {
                    onFailure()
                    shouldRetry = false
                } else {
                    retryCount++
                }
            }
        }
        return null
    }

    /**
     * Use email and password to get token (hero scenario 15, use case 1.2.1) - Test case 37
     */
    @Test
    fun testSuccessEmailPassword() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        val result = application.signIn(username, password.toCharArray())
        Assert.assertTrue(result is SignInResult.Complete)
    }

    /**
     * Use email and password to get token while user is not registered with given email (use case 1.2.2) - Test case 38
     */
    @Test
    fun testErrorEmailPasswordIsUserNotFound() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        // Turn an existing username to a non-existing username
        val alteredUsername = username.replace("@", "1234@")
        val result = application.signIn(alteredUsername, password.toCharArray())
        Assert.assertTrue(result is SignInError)
        Assert.assertTrue((result as SignInError).isUserNotFound())
    }

    /**
     * Use email and password to get token while password is incorrect (use case 1.2.3) - Test case 39
     */
    @Test
    fun testErrorEmailPasswordIsInvalidCredentials() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = getSafePassword()
        // Turn correct password into an incorrect one
        val alteredPassword = password + "1234"
        val result = application.signIn(username, alteredPassword.toCharArray())
        Assert.assertTrue(result is SignInError)
        Assert.assertTrue((result as SignInError).isInvalidCredentials())
    }

    /**
     * Use email and OTP to get token and sign in (hero scenario 6, use case 2.2.1) - Test case 30
     */
    @Test
    fun testSuccessEmailOTP() {
        var signInResult: SignInResult
        var otp: String

        retryOperation {
            runBlocking {
                val user = tempEmailApi.generateRandomEmailAddress()
                signInResult = application.signIn(user)
                Assert.assertTrue(signInResult is SignInResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signInResult as SignInResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignInResult.Complete)
                "Success"
            }
        }
    }

    /**
     * Use email and OTP to get token while user is not registered with given email (use case 2.2.2) - Test case 31
     */
    @Test
    fun testErrorEmailOTPIsUserNotFound() = runTest {
        val user = tempEmailApi.generateRandomEmailAddress()
        val signInResult = application.signIn(user)
        Assert.assertTrue(signInResult is SignInError)
        Assert.assertTrue((signInResult as SignInError).isUserNotFound())
    }

    /**
     * Use email and OTP to get token while OTP is incorrect (use case 2.2.7) - Test case 35
     */
    @Test
    fun testErrorEmailOTPIsInvalidCode() {
        var signInResult: SignInResult
        var otp: String

        retryOperation {
            runBlocking {
                val user = tempEmailApi.generateRandomEmailAddress()
                signInResult = application.signIn(user)
                Assert.assertTrue(signInResult is SignInResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                // Turn correct OTP into an incorrect one
                val alteredOtp = otp + "1234"
                val submitCodeResult = (signInResult as SignInResult.CodeRequired).nextState.submitCode(alteredOtp)
                Assert.assertTrue(submitCodeResult is SubmitCodeError)
                Assert.assertTrue((submitCodeResult as SubmitCodeError).isInvalidCode())
                "Success"
            }
        }
    }
}