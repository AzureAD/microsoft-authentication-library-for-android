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

/**
 * MSAL internal class for creating refresh token item cache key.
 */
final class RefreshTokenCacheKey extends TokenCacheKey<RefreshTokenCacheItem> {

    /**
     * The host of authority.
     */
    private final String mEnvironment;

    private RefreshTokenCacheKey(final String environment, final String clientId, final String uid, final String utid) {
        super(clientId, uid, utid);

        if (MsalUtils.isEmpty(environment)) {
            throw new IllegalArgumentException("environment");
        }
        mEnvironment = environment;
    }

    static RefreshTokenCacheKey createTokenCacheKey(final String environment, final String clientId, final User user) {
        return new RefreshTokenCacheKey(environment, clientId, user.getUid(), user.getUtid());
    }

    public String toString() {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MsalUtils.base64UrlEncodeToString(mEnvironment));
        stringBuilder.append(TOKEN_CACHE_KEY_DELIMITER);
        stringBuilder.append(MsalUtils.base64UrlEncodeToString(mClientId));
        stringBuilder.append(TOKEN_CACHE_KEY_DELIMITER);
        stringBuilder.append(mUserIdentifier);

        return stringBuilder.toString();
    }

    @Override
    public boolean matches(final RefreshTokenCacheItem item) {
        return mEnvironment.equalsIgnoreCase(item.getEnvironment())
                && mClientId.equalsIgnoreCase(item.getClientId())
                && mUserIdentifier.equals(item.getUserIdentifier());
    }
}
