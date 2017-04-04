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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.Collection;
import java.util.Map;

/**
 * MSAL Internal class for access data storage for token read and write.
 */
final class TokenCacheAccessor {
    private static final String TAG = TokenCacheAccessor.class.getSimpleName();

    private static final String ACCESS_TOKEN_SHARED_PREFERENCE = "com.microsoft.identity.client.token";
    private static final String REFRESH_TOKEN_SHARED_PREFERENCE = "com.microsoft.identity.client.refreshToken";

    private final Context mContext;
    private final SharedPreferences mAccessTokenSharedPreference;
    private final SharedPreferences mRefreshTokenSharedPreference;

    /**
     * Constructor for {@link TokenCacheAccessor}. Access token and refresh token will be stored separately.
     * @param context
     */
    TokenCacheAccessor(final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context");
        }

        mContext = context;

        mAccessTokenSharedPreference = mContext.getSharedPreferences(ACCESS_TOKEN_SHARED_PREFERENCE,
                Activity.MODE_PRIVATE);
        mRefreshTokenSharedPreference = mContext.getSharedPreferences(REFRESH_TOKEN_SHARED_PREFERENCE,
                Activity.MODE_PRIVATE);

        if (mAccessTokenSharedPreference == null || mRefreshTokenSharedPreference == null) {
            throw new IllegalStateException("Fail to create SharedPreference");
        }
    }

    /**
     * When storing access token, the key needs to be a strict match.
     */
    void saveAccessToken(final String accessTokenCacheKey, final String accessTokenItem) {
        // there shouldn't be any case that this method is called with null/empty key or item
        if (MSALUtils.isEmpty(accessTokenCacheKey) || MSALUtils.isEmpty(accessTokenItem)) {
            throw new IllegalArgumentException("accessTokenCacheKey/accessTokenItem empty or null");
        }

        final Editor editor = mAccessTokenSharedPreference.edit();
        editor.putString(accessTokenCacheKey, accessTokenItem);
        editor.apply();

        Logger.verbose(TAG, null, "Access token is saved into cache.");
        Logger.verbosePII(TAG, null, "Access token is saved with key: " + accessTokenCacheKey);
    }

    /**
     * Save the refresh token item.
     */
    void saveRefreshToken(final String refreshTokenCacheKey, final String refreshTokenItem) {
        // there shouldn't be any case that this method is called with null/empty key or item
        if (MSALUtils.isEmpty(refreshTokenCacheKey) || MSALUtils.isEmpty(refreshTokenItem)) {
            throw new IllegalArgumentException("refreshTokenCacheKey/refreshTokenItem empty or null");
        }

        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.putString(refreshTokenCacheKey, refreshTokenItem);
        editor.apply();

        Logger.verbose(TAG, null, "Refresh token is successfully saved into cache.");
        Logger.verbosePII(TAG, null, "Refresh token is saved with key: " + refreshTokenCacheKey);
    }

    void deleteAccessToken(final String accessTokenKey) {
        final Editor editor = mAccessTokenSharedPreference.edit();
        editor.remove(accessTokenKey);
        editor.apply();
    }

    /**
     * Delete the refresh token item.
     * @param refreshTokenCacheKey The string value of the refresh token cache item key to remove.
     */
    void deleteRefreshToken(final String refreshTokenCacheKey) {
        Logger.verbose(TAG, null, "Remove the given refresh token item.");
        Logger.verbosePII(TAG, null, "Refresh token is deleted with key: " + refreshTokenCacheKey);

        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.remove(refreshTokenCacheKey);
        editor.apply();
    }

    /**
     * @return Immutable List of all the {@link AccessTokenCacheItem}s.
     */
    Collection<String> getAllAccessTokens() {
        final Map<String, String> allAT = (Map<String, String>) mAccessTokenSharedPreference.getAll();
        return allAT.values();
    }

    /**
     * @return Immutable List of all the {@link RefreshTokenCacheItem}s.
     */
    Collection<String> getAllRefreshTokens() {
        final Map<String, String> allRTs = (Map<String, String>) mRefreshTokenSharedPreference.getAll();
        return allRTs.values();
    }
}
