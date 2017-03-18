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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Object used to group the all the conditions to lookup into token cache.
 */
final class TokenCacheKey {
    private final String mAuthority;
    private final String mClientId;
    private final TreeSet<String> mScope = new TreeSet<>();
    private final String mHomeObjectId;

    private TokenCacheKey(final String authority, final String clientId, final Set<String> scope, final User user) {
        this(authority, clientId, scope, user.getHomeObjectId());
    }

    private TokenCacheKey(final String authority, final String clientId, final Set<String> scope, final String homeObjectId) {
        // All the tokens issued by AAD is cross tenant, ADFS 2016 should work the same as AAD, and client id.
        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("clientId");
        }

        mAuthority = MsalUtils.isEmpty(authority) ? "" : authority.toLowerCase(Locale.US);
        mClientId = clientId.toLowerCase(Locale.US);

        // guarantee the order in the serialized string
        mScope.addAll(scope);

        mHomeObjectId = MsalUtils.isEmpty(homeObjectId) ? "" : homeObjectId.toLowerCase(Locale.US);
    }

    static TokenCacheKey createKeyForAT(final String authority, final String clientId, final Set<String> scopes, final User user) {
        return new TokenCacheKey(authority, clientId, scopes, user);
    }

    // RT entry doesn't contain scope.
    static TokenCacheKey createKeyForRT(final String clientId, final User user) {
        return new TokenCacheKey("", clientId, new HashSet<String>(), user);
    }

    Set<String> getScope() {
        return Collections.unmodifiableSet(mScope);
    }

    /**
     * {@inheritDoc}
     * Cache key will be delimited by $, each individual attribute put on the cachekey will be base64 encoded.
     */
    @Override
    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MsalUtils.base64EncodeToString(mAuthority) + "$");
        stringBuilder.append(MsalUtils.base64EncodeToString(mClientId) + "$");
        // scope is treeSet to guarantee the order of the scopes when converting to string.
        stringBuilder.append(MsalUtils.base64EncodeToString(MsalUtils.convertSetToString(mScope, " ")) + "$");
        stringBuilder.append(MsalUtils.base64EncodeToString(mHomeObjectId));

        return stringBuilder.toString();
    }

    /**
     * For access token cache item match, scope in the items needs to contain all the scope in the lookup
     * key, if user is passed in, user needs to be match.
     * For refresh token cache item, every RT is multi-scope, no need to check for the scope intersection.
     */
     boolean matches(final BaseTokenCacheItem item) {
        return mClientId.equalsIgnoreCase(item.getClientId())
                && (MsalUtils.isEmpty(mAuthority) || mAuthority.equalsIgnoreCase(item.getAuthority()))
                && mHomeObjectId.equalsIgnoreCase(item.getHomeObjectId());
    }
}
