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
package com.microsoft.identity.nativeauth

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.client.ILoggerCallback
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.internal.controllers.CommandDispatcherHelper
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.util.ResultFuture
import com.microsoft.identity.common.nativeauth.MockApiEndpoint
import com.microsoft.identity.common.nativeauth.MockApiResponseType
import com.microsoft.identity.common.nativeauth.MockApiUtils.Companion.configureMockApi
import com.microsoft.identity.internal.testutils.TestUtils
import com.microsoft.identity.nativeauth.statemachine.errors.ErrorTypes
import com.microsoft.identity.nativeauth.statemachine.errors.GetAccessTokenError
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordError
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordSubmitPasswordError
import com.microsoft.identity.nativeauth.statemachine.errors.SignInContinuationError
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpError
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpSubmitAttributesError
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordSubmitCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import com.microsoft.identity.nativeauth.statemachine.results.SignOutResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResendCodeResult
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.nativeauth.statemachine.errors.GetAccessTokenErrorTypes
import com.microsoft.identity.nativeauth.statemachine.states.SignInContinuationState
import com.microsoft.identity.nativeauth.utils.LoggerCheckHelper
import com.microsoft.identity.nativeauth.utils.mockCorrelationId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Arrays
import java.util.UUID
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


@ExperimentalCoroutinesApi
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(shadows = [ShadowAndroidSdkStorageEncryptionManager::class])
class NativeAuthPublicClientApplicationKotlinTest(private val allowPII: Boolean) : PublicClientApplicationAbstractTest() {
    private lateinit var context: Context
    private lateinit var components: IPlatformComponents
    private lateinit var activity: Activity
    private lateinit var application: INativeAuthPublicClientApplication
    private lateinit var loggerCheckHelper: LoggerCheckHelper
    private val username = "user@email.com"
    private val invalidUsername = "invalidUsername"
    private val password = "verySafePassword".toCharArray()
    private val code = "1234"
    private val emptyString = ""

    @Mock
    private lateinit var externalLogger: ILoggerCallback

