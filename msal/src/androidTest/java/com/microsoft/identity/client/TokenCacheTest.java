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
import java.util.HashSet;
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
    static final String ACCESS_TOKEN = "some access token";
    static final String REFRESH_TOKEN = "some refresh token";
    static final int EXPECTED_RT_SIZE = 3;

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
        final TokenResponse tokenResponse = useDefault
                ? getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, testScope, AndroidTestUtil.getValidExpiresOn(),
                getDefaultClientInfo()) : // otherwise...
                getTokenResponseForDifferentUser(ACCESS_TOKEN, REFRESH_TOKEN, testScope, AndroidTestUtil.getValidExpiresOn(), getClientInfoForDifferentUser());
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, tokenResponse);

        return getRequestParameters(AUTHORITY, Collections.singleton(testScope), CLIENT_ID);
    }

    @Test
    public void testDeleteRefreshTokenByUser() throws MsalException {
        // Add a refresh token to the cache for the default user
        final AuthenticationRequestParameters requestParameters = addTokenForUser(true);
        // Verify token was inserted
        assertNotNull(mTokenCache.findRefreshToken(requestParameters, mDefaultUser));
        // Delete that token
        mTokenCache.deleteRefreshTokenByUser(mDefaultUser, AndroidTestUtil.getTestRequestContext());
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
        mTokenCache.deleteRefreshTokenByUser(mDefaultUser, AndroidTestUtil.getTestRequestContext());
        // Verify that that the cache still contains the other token
        assertNotNull(mTokenCache.findRefreshToken(differentUserParams, User.create(new IdToken(getIdTokenForDifferentUser()),
                new ClientInfo(getClientInfoForDifferentUser()))));
    }

    @Test
    public void testDeleteAccessTokenByUser() throws MsalException {
        // Add an access token to the cache for the default user
        final AuthenticationRequestParameters defaultUserRequestParameters = addTokenForUser(true);
        // Verify that token was inserted
        assertNotNull(mTokenCache.findAccessToken(defaultUserRequestParameters, mDefaultUser));
        // Delete that token
        mTokenCache.deleteAccessTokenByUser(mDefaultUser, AndroidTestUtil.getTestRequestContext());
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
        mTokenCache.deleteAccessTokenByUser(mDefaultUser, AndroidTestUtil.getTestRequestContext());
        // Verify that that the cache still contains the other token
        assertNotNull(mTokenCache.findAccessToken(differentUserParams,
                User.create(new IdToken(getIdTokenForDifferentUser()), new ClientInfo(getClientInfoForDifferentUser()))));
    }

    /**
     * Verify that expired AT is not returned.
     */
    @Test
    public void testGetAccessTokenForExpiredItem() throws MsalException {
        // prepare an expired AT item in the cache
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getExpiredDate(), getDefaultClientInfo()));

        // access token is already expired, verify that the access token is not returned.
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(singleScope), CLIENT_ID);
        assertNull(mTokenCache.findAccessToken(requestParameters, mDefaultUser));
        final RefreshTokenCacheItem refreshTokenCacheItem = mTokenCache.findRefreshToken(requestParameters, mDefaultUser);
        assertNotNull(refreshTokenCacheItem);
        assertTrue(refreshTokenCacheItem.getRefreshToken().equals(REFRESH_TOKEN));
        assertTrue(refreshTokenCacheItem.getVersion().equals("1"));
    }

    /**
     * Verify that if RT is not returned, it won't be saved. AT is still saved correctly.
     */
    @Test
    public void testGetTokenWithResponseNotContainingRT() throws MsalException {
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, "", singleScope, AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

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
        final String uid = "some@uid_for";
        final String utid = "some@utid_for";
        final String idToken = AndroidTestUtil.getRawIdToken(displayableId, UNIQUE_ID, homeOid);
        final String clientInfo = AndroidTestUtil.createRawClientInfo(uid, utid);
        final User user = User.create(new IdToken(idToken), new ClientInfo(clientInfo));

        final TokenResponse response = new TokenResponse(
                ACCESS_TOKEN,
                idToken,
                REFRESH_TOKEN,
                AndroidTestUtil.getValidExpiresOn(),
                3599L,
                AndroidTestUtil.getValidExpiresOn(),
                AndroidTestUtil.getExpirationDate(
                        AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2
                ),
                262800L,
                scope,
                "Bearer",
                clientInfo,
                null
        );

        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, response);

        // save another token for default user
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN, scope, AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

        // verify the access token is saved
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(scope), CLIENT_ID);

        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);

        final AccessTokenCacheItem accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, user);
        assertNotNull(accessTokenCacheItem);
        assertTrue(ACCESS_TOKEN.equals(accessTokenCacheItem.getAccessToken()));
        assertTrue(idToken.equals(accessTokenCacheItem.getRawIdToken()));
    }

    /**
     * Verify that token is correctly for b2c scenario, which will have policy as part of the authority.
     */
    @Test
    public void testGetTokenWithB2cAuthority() throws MsalException {
        // prepare a valid AT item stored with policy in the cache
        final String singleScope = "scope";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, B2C_AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, singleScope, AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

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
                accessToken2, refreshToken2, singleScope, AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

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
                ACCESS_TOKEN, REFRESH_TOKEN, scope1, AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(scope1), CLIENT_ID);
        assertNotNull(mTokenCache.findAccessToken(requestParameters, getDefaultUser()));
        assertNotNull(mTokenCache.findRefreshToken(requestParameters, getDefaultUser()));

        // add another access token entry into cache for same user with scope1 and scope2
        final String scope2 = "scope2";
        final String accessToken2 = "new access token 2";
        final String refreshToken2 = "new refresh token 2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                accessToken2, refreshToken2, scope1 + " " + scope2, AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

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
        final String accessTokenForDifferentUser = "new access token 3";
        final String refreshTokenForDifferentUser = "new refresh token 3";
        final TokenResponse response = getTokenResponseForDifferentUser(accessTokenForDifferentUser, refreshTokenForDifferentUser,
                scope1, AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES), getClientInfoForDifferentUser());
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
                "refresh_token", MsalUtils.convertSetToString(scopes1, " "), AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));


        // save token with scope2 and scope3
        final Set<String> scopes2 = new HashSet<>();
        scopes2.add(scope2);
        scopes2.add(scope3);

        final String accessToken2 = "accessToken2";
        final String refreshToken2 = "refreshToken2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(accessToken2,
                refreshToken2, MsalUtils.convertSetToString(scopes2, " "), AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

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

        final Set<String> scopes1 = new HashSet<>();
        scopes1.add("scope1");

        // save access token for user with scope1
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(firstAT, "",
                MsalUtils.convertSetToString(scopes1, " "), AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

        // verify the access token is saved
        final User user = getDefaultUser();
        AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, scopes1, CLIENT_ID);
        AccessTokenCacheItem accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, user);
        assertNotNull(accessTokenCacheItem);
        assertTrue(accessTokenCacheItem.getAccessToken().equals(firstAT));
        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);

        // save another access token for the same user with scope1 and and scope2
        final Set<String> scopes2 = new HashSet<>();
        scopes2.addAll(scopes1);
        scopes2.add("scope2");
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(secondAT, "",
                MsalUtils.convertSetToString(scopes2, " "), AndroidTestUtil.getValidExpiresOn(), getDefaultClientInfo()));

        // verify there are two access token entries in the case
        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 1);

        // verify the new access token is saved, there will be two separate items in the cache.
        // scopes set is updated.
        requestParameters = getRequestParameters(AUTHORITY, scopes2, CLIENT_ID);
        accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, user);
        assertNotNull(accessTokenCacheItem);
        assertTrue(accessTokenCacheItem.getAccessToken().equals(secondAT));

        // retrieve tokens with scopes, should still be able to get the access token back
        // getAccessToken will check for scope contains. If the scope in the key contains all the scope in the item.
        requestParameters = getRequestParameters(AUTHORITY, scopes1, CLIENT_ID);
        accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, user);
        assertNotNull(accessTokenCacheItem);
        assertTrue(accessTokenCacheItem.getAccessToken().endsWith(secondAT));
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
                ACCESS_TOKEN, REFRESH_TOKEN, scope, expirationDate, getDefaultClientInfo()));

        // save token for another user with scope1
        final String accessToken = "another access token";
        final String refreshToken = "another refresh token";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID,
                getTokenResponseForDifferentUser(accessToken, refreshToken, scope, expirationDate, getClientInfoForDifferentUser()));

        assertTrue(AndroidTestUtil.getAllAccessTokens(mAppContext).size() == 2);
        assertTrue(AndroidTestUtil.getAllRefreshTokens(mAppContext).size() == 2);

        // retrieve token for default user1
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, Collections.singleton(scope), CLIENT_ID);
        final AccessTokenCacheItem accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, mDefaultUser);
        assertNotNull(accessTokenCacheItem);
        verifyUserReturnedFromCacheIsDefaultUser(accessTokenCacheItem);
        final String atId = accessTokenCacheItem.getRawIdToken();
        final String[] segments = atId.split("\\.");
        final String[] defaultIdSegments = getDefaultIdToken().split("\\.");
        assertEquals(segments[0] + segments[1], defaultIdSegments[0] + defaultIdSegments[1]);
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
                ACCESS_TOKEN, REFRESH_TOKEN, scope, expirationDate, getDefaultClientInfo()));

        // save token for default user with scope1 for another client id
        final String anotherClientId = "another-client-id";
        final String accessToken2 = "new access token for client id 2";
        final String refreshToken2 = "new refresh token for client id 2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, anotherClientId, getTokenResponseForDefaultUser(
                accessToken2, refreshToken2, scope, expirationDate, getDefaultClientInfo()));

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

    // Verify that RT is saved correctly for different client id and authority.
    @Test
    public void testSaveRTForDifferentClientIdAndAuthority() throws MsalException {
        final Set<String> scope = Collections.singleton("scope1");
        final Date expirationDate = AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES);

        // add RT with default authority, default client id
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(ACCESS_TOKEN, REFRESH_TOKEN,
                MsalUtils.convertSetToString(scope, " "), expirationDate, getDefaultClientInfo()));

        // add RT with default authority, different client id
        final String clientId2 = "another-clientid-2";
        final String accessToken2 = "access-token-2";
        final String refreshToken2 = "refresh-token-2";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, clientId2, getTokenResponseForDefaultUser(accessToken2, refreshToken2,
                MsalUtils.convertSetToString(scope, " "), expirationDate, getDefaultClientInfo()));


        // add rt for same user, different authority
        final String anotherAuthority = "https://login.microsoftonline.de/some-other-tenant";
        final String accessToken3 = "access-token-3";
        final String refreshToken3 = "refresh-token-3";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, anotherAuthority, CLIENT_ID, getTokenResponseForDefaultUser(accessToken3, refreshToken3,
                MsalUtils.convertSetToString(scope, " "), expirationDate, getDefaultClientInfo()));

        // verify that there should be 3 refresh tokens stored in the cache;
        assertTrue(mTokenCache.getAllRefreshTokens(AndroidTestUtil.getTestRequestContext()).size() == EXPECTED_RT_SIZE);

        // verify rt for default authority host and default client
        AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, scope, CLIENT_ID);
        final RefreshTokenCacheItem rtForDefaultAuthorityDefaultClient = mTokenCache.findRefreshToken(requestParameters, getDefaultUser());
        assertNotNull(rtForDefaultAuthorityDefaultClient);
        assertTrue(rtForDefaultAuthorityDefaultClient.getRefreshToken().equals(REFRESH_TOKEN));

        // verify rt for default authority host and different client
        requestParameters = getRequestParameters(AUTHORITY, scope, clientId2);
        final RefreshTokenCacheItem rtForDefaultAuthorityDifferentClient = mTokenCache.findRefreshToken(requestParameters, getDefaultUser());
        assertNotNull(rtForDefaultAuthorityDifferentClient);
        assertTrue(rtForDefaultAuthorityDifferentClient.getRefreshToken().equals(refreshToken2));

        // verify rt for another authority and default client
        requestParameters = getRequestParameters(anotherAuthority, scope, CLIENT_ID);
        final RefreshTokenCacheItem rtForAnotherAuthorityDefaultClientId = mTokenCache.findRefreshToken(requestParameters, getDefaultUser());
        assertNotNull(rtForAnotherAuthorityDefaultClientId);
        assertTrue(rtForAnotherAuthorityDefaultClientId.getRefreshToken().equals(refreshToken3));
    }

    @Test
    public void testSaveRTForMultipleUsers() throws MsalException {
        final String scope = "scope1";
        final Date expirationDate = AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES);

        // save RT for default user
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, scope, expirationDate, getDefaultClientInfo()));

        // create another user and save RT for another user
        final String anotherAccessToken = "another-access-token";
        final String anotherRefreshToken = "another-refresh-token";
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDifferentUser(anotherAccessToken,
                anotherRefreshToken, scope, expirationDate, getClientInfoForDifferentUser()));

        // verify the number of refresh token, there should be 2
        assertTrue(mTokenCache.getAllRefreshTokens(AndroidTestUtil.getTestRequestContext()).size() == 2);

        // verify RT for each user
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, MsalUtils.getScopesAsSet(scope), CLIENT_ID);
        // retrieve RT for default user
        final RefreshTokenCacheItem rtForDefaultUser = mTokenCache.findRefreshToken(requestParameters, getDefaultUser());
        assertNotNull(rtForDefaultUser);
        assertTrue(rtForDefaultUser.getRefreshToken().equals(REFRESH_TOKEN));

        // retrieve RT for another user
        final RefreshTokenCacheItem rtForAnotherUser = mTokenCache.findRefreshToken(requestParameters, getDifferentUser());
        assertNotNull(rtForAnotherUser);
        assertTrue(rtForAnotherUser.getRefreshToken().equals(anotherRefreshToken));
    }

    @Test
    public void testTokenSavedWithNoClientInfo() throws MsalException {
        final String scope = "scope1";
        final Date expirationDate = AndroidTestUtil.getExpirationDate(AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES);

        // save RT for default user
        PublicClientApplicationTest.saveTokenResponse(mTokenCache, AUTHORITY, CLIENT_ID, getTokenResponseForDefaultUser(
                ACCESS_TOKEN, REFRESH_TOKEN, scope, expirationDate, null));

        final User user = new User(DISPLAYABLE, "name", "idp", "", "");
        final AuthenticationRequestParameters requestParameters = getRequestParameters(AUTHORITY, MsalUtils.getScopesAsSet(scope), CLIENT_ID);
        final AccessTokenCacheItem accessTokenCacheItem = mTokenCache.findAccessToken(requestParameters, user);
        assertNotNull(accessTokenCacheItem);
        assertNotNull(accessTokenCacheItem.getUser());
        final User returnedUser = accessTokenCacheItem.getUser();
        assertTrue(returnedUser.getUid().equals(""));
        assertTrue(returnedUser.getUtid().equals(""));

        final RefreshTokenCacheItem refreshTokenCacheItem = mTokenCache.findRefreshToken(requestParameters, user);
        assertNotNull(refreshTokenCacheItem);
        assertNotNull(refreshTokenCacheItem.getUser());
        assertTrue(refreshTokenCacheItem.getUser().getUid().equals(""));
        assertTrue(refreshTokenCacheItem.getUser().getUtid().equals(""));
    }

    private void verifyUserReturnedFromCacheIsDefaultUser(final BaseTokenCacheItem item) {
        if (item instanceof AccessTokenCacheItem) {
            final AccessTokenCacheItem accessTokenCacheItem = (AccessTokenCacheItem) item;
            assertTrue(accessTokenCacheItem.getRawClientInfo().equals(AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID)));
        } else {
            final RefreshTokenCacheItem refreshTokenCacheItem = (RefreshTokenCacheItem) item;
            refreshTokenCacheItem.getUserIdentifier().equals(MsalUtils.getUniqueUserIdentifier(AndroidTestUtil.UID, AndroidTestUtil.UTID));
            assertTrue(refreshTokenCacheItem.getDisplayableId().equals(DISPLAYABLE));
        }
    }

    static TokenResponse getTokenResponseForDefaultUser(final String accessToken, final String refreshToken,
                                                        final String scopesInResponse, final Date expiresOn,
                                                        final String rawClientInfo)
            throws MsalException {
        final String idToken = getDefaultIdToken();

        return new TokenResponse(
                accessToken,
                idToken,
                refreshToken,
                expiresOn,
                3599L,
                expiresOn,
                AndroidTestUtil.getExpirationDate(
                        AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2
                ),
                262800L,
                scopesInResponse,
                "Bearer",
                rawClientInfo,
                null
        );
    }

    static TokenResponse getTokenResponseForDifferentUser(final String accessToken, final String refreshToken, final String scopesInResponse,
                                                          final Date expiresOn, final String clientInfo)
            throws MsalException {
        return new TokenResponse(
                accessToken,
                getIdTokenForDifferentUser(),
                refreshToken,
                expiresOn,
                3599L,
                expiresOn,
                AndroidTestUtil.getExpirationDate(
                        AndroidTestUtil.TOKEN_EXPIRATION_IN_MINUTES * 2
                ),
                262800L,
                scopesInResponse,
                "Bearer",
                clientInfo,
                null
        );
    }

    static String getIdTokenForDifferentUser() {
        return AndroidTestUtil.createIdToken(
                AUTHORITY,
                "issuer",
                "test user",
                "other user",
                "other displayable",
                "sub",
                "tenant",
                "version",
                null);
    }

    static String getClientInfoForDifferentUser() {
        return AndroidTestUtil.createRawClientInfo("another uid", "another utid");
    }

    static User getDifferentUser() throws MsalException {
        final IdToken idToken = new IdToken(getIdTokenForDifferentUser());
        final ClientInfo clientInfo = new ClientInfo(getClientInfoForDifferentUser());
        return User.create(idToken, clientInfo);
    }

    static User getDefaultUser() throws MsalException {
        final IdToken idToken = new IdToken(getDefaultIdToken());
        final ClientInfo clientInfo = new ClientInfo(getDefaultClientInfo());
        return User.create(idToken, clientInfo);
    }

    static String getDefaultIdToken() {
        return AndroidTestUtil.createIdToken(
                AUTHORITY,
                "https://login.microsoftonline.com/common",
                "test user",
                UNIQUE_ID,
                DISPLAYABLE,
                "sub",
                "tenant",
                "version",
                null
        );
    }

    static String getDefaultClientInfo() {
        return AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID);
    }

    private AuthenticationRequestParameters getRequestParameters(final String authority, final Set<String> scopes, final String clientId) {
        return AuthenticationRequestParameters.create(AuthorityMetadata.createAuthority(authority, false), mTokenCache, scopes, clientId,
                "some redirect", "", "", UiBehavior.SELECT_ACCOUNT, null, null, new RequestContext(UUID.randomUUID(), "", Telemetry.generateNewRequestId()));
    }
}