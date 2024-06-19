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

class SignUpEmailOTPAttributesTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    // Remove default Coroutine test timeout of 10 seconds.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    override fun setup() {
        super.setup()
        setupPCA("Email OTP Attributes Config from BuildConfig") // TODO: Update setupPCA() logic to use config string
        Dispatchers.setMain(testDispatcher)
    }

    /**
     * Signup user with custom attributes with verify OTP as last step (hero scenario 2, use case 2.1.2) - Test case 2
     */
    @Test
    fun testSuccessAttributesFirst() {
        var signUpResult: SignUpResult
        var otp: String

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                val attributes = UserAttributes.country("Ireland").city("Dublin").build()
                signUpResult = application.signUp(user, attributes = attributes)
                Assert.assertTrue(signUpResult is SignUpResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Verify email OTP first and then collect custom attributes (hero scenario 3, use case 2.1.3) - Test case 3
     */
    @Test
    fun testSuccessAttributesLastSameScreen() { // The difference between test case 28 & 29 is simply the way UX and code are combined. Test code is the same as testSuccessAttributesLastMultipleScreens.
        var signUpResult: SignUpResult
        var otp: String

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                signUpResult = application.signUp(user)
                Assert.assertTrue(signUpResult is SignUpResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.AttributesRequired)
                val attributes = UserAttributes.country("Ireland").city("Dublin").build()
                val submitAttributesResult = (signUpResult as SignUpResult.AttributesRequired).nextState.submitAttributes(attributes)
                Assert.assertTrue(submitAttributesResult is SignUpResult.Complete)
            }
        }
    }

    /**
     * Verify email OTP first and then collect custom attributes on multiple screens (hero scenario 4, use case 2.1.4) - Test case 4
     */
    @Test
    fun testSuccessAttributesLastMultipleScreens() {
        var signUpResult: SignUpResult
        var otp: String

        retryOperation {
            runBlocking { // Running with runBlocking to avoid default 10 second execution timeout.
                val user = tempEmailApi.generateRandomEmailAddress()
                signUpResult = application.signUp(user)
                Assert.assertTrue(signUpResult is SignUpResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.AttributesRequired)
                val attributes = UserAttributes.country("Ireland").build()
                val submitAttributesResult = (signUpResult as SignUpResult.AttributesRequired).nextState.submitAttributes(attributes)
                Assert.assertTrue(submitAttributesResult is SignUpResult.AttributesRequired)
                val attributes2 = UserAttributes.city("Dublin").build()
                val submitAttributesResult2 = (signUpResult as SignUpResult.AttributesRequired).nextState.submitAttributes(attributes2)
                Assert.assertTrue(submitAttributesResult2 is SignUpResult.Complete)
            }
        }
    }
}