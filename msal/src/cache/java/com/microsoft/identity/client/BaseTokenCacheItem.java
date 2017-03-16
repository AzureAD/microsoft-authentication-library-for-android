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

import java.util.HashSet;
import java.util.Set;

/**
 * MSAL Internal abstract class to represent the {@link BaseTokenCacheItem}.
 */
abstract class BaseTokenCacheItem {
    final String mAuthority;
    final String mClientId;
    final Set<String> mScope = new HashSet<>();
    String mUniqueId;
    String mHomeObjectId;
    String mDisplayableId;

    // excludes the field from being serialized
    transient String mTenantId;
    String mRawIdToken;

    /**
     * Constructor for creating the token cache item.
     */
    BaseTokenCacheItem(final String authority, final String clientId, final TokenResponse response) throws MsalClientException {
        if (!MsalUtils.isEmpty(response.getRawIdToken())) {
            final IdToken idToken = new IdToken(response.getRawIdToken());
            final User user = new User(idToken);
            mUniqueId = user.getUniqueId();
            mDisplayableId = user.getDisplayableId();
            mHomeObjectId = user.getHomeObjectId();
            mRawIdToken = response.getRawIdToken();
            mTenantId = idToken.getTenantId();
        }

        mAuthority = authority;
        mClientId = clientId;
        mScope.addAll(MsalUtils.getScopesAsSet(response.getScope()));
    }

    String getAuthority() {
        return mAuthority;
    }

    String getClientId() {
        return mClientId;
    }

    Set<String> getScope() {
        return mScope;
    }

    String getUniqueId() {
        return mUniqueId;
    }

    String getDisplayableId() {
        return mDisplayableId;
    }

    String getHomeObjectId() {
        return mHomeObjectId;
    }

    String getRawIdToken() {
        return mRawIdToken;
    }
}
