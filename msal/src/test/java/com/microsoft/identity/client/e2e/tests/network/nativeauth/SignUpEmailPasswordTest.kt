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

import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.nativeauth.UserAttributes
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpError
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SignUpEmailPasswordTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    // Remove default Coroutine test timeout of 10 seconds.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    override fun setup() {
        super.setup()
        setupPCA("Email Password Config from BuildConfig") // TODO: Update setupPCA() logic to use config string
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun testSignUpErrorSimple() = runTest {
        val user = tempEmailApi.generateRandomEmailAddress()
        val result = application.signUp(user, "invalidpassword".toCharArray())
        Assert.assertTrue(result is SignUpError)
        Assert.assertTrue((result as SignUpError).isInvalidPassword())
    }

    /**
     * Set email and password and then verify email OOB as last step (hero scenario 9, use case 1.1.1) - Test case 13
     */
    @Test
    fun testSuccessOTPLast() {
        var signUpResult: SignUpResult
        var otp: String

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val password = getSafePassword()
                signUpResult = application.signUp(user, password.toCharArray())
                Assert.assertTrue(signUpResult is SignUpResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Verify email address using email OTP and then set password (use case 1.1.4) - Test case 16
     */
    @Test
    fun testSuccessOTPFirst() {
        var signUpResult: SignUpResult
        var otp: String

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                signUpResult = application.signUp(user)
                Assert.assertTrue(signUpResult is SignUpResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.PasswordRequired)
                val submitPasswordResult = (submitCodeResult as SignUpResult.PasswordRequired).nextState.submitPassword(getSafePassword().toCharArray())
                Assert.assertTrue(submitPasswordResult is SignUpResult.Complete)
            }
        }
    }
}