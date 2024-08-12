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

import com.microsoft.identity.client.e2e.utils.assertState
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.results.MFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertNotNull
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
        assertState<SignInResult.MFARequired>(result)
        // Initiate challenge, send code to email
        val sendChallengeResult = (result as SignInResult.MFARequired).nextState.sendChallenge()
        assertState<MFARequiredResult.VerificationRequired>(sendChallengeResult)
        (sendChallengeResult as MFARequiredResult.VerificationRequired)
        assertNotNull(sendChallengeResult.sentTo)
        assertNotNull(sendChallengeResult.codeLength)
        assertNotNull(sendChallengeResult.channel)

        // Retrieve all methods to build additional "pick MFA method UI"
        val authMethodsResult = sendChallengeResult.nextState.getAuthMethods()
        assertTrue(authMethodsResult.authMethods.isNotEmpty())
        // call /challenge with specified ID
        val sendChallengeResult2 = sendChallengeResult.nextState.sendChallenge(authMethodsResult.authMethods[0])
        assertState<MFARequiredResult.VerificationRequired>(sendChallengeResult2)

        // Submit the user supplied code to the API
        val submitCodeResult = (sendChallengeResult2 as MFARequiredResult.VerificationRequired).nextState.submitChallenge(1234)
        assertState<SignInResult.DummyComplete>(submitCodeResult)
    }
}
