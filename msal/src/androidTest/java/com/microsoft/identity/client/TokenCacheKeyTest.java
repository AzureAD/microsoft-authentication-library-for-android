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
    private static final String DISPLAYABLE = "test@contoso.onmicrosoft.com";
    private static final String UNIQUE_ID = "some-unique-id";
    private static final String HOME_OBJECT_ID = "some-home-oid";

    // TODO: add ignore for now. If everybody agrees on not putting authority as the key for rt entry, remove the test.
    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void testAccessTokenKeyCreationNoAuthority() throws MsalException {
        TokenCacheKey.createKeyForAT(null, CLIENT_ID, getScopes(), getUser());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAccessTokenKeyCreationNoClientId() throws MsalException {
        TokenCacheKey.createKeyForAT(AUTHORITY, null, getScopes(), getUser());
    }

    @Test
    public void testAccessTokenKeyCreationWithEmptyUser() throws MsalException {
        final String rawIdToken = AndroidTestUtil.createIdToken(AndroidTestUtil.AUDIENCE, AndroidTestUtil.ISSUER, AndroidTestUtil.NAME,
                "", "", "", AndroidTestUtil.TENANT_ID, AndroidTestUtil.VERSION, "");
        final IdToken idToken = new IdToken(rawIdToken);
        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, getScopes(), new User(idToken));
        Assert.assertTrue(accessTokenCacheKey.toString().equals(MSALUtils.base64EncodeToString(AUTHORITY) + "$" + MSALUtils.base64EncodeToString(CLIENT_ID) + "$"
                + MSALUtils.base64EncodeToString("scope1 scope2") + "$"));
    }

    @Test
    public void testAccessTokenKeyCreationWithUser() throws MsalException {
        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, getScopes(), getUser());
        Assert.assertTrue(accessTokenCacheKey.toString().equals(MSALUtils.base64EncodeToString(AUTHORITY) + "$" + MSALUtils.base64EncodeToString(CLIENT_ID) + "$" + MSALUtils.base64EncodeToString("scope1 scope2") + "$" + MSALUtils.base64EncodeToString(HOME_OBJECT_ID)));
    }

    @Test
    public void testAccessTokenKeyWithDifferentOrderOfScopes() throws MsalException {
        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID, getScopes(), getUser());

        final Set<String> scopesInDifferentOrder = new HashSet<>();
        scopesInDifferentOrder.add("scope2");
        scopesInDifferentOrder.add("scope1");
        final TokenCacheKey cacheKeyWithScopesInDifferentOrder = TokenCacheKey.createKeyForAT(AUTHORITY, CLIENT_ID,
                scopesInDifferentOrder, getUser());

        Assert.assertTrue(accessTokenCacheKey.toString().equals(cacheKeyWithScopesInDifferentOrder.toString()));
    }

    @Test
    public void testAccessTokenKeyCreationSavedWithLowerCase() throws MsalException {
        final String rawIdToken = AndroidTestUtil.getRawIdToken(DISPLAYABLE.toUpperCase(Locale.US), UNIQUE_ID.toUpperCase(Locale.US),
                HOME_OBJECT_ID.toUpperCase(Locale.US));
        final User user = new User(new IdToken(rawIdToken));

        final TokenCacheKey accessTokenCacheKey = TokenCacheKey.createKeyForAT(AUTHORITY.toUpperCase(Locale.US), CLIENT_ID.toUpperCase(Locale.US), getScopes(),
                user);

        Assert.assertTrue(accessTokenCacheKey.toString().equals(MSALUtils.base64EncodeToString(AUTHORITY) + "$" + MSALUtils.base64EncodeToString(CLIENT_ID) + "$"
                + MSALUtils.base64EncodeToString("scope1 scope2") + "$" + MSALUtils.base64EncodeToString(HOME_OBJECT_ID)));
    }

    @Test
    public void testRefreshTokenKeyCreation() throws MsalException {
        final TokenCacheKey refreshTokenCacheKey = TokenCacheKey.createKeyForRT(CLIENT_ID, getUser());
        Assert.assertTrue(refreshTokenCacheKey.toString().equals("$" + MSALUtils.base64EncodeToString(CLIENT_ID) + "$" + "$" + MSALUtils.base64EncodeToString(HOME_OBJECT_ID)));
    }

    @Test
    public void testRefreshTokenKeyExtractionFromTokenCacheItem() throws MsalException {
        final String idToken = AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", UNIQUE_ID, DISPLAYABLE, "sub", "tenant",
                "version", HOME_OBJECT_ID);
        final TokenResponse response = new TokenResponse("access_token", idToken, "refresh_token", new Date(), new Date(), new Date(),
                MSALUtils.convertSetToString(getScopes(), " "), "Bearer");
        final RefreshTokenCacheItem item = new RefreshTokenCacheItem(CLIENT_ID, response);


        Assert.assertTrue(item.extractTokenCacheKey().toString().equals("" + "$" + MSALUtils.base64EncodeToString(CLIENT_ID) + "$" + "$"
                + MSALUtils.base64EncodeToString(HOME_OBJECT_ID)));
    }

    private Set<String> getScopes() {
        final Set<String> scopes = new HashSet<>();
        scopes.add("scope1");
        scopes.add("scope2");

        return scopes;
    }

    private User getUser() throws MsalException {
        final String rawIdToken = AndroidTestUtil.getRawIdToken(DISPLAYABLE, UNIQUE_ID, HOME_OBJECT_ID);
        final IdToken idToken = new IdToken(rawIdToken);

        return new User(idToken);
    }
}
