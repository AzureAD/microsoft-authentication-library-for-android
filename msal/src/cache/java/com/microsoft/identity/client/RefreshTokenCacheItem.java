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
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.RefreshToken;

/**
 * MSAL internal class for representing an refresh token item.
 */
final class RefreshTokenCacheItem extends BaseTokenCacheItem {

    @SerializedName("refresh_token")
    private String mRefreshToken;

    @SerializedName("environment")
    private String mEnvironment;

    // meta data used to construct user object from refresh token cache item.
    @SerializedName("displayable_id")
    String mDisplayableId;

    @SerializedName("name")
    String mName;

    @SerializedName("identity_provider")
    String mIdentityProvider;

    /**
     * No args constructor for use in serialization for Gson to prevent usage of sun.misc.Unsafe.
     */
    @SuppressWarnings("unused")
    RefreshTokenCacheItem() {
    }

    RefreshTokenCacheItem(final RefreshToken refreshToken,
                          final Account account,
                          final com.microsoft.identity.common.internal.dto.IdToken idToken) {
        super(refreshToken.getClientId(), refreshToken.getHomeAccountId(), null);
        mRefreshToken = refreshToken.getSecret();
        mEnvironment = refreshToken.getEnvironment();
        mDisplayableId = account.getUsername();
        mName = account.getName();
        mIdentityProvider = idToken.getAuthority();
    }

    RefreshTokenCacheItem(final String environment, final String clientId, final TokenResponse response)
            throws MsalClientException {
        super(clientId, response.getRawClientInfo());
        mEnvironment = environment;
        mRefreshToken = response.getRefreshToken();

        final IdToken idToken = new IdToken(response.getRawIdToken());
        mDisplayableId = idToken.getPreferredName();
        mName = idToken.getName();
        mIdentityProvider = idToken.getIssuer();

        mUser = new User(mDisplayableId, mName, mIdentityProvider, getClientInfo().getUniqueIdentifier(),
                getClientInfo().getUniqueTenantIdentifier());
    }

    @Override
    RefreshTokenCacheKey extractTokenCacheKey() {
        return RefreshTokenCacheKey.createTokenCacheKey(mEnvironment, mClientId, mUser);
    }

    String getRefreshToken() {
        return mRefreshToken;
    }

    String getEnvironment() {
        return mEnvironment;
    }

    String getDisplayableId() {
        return mDisplayableId;
    }

    String getName() {
        return mName;
    }

    String getIdentityProvider() {
        return mIdentityProvider;
    }
}
