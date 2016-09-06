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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Tests for {@link TokenLookupEngine}.
 */
@RunWith(AndroidJUnit4.class)
public final class TokenLookupEngineTest extends AndroidTestCase {
    static final String AUTHORITY = "https://login.microsoftonline.com/common";
    static final String CLIENT_ID = "some-client-id";
    static final String DISPLAYABLE = "some-displayable-id";
    static final String UNIQUE_ID = "some-unique-id";
    static final String HOME_OID = "some-home-oid";
    static final String ACCESS_TOKEN = "some access token";
    static final String REFRESH_TOKEN = "some refresh token";

    private TokenCache mTokenCache;
    private User mUser;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());

        mTokenCache = new TokenCache(InstrumentationRegistry.getContext());
        // make sure the tests start with a clean state.
        mTokenCache.removeAll();

        mUser = new User();
        mUser.setDisplayableId(DISPLAYABLE);
        mUser.setUniqueId(UNIQUE_ID);
        mUser.setHomeObjectId(HOME_OID);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        // clear the state left by the tests.
        mTokenCache.removeAll();
    }

    /**
     * Verify that expired AT is not returned.
     */
    @Test
    public void testGetAccessTokenForExpiredItem() throws UnsupportedEncodingException, AuthenticationException {
        // prepare an expired AT item in the cache
        final String singleScope = "scope";
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN,
                singleScope, AndroidTestUtil.getExpiredDate()));

        // access token is already expired, verify that the access token is not returned.
        final TokenLookupEngine tokenLookupEngine = getTokenLookupEngine(Collections.singleton(singleScope), "");
        assertNull(tokenLookupEngine.getAccessToken());
        final RefreshTokenCacheItem refreshTokenCacheItem = tokenLookupEngine.getRefreshToken();
        assertNotNull(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(REFRESH_TOKEN));
    }

    @Test
    public void testGetTokenWithResponseNotContainingRT() throws UnsupportedEncodingException, AuthenticationException {
        final String singleScope = "scope";
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", getTokenResponseForDefaultUser(ACCESS_TOKEN, "",
                singleScope, AndroidTestUtil.getValidExpiresOn()));

        final TokenLookupEngine tokenLookupEngine = getTokenLookupEngine(Collections.singleton(singleScope), "");
        assertTrue(ACCESS_TOKEN.equals(tokenLookupEngine.getAccessToken().getAccessToken()));
        assertNull(tokenLookupEngine.getRefreshToken());
    }

    @Test
    public void testGetTokenWithReopnseNotContainingAT() throws UnsupportedEncodingException, AuthenticationException {
        final String singleScope = "scope";
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", getTokenResponseForDefaultUser("", "",
                singleScope, AndroidTestUtil.getValidExpiresOn()));

        final TokenLookupEngine tokenLookupEngine = getTokenLookupEngine(Collections.singleton(singleScope), "");
        assertTrue(MSALUtils.isEmpty(tokenLookupEngine.getAccessToken().getAccessToken()));
        assertNull(tokenLookupEngine.getRefreshToken());
    }

    /**
     * Verify that scenario that lookup engine with policy.
     */
    @Test
    public void testGetTokenWithPolicy() throws AuthenticationException, UnsupportedEncodingException {
        // prepare a valid AT item stored with policy in the cache
        final String singleScope = "scope";
        final String policy = "some policy";
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, policy, getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN,
                singleScope, AndroidTestUtil.getValidExpiresOn()));

        // asks a token with no policy
        TokenLookupEngine tokenLookupEngine = getTokenLookupEngine(Collections.singleton(singleScope), "");
        assertNull(tokenLookupEngine.getAccessToken());
        assertNull(tokenLookupEngine.getRefreshToken());

        // asks the token with policy
        tokenLookupEngine = getTokenLookupEngine(Collections.singleton(singleScope), policy);
        assertNotNull(tokenLookupEngine.getAccessToken());
        assertNotNull(tokenLookupEngine.getRefreshToken());
        mTokenCache.removeAll();

        // save a token without policy
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN,
                singleScope, AndroidTestUtil.getValidExpiresOn()));

        // asks a token with scope
        tokenLookupEngine = getTokenLookupEngine(Collections.singleton(singleScope), policy);
        assertNull(tokenLookupEngine.getAccessToken());
        assertNull(tokenLookupEngine.getRefreshToken());
    }

    /**
     * Verify that access token return when no user is passed for lookup engine.
     */
    @Test
    public void testGetTokenNoUser() throws UnsupportedEncodingException, AuthenticationException {
        final String scope1 = "scope1";
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN,
                scope1, AndroidTestUtil.getValidExpiresOn()));

        final TokenLookupEngine lookupEngine = new TokenLookupEngine(getRequestParameters(Collections.singleton(scope1), ""), null);
        assertNotNull(lookupEngine.getAccessToken());
        assertNotNull(lookupEngine.getRefreshToken());

        // add another access token entry into cache for same user with scope1 and scope2
        final String scope2 = "scope2";
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN,
                scope1 + " " + scope2, AndroidTestUtil.getValidExpiresOn()));

        // verify token is returned for scope1
        assertNotNull(lookupEngine.getAccessToken());
        assertNotNull(lookupEngine.getRefreshToken());

        // add a token for different user for scope1
        final TokenResponse response = getTokenResponseForDifferentUser(scope1, AndroidTestUtil.getExpirationDate(
                AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES));
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", response);

        //verify that no token is returned
        assertNull(lookupEngine.getAccessToken());
        try {
            lookupEngine.getRefreshToken();
            fail();
        } catch (final AuthenticationException e) {
            assertTrue(e.getErrorCode().equals(MSALError.MULTIPLE_CACHE_ENTRY_FOUND));
        }
    }

    /**
     * Verify that access token is correctly returned for cache with multiple user.
     */
    @Test
    public void testGetTokenWithMultipleUser() throws UnsupportedEncodingException, AuthenticationException {
        final String scope = "scope1";
        final Date expirationDate = AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES);
        // save token for default user with scope1
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN,
                scope, expirationDate));

        // save token for another user with scope1
        mTokenCache.saveTokenResponse(AUTHORITY, CLIENT_ID, "", getTokenResponseForDifferentUser(scope, expirationDate));

        // retrieve token for default user1
        final TokenLookupEngine lookupEngine = getTokenLookupEngine(Collections.singleton(scope), "");
        final AccessTokenCacheItem accessTokenCacheItem = lookupEngine.getAccessToken();
        assertNotNull(accessTokenCacheItem);
        verifyUserReturnedFromCacheIsDefaultUser(accessTokenCacheItem);
        assertTrue(accessTokenCacheItem.getAccessToken().equals(ACCESS_TOKEN));

        final RefreshTokenCacheItem refreshTokenCacheItem = lookupEngine.getRefreshToken();
        assertNotNull(refreshTokenCacheItem.getRefreshToken());
        verifyUserReturnedFromCacheIsDefaultUser(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(REFRESH_TOKEN));
    }

    private void verifyUserReturnedFromCacheIsDefaultUser(final TokenCacheItem item) {
        assertTrue(item.getUniqueId().equals(UNIQUE_ID));
        assertTrue(item.getDisplayableId().equals(DISPLAYABLE));
        assertTrue(item.getHomeObjectId().equals(HOME_OID));
    }

    static TokenResponse getTokenResponseForDefaultUser(final String accessToken, final String refreshToken, final String scopesInResponse,
                                                         final Date expiresOn) throws UnsupportedEncodingException {
        final String idToken = getDefaultIdToken();

        return new TokenResponse(accessToken, idToken, refreshToken, expiresOn,
                expiresOn, AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2), scopesInResponse, "Bearer", null);
    }

    static TokenResponse getTokenResponseForDifferentUser(final String scopesInResponse, final Date expiresOn)
            throws UnsupportedEncodingException, AuthenticationException {
        final String idToken = AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", "other user", "other displayable", "sub", "tenant",
                "version", "other homeOID");

        return new TokenResponse("access_token", idToken, "refreshToken", expiresOn, expiresOn,
                AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2), scopesInResponse, "Bearer", null);
    }

    static String getDefaultIdToken() throws UnsupportedEncodingException {
        return AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", UNIQUE_ID, DISPLAYABLE, "sub", "tenant",
                "version", HOME_OID);
    }

    private TokenLookupEngine getTokenLookupEngine(final Set<String> scopes, final String policy) {
        final AuthenticationRequestParameters requestParameters =  getRequestParameters(scopes, policy);

        return new TokenLookupEngine(requestParameters, mUser);
    }

    private AuthenticationRequestParameters getRequestParameters(final Set<String> scopes, final String policy) {
        return AuthenticationRequestParameters.create(new Authority(AUTHORITY, false),
                mTokenCache, scopes, CLIENT_ID, "some redirect", policy, true, "", "", UIOptions.SELECT_ACCOUNT, UUID.randomUUID(), new Settings());
    }
}
