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

import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.nativeauth.statemachine.errors.MFAError
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.results.SignInMFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInTest : NativeAuthPublicClientApplicationAbstractTest() {

    override val configType = ConfigType.SIGN_IN_PASSWORD

    @Test
    fun testSignInErrorSimple() = runTest {
        val username = config.email
        val password = getSafePassword()
        // Turn correct password into an incorrect one
        val alteredPassword = password + "1234"
        val result = application.signIn(username, alteredPassword.toCharArray())
        Assert.assertTrue(result is SignInError)
        Assert.assertTrue((result as SignInError).isInvalidCredentials())
    }

    @Test
    fun testSignInSuccessSimple() = runTest {
        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(username, password.toCharArray())
        Assert.assertTrue(result is SignInResult.Complete)
    }

    @Test
    fun testSignInMFASimple() = runTest {
        val result = application.signIn("user", "password".toCharArray())
        when (result) {
            is SignInResult.Complete -> {

            }
            is SignInResult.MFARequired -> {
                val nextState = result.nextState
                // Initiate challenge, send code to email
                val sendChallengeResult = nextState.sendChallenge()
                when (sendChallengeResult) {
                    is SignInMFARequiredResult.VerificationRequired -> {
                        // Show code UI
                        showCodeUI(
                            sentTo = sendChallengeResult.sentTo,
                            codeLength = sendChallengeResult.codeLength
                        )

                        // Retrieve all methods to build additional "pick MFA method UI"
                        val authMethodsResult = sendChallengeResult.nextState.getAuthMethods()
                        // call /challenge with specified ID
                        sendChallengeResult.nextState.sendChallenge(authMethodsResult.authMethods[0])
                    }
                    is SignInMFARequiredResult.SelectionRequired -> {
                        val authMethods = sendChallengeResult.authMethods
                        val sendSelectedChallengeResult = sendChallengeResult.nextState.sendChallenge(authMethods[0])
                        if (sendSelectedChallengeResult is SignInMFARequiredResult.VerificationRequired) {
                            val submitCodeResult = sendSelectedChallengeResult.nextState.submitChallenge(1234)
                            if (submitCodeResult is SignInResult.DummyComplete) {
                                assertTrue(true)
                            } else {
                                assertTrue(false)
                            }
                        }
                    }
                    is MFAError -> {
                        assertTrue(false)
                    }
                }
            }
            is SignInError -> {
                assertTrue(false)
            }
        }
    }

    private fun showCodeUI(sentTo: String, codeLength: Int) {

    }
}
