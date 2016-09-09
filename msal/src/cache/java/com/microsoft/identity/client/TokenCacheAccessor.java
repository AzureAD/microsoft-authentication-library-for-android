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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * MSAL Internal class for access data storage for token read and write.
 */
final class TokenCacheAccessor {
    private static final String TAG = TokenCacheAccessor.class.getSimpleName();

    private static final String SHARED_PREFERENCE_NAME = "com.microsoft.identity.client";
    private static final String ACCESS_TOKEN_SUFFIX = ".accessToken";
    private static final String REFRESH_TOKEN_SUFFIX = ".refreshToken";

    private final Context mContext;
    private final SharedPreferences mAccessTokenSharedPreference;
    private final SharedPreferences mRefreshTokenSharedPreference;

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTimeAdapter())
            .create();

    /**
     * Constructor for {@link TokenCacheAccessor}. Access token and refresh token will be stored separately.
     * @param context
     */
    TokenCacheAccessor(final Context context) {
        if (context == null) {
            throw new IllegalArgumentException("context");
        }

        mContext = context;

        mAccessTokenSharedPreference = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME + ACCESS_TOKEN_SUFFIX,
                Activity.MODE_PRIVATE);
        mRefreshTokenSharedPreference = mContext.getSharedPreferences(SHARED_PREFERENCE_NAME + REFRESH_TOKEN_SUFFIX,
                Activity.MODE_PRIVATE);

        if (mAccessTokenSharedPreference == null || mRefreshTokenSharedPreference == null) {
            throw new IllegalStateException("Fail to create SharedPreference");
        }
    }

    /**
     * When storing access token, the key needs to be a strict match.
     */
    void saveAccessToken(final AccessTokenCacheItem accessToken) {
        final TokenCacheKey key = TokenCacheKey.extractKeyForAT(accessToken);
        final Editor editor = mAccessTokenSharedPreference.edit();
        editor.putString(key.toString(), mGson.toJson(accessToken));
        editor.apply();
    }

    /**
     * Save the refresh token item.
     */
    void saveRefreshToken(final RefreshTokenCacheItem refreshToken) {
        final TokenCacheKey key = TokenCacheKey.extractKeyForRT(refreshToken);
        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.putString(key.toString(), mGson.toJson(refreshToken));
        editor.apply();
    }

    /**
     * For access token item, authority, clientid and policy(if applicable) has to be matched. If user
     * is provided, it also has to be matched. Scope in the cached access token item has to be the exact same with the
     * scopes in the lookup key.
     */
    List<AccessTokenCacheItem> getAccessToken(final TokenCacheKey tokenCacheKey) {
        final Map<String, String> accessTokens = (Map<String, String>) mAccessTokenSharedPreference.getAll();
        final List<AccessTokenCacheItem> foundATs = new ArrayList<>();
        for (final String accessTokenValue: accessTokens.values()) {
            final AccessTokenCacheItem accessTokenCacheItem = mGson.fromJson(accessTokenValue, AccessTokenCacheItem.class);
            if (tokenCacheKey.matches(accessTokenCacheItem) && tokenCacheKey.isScopeEqual(accessTokenCacheItem)) {
                foundATs.add(accessTokenCacheItem);
            }
        }

        return foundATs;
    }

    /**
     * For refresh token item, all the RTs are multi-scope. If authority, clientid, policy and user (if applicable)
     * are matched, try to use the RT.
     * @param tokenCacheKey
     * @return
     */
    List<RefreshTokenCacheItem> getRefreshToken(final TokenCacheKey tokenCacheKey) {
        final Map<String, String> refreshTokens = (Map<String, String>) mRefreshTokenSharedPreference.getAll();
        final List<RefreshTokenCacheItem> foundRTs = new ArrayList<>();
        for (final String refreshTokenValue : refreshTokens.values()) {
            final RefreshTokenCacheItem refreshTokenCacheItem = mGson.fromJson(refreshTokenValue, RefreshTokenCacheItem.class);
            if (tokenCacheKey.matches(refreshTokenCacheItem)) {
                foundRTs.add(refreshTokenCacheItem);
            }
        }

        return foundRTs;
    }

    /**
     * Delete the refreh token item.
     * @param rtItem The {@link TokenCacheItem} to remove.
     */
    void deleteRefreshToken(final TokenCacheItem rtItem) {
        final String key = TokenCacheKey.extractKeyForRT(rtItem).toString();
        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.remove(key);
        editor.apply();
    }

    /**
     * Remove all the tokens from cache.
     */
    void removeAll() {
        final Editor accessTokenSharedPreferenceEditor = mAccessTokenSharedPreference.edit();
        accessTokenSharedPreferenceEditor.clear();
        accessTokenSharedPreferenceEditor.apply();

        final Editor refreshTokenSharedPreferenceEditor = mRefreshTokenSharedPreference.edit();
        refreshTokenSharedPreferenceEditor.clear();
        refreshTokenSharedPreferenceEditor.apply();
    }

    /**
     * @return Immutable List of all the {@link AccessTokenCacheItem}s.
     */
    List<AccessTokenCacheItem> getAllAccessTokens() {
        final Map<String, String> allAT = (Map<String, String>) mAccessTokenSharedPreference.getAll();
        final List<AccessTokenCacheItem> accessTokenCacheItems = new ArrayList<>(allAT.size());
        for (final String accessTokenValue : allAT.values()) {
            final AccessTokenCacheItem accessTokenCacheItem = mGson.fromJson(accessTokenValue, AccessTokenCacheItem.class);
            accessTokenCacheItems.add(accessTokenCacheItem);
        }

        return Collections.unmodifiableList(accessTokenCacheItems);
    }

    /**
     * @return Immutable List of all the {@link RefreshTokenCacheItem}s.
     */
    List<RefreshTokenCacheItem> getAllRefreshTokens() {
        final Map<String, String> allRTs = (Map<String, String>) mRefreshTokenSharedPreference.getAll();
        final List<RefreshTokenCacheItem> refreshTokenCacheItems = new ArrayList<>(allRTs.size());
        for (final String rtValue : allRTs.values()) {
            final RefreshTokenCacheItem refreshTokenCacheItem = mGson.fromJson(rtValue, RefreshTokenCacheItem.class);
            refreshTokenCacheItems.add(refreshTokenCacheItem);
        }

        return Collections.unmodifiableList(refreshTokenCacheItems);
    }

    List<RefreshTokenCacheItem> getAllRefreshTokens(final String clientId) {
        final List<RefreshTokenCacheItem> allRTs = getAllRefreshTokens();

        final List<RefreshTokenCacheItem> allRTsForApp = new ArrayList<>(allRTs.size());
        for (final RefreshTokenCacheItem refreshTokenCacheItem : allRTs) {
            if (clientId.equals(refreshTokenCacheItem.getClientId())) {
                allRTsForApp.add(refreshTokenCacheItem);
            }
        }

        return Collections.unmodifiableList(allRTsForApp);
    }
}
