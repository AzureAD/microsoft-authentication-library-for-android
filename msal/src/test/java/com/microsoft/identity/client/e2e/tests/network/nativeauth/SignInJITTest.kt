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

import com.microsoft.identity.client.claims.ClaimsRequest
import com.microsoft.identity.client.e2e.utils.assertResult
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import com.microsoft.identity.nativeauth.parameters.NativeAuthChallengeAuthMethodParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthGetAccessTokenParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInContinuationParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignInParameters
import com.microsoft.identity.nativeauth.parameters.NativeAuthSignUpParameters
import com.microsoft.identity.nativeauth.statemachine.errors.MFASubmitChallengeError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.MFARequiredResult
import com.microsoft.identity.nativeauth.statemachine.results.RegisterStrongAuthChallengeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.nativeauth.statemachine.states.RegisterStrongAuthVerificationRequiredState
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.robolectric.RuntimeEnvironment.application
import org.robolectric.shadows.ShadowPackageManager.resources
import org.robolectric.versioning.AndroidVersions
import java.util.Base64

class SignInJITTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    private lateinit var resources: List<NativeAuthTestConfig.Resource>

    lateinit var application: INativeAuthPublicClientApplication
    lateinit var config: NativeAuthTestConfig.Config

    private val defaultConfigType = ConfigType.SIGN_IN_MFA_SINGLE_AUTH
    private val defaultChallengeTypes = listOf("password", "oob")

    /**
     * Full flow: Ensure JIT is triggered on first signIn
     * - SignUp a new user with username and password.
     * - Do not SignIn after signUp, but start a new signIn flow.
     * - Check that JIT flow is triggered.
     * - Specify a different email as verification contact.
     * - Complete JIT. Verification email should be sent to the second email.
     * - Access token is received.
     *
     */
    @Ignore("Retrieving OTP code failure.")
    @Test
    fun `test sign in specifying custom verification contact`()  {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)
        resources = config.resources
        val authenticationContextId = "c4"
        val authenticationContextRequestClaimJson = "{\"access_token\":{\"acrs\":{\"essential\":true,\"value\":\"$authenticationContextId\"}}}"
        val authenticationContextATClaimJson = "\"acrs\":[\"$authenticationContextId\"]"

        retryOperation {
            runBlocking {
                // SignUp a new user with username and password
                val username = tempEmailApi.generateRandomEmailAddressLocally()
                val signUpParams = NativeAuthSignUpParameters(username)
                signUpParams.password = getSafePassword().toCharArray()
                val signUpResult = application.signUp(signUpParams)
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp1 = tempEmailApi.retrieveCodeFromInbox(username)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp1)
                assertResult<SignUpResult.Complete>(submitCodeResult)

                // Start a new signIn flow. Check that JIT flow is triggered.
                val signInParams = NativeAuthSignInParameters(username)
                signInParams.password = getSafePassword().toCharArray()
                signInParams.claimsRequest = ClaimsRequest.getClaimsRequestFromJsonString(authenticationContextRequestClaimJson)
                val signInResult = application.signIn(signInParams)
                assertResult<SignInResult.StrongAuthMethodRegistrationRequired>(signInResult)
                val authMethod = (signInResult as SignInResult.StrongAuthMethodRegistrationRequired).authMethods[0]

                // Specify a different email as verification contact.
                val authMethodParams = NativeAuthChallengeAuthMethodParameters(authMethod)
                val contact = tempEmailApi.generateRandomEmailAddressLocally()
                authMethodParams.verificationContact = contact

                // Complete JIT. Verification email should be sent to the second email.
                val challengeResult = signInResult.nextState.challengeAuthMethod(authMethodParams)
                val otp2 = tempEmailApi.retrieveCodeFromInbox(contact)
                val submitChallengeResult = (challengeResult as  RegisterStrongAuthChallengeResult.VerificationRequired).result.getNextState().submitChallenge(otp2)
                assertResult<SignInResult.Complete>(submitChallengeResult)

                // Access token is received.
                val accountState = (submitChallengeResult as SignInResult.Complete).resultValue
                val accountParam = NativeAuthGetAccessTokenParameters()
                val getAccessTokenResult = accountState.getAccessToken(accountParam)
                assertResult<GetAccessTokenResult.Complete>(getAccessTokenResult)
                val authResult = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue
                assertNotNull(authResult)

                // Check that AT contains authentication context claim
                val atParts = authResult.accessToken.split(".")
                if (atParts.size != 3) {
                    fail("Invalid Access token received")
                    return@runBlocking
                }
                val atBody = atParts[1]
                val charset = Charsets.UTF_8
                val atDecoded = String(
                    Base64.getUrlDecoder().decode(atBody.toByteArray(charset)),
                    charset
                )
                assertTrue(atDecoded.contains(authenticationContextATClaimJson))
            }
        }
    }

    /**
     * Full flow: Ensure JIT is triggered in signIn after signUp (preverified)
     * - SignUp a new user with username and password.
     * - SignIn after signUp with authentication context as claims to trigger MFA. // TODO: tenant setting
     * - Check that JIT flow is triggered.
     * - Do not specify a verification contact.
     * - SignIn should be completed without needs to send a code to the email.
     * - Access token is received.
     *
     */
    @Ignore("Retrieving OTP code failure.")
    @Test
    fun `test sign after sign up without specify verification contact`()  {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)
        resources = config.resources
        val authenticationContextId = "c4"
        val authenticationContextRequestClaimJson = "{\"access_token\":{\"acrs\":{\"essential\":true,\"value\":\"$authenticationContextId\"}}}"
        val authenticationContextATClaimJson = "\"acrs\":[\"$authenticationContextId\"]"

        retryOperation {
            runBlocking {
                // SignUp a new user with username and password.
                val username = tempEmailApi.generateRandomEmailAddressLocally()
                val signUpParams = NativeAuthSignUpParameters(username)
                signUpParams.password = getSafePassword().toCharArray()
                val signUpResult = application.signUp(signUpParams)
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp1 = tempEmailApi.retrieveCodeFromInbox(username)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp1)
                assertResult<SignUpResult.Complete>(submitCodeResult)

                // SignIn after signUp with authentication context as claims to trigger MFA.
                val continuationParameters = NativeAuthSignInContinuationParameters()
                continuationParameters.claimsRequest = ClaimsRequest.getClaimsRequestFromJsonString(authenticationContextRequestClaimJson)
                val signWithContinuationResult = (submitCodeResult as SignUpResult.Complete).nextState.signIn(continuationParameters)

                // Check that JIT flow is triggered.
                assertResult<SignInResult.StrongAuthMethodRegistrationRequired>(signWithContinuationResult)
                val authMethod = (signWithContinuationResult as SignInResult.StrongAuthMethodRegistrationRequired).authMethods[0]
                // Do not specify a verification contact.
                val authMethodParams = NativeAuthChallengeAuthMethodParameters(authMethod)

                // SignIn should be completed without needs to send a code to the email.
                val challengeResult = signWithContinuationResult.nextState.challengeAuthMethod(authMethodParams)
                assertResult<SignInResult.Complete>(challengeResult)

                // Access token is received.
                val accountState = (challengeResult as SignInResult.Complete).resultValue
                val accountParam = NativeAuthGetAccessTokenParameters()
                val getAccessTokenResult = accountState.getAccessToken(accountParam)
                assertResult<GetAccessTokenResult.Complete>(getAccessTokenResult)
                val authResult = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue

                // Check that AT contains authentication context claim
                val atParts = authResult.accessToken.split(".")
                if (atParts.size != 3) {
                    fail("Invalid Access token received")
                    return@runBlocking
                }
                val atBody = atParts[1]
                val charset = Charsets.UTF_8
                val atDecoded = String(
                    Base64.getUrlDecoder().decode(atBody.toByteArray(charset)),
                    charset
                )
                assertTrue(atDecoded.contains(authenticationContextATClaimJson))
            }
        }
    }

    /**
     * Full flow: Ensure JIT is triggered in signIn after signUp and a second email is used as verification contact
     * - SignUp a new user with username and password.
     * - SignIn after signUp with authentication context as claims to trigger MFA. // TODO: tenant setting
     * - Check that JIT flow is triggered.
     * - Specify a different email as verification contact.
     * - Complete JIT. Verification email should be sent to the second email.
     * - Access token is received.
     *
     */
    @Ignore("Retrieving OTP code failure.")
    @Test
    fun `test sign after sign up with specify verification contact`()  {
        config = getConfig(defaultConfigType)
        application = setupPCA(config, defaultChallengeTypes)
        resources = config.resources
        val authenticationContextId = "c4"
        val authenticationContextRequestClaimJson = "{\"access_token\":{\"acrs\":{\"essential\":true,\"value\":\"$authenticationContextId\"}}}"
        val authenticationContextATClaimJson = "\"acrs\":[\"$authenticationContextId\"]"

        retryOperation {
            runBlocking {
                // SignUp a new user with username and password.
                val username = tempEmailApi.generateRandomEmailAddressLocally()
                val signUpParams = NativeAuthSignUpParameters(username)
                signUpParams.password = getSafePassword().toCharArray()
                val signUpResult = application.signUp(signUpParams)
                assertResult<SignUpResult.CodeRequired>(signUpResult)
                val otp1 = tempEmailApi.retrieveCodeFromInbox(username)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp1)
                assertResult<SignUpResult.Complete>(submitCodeResult)

                // SignIn after signUp with authentication context as claims to trigger MFA.
                val continuationParameters = NativeAuthSignInContinuationParameters()
                continuationParameters.claimsRequest = ClaimsRequest.getClaimsRequestFromJsonString(authenticationContextRequestClaimJson)
                val signWithContinuationResult = (submitCodeResult as SignUpResult.Complete).nextState.signIn(continuationParameters)

                // Check that JIT flow is triggered.
                assertResult<SignInResult.StrongAuthMethodRegistrationRequired>(signWithContinuationResult)
                val authMethod = (signWithContinuationResult as SignInResult.StrongAuthMethodRegistrationRequired).authMethods[0]
                // Specify a different email as verification contact.
                val authMethodParams = NativeAuthChallengeAuthMethodParameters(authMethod)
                val contact = tempEmailApi.generateRandomEmailAddressLocally()
                authMethodParams.verificationContact = contact

                // Complete JIT. Verification email should be sent to the second email.
                val challengeResult = signWithContinuationResult.nextState.challengeAuthMethod(authMethodParams)
                val otp2 = tempEmailApi.retrieveCodeFromInbox(contact)
                val submitChallengeResult = (challengeResult as  RegisterStrongAuthChallengeResult.VerificationRequired).result.getNextState().submitChallenge(otp2)
                assertResult<SignInResult.Complete>(submitChallengeResult)

                // Access token is received.
                val accountState = (submitChallengeResult as SignInResult.Complete).resultValue
                val accountParam = NativeAuthGetAccessTokenParameters()
                val getAccessTokenResult = accountState.getAccessToken(accountParam)
                assertResult<GetAccessTokenResult.Complete>(getAccessTokenResult)
                val authResult = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue

                // Check that AT contains authentication context claim
                val atParts = authResult.accessToken.split(".")
                if (atParts.size != 3) {
                    fail("Invalid Access token received")
                    return@runBlocking
                }
                val atBody = atParts[1]
                val charset = Charsets.UTF_8
                val atDecoded = String(
                    Base64.getUrlDecoder().decode(atBody.toByteArray(charset)),
                    charset
                )
                assertTrue(atDecoded.contains(authenticationContextATClaimJson))
            }
        }
    }
}
