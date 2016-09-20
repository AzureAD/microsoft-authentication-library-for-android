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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Tests for {@link TokenCacheKey}.
 */
public final class TokenCacheKeyTest {
    private static final String AUTHORITY = "https://login.microsoftonline.com/common";
    private static final String CLIENT_ID = "some-client-id";
    private static final String POLICY = "signin";
    private static final String DISPLAYABLE = "test@contoso.onmicrosoft.com";
    private static final String UNIQUE_ID = "some-unique-id";
    private static final String HOME_OBJECT_ID = "some-home-oid";

    // TODO: add ignore for now. If everybody agrees on not putting authority as the key for rt entry, remove the test.
    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void testAccessTokenKeyCreationNoAuthority() throws UnsupportedEncodingException, AuthenticationException {
        TokenCacheKey.createKeyForAT(null, CLIENT_ID, getScopes(), getUser(), POLICY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAccessTokenKeyCreationNoClientId() throws UnsupportedEncodingException, AuthenticationException {
        TokenCacheKey.createKeyForAT(AUTHORITY, null, getScopes(), getUser(), POLICY);
    }

    @Test(expected = NullPointerException.class)
    public void testRrefrshTokenKeyCreationWithNullTokenCacheItem() {
        TokenCacheKey.extractKeyForRT(null);
    }

    /**
     * Test token cache key is correctly constructed. Key is consisted of authority$client-id$scopes$displayable$unique-id$home-oid$policy
     * scopes are delimited by spaces. If the part constructing key is empty, empty string will be there as value
     */
    @Test
    public void testAccessTokenKeyCreationNoUser() {
        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, getScopes(), null, POLICY);
        Assert.assertTrue(accessTokenCacheKey.toString().equals(AUTHORITY + "$" + CLIENT_ID + "$" + "scope1 scope2$" + "$$$" + POLICY));
    }

    /**
     * Policy is not provided.
     */
    @Test
    public void testAccessTokenKeyCreationNoUserNoPolicy() {
        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, getScopes(), null, "");
        Assert.assertTrue(accessTokenCacheKey.toString().equals(AUTHORITY + "$" + CLIENT_ID + "$" + "scope1 scope2$" + "$$$"));
    }

    @Test
    public void testAccessTokenKeyCreationWithEmptyUser() throws UnsupportedEncodingException, AuthenticationException {
        final String rawIdToken = AndroidTestUtil.createIdToken(AndroidTestUtil.AUDIENCE, AndroidTestUtil.ISSUER, AndroidTestUtil.NAME,
                "", "", "", AndroidTestUtil.TENANT_ID, AndroidTestUtil.VERSION, "");
        final IdToken idToken = new IdToken(rawIdToken);
        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, getScopes(), new User(idToken), POLICY);
        Assert.assertTrue(accessTokenCacheKey.toString().equals(AUTHORITY + "$" + CLIENT_ID + "$" + "scope1 scope2$" + "$$$" + POLICY));
    }

    @Test
    public void testAccessTokenKeyCreationWithUser() throws UnsupportedEncodingException, AuthenticationException {
        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, getScopes(), getUser(), POLICY);
        Assert.assertTrue(accessTokenCacheKey.toString().equals(AUTHORITY + "$" + CLIENT_ID + "$" + "scope1 scope2$" + DISPLAYABLE + "$"
                + UNIQUE_ID + "$" + HOME_OBJECT_ID + "$" + POLICY));
    }

    @Test
    public void testAccessTokenKeyWithDifferentOrderOfScopes() throws UnsupportedEncodingException, AuthenticationException {
        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, getScopes(), getUser(), POLICY);

        final Set<String> scopesInDifferentOrder = new HashSet<>();
        scopesInDifferentOrder.add("scope2");
        scopesInDifferentOrder.add("scope1");
        final TokenCacheKey cacheKeyWithScopesInDifferentOrder = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID,
                scopesInDifferentOrder, getUser(), POLICY);

        Assert.assertTrue(accessTokenCacheKey.toString().equals(cacheKeyWithScopesInDifferentOrder.toString()));
    }

    @Test
    public void testAccessTokenKeyCreationSavedWithLowerCase() throws UnsupportedEncodingException, AuthenticationException {
        final String rawIdToken = AndroidTestUtil.getRawIdToken(DISPLAYABLE.toUpperCase(Locale.US), UNIQUE_ID.toUpperCase(Locale.US),
                HOME_OBJECT_ID.toUpperCase(Locale.US));
        final User user = new User(new IdToken(rawIdToken));

        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY.toUpperCase(Locale.US), CLIENT_ID.toUpperCase(Locale.US), getScopes(),
                user, POLICY.toUpperCase(Locale.US));

        Assert.assertTrue(accessTokenCacheKey.toString().equals(AUTHORITY + "$" + CLIENT_ID + "$" + "scope1 scope2$" + DISPLAYABLE + "$"
                + UNIQUE_ID + "$" + HOME_OBJECT_ID + "$" + POLICY));
    }

    @Test
    public void testRefreshTokenKeyCreation() throws UnsupportedEncodingException, AuthenticationException {
        final TokenCacheKey refreshTokenCacheKey = TokenCacheKey.createKeyForRT(CLIENT_ID, getUser(), POLICY);
        Assert.assertTrue(refreshTokenCacheKey.toString().equals("$" + CLIENT_ID + "$" + "$" + DISPLAYABLE + "$"
                + UNIQUE_ID + "$" + HOME_OBJECT_ID + "$" + POLICY));
    }

    @Test
    public void testRefreshTokenKeyExtractionFromTokenCacheItem() throws AuthenticationException, UnsupportedEncodingException {
        final String idToken = AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", UNIQUE_ID, DISPLAYABLE, "sub", "tenant",
                "version", HOME_OBJECT_ID);
        final TokenResponse response = new TokenResponse("access_token", idToken, "refresh_token", new Date(), new Date(), new Date(),
                MSALUtils.convertSetToString(getScopes(), " "), "Bearer", null);
        final BaseTokenCacheItem item = new RefreshTokenCacheItem(AUTHORITY, CLIENT_ID, POLICY, new SuccessTokenResponse(response));


        Assert.assertTrue(TokenCacheKey.extractKeyForRT(item).toString().equals("" + "$" + CLIENT_ID + "$" + "$" + DISPLAYABLE + "$"
                + UNIQUE_ID + "$" + HOME_OBJECT_ID + "$" + POLICY));
    }

    private Set<String> getScopes() {
        final Set<String> scopes = new HashSet<>();
        scopes.add("scope1");
        scopes.add("scope2");

        return scopes;
    }

    private User getUser() throws UnsupportedEncodingException, AuthenticationException {
        final String rawIdToken = AndroidTestUtil.getRawIdToken(DISPLAYABLE, UNIQUE_ID, HOME_OBJECT_ID);
        final IdToken idToken = new IdToken(rawIdToken);

        return new User(idToken);
    }
}
