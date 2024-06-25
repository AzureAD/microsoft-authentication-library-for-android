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
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordError
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordSubmitCodeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SSPRTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    // Remove default Coroutine test timeout of 10 seconds.
    private val testDispatcher = StandardTestDispatcher()

    @Before
    override fun setup() {
        super.setup()
        setupPCA("SSPR")
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun testSSPRErrorSimple() = runTest {
        val user = config.email
        // Turn correct username into an incorrect one
        val invalidUser = user + "x"
        val result = application.resetPassword(invalidUser)
        Assert.assertTrue(result is ResetPasswordError)
        Assert.assertTrue((result as ResetPasswordError).isUserNotFound())
    }

    /**
     * Running with runBlocking to avoid default 10 second execution timeout.
     */
    @Test
    fun testSSPRSuccessSimple() = runBlocking {
        var retryCount = 0
        var shouldRetry = true

        while (shouldRetry) {
            try {
                val user = config.email
                val result = application.resetPassword(user)
                Assert.assertTrue(result is ResetPasswordStartResult.CodeRequired)
                val otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (result as ResetPasswordStartResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is ResetPasswordSubmitCodeResult.PasswordRequired)
                val password = getSafePassword()
                val submitPasswordResult = (submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState.submitPassword(password.toCharArray())
                Assert.assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
                shouldRetry = false
                break
            } catch (e: IllegalStateException) {
                // Re-run this test if the OTP retrieval fails. 1SecMail is known for emails to sometimes never arrive.
                // In that case, restart the test case with a new email address and try again, to make test less flaky.
                if (retryCount == 3) {
                    Assert.fail()
                    shouldRetry = false
                }
                retryCount++
            }
        }
    }
}