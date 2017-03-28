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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Tests for {@link TokenCache}.
 */
@RunWith(AndroidJUnit4.class)
public final class TokenCacheTest extends AndroidTestCase {
    static final String AUTHORITY = "https://login.microsoftonline.com/common";
    static final String B2C_AUTHORITY = "https://login.microsoftonline.com/tfp/tenant/policy";
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
        Telemetry.disableForTest(true);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        // clear the state left by the tests.
        AndroidTestUtil.removeAllTokens(mAppContext);
        Telemetry.disableForTest(false);
    }

    private AuthenticationRequestParameters addTokenForUser(final boolean useDefault) throws MsalException {
        final String testScope = "scope";
        // Prepare a TokenResponse for either the default of the 'different' User, by param
        final TokenResponse tokenResponse = useDefault ?
                getTokenResponseForDefaultUser(
                        ACCESS_TOKEN,
                        REFRESH_TOKEN,
                        testScope,
                        AndroidTestUtil.getValidExpiresOn()) : // otherwise...
                getTokenResponseForDifferentUser(
                        testScope,
                        AndroidTestUtil.getValidExpiresOn()
                );
        PublicClientApplicationTest.saveTokenResponse(
                mTokenCache,
                AUTHORITY,
                CLIENT_ID,
                tokenResponse
        );
        return getRequestParameters(
                AUTHORITY,
                Collections.singleton(testScope),
                CLIENT_ID
        );
    }

    @Test
    public void testDeleteRefreshTokenByUser() throws MsalException {
        // Add a refresh token to the cache for the default user
        final AuthenticationRequestParameters requestParameters = addTokenForUser(true);
        // Verify token was inserted
        assertNotNull(mTokenCache.findRefreshToken(requestParameters, mDefaultUser));
        // Delete that token
        mTokenCache.deleteRefreshTokenByUser(mDefaultUser, Telemetry.generateNewRequestId());
        // Verify that the token is deleted
        assertNull(mTokenCache.findRefreshToken(requestParameters, mDefaultUser));
    }

    @Test
    public void testDeleteRefreshTokenByUserClearsCorrectToken() throws MsalException {
        // Add a refresh token to the cache that is not associated with the current user
        final AuthenticationRequestParameters differentUserParams = addTokenForUser(false);
        // Add a refresh token to the cache for the default user
        addTokenForUser(true);
        // Delete the default user's token
        mTokenCache.deleteRefreshTokenByUser(mDefaultUser, Telemetry.generateNewRequestId());
        // Verify that that the cache still contains the other token
        assertNotNull(mTokenCache.findRefreshToken(differentUserParams, new User(new IdToken(getIdTokenForDifferentUser()))));
    }

    @Test
    public void testDeleteAccessTokenByUser() throws MsalException {
        // Add an access token to the cache for the default user
        final AuthenticationRequestParameters defaultUserRequestParameters = addTokenForUser(true);
        // Verify that token was inserted
        assertNotNull(mTokenCache.findAccessToken(defaultUserRequestParameters, mDefaultUser));
        // Delete that token
        mTokenCache.deleteAccessTokenByUser(mDefaultUser, Telemetry.generateNewRequestId());
        // Verify that the token is deleted
        assertNull(mTokenCache.findAccessToken(defaultUserRequestParameters, mDefaultUser));
    }

    @Test
    public void testDeleteAccessTokenByUserClearsCorrectToken() throws MsalException {
        // Add an access token to the cache that is not associated with the current user
        final AuthenticationRequestParameters differentUserParams = addTokenForUser(false);
        // Add an access token to the cache for the default user
        addTokenForUser(true);
        // Delete the default user's token
        mTokenCache.deleteAccessTokenByUser(mDefaultUser, Telemetry.generateNewRequestId());
        // Verify that that the cache still contains the other token
        assertNotNull(
                mTokenCache.findAccessToken(
                        differentUserParams,
                        new User(
                                new IdToken(getIdTokenForDifferentUser()
                                )
                        )
                )
        );
    }

    /**
     * Verify that expired AT is not returned.
     */
    @Test
    public void testGetAccessTokenForExpiredItem() throws MsalException {
        // prepare an expired AT item in the cache
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getExpiredDate()));

        // access token is already expired, verify that the access token is not returned.
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(singleScope), CLIENT_ID);
        assertNull(mTokenCache.findAccessToken(requestParameters, mDefaultUser));
        final RefreshTokenCacheItem refreshTokenCacheItem = mTokenCache.findRefreshToken(requestParameters, mDefaultUser);
        assertNotNull(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(REFRESH_TOKEN));
    }

    /**
     * Verify that if RT is not returned, it won't be saved. AT is still saved correctly.
     */
    @Test
    public void testGetTokenWithResponseNotContainingRT() throws MsalException {
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, "", singleScope, AndroidTestUtil.getValidExpiresOn()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(singleScope), CLIENT_ID);
        assertTrue(ACCESS_TOKEN.equals(mTokenCache.findAccessToken(requestParameters, mDefaultUser).getAccessToken()));
        assertNull(mTokenCache.findRefreshToken(requestParameters, mDefaultUser));
    }

    /**
     * Verify that tokens are retrieved correctly if displayable id contains special character. Token cache key
     * for access token is stored with authority-clientid-homeoid, special characters in displayable doesn't
     * affect the key creation.
     */
    @Test
    public void testGetTokenWithUserContainSpecialCharacter() throws MsalException {
        final String scope = "scope";
        final String displayableId = "a$$b+c%3$*c_d#^d@contoso.com";
        final String homeOid = "home_oid_for_different_user";
        final String idToken = AndroidTestUtil.getRawIdToken(displayableId, UNIQUE_ID, homeOid);
        final User user = new User(new IdToken(idToken));

        final TokenResponse response = new TokenResponse(ACCESS_TOKEN, idToken, REFRESH_TOKEN, AndroidTestUtil.getValidExpiresOn(),
                AndroidTestUtil.getValidExpiresOn(), AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2),
                scope, "Bearer", null);

        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, response);

        // save another token for default user
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, scope, AndroidTestUtil.getValidExpiresOn()));

        // verify the access token is saved
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(scope), CLIENT_ID);

        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);

        final AccessTokenCacheItem accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, user);
        assertNotNull(accessTokenCacheItem);
        assertTrue(ACCESS_TOKEN.equals(accessTokenCacheItem.getAccessToken()));
        assertTrue(idToken.equals(accessTokenCacheItem.getRawIdToken()));
        assertTrue(displayableId.equals(accessTokenCacheItem.getDisplayableId()));
    }

    /**
     * Verify that token is correctly for b2c scenario, which will have policy as part of the authority.
     */
    @Test
    public void testGetTokenWithB2cAuthority() throws MsalException {
        // prepare a valid AT item stored with policy in the cache
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, B2C_AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn()));

        // asks a token with aad authority
        final AuthenticationRequestParameters requestParametersWithAadAuthoirty = getRequestParameters(AUTHORITY, Collections.singleton(
                singleScope), CLIENT_ID);
        assertNull(mTokenCache.findAccessToken(requestParametersWithAadAuthoirty, mDefaultUser));
        // refresh token is not cached with authority.
        assertNotNull(mTokenCache.findRefreshToken(requestParametersWithAadAuthoirty, mDefaultUser));

        // asks the token with b2c authority
        final AuthenticationRequestParameters requestParametersWithB2cAuthority = getRequestParameters(B2C_AUTHORITY, Collections.singleton(
                singleScope), CLIENT_ID);
        assertNotNull(mTokenCache.findAccessToken(requestParametersWithB2cAuthority, mDefaultUser));
        assertNotNull(mTokenCache.findRefreshToken(requestParametersWithB2cAuthority, mDefaultUser));
        AndroidTestUtil.removeAllTokens(mAppContext);

        // save a token without paad authority
        final String accessToken2 = "some_access_token2";
        final String refreshToken2 = "some_refresh_token2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                accessToken2, refreshToken2, singleScope, AndroidTestUtil.getValidExpiresOn()));

        // asks a token with scope
        final AccessTokenCacheItem accessTokenCacheItem = mTokenCache.findAccessToken(requestParametersWithB2cAuthority, mDefaultUser);
        final RefreshTokenCacheItem refreshTokenCacheItem = mTokenCache.findRefreshToken(requestParametersWithB2cAuthority, mDefaultUser);
        assertNull(accessTokenCacheItem);
        assertNotNull(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(refreshToken2));
    }

    /**
     * User has to be passed in for silent request. Verify token lookup.
     */
    @Test
    public void testGetTokenForUser() throws MsalException {
        final String scope1 = "scope1";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, scope1, AndroidTestUtil.getValidExpiresOn()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(scope1), CLIENT_ID);
        assertNotNull(mTokenCache.findAccessToken(requestParameters, getDefaultUser()));
        assertNotNull(mTokenCache.findRefreshToken(requestParameters, getDefaultUser()));

        // add another access token entry into cache for same user with scope1 and scope2
        final String scope2 = "scope2";
        final String accessToken2 = "new access token 2";
        final String refreshToken2 = "new refresh token 2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                accessToken2, refreshToken2, scope1 + " " + scope2, AndroidTestUtil.getValidExpiresOn()));

        // verify token is returned for scope1, there is only one entry for access token and returned access token should
        // be the latest one.
        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);
        final AccessTokenCacheItem accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, getDefaultUser());
        assertNotNull(accessTokenCacheItem);
        assertTrue(accessTokenCacheItem.getAccessToken().equals(accessToken2));

        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);
        final RefreshTokenCacheItem refreshTokenCacheItem = mTokenCache.findRefreshToken(requestParameters, getDefaultUser());
        assertNotNull(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(refreshToken2));

        // add a token for different user for scope1
        final TokenResponse response = getTokenResponseForDifferentUser(scope1, AndroidTestUtil.getExpirationDate(
                AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES));
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, response);

        //verify token lookup for the second user
        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);
        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 2);
        assertNotNull(mTokenCache.findAccessToken(requestParameters, getDifferentUser()));
        assertNotNull(mTokenCache.findRefreshToken(requestParameters, getDifferentUser()));
    }

    /**
     * Verify that if scopes in stored access token has intersection with the new access token item, remove them first.
     */
    @Test
    public void testSaveATWithScopeIntersection() throws MsalException {
        final String scope1 = "scope1";
        final String scope2 = "scope2";
        final String scope3 = "scope3";

        // save at with scope1 and scope2
        final Set<String> scopes1 = new HashSet<>();
        scopes1.add(scope1);
        scopes1.add(scope2);

        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser("accessToken",
                "refresh_token", MSALUtils.convertSetToString(scopes1, " "), AndroidTestUtil.getValidExpiresOn()));


        // save token with scope2 and scope3
        final Set<String> scopes2 = new HashSet<>();
        scopes2.add(scope2);
        scopes2.add(scope3);

        final String accessToken2 = "accessToken2";
        final String refreshToken2 = "refreshToken2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(accessToken2,
                refreshToken2, MSALUtils.convertSetToString(scopes2, " "), AndroidTestUtil.getValidExpiresOn()));

        // verify the current access token entries in the cache, the first access token entry with s1, s2 will be deleted.
        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);

        // refresh token is keyed by clientid-user
        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 1);

        // retrieve token for scope2, current entry for access token is s2 and s3.
        final Set<String> scopesToRetrieve = Collections.singleton(scope2);
        final AccessTokenCacheItem tokenCacheItem = mTokenCache.findAccessToken(getRequestParameters(AUTHORITY, scopesToRetrieve, CLIENT_ID), getDefaultUser());
        assertNotNull(tokenCacheItem);
        assertTrue(tokenCacheItem.getAccessToken().equals(accessToken2));

        // retrieve token for scope3
        final AccessTokenCacheItem tokenCacheItemForScope3 = mTokenCache.findAccessToken(getRequestParameters(AUTHORITY, Collections.singleton(scope3), CLIENT_ID),
                getDefaultUser());
        assertNotNull(tokenCacheItemForScope3);
        assertTrue(tokenCacheItemForScope3.getAccessToken().equals(accessToken2));

        // retrieve token for scope2 and scope 3
        final AccessTokenCacheItem tokenCacheItemForScope2And3 = mTokenCache.findAccessToken(getRequestParameters(AUTHORITY, scopes2, CLIENT_ID), getDefaultUser());
        assertNotNull(tokenCacheItemForScope2And3);
        assertTrue(tokenCacheItemForScope2And3.getAccessToken().equals(accessToken2));

        // retrieve token for scope 1
        final AccessTokenCacheItem accessTokenCacheItemForScope1 = mTokenCache.findAccessToken(getRequestParameters(AUTHORITY, Collections.singleton(scope1), CLIENT_ID),
                getDefaultUser());
        assertNull(accessTokenCacheItemForScope1);

        // retrieve token for scope1, scope2, scope3
        final Set<String> allScopes = new TreeSet<>();
        allScopes.add(scope1);
        allScopes.add(scope2);
        allScopes.add(scope3);
        final AccessTokenCacheItem accessTokenCacheItemForAllThreeScopes = mTokenCache.findAccessToken(getRequestParameters(AUTHORITY, allScopes, CLIENT_ID), getDefaultUser());
        assertNull(accessTokenCacheItemForAllThreeScopes);
    }

    /**
     * Verify that access token is stored with the passed in cache key.
     */
    @Test
    public void testSaveAT() throws MsalException {
        final String firstAT = "accessToken1";
        final String secondAT = "accessToken2";

        final Set<String> scopes = new HashSet<>();
        scopes.add("scope1");

        // save access token for user with scope1
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(firstAT, "",
                MSALUtils.convertSetToString(scopes, " "), AndroidTestUtil.getValidExpiresOn()));

        // verify the access token is saved
        final User user = getDefaultUser();
        final TokenCacheKey keyForAT = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, scopes, user);
        List<AccessTokenCacheItem> accessTokens = mTokenCache.getAccessToken(keyForAT, Telemetry.generateNewRequestId());
        assertTrue(accessTokens.size() == 1);
        final AccessTokenCacheItem accessTokenCacheItem = accessTokens.get(0);
        assertTrue(accessTokenCacheItem.getAccessToken().equals(firstAT));
        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);

        // save another access token for the same user with scope1 and and scope2
        scopes.add("scope2");
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(secondAT, "",
                MSALUtils.convertSetToString(scopes, " "), AndroidTestUtil.getValidExpiresOn()));

        // verify there are two access token entries in the case
        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);

        // verify the new access token is saved, there will be two separate items in the cache
        final TokenCacheKey keyForAT2 = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, scopes, user);
        accessTokens = mTokenCache.getAccessToken(keyForAT2, Telemetry.generateNewRequestId());
        assertTrue(accessTokens.size() == 1);
        AccessTokenCacheItem accessTokenCacheItemToVerify = accessTokens.get(0);
        assertTrue(accessTokenCacheItemToVerify.getAccessToken().equals(secondAT));

        // if retrieve access token with keyForAT1, should still be able to get the access token back
        // getAccessToken will check for scope contains. If the scope in the key contains all the scope in the item.
        accessTokens = mTokenCache.getAccessToken(keyForAT, Telemetry.generateNewRequestId());
        Assert.assertTrue(accessTokens.size() == 1);
        final AccessTokenCacheItem accessTokenForScope1and2 = accessTokens.get(0);
        assertTrue(accessTokenForScope1and2.getAccessToken().equals(secondAT));
    }

    /**
     * Verify that access token is correctly returned for cache with multiple user.
     */
    @Test
    public void testGetTokenWithMultipleUser() throws MsalException {
        final String scope = "scope1";
        final Date expirationDate = AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES);
        // save token for default user with scope1
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, scope, expirationDate));

        // save token for another user with scope1
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDifferentUser(scope, expirationDate));

        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);
        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 2);

        // retrieve token for default user1
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(scope), CLIENT_ID);
        final AccessTokenCacheItem accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, mDefaultUser);
        assertNotNull(accessTokenCacheItem);
        verifyUserReturnedFromCacheIsDefaultUser(accessTokenCacheItem);
        assertTrue(accessTokenCacheItem.getAccessToken().equals(ACCESS_TOKEN));

        final RefreshTokenCacheItem refreshTokenCacheItem = mTokenCache.findRefreshToken(requestParameters, mDefaultUser);
        assertNotNull(refreshTokenCacheItem.getRefreshToken());
        verifyUserReturnedFromCacheIsDefaultUser(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(REFRESH_TOKEN));
    }

    /**
     * Verify token look up for same user different client id.
     */
    @Test
    public void testGetTokenForSameUserDifferentApp() throws MsalException {
        final String scope = "scope1";
        final Date expirationDate = AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES);
        // save token for default user with scope1
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, scope, expirationDate));

        // save token for default user with scope1 for another client id
        final String anotherClientId = "another-client-id";
        final String accessToken2 = "new access token for client id 2";
        final String refreshToken2 = "new refresh token for client id 2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, anotherClientId, getTokenResponseForDefaultUser(
                accessToken2, refreshToken2, scope, expirationDate));

        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);
        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 2);

        // retrieve token for user1, client id 1
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(scope), CLIENT_ID);
        final AccessTokenCacheItem accessTokenItemForClientId1 = mTokenCache.findAccessToken(requestParameters, getDefaultUser());
        assertTrue(accessTokenItemForClientId1.getAccessToken().equals(ACCESS_TOKEN));

        final RefreshTokenCacheItem refreshTokenCacheItemForClientId1 = mTokenCache.findRefreshToken(requestParameters, getDefaultUser());
        assertTrue(refreshTokenCacheItemForClientId1.getRefreshToken().equals(REFRESH_TOKEN));

        // retrieve token for user1, client id2
        final AuthenticationRequestParameters requestParametersForClientId2 = getRequestParameters(AUTHORITY, Collections.singleton(scope), anotherClientId);
        final AccessTokenCacheItem accessTokenCacheItemForClientId2 = mTokenCache.findAccessToken(requestParametersForClientId2, getDefaultUser());
        assertTrue(accessTokenCacheItemForClientId2.getAccessToken().equals(accessToken2));

        final RefreshTokenCacheItem refreshTokenCacheItemForClientId2 = mTokenCache.findRefreshToken(requestParametersForClientId2, getDefaultUser());
        assertTrue(refreshTokenCacheItemForClientId2.getRefreshToken().equals(refreshToken2));
    }

    private void verifyUserReturnedFromCacheIsDefaultUser(final BaseTokenCacheItem item) {
        assertTrue(item.getUniqueId().equals(UNIQUE_ID));
        assertTrue(item.getDisplayableId().equals(DISPLAYABLE));
        assertTrue(item.getHomeObjectId().equals(HOME_OID));
    }

    static TokenResponse getTokenResponseForDefaultUser(final String accessToken, final String refreshToken,
                                                        final String scopesInResponse, final Date expiresOn)
            throws MsalException {
        final String idToken = getDefaultIdToken();

        return new TokenResponse(accessToken, idToken, refreshToken, expiresOn,
                expiresOn, AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2), scopesInResponse, "Bearer", null);
    };

    static TokenResponse getTokenResponseForDifferentUser(final String scopesInResponse, final Date expiresOn)
            throws MsalException {
        return new TokenResponse("access_token", getIdTokenForDifferentUser(), "refreshToken", expiresOn, expiresOn,
                AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2), scopesInResponse, "Bearer", null);
    }

    static String getIdTokenForDifferentUser() {
        return AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", "other user", "other displayable", "sub", "tenant",
                "version", "other homeOID");
    }

    static User getDifferentUser() throws MsalException {
        final IdToken idToken = new IdToken(getIdTokenForDifferentUser());
        return new User(idToken);
    }

    static User getDefaultUser() throws MsalException {
        final IdToken idToken = new IdToken(getDefaultIdToken());
        return new User(idToken);
    }

    static String getDefaultIdToken() {
        return AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", UNIQUE_ID, DISPLAYABLE, "sub", "tenant",
                "version", HOME_OID);
    }

    private AuthenticationRequestParameters getRequestParameters(final String authority, final Set<String> scopes, final String clientId) {
        return AuthenticationRequestParameters.create(Authority.createAuthority(authority, false),
                mTokenCache, scopes, clientId, "some redirect", "", "", UIBehavior.SELECT_ACCOUNT,
                new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()));
    }
}
