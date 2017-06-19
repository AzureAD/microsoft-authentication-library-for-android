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
 * MSAL Internal abstract class to represent the {@link BaseTokenCacheItem}.
 */
abstract class BaseTokenCacheItem {

    @SerializedName("client_id")
    final String mClientId;

    @SerializedName("client_info")
    final String mRawClientInfo;

    @SerializedName("ver")
    private final String mVersion = "1";

    transient User mUser;
    transient ClientInfo mClientInfo;

    /**
     * @return {@link TokenCacheKey} for the given token item.
     */
    abstract TokenCacheKey extractTokenCacheKey();

    /**
     * No args constructor for use ill serialization for Gson to prevent usage of sun.misc.Unsafe
     */
    @SuppressWarnings("unused")
    BaseTokenCacheItem() throws MsalClientException {
        this(null, null);
    }

    /**
     * Constructor for creating the token cache item.
     */
    BaseTokenCacheItem(final String clientId, final String rawClientInfo) throws MsalClientException {
        mClientId = clientId;
        mRawClientInfo = rawClientInfo;
        mClientInfo = new ClientInfo(rawClientInfo);
    }

    String getClientId() {
        return mClientId;
    }

    String getRawClientInfo() {
        return mRawClientInfo;
    }

    ClientInfo getClientInfo() {
        return mClientInfo;
    }

    void setClientInfo(final ClientInfo clientInfo) {
        mClientInfo = clientInfo;
    }

    User getUser() {
        return mUser;
    }

    void setUser(final User user) {
        mUser = user;
    }

    String getVersion() {
        return mVersion;
    }

    final String getUserIdentifier() {
        return MsalUtils.getUniqueUserIdentifier(mClientInfo.getUniqueIdentifier(), mClientInfo.getUniqueTenantIdentifier());
    }
}
