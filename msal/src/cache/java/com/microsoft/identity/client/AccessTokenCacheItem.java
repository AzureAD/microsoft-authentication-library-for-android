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

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * MSAL internal class for representing the access token cache item.
 */
final class AccessTokenCacheItem extends BaseTokenCacheItem {

    @SerializedName("authority")
    final String mAuthority;

    @SerializedName("access_token")
    private String mAccessToken;

    @SerializedName("expires_on")
    private Date mExpiresOn;

    @SerializedName("scope")
    final String mScope;

    @SerializedName("token_type")
    final String mTokenType;

    /**
     * Constructor for creating the {@link AccessTokenCacheItem}.
     */
    AccessTokenCacheItem(final String authority, final String clientId, final TokenResponse response) throws AuthenticationException {
        super(clientId, response);

        mAuthority = authority;
        mAccessToken = response.getAccessToken();
        mExpiresOn = response.getExpiresOn();
        mScope = response.getScope();
        mTokenType = response.getTokenType();
    }

    /**
     * {@inheritDoc}
     */
    TokenCacheKey extractTokenCacheKey() {
        return TokenCacheKey.createKeyForAT(mAuthority, mClientId, MSALUtils.getScopesAsSet(mScope), mUser);
    }

    /**
     * @return The authority for the request.
     */
    String getAuthority() {
        return mAuthority;
    }

    /**
     * @return The access token returned in the token respone.
     */
    String getAccessToken() {
        return mAccessToken;
    }

    /**
     * @return The access token expires on.
     */
    Date getExpiresOn() {
        return mExpiresOn;
    }

    /**
     * @return The tenant id.
     */
    String getTenantId() {
        return mIdToken != null ? mIdToken.getTenantId() : "";
    }

    /**
     * @return Scopes in the format of set.
     */
    Set<String> getScope() {
        return MSALUtils.getScopesAsSet(mScope);
    }

    /**
     * @return The token type, i.e Bearer.
     */
    String getTokenType() {
        return mTokenType;
    }
}
