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

import java.util.Date;
import java.util.Set;

/**
 * MSAL successful authentication result. When auth succeeds, token will be wrapped into the
 * {@link AuthenticationResult} and passed back through the {@link AuthenticationCallback}.
 */
public final class AuthenticationResult {

    private final AccessTokenCacheItem mAccessTokenCacheItem;
    private final User mUser;
    private final String mTenantId;
    private final String mRawIdToken;
    private final String mUniqueId;

    AuthenticationResult(final AccessTokenCacheItem accessTokenCacheItem) throws MsalClientException {
        mAccessTokenCacheItem = accessTokenCacheItem;
        mUser = accessTokenCacheItem.getUser();
        mRawIdToken = accessTokenCacheItem.getRawIdToken();
        final IdToken idToken = accessTokenCacheItem.getIdToken();
        if (idToken != null) {
            mTenantId = idToken.getTenantId();
            mUniqueId = idToken.getUniqueId();
        } else {
            mTenantId = "";
            mUniqueId = "";
        }
    }

    /**
     * @return The access token requested.
     */
    public String getAccessToken() {
        return mAccessTokenCacheItem.getAccessToken();
    }

    /**
     * @return The expiration time of the access token returned in the Token property.
     * This value is calculated based on current UTC time measured locally and the value expiresIn returned from the
     * service.
     */
    public Date getExpiresOn() {
        return mAccessTokenCacheItem.getExpiresOn();
    }

    /**
     * @return A unique tenant identifier that was used in token acquisiton. Could be null if tenant information is not
     * returned by the service.
     */
    public String getTenantId() {
        return mTenantId;
    }

    /**
     * @return The unique identifier of the user.
     */
    public String getUniqueId() {
        return mUniqueId;
    }

    /**
     * @return The id token returned by the service or null if no id token is returned.
     */
    public String getIdToken() {
        return mRawIdToken;
    }

    /**
     * @return The {@link User} that tokens were acquired. Some elements inside {@link User} could be null if not
     * returned by the service.
     */
    public User getUser() {
        return mUser;
    }

    /**
     * @return The scopes returned from the service.
     */
    public String[] getScope() {
        final Set<String> scopes = mAccessTokenCacheItem.getScope();
        return scopes.toArray(new String[scopes.size()]);
    }
}