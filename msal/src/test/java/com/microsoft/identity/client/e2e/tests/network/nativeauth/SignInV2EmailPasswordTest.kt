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
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.parameters.NativeAuthGetAccessTokenParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.statemachine.errors.MFASubmitChallengeErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SignInErrorV2
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitPasswordErrorV2
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.NativeAuthResultV2
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * End-to-end coverage for Native Auth V2 sign-in: password as the first factor, and password
 * followed by an email one-time code.
 *
 * Every test here is gated with [Ignore] because the V2 authorize-challenge sign-in endpoints are
 * not available on the tenant/slice these E2E tests run against; see each reason string. The tests
 * are kept compiled, and therefore maintained, so they can be enabled unchanged once a V2-capable
 * slice exists. The equivalent behaviour is asserted today without a service in
 * `com.microsoft.identity.nativeauth.v2.NativeAuthV2SignInTest`.
 *
 * Credentials come from the existing secure lab infrastructure exactly as the V1 suites do; no
 * captured token, continuation token, authorization code, password or one-time code appears here.
 */
class SignInV2EmailPasswordTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    private lateinit var resources: List<NativeAuthTestConfig.Resource>

    lateinit var application: INativeAuthPublicClientApplication
    lateinit var config: NativeAuthTestConfig.Config

    private val defaultConfigType = ConfigType.SIGN_IN_PASSWORD
    private val mfaConfigType = ConfigType.SIGN_IN_MFA_SINGLE_AUTH
    private val defaultChallengeTypes = listOf("password", "oob")
    private val defaultCapabilities = listOf("mfa_required", "registration_required")

    /**
     * Sign in with a valid email and password supplied at the entry point, and receive tokens.
     */
    @Ignore("Native Auth V2 sign-in endpoints are not available on the E2E slice.")
    @Test
    fun testSuccessWithPasswordAtEntry() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes, defaultCapabilities)

        runBlocking {
            val param = NativeAuthSignInParameters(username = config.email)
            param.password = getSafePassword().toCharArray()
            val result = application.signInV2(param)
            assertResult<NativeAuthResultV2.Complete>(result)
        }
    }

    /**
     * Omit the password at the entry point, then submit it from the password-required state.
     */
    @Ignore("Native Auth V2 sign-in endpoints are not available on the E2E slice.")
    @Test
    fun testSuccessWithDeferredPassword() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes, defaultCapabilities)

        runBlocking {
            val result = application.signInV2(NativeAuthSignInParameters(username = config.email))
            assertResult<NativeAuthResultV2.PasswordRequired>(result)

            val submitResult = (result as NativeAuthResultV2.PasswordRequired)
                .nextState.submitPassword(getSafePassword().toCharArray())
            assertResult<NativeAuthResultV2.Complete>(submitResult)
        }
    }

    /**
     * An unknown account is reported as user-not-found.
     */
    @Ignore("Native Auth V2 sign-in endpoints are not available on the E2E slice.")
    @Test
    fun testErrorIsUserNotFound() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes, defaultCapabilities)

        runBlocking {
            val param = NativeAuthSignInParameters(username = INVALID_EMAIL)
            param.password = getSafePassword().toCharArray()
            val result = application.signInV2(param)
            assertTrue(result is SignInErrorV2)
            assertTrue((result as SignInErrorV2).isUserNotFound())
        }
    }

    /**
     * A wrong password supplied at the entry point is invalid credentials, matching MSAL iOS V2.
     */
    @Ignore("Native Auth V2 sign-in endpoints are not available on the E2E slice.")
    @Test
    fun testErrorEntryPasswordIsInvalidCredentials() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes, defaultCapabilities)

        runBlocking {
            val param = NativeAuthSignInParameters(username = config.email)
            param.password = INVALID_PASSWORD.toCharArray()
            val result = application.signInV2(param)
            assertTrue(result is SignInErrorV2)
            assertTrue((result as SignInErrorV2).isInvalidCredentials())
        }
    }

    /**
     * A wrong password submitted from the password-required state is an invalid password, not
     * invalid credentials, matching MSAL iOS V2. The caller retries on the state it already holds.
     */
    @Ignore("Native Auth V2 sign-in endpoints are not available on the E2E slice.")
    @Test
    fun testErrorDeferredPasswordIsInvalidPassword() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes, defaultCapabilities)

        runBlocking {
            val result = application.signInV2(NativeAuthSignInParameters(username = config.email))
            assertResult<NativeAuthResultV2.PasswordRequired>(result)
            val passwordState = (result as NativeAuthResultV2.PasswordRequired).nextState

            val wrongPassword = passwordState.submitPassword(INVALID_PASSWORD.toCharArray())
            assertTrue(wrongPassword is SubmitPasswordErrorV2)
            assertTrue((wrongPassword as SubmitPasswordErrorV2).isInvalidPassword())

            // Retrying on the retained state succeeds; the error carried no state of its own.
            val retry = passwordState.submitPassword(getSafePassword().toCharArray())
            assertResult<NativeAuthResultV2.Complete>(retry)
        }
    }

    /**
     * Password first factor followed by an email one-time code: submit an invalid code, request a
     * fresh challenge from the retained verification state, then submit the correct code and complete.
     * Also asserts the scopes requested at sign in are present in the resulting token.
     */
    @Ignore("Native Auth V2 sign-in endpoints are not available on the E2E slice.")
    @Test
    fun testSuccessWithEmailOtpMfaAfterInvalidCode() {
        config = getConfig(mfaConfigType)
        application = setupPCA(config, defaultChallengeTypes, defaultCapabilities)
        resources = config.resources

        retryOperation {
            runBlocking {
                val username = config.email
                val scopeA = resources[0].scopes[0]
                val scopeB = resources[0].scopes[1]

                val param = NativeAuthSignInParameters(username = username)
                param.password = getSafePassword().toCharArray()
                param.scopes = listOf(scopeA, scopeB)
                val result = application.signInV2(param)
                assertResult<NativeAuthResultV2.MFARequired>(result)

                val mfaState = (result as NativeAuthResultV2.MFARequired).nextState
                val emailMethod = result.authMethods.first { it.challengeChannel == "email" }

                val challengeResult = mfaState.selectAuthMethod(emailMethod)
                assertResult<NativeAuthResultV2.MFAVerificationRequired>(challengeResult)
                challengeResult as NativeAuthResultV2.MFAVerificationRequired
                assertNotNull(challengeResult.sentTo)
                assertEquals("email", challengeResult.channel)
                assertTrue(challengeResult.codeLength > 0)

                val incorrect = challengeResult.nextState.submitChallenge(INCORRECT_CODE)
                assertTrue(incorrect is MFASubmitChallengeErrorV2)
                assertTrue((incorrect as MFASubmitChallengeErrorV2).isInvalidCode())

                val freshChallenge = challengeResult.nextState.resendChallenge()
                assertResult<NativeAuthResultV2.MFAVerificationRequired>(freshChallenge)

                val otp = tempEmailApi.retrieveCodeFromInbox(username)
                val complete = (freshChallenge as NativeAuthResultV2.MFAVerificationRequired)
                    .nextState.submitChallenge(otp)
                assertResult<NativeAuthResultV2.Complete>(complete)

                val accountState = (complete as NativeAuthResultV2.Complete).resultValue
                val getAccessTokenResult =
                    accountState.getAccessToken(NativeAuthGetAccessTokenParameters())
                assertResult<GetAccessTokenResult.Complete>(getAccessTokenResult)
                val authResult = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue
                assertTrue(authResult.scope.contains(scopeA))
                assertTrue(authResult.scope.contains(scopeB))
            }
        }
    }

    /**
     * Selecting a method the current MFA state never offered is rejected without contacting the
     * service, and the retained state remains usable.
     */
    @Ignore("Native Auth V2 sign-in endpoints are not available on the E2E slice.")
    @Test
    fun testErrorSelectingAMethodTheServerDidNotOffer() {
        config = getConfig(mfaConfigType)
        application = setupPCA(config, defaultChallengeTypes, defaultCapabilities)

        runBlocking {
            val param = NativeAuthSignInParameters(username = config.email)
            param.password = getSafePassword().toCharArray()
            val result = application.signInV2(param)
            assertResult<NativeAuthResultV2.MFARequired>(result)

            val mfaState = (result as NativeAuthResultV2.MFARequired).nextState
            val stale = com.microsoft.identity.nativeauth.AuthMethod(
                id = "not-offered",
                challengeType = "oob",
                loginHint = null,
                challengeChannel = "email"
            )
            val staleResult = mfaState.selectAuthMethod(stale)
            assertTrue(staleResult is com.microsoft.identity.nativeauth.statemachine.errors.MFARequestChallengeErrorV2)

            // The real method still works afterwards.
            val emailMethod = result.authMethods.first { it.challengeChannel == "email" }
            assertResult<NativeAuthResultV2.MFAVerificationRequired>(mfaState.selectAuthMethod(emailMethod))
        }
    }

    /**
     * Signing in while an account is already signed in is rejected, preserving Android V1
     * behaviour. Repeated same-account and switch-account sign in are not supported in V2.
     */
    @Ignore("Native Auth V2 sign-in endpoints are not available on the E2E slice.")
    @Test
    fun testErrorWhenAnAccountIsAlreadySignedIn() {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes, defaultCapabilities)

        runBlocking {
            val first = NativeAuthSignInParameters(username = config.email)
            first.password = getSafePassword().toCharArray()
            assertResult<NativeAuthResultV2.Complete>(application.signInV2(first))

            val second = NativeAuthSignInParameters(username = config.email)
            second.password = getSafePassword().toCharArray()
            val result = application.signInV2(second)

            assertTrue(result is SignInErrorV2)
            val error = result as SignInErrorV2
            assertFalse(error.isInvalidCredentials())
            assertTrue(error.exception is MsalClientException)
            assertEquals("An account is already signed in.", error.exception!!.message)
        }
    }
}
