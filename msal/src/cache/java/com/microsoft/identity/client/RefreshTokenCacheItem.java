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

/**
 * MSAL internal class for representing an refresh token item.
 */
final class RefreshTokenCacheItem extends BaseTokenCacheItem {

    @SerializedName("refresh_token")
    private final String mRefreshToken;

    @SerializedName("environment")
    private final String mEnvironment;

    // For meta data versio
    @SerializedName("displayable_id")
    final String mDisplayableId;

    @SerializedName("name")
    final String mName;

    @SerializedName("identity_provider")
    final String mIdentityProvider;

    // uid and utid will help to uniquely identify user across tenants. For b2c, current design is that users are never
    // in more than one tenant, client_info may not be returned at all. When it's not returned, we need id token for the
    // fallback logic.
    @SerializedName("uid")
    final String mUid;

    @SerializedName("utid")
    final String mUtid;

    RefreshTokenCacheItem(final String environment, final String clientId, final TokenResponse response)
            throws MsalClientException {
        super(clientId);
        mEnvironment = environment;
        mRefreshToken = response.getRefreshToken();

        final IdToken idToken = new IdToken(response.getRawIdToken());
        mDisplayableId = idToken.getPreferredName();
        mName = idToken.getName();
        mIdentityProvider = idToken.getIssuer();

        if (!MSALUtils.isEmpty(response.getRawClientInfo())) {
            final ClientInfo clientInfo = new ClientInfo(response.getRawClientInfo());
            mUid = clientInfo.getUniqueIdentifier();
            mUtid = clientInfo.getUniqueTenantIdentifier();
        } else {
            mUid = idToken.getUniqueId();
            mUtid = idToken.getTenantId();
        }

        mUser = new User(mDisplayableId, mName, mIdentityProvider, mUid, mUtid);
    }

    @Override
    RefreshTokenCacheKey extractTokenCacheKey() {
        return RefreshTokenCacheKey.createTokenCacheKey(mEnvironment, mClientId, mUser);
    }

    @Override
    String getUserIdentifier() {
        return MSALUtils.getUniqueUserIdentifier(mUid, mUtid);
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

    String getUid() {
        return mUid;
    }

    String getUtid() {
        return mUtid;
    }
}
