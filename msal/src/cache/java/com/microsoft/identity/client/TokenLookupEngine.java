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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * MSAL internal class for handling key construction, cache lookup.
 */
final class TokenLookupEngine {
    private static final String TAG = TokenLookupEngine.class.getSimpleName();
    private static final int DEFAULT_EXPIRATION_BUFFER = 300;

    private final TokenCache mTokenCache;
    private final AuthenticationRequestParameters mAuthRequestParams;
    private final User mUser;

    TokenLookupEngine(final AuthenticationRequestParameters authRequestParams, final User user) {
        mTokenCache = authRequestParams.getTokenCache();
        mAuthRequestParams = authRequestParams;
        mUser = user;
    }

    AccessTokenCacheItem getAccessToken() throws AuthenticationException {
        final TokenCacheKey key = TokenCacheKey.createKeyForAT(mAuthRequestParams.getAuthority().getAuthorityUrl(),
                mAuthRequestParams.getClientId(), mAuthRequestParams.getScope(), mUser, mAuthRequestParams.getPolicy());
        final List<AccessTokenCacheItem> accessTokenCacheItems = mTokenCache.getAccessTokenItem(key);

        if (accessTokenCacheItems.isEmpty()) {
            // TODO: log access token not found
            return null;
        }

        // TODO: If user is not provided for silent request, and there is only one item found in the cache. Should we return it?
        if (accessTokenCacheItems.size() > 1) {
            // TODO: log there are multiple access tokens found, don't know which one to use.
            return null;
        }

        // Access token lookup needs to be a strict match. In the JSON response from token endpoint, server only returns the scope
        // the developer requires the token for. We store the token separately for considerations i.e. MFA.
        final AccessTokenCacheItem accessTokenCacheItem = accessTokenCacheItems.get(0);
        if (!isExpired(accessTokenCacheItem.getExpiresOn())) {
            return accessTokenCacheItem;
        }

        //TODO: log the access token found is expired.
        return null;
    }

    // All the token AAD returns are multi-scopes. MSAL only support ADFS 2016, which issues multi-scope RT.
    RefreshTokenCacheItem getRefreshToken() throws AuthenticationException {
        final TokenCacheKey key = TokenCacheKey.createKeyForRT(mAuthRequestParams.getClientId(), mUser, mAuthRequestParams.getPolicy());
        final List<RefreshTokenCacheItem> refreshTokenCacheItems = mTokenCache.getRefreshTokenItem(key);

        if (refreshTokenCacheItems.size() == 0) {
            // TODO: no RT returned
            return null;
        }

        // User info already provided, if there are multiple items found will throw since we don't what
        // is the one we should use.
        if (refreshTokenCacheItems.size() > 1) {
            throw new AuthenticationException(MSALError.MULTIPLE_CACHE_ENTRY_FOUND);
        }

        return refreshTokenCacheItems.get(0);
    }

    private boolean isExpired(final Date expiresOn) {
        final Calendar calendarWithBuffer = Calendar.getInstance();
        calendarWithBuffer.add(Calendar.SECOND, DEFAULT_EXPIRATION_BUFFER);
        final Date validity = calendarWithBuffer.getTime();
        //TODO: log
//        Logger.v(TAG, "expiresOn:" + expiresOn + " timeWithBuffer:" + calendarWithBuffer.getTime()
//                + " Buffer:" + AuthenticationSettings.INSTANCE.getExpirationBuffer());

        return expiresOn != null && expiresOn.before(validity);
    }
}
