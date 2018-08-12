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
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

    static final String AUTHORIZE_ENDPOINT = "https://login.microsoftonline.com/sometenant/authorize";
    static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/sometenant/token";

    private Context mAppContext;
    private AccountCredentialManager mAccountCredentialManager;
    private User mDefaultUser;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());

        Authority.RESOLVED_AUTHORITY.clear();
        AndroidTestMockUtil.mockSuccessTenantDiscovery(AUTHORIZE_ENDPOINT, TOKEN_ENDPOINT);

        mAppContext = new InteractiveRequestTest.MockContext(InstrumentationRegistry.getContext().getApplicationContext());
        InteractiveRequestTest.mockNetworkConnected(mAppContext, true);
        mAccountCredentialManager = new AccountCredentialManager(mAppContext);
        // make sure the tests start with a clean state.
        AndroidTestUtil.removeAllTokens(mAppContext);

        mDefaultUser = AccountCredentialManagerTest.getDefaultUser();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        // clear the state left by the tests.
        AndroidTestUtil.removeAllTokens(mAppContext);
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClientIdProvidedAsSingleScope() {
        final Set<String> scopes = new HashSet<>();
        scopes.add(AccountCredentialManagerTest.CLIENT_ID);

        new SilentRequest(mAppContext, getRequestParameters(scopes), false, mDefaultUser);
    }

    /**
     * Verify that correct exception is thrown if device is not connected to the network.
     */
    @Test
    public void testNetworkNotConnected() throws MsalException, IOException, InterruptedException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope,
                        AndroidTestUtil.getExpiredDate(), AccountCredentialManagerTest.getDefaultClientInfo()));

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(AccountCredentialManagerTest.getDefaultIdToken(), AndroidTestUtil.ACCESS_TOKEN, singleScope, null));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        InteractiveRequestTest.mockNetworkConnected(mAppContext, false);

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope)),
                false, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(exception instanceof MsalClientException);
                assertTrue(exception.getErrorCode().equals(MsalClientException.DEVICE_NETWORK_NOT_AVAILABLE));
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
    public void testValidAccessTokenInTheCache() throws MsalException, InterruptedException, MalformedURLException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn(),
                        AccountCredentialManagerTest.getDefaultClientInfo()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope));
        final BaseRequest request = new SilentRequest(mAppContext, requestParameters, false, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));

                verifyUserReturnedInResult(authenticationResult);
                assertTrue(mAccountCredentialManager.findAccessToken(requestParameters, mDefaultUser).getAccessToken().equals(ACCESS_TOKEN));

                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);
                resultLock.countDown();
            }

            @Override
            public void onError(MsalException exception) {
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
    public void testSavedTokenInCacheNotHaveAccessToken() throws MsalException, InterruptedException, MalformedURLException {
        final String singleScope = "scope1";

        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(AndroidTestUtil.ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn(),
                        AccountCredentialManagerTest.getDefaultClientInfo()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope));
        final BaseRequest request = new SilentRequest(mAppContext, requestParameters, false, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                verifyUserReturnedInResult(authenticationResult);
                assertTrue(mAccountCredentialManager.findAccessToken(requestParameters,
                        mDefaultUser).getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));

                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);
                resultLock.countDown();
            }

            @Override
            public void onError(MsalException exception) {
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
    public void testAccessTokenNotValidRTIsUsed() throws MsalException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getExpiredDate(),
                        AccountCredentialManagerTest.getDefaultClientInfo()));

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(AccountCredentialManagerTest.getDefaultIdToken(), AndroidTestUtil.ACCESS_TOKEN, singleScope, AccountCredentialManagerTest.getDefaultClientInfo()));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope));
        final BaseRequest request = new SilentRequest(mAppContext, requestParameters, false, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));

                verifyUserReturnedInResult(authenticationResult);
                // verify that access token is stored with tenant specific authority.

                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(mAccountCredentialManager.findAccessToken(requestParameters, mDefaultUser).getAccessToken()));
                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);

                try {
                    assertTrue(AndroidTestUtil.REFRESH_TOKEN.equals(mAccountCredentialManager.findRefreshToken(requestParameters, mDefaultUser).getRefreshToken()));
                } catch (final MsalException e) {
                    fail();
                }

                assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
                resultLock.countDown();
            }

            @Override
            public void onError(MsalException exception) {
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
    public void testScopeNotProvidedTheSameOrder() throws MsalException, InterruptedException, IOException {
        // store valid token in the cache
        final String scopeInResponse = "user.read email.read scope2";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, scopeInResponse,
                        AndroidTestUtil.getValidExpiresOn(), AccountCredentialManagerTest.getDefaultClientInfo()));

        final String[] requestedScope = new String[]{"email.read", "scope2", "user.read"};
        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(new HashSet<>(Arrays.asList(
                requestedScope))), false, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);

        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                verifyUserReturnedInResult(authenticationResult);

                resultLock.countDown();
            }

            @Override
            public void onError(MsalException exception) {
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
    public void testSilentRequestScopeNotSameAsTokenCacheItem() throws MsalException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getExpiredDate(),
                        AccountCredentialManagerTest.getDefaultClientInfo()));

        final String anotherScope = "scope2";
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(AccountCredentialManagerTest.getDefaultIdToken(), AndroidTestUtil.ACCESS_TOKEN, anotherScope,
                        AccountCredentialManagerTest.getDefaultClientInfo()));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final AuthenticationRequestParameters requestParametersWithAnotherScope = getRequestParameters(Collections.singleton(anotherScope));
        final BaseRequest request = new SilentRequest(mAppContext, requestParametersWithAnotherScope, false, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(AndroidTestUtil.ACCESS_TOKEN.equals(authenticationResult.getAccessToken()));
                verifyUserReturnedInResult(authenticationResult);

                assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);

                assertNotNull(AndroidTestUtil.getAccessTokenSharedPreference(mAppContext).getString(AccessTokenCacheKey.createTokenCacheKey(
                        AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID, Collections.singleton(singleScope), mDefaultUser).toString(), null));
                // find token with the single scope1
                // The access token for scope1 in the cache is no longer valid
                assertNull(mAccountCredentialManager.findAccessToken(getRequestParameters(Collections.singleton(singleScope)), mDefaultUser));
                assertTrue(mAccountCredentialManager.findAccessToken(requestParametersWithAnotherScope, mDefaultUser).getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));

                assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
                try {
                    assertTrue(AndroidTestUtil.REFRESH_TOKEN.equals(mAccountCredentialManager.findRefreshToken(requestParametersWithAnotherScope,
                            mDefaultUser).getRefreshToken()));
                } catch (final MsalException e) {
                    fail();
                }

                resultLock.countDown();
            }

            @Override
            public void onError(MsalException exception) {
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
    public void testSilentRequestNoAuthorityMutipleAccessTokenAuthoritiesInCache() throws MsalException, IOException, InterruptedException {
        final String scope = "scope1 scope2";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, scope, AndroidTestUtil.getExpiredDate(),
                        AccountCredentialManagerTest.getDefaultClientInfo()));

        // put another token for same user with a different authority
        final String anotherAuthority = "https://login.microsoftonline.com/othertenant";
        final String accessToken = "other access token";
        final String refreshToken = "other refresh token";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, anotherAuthority, AccountCredentialManagerTest.CLIENT_ID, AccountCredentialManagerTest.getTokenResponseForDefaultUser(
                accessToken, refreshToken, scope, AndroidTestUtil.getExpiredDate(), AccountCredentialManagerTest.getDefaultClientInfo()));

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(AccountCredentialManagerTest.getDefaultIdToken(), AndroidTestUtil.ACCESS_TOKEN, scope,
                        AccountCredentialManagerTest.getDefaultClientInfo()));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final AuthenticationRequestParameters requestParameters = getRequestParameters(MsalUtils.getScopesAsSet(scope));
        final User user = User.create(new IdToken(AccountCredentialManagerTest.getDefaultIdToken()), new ClientInfo(AccountCredentialManagerTest.getDefaultClientInfo()));
        final SilentRequest request = new SilentRequest(mAppContext, requestParameters, false, user);
        request.setIsAuthorityProvided(false);

        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(exception instanceof MsalClientException);
                final MsalClientException msalClientException = (MsalClientException) exception;
                assertTrue(msalClientException.getErrorCode().equals(MsalClientException.MULTIPLE_MATCHING_TOKENS_DETECTED));
                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();
    }

    @Test
    public void testAuthorityNotProvidedButOnlyOneExist() throws MsalException, InterruptedException, IOException {
        final String scope = "scope1 scope2";
        final String anotherAuthority = "https://login.microsoftonline.com/othertenant";
        final String accessToken = "other access token";
        final String refreshToken = "other refresh token";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, anotherAuthority, AccountCredentialManagerTest.CLIENT_ID, AccountCredentialManagerTest.getTokenResponseForDefaultUser(
                accessToken, refreshToken, scope, AndroidTestUtil.getExpiredDate(), AccountCredentialManagerTest.getDefaultClientInfo()));

        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessResponse(AccountCredentialManagerTest.getDefaultIdToken(), AndroidTestUtil.ACCESS_TOKEN, scope,
                        AccountCredentialManagerTest.getDefaultClientInfo()));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);

        final AuthenticationRequestParameters requestParameters = getRequestParameters(MsalUtils.getScopesAsSet(scope));
        final User user = User.create(new IdToken(AccountCredentialManagerTest.getDefaultIdToken()), new ClientInfo(AccountCredentialManagerTest.getDefaultClientInfo()));
        final SilentRequest request = new SilentRequest(mAppContext, requestParameters, false, user);
        request.setIsAuthorityProvided(false);

        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                assertTrue(authenticationResult.getAccessToken().equals(AndroidTestUtil.ACCESS_TOKEN));
                final List<AccessTokenCacheItem> accessTokenCacheItems = AndroidTestUtil.getAllAccessTokens(mAppContext);
                assertTrue(accessTokenCacheItems.size() == 1);
                final AccessTokenCacheItem accessTokenCacheItem = accessTokenCacheItems.get(0);
                assertTrue(accessTokenCacheItem.getAuthority().equals(anotherAuthority));
                resultLock.countDown();
            }

            @Override
            public void onError(MsalException exception) {
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
    public void testNoRefreshTokenIsFound() throws MsalException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDifferentUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn(),
                        AccountCredentialManagerTest.getClientInfoForDifferentUser()));

        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope)), false, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(exception instanceof MsalUiRequiredException);
                assertTrue(exception.getErrorCode().equals(MsalUiRequiredException.NO_TOKENS_FOUND));
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
    public void testForceRefreshRequestFailedWithInvalidGrant() throws MsalException,
            InterruptedException, IOException {
        final String singleScope = "scope1";
        final String clientInfo = AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID);
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn(), clientInfo));

        mockFailureResponse("invalid_grant");

        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope)), true, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(exception instanceof MsalUiRequiredException);
                assertTrue(exception.getErrorCode().equals(MsalUiRequiredException.INVALID_GRANT));
                resultLock.countDown();
            }

            @Override
            public void onCancel() {
                fail();
            }
        });

        resultLock.await();

        // MSAL will not delete RT if receiving invalid_grant
        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
    }

    /**
     * Verify that refresh token is not cleared if RT request failed with invalid_request.
     */
    @Test
    public void testForceRefreshTokenRequestFailedWithInvalidRequest() throws MsalException, InterruptedException,
            IOException {
        final String singleScope = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mAccountCredentialManager, AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, AccountCredentialManagerTest.CLIENT_ID,
                AccountCredentialManagerTest.getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn(),
                        AccountCredentialManagerTest.getDefaultClientInfo()));

        mockFailureResponse("invalid_request");

        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
        final BaseRequest request = new SilentRequest(mAppContext, getRequestParameters(Collections.singleton(singleScope)), true, mDefaultUser);
        final CountDownLatch resultLock = new CountDownLatch(1);
        request.getToken(new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                fail();
            }

            @Override
            public void onError(MsalException exception) {
                assertTrue(exception instanceof MsalServiceException);

                final MsalServiceException msalServiceException = (MsalServiceException) exception;
                assertTrue(msalServiceException.getErrorCode().equals(MsalServiceException.INVALID_REQUEST));
                assertTrue(msalServiceException.getHttpStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);

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
        return AuthenticationRequestParameters.create(Authority.createAuthority(AndroidTestUtil.DEFAULT_AUTHORITY_WITH_TENANT, false),
                mAccountCredentialManager, scopes, AccountCredentialManagerTest.CLIENT_ID, "some redirect", "", "", UiBehavior.SELECT_ACCOUNT, null, null,
                new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()));
    }

    private void mockFailureResponse(final String errorCode) throws IOException {
        final HttpURLConnection mockedConnection = AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_REQUEST, AndroidTestUtil.getErrorResponseMessage(errorCode));
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }
}
