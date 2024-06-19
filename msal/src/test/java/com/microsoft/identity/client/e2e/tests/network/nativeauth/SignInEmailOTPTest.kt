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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SignInEmailOTPTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    // Remove default Coroutine test timeout of 10 seconds.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    override fun setup() {
        super.setup()
        setupPCA("Email OTP Config from BuildConfig") // TODO: Update setupPCA() logic to use config string
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Use email and OTP to get token and sign in (hero scenario 6, use case 2.2.1) - Test case 30
     */
    @Test
    fun testSuccess() {
        var signInResult: SignInResult
        var otp: String

        retryOperation {
            runBlocking {
                val user = NativeAuthCredentialHelper.nativeAuthSignInUsername
                signInResult = application.signIn(user)
                Assert.assertTrue(signInResult is SignInResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signInResult as SignInResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignInResult.Complete)
            }
        }
    }

    /**
     * Use email and OTP to get token while user is not registered with given email (use case 2.2.2) - Test case 31
     */
    @Test
    fun testErrorIsUserNotFound() = runTest {
        val user = tempEmailApi.generateRandomEmailAddress()
        val signInResult = application.signIn(user)
        Assert.assertTrue(signInResult is SignInError)
        Assert.assertTrue((signInResult as SignInError).isUserNotFound())
    }

    /**
     * Use email and OTP to get token while OTP is incorrect (use case 2.2.7) - Test case 35
     */
    @Test
    fun testErrorIsInvalidCode() {
        var signInResult: SignInResult
        var otp: String

        retryOperation {
            runBlocking {
                val user = NativeAuthCredentialHelper.nativeAuthSignInUsername
                signInResult = application.signIn(user)
                Assert.assertTrue(signInResult is SignInResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                // Turn correct OTP into an incorrect one
                val alteredOtp = otp + "1234"
                val submitCodeResult = (signInResult as SignInResult.CodeRequired).nextState.submitCode(alteredOtp)
                Assert.assertTrue(submitCodeResult is SubmitCodeError)
                Assert.assertTrue((submitCodeResult as SubmitCodeError).isInvalidCode())
            }
        }
    }
}