    override fun getConfigFilePath() = "src/test/res/raw/native_auth_native_only_test_config.json"

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun data(): Collection<Boolean> {
            return listOf(true, false)
        }

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            BuildValues.setUseMockApiForNativeAuth(true)
        }

        @AfterClass
        @JvmStatic
        fun tearDownClass() {
            BuildValues.setUseMockApiForNativeAuth(false)
        }
    }

    @Before
    override fun setup() {
        MockitoAnnotations.initMocks(this)
        context = ApplicationProvider.getApplicationContext()
        components = AndroidPlatformComponentsFactory.createFromContext(context)
        activity = Mockito.mock(Activity::class.java)
        loggerCheckHelper = LoggerCheckHelper(externalLogger, allowPII)
        whenever(activity.applicationContext).thenReturn(context)
        setupPCA()
        CommandDispatcherHelper.clear()
    }

    @After
    fun cleanup() {
        loggerCheckHelper.checkSafeLogging()
        AcquireTokenTestHelper.setAccount(null)
        // remove everything from cache after test ends
        TestUtils.clearCache(SHARED_PREFERENCES_NAME)
    }

    private fun setupPCA() {
        val configFile = File(configFilePath)

        try {
            application = PublicClientApplication.createNativeAuthPublicClientApplication(context, configFile)
        } catch (e: MsalException) {
            fail(e.message)
        }
    }

    /**
     * Test sign in scenario 6:
     * 1a -> sign in with (invalid) username
     * 1b <- server returns invalid user error
     */
    @Test
    fun testSignInScenario6() = runTest {
        // 1. Sign in with username
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = MockApiResponseType.USER_NOT_FOUND
        )

        // 1b. Call SDK interface
        val codeRequiredResult = application.signIn(username)
        // 1a. Server returns invalid user error
        assertTrue(codeRequiredResult is SignInError )
        assertTrue((codeRequiredResult as SignInError).isUserNotFound())
    }

    /**
     * Test sign in scenario 7:
     * 1a -> sign in with (valid) username
     * 1b <- server requires code challenge
     * 2a -> submit (invalid) code
     * 2b <- server returns invalid code error, code challenge is still required
     * 3a -> submit valid code
     * 3b <- sign in succeeds
     */
    @Test
    fun testSignInScenario7() = runTest {
        // 1. Sign in with username
        // 1a. Setup server response
        var correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignInInitiate,
            correlationId = correlationId,
            responseType = MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val codeRequiredResult = application.signIn(username)
        // 1a. Server returns invalid user error
        assertTrue(codeRequiredResult is SignInResult.CodeRequired)
        val nextState = spy((codeRequiredResult as SignInResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 2. Submit (invalid) code
        // 2a. Setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )
        // 2b. Call SDK interface
        val invalidCodeResult = nextState.submitCode(code)
        // 2a. Server returns invalid code, stays in CodeRequired state
        assertTrue(invalidCodeResult is SubmitCodeError)
        assertTrue((invalidCodeResult as SubmitCodeError).isInvalidCode())

        // 3. Submit (valid) code
        // 3a. Setup server response
        correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        // 3b. Call SDK interface
        val successResult = nextState.submitCode(code)
        // 3a. Server accepts code, returns tokens
        assertTrue(successResult is SignInResult.Complete)
    }

    /**
     * Test sign in blocked (when account is already signed in)
     */
    @Test
    fun testSignInBlocked() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val result = application.signIn(username, password)
        assertTrue(result is SignInResult.Complete)

        val newResult = application.signIn(username)
        assertTrue(newResult is SignInError)
        assertTrue((newResult as SignInError).exception?.message.equals("An account is already signed in."))
    }

    /**
     * Test sign in with continuation token scenario 1:
     * 1a -> sign in with (valid) continuation token
     * 1b <- server returns token
     */
    @Test
    fun testSignInWithContinuationToken() = runTest {
        // Setup - sign up the user, so that we don't have to construct the ContinuationToken state manually
        // as this doesn't allow for the NativeAuthPublicClientApplicationConfiguration to be set
        // up, meaning it would need to be mocked (which we don't want in these tests).
        val signInWithContinuationTokenState = signUpUser()

        // 1a. sign in with (valid) continuation token
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        // 1b. server returns token
        val result = signInWithContinuationTokenState.signIn(scopes = null)
        assertTrue(result is SignInResult.Complete)
    }

    /**
     * Test sign in with continuation token scenario 2:
     * 1a -> sign in with (null) continuation token
     * 1b <- client returns error right away
     */
    @Test
    fun testSignInWithContinuationTokenNullContinuationToken() = runTest {
        // 1a. sign in with (null) continuation token
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        // 1b. client returns error
        val config = mock<NativeAuthPublicClientApplicationConfiguration>()
        val continuationTokenState = SignInContinuationState(
            continuationToken = null,
            correlationId = correlationId,
            username = username,
            config = config
        )
        val result = continuationTokenState.signIn(scopes = null)
        assertTrue(result is SignInContinuationError)

    }

    /**
     * Test sign in with continuation token scenario 2:
     * 1a -> sign in with (expired) continuation token
     * 1b <- server returns error
     */
    @Ignore("Waiting for continuation token Mock API Integration (Out of scope?)")
    @Test
    fun testSignInWithExpiredSLT() = runTest {
        // Setup - sign up the user, so that we don't have to construct the continuation token state manually
        // as this doesn't allow for the NativeAuthPublicClientApplicationConfiguration to be set
        // up, meaning it would need to be mocked (which we don't want in these tests).
        val signInWithContinuationTokenState = signUpUser()

        // 1a. sign in with (expired) SLT
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_EXPIRED_SLT
        )

        // 1b. server returns error
        val result = signInWithContinuationTokenState.signIn(scopes = null)
        assertTrue(result is SignInError)
        assertTrue((result as SignInError).errorType == null)
    }

    /**
     * Test sign in, sign out and sign in again
     */
    @Test
    fun testSignInSignOutSignIn() = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val signInResult = application.signIn(username, password)
        assertTrue(signInResult is SignInResult.Complete)

        val signOutResult = (signInResult as SignInResult.Complete).resultValue.signOut()
        assertTrue(signOutResult is SignOutResult.Complete)

        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val secondSignInResult = application.signIn(username, password)
        assertTrue(secondSignInResult is SignInResult.Complete)
    }

    /**
     * Test sign in, get access token. Compare to token from getAccount()
     */
    @Test
    fun testGetAccessToken() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val signInResult = application.signIn(username, password)
        assertTrue(signInResult is SignInResult.Complete)

        val accessTokenState = (signInResult as SignInResult.Complete).resultValue.getAccessToken()
        assertTrue(accessTokenState is GetAccessTokenResult.Complete)

        val accessToken = (accessTokenState as GetAccessTokenResult.Complete).resultValue.accessToken
        assertNotNull(accessToken)

        val getAccountResult = application.getCurrentAccount()
        assertTrue(getAccountResult is GetAccountResult.AccountFound)

        val accountState = (getAccountResult as GetAccountResult.AccountFound).resultValue
        assertEquals(accountState.correlationId, correlationId)

        val accessTokenResultTwo = accountState.getAccessToken()
        assertTrue(accessTokenResultTwo is GetAccessTokenResult.Complete)

        val authResult = (accessTokenResultTwo as GetAccessTokenResult.Complete).resultValue
        val accessTokenTwo = authResult.accessToken
        assertNotNull(accessTokenTwo)

        assertEquals(accessToken, accessTokenTwo)
        assertEquals(authResult.correlationId.toString(), correlationId)
    }

    /**
     * Test sign in, get access token with scopes. Compare to token from getAccount() and getAccessToken()
     */
    @Test
    fun testGetAccessTokenWithSignInScopes() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val signInResult = application.signIn(username, password)
        assertTrue(signInResult is SignInResult.Complete)

        val accessTokenState = (signInResult as SignInResult.Complete).resultValue.getAccessToken()
        assertTrue(accessTokenState is GetAccessTokenResult.Complete)

        val accessToken = (accessTokenState as GetAccessTokenResult.Complete).resultValue.accessToken
        assertNotNull(accessToken)

        val getAccountResult = application.getCurrentAccount()
        assertTrue(getAccountResult is GetAccountResult.AccountFound)

        val accessTokenResultTwo = (getAccountResult as GetAccountResult.AccountFound).resultValue.getAccessToken()
        assertTrue(accessTokenResultTwo is GetAccessTokenResult.Complete)

        val accessTokenTwo = (accessTokenResultTwo as GetAccessTokenResult.Complete).resultValue.accessToken
        assertNotNull(accessTokenTwo)

        assertEquals(accessToken, accessTokenTwo)

        val scopes = Arrays.asList(AuthenticationConstants.OAuth2Scopes.OPEN_ID_SCOPE)
        val accessTokenResultThree = (getAccountResult as GetAccountResult.AccountFound).resultValue.getAccessToken(false, scopes)
        assertTrue(accessTokenResultThree is GetAccessTokenResult.Complete)

        val accessTokenThree = (accessTokenResultThree as GetAccessTokenResult.Complete).resultValue.accessToken
        assertNotNull(accessTokenThree)

        assertEquals(accessTokenTwo, accessTokenThree)
        assertEquals(accessToken, accessTokenThree)
    }


    /**
     * Test sign in, get access token with empty scopes. Compare to token from getAccount() and getAccessToken()
     */
    @Test
    fun testGetAccessTokenWithEmptyScopes() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val signInResult = application.signIn(username, password)
        assertTrue(signInResult is SignInResult.Complete)

        val getAccountResult = application.getCurrentAccount()
        assertTrue(getAccountResult is GetAccountResult.AccountFound)

        val accountState = (getAccountResult as GetAccountResult.AccountFound).resultValue

        var accessTokenState = accountState.getAccessToken(false, emptyList())

        assertTrue(accessTokenState is GetAccessTokenError)
        assertTrue((accessTokenState as GetAccessTokenError).isInvalidScopes())
        val tokenError = accessTokenState as GetAccessTokenError
        assertEquals(tokenError.errorType, GetAccessTokenErrorTypes.INVALID_SCOPES)
        assertEquals(tokenError.correlationId, correlationId)

    }

    /**
     * Test sign in, sign out, get access token
     */
    @Test
    fun testSignOutGetAccessToken() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val signInResult = application.signIn(username, password)
        assertTrue(signInResult is SignInResult.Complete)

        val accountState = (signInResult as SignInResult.Complete).resultValue

        val signOutResult = accountState.signOut()
        assertTrue(signOutResult is SignOutResult.Complete)

        val accessTokenState = accountState.getAccessToken()
        assertTrue(accessTokenState is GetAccessTokenError)
        assertTrue((accessTokenState as GetAccessTokenError).isNoAccountFound())
    }

    /**
     * Test sign in, sign out, get access token
     */
    @Test
    fun testSignOutGetAccessTokenTwoParams() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val signInResult = application.signIn(username, password)
        assertTrue(signInResult is SignInResult.Complete)

        val accountState = (signInResult as SignInResult.Complete).resultValue

        val signOutResult = accountState.signOut()
        assertTrue(signOutResult is SignOutResult.Complete)

        val accessTokenState = accountState.getAccessToken(false, ArrayList<String>(AuthenticationConstants.DEFAULT_SCOPES))
        assertTrue(accessTokenState is GetAccessTokenError)
        assertTrue((accessTokenState as GetAccessTokenError).isNoAccountFound())
    }

    /**
     * Test sign in, sign out, get access token with empty scopes
     */
    @Test
    fun testSignOutGetAccessTokenTwoParamsEmptyScopes() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val signInResult = application.signIn(username, password)
        assertTrue(signInResult is SignInResult.Complete)

        val accountState = (signInResult as SignInResult.Complete).resultValue

        val signOutResult = accountState.signOut()
        assertTrue(signOutResult is SignOutResult.Complete)

        var accessTokenState = accountState.getAccessToken(false, emptyList())

        assertTrue(accessTokenState is GetAccessTokenError)
        assertTrue((accessTokenState as GetAccessTokenError).isInvalidScopes())
        assertEquals((accessTokenState as GetAccessTokenError).errorType, GetAccessTokenErrorTypes.INVALID_SCOPES)

    }

    /**
     * Test SSPR scenario 3.2.1:
     * 1 -> USER click resetPassword
     * 1 <- user found, SERVER requires code verification
     * 2 -> USER submit valid code
     * 2 <- code valid, SERVER requires new password to be set
     * 3 -> USER submit valid password
     * 3 <- password reset succeeds
     */
    @Test
    fun testSSPRScenario3_2_1() = runTest {
        var nextState: Any?
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        val resetPasswordResult = application.resetPassword(username = username)
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        assertTrue(resetPasswordResult is ResetPasswordStartResult.CodeRequired)
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        nextState = spy((resetPasswordResult as ResetPasswordStartResult.CodeRequired).nextState)

        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 2. Submit valid code
        // 2_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        // 2a. Call SDK interface - submitCode()
        val submitCodeResult = nextState.submitCode(code = code)
        // 2b. Transform /continue(success) to Result(PasswordRequired).
        assertTrue(submitCodeResult is ResetPasswordSubmitCodeResult.PasswordRequired)
        // 2c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordPasswordRequired state.
        nextState = spy((submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 3. Submit valid password
        // 3_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )
        // 3_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )
        // 3a. Call SDK interface - submitPassword()
        val submitPasswordResult = nextState.submitPassword(password = password)
        // 3b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
    }

    /**
     * Test SSPR scenario 3.2.2:
     * 1 -> USER click resetPassword
     * 1 <- user found, SERVER requires code verification
     * 2 -> USER submit valid code
     * 2 <- code valid, SERVER requires new password to be set
     * 3 -> USER submit valid password
     * 3 <- password reset succeeds
     * 4 -> USER calls sign in on the provided state
     * 4 <- SERVER returns tokens
     */
    @Test
    fun testSSPRScenario3_2_2() = runTest {
        var nextState: Any?
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        val resetPasswordResult = application.resetPassword(username = username)
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        assertTrue(resetPasswordResult is ResetPasswordStartResult.CodeRequired)
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        nextState = spy((resetPasswordResult as ResetPasswordStartResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 2. Submit valid code
        // 2_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        // 2a. Call SDK interface - submitCode()
        val submitCodeResult = nextState.submitCode(code = code)
        // 2b. Transform /continue(success) to Result(PasswordRequired).
        assertTrue(submitCodeResult is ResetPasswordSubmitCodeResult.PasswordRequired)
        // 2c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordPasswordRequired state.
        nextState = spy((submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 3. Submit valid password
        // 3_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )
        // 3_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )
        // 3a. Call SDK interface - submitPassword()
        val submitPasswordResult = nextState.submitPassword(password = password)
        // 3b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
        // 3c. Respond to Result(Complete): shifting from ResetPasswordPasswordRequired to end.
        assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
        val signInWithContinuationTokenState = spy((submitPasswordResult as ResetPasswordResult.Complete).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        signInWithContinuationTokenState.mockCorrelationId(correlationId)

        // 4a. Sign in with (valid) continuation token
        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        // 4b. Server returns tokens
        val result = signInWithContinuationTokenState.signIn(scopes = null)
        assertTrue(result is SignInResult.Complete)
    }

    /**
     * Test SSPR scenario 3.2.3
     * 1 -> USER click resetPassword
     * 1 <- user found, SERVER requires code verification
     * 2 -> USER submit valid code
     * 2 <- code valid, SERVER requires new password to be set
     * 3 -> USER submit invalid password
     * 3 <- password invalid, SERVER returns error
     * 4 -> USER submit valid password
     * 4 <- password reset succeeds
     */
    fun testSSPRScenario3_2_3() = runTest {
        var nextState: Any?
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        val resetPasswordResult = application.resetPassword(username = username)
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        assertTrue(resetPasswordResult is ResetPasswordStartResult.CodeRequired)
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        nextState = spy((resetPasswordResult as ResetPasswordStartResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 2. Submit valid code
        // 2_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        // 2a. Call SDK interface - submitCode()
        val submitCodeResult = nextState.submitCode(code = code)
        // 2b. Transform /continue(success) to Result(PasswordRequired).
        assertTrue(submitCodeResult is ResetPasswordSubmitCodeResult.PasswordRequired)
        // 2c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordCodeRequired state.
        nextState = spy((submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 3. Submit invalid password
        // 3_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Error: password too weak
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )
        // 3a. Call SDK interface
        var submitPasswordResult = nextState.submitPassword(password = password)
        // 3b. Transform /submit(error) to Result(Complete).
        assertTrue(submitPasswordResult is ResetPasswordSubmitPasswordError)
        assertTrue((submitPasswordResult as ResetPasswordSubmitPasswordError).isInvalidPassword())

        // 4. Submit valid password
        // 4_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )
        // 4_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )
        // 4a. Call SDK interface
        submitPasswordResult = nextState.submitPassword(password = password)
        // 4b. Transform /submit(error) + /resetpassword/poll_completion(success) to Result(Complete).
        assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
    }

    /**
     * Test SSPR scenario 3.2.4:
     * 1 -> USER click resetPassword
     * 1 <- user found, SERVER requires code verification
     * 2 -> USER click resend code
     * 2 <- code sent, SERVER returns success
     * 3 -> USER submit valid code
     * 3 <- code valid, SERVER requires new password to be set
     * 4 -> USER submit valid password
     * 4 <- password reset succeeds
     */
    @Test
    fun testSSPRScenario3_2_4() = runTest {
        var nextState: Any?
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        val resetPasswordResult = application.resetPassword(username = username)
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        assertTrue(resetPasswordResult is ResetPasswordStartResult.CodeRequired)
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        nextState = spy((resetPasswordResult as ResetPasswordStartResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 2. Click resend code
        // 2_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        // 2a. Call SDK interface - resendCode()
        val resendCodeResult = nextState.resendCode()
        // 2b. Transform /challenge(success) to Result(CodeRequired).
        assertTrue(resendCodeResult is ResetPasswordResendCodeResult.Success)

        // 3. Submit valid code
        // 3_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        // 3a. Call SDK interface - submitCode()
        val submitCodeResult = nextState.submitCode(code = code)
        // 3b. Transform /continue(success) to Result(PasswordRequired).
        assertTrue(submitCodeResult is ResetPasswordSubmitCodeResult.PasswordRequired)
        // 3c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordCodeRequired state.
        nextState = spy((submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 4. Submit valid password
        // 4_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )
        // 4_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )
        // 4a. Call SDK interface
        val submitPasswordResult = nextState.submitPassword(password = password)
        // 4b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
    }

    /**
     * Test SSPR scenario 3.2.5:
     * 1 -> USER click resetPassword
     * 1 <- user not found, SERVER returns error
     */
    @Test
    fun testSSPRScenario3_2_5() = runTest {
        var nextState: Any?
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Error: user not found
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.USER_NOT_FOUND
        )
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        val resetPasswordResult = application.resetPassword(username = username)
        // 1b. Transform /start(error) to Result(UserNotFound)
        assertTrue(resetPasswordResult is ResetPasswordError)
        assertTrue((resetPasswordResult as ResetPasswordError).isUserNotFound())
    }

    /**
     * Test SSPR scenario 3.2.6:
     * 1 -> USER click resetPassword
     * 1 <- challenge type do no support, SERVER requires error: redirect
     */
    @Test
    fun testSSPRScenario3_2_6() = runTest {
        var nextState: Any?
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Error: redirect
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        val resetPasswordResult = application.resetPassword(username = username)
        // 1b. Transform /start(error) to Result(UserNotFound)
        assertTrue(resetPasswordResult is ResetPasswordError)
        assertTrue((resetPasswordResult as ResetPasswordError).isBrowserRequired())
    }

    /**
     * 1 -> USER click resetPassword
     * 1 <- challenge type do no support, SERVER returns error: invalid request
     */
    @Test
    fun testSSPRScenarioUnknownError() = runTest {
        var nextState: Any?
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Error: invalid request
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.UNSUPPORTED_CHALLENGE_TYPE
        )
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        val resetPasswordResult = application.resetPassword(username = username)
        // 1b. Transform /start(error) to Result(UnexpectedError)
        assertTrue(resetPasswordResult is ResetPasswordError)
        assertTrue((resetPasswordResult as ResetPasswordError).errorType == null)
    }

    /**
     * Test SSPR scenario 3.2.10:
     * 1 -> USER click resetPassword
     * 1 <- user found, SERVER requires code verification
     * 2 -> USER submit invalid code
     * 2 <- code invalid, SERVER returns error
     * 3 -> USER submit valid code
     * 3 <- code valid, SERVER requires new password to be set
     * 4 -> USER submit valid password
     * 4 <- password reset succeeds
     */
    @Test
    fun testSSPRScenario3_2_10() = runTest {
        var nextState: Any?
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_START_SUCCESS
        )
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        val resetPasswordResult = application.resetPassword(username = username)
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        assertTrue(resetPasswordResult is ResetPasswordStartResult.CodeRequired)
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        nextState = spy((resetPasswordResult as ResetPasswordStartResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 2. Submit invalid code
        // 2_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Error: explicit invalid oob value
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )
        // 2a. Call SDK interface - submitCode()
        var submitCodeResult = nextState.submitCode(code = code)
        // 2b. Transform /continue(error) to Result(CodeIncorrect).
        assertTrue(submitCodeResult is SubmitCodeError)
        assertTrue((submitCodeResult as SubmitCodeError).isInvalidCode())

        // 3. Submit valid code
        // 3_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_CONTINUE_SUCCESS
        )
        // 3a. Call SDK interface - submitCode()
        submitCodeResult = nextState.submitCode(code = code)
        // 3b. Transform /continue(error) to Result(CodeIncorrect).
        assertTrue(submitCodeResult is ResetPasswordSubmitCodeResult.PasswordRequired)
        // 3c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordCodeRequired state.
        nextState = spy((submitCodeResult as ResetPasswordSubmitCodeResult.PasswordRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        // 4. Submit valid password
        // 4_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRSubmit,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_SUBMIT_SUCCESS
        )
        // 4_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        configureMockApi(
            endpointType = MockApiEndpoint.SSPRPoll,
            correlationId = correlationId,
            responseType = MockApiResponseType.SSPR_POLL_SUCCESS
        )
        // 4a. Call SDK interface
        val submitPasswordResult = nextState.submitPassword(password = password)
        // 4b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        assertTrue(submitPasswordResult is ResetPasswordResult.Complete)
    }

    /**
     * Test sign up with password blocked (when account is already signed in)
     */
    @Test
    fun testSignUpPasswordBlocked() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val result = application.signIn(username, password)
        assertTrue(result is SignInResult.Complete)

        val newResult = application.signUp(username)
        assertTrue(newResult is SignUpError)
        assertTrue((newResult as SignUpError).exception?.message.equals("An account is already signed in."))
    }

    /**
     * Test sign up blocked (when account is already signed in)
     */
    @Test
    fun testSignUpBlocked() = runTest {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val result = application.signIn(username, password)
        assertTrue(result is SignInResult.Complete)

        val newResult = application.signUp(username)
        assertTrue(newResult is SignUpError)
        assertTrue((newResult as SignUpError).exception?.message.equals("An account is already signed in."))
    }

    /**
     * Test reset password blocked (when account is already signed in)
     */
    @Test
    fun testResetPasswordBlocked() = runBlocking {
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = correlationId,
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val result = application.signIn(username, password)
        assertTrue(result is SignInResult.Complete)

        val newResult = application.resetPassword(username)
        assertTrue(newResult is ResetPasswordError)
        assertTrue((newResult as ResetPasswordError).exception?.message.equals("An account is already signed in."))
    }

    /**
     * Test sign in scenario 9:
     * 1a -> sign in with username and password
     * 1b <- server returns INVALID_AUTHENTICATION_METHOD error
     * 2a -> sign in initiate is called
     * 2b <- server returns success
     * 3a -> sign in challenge is called
     * 3b <- server returns oob required
     * 3c <- Call SDK interface
     * 3d <- CodeRequired is returned to the user
     */
    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun testSignInScenario9() {
        // 1. Sign in with username and password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInToken,
            correlationId,
            MockApiResponseType.INVALID_AUTHENTICATION_METHOD
        )

        // 2a. Sign in with username
        // 2b. Setup server response
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.INITIATE_SUCCESS
        )

        // 3a. Sign in challenge
        // 3b. Setup server response with oob required
        configureMockApi(
            MockApiEndpoint.SignInChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 3c. Call SDK interface
        val signInResult = ResultFuture<SignInResult>()
        val callback: NativeAuthPublicClientApplication.SignInCallback = object : NativeAuthPublicClientApplication.SignInCallback {
            override fun onResult(result: SignInResult) {
                signInResult.setResult(result)
            }

            override fun onError(exception: BaseException) {
                signInResult.setException(exception)
            }
        }
        application.signIn(username, password, null, callback)
        // 3d. Server returns InvalidAuthMethodForUser error
        assertTrue(signInResult[30, TimeUnit.SECONDS] is SignInResult.CodeRequired)
    }

    /* Test sign in scenario 10:
     * 1a -> sign in with username and password
     * 1b <- server returns INVALID_AUTHENTICATION_METHOD error
     * 2a -> sign in initiate is called
     * 2b <- server returns redirect
     * 3a <- Call SDK interface
     * 3b <- BrowserRequired is returned to the user
     */
    @Test
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    fun testSignInScenario10() {
        // 1. Sign in with username and password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignInToken,
            correlationId,
            MockApiResponseType.INVALID_AUTHENTICATION_METHOD
        )

        // 2a. Sign in with username
        // 2b. Setup server response
        configureMockApi(
            MockApiEndpoint.SignInInitiate,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        // 3a. Call SDK interface
        val signInResult = ResultFuture<SignInResult>()
        val callback: NativeAuthPublicClientApplication.SignInCallback = object : NativeAuthPublicClientApplication.SignInCallback {
            override fun onResult(result: SignInResult) {
                signInResult.setResult(result)
            }

            override fun onError(exception: BaseException) {
                signInResult.setException(exception)
            }
        }
        application.signIn(username, password, null, callback)
        // 3b. Server returns BrowserRequired error
        assertTrue(signInResult[30, TimeUnit.SECONDS] is SignInError)
        val result = signInResult.get() as SignInError
        assertTrue(result.isBrowserRequired())
    }

    /**
     * Check that we don't get a type casting exception thrown when we get it,
     * should get error result instead.
     */
    @Test
    fun testSignInEmptyUsernameNoException() = runTest {
        configureMockApi(
            endpointType = MockApiEndpoint.SignInToken,
            correlationId = UUID.randomUUID().toString(),
            responseType = MockApiResponseType.TOKEN_SUCCESS
        )

        val result = application.signIn(emptyString, password)
        assertTrue(result is SignInError)
        assertTrue((result as SignInError).errorType == ErrorTypes.INVALID_USERNAME)
    }

    // Helper methods
    @Throws(
        ExecutionException::class,
        InterruptedException::class,
        TimeoutException::class
    )
    private suspend fun signUpUser(): SignInContinuationState {
        // 1. sign up with password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()
        configureMockApi(
            MockApiEndpoint.SignUpStart,
            correlationId,
            MockApiResponseType.SIGNUP_START_SUCCESS
        )
        configureMockApi(
            MockApiEndpoint.SignUpChallenge,
            correlationId,
            MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username, password)
        assertTrue(result is SignUpResult.CodeRequired)

        // 2. submit (valid) code
        // 2a. setup server response
        configureMockApi(
            MockApiEndpoint.SignUpContinue,
            correlationId,
            MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )
        val submitCodeState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        submitCodeState.mockCorrelationId(correlationId)

        val submitCodeResult = submitCodeState.submitCode(code)
        assertTrue(submitCodeResult is SignUpResult.Complete)
        return (submitCodeResult as SignUpResult.Complete).nextState
    }

    /**
     * Test Sign Up scenario 1:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 2b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario1() = runTest {
        // 1. sign up with password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username, password)

        assertTrue(result is SignUpResult.CodeRequired)

        // 2. submit (valid) code
        // 2a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val submitCodeState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        submitCodeState.mockCorrelationId(correlationId)

        val successResult = submitCodeState.submitCode(code)

        // 2b. Server accepts code, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 2:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> user prompts resend code, challenge endpoint is called
     * 2b <- codeRequired is returned
     * 3a -> submitCode is called with valid code
     * 3b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario2() = runTest {
        // 1. sign up with password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username, password)

        assertTrue(result is SignUpResult.CodeRequired)

        // 2a. Setup resend code challenge
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val codeRequiredState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        codeRequiredState.mockCorrelationId(correlationId)

        // 2b. Call resendCode
        val resendCodeResult = codeRequiredState.resendCode()
        assertTrue(resendCodeResult is SignUpResendCodeResult.Success)

        // 3. submit (valid) code
        // 3a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val submitCodeState = spy((resendCodeResult as SignUpResendCodeResult.Success).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        submitCodeState.mockCorrelationId(correlationId)

        val successResult = submitCodeState.submitCode(code)

        // 3b. Server accepts code, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 3:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 3a <- sign up token has expired, server returns token expired error
     */
    @Test
    fun testSignUpScenario3() = runTest {
        // 1. sign up
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username, password)

        assertTrue(result is SignUpResult.CodeRequired)

        // 2. submit (valid) code
        // 2a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.EXPIRED_TOKEN
        )

        val nextState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        val expiredTokenResult = nextState.submitCode(code)

        assertTrue(expiredTokenResult is SubmitCodeError)
        assertTrue((expiredTokenResult as SubmitCodeError).errorType == null)
    }

    /**
     * Test Sign Up scenario 4:
     * 1a -> signUp
     * 1b <- server does not support password authentication
     */
    @Test
    fun testSignUpScenario4() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        // 1b. Call SDK interface
        val result = application.signUp(username, password)

        assertTrue(result is SignUpError)
        assertTrue((result as SignUpError).isBrowserRequired())
    }

    /**
     * Test Sign Up scenario 5:
     * 1a -> signUp
     * 1b <- server does not support password authentication, returns error to use OOB instead
     */
    @Test
    fun testSignUpScenario5() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.AUTH_NOT_SUPPORTED
        )

        // 1b. Call SDK interface
        val result = application.signUp(username, password)

        assertTrue(result is SignUpError)
        assertTrue((result as SignUpError).isAuthNotSupported())
    }

    /**
     * Test Sign Up scenario 6:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> user prompts resend code, challenge endpoint is called
     * 2b <- codeRequired is returned
     * 3a -> submit valid code
     * 3b <- server returns error requiring attributes
     * 4a -> submit invalid attributes
     * 4b <- server returns error indicating invalid attributes
     * 5a -> submit valid attributes
     * 5b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario6() = runTest {
        // 1. sign up with password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username, password)

        assertTrue(result is SignUpResult.CodeRequired)

        // 3. submit (valid) code
        // 3a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.ATTRIBUTES_REQUIRED
        )

        val codeRequiredState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        codeRequiredState.mockCorrelationId(correlationId)

        val attributesRequiredResult = codeRequiredState.submitCode(code)
        assertTrue(attributesRequiredResult is SignUpResult.AttributesRequired)

        // 4. Submit invalid attributes
        // 4a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.VALIDATION_FAILED
        )

        val attributesRequiredState = spy((attributesRequiredResult as SignUpResult.AttributesRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        attributesRequiredState.mockCorrelationId(correlationId)

        val invalidAttributes = UserAttributes.Builder.customAttribute("attribute", "invalid_attribute").build()
        val attributesFailedResult = attributesRequiredState.submitAttributes(invalidAttributes)

        assertTrue(attributesFailedResult is SignUpSubmitAttributesError)
        assertTrue((attributesFailedResult as SignUpSubmitAttributesError).isInvalidAttributes())

        // 4. Submit invalid attributes
        // 4a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val validAttributes = UserAttributes.Builder.customAttribute("attribute", "valid_attribute").build()
        val successResult = attributesRequiredState.submitAttributes(validAttributes)

        // 4b. Server accepts password, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 7:
     * 1a -> signUp with invalid custom attributes
     * 1b <- server returns invalid attribute error
     * 2a -> call signUpWithPassword with correct attributes
     * 2b <- server returns code required, flow continues
     */
    @Test
    fun testSignUpScenario7() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.VALIDATION_FAILED
        )

        // 1b. Call SDK interface
        val invalidAttributes = UserAttributes.Builder.customAttribute("attribute", "invalid_attribute").build()
        val invalidAttributesResult = application.signUp(username, password, invalidAttributes)

        assertTrue(invalidAttributesResult is SignUpError)
        assertTrue((invalidAttributesResult as SignUpError).isInvalidAttributes())

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 2b. Call SDK interface again
        val validAttributes = UserAttributes.Builder.customAttribute("attribute", "valid_attribute").build()
        val result = application.signUp(username, password, validAttributes)
        assertTrue(result is SignUpResult.CodeRequired)
    }

    /**
     * Test Sign Up scenario 8:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 2b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario8() = runTest {
        // 1. sign up with password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username)
        assertTrue(result is SignUpResult.CodeRequired)

        // 2. submit (valid) code
        // 2a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val submitCodeState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        submitCodeState.mockCorrelationId(correlationId)

        val successResult = submitCodeState.submitCode(code)

        // 2b. Server accepts code, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 9:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> user prompts resend code, challenge endpoint is called
     * 2b <- codeRequired is returned
     * 3a -> submitCode is called with valid code
     * 3b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario9() = runTest {
        // 1. sign up with password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username)
        assertTrue(result is SignUpResult.CodeRequired)

        // 2a. Setup resend code challenge
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val codeRequiredState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        codeRequiredState.mockCorrelationId(correlationId)

        // 2b. Call resendCode
        val resendCodeResult = codeRequiredState.resendCode()
        assertTrue(resendCodeResult is SignUpResendCodeResult.Success)

        // 3. submit (valid) code
        // 3a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val submitCodeState = spy((resendCodeResult as SignUpResendCodeResult.Success).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        submitCodeState.mockCorrelationId(correlationId)

        val successResult = submitCodeState.submitCode(code)

        // 3b. Server accepts code, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 10:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 2b <- server returns error requiring additional authentication (password)
     * 3a -> submit valid password
     * 3b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario10() = runTest {
        // 1. sign up
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username)
        assertTrue(result is SignUpResult.CodeRequired)

        // 2. submit (valid) code
        // 2a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.CREDENTIAL_REQUIRED
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        val nextState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        val passwordRequiredResult = nextState.submitCode(code)

        assertTrue(passwordRequiredResult is SignUpResult.PasswordRequired)

        // 3. Submit valid password
        // 3a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val passwordRequiredState = spy((passwordRequiredResult as SignUpResult.PasswordRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        passwordRequiredState.mockCorrelationId(correlationId)

        val successResult = passwordRequiredState.submitPassword(password)

        // 3b. Server accepts password, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 11:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> user prompts resend code, challenge endpoint is called
     * 2b <- codeRequired is returned
     * 3a -> submit valid code
     * 3b <- server returns error requiring additional authentication (password)
     * 4a -> submit valid password
     * 4b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario11() = runTest {
        // 1. sign up with password
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username)
        assertTrue(result is SignUpResult.CodeRequired)

        // 2a. Setup resend code challenge
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val codeRequiredState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        codeRequiredState.mockCorrelationId(correlationId)

        // 2b. Call resendCode
        val resendCodeResult = codeRequiredState.resendCode()
        assertTrue(resendCodeResult is SignUpResendCodeResult.Success)

        // 3. submit (valid) code
        // 3a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.CREDENTIAL_REQUIRED
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        val submitCodeState = spy((resendCodeResult as SignUpResendCodeResult.Success).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        submitCodeState.mockCorrelationId(correlationId)

        val passwordRequiredResult = submitCodeState.submitCode(code)

        assertTrue(passwordRequiredResult is SignUpResult.PasswordRequired)

        // 4. Submit valid password
        // 4a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val passwordRequiredState = spy((passwordRequiredResult as SignUpResult.PasswordRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        passwordRequiredState.mockCorrelationId(correlationId)

        val successResult = passwordRequiredState.submitPassword(password)

        // 4b. Server accepts password, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 12:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 2b <- server returns error requiring additional authentication (password)
     * 3a -> submit valid password
     * 3b <- server returns error requiring additional information (user attributes)
     * 4a -> submit valid attributes
     * 4b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario12() = runTest {
        // 1. sign up
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username)
        assertTrue(result is SignUpResult.CodeRequired)

        // 2. submit (valid) code
        // 2a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.CREDENTIAL_REQUIRED
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        )

        val nextState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        nextState.mockCorrelationId(correlationId)

        val passwordRequiredResult = nextState.submitCode(code)

        assertTrue(passwordRequiredResult is SignUpResult.PasswordRequired)

        // 3. submit required attributes
        // 3a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.ATTRIBUTES_REQUIRED
        )

        val passwordRequiredState = spy((passwordRequiredResult as SignUpResult.PasswordRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        passwordRequiredState.mockCorrelationId(correlationId)

        val attributesRequiredResult = passwordRequiredState.submitPassword(password)

        assertTrue(attributesRequiredResult is SignUpResult.AttributesRequired)

        // 4. Submit valid password
        // 4a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val attributesRequiredState = spy((attributesRequiredResult as SignUpResult.AttributesRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        attributesRequiredState.mockCorrelationId(correlationId)

        val attributes = UserAttributes.Builder.customAttribute("attribute", "attribute").build()
        val successResult = attributesRequiredState.submitAttributes(attributes)

        // 4b. Server accepts attributes, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 13:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 2b <- server returns error requiring additional information (user attributes)
     * 3a -> submit incomplete set of attributes
     * 3b <- server returns another error specifying missing attribute fields
     * 4a -> submit complete set of attributes
     * 4b <- sign up succeeds
     */
    @Test
    fun testSignUpScenario13() = runTest {
        // 1. sign up
        // 1a. Setup server response
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        // 1b. Call SDK interface
        val result = application.signUp(username)
        assertTrue(result is SignUpResult.CodeRequired)

        // 2. submit (valid) code
        // 2a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.ATTRIBUTES_REQUIRED
        )

        val submitCodeState = spy((result as SignUpResult.CodeRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        submitCodeState.mockCorrelationId(correlationId)

        val attributesRequiredResult = submitCodeState.submitCode(code)

        assertTrue(attributesRequiredResult is SignUpResult.AttributesRequired)
        attributesRequiredResult as SignUpResult.AttributesRequired
        assertNotNull(attributesRequiredResult.requiredAttributes)

        // 3. submit required attributes
        // 3a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.ATTRIBUTES_REQUIRED
        )

        val attributesRequiredState = spy((attributesRequiredResult).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        attributesRequiredState.mockCorrelationId(correlationId)

        val incompleteAttributes = UserAttributes.Builder.customAttribute("attribute", "incomplete_attribute").build()
        val additionalAttributesRequiredResult = attributesRequiredState.submitAttributes(incompleteAttributes)

        assertTrue(additionalAttributesRequiredResult is SignUpResult.AttributesRequired)

        // 4. Submit valid password
        // 4a. setup server response
        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        )

        val additionalAttributesRequiredState = spy((additionalAttributesRequiredResult as SignUpResult.AttributesRequired).nextState)
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        additionalAttributesRequiredState.mockCorrelationId(correlationId)

        val attributes = UserAttributes.Builder.customAttribute("attribute", "attribute").build()
        val successResult = additionalAttributesRequiredState.submitAttributes(attributes)

        // 4b. Server accepts password, returns tokens
        assertTrue(successResult is SignUpResult.Complete)
    }

    /**
     * Test Sign Up scenario 14:
     * 1a -> signUp
     * 1b <- server does not support code authentication
     */
    @Test
    fun testSignUpScenario14() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        // 1b. Call SDK interface
        val result = application.signUp(username)
        assertTrue(result is SignUpError)
        assertTrue((result as SignUpError).isBrowserRequired())
    }

    @Test
    fun testSignUpInvalidOTPReturnsInvalidCodeError() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val result = application.signUp(username)
        assertTrue(result is SignUpResult.CodeRequired)

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )

        val submitCodeState = spy((result as SignUpResult.CodeRequired).nextState)
        val submitCodeResult = submitCodeState.submitCode("INVALID_CODE")

        assertTrue(submitCodeResult is SubmitCodeError)
        assertTrue((submitCodeResult as SubmitCodeError).isInvalidCode())
    }

    /**
     * Check that we don't get a type casting exception thrown when we get it,
     * should get error result instead.
     */
    @Test
    fun testSignUpNullUsernameNoException() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        //Call SDK interface
        val result = application.signUp(emptyString)
        assertTrue(result is SignUpError)
        assertTrue((result as SignUpError).errorType == ErrorTypes.INVALID_USERNAME)
    }

    /**
     * Check that we don't get a type casting exception thrown when we get it,
     * should get error result instead.
     */
    @Test
    fun testResetPasswordNullUsernameNoException() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SSPRStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        )

        // 1b. Call SDK interface
        val result = application.resetPassword(emptyString)
        assertTrue(result is ResetPasswordError)
        assertTrue((result as ResetPasswordError).errorType == ErrorTypes.INVALID_USERNAME)
    }

    @Test
    fun testSignUpInvalidPasswordReturnsError() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.PASSWORD_TOO_WEAK
        )

        val result = application.signUp(username, password)
        assertTrue(result is SignUpError)
        assertTrue((result as SignUpError).isInvalidPassword())
    }

    @Test
    fun testSignUpWithPasswordInvalidEmailReturnsError() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_USERNAME
        )

        val result = application.signUp(invalidUsername, password)
        assertTrue(result is SignUpError)
        assertTrue((result as SignUpError).isInvalidUsername())
    }

    @Test
    fun testSignUpInvalidEmailReturnsError() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_USERNAME
        )

        val result = application.signUp(invalidUsername)
        assertTrue(result is SignUpError)
        assertTrue((result as SignUpError).isInvalidUsername())
    }

    @Test
    fun testEmptyRequestParametersToGenericErrorNotThrownException() = runTest {
        val correlationId = UUID.randomUUID().toString()

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpStart,
            correlationId = correlationId,
            responseType = MockApiResponseType.SIGNUP_START_SUCCESS
        )

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpChallenge,
            correlationId = correlationId,
            responseType = MockApiResponseType.CHALLENGE_TYPE_OOB
        )

        val result = application.signUp(username)
        assertTrue(result is SignUpResult.CodeRequired)

        configureMockApi(
            endpointType = MockApiEndpoint.SignUpContinue,
            correlationId = correlationId,
            responseType = MockApiResponseType.INVALID_OOB_VALUE
        )

        val submitCodeState = spy((result as SignUpResult.CodeRequired).nextState)
        val submitCodeResult = submitCodeState.submitCode(emptyString)  // Empty code will trigger ArgUtils.validateNonNullArg(oob, "oob") of the SignUpContinueRequest to thrown ClientException
        assertTrue(submitCodeResult is SubmitCodeError)
        assertTrue((submitCodeResult as SubmitCodeError).error.equals("unsuccessful_command")) // ClientException will be caught in CommandResultUtil.kt and converted to generic error in interface layer
    }
}
