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

package com.microsoft.identity.client;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Tests for {@link SilentRequest}.
 */
@RunWith(AndroidJUnit4.class)
public final class SilentRequestTest extends AndroidTestCase {
    static final String ACCESS_TOKEN = "I am an access token";
    static final String REFRESH_TOKEN = "I am an new refresh token";

    private Context mAppContext;
    private TokenCache mTokenCache;
    private User mDefaultUser;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());

        mAppContext = new InteractiveRequestTest.MockContext(InstrumentationRegistry.getContext().getApplicationContext());
        InteractiveRequestTest.mockNetworkConnected(mAppContext, true);
        mTokenCache = new TokenCache(mAppContext);
        // make sure the tests start with a clean state.
        AndroidTestUtil.removeAllTokens(mAppContext);

        mDefaultUser = TokenCacheTest.getDefaultUser();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        // clear the state left by the tests.
        AndroidTestUtil.removeAllTokens(mAppContext);
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    /**
     * Verify that correct exception is thrown if device is not connected to the network.
     */
    @Test
    public void testNetworkNotConnected() throws AuthenticationException, IOException, InterruptedException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope,
                        AndroidTestUtil.getExpiredDate()));

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(TokenCacheTest.getDefaultIdToken(), AndroidTestUtil.ACCESS_TOKEN, singleScope));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        InteractiveRequestTest.mockNetworkConnected(mAppContext, false);

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope)), false, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(AuthenticationException exception) {
                assertTrue(exception.getErrorCode().equals(MSALError.DEVICE_CONNECTION_NOT_AVAILABLE));
                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
    }

    /**
     * Verify that valid access token is successfully returned.
     */
    @Test
    public void testValidAccessTokenInTheCache() throws AuthenticationException, InterruptedException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope));
        final BaseRequest request = new SilentRequest(mAppContext, requestParameters, false, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));

                verifyUserReturnedInResult(authenticationResult);

                assertTrue(mTokenCache.findAccessToken(requestParameters, mDefaultUser).getAccessToken().equals(ACCESS_TOKEN));
                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);
                resultLock.countDown();
            }

            @Override
            public void onError(AuthenticationException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
    }

    /**
     * Verify that if the access token item in the cache does not contain access_token, id token will be returned instead.
     */
    @Test
    public void testSavedTokenInCacheNotHaveAccessToken() throws AuthenticationException, InterruptedException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDefaultUser(AndroidTestUtil.ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope));
        final BaseRequest request = new SilentRequest(mAppContext, requestParameters, false, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                verifyUserReturnedInResult(authenticationResult);
                assertTrue(mTokenCache.findAccessToken(requestParameters,
                        mDefaultUser).getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));
                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);
                resultLock.countDown();
            }

            @Override
            public void onError(AuthenticationException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
    }

    /**
     * Verify that refresh token is correctly used if at is not valid.
     */
    @Test
    public void testAccessTokenNotValidRTIsUsed() throws AuthenticationException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getExpiredDate()));

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(TokenCacheTest.getDefaultIdToken(), AndroidTestUtil.ACCESS_TOKEN, singleScope));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope));
        final BaseRequest request = new SilentRequest(mAppContext, requestParameters, false, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));

                verifyUserReturnedInResult(authenticationResult);

                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(mTokenCache.findAccessToken(requestParameters, mDefaultUser).getAccessToken()));
                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);

                try {
                    assertTrue(AndroidTestUtil.REFRESH_TOKEN.equals(mTokenCache.findRefreshToken(requestParameters, mDefaultUser).getRefreshToken()));
                } catch (final AuthenticationException e) {
                    fail();
                }

                assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
                resultLock.countDown();
            }

            @Override
            public void onError(AuthenticationException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
    }

    /**
     * Verify that token is correctly retrieved if scopes returned in the original response is not in the same order as
     * what's requested for.
     */
    @Test
    public void testScopeNotProvidedTheSameOrder() throws AuthenticationException, InterruptedException, IOException {
        // store valid token in the cache
        final String scopeInResponse = "user.read email.read scope2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, scopeInResponse,
                        AndroidTestUtil.getValidExpiresOn()));

        final String[] requestedScope = new String[]{"email.read", "scope2", "user.read"};
        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(new HashSet<>(Arrays.asList(
                requestedScope))), false, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);

        assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                verifyUserReturnedInResult(authenticationResult);

                resultLock.countDown();
            }

            @Override
            public void onError(AuthenticationException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
    }

    @Test
    public void testSilentRequestScopeNotSameAsTokenCacheItem() throws AuthenticationException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getExpiredDate()));

        final String anotherScope = "scope2";
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(TokenCacheTest.getDefaultIdToken(), AndroidTestUtil.ACCESS_TOKEN, anotherScope));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final AuthenticationRequestParameters requestParametersWithAnotherScope = getRequestParameters(Collections.singleton(anotherScope));
        final BaseRequest request = new SilentRequest(mAppContext, requestParametersWithAnotherScope, false, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                verifyUserReturnedInResult(authenticationResult);

                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);

                assertNotNull(AndroidTestUtil.getAccessTokenSharedPreference(mAppContext).getString(TokenCacheKey.createKeyForAT(
                        AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID, Collections.singleton(singleScope), mDefaultUser).toString(), null));
                // find token with the single scope1
                // The access token for scope1 in the cache is no longer valid
                assertNull(mTokenCache.findAccessToken(getRequestParameters(Collections.singleton(singleScope)), mDefaultUser));

                assertTrue(mTokenCache.findAccessToken(requestParametersWithAnotherScope, mDefaultUser).getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));

                assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
                try {
                    assertTrue(AndroidTestUtil.REFRESH_TOKEN.equals(mTokenCache.findRefreshToken(requestParametersWithAnotherScope,
                            mDefaultUser).getRefreshToken()));
                } catch (final AuthenticationException e) {
                    fail();
                }

                resultLock.countDown();
            }

            @Override
            public void onError(AuthenticationException exception) {
                fail();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
    }

    /**
     * Verify that correct exception is returned if no refresh token is found.
     */
    @Test
    public void testNoRefreshTokenIsFound() throws AuthenticationException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDifferentUser(singleScope, AndroidTestUtil.getValidExpiresOn()));

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope)), false, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(AuthenticationException exception) {
                assertTrue(exception.getErrorCode().equals(MSALError.INTERACTION_REQUIRED));
                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
    }

    /**
     * Verify that refresh token is deleted if failed with invalid grant.
     */
    @Test
    public void testForceRefreshRequestFailedWithInvalidGrant() throws AuthenticationException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        mockFailureResponse("invalid_grant");

        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope)), true, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(AuthenticationException exception) {
                assertTrue(exception.getErrorCode().equals(MSALError.INTERACTION_REQUIRED));
                assertNotNull(exception.getCause());
                assertTrue(exception instanceof AuthenticationException);
                final AuthenticationException innerException = (AuthenticationException) exception.getCause();
                assertTrue(innerException.getErrorCode().equals(MSALError.OAUTH_ERROR));

                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
        // verify that no rt existed in the cache
        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 0);
    }

    /**
     * Verify that refresh token is not cleared if RT request failed with invalid_request.
     */
    @Test
    public void testForceRefreshTokenRequestFailedWithInvalidRequest() throws AuthenticationException, InterruptedException,
            IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AndroidTestUtil.DEFAULT_AUTHORITY, TokenCacheTest.CLIENT_ID,
                TokenCacheTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        mockFailureResponse("invalid_request");

        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope)), true, mDefaultUser, ApiEventTest.getRandomTestApiEventBuilder());
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(AuthenticationException exception) {
                assertTrue(exception.getErrorCode().equals(MSALError.INTERACTION_REQUIRED));
                assertNotNull(exception.getCause());
                assertTrue(exception instanceof AuthenticationException);
                final AuthenticationException innerException = (AuthenticationException) exception.getCause();
                assertTrue(innerException.getErrorCode().equals(MSALError.OAUTH_ERROR));

                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
        // verify that no rt existed in the cache
        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
    }

    private void verifyUserReturnedInResult(final AuthenticationResult result) {
        final User user = result.getUser();
        assertNotNull(user);
    }

    private AuthenticationRequestParameters getRequestParameters(final Set<String> scopes) {
        return AuthenticationRequestParameters.create(Authority.createAuthority(AndroidTestUtil.DEFAULT_AUTHORITY, false),
                mTokenCache, scopes, TokenCacheTest.CLIENT_ID, "some redirect", "", "", UIBehavior.SELECT_ACCOUNT,
                new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()));
    }

    private void mockFailureResponse(final String errorCode) throws IOException {
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage(errorCode));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }
}
