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
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link TokenCacheAccessor}.
 */
@RunWith(AndroidJUnit4.class)
public final class TokenCacheAccessorTest extends AndroidTestCase {
    private static final String AUTHORITY = "https://login.microsoftonline.com/tenant";
    private static final String CLIENT_ID = "some-client-id";
    private static final String DISPLAYABLE = "test@tenant.onmicrosoft.com";
    private static final String UNIQUE_ID = "some-unique-id";
    private static final String HOME_OID = "some-home-oid";
    private static final int EXPECTED_RT_SIZE = 2;

    private TokenCacheAccessor mAccessor;
    private Context mAppContext;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        InstrumentationRegistry.getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache",
                InstrumentationRegistry.getContext().getCacheDir().getPath());

        mAppContext = InstrumentationRegistry.getContext().getApplicationContext();
        mAccessor = new TokenCacheAccessor(mAppContext);
        AndroidTestUtil.removeAllTokens(mAppContext);
    }

    @After
    public void tearDown() {
        AndroidTestUtil.removeAllTokens(mAppContext);
        assertTrue(mAccessor.getAllAccessTokens().size() == 0);
        assertTrue(mAccessor.getAllRefreshTokens().size() == 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenCacheAccessorConstructor() {
        new TokenCacheAccessor(null);
    }

    /**
     * Verify that access token is stored with the passed in cache key.
     */
    @Test
    public void testSaveAT() throws AuthenticationException {
        final String firstAT = "accessToken1";
        final String secondAT = "accessToken2";

        final Set<String> scopes = new HashSet<>();
        scopes.add("scope1");

        // save access token for user with scope1
        final User user = getUser(DISPLAYABLE, UNIQUE_ID, HOME_OID);
        final AccessTokenCacheItem tokenCacheItem = new AccessTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse(firstAT, "",
                scopes, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenCacheItem);

        // verify the access token is saved
        assertTrue(mAccessor.getAllAccessTokens().size() == 1);

        // save another access token for the same user with scope1 and and scope2
        scopes.add("scope2");
        final AccessTokenCacheItem accessTokenCacheItem2 = new AccessTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse(secondAT, "",
                scopes, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(accessTokenCacheItem2);

        // verify there are two access token entries in the case
        assertTrue(mAccessor.getAllAccessTokens().size() == 2);
    }

    /**
     * Verify that AT is saved correctly for multiple users.
     */
    @Test
    public void testSaveATWithMultipleUser() throws AuthenticationException {
        final Set<String> scopes = Collections.singleton("scope");

        // save token for user1
        final AccessTokenCacheItem accessTokenCacheItem = new AccessTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("accessToken",
                "", scopes, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(accessTokenCacheItem);

        // save token for user2
        final String anotherDisplayable = "anotherDisplayable";
        final String anotherUniqueId = "another-unique-id";
        final String anotherHomeOid = "another-home-oid";
        final AccessTokenCacheItem tokenItemForAnotherUser = new AccessTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("accessToken2",
                "", scopes, AndroidTestUtil.getRawIdToken(anotherDisplayable, anotherUniqueId, anotherHomeOid)));
        mAccessor.saveAccessToken(tokenItemForAnotherUser);

        assertTrue(mAccessor.getAllAccessTokens().size() == 2);
    }

    /**
     * Verify that AT is stored correctly for scopes with no intersection.
     */
    @Test
    public void testSaveATWithSingleUserNoScopeIntersection() throws AuthenticationException {
        final Set<String> scopes1 = Collections.singleton("scope1");
        final AccessTokenCacheItem tokenItem1 = new AccessTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("accessToken", "",
                scopes1, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenItem1);

        final Set<String> scopes2 = Collections.singleton("scope2");
        final AccessTokenCacheItem accessTokenCacheItem2 = new AccessTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("accessToken2", "",
                scopes2, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(accessTokenCacheItem2);

        assertTrue(mAccessor.getAllAccessTokens().size() == 2);
    }

    /**
     * Verify that RT is saved correctly for single user case.
     */
    @Test
    public void testSaveRT() throws AuthenticationException {
        // save RT item with scope1
        final Set<String> scope1 = Collections.singleton("scope1");
        final RefreshTokenCacheItem rtItem = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("", "refreshToken",
                scope1, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtItem);

        // save RT item with scope2 for same user
        final Set<String> scope2 = Collections.singleton("scope2");
        final RefreshTokenCacheItem rtItem2 = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("", "refreshToken2",
                scope2, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtItem2);

        // verify refresh tokens in the cache
        List<RefreshTokenCacheItem> rts = mAccessor.getAllRefreshTokens();
        assertTrue(rts.size() == 1);
        assertTrue(rts.get(0).getRefreshToken().equals("refreshToken2"));

        // save items with intersection scopes for same user
        final Set<String> scope3 = new HashSet<>();
        scope3.add("scope2");
        scope3.add("scope3");
        final RefreshTokenCacheItem rtItem3 = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("", "refreshToken3",
                scope3, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtItem3);

        // verify saved RTs
        rts = mAccessor.getAllRefreshTokens();
        assertTrue(rts.size() == 1);
        assertTrue(rts.get(0).getRefreshToken().equals("refreshToken3"));
    }

    /**
     * Verify that RT is saved correctly for multi-user case.
     */
    @Test
    public void testSaveRTMultipleUsers() throws AuthenticationException {
        final Set<String> scope = Collections.singleton("scope");

        final RefreshTokenCacheItem rtForUser1 = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("", "refreshToken1",
                scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForUser1);

        final String anotherDisplayable = "anotherDisplayable";
        final String anotherUniqueId = "some-other-unique-id";
        final String anotherHomeOid = "some-other-home-oid";
        final RefreshTokenCacheItem rtForUser2 = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("", "refreshToken2",
                scope, AndroidTestUtil.getRawIdToken(anotherDisplayable, anotherUniqueId, anotherHomeOid)));
        mAccessor.saveRefreshToken(rtForUser2);

        assertTrue(mAccessor.getAllRefreshTokens().size() == 2);
        final List<RefreshTokenCacheItem> retrievedRTForUser1 = mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID,
                getUser(DISPLAYABLE, UNIQUE_ID, HOME_OID)));
        assertTrue(retrievedRTForUser1.size() == 1);
        assertTrue(retrievedRTForUser1.get(0).getRefreshToken().equals("refreshToken1"));

        final List<RefreshTokenCacheItem> retrievedRtForUser2 = mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID,
                getUser(anotherDisplayable, anotherUniqueId, anotherHomeOid)));
        assertTrue(retrievedRtForUser2.size() == 1);
        assertTrue(retrievedRtForUser2.get(0).getRefreshToken().equals("refreshToken2"));
    }

    // Verify that RT is saved correctly for different client id and authority.
    @Test
    public void testSaveRTForDifferentClientIdAndAuthority() throws AuthenticationException {
        final Set<String> scope = Collections.singleton("scope1");

        // add rt with default client id, authority and user
        final RefreshTokenCacheItem rtForDefaultClient = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("", "refreshToken1",
                scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForDefaultClient);

        // add rt with default clientid, authority, user and policy
        final RefreshTokenCacheItem rtForDefaultClientIDWithPolicy = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("",
                "refreshToken1WithPolicy", scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForDefaultClientIDWithPolicy);

        // add rt with another clientid
        final String anotherClientId = "another_clientId";
        final RefreshTokenCacheItem rtForAnotherClientId = new RefreshTokenCacheItem(AUTHORITY, anotherClientId, getTokenResponse("",
                "refreshToken2", scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForAnotherClientId);

        // add rt for same user, different authority
        final String anotherAuthority = "another_authority";
        final RefreshTokenCacheItem rtForDifferentAuthority = new RefreshTokenCacheItem(anotherAuthority, CLIENT_ID, getTokenResponse("",
                "refreshToken3", scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForDifferentAuthority);

        // TODO: enable or not
        assertTrue(mAccessor.getAllRefreshTokens().size() == EXPECTED_RT_SIZE);
        final List<RefreshTokenCacheItem> rtItem = mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID, getDefaultUser()));
        assertTrue(rtItem.size() == 1);
        assertTrue(rtItem.get(0).getRefreshToken().equals("refreshToken3"));
        assertTrue(mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID, getDefaultUser())).size() == 1);
        assertTrue(mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(anotherClientId, getDefaultUser())).size() == 1);
    }

    @Test
    public void testDeleteRTItems() throws AuthenticationException {
        final Set<String> scope = Collections.singleton("scope");

        final RefreshTokenCacheItem rtItem = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse("accessToken", "refresh_token",
                scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtItem);

        assertTrue(mAccessor.getAllRefreshTokens().size() == 1);
        assertTrue(mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID, getDefaultUser())).size() == 1);

        mAccessor.deleteRefreshToken(rtItem);
        assertTrue(mAccessor.getAllRefreshTokens().size() == 0);
    }

    private User getDefaultUser() throws AuthenticationException {
        return getUser(DISPLAYABLE, UNIQUE_ID, HOME_OID);
    }

    static User getUser(final String displayable, final String uniqueId, final String homeOid)
            throws AuthenticationException {
        final IdToken idToken = new IdToken(AndroidTestUtil.getRawIdToken(displayable, uniqueId, homeOid));
        final User user = new User(idToken);
        user.setDisplayableId(displayable);
        user.setUniqueId(uniqueId);
        user.setHomeObjectId(homeOid);

        return user;
    }

    static TokenResponse getTokenResponse(final String accessToken, final String refreshToken, final Set<String> scopes,
                                           final String idToken)
            throws AuthenticationException {
        return new TokenResponse(accessToken, idToken, refreshToken, new Date(), new Date(), new Date(),
                MSALUtils.convertSetToString(scopes, " "), "Bearer", null);
    }

    private String getIdTokenWithDefaultUser() {
        return AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", UNIQUE_ID, DISPLAYABLE, "sub", "some-tenant",
                "version", HOME_OID);
    }
}
