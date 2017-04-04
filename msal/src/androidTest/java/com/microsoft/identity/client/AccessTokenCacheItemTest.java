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
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Tests for {@link AccessTokenCacheItem} and {@link RefreshTokenCacheItem}.
 */
public final class AccessTokenCacheItemTest {
    private static final String AUTHORITY = "https://login.microsoftonline.com/common";
    private static final String AUTHORITY_HOST = "http://login.microsoftonline.com";
    private static final String CLIENT_ID = "some-client-id";
    private static final String DISPLAYABLE = "test@contoso.onmicrosoft.com";
    private static final String UNIQUE_ID = "some-unique-id";
    private static final String HOME_OBJECT_ID = "some-home-oid";
    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String SCOPE_1 = "scope1";
    private static final String SCOPE_2 = "scope2";
    private static final String TENANT_ID = "tenant";

    @Test
    public void testAccessTokenItemCreation() throws MsalClientException, MsalServiceException {
        final AccessTokenCacheItem item = new AccessTokenCacheItem(AUTHORITY, CLIENT_ID, getTokenResponse(ACCESS_TOKEN, ""));
        Assert.assertTrue(item instanceof AccessTokenCacheItem);
        Assert.assertTrue(item.getAuthority().equals(AUTHORITY));
        Assert.assertTrue(item.getClientId().equals(CLIENT_ID));

        final Set<String> scopes = item.getScope();
        Assert.assertTrue(scopes.size() == 2);
        Assert.assertTrue(scopes.contains(SCOPE_1));
        Assert.assertTrue(scopes.contains(SCOPE_2));

        Assert.assertTrue(item.getRawClientInfo().equals(AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID)));
        Assert.assertTrue(item.getUserIdentifier().equalsIgnoreCase(MSALUtils.getUniqueUserIdentifier(AndroidTestUtil.UID, AndroidTestUtil.UTID)));
        Assert.assertTrue(item.getTenantId().equals(TENANT_ID));
        Assert.assertTrue(item.getAccessToken().equals(ACCESS_TOKEN));
    }

    @Test
    public void testRefreshTokenCreation() throws MsalClientException, MsalServiceException {
        final RefreshTokenCacheItem item = new RefreshTokenCacheItem(AUTHORITY_HOST, CLIENT_ID, getTokenResponse("", REFRESH_TOKEN));
        Assert.assertTrue(item.getRefreshToken().equals(REFRESH_TOKEN));
    }

    static Set<String> getScopes() {
        final Set<String> scopes = new HashSet<>();
        scopes.add(SCOPE_1);
        scopes.add(SCOPE_2);

        return scopes;
    }

    static TokenResponse getTokenResponse(final String accessToken, final String refreshToken) {
        return new TokenResponse(accessToken, getIdToken(), refreshToken, new Date(), new Date(), new Date(),
                MSALUtils.convertSetToString(getScopes(), " "), "Bearer", AndroidTestUtil.createRawClientInfo(AndroidTestUtil.UID, AndroidTestUtil.UTID));
    }

    static String getIdToken() {
        return AndroidTestUtil.createIdToken(AUTHORITY, "issuer", "test user", UNIQUE_ID, DISPLAYABLE, "sub", TENANT_ID,
                "version");
    }
}
