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

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * MSAL internal class for create access token cache key.
 */
final class AccessTokenCacheKey extends TokenCacheKey<AccessTokenCacheItem> {

    private final String mAuthority;
    private final TreeSet<String> mScope = new TreeSet<>();

    private AccessTokenCacheKey(final String authority, final String clientId, final Set<String> scope, final String uid, final String utid) {
        super(clientId, uid, utid);

        if (MSALUtils.isEmpty(authority)) {
            throw new IllegalArgumentException("authority");
        }
        mAuthority = authority.toLowerCase(Locale.US);
        mScope.addAll(scope);
    }

    static AccessTokenCacheKey createTokenCacheKey(final String authority, final String clientId, final Set<String> scopes, final User user) {
        if (user == null) {
            throw new IllegalArgumentException("null user");
        }

        return new AccessTokenCacheKey(authority, clientId, scopes, user.getUid(), user.getUtid());
    }

    Set<String> getScope() {
        return Collections.unmodifiableSet(mScope);
    }

    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MSALUtils.base64UrlEncodeToString(mAuthority) + "$");
        stringBuilder.append(MSALUtils.base64UrlEncodeToString(mClientId) + "$");
        // scope is treeSet to guarantee the order of the scopes when converting to string.
        stringBuilder.append(MSALUtils.base64UrlEncodeToString(MSALUtils.convertSetToString(mScope, " ")) + "$");
        stringBuilder.append(mUserIdentifier);

        return stringBuilder.toString();
    }

    @Override
    boolean matches(final AccessTokenCacheItem item) {
        return mAuthority.equalsIgnoreCase(item.getAuthority())
                && mClientId.equalsIgnoreCase(item.getClientId())
                && mUserIdentifier.equals(item.getUserIdentifier());
    }
}
