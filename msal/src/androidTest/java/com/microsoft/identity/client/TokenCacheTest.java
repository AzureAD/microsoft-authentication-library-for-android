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

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Tests for {@link TokenCache}.
 */
@RunWith(AndroidJUnit4.class)
public final class TokenCacheTest extends AndroidTestCase {
    static final String AUTHORITY = "https://login.microsoftonline.com/common";
    static final String CLIENT_ID = "some-client-id";
    static final String DISPLAYABLE = "some-displayable-id";
    static final String UNIQUE_ID = "some-unique-id";
    static final String HOME_OID = "some-home-oid";
    static final String ACCESS_TOKEN = "some access token";
    static final String REFRESH_TOKEN = "some refresh token";

    private TokenCache mTokenCache;
    private User mDefaultUser;
    private Context mAppContext;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());

        mAppContext = InstrumentationRegistry.getContext();
        mTokenCache = new TokenCache(mAppContext);
        // make sure the tests start with a clean state.
        AndroidTestUtil.removeAllTokens(mAppContext);

        mDefaultUser = getDefaultUser();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        // clear the state left by the tests.
        AndroidTestUtil.removeAllTokens(mAppContext);
    }

    /**
     * Verify that expired AT is not returned.
     */
    @Test
    public void testGetAccessTokenForExpiredItem() throws AuthenticationException {
        // prepare an expired AT item in the cache
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getExpiredDate()));

        // access token is already expired, verify that the access token is not returned.
        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope), "");
        assertNull(mTokenCache.findAccessToken(requestParameters, mDefaultUser));
        final RefreshTokenCacheItem refreshTokenCacheItem = mTokenCache.findRefreshToken(requestParameters, mDefaultUser);
        assertNotNull(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(REFRESH_TOKEN));
    }

    @Test
    public void testGetTokenWithResponseNotContainingRT() throws AuthenticationException {
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, "", singleScope, AndroidTestUtil.getValidExpiresOn()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope), "");
        assertTrue(ACCESS_TOKEN.equals(mTokenCache.findAccessToken(requestParameters, mDefaultUser).getToken()));
        assertNull(mTokenCache.findRefreshToken(requestParameters, mDefaultUser));
    }

    /**
     * Verify that if access token is not returned in the token response but id token is returned, id token will be stored as
     * token.
     */
    @Test
    public void testGetTokenWithResponseNotContainingAT() throws  AuthenticationException {
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser("", "",
                singleScope, AndroidTestUtil.getValidExpiresOn()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(singleScope), "");
        final String accessToken = mTokenCache.findAccessToken(requestParameters, mDefaultUser).getToken();
        assertNotNull(accessToken);
        assertTrue(accessToken.equals(getDefaultIdToken()));
        assertNull(mTokenCache.findRefreshToken(requestParameters, mDefaultUser));
    }

    /**
     * Verify that tokens are retrieved correctly if displayable id contains special character.
     */
    @Test
    public void testGetTokenWithUserContainSpecialCharacter() throws AuthenticationException {
        final String scope = "scope";
        final String displayableId = "a$$b+c%3$*c_d#^d@contoso.com";
        final String idToken = AndroidTestUtil.getRawIdToken(displayableId, UNIQUE_ID, HOME_OID);
        final User user = new User(new IdToken(idToken));

        final TokenResponse response = new TokenResponse(ACCESS_TOKEN, idToken, REFRESH_TOKEN, AndroidTestUtil.getValidExpiresOn(),
                AndroidTestUtil.getValidExpiresOn(), AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2),
                scope, "Bearer", null);

        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, response);

        // save another token for default user
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, scope, AndroidTestUtil.getValidExpiresOn()));

        // verify the access token is saved
        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(scope), "");

        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);

        final TokenCacheItem tokenCacheItem = mTokenCache.findAccessToken(requestParameters, user);
        assertNotNull(tokenCacheItem);
        assertTrue(ACCESS_TOKEN.equals(tokenCacheItem.getToken()));
        assertTrue(idToken.equals(tokenCacheItem.getRawIdToken()));
        assertTrue(displayableId.equals(tokenCacheItem.getDisplayableId()));
    }

    /**
     * Verify that token is correctly when cache key contains policy.
     */
    @Test
    public void testGetTokenWithPolicy() throws AuthenticationException {
        // prepare a valid AT item stored with policy in the cache
        final String singleScope = "scope";
        final String policy = "some policy";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        // asks a token with no policy
        final AuthenticationRequestParameters requestParametersWithoutPolicy = getRequestParameters(Collections.singleton(
                singleScope), "");
        assertNull(mTokenCache.findAccessToken(requestParametersWithoutPolicy, mDefaultUser));
        assertNull(mTokenCache.findRefreshToken(requestParametersWithoutPolicy, mDefaultUser));

        // asks the token with policy
        final AuthenticationRequestParameters requestParametersWithPolicy = getRequestParameters(Collections.singleton(
                singleScope), policy);
        assertNotNull(mTokenCache.findAccessToken(requestParametersWithPolicy, mDefaultUser));
        assertNotNull(mTokenCache.findRefreshToken(requestParametersWithPolicy, mDefaultUser));
        AndroidTestUtil.removeAllTokens(mAppContext);

        // save a token without policy
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        // asks a token with scope
        assertNull(mTokenCache.findAccessToken(requestParametersWithPolicy, mDefaultUser));
        assertNull(mTokenCache.findRefreshToken(requestParametersWithPolicy, mDefaultUser));
    }

    /**
     * Verify that access token return when no user is passed for lookup.
     */
    @Test
    public void testGetTokenNoUser() throws AuthenticationException {
        final String scope1 = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, scope1, AndroidTestUtil.getValidExpiresOn()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(scope1), "");
        assertNotNull(mTokenCache.findAccessToken(requestParameters, null));
        assertNotNull(mTokenCache.findRefreshToken(requestParameters, null));

        // add another access token entry into cache for same user with scope1 and scope2
        final String scope2 = "scope2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, scope1 + " " + scope2, AndroidTestUtil.getValidExpiresOn()));

        // verify token is returned for scope1
        assertNotNull(mTokenCache.findAccessToken(requestParameters, null));
        assertNotNull(mTokenCache.findRefreshToken(requestParameters, null));

        // add a token for different user for scope1
        final TokenResponse response = getTokenResponseForDifferentUser(scope1, AndroidTestUtil.getExpirationDate(
                AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES));
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, response);

        //verify that no token is returned
        assertNull(mTokenCache.findAccessToken(requestParameters, null));
        try {
            mTokenCache.findRefreshToken(requestParameters, null);
            fail();
        } catch (final AuthenticationException e) {
            assertTrue(e.getErrorCode().equals(MSALError.MULTIPLE_CACHE_ENTRY_FOUND));
        }
    }

    /**
     * Verify that access token is correctly returned for cache with multiple user.
     */
    @Test
    public void testGetTokenWithMultipleUser() throws AuthenticationException {
        final String scope = "scope1";
        final Date expirationDate = AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES);
        // save token for default user with scope1
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, scope, expirationDate));

        // save token for another user with scope1
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDifferentUser(scope, expirationDate));

        // retrieve token for default user1
        final AuthenticationRequestParameters requestParameters = getRequestParameters(Collections.singleton(scope), "");
        final TokenCacheItem tokenCacheItem = mTokenCache.findAccessToken(requestParameters, mDefaultUser);
        assertNotNull(tokenCacheItem);
        verifyUserReturnedFromCacheIsDefaultUser(tokenCacheItem);
        assertTrue(tokenCacheItem.getToken().equals(ACCESS_TOKEN));

        final RefreshTokenCacheItem refreshTokenCacheItem = mTokenCache.findRefreshToken(requestParameters, mDefaultUser);
        assertNotNull(refreshTokenCacheItem.getRefreshToken());
        verifyUserReturnedFromCacheIsDefaultUser(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(REFRESH_TOKEN));
    }

    private void verifyUserReturnedFromCacheIsDefaultUser(final BaseTokenCacheItem item) {
        assertTrue(item.getUniqueId().equals(UNIQUE_ID));
        assertTrue(item.getDisplayableId().equals(DISPLAYABLE));
        assertTrue(item.getHomeObjectId().equals(HOME_OID));
    }

    static TokenResponse getTokenResponseForDefaultUser(final String accessToken, final String refreshToken,
                                                        final String scopesInResponse, final Date expiresOn)
            throws AuthenticationException {
        final String idToken = getDefaultIdToken();

        return new TokenResponse(accessToken, idToken, refreshToken, expiresOn,
                expiresOn, AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2), scopesInResponse, "Bearer", null);
    };

    static TokenResponse getTokenResponseForDifferentUser(final String scopesInResponse, final Date expiresOn)
            throws AuthenticationException {
        final String idToken = AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", "other user", "other displayable", "sub", "tenant",
                "version", "other homeOID");

        return new TokenResponse("access_token", idToken, "refreshToken", expiresOn, expiresOn,
                AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2), scopesInResponse, "Bearer", null);
    }

    static User getDefaultUser() throws AuthenticationException {
        final IdToken idToken = new IdToken(getDefaultIdToken());
        return new User(idToken);
    }

    static String getDefaultIdToken() {
        return AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", UNIQUE_ID, DISPLAYABLE, "sub", "tenant",
                "version", HOME_OID);
    }

    private AuthenticationRequestParameters getRequestParameters(final Set<String> scopes, final String policy) {
        return AuthenticationRequestParameters.create(Authority.createAuthority(AUTHORITY, false),
                mTokenCache, scopes, CLIENT_ID, "some redirect", policy, "", "", UIOptions.SELECT_ACCOUNT,
                new RequestContext(UUID.randomUUID(), ""));
    }
}
