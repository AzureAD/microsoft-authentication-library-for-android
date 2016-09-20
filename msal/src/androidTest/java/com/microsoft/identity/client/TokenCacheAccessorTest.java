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

import java.io.UnsupportedEncodingException;
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
    private static final int EXPECTED_RT_SIZE = 3;

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
     * Verify that access token is stored with a strict match of the scopes, even if the token contains all the scopes for
     * existing entry, we store as separate entry.
     */
    @Test
    public void testSaveAT() throws UnsupportedEncodingException, AuthenticationException {
        final String firstAT = "accessToken1";
        final String secondAT = "accessToken2";

        final Set<String> scopes = new HashSet<>();
        scopes.add("scope1");

        // save access token for user with scope1
        final User user = getUser(DISPLAYABLE, UNIQUE_ID, HOME_OID);
        final TokenCacheItem tokenCacheItem = new TokenCacheItem(AUTHORITY, CLIENT_ID, "",
                getTokenResponse(firstAT, "", scopes, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenCacheItem);

        // verify the access token is saved
        final TokenCacheKey keyForAT = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, scopes, user, "");
        List<TokenCacheItem> accessTokens = mAccessor.getAccessToken(keyForAT);
        assertTrue(accessTokens.size() == 1);
        final TokenCacheItem accessTokenCacheItem = accessTokens.get(0);
        assertTrue(accessTokenCacheItem.getToken().equals(firstAT));
        assertTrue(mAccessor.getAllAccessTokens().size() == 1);

        // save another access token for the same user with scope1 and and scope2
        scopes.add("scope2");
        final TokenCacheItem tokenCacheItem2 = new TokenCacheItem(AUTHORITY, CLIENT_ID, "",
                getTokenResponse(secondAT, "", scopes, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenCacheItem2);

        // verify there are two access token entries in the cace
        assertTrue(mAccessor.getAllAccessTokens().size() == 2);

        // verify the new access token is saved, there will be two separate items in the cache
        final TokenCacheKey keyForAT2 = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, scopes, user, "");
        accessTokens = mAccessor.getAccessToken(keyForAT2);
        assertTrue(accessTokens.size() == 1);
        TokenCacheItem tokenCacheItemToVerify = accessTokens.get(0);
        assertTrue(tokenCacheItemToVerify.getToken().equals(secondAT));


        // if retrieve access token with keyForAT1, should still be able to get the access token back
        accessTokens = mAccessor.getAccessToken(keyForAT);
        Assert.assertTrue(accessTokens.size() == 1);
        tokenCacheItemToVerify = accessTokens.get(0);
        assertTrue(tokenCacheItemToVerify.getToken().equals(firstAT));
    }

    /**
     * Verify that we store access tokens in separate entries if the scope in the response has interaction.
     */
    @Test
    public void testSaveATWithScopeIntersection() throws UnsupportedEncodingException, AuthenticationException {
        // save at with scope1 and scope2
        final Set<String> scopes1 = new HashSet<>();
        scopes1.add("scope1");
        scopes1.add("scope2");

        final User user = getUser(DISPLAYABLE, UNIQUE_ID, HOME_OID);
        final TokenCacheItem tokenCacheItem = new TokenCacheItem(AUTHORITY, CLIENT_ID, "",
                getTokenResponse("accessToken", "", scopes1, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenCacheItem);

        // save token with scope2 and scope3
        final Set<String> scopes2 = new HashSet<>();
        scopes2.add("scope2");
        scopes2.add("scope3");
        final TokenCacheItem tokenCacheItem2 = new TokenCacheItem(AUTHORITY, CLIENT_ID, "",
                getTokenResponse("accessToken2", "", scopes2, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenCacheItem2);

        // verify the current access token entries in the cache
        assertTrue(mAccessor.getAllAccessTokens().size() == 2);

        // retrieve token for scope2, there won't be any token returned
        final Set<String> scopesToRetrieve = Collections.singleton("scope2");
        final TokenCacheKey key = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, scopesToRetrieve, user, "");
        final List<TokenCacheItem> accessTokens = mAccessor.getAccessToken(key);
        assertTrue(accessTokens.size() == 0);
    }

    /**
     * Verify that AT is saved correctly for multiple users.
     */
    @Test
    public void testSaveATWithMultipleUser() throws UnsupportedEncodingException, AuthenticationException {
        final Set<String> scopes = Collections.singleton("scope");

        // save token for user1
        final TokenCacheItem tokenCacheItem = new TokenCacheItem(AUTHORITY, CLIENT_ID, "",
                getTokenResponse("accessToken", "", scopes, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenCacheItem);

        // save token for user2
        final String anotherDisplayable = "anotherDisplayable";
        final String anotherUniqueId = "another-unique-id";
        final String anotherHomeOid = "another-home-oid";
        final TokenCacheItem tokenItemForAnotherUser = new TokenCacheItem(AUTHORITY, CLIENT_ID, "",
                getTokenResponse("accessToken2", "", scopes, AndroidTestUtil.getRawIdToken(anotherDisplayable,
                        anotherUniqueId, anotherHomeOid)));
        mAccessor.saveAccessToken(tokenItemForAnotherUser);

        assertTrue(mAccessor.getAllAccessTokens().size() == 2);
    }

    /**
     * Verify that AT is stored correctly for scopes with no intersection.
     */
    @Test
    public void testSaveATWithSingleUserNoScopeIntersection() throws UnsupportedEncodingException, AuthenticationException {
        final Set<String> scopes1 = Collections.singleton("scope1");
        final TokenCacheItem tokenItem1 = new TokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("accessToken", "",
                scopes1, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenItem1);

        final Set<String> scopes2 = Collections.singleton("scope2");
        final TokenCacheItem tokenCacheItem2 = new TokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("accessToken2", "",
                scopes2, getIdTokenWithDefaultUser()));
        mAccessor.saveAccessToken(tokenCacheItem2);

        assertTrue(mAccessor.getAllAccessTokens().size() == 2);
    }

    /**
     * Verify that RT is saved correctly for single user case.
     */
    @Test
    public void testSaveRT() throws UnsupportedEncodingException, AuthenticationException {
        // save RT item with scope1
        final Set<String> scope1 = Collections.singleton("scope1");
        final RefreshTokenCacheItem rtItem = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("", "refreshToken",
                scope1, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtItem);

        // save RT item with scope2 for same user
        final Set<String> scope2 = Collections.singleton("scope2");
        final RefreshTokenCacheItem rtItem2 = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("", "refreshToken2",
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
        final RefreshTokenCacheItem rtItem3 = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("", "refreshToken3",
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
    public void testSaveRTMultipleUsers() throws UnsupportedEncodingException, AuthenticationException {
        final Set<String> scope = Collections.singleton("scope");

        final RefreshTokenCacheItem rtForUser1 = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("", "refreshToken1",
                scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForUser1);

        final String anotherDisplayable = "anotherDisplayable";
        final String anotherUniqueId = "some-other-unique-id";
        final String anotherHomeOid = "some-other-home-oid";
        final RefreshTokenCacheItem rtForUser2 = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("", "refreshToken2",
                scope, AndroidTestUtil.getRawIdToken(anotherDisplayable, anotherUniqueId, anotherHomeOid)));
        mAccessor.saveRefreshToken(rtForUser2);

        assertTrue(mAccessor.getAllRefreshTokens().size() == 2);
        final List<RefreshTokenCacheItem> retrievedRTForUser1 = mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID,
                getUser(DISPLAYABLE, UNIQUE_ID, HOME_OID), ""));
        assertTrue(retrievedRTForUser1.size() == 1);
        assertTrue(retrievedRTForUser1.get(0).getRefreshToken().equals("refreshToken1"));

        final List<RefreshTokenCacheItem> retrievedRtForUser2 = mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID,
                getUser(anotherDisplayable, anotherUniqueId, anotherHomeOid), ""));
        assertTrue(retrievedRtForUser2.size() == 1);
        assertTrue(retrievedRtForUser2.get(0).getRefreshToken().equals("refreshToken2"));
    }

    // Verify that RT is saved correctly for different client id and authority.
    @Test
    public void testSaveRTForDifferentClientIdAndAuthority() throws UnsupportedEncodingException, AuthenticationException {
        final Set<String> scope = Collections.singleton("scope1");

        // add rt with default client id, authority and user
        final RefreshTokenCacheItem rtForDefaultClient = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("", "refreshToken1",
                scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForDefaultClient);

        // add rt with default clientid, authority, user and policy
        final RefreshTokenCacheItem rtForDefaultClientIDWithPolicy = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, "policy", getTokenResponse("",
                "refreshToken1WithPolicy", scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForDefaultClientIDWithPolicy);

        // add rt with another clientid
        final String anotherClientId = "another_clientId";
        final RefreshTokenCacheItem rtForAnotherClientId = new RefreshTokenCacheItem(AUTHORITY, anotherClientId, "", getTokenResponse("",
                "refreshToken2", scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForAnotherClientId);

        // add rt for same user, different authority
        final String anotherAuthority = "another_authority";
        final RefreshTokenCacheItem rtForDifferentAuthority = new RefreshTokenCacheItem(anotherAuthority, CLIENT_ID, "", getTokenResponse("",
                "refreshToken3", scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtForDifferentAuthority);

        // TODO: enable or not
        assertTrue(mAccessor.getAllRefreshTokens().size() == EXPECTED_RT_SIZE);
        final List<RefreshTokenCacheItem> rtItem = mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID, getDefaultUser(), ""));
        assertTrue(rtItem.size() == 1);
        assertTrue(rtItem.get(0).getRefreshToken().equals("refreshToken3"));
        assertTrue(mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID, getDefaultUser(), "policy")).size() == 1);
        assertTrue(mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(anotherClientId, getDefaultUser(), "")).size() == 1);
    }

    @Test
    public void testDeleteRTItems() throws UnsupportedEncodingException, AuthenticationException {
        final Set<String> scope = Collections.singleton("scope");

        final RefreshTokenCacheItem rtItem = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, "", getTokenResponse("accessToken", "refresh_token",
                scope, getIdTokenWithDefaultUser()));
        mAccessor.saveRefreshToken(rtItem);

        assertTrue(mAccessor.getAllRefreshTokens().size() == 1);
        assertTrue(mAccessor.getRefreshToken(TokenCacheKey.createKeyForRT(CLIENT_ID, getDefaultUser(), "")).size() == 1);

        mAccessor.deleteRefreshToken(rtItem);
        assertTrue(mAccessor.getAllRefreshTokens().size() == 0);
    }

    private User getDefaultUser() throws UnsupportedEncodingException, AuthenticationException {
        return getUser(DISPLAYABLE, UNIQUE_ID, HOME_OID);
    }

    private User getUser(final String displayable, final String uniqueId, final String homeOid)
            throws UnsupportedEncodingException, AuthenticationException {
        final IdToken idToken = new IdToken(AndroidTestUtil.getRawIdToken(displayable, uniqueId, homeOid));
        final User user = new User(idToken);
        user.setDisplayableId(displayable);
        user.setUniqueId(uniqueId);
        user.setHomeObjectId(homeOid);

        return user;
    }

    private SuccessTokenResponse getTokenResponse(final String accessToken, final String refreshToken, final Set<String> scopes,
                                                  final String idToken)
            throws UnsupportedEncodingException, AuthenticationException {
        final TokenResponse tokenResponse = new TokenResponse(accessToken, idToken, refreshToken, new Date(), new Date(), new Date(),
                MSALUtils.convertSetToString(scopes, " "), "Bearer", null);
        return new SuccessTokenResponse(tokenResponse);
    }

    private String getIdTokenWithDefaultUser() throws UnsupportedEncodingException {
        return AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", UNIQUE_ID, DISPLAYABLE, "sub", "some-tenant",
                "version", HOME_OID);
    }
}
