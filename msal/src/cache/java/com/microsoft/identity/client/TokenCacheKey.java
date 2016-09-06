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
    // scope is treeSet to guarantee the order of the scopes when converting to string.
    private final Set<String> mScope = new TreeSet<>();
    private final String mDisplayableId;
    private final String mUniqueId;
    private final String mHomeObjectId;
    private final String mPolicy;

    private TokenCacheKey(final String authority, final String clientId, final Set<String> scope, final User user, final String policy) {
        this(authority, clientId, scope, user == null ? "" : user.getDisplayableId(),
                user == null ? "" : user.getUniqueId(), user == null ? "" : user.getHomeObjectId(), policy);
    }

    private TokenCacheKey(final String authority, final String clientId, final Set<String> scope, final String displayableId,
                  final String uniqueId, final String homeObjectId, final String policy) {
        // TODO: remove the authority from cache key for refresh token entry. Discuss with the team on whether we need authority as the key.
        // All the tokens issued by AAD is cross tenant, ADFS 2016 should work the same as AAD, and client id.
//        if (MSALUtils.isEmpty(authority)) {
//            throw new IllegalArgumentException("authority");
//        }

        if (MSALUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("clientid");
        }

        mAuthority = MSALUtils.isEmpty(authority) ? "" : authority.toLowerCase(Locale.US);
        mClientId = clientId.toLowerCase(Locale.US);

        // guarantee the order in the serialized string
        mScope.addAll(scope);

        mDisplayableId = MSALUtils.isEmpty(displayableId) ? "" : displayableId.toLowerCase(Locale.US);
        mUniqueId = MSALUtils.isEmpty(uniqueId) ? "" : uniqueId.toLowerCase(Locale.US);
        mHomeObjectId = MSALUtils.isEmpty(homeObjectId) ? "" : homeObjectId.toLowerCase(Locale.US);
        mPolicy = MSALUtils.isEmpty(policy) ? "" : policy.toLowerCase(Locale.US);
    }

    static TokenCacheKey createKeyForAT(final String authority, final String clientId, final Set<String> scopes, final User user, final String policy) {
        return new TokenCacheKey(authority, clientId, scopes, user, policy);
    }

    // RT entry doesn't contain scope.
    static TokenCacheKey createKeyForRT(final String clientId, final User user, final String policy) {
        return new TokenCacheKey("", clientId, new HashSet<String>(), user, policy);
    }

    static TokenCacheKey extractKeyForAT(final TokenCacheItem tokenCacheItem) {
        if (tokenCacheItem == null) {
            throw new NullPointerException("token cache item is null");
        }

        return new TokenCacheKey(tokenCacheItem.getAuthority(), tokenCacheItem.getClientId(), tokenCacheItem.getScope(), tokenCacheItem.getDisplayableId(),
                tokenCacheItem.getUniqueId(), tokenCacheItem.getHomeObjectId(), tokenCacheItem.getPolicy());
    }

    static TokenCacheKey extractKeyForRT(final TokenCacheItem tokenCacheItem) {
        if(tokenCacheItem == null) {
            throw new NullPointerException("tokencacheItem null");
        }

        return new TokenCacheKey("", tokenCacheItem.getClientId(), new HashSet<String>(), tokenCacheItem.getDisplayableId(),
                tokenCacheItem.getUniqueId(), tokenCacheItem.getHomeObjectId(), tokenCacheItem.getPolicy());
    }

    Set<String> getScope() {
        return Collections.unmodifiableSet(mScope);
    }

    @Override
     public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mAuthority + "$");
        stringBuilder.append(mClientId + "$");
        stringBuilder.append(MSALUtils.convertSetToString(mScope, " ") + "$");
        stringBuilder.append(mDisplayableId + "$");
        stringBuilder.append(mUniqueId + "$");
        stringBuilder.append(mHomeObjectId + "$");
        stringBuilder.append(mPolicy);

        return stringBuilder.toString();
    }

    /**
     * For access token cache item match, scope in the items needs to contain all the scope in the lookup
     * key, if user is passed in, user needs to be match.
     * For refresh token cache item, every RT is multi-scope, no need to check for the scope intersection.
     */
     boolean matches(final TokenCacheItem item) {
        return mClientId.equalsIgnoreCase(item.getClientId())
                && (MSALUtils.isEmpty(mAuthority) || mAuthority.equalsIgnoreCase(item.getAuthority()))
                && (MSALUtils.isEmpty(mUniqueId) || mUniqueId.equalsIgnoreCase(item.getUniqueId()))
                && (MSALUtils.isEmpty(mDisplayableId) || mDisplayableId.equalsIgnoreCase(item.getDisplayableId()))
                && (MSALUtils.isEmpty(mHomeObjectId) || mHomeObjectId.equalsIgnoreCase(item.getHomeObjectId()))
                && isPolicyMatch(item);
    }

    boolean isScopeEqual(final TokenCacheItem item) {
        return mScope.size() == item.getScope().size() && mScope.containsAll(item.getScope());
    }

    private boolean isPolicyMatch(final TokenCacheItem item) {
        if (MSALUtils.isEmpty(mPolicy)) {
            return MSALUtils.isEmpty(item.getPolicy());
        }

        return mPolicy.equalsIgnoreCase(item.getPolicy());
    }
}
