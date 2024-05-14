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
package com.microsoft.identity.nativeauth;

import static com.microsoft.identity.common.java.nativeauth.BuildValues.*;

import android.app.Activity;
import android.content.Context;

import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.tests.PublicClientApplicationAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.nativeauth.statemachine.errors.GetAccessTokenError;
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordError;
import com.microsoft.identity.nativeauth.statemachine.errors.ResetPasswordSubmitPasswordError;
import com.microsoft.identity.nativeauth.statemachine.errors.SignInContinuationError;
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError;
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpError;
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpSubmitAttributesError;
import com.microsoft.identity.nativeauth.statemachine.errors.SubmitCodeError;
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult;
import com.microsoft.identity.nativeauth.statemachine.results.GetAccountResult;
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResendCodeResult;
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordResult;
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordStartResult;
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordSubmitCodeResult;
import com.microsoft.identity.nativeauth.statemachine.results.ResetPasswordSubmitPasswordResult;
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult;
import com.microsoft.identity.nativeauth.statemachine.results.SignInSubmitCodeResult;
import com.microsoft.identity.nativeauth.statemachine.results.SignOutResult;
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResendCodeResult;
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult;
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitAttributesResult;
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitCodeResult;
import com.microsoft.identity.nativeauth.statemachine.results.SignUpSubmitPasswordResult;
import com.microsoft.identity.nativeauth.statemachine.states.AccountState;
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordCodeRequiredState;
import com.microsoft.identity.nativeauth.statemachine.states.ResetPasswordPasswordRequiredState;
import com.microsoft.identity.nativeauth.statemachine.states.SignInCodeRequiredState;
import com.microsoft.identity.nativeauth.statemachine.states.SignInContinuationState;
import com.microsoft.identity.nativeauth.statemachine.states.SignUpAttributesRequiredState;
import com.microsoft.identity.nativeauth.statemachine.states.SignUpCodeRequiredState;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.controllers.CommandDispatcherHelper;
import com.microsoft.identity.common.nativeauth.MockApiEndpoint;
import com.microsoft.identity.common.nativeauth.MockApiResponseType;
import com.microsoft.identity.common.nativeauth.MockApiUtils;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.nativeauth.statemachine.states.SignUpPasswordRequiredState;
import com.microsoft.identity.nativeauth.utils.LoggerCheckHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import static com.microsoft.identity.nativeauth.utils.MockCorrelationIdHelperKt.mockCorrelationId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

@RunWith(ParameterizedRobolectricTestRunner.class)
@LooperMode(LEGACY)
@Config(shadows = {ShadowAndroidSdkStorageEncryptionManager.class})
public class NativeAuthPublicClientApplicationJavaTest extends PublicClientApplicationAbstractTest {

    private Context context;
    private IPlatformComponents components;
    private Activity activity;
    private INativeAuthPublicClientApplication application;
    private LoggerCheckHelper loggerCheckHelper;
    private final String username = "user@email.com";
    private final String invalidUsername = "invalidUsername";
    private final char[] password = "verySafePassword".toCharArray();
    private final String code = "1234";
    private final String emptyString = "";
    private final boolean allowPII;

    public NativeAuthPublicClientApplicationJavaTest(boolean allowPII) {
        this.allowPII = allowPII;
    }

    @Mock
    private ILoggerCallback externalLogger;


    @Override
    public String getConfigFilePath() {
        return "src/test/res/raw/native_auth_native_only_test_config.json";
    }

    @BeforeClass
    public static void setupClass() {
        setUseMockApiForNativeAuth(true);
    }

    @AfterClass
    public static void tearDownClass() {
        setUseMockApiForNativeAuth(false);
    }

    @ParameterizedRobolectricTestRunner.Parameters
    public static Collection<Boolean> data() {
        return Arrays.asList(true, false);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        context = ApplicationProvider.getApplicationContext();
        components = AndroidPlatformComponentsFactory.createFromContext(context);
        activity = Mockito.mock(Activity.class);
        loggerCheckHelper = new LoggerCheckHelper(externalLogger, allowPII);
        Mockito.when(activity.getApplicationContext()).thenReturn(context);
        setupPCA();
        CommandDispatcherHelper.clear();
    }

