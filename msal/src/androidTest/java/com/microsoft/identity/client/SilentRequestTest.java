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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;
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
    private User mUser;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());

        mAppContext = new InteractiveRequestTest.MockContext(InstrumentationRegistry.getContext().getApplicationContext());
        InteractiveRequestTest.mockNetworkConnected(mAppContext, true);
        mTokenCache = new TokenCache(mAppContext);
        // make sure the tests start with a clean state.
        mTokenCache.removeAll();

        mUser = new User();
        mUser.setDisplayableId(TokenLookupEngineTest.DISPLAYABLE);
        mUser.setUniqueId(TokenLookupEngineTest.UNIQUE_ID);
        mUser.setHomeObjectId(TokenLookupEngineTest.HOME_OID);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        // clear the state left by the tests.
        mTokenCache.removeAll();
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    /**
     * Verify that correct exception is thrown if device is not connected to the network.
     */
    @Test
    public void testNetworkNotConnected() throws AuthenticationException, IOException, InterruptedException {
        final String singleScope = "scope1";
        mTokenCache.saveTokenResponse(AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, "",
                TokenLookupEngineTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope,
                        AndroidTestUtil.getExpiredDate()));

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(TokenLookupEngineTest.getDefaultIdToken(), singleScope));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        InteractiveRequestTest.mockNetworkConnected(mAppContext, false);

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope), ""), false, mUser);
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
    public void testValidAccessTokenInTheCache() throws UnsupportedEncodingException, AuthenticationException, InterruptedException {
        final String singleScope = "scope1";
        mTokenCache.saveTokenResponse(AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, "",
                TokenLookupEngineTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope), ""), false, mUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(ACCESS_TOKEN.equals(authenticationResult.getToken()));

                verifyUserReturnedInResult(authenticationResult);

                final TokenCache tokenCache = authenticationResult.getUser().getTokenCache();
                assertTrue(tokenCache.getAllAccessTokens().size() == 1);
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
    public void testSavedTokenInCacheNotHaveAccessToken() throws UnsupportedEncodingException, AuthenticationException, InterruptedException {
        final String singleScope = "scope1";
        mTokenCache.saveTokenResponse(AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, "",
                TokenLookupEngineTest.getTokenResponseForDefaultUser("", REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope), ""), false, mUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        final String expectedToken = TokenLookupEngineTest.getDefaultIdToken();
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(expectedToken.equals(authenticationResult.getToken()));
                verifyUserReturnedInResult(authenticationResult);
                assertTrue(authenticationResult.getUser().getTokenCache().getAllAccessTokens().size() == 1);
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
     * Verify that refresh token is corrrectly used if at is not valid.
     */
    @Test
    public void testAccessTokenNotValidRTIsUsed() throws AuthenticationException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        mTokenCache.saveTokenResponse(AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, "",
                TokenLookupEngineTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope,
                        AndroidTestUtil.getExpiredDate()));

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(TokenLookupEngineTest.getDefaultIdToken(), singleScope));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope), ""), false, mUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getToken()));

                verifyUserReturnedInResult(authenticationResult);

                final TokenCache cache = authenticationResult.getUser().getTokenCache();
                final List<AccessTokenCacheItem> allATs = cache.getAllAccessTokens();
                assertTrue(allATs.size() == 1);
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(allATs.get(0).getAccessToken()));

                final List<RefreshTokenCacheItem> allRTs = cache.getAllRefreshTokens();
                assertTrue(allRTs.size() == 1);
                assertTrue(AndroidTestUtil.REFRESH_TOKEN.equals(allRTs.get(0).getRefreshToken()));
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
        mTokenCache.saveTokenResponse(AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, "",
                TokenLookupEngineTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope,
                        AndroidTestUtil.getExpiredDate()));

        final String anotherScope = "scope2";
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(TokenLookupEngineTest.getDefaultIdToken(), anotherScope));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(anotherScope), ""), false, mUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getToken()));
                verifyUserReturnedInResult(authenticationResult);

                final TokenCache cache = authenticationResult.getUser().getTokenCache();
                assertTrue(cache.getAllAccessTokens().size() == 2);

                final List<AccessTokenCacheItem> atForScope1 = mTokenCache.getAccessTokenItem(TokenCacheKey.createKeyForAT(
                        AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, Collections.singleton(singleScope), mUser, ""));
                assertTrue(atForScope1.size() == 1);
                assertTrue(atForScope1.get(0).getAccessToken().equals(ACCESS_TOKEN));

                final List<AccessTokenCacheItem> atForScope2 = cache.getAccessTokenItem(TokenCacheKey.createKeyForAT(
                        AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, Collections.singleton(anotherScope), mUser, ""));
                assertTrue(atForScope2.size() == 1);
                assertTrue(atForScope2.get(0).getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));

                final List<RefreshTokenCacheItem> allRTs = cache.getAllRefreshTokens();
                assertTrue(allRTs.size() == 1);
                assertTrue(AndroidTestUtil.REFRESH_TOKEN.equals(allRTs.get(0).getRefreshToken()));
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
        mTokenCache.saveTokenResponse(AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, "policy",
                TokenLookupEngineTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope), ""), false, mUser);
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
        mTokenCache.saveTokenResponse(AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, "",
                TokenLookupEngineTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        mockFailureResponse("invalid_grant");

        assertTrue(mTokenCache.getAllRefreshTokens().size() == 1);
        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope), ""), true, mUser);
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
        assertTrue(mTokenCache.getAllRefreshTokens().size() == 0);
    }

    /**
     * Verify that refresh token is not cleared if RT request failed with invalid_request.
     */
    @Test
    public void testForceRefreshTokenRequestFailedWithInvalidRequest() throws AuthenticationException, InterruptedException,
            IOException {
        final String singleScope = "scope1";
        mTokenCache.saveTokenResponse(AndroidTestUtil.DEFAULT_AUTHORITY, TokenLookupEngineTest.CLIENT_ID, "",
                TokenLookupEngineTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        mockFailureResponse("invalid_request");

        assertTrue(mTokenCache.getAllRefreshTokens().size() == 1);
        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope), ""), true, mUser);
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
        assertTrue(mTokenCache.getAllRefreshTokens().size() == 1);
    }

    private void verifyUserReturnedInResult(final AuthenticationResult result) {
        final User user = result.getUser();
        assertNotNull(user);
        assertTrue(user.getClientId().equals(TokenLookupEngineTest.CLIENT_ID));
        assertNotNull(user.getTokenCache());
    }

    private AuthenticationRequestParameters getRequestParameters(final Set<String> scopes, final String policy) {
        return AuthenticationRequestParameters.create(new Authority(AndroidTestUtil.DEFAULT_AUTHORITY, false),
                mTokenCache, scopes, TokenLookupEngineTest.CLIENT_ID, "some redirect", policy, true, "", "", UIOptions.SELECT_ACCOUNT, UUID.randomUUID(), new Settings());
    }

    private void mockFailureResponse(final String errorCode) throws IOException {
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage(errorCode));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }
}
