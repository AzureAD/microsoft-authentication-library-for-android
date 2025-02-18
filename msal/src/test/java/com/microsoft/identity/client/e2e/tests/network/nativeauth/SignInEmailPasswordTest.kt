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
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignOutResult
import kotlinx.coroutines.runBlocking
import org.checkerframework.checker.units.qual.s
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInEmailPasswordTest : NativeAuthPublicClientApplicationAbstractTest() {

    lateinit var application: INativeAuthPublicClientApplication
    lateinit var config: NativeAuthTestConfig.Config

    private val defaultConfigType = ConfigType.SIGN_IN_PASSWORD
    private val defaultChallengeTypes = listOf("password", "oob")

    /**
     * Use valid email and password to get token.
     * (hero scenario 15, use case 1.2.1)
     */
    @Test
    fun testSuccess() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val username = config.email
            val param = NativeAuthSignInParameters(username = username)
            param.password = getSafePassword().toCharArray()
            val result = application.signIn(param)
            assertResult<SignInResult.Complete>(result)
        }
    }

    /**
     * Use invalid email address to receive a "user not found" error.
     * (use case 1.2.2)
     */
    @Test
    fun testErrorIsUserNotFound() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val username = INVALID_EMAIL
            val param = NativeAuthSignInParameters(username = username)
            param.password = getSafePassword().toCharArray()
            val result = application.signIn(param)
            assertTrue(result is SignInError)
            assertTrue((result as SignInError).isUserNotFound())
        }
    }

    /**
     * Use valid email address and invalid password to receive a "invalid credentials" error.
     * (use case 1.2.3)
     */
    @Test
    fun testErrorIsInvalidCredentials() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val username = config.email
            val param = NativeAuthSignInParameters(username = username)
            param.password = INVALID_PASSWORD.toCharArray()
            val result = application.signIn(param)
            assertTrue(result is SignInError)
            assertTrue((result as SignInError).isInvalidCredentials())
        }
    }

    /**
     * User signs in with account A, while data for account A already exists in SDK persistence.
     * (use case 1.2.4)
     */
    @Test
    fun testErrorOutOfPersistence() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val username = config.email
            val param1 = NativeAuthSignInParameters(username = username)
            param1.password = getSafePassword().toCharArray()
            val result = application.signIn(param1)
            assertTrue(result is SignInResult.Complete)

            val param2 = NativeAuthSignInParameters(username = username)
            param2.password = getSafePassword().toCharArray()
            val result2 = application.signIn(param2)

            assertTrue(result2 is SignInError)
            assertTrue((result2 as SignInError).exception is MsalClientException)
            assertEquals("An account is already signed in.", result2.exception!!.message)
        }
    }

    /**
     * User signs in with account B, while data for account A already exists in SDK persistence.
     * (use case 1.2.5)
     */
    @Test
    fun testErrorOutOfPersistenceDifferentAccount() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val param = NativeAuthSignInParameters(username = config.email)
            param.password = getSafePassword().toCharArray()
            val result = application.signIn(param)

            assertTrue(result is SignInResult.Complete)

            val config2 = getConfig(ConfigType.SIGN_IN_OTP)
            val param2 = NativeAuthSignInParameters(username = config2.email)
            param.password = getSafePassword().toCharArray()
            val result2 = application.signIn(param2)

            assertTrue(result2 is SignInError)
            assertTrue((result2 as SignInError).exception is MsalClientException)
            assertEquals("An account is already signed in.", result2.exception!!.message)
        }
    }

    /**
     * Ability to provide scope to control auth strength of the token.
     * (use case 1.2.6)
     * Please refer to GetTokenTests.kt (testGetAccessTokenFromCache) for the test.
     */
    //    val result = application.signIn(
    //        username = username,
    //        password = password.toCharArray(),
    //        scopes = listOf(scopeA)
    //    )
    //    val accessTokenForImplicitScopes = authResult.accessToken
    //    Assert.assertTrue(authResult.scope.contains(scopeA))

    /**
     * User email is registered with email OTP auth method, which is supported by the developer.
     * (use case 1.2.7)
     */
    @Test
    fun testSuccessOTPConfigCodeRequired() {
        config = getConfig(ConfigType.SIGN_IN_OTP)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val param = NativeAuthSignInParameters(username = config.email)
            param.password = getSafePassword().toCharArray()
            val result = application.signIn(param)
            assertResult<SignInResult.CodeRequired>(result)
        }
    }

    /**
     * User attempts to sign in with email and password, but server requires second factor authentication (MFA OTP).
     * (use case 1.2.8)
     * Please refer to SignInMFATest.kt (`test MFA flow is triggered when authentication context is used as claim`) for the test.
     */
    // val sendChallengeResult = (result as SignInResult.MFARequired).nextState.requestChallenge()
    // assertResult<MFARequiredResult.VerificationRequired>(sendChallengeResult)
    // (sendChallengeResult as MFARequiredResult.VerificationRequired)

    /**
     * User email is registered with email OTP auth method, which is not supported by the developer (aka redirect flow)
     * (use case 1.2.9)
     */
    @Test
    fun testErrorOTPConfigBrowserRequired() {
        config = getConfig(ConfigType.SIGN_IN_OTP)
        application = setupPCA(config, listOf("password"))

        runBlocking {
            val param = NativeAuthSignInParameters(username = config.email)
            param.password = getSafePassword().toCharArray()
            val result = application.signIn(param)
            assertTrue(result is SignInError)
            assertTrue((result as SignInError).isBrowserRequired())
        }
    }

    /**
     * Sign in then sign out.
     * (hero scenario 18)
     */
    @Test
    fun testSignOut() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)

        runBlocking {
            val param = NativeAuthSignInParameters(username = config.email)
            param.password = getSafePassword().toCharArray()
            val signInResult = application.signIn(param)
            assertResult<SignInResult.Complete>(signInResult)
            val getAccountResult = application.getCurrentAccount()
            assertResult<GetAccountResult.AccountFound>(getAccountResult)
            val signOutResult = (getAccountResult as GetAccountResult.AccountFound).resultValue.signOut()
            assertResult<SignOutResult.Complete>(signOutResult)
        }
    }
}