    @After
    public void cleanup() {
        loggerCheckHelper.checkSafeLogging();
        AcquireTokenTestHelper.setAccount(null);
        // remove everything from cache after test ends
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);
    }

    private void setupPCA() {
        final File configFile = new File(getConfigFilePath());

        try {
            application = PublicClientApplication.createNativeAuthPublicClientApplication(context, configFile);
        } catch (InterruptedException | MsalException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Test sign in scenario 1:
     * 1a -> sign in initiate with username
     * 1b <- sign in initiate succeeds
     * 2a -> sign in challenge
     * 2b <- challenge required is password
     * 3a -> token with password
     * 3b <- success, with tokens
     */
    @Test
    public void testSignInScenario1() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. Sign in initiate with username
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        // 2a. Sign in challenge
        // 2b. Setup server response with password required
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        // 3a. Token with password
        // 3b. Success, with tokens
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        final ResultFuture<SignInResult> signInWithPasswordResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback signInWithPasswordResultCallback
                = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInWithPasswordResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInWithPasswordResult.setException(exception);
            }
        };

        application.signIn(
                "user@contoso.com",
                "password".toCharArray(),
                null,
                signInWithPasswordResultCallback
        );

        assertTrue(signInWithPasswordResult.get(10, TimeUnit.SECONDS) instanceof SignInResult.Complete);
    }

    /**
     * Test sign in scenario 1:
     * 1a -> sign in initiate with username
     * 1b <- sign in initiate succeeds
     * 2a -> sign in challenge
     * 2b <- challenge required is password
     * 3a -> token with password
     * 3b <- server returns invalid password error
     */
    @Test
    public void testSignInScenario4() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. Sign in initiate with username
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        // 2a. Sign in challenge
        // 2b. Setup server response with password required
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        // 3a. Token with password
        // 3b. Invalid password
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.INVALID_PASSWORD
        );

        // 1b. Call SDK interface
        final ResultFuture<SignInResult> signInResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback callback = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInResult.setException(exception);
            }
        };
        application.signIn(username, password, null, callback);
        // 1a. Server returns invalid password error
        SignInResult result = signInResult.get(30, TimeUnit.SECONDS);
        assertTrue(result instanceof SignInError);

        SignInError error = (SignInError)result;
        assertTrue(error.isInvalidCredentials());
    }

    /**
     * Test sign in scenario 5:
     * 1a -> sign in initiate with username
     * 1b <- server returns invalid user error
     */
    @Test
    public void testSignInScenario5() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. Sign in with username and password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.Companion.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.USER_NOT_FOUND
        );

        // 1b. Call SDK interface
        final ResultFuture<SignInResult> signInResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback callback = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInResult.setException(exception);
            }
        };
        application.signIn(username, password, null, callback);
        // 1a. Server returns invalid user error
        SignInResult result = signInResult.get(30, TimeUnit.SECONDS);
        assertTrue(result instanceof SignInError);

        SignInError error = (SignInError)result;
        assertTrue(error.isUserNotFound());
    }

    /**
     * Test sign in scenario 6:
     * 1a -> sign in with (invalid) username
     * 1b <- server returns invalid user error
     */
    @Test
    public void testSignInScenario6() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. Sign in with username
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.USER_NOT_FOUND
        );

        // 1b. Call SDK interface
        final ResultFuture<SignInResult> signInResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback callback = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInResult.setException(exception);
            }
        };
        application.signIn(username, null, null, callback);
        // 1a. Server returns invalid user error
        SignInResult result = signInResult.get(30, TimeUnit.SECONDS);
        assertTrue(result instanceof SignInError);

        SignInError error = (SignInError)result;
        assertTrue(error.isUserNotFound());
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
    public void testSignInScenario7() throws ExecutionException, InterruptedException, TimeoutException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // 1. Sign in initiate with username
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        // 2a. Sign in challenge
        // 2b. Setup server response with oob required
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        final ResultFuture<SignInResult> signInResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback signInCallback = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInResult.setException(exception);
            }
        };
        application.signIn(username, null, null, signInCallback);
        // 1a. Server returns invalid user error
        assertTrue(signInResult.get(30, TimeUnit.SECONDS) instanceof SignInResult.CodeRequired);
        SignInCodeRequiredState nextState = spy((((SignInResult.CodeRequired) signInResult.get(30, TimeUnit.SECONDS)).getNextState()));

        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(nextState, correlationId);

        // 2. Submit (invalid) code
        // 2a. Setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.INVALID_OOB_VALUE
        );
        // 2b. Call SDK interface
        final ResultFuture<SignInSubmitCodeResult> submitCodeResult = new ResultFuture<>();
        SignInCodeRequiredState.SubmitCodeCallback submitCodeCallback = new SignInCodeRequiredState.SubmitCodeCallback() {
            @Override
            public void onResult(SignInSubmitCodeResult result) {
                submitCodeResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                submitCodeResult.setException(exception);
            }
        };
        nextState.submitCode(code, submitCodeCallback);
        // 2a. Server returns invalid code, stays in CodeRequired state
        SignInSubmitCodeResult result = submitCodeResult.get(30, TimeUnit.SECONDS);
        assertTrue(result instanceof SubmitCodeError);

        SubmitCodeError error = spy((SubmitCodeError)result);
        assertTrue(error.isInvalidCode());

        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(error, correlationId);

        // 3. Submit (valid) code
        // 3a. Setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        // 3b. Call SDK interface
        final ResultFuture<SignInSubmitCodeResult> submitCodeResult2 = new ResultFuture<>();
        SignInCodeRequiredState.SubmitCodeCallback submitCodeCallback2 = new SignInCodeRequiredState.SubmitCodeCallback() {
            @Override
            public void onResult(SignInSubmitCodeResult result) {
                submitCodeResult2.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                submitCodeResult2.setException(exception);
            }
        };
        nextState.submitCode(code, submitCodeCallback2);
        // 3a. Server accepts code, returns tokens
        assertTrue(submitCodeResult2.get(30, TimeUnit.SECONDS) instanceof SignInResult.Complete);
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
    public void testSignInScenario9() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1a. Sign in with username
        // 1b. Setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        // 2a. Sign in challenge
        // 2b. Setup server response with oob required
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 3c. Call SDK interface
        final ResultFuture<SignInResult> signInResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback callback = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInResult.setException(exception);
            }
        };
        application.signIn(username, password, null, callback);
        // 3d. Server returns InvalidAuthMethodForUser error
        assertTrue(signInResult.get(30, TimeUnit.SECONDS) instanceof SignInResult.CodeRequired);
    }

    /** Test sign in scenario 10:
     * 1a -> sign in with username and password
     * 1b <- server returns INVALID_AUTHENTICATION_METHOD error
     * 2a -> sign in initiate is called
     * 2b <- server returns redirect
     * 3a <- Call SDK interface
     * 3b <- BrowserRequired is returned to the user
     */
    @Test
    public void testSignInScenario10() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. Sign in with username and password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.INVALID_AUTHENTICATION_METHOD
        );

        // 2a. Sign in with username
        // 2b. Setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        );

        // 3a. Call SDK interface
        final ResultFuture<SignInResult> signInResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback callback = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInResult.setException(exception);
            }
        };
        application.signIn(username, password, null, callback);
        // 3b. Server returns BrowserRequired error
        SignInResult result = signInResult.get(30, TimeUnit.SECONDS);
        assertTrue(result instanceof SignInError);
    }

    /**
     * Test sign in blocked (when account is already signed in)
     */
    @Test
    public void testSignInBlocked() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );


        final ResultFuture<SignInResult> signInWithPasswordResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback signInWithPasswordResultCallback
                = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInWithPasswordResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInWithPasswordResult.setException(exception);
            }
        };

        application.signIn(
                username,
                password,
                null,
                signInWithPasswordResultCallback
        );

        assertTrue(signInWithPasswordResult.get(10, TimeUnit.SECONDS) instanceof SignInResult.Complete);

        ResultFuture<BaseException> signInExceptionResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback signInWithPasswordResultCallback2
                = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {

            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInExceptionResult.setResult(exception);
            }
        };

        application.signIn(username, password, null, signInWithPasswordResultCallback2);
        BaseException exception = signInExceptionResult.get(10, TimeUnit.SECONDS);
        assertEquals(MsalClientException.INVALID_PARAMETER, exception.getErrorCode());
        assertEquals("An account is already signed in.", exception.getMessage());
    }

    /**
     * Test sign in, sign out and sign in again
     */
    @Test
    public void testSignInSignOutSignIn() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        SignInTestCallback signInTestCallback = new SignInTestCallback();
        application.signIn(
                username,
                password,
                null,
                signInTestCallback
        );
        SignInResult signInWithPasswordResult = signInTestCallback.get();
        assertTrue(signInWithPasswordResult instanceof SignInResult.Complete);

        // Sign out
        SignOutTestCallback signOutTestCallback = new SignOutTestCallback();
        ((SignInResult.Complete) signInWithPasswordResult).getResultValue().signOut(signOutTestCallback);
        SignOutResult signOutResult = signOutTestCallback.get();
        assertNotNull(signOutResult);

        // Sign in again
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        SignInTestCallback signInTestCallback2 = new SignInTestCallback();
        application.signIn(
                username,
                password,
                null,
                signInTestCallback2
        );

        SignInResult signInWithPasswordResult2 = signInTestCallback2.get();
        assertTrue(signInWithPasswordResult2 instanceof SignInResult.Complete);
    }

    /**
     * Test sign in, get access token. Compare to token from getAccount()
     */
    @Test
    public void testGetAccessToken() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        SignInTestCallback signInTestCallback = new SignInTestCallback();
        application.signIn(
                username,
                password,
                null,
                signInTestCallback
        );
        SignInResult signInWithPasswordResult = signInTestCallback.get();
        assertTrue(signInWithPasswordResult instanceof SignInResult.Complete);

        // Get access token from sign in result
        GetAccessTokenTestCallback getAccessTokenCallback = new GetAccessTokenTestCallback();
        ((SignInResult.Complete) signInWithPasswordResult).getResultValue().getAccessToken(false, getAccessTokenCallback);
        GetAccessTokenResult getAccessTokenResult = getAccessTokenCallback.get();
        assertTrue(getAccessTokenResult instanceof GetAccessTokenResult.Complete);

        String accessToken = ((GetAccessTokenResult.Complete) getAccessTokenResult).getResultValue().getAccessToken();
        assertNotNull(accessToken);

        // For comparison, get access token from getAccount()
        GetAccountTestCallback getAccountTestCallback = new GetAccountTestCallback();
        application.getCurrentAccount(getAccountTestCallback);

        GetAccountResult getAccountResult = getAccountTestCallback.get();
        assertTrue(getAccountResult instanceof GetAccountResult.AccountFound);
        ((GetAccountResult.AccountFound) getAccountResult).getResultValue().getAccessToken(false, getAccessTokenCallback);
        GetAccessTokenResult getAccessTokenResultTwo = getAccessTokenCallback.get();
        assertTrue(getAccessTokenResultTwo instanceof GetAccessTokenResult.Complete);

        String accessTokenTwo = ((GetAccessTokenResult.Complete) getAccessTokenResult).getResultValue().getAccessToken();
        assertNotNull(accessTokenTwo);

        assertEquals(accessToken, accessTokenTwo);
    }

    /**
     * Test sign in, sign out, get access token
     */
    @Test
    public void testSignOutGetAccessToken() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        // sign in
        SignInTestCallback signInTestCallback = new SignInTestCallback();
        application.signIn(
                username,
                password,
                null,
                signInTestCallback
        );
        SignInResult signInWithPasswordResult = signInTestCallback.get();
        assertTrue(signInWithPasswordResult instanceof SignInResult.Complete);

        // Sign out
        AccountState accountState = ((SignInResult.Complete) signInWithPasswordResult).getResultValue();
        SignOutTestCallback signOutTestCallback = new SignOutTestCallback();
        accountState.signOut(signOutTestCallback);
        SignOutResult signOutResult = signOutTestCallback.get();
        assertNotNull(signOutResult);

        // Attempt to get token
        GetAccessTokenTestCallback getAccessTokenTestCallback = new GetAccessTokenTestCallback();
        accountState.getAccessToken(false, getAccessTokenTestCallback);
        GetAccessTokenResult getAccessTokenResult = getAccessTokenTestCallback.get();
        assertTrue(getAccessTokenResult instanceof GetAccessTokenError);
        assertTrue(((GetAccessTokenError) getAccessTokenResult).isNoAccountFound());
    }



    /**
     * Test sign up with password blocked (when account is already signed in)
     */
    @Test
    public void testSignUpPasswordBlocked() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        final ResultFuture<SignInResult> signInWithPasswordResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback signInWithPasswordResultCallback
                = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInWithPasswordResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInWithPasswordResult.setException(exception);
            }
        };

        application.signIn(
                username,
                password,
                null,
                signInWithPasswordResultCallback
        );

        assertTrue(signInWithPasswordResult.get(10, TimeUnit.SECONDS) instanceof SignInResult.Complete);

        ResultFuture<BaseException> signUpExceptionResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignUpCallback signUpWithPasswordResultCallback
                = new NativeAuthPublicClientApplication.SignUpCallback() {

            @Override
            public void onResult(SignUpResult result) {

            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signUpExceptionResult.setResult(exception);
            }
        };
        application.signUp(username, password, null, signUpWithPasswordResultCallback);
        BaseException exception = signUpExceptionResult.get(10, TimeUnit.SECONDS);

        assertEquals(MsalClientException.INVALID_PARAMETER, exception.getErrorCode());
        assertEquals("An account is already signed in.", exception.getMessage());
    }

    /**
     * Test sign up blocked (when account is already signed in)
     */
    @Test
    public void testSignUpBlocked() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        final ResultFuture<SignInResult> signInWithPasswordResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback signInWithPasswordResultCallback
                = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInWithPasswordResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInWithPasswordResult.setException(exception);
            }
        };

        application.signIn(
                username,
                password,
                null,
                signInWithPasswordResultCallback
        );

        assertTrue(signInWithPasswordResult.get(10, TimeUnit.SECONDS) instanceof SignInResult.Complete);
        ResultFuture<BaseException> signUpExceptionResult = new ResultFuture<>();

        NativeAuthPublicClientApplication.SignUpCallback signUpWithCodeResultCallback
                = new NativeAuthPublicClientApplication.SignUpCallback() {

            @Override
            public void onResult(SignUpResult result) {

            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signUpExceptionResult.setResult(exception);
            }
        };
        application.signUp(username, null, null, signUpWithCodeResultCallback);

        BaseException exception = signUpExceptionResult.get(10, TimeUnit.SECONDS);
        assertEquals(MsalClientException.INVALID_PARAMETER, exception.getErrorCode());
        assertEquals("An account is already signed in.", exception.getMessage());
    }

    /**
     * Test reset password blocked (when account is already signed in)
     */
    @Test
    public void testResetPasswordBlocked() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("testResetPasswordBlocked");

        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.INITIATE_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        SignInTestCallback signInWithPasswordResultTestCallback = new SignInTestCallback();

        application.signIn(
                username,
                password,
                null,
                signInWithPasswordResultTestCallback
        );
        SignInResult signInWithPasswordResult = signInWithPasswordResultTestCallback.get();

        assertTrue(signInWithPasswordResult instanceof SignInResult.Complete);

        ResultFuture<BaseException> resetPasswordExceptionResult = new ResultFuture<>();

        NativeAuthPublicClientApplication.ResetPasswordCallback resetPasswordCallback
                = new NativeAuthPublicClientApplication.ResetPasswordCallback() {
            @Override
            public void onResult(ResetPasswordStartResult result) {
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                resetPasswordExceptionResult.setResult(exception);

            }
        };
        application.resetPassword(username, resetPasswordCallback);

        BaseException exception = resetPasswordExceptionResult.get(10, TimeUnit.SECONDS);
        assertEquals(MsalClientException.INVALID_PARAMETER, exception.getErrorCode());
        assertEquals("An account is already signed in.", exception.getMessage());
    }

    /**
     * Test sign in with SLT scenario 1:
     * 1a -> sign in with (valid) SLT
     * 1b <- server returns token
     */
    @Test
    public void testSignInWithSLT() throws ExecutionException, InterruptedException, TimeoutException {
        System.out.println("testSignInWithSLT");

        // Setup - sign up the user, so that we don't have to construct the SLT state manually
        // as this doesn't allow for the NativeAuthPublicClientApplicationConfiguration to be set
        // up, meaning it would need to be mocked (which we don't want in these tests).
        SignInContinuationState signInWithSLTState = signUpUser();

        // 1a. sign in with (valid) SLT
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        // 1b. server returns token
        final ResultFuture<SignInResult> resultFuture = new ResultFuture<>();
        SignInContinuationState.SignInContinuationCallback callback = new SignInContinuationState.SignInContinuationCallback() {
            @Override
            public void onResult(SignInResult result) {
                resultFuture.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                resultFuture.setException(exception);
            }
        };
        signInWithSLTState.signIn(null, callback);
        assertTrue(resultFuture.get(30, TimeUnit.SECONDS) instanceof SignInResult.Complete);
    }

    /**
     * Test sign in with SLT scenario 2:
     * 1a -> sign in with (null) SLT
     * 1b <- client returns error right away
     */
    @Test
    public void testSignInWithSLTNullSLT() throws ExecutionException, InterruptedException, TimeoutException {
        // 1a. sign in with (null) SLT
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        // 1b. client returns error
        final NativeAuthPublicClientApplicationConfiguration config = Mockito.mock(NativeAuthPublicClientApplicationConfiguration.class);
        final SignInContinuationState state = new SignInContinuationState(
                null,
                correlationId,
                username,
                config
        );
        final ResultFuture<SignInResult> resultFuture = new ResultFuture<>();
        SignInContinuationState.SignInContinuationCallback callback = new SignInContinuationState.SignInContinuationCallback() {
            @Override
            public void onResult(SignInResult result) {
                resultFuture.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                resultFuture.setException(exception);
            }
        };
        state.signIn(null, callback);

        SignInResult result = resultFuture.get(10, TimeUnit.SECONDS);
        assertTrue(result instanceof SignInContinuationError);
    }

    /**
     * Test sign in with SLT scenario 3:
     * 1a -> sign in with (expired) SLT
     * 1b <- server returns error
     */
    @Ignore("Waiting for SLT Mock API Integration (Out of scope?)")
    @Test
    public void testSignInWithExpiredSLT() throws ExecutionException, InterruptedException, TimeoutException {
        // Setup - sign up the user, so that we don't have to construct the SLT state manually
        // as this doesn't allow for the NativeAuthPublicClientApplicationConfiguration to be set
        // up, meaning it would need to be mocked (which we don't want in these tests).
        SignInContinuationState signInWithSLTState = signUpUser();

        // 1a. sign in with (expired) SLT
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_EXPIRED_SLT
        );

        // 1b. server returns error
        final ResultFuture<SignInResult> resultFuture = new ResultFuture<>();
        SignInContinuationState.SignInContinuationCallback callback = new SignInContinuationState.SignInContinuationCallback() {
            @Override
            public void onResult(SignInResult result) {
                resultFuture.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                resultFuture.setException(exception);
            }
        };
        signInWithSLTState.signIn(null, callback);

        SignInResult result = resultFuture.get(10, TimeUnit.SECONDS);
        assertTrue(result instanceof SignInError);

        SignInError error = (SignInError)result;

        assertFalse(error.isBrowserRequired());
        assertFalse(error.isUserNotFound());
    }

    /**
     * Check that we don't get a type casting exception thrown when we get it,
     * should get error result instead.
     */
    @Test
    public void testSignInEmptyUsernameNoException() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInInitiate,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        );


        final ResultFuture<SignInResult> signInWithPasswordResult = new ResultFuture<>();
        NativeAuthPublicClientApplication.SignInCallback signInWithPasswordResultCallback
                = new NativeAuthPublicClientApplication.SignInCallback() {
            @Override
            public void onResult(SignInResult result) {
                signInWithPasswordResult.setResult(result);
            }

            @Override
            public void onError(@NonNull BaseException exception) {
                signInWithPasswordResult.setException(exception);
            }
        };

        application.signIn(
                emptyString,
                "password".toCharArray(),
                null,
                signInWithPasswordResultCallback
        );

        SignInResult result = signInWithPasswordResult.get(10, TimeUnit.SECONDS);
        assertTrue(result instanceof SignInError);

        SignInError error = (SignInError)result;

        assertTrue(error.isInvalidUsername());
        assertFalse(error.isBrowserRequired());
        assertFalse(error.isUserNotFound());
        assertFalse(error.isInvalidCredentials());
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
    public void testSSPRScenario3_2_1() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.SSPR_START_SUCCESS
        );
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        ResetPasswordStartTestCallback resetPasswordCallback = new ResetPasswordStartTestCallback();
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        application.resetPassword(username, resetPasswordCallback);
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        ResetPasswordStartResult resetPasswordResult = resetPasswordCallback.get();
        assertTrue(resetPasswordResult instanceof ResetPasswordStartResult.CodeRequired);
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        ResetPasswordCodeRequiredState nextState = ((ResetPasswordStartResult.CodeRequired) resetPasswordResult).getNextState();

        // 2. Submit valid code
        // 2_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRContinue,
                correlationId,
                MockApiResponseType.SSPR_CONTINUE_SUCCESS
        );

        ResetPasswordSubmitCodeTestCallback submitCodeCallback = new ResetPasswordSubmitCodeTestCallback();
        // 2a. Call SDK interface - submitCode()
        nextState.submitCode(code, submitCodeCallback);
        // 2b. Transform /continue(success) to Result(PasswordRequired).
        ResetPasswordSubmitCodeResult submitCodeResult = submitCodeCallback.get();
        assertTrue(submitCodeResult instanceof ResetPasswordSubmitCodeResult.PasswordRequired);
        // 2c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordPasswordRequired state.
        ResetPasswordPasswordRequiredState nextState_ = ((ResetPasswordSubmitCodeResult.PasswordRequired) submitCodeResult).getNextState();

        // 3. Submit valid password
        // 3_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRSubmit,
                correlationId,
                MockApiResponseType.SSPR_SUBMIT_SUCCESS
        );
        // 3_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRPoll,
                correlationId,
                MockApiResponseType.SSPR_POLL_SUCCESS
        );

        ResetPasswordSubmitPasswordTestCallback submitPasswordCallback = new ResetPasswordSubmitPasswordTestCallback();
        // 3a. Call SDK interface - submitPassword()
        nextState_.submitPassword(password, submitPasswordCallback);
        // 3b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        ResetPasswordSubmitPasswordResult submitPasswordResult = submitPasswordCallback.get();
        assertTrue(submitPasswordResult instanceof ResetPasswordResult.Complete);
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
    public void testSSPRScenario3_2_2() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.SSPR_START_SUCCESS
        );
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        ResetPasswordStartTestCallback resetPasswordCallback = new ResetPasswordStartTestCallback();
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        application.resetPassword(username, resetPasswordCallback);
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        ResetPasswordStartResult resetPasswordResult = resetPasswordCallback.get();
        assertTrue(resetPasswordResult instanceof ResetPasswordStartResult.CodeRequired);
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        ResetPasswordCodeRequiredState nextState = ((ResetPasswordStartResult.CodeRequired) resetPasswordResult).getNextState();

        // 2. Submit valid code
        // 2_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRContinue,
                correlationId,
                MockApiResponseType.SSPR_CONTINUE_SUCCESS
        );

        ResetPasswordSubmitCodeTestCallback submitCodeCallback = new ResetPasswordSubmitCodeTestCallback();
        // 2a. Call SDK interface - submitCode()
        nextState.submitCode(code, submitCodeCallback);
        // 2b. Transform /continue(success) to Result(PasswordRequired).
        ResetPasswordSubmitCodeResult submitCodeResult = submitCodeCallback.get();
        assertTrue(submitCodeResult instanceof ResetPasswordSubmitCodeResult.PasswordRequired);
        // 2c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordPasswordRequired state.
        ResetPasswordPasswordRequiredState nextState_ = ((ResetPasswordSubmitCodeResult.PasswordRequired) submitCodeResult).getNextState();

        // 3. Submit valid password
        // 3_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRSubmit,
                correlationId,
                MockApiResponseType.SSPR_SUBMIT_SUCCESS
        );
        // 3_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRPoll,
                correlationId,
                MockApiResponseType.SSPR_POLL_SUCCESS
        );

        ResetPasswordSubmitPasswordTestCallback submitPasswordCallback = new ResetPasswordSubmitPasswordTestCallback();
        // 3a. Call SDK interface - submitPassword()
        nextState_.submitPassword(password, submitPasswordCallback);
        // 3b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        ResetPasswordSubmitPasswordResult submitPasswordResult = submitPasswordCallback.get();
        assertTrue(submitPasswordResult instanceof ResetPasswordResult.Complete);
        // 3c. Respond to Result(Complete): shifting from ResetPasswordPasswordRequired to end
        SignInContinuationState signInState = ((ResetPasswordResult.Complete) submitPasswordResult).getNextState();

        // 4a. Sign in with (valid) continuation token
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignInToken,
                correlationId,
                MockApiResponseType.TOKEN_SUCCESS
        );

        SignInContinuationTestCallback signInCallback = new SignInContinuationTestCallback();
        signInState.signIn(null, signInCallback);

        SignInResult signInResult = signInCallback.get();
        assertTrue(signInResult instanceof SignInResult.Complete);
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
    @Test
    public void testSSPRScenario3_2_3() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.SSPR_START_SUCCESS
        );
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        ResetPasswordStartTestCallback resetPasswordCallback = new ResetPasswordStartTestCallback();
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        application.resetPassword(username, resetPasswordCallback);
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        ResetPasswordStartResult resetPasswordResult = resetPasswordCallback.get();
        assertTrue(resetPasswordResult instanceof ResetPasswordStartResult.CodeRequired);
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        ResetPasswordCodeRequiredState ResetPasswordNextState = spy(((ResetPasswordStartResult.CodeRequired) resetPasswordResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(ResetPasswordNextState, correlationId);

        // 2. Submit valid code
        // 2_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRContinue,
                correlationId,
                MockApiResponseType.SSPR_CONTINUE_SUCCESS
        );

        ResetPasswordSubmitCodeTestCallback submitCodeCallback = new ResetPasswordSubmitCodeTestCallback();
        // 2a. Call SDK interface - submitCode()
        ResetPasswordNextState.submitCode(code, submitCodeCallback);
        // 2b. Transform /continue(success) to Result(PasswordRequired).
        ResetPasswordSubmitCodeResult submitCodeResult = submitCodeCallback.get();
        assertTrue(submitCodeResult instanceof ResetPasswordSubmitCodeResult.PasswordRequired);
        // 2c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordPasswordRequired state.
        ResetPasswordPasswordRequiredState submitCodeNextState = spy(((ResetPasswordSubmitCodeResult.PasswordRequired) submitCodeResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(submitCodeNextState, correlationId);

        // 3. Submit invalid password
        // 3_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRSubmit,
                correlationId,
                MockApiResponseType.PASSWORD_TOO_WEAK
        );

        ResetPasswordSubmitPasswordTestCallback submitPasswordCallback1 = new ResetPasswordSubmitPasswordTestCallback();
        // 3a. Call SDK interface - submitPassword()
        submitCodeNextState.submitPassword(password, submitPasswordCallback1);
        // 3b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        ResetPasswordSubmitPasswordResult submitPasswordResult1 = submitPasswordCallback1.get();

        assertTrue(submitPasswordResult1 instanceof ResetPasswordSubmitPasswordError);
        assertTrue(((ResetPasswordSubmitPasswordError) submitPasswordResult1).isInvalidPassword());

        // 4. Submit valid password
        // 4_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRSubmit,
                correlationId,
                MockApiResponseType.SSPR_SUBMIT_SUCCESS
        );
        // 4_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRPoll,
                correlationId,
                MockApiResponseType.SSPR_POLL_SUCCESS
        );

        ResetPasswordSubmitPasswordTestCallback submitPasswordCallback2 = new ResetPasswordSubmitPasswordTestCallback();
        // 4a. Call SDK interface - submitPassword()
        submitCodeNextState.submitPassword(password, submitPasswordCallback2);
        // 4b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        ResetPasswordSubmitPasswordResult submitPasswordResult2 = submitPasswordCallback2.get();
        assertTrue(submitPasswordResult2 instanceof ResetPasswordResult.Complete);
        // 4c. Respond to Result(Complete): shifting from ResetPasswordPasswordRequired to end. SLT as resultValue will be returned after private preview.
        Object resultValue = ((ResetPasswordResult.Complete) submitPasswordResult2).getResultValue();
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
    public void testSSPRScenario3_2_4() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.SSPR_START_SUCCESS
        );
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        ResetPasswordStartTestCallback resetPasswordCallback = new ResetPasswordStartTestCallback();
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        application.resetPassword(username, resetPasswordCallback);
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        ResetPasswordStartResult resetPasswordResult = resetPasswordCallback.get();
        assertTrue(resetPasswordResult instanceof ResetPasswordStartResult.CodeRequired);
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        ResetPasswordCodeRequiredState nextState1 = ((ResetPasswordStartResult.CodeRequired) resetPasswordResult).getNextState();

        // 2. Click resend code
        // 2_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        ResetPasswordResendCodeTestCallback resendCodeCallback = new ResetPasswordResendCodeTestCallback();
        // 2a. Call SDK interface - resendCode()
        nextState1.resendCode(resendCodeCallback);
        // 2b. Transform /challenge(challenge_type=OOB) to Result(CodeRequired).
        ResetPasswordResendCodeResult resendCodeResult = resendCodeCallback.get();
        assertTrue(resendCodeResult instanceof ResetPasswordResendCodeResult.Success);

        // 3. Submit valid code
        // 3_mock_api. Setup server response - endpoint: resetpassword/continue - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRContinue,
                correlationId,
                MockApiResponseType.SSPR_CONTINUE_SUCCESS
        );

        ResetPasswordSubmitCodeTestCallback submitCodeCallback = new ResetPasswordSubmitCodeTestCallback();
        // 3a. Call SDK interface - submitCode()
        nextState1.submitCode(code, submitCodeCallback);
        // 3b. Transform /continue(success) to Result(PasswordRequired).
        ResetPasswordSubmitCodeResult submitCodeResult = submitCodeCallback.get();
        assertTrue(submitCodeResult instanceof ResetPasswordSubmitCodeResult.PasswordRequired);
        // 3c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordPasswordRequired state.
        ResetPasswordPasswordRequiredState nextState2 = ((ResetPasswordSubmitCodeResult.PasswordRequired) submitCodeResult).getNextState();

        // 4. Submit valid password
        // 4_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRSubmit,
                correlationId,
                MockApiResponseType.SSPR_SUBMIT_SUCCESS
        );
        // 4_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRPoll,
                correlationId,
                MockApiResponseType.SSPR_POLL_SUCCESS
        );

        ResetPasswordSubmitPasswordTestCallback submitPasswordCallback = new ResetPasswordSubmitPasswordTestCallback();
        // 4a. Call SDK interface - submitPassword()
        nextState2.submitPassword(password, submitPasswordCallback);
        // 4b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        ResetPasswordSubmitPasswordResult submitPasswordResult = submitPasswordCallback.get();
        assertTrue(submitPasswordResult instanceof ResetPasswordResult.Complete);
        // 4c. Respond to Result(Complete): shifting from ResetPasswordPasswordRequired to end. SLT as resultValue will be returned after private preview.
        Object resultValue = ((ResetPasswordResult.Complete) submitPasswordResult).getResultValue();
    }

    /**
     * Test SSPR scenario 3.2.5:
     * 1 -> USER click resetPassword
     * 1 <- user not found, SERVER returns error
     */
    @Test
    public void testSSPRScenario3_2_5() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Error: user not found
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.USER_NOT_FOUND
        );

        ResetPasswordStartTestCallback resetPasswordCallback = new ResetPasswordStartTestCallback();
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        application.resetPassword(username, resetPasswordCallback);
        // 1b. Transform /start(error) to Result(UserNotFound)
        ResetPasswordStartResult resetPasswordResult = resetPasswordCallback.get();
        assertTrue(resetPasswordResult instanceof ResetPasswordError);
        assertTrue(((ResetPasswordError) resetPasswordResult).isUserNotFound());
    }

    /**
     * Test SSPR scenario 3.2.6:
     * 1 -> USER click resetPassword
     * 1 <- challenge type do no support, SERVER requires error: redirect
     */
    @Test
    public void testSSPRScenario3_2_6() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Error: user not found
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        );

        ResetPasswordStartTestCallback resetPasswordCallback = new ResetPasswordStartTestCallback();
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        application.resetPassword(username, resetPasswordCallback);
        // 1b. Transform /start(error) to Result(UserNotFound)
        ResetPasswordStartResult resetPasswordResult = resetPasswordCallback.get();
        assertTrue(resetPasswordResult instanceof ResetPasswordError);
        assertTrue(((ResetPasswordError) resetPasswordResult).isBrowserRequired());
    }

    /**
     * 1 -> USER click resetPassword
     * 1 <- challenge type do no support, SERVER returns error: invalid request
     */
    @Test
    public void testSSPRScenarioUnknownError() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Error: invalid request
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.UNSUPPORTED_CHALLENGE_TYPE
        );

        ResetPasswordStartTestCallback resetPasswordCallback = new ResetPasswordStartTestCallback();
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        application.resetPassword(username, resetPasswordCallback);
        // 1b. Transform /start(error) to Result(UserNotFound)
        ResetPasswordStartResult resetPasswordResult = resetPasswordCallback.get();

        assertTrue(resetPasswordResult instanceof ResetPasswordError);
        assertFalse(((ResetPasswordError) resetPasswordResult).isBrowserRequired());
        assertFalse(((ResetPasswordError) resetPasswordResult).isUserNotFound());
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
    public void testSSPRScenario3_2_10() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        // 1. Click reset password
        // 1_mock_api. Setup server response - endpoint: resetpassword/start - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.SSPR_START_SUCCESS
        );
        // 1_mock_api. Setup server response - endpoint: resetpassword/challenge - Server returns Success: challenge_type = OOB
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        ResetPasswordStartTestCallback resetPasswordCallback = new ResetPasswordStartTestCallback();
        // 1a. Call SDK interface - resetPassword(ResetPasswordStart)
        application.resetPassword(username, resetPasswordCallback);
        // 1b. Transform /start(success) +/challenge(challenge_type=OOB) to Result(CodeRequired).
        ResetPasswordStartResult resetPasswordResult = resetPasswordCallback.get();
        assertTrue(resetPasswordResult instanceof ResetPasswordStartResult.CodeRequired);
        // 1c. Respond to Result(Code Required): shifting from start to ResetPasswordCodeRequired state.
        ResetPasswordCodeRequiredState nextState = spy(((ResetPasswordStartResult.CodeRequired) resetPasswordResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(nextState, correlationId);

        // 2. Submit invalid code
        // 2_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Error: explicit invalid oob value
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRContinue,
                correlationId,
                MockApiResponseType.INVALID_OOB_VALUE
        );

        ResetPasswordSubmitCodeTestCallback submitCodeCallback1 = new ResetPasswordSubmitCodeTestCallback();
        // 2a. Call SDK interface - submitCode()
        nextState.submitCode(code, submitCodeCallback1);
        // 2b. Transform /continue(success) to Result(PasswordRequired).
        ResetPasswordSubmitCodeResult submitCodeResult = submitCodeCallback1.get();
        assertTrue(submitCodeResult instanceof SubmitCodeError);
        assertTrue(((SubmitCodeError) submitCodeResult).isInvalidCode());

        // 3. Submit valid code
        // 3_mock_api. Setup server response - endpoint: resetpassowrd/continue - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRContinue,
                correlationId,
                MockApiResponseType.SSPR_CONTINUE_SUCCESS
        );

        ResetPasswordSubmitCodeTestCallback submitCodeCallback2 = new ResetPasswordSubmitCodeTestCallback();
        // 3a. Call SDK interface - submitCode()
        nextState.submitCode(code, submitCodeCallback2);
        // 3b. Transform /continue(success) to Result(PasswordRequired).
        ResetPasswordSubmitCodeResult submitCodeResult2 = submitCodeCallback2.get();
        assertTrue(submitCodeResult2 instanceof ResetPasswordSubmitCodeResult.PasswordRequired);
        // 3c. Respond to Result(PasswordRequired): shifting from ResetPasswordCodeRequired to ResetPasswordPasswordRequired state.
        ResetPasswordPasswordRequiredState nextState_ = spy(((ResetPasswordSubmitCodeResult.PasswordRequired) submitCodeResult2).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(nextState_, correlationId);

        // 4. Submit valid password
        // 4_mock_api. Setup server response - endpoint: resetpassword/submit - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRSubmit,
                correlationId,
                MockApiResponseType.SSPR_SUBMIT_SUCCESS
        );
        // 4_mock_api. Setup server response - endpoint: resetpassword/poll_completion - Server returns Success
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRPoll,
                correlationId,
                MockApiResponseType.SSPR_POLL_SUCCESS
        );

        ResetPasswordSubmitPasswordTestCallback submitPasswordCallback = new ResetPasswordSubmitPasswordTestCallback();
        // 4a. Call SDK interface - submitPassword()
        nextState_.submitPassword(password, submitPasswordCallback);
        // 4b. Transform /submit(success) +/poll_completion(success) to Result(Complete).
        ResetPasswordSubmitPasswordResult submitPasswordResult = submitPasswordCallback.get();
        assertTrue(submitPasswordResult instanceof ResetPasswordResult.Complete);
        // 4c. Respond to Result(Complete): shifting from ResetPasswordPasswordRequired to end. SLT as resultValue will be returned after private preview.
        Object resultValue = ((ResetPasswordResult.Complete) submitPasswordResult).getResultValue();
    }

    /**
     * Check that we don't get a type casting exception thrown when we get it,
     * should get error result instead.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Test
    public void testSSPREmptyUsernameNoException() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SSPRStart,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        );

        ResetPasswordStartTestCallback resetPasswordStartTestCallback = new ResetPasswordStartTestCallback();

        application.resetPassword(emptyString, resetPasswordStartTestCallback);
        ResetPasswordStartResult resetPasswordResult = resetPasswordStartTestCallback.get();

        assertTrue(resetPasswordResult instanceof ResetPasswordError);
        assertTrue(((ResetPasswordError) resetPasswordResult).isInvalidUsername());
    }

    // Helper methods
    // TODO update this after sign up SDK tests PR
    private SignInContinuationState signUpUser() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up with password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, password, null, signUpTestCallback);

        SignUpResult signUpResult = signUpTestCallback.get();
        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2. submit (valid) code
        // 2a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpCodeRequiredState submitCodeState = (((SignUpResult.CodeRequired) signUpResult).getNextState());

        SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        submitCodeState.submitCode(code, codeRequiredCallback);

        SignUpSubmitCodeResult signUpCodeRequiredResult = codeRequiredCallback.get();
        assertTrue(signUpCodeRequiredResult instanceof SignUpResult.Complete);

        return ((SignUpResult.Complete) signUpCodeRequiredResult).getNextState();
    }

    /**
     * Test Sign Up scenario 1:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 2b <- sign up succeeds
     */
    @Test
    public void testSignUpScenario1() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up with password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, password, null, signUpTestCallback);

        SignUpResult signInResult = signUpTestCallback.get();

        assertTrue(signInResult instanceof SignUpResult.CodeRequired);

        // 2. submit (valid) code
        // 2a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpCodeRequiredState submitCodeState = (((SignUpResult.CodeRequired) signInResult).getNextState());

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        submitCodeState.submitCode(code, codeRequiredCallback);

        assertTrue(codeRequiredCallback.get() instanceof SignUpResult.Complete);
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
    public void testSignUpScenario2() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up with password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, password, null, signUpTestCallback);

        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2a. Setup resend code challenge
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        SignUpCodeRequiredState codeRequiredState = spy(((SignUpResult.CodeRequired) signUpResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(codeRequiredState, correlationId);

        // 2b. Call resendCode
        SignUpResendCodeRequiredTestCallback resendCodeCallback = new SignUpResendCodeRequiredTestCallback();
        codeRequiredState.resendCode(resendCodeCallback);

        SignUpResendCodeResult resendCodeResult = resendCodeCallback.get();

        assertTrue(resendCodeResult instanceof SignUpResendCodeResult.Success);

        // 3. submit (valid) code
        // 3a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpCodeRequiredState submitCodeState = spy(((SignUpResendCodeResult.Success) resendCodeResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(submitCodeState, correlationId);

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        submitCodeState.submitCode(code, codeRequiredCallback);

        // 3b. Server accepts code, returns tokens
        assertTrue(codeRequiredCallback.get() instanceof SignUpResult.Complete);
    }

    /**
     * Test Sign Up scenario 3:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 3a <- sign up token has expired, server returns token expired error
     */
    @Test
    public void testSignUpScenario3() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, password, null, signUpTestCallback);

        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2. submit (valid) code
        // 2a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.EXPIRED_TOKEN
        );

        SignUpCodeRequiredState codeRequiredState = spy(((SignUpResult.CodeRequired) signUpResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(codeRequiredState, correlationId);

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();

        codeRequiredState.submitCode(code, codeRequiredCallback);

        assertTrue(codeRequiredCallback.get() instanceof SubmitCodeError);
        assertFalse(((SubmitCodeError) codeRequiredCallback.get()).isInvalidCode());
        assertFalse(((SubmitCodeError) codeRequiredCallback.get()).isBrowserRequired());
    }

    /**
     * Test Sign Up scenario 4:
     * 1a -> signUp
     * 1b <- server does not support password authentication
     */
    @Test
    public void testSignUpScenario4() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, password, null, signUpTestCallback);

        assertTrue(signUpTestCallback.get() instanceof SignUpError);
        assertTrue(((SignUpError) signUpTestCallback.get()).isBrowserRequired());
    }

    /**
     * Test Sign Up scenario 5:
     * 1a -> signUp
     * 1b <- server does not support password authentication, returns error to use OOB instead
     */
    @Test
    public void testSignUpScenario5() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.AUTH_NOT_SUPPORTED
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, password, null, signUpTestCallback);

        assertTrue(signUpTestCallback.get() instanceof SignUpError);
        assertTrue(((SignUpError) signUpTestCallback.get()).isAuthNotSupported());
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
    public void testSignUpScenario6() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up with password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(username, password, null, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        SignUpCodeRequiredState codeRequiredState = spy(((SignUpResult.CodeRequired) signUpResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(codeRequiredState, correlationId);

        // 3. submit (valid) code
        // 3a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.ATTRIBUTES_REQUIRED
        );

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        codeRequiredState.submitCode(code, codeRequiredCallback);

        SignUpSubmitCodeResult submitCodeResult = codeRequiredCallback.get();

        assertTrue(submitCodeResult instanceof SignUpResult.AttributesRequired);

        //4. Submit invalid attributes
        //4a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.VALIDATION_FAILED
        );

        SignUpAttributesRequiredState attributesRequiredState = spy(((SignUpResult.AttributesRequired) submitCodeResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(attributesRequiredState, correlationId);

        UserAttributes invalidAttributes = UserAttributes.Builder.customAttribute("attribute", "invalid_attribute").build();
        SignUpSubmitUserAttributesTestCallback failedUserAttributesCallback = new SignUpSubmitUserAttributesTestCallback();

        attributesRequiredState.submitAttributes(invalidAttributes, failedUserAttributesCallback);
        SignUpSubmitAttributesResult attributesFailedResult = failedUserAttributesCallback.get();

        assertTrue(attributesFailedResult instanceof SignUpSubmitAttributesError);
        assertTrue(((SignUpSubmitAttributesError) attributesFailedResult).isInvalidAttributes());

        //4. Submit invalid attributes
        //4a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        UserAttributes validAttributes = UserAttributes.Builder.customAttribute("attribute", "valid_attribute").build();
        SignUpSubmitUserAttributesTestCallback validUserAttributesCallback = new SignUpSubmitUserAttributesTestCallback();

        attributesRequiredState.submitAttributes(validAttributes, validUserAttributesCallback);
        SignUpSubmitAttributesResult attributesSucceededResult = validUserAttributesCallback.get();

        // 4b. Server accepts password, returns tokens
        assertTrue(attributesSucceededResult instanceof SignUpResult.Complete);
    }

    /**
     * Test Sign Up scenario 7:
     * 1a -> signUp with invalid custom attributes
     * 1b <- server returns invalid attribute error
     * 2a -> call signUp with correct attributes
     * 2b <- server returns code required, flow continues
     */
    @Test
    public void testSignUpScenario7() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.VALIDATION_FAILED
        );

        // 1b. Call SDK interface
        UserAttributes invalidAttributes = UserAttributes.Builder.customAttribute("attribute", "valid_attribute").build();
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(username, password, invalidAttributes, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpError);
        assertTrue(((SignUpError) signUpResult).isInvalidAttributes());

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 2b. Call SDK interface again
        UserAttributes validAttributes = UserAttributes.Builder.customAttribute("attribute", "valid_attribute").build();

        SignUpTestCallback signUpSuccessCallback = new SignUpTestCallback();

        application.signUp(username, password, validAttributes, signUpSuccessCallback);
        SignUpResult signUpSuccessResult = signUpSuccessCallback.get();

        assertTrue(signUpSuccessResult instanceof SignUpResult.CodeRequired);
    }

    /**
     * Test Sign Up scenario 8:
     * 1a -> signUp
     * 1b <- server requires code verification
     * 2a -> submit valid code
     * 2b <- sign up succeeds
     */
    @Test
    public void testSignUpScenario8() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up with password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(username, null, null, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2. submit (valid) code
        // 2a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpCodeRequiredState codeRequiredState = ((SignUpResult.CodeRequired) signUpResult).getNextState();

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        codeRequiredState.submitCode(code, codeRequiredCallback);

        SignUpSubmitCodeResult submitCodeResult = codeRequiredCallback.get();

        // 2b. Server accepts code, returns tokens
        assertTrue(submitCodeResult instanceof SignUpResult.Complete);
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
    public void testSignUpScenario9() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up with password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(username, null, null, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2a. Setup resend code challenge
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        SignUpCodeRequiredState codeRequiredState = spy(((SignUpResult.CodeRequired) signUpResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(codeRequiredState, correlationId);

        // 2b. Call resendCode
        SignUpResendCodeRequiredTestCallback resendCodeCallback = new SignUpResendCodeRequiredTestCallback();
        codeRequiredState.resendCode(resendCodeCallback);

        SignUpResendCodeResult resendCodeResult = resendCodeCallback.get();

        assertTrue(resendCodeResult instanceof SignUpResendCodeResult);

        // 3. submit (valid) code
        // 3a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpCodeRequiredState submitCodeState = ((SignUpResendCodeResult.Success) resendCodeResult).getNextState();

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        submitCodeState.submitCode(code, codeRequiredCallback);

        SignUpSubmitCodeResult submitCodeResult = codeRequiredCallback.get();

        // 2b. Server accepts code, returns tokens
        assertTrue(submitCodeResult instanceof SignUpResult.Complete);
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
    public void testSignUpScenario10() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, null, null, signUpTestCallback);

        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2. submit (valid) code
        // 2a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.CREDENTIAL_REQUIRED
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        SignUpCodeRequiredState codeRequiredState = spy(((SignUpResult.CodeRequired) signUpResult).getNextState());

        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(codeRequiredState, correlationId);

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        codeRequiredState.submitCode(code, codeRequiredCallback);

        SignUpSubmitCodeResult passwordRequiredResult = codeRequiredCallback.get();

        assertTrue(passwordRequiredResult instanceof SignUpResult.PasswordRequired);

        //3. Submit valid password
        //3a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpPasswordRequiredState passwordRequiredState = spy(((SignUpResult.PasswordRequired) passwordRequiredResult).getNextState());

        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(passwordRequiredState, correlationId);

        SignUpSubmitPasswordTestCallback passwordCallback = new SignUpSubmitPasswordTestCallback();
        passwordRequiredState.submitPassword(password, passwordCallback);

        SignUpSubmitPasswordResult successResult = passwordCallback.get();

        // 3b. Server accepts password, returns tokens
        assertTrue(successResult instanceof SignUpResult.Complete);
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
    public void testSignUpScenario11() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up with password
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(username, null, null, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2a. Setup resend code challenge
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        SignUpCodeRequiredState codeRequiredState = spy(((SignUpResult.CodeRequired) signUpResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(codeRequiredState, correlationId);

        // 2b. Call resendCode
        SignUpResendCodeRequiredTestCallback resendCodeCallback = new SignUpResendCodeRequiredTestCallback();
        codeRequiredState.resendCode(resendCodeCallback);

        SignUpResendCodeResult resendCodeResult = resendCodeCallback.get();

        assertTrue(resendCodeResult instanceof SignUpResendCodeResult.Success);

        // 3. submit (valid) code
        // 3a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.CREDENTIAL_REQUIRED
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        SignUpCodeRequiredState submitCodeState = spy(((SignUpResendCodeResult.Success) resendCodeResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(submitCodeState, correlationId);

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        submitCodeState.submitCode(code, codeRequiredCallback);

        SignUpSubmitCodeResult passwordRequiredResult = codeRequiredCallback.get();

        assertTrue(passwordRequiredResult instanceof SignUpResult.PasswordRequired);

        //4. Submit valid password
        //4a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpPasswordRequiredState passwordRequiredState = spy(((SignUpResult.PasswordRequired) passwordRequiredResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(passwordRequiredState, correlationId);

        SignUpSubmitPasswordTestCallback passwordCallback = new SignUpSubmitPasswordTestCallback();
        passwordRequiredState.submitPassword(password, passwordCallback);

        SignUpSubmitPasswordResult successResult = passwordCallback.get();

        // 3b. Server accepts password, returns tokens
        assertTrue(successResult instanceof SignUpResult.Complete);
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
    public void testSignUpScenario12() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(username, null, null, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2. submit (valid) code
        // 2a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.CREDENTIAL_REQUIRED
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_PASSWORD
        );

        SignUpCodeRequiredState submitCodeState = spy(((SignUpResult.CodeRequired) signUpResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(submitCodeState, correlationId);

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        submitCodeState.submitCode(code, codeRequiredCallback);

        SignUpSubmitCodeResult passwordRequiredResult = codeRequiredCallback.get();

        assertTrue(passwordRequiredResult instanceof SignUpResult.PasswordRequired);

        //3. submit required attributes
        // 3a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.ATTRIBUTES_REQUIRED
        );

        SignUpPasswordRequiredState passwordRequiredState = spy(((SignUpResult.PasswordRequired) passwordRequiredResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(passwordRequiredState, correlationId);

        SignUpSubmitPasswordTestCallback passwordCallback = new SignUpSubmitPasswordTestCallback();
        passwordRequiredState.submitPassword(password, passwordCallback);

        SignUpSubmitPasswordResult attributesRequiredResult = passwordCallback.get();

        assertTrue(attributesRequiredResult instanceof SignUpResult.AttributesRequired);

        //4. Submit valid password
        //4a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpAttributesRequiredState attributesRequiredState = spy(((SignUpResult.AttributesRequired) attributesRequiredResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(attributesRequiredState, correlationId);

        SignUpSubmitUserAttributesTestCallback attributesRequiredCallback = new SignUpSubmitUserAttributesTestCallback();
        UserAttributes attributes = UserAttributes.Builder.customAttribute("attribute", "attribute").build();

        attributesRequiredState.submitAttributes(attributes, attributesRequiredCallback);
        SignUpSubmitAttributesResult successResult = attributesRequiredCallback.get();

        // 4b. Server accepts attributes, returns tokens
        assertTrue(successResult instanceof SignUpResult.Complete);
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
    public void testSignUpScenario13() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. sign up
        // 1a. Setup server response
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        // 1b. Call SDK interface
        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(username, null, null, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);

        // 2. submit (valid) code
        // 2a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.ATTRIBUTES_REQUIRED
        );

        SignUpCodeRequiredState submitCodeState = spy(((SignUpResult.CodeRequired) signUpResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(submitCodeState, correlationId);

        final SignUpCodeRequiredTestCallback codeRequiredCallback = new SignUpCodeRequiredTestCallback();
        submitCodeState.submitCode(code, codeRequiredCallback);

        SignUpSubmitCodeResult attributesRequiredResult = codeRequiredCallback.get();

        assertTrue(attributesRequiredResult instanceof SignUpResult.AttributesRequired);
        assertNotNull(((SignUpResult.AttributesRequired) attributesRequiredResult).getRequiredAttributes());

        //3. submit required attributes
        // 3a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.ATTRIBUTES_REQUIRED
        );

        SignUpAttributesRequiredState attributesRequiredState = spy(((SignUpResult.AttributesRequired) attributesRequiredResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(attributesRequiredState, correlationId);

        UserAttributes incompleteAttributes = UserAttributes.Builder.customAttribute("attribute", "incomplete_attribute").build();
        SignUpSubmitUserAttributesTestCallback attributesRequiredCallback = new SignUpSubmitUserAttributesTestCallback();

        attributesRequiredState.submitAttributes(incompleteAttributes, attributesRequiredCallback);
        SignUpSubmitAttributesResult additionalAttributesRequiredResult = attributesRequiredCallback.get();

        assertTrue(additionalAttributesRequiredResult instanceof SignUpResult.AttributesRequired);

        //4. Submit valid password
        //4a. setup server response
        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.SIGNUP_CONTINUE_SUCCESS
        );

        SignUpAttributesRequiredState additionalAttributesRequiredState = spy(((SignUpResult.AttributesRequired) attributesRequiredResult).getNextState());
        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(additionalAttributesRequiredState, correlationId);

        UserAttributes attributes = UserAttributes.Builder.customAttribute("attribute", "attribute").build();
        SignUpSubmitUserAttributesTestCallback additionalAttributesRequiredCallback = new SignUpSubmitUserAttributesTestCallback();

        additionalAttributesRequiredState.submitAttributes(attributes, additionalAttributesRequiredCallback);
        SignUpSubmitAttributesResult successResult = additionalAttributesRequiredCallback.get();

        // 4b. Server accepts password, returns tokens
        assertTrue(successResult instanceof SignUpResult.Complete);
    }

    /**
     * Test Sign Up scenario 14:
     * 1a -> signUp
     * 1b <- server does not support code authentication
     */
    @Test
    public void testSignUpScenario14() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        );

        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(username, null, null, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpError);
        assertTrue(((SignUpError) signUpResult).isBrowserRequired());
    }

    /**
     * Check that we don't get a type casting exception thrown when we get it,
     * should get error result instead.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Test
    public void testSignUpEmptyUsernameNoException() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_REDIRECT
        );

        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();

        application.signUp(emptyString, null, null, signUpTestCallback);
        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpError);
        assertTrue(((SignUpError) signUpResult).isInvalidUsername());
        assertFalse(((SignUpError) signUpResult).isBrowserRequired());
        assertFalse(((SignUpError) signUpResult).isInvalidPassword());
        assertFalse(((SignUpError) signUpResult).isInvalidAttributes());
        assertFalse(((SignUpError) signUpResult).isUserAlreadyExists());
    }

    @Test
    public void testSignUpInvalidPasswordReturnsError() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.PASSWORD_TOO_WEAK
        );

        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, password, null, signUpTestCallback);

        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpError);
        assertTrue(((SignUpError) signUpResult).isInvalidPassword());
    }

    @Test
    public void testSignUpInvalidOTPReturnsInvalidCodeError() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.SIGNUP_START_SUCCESS
        );

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpChallenge,
                correlationId,
                MockApiResponseType.CHALLENGE_TYPE_OOB
        );

        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(username, null, null, signUpTestCallback);

        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpResult.CodeRequired);
        SignUpCodeRequiredState nextState = spy((((SignUpResult.CodeRequired) signUpResult).getNextState()));

        // correlation ID field in will be null, because the mock API doesn't return this. So, we mock
        // it's value in order to make it consistent with the subsequent call to mock API.
        mockCorrelationId(nextState, correlationId);

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpContinue,
                correlationId,
                MockApiResponseType.INVALID_OOB_VALUE
        );

        SignUpCodeRequiredTestCallback submitCodeCallback = new SignUpCodeRequiredTestCallback();
        nextState.submitCode(code, submitCodeCallback);

        SignUpSubmitCodeResult result = submitCodeCallback.get();
        assertTrue(result instanceof SubmitCodeError);

        SubmitCodeError error = spy((SubmitCodeError) result);
        assertTrue(error.isInvalidCode());
    }

    @Test
    public void testSignUpWithPasswordInvalidEmailReturnsError() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.INVALID_USERNAME
        );

        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(invalidUsername, password, null, signUpTestCallback);

        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpError);
        assertTrue(((SignUpError) signUpResult).isInvalidUsername());
    }

    @Test
    public void testSignUpInvalidEmailReturnsError() throws ExecutionException, InterruptedException, TimeoutException {
        String correlationId = UUID.randomUUID().toString();

        MockApiUtils.configureMockApi(
                MockApiEndpoint.SignUpStart,
                correlationId,
                MockApiResponseType.INVALID_USERNAME
        );

        SignUpTestCallback signUpTestCallback = new SignUpTestCallback();
        application.signUp(invalidUsername, null, null, signUpTestCallback);

        SignUpResult signUpResult = signUpTestCallback.get();

        assertTrue(signUpResult instanceof SignUpError);
        assertTrue(((SignUpError) signUpResult).isInvalidUsername());
    }
}

abstract class TestCallback<T> {
    ResultFuture<T> future;

    TestCallback() {
        this.future = new ResultFuture<>();
    }

    T get() throws InterruptedException, TimeoutException, ExecutionException {
        return future.get(30, TimeUnit.SECONDS);
    }
}

class SignOutTestCallback extends TestCallback<SignOutResult> implements AccountState.SignOutCallback {

    @Override
    public void onResult(SignOutResult result) { future.setResult(result); }

    @Override
    public void onError(@NonNull BaseException exception) { future.setException(exception); }
}

class SignInTestCallback extends TestCallback<SignInResult> implements NativeAuthPublicClientApplication.SignInCallback {

    @Override
    public void onResult(SignInResult result) { future.setResult(result); }

    @Override
    public void onError(@NonNull BaseException exception) { future.setException(exception); }
}

class SignUpTestCallback extends TestCallback<SignUpResult> implements NativeAuthPublicClientApplication.SignUpCallback {

    @Override
    public void onResult(SignUpResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class SignUpCodeRequiredTestCallback extends TestCallback<SignUpSubmitCodeResult> implements SignUpCodeRequiredState.SubmitCodeCallback {

    @Override
    public void onResult(SignUpSubmitCodeResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class SignUpResendCodeRequiredTestCallback extends TestCallback<SignUpResendCodeResult> implements SignUpCodeRequiredState.SignUpWithResendCodeCallback {

    @Override
    public void onResult(SignUpResendCodeResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class SignUpSubmitUserAttributesTestCallback extends TestCallback<SignUpSubmitAttributesResult> implements SignUpAttributesRequiredState.SignUpSubmitUserAttributesCallback {

    @Override
    public void onResult(SignUpSubmitAttributesResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class SignUpSubmitPasswordTestCallback extends TestCallback<SignUpSubmitPasswordResult> implements SignUpPasswordRequiredState.SignUpSubmitPasswordCallback {

    @Override
    public void onResult(SignUpSubmitPasswordResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class SignInContinuationTestCallback extends TestCallback<SignInResult> implements SignInContinuationState.SignInContinuationCallback {

    @Override
    public void onResult(SignInResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class ResetPasswordStartTestCallback extends TestCallback<ResetPasswordStartResult> implements NativeAuthPublicClientApplication.ResetPasswordCallback {

    @Override
    public void onResult(ResetPasswordStartResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class ResetPasswordSubmitCodeTestCallback extends TestCallback<ResetPasswordSubmitCodeResult> implements ResetPasswordCodeRequiredState.SubmitCodeCallback {

    @Override
    public void onResult(ResetPasswordSubmitCodeResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class ResetPasswordSubmitPasswordTestCallback extends TestCallback<ResetPasswordSubmitPasswordResult> implements ResetPasswordPasswordRequiredState.SubmitPasswordCallback {

    @Override
    public void onResult(ResetPasswordSubmitPasswordResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class ResetPasswordResendCodeTestCallback extends TestCallback<ResetPasswordResendCodeResult> implements ResetPasswordCodeRequiredState.ResendCodeCallback {

    @Override
    public void onResult(ResetPasswordResendCodeResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class GetAccountTestCallback extends TestCallback<GetAccountResult> implements NativeAuthPublicClientApplication.GetCurrentAccountCallback {

    @Override
    public void onResult(GetAccountResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}

class GetAccessTokenTestCallback extends TestCallback<GetAccessTokenResult> implements AccountState.GetAccessTokenCallback {

    @Override
    public void onResult(GetAccessTokenResult result) {
        future.setResult(result);
    }

    @Override
    public void onError(@NonNull BaseException exception) {
        future.setException(exception);
    }
}
