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

import java.util.HashSet;
import java.util.Set;

/**
 * MSAL Internal abstract class to represent the {@link BaseTokenCacheItem}.
 */
abstract class BaseTokenCacheItem {

    @SerializedName("client_id")
    final String mClientId;

    @SerializedName("id_token")
    String mRawIdToken;

    // excludes the field from being serialized
    transient User mUser;
    transient IdToken mIdToken;

    /**
     * @return {@link TokenCacheKey} for the given token item.
     */
    abstract TokenCacheKey extractTokenCacheKey();

    /**
     * Constructor for creating the token cache item.
     */
    BaseTokenCacheItem(final String clientId, final TokenResponse response)
            throws AuthenticationException {
        if (!MSALUtils.isEmpty(response.getRawIdToken())) {
            mRawIdToken = response.getRawIdToken();
            mIdToken = new IdToken(mRawIdToken);
            mUser = new User(mIdToken);
        }

        mClientId = clientId;
    }

    String getClientId() {
        return mClientId;
    }

    String getUniqueId() {
        return mUser != null ? mUser.getUniqueId() : "";
    }

    String getDisplayableId() {
        return mUser != null ? mUser.getDisplayableId() : "";
    }

    String getHomeObjectId() {
        return mUser != null? mUser.getHomeObjectId() : "";
    }

    void setIdToken(final IdToken idToken) {
        mIdToken = idToken;
    }

    String getRawIdToken() {
        return mRawIdToken;
    }

    void setUser(final User user) {
        mUser = user;
    }

    String getAuthority() {
        return "";
    }
}
