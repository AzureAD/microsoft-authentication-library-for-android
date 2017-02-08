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

    private static final String ACCESS_TOKEN_SHARED_PREFERENCE = "com.microsoft.identity.client.token";
    private static final String REFRESH_TOKEN_SHARED_PREFERENCE = "com.microsoft.identity.client.refreshToken";

    private final Context mContext;
    private final SharedPreferences mAccessTokenSharedPreference;
    private final SharedPreferences mRefreshTokenSharedPreference;

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateTimeAdapter())
            .create();

    /**
     * Constructor for {@link TokenCacheAccessor}. Access token and refresh token will be stored separately.
     *
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
    void saveAccessToken(final TokenCacheItem accessToken) {
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
    List<TokenCacheItem> getAccessToken(final TokenCacheKey tokenCacheKey) {
        final Map<String, String> accessTokens = (Map<String, String>) mAccessTokenSharedPreference.getAll();
        final List<TokenCacheItem> foundATs = new ArrayList<>();
        for (final String accessTokenValue : accessTokens.values()) {
            final TokenCacheItem tokenCacheItem = mGson.fromJson(accessTokenValue, TokenCacheItem.class);
            if (tokenCacheKey.matches(tokenCacheItem) && tokenCacheKey.isScopeEqual(tokenCacheItem)) {
                foundATs.add(tokenCacheItem);
            }
        }

        return foundATs;
    }

    /**
     * For refresh token item, all the RTs are multi-scope. If authority, clientid, policy and user (if applicable)
     * are matched, try to use the RT.
     *
     * @param tokenCacheKey The {@link TokenCacheKey} that is used to find refresh tokens.
     * @return The List of refresh tokens matching the given key.
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
     * Delete the access token item.
     *
     * @param atItem the {@link BaseTokenCacheItem} to remove.
     */
    void deleteAccessToken(final BaseTokenCacheItem atItem) {
        final String key = TokenCacheKey.extractKeyForAT(atItem).toString();
        final Editor editor = mAccessTokenSharedPreference.edit();
        editor.remove(key).apply();
    }

    /**
     * Delete the refresh token item.
     *
     * @param rtItem The {@link BaseTokenCacheItem} to remove.
     */
    void deleteRefreshToken(final BaseTokenCacheItem rtItem) {
        final String key = TokenCacheKey.extractKeyForRT(rtItem).toString();
        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.remove(key).apply();
    }

    /**
     * @return Immutable List of all the {@link TokenCacheItem}s.
     */
    List<TokenCacheItem> getAllAccessTokens() {
        final Map<String, String> allAT = (Map<String, String>) mAccessTokenSharedPreference.getAll();
        final List<TokenCacheItem> tokenCacheItems = new ArrayList<>(allAT.size());
        for (final String accessTokenValue : allAT.values()) {
            final TokenCacheItem tokenCacheItem = mGson.fromJson(accessTokenValue, TokenCacheItem.class);
            tokenCacheItems.add(tokenCacheItem);
        }

        return Collections.unmodifiableList(tokenCacheItems);
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

    /**
     * @param clientId The client id to query the refresh token.
     * @return Immutable List of the {@link RefreshTokenCacheItem}s matching the given client id.
     */
    List<RefreshTokenCacheItem> getAllRefreshTokensForGivenClientId(final String clientId) {
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
