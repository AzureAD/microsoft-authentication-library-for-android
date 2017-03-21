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

import static com.microsoft.identity.client.EventConstants.EventProperty.Value.TOKEN_TYPE_AT;
import static com.microsoft.identity.client.EventConstants.EventProperty.Value.TOKEN_TYPE_RT;

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
            .registerTypeAdapter(AccessTokenCacheItem.class, new TokenCacheItemDeserializer<AccessTokenCacheItem>())
            .registerTypeAdapter(RefreshTokenCacheItem.class, new TokenCacheItemDeserializer<RefreshTokenCacheItem>())
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

    private static CacheEvent.Builder createNewCacheEvent(final Telemetry.RequestId requestId,
                                                          final EventName eventName,
                                                          final boolean isRT) {
        return new CacheEvent.Builder(requestId, eventName).tokenType(isRT ? TOKEN_TYPE_RT : TOKEN_TYPE_AT);
    }

    private static CacheEvent.Builder createAndStartNewCacheEvent(final Telemetry.RequestId requestId,
                                                                  final EventName eventName,
                                                                  final boolean isRT) {
        final CacheEvent.Builder cacheEventBuilder = createNewCacheEvent(requestId, eventName, isRT);
        Telemetry.getInstance().startEvent(cacheEventBuilder);
        return cacheEventBuilder;
    }

    /**
     * When storing access token, the key needs to be a strict match.
     */
    void saveAccessToken(final AccessTokenCacheItem accessToken,
                         final Telemetry.RequestId telemetryRequestId) {
        final CacheEvent.Builder cacheEventBuilder =
                createAndStartNewCacheEvent(telemetryRequestId, EventName.TOKEN_CACHE_WRITE, false);

        final TokenCacheKey key = accessToken.extractTokenCacheKey();

        final Editor editor = mAccessTokenSharedPreference.edit();
        editor.putString(key.toString(), mGson.toJson(accessToken));
        editor.apply();

        Telemetry.getInstance().stopEvent(cacheEventBuilder.build());

        Logger.verbose(TAG, null, "Access token is into cache.");
        Logger.verbosePII(TAG, null, "Access token is saved with key: " + key);
    }

    /**
     * Save the refresh token item.
     */
    void saveRefreshToken(final RefreshTokenCacheItem refreshToken,
                          final Telemetry.RequestId telemetryRequestId) {
        if (refreshToken == null) {
            Logger.warning(TAG, null, "Refresh token is null, cannot save.");
            return;
        }

        final CacheEvent.Builder cacheEventBuilder =
                createAndStartNewCacheEvent(telemetryRequestId, EventName.TOKEN_CACHE_WRITE, true);

        final TokenCacheKey key = refreshToken.extractTokenCacheKey();

        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.putString(key.toString(), mGson.toJson(refreshToken));
        editor.apply();

        Telemetry.getInstance().stopEvent(cacheEventBuilder.build());

        Logger.verbose(TAG, null, "Refresh token is successfully saved into cache.");
        Logger.verbosePII(TAG, null, "Refresh token is saved with key: " + key);
    }

    /**
     * For refresh token item, all the RTs are multi-scope. If authority, clientid, and user (if applicable)
     * are matched, try to use the RT.
     *
     * @param tokenCacheKey The {@link TokenCacheKey} that is used to find refresh tokens.
     * @return The List of refresh tokens matching the given key.
     */
    List<RefreshTokenCacheItem> getRefreshToken(final TokenCacheKey tokenCacheKey,
                                                final Telemetry.RequestId telemetryRequestId) {
        final CacheEvent.Builder cacheEventBuilder =
                createAndStartNewCacheEvent(telemetryRequestId, EventName.TOKEN_CACHE_LOOKUP, true);

        final Map<String, String> refreshTokens = (Map<String, String>) mRefreshTokenSharedPreference.getAll();
        final List<RefreshTokenCacheItem> foundRTs = new ArrayList<>();
        for (final String refreshTokenValue : refreshTokens.values()) {
            final RefreshTokenCacheItem refreshTokenCacheItem = mGson.fromJson(refreshTokenValue, RefreshTokenCacheItem.class);
            if (tokenCacheKey.matches(refreshTokenCacheItem)) {
                foundRTs.add(refreshTokenCacheItem);
            }
        }

        Telemetry.getInstance().stopEvent(cacheEventBuilder.build());

        Logger.verbose(TAG, null, "Retrieve refresh tokens for the given cache key");
        Logger.verbosePII(TAG, null, "Key used to retrieve refresh tokens is: " + tokenCacheKey);
        return foundRTs;
    }

    void deleteAccessToken(final AccessTokenCacheItem atItem,
                           final Telemetry.RequestId telemetryRequestId) {
        if (atItem == null) {
            Logger.warning(TAG, null, "AccessTokenCacheItem is null, no need to delete.");
            return;
        }

        final CacheEvent.Builder cacheEventBuilder =
                createAndStartNewCacheEvent(telemetryRequestId, EventName.TOKEN_CACHE_DELETE, false);

        final String key = atItem.extractTokenCacheKey().toString();
        final Editor editor = mAccessTokenSharedPreference.edit();
        editor.remove(key);
        editor.apply();

        Telemetry.getInstance().stopEvent(cacheEventBuilder.build());
    }

    /**
     * Delete the refresh token item.
     *
     * @param rtItem The {@link BaseTokenCacheItem} to remove.
     */
    void deleteRefreshToken(final RefreshTokenCacheItem rtItem,
                            final Telemetry.RequestId telemetryRequestId) {
        if (rtItem == null) {
            Logger.warning(TAG, null, "Null refresh token item is passed.");
            return;
        }

        final CacheEvent.Builder cacheEventBuilder =
                createAndStartNewCacheEvent(telemetryRequestId, EventName.TOKEN_CACHE_DELETE, true);

        final String key = rtItem.extractTokenCacheKey().toString();
        Logger.verbose(TAG, null, "Remove the given refresh token item.");
        Logger.verbosePII(TAG, null, "Refresh token is deleted with key: " + key);

        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.remove(key);
        editor.apply();

        Telemetry.getInstance().stopEvent(cacheEventBuilder.build());
    }

    /**
     * @return Immutable List of all the {@link AccessTokenCacheItem}s.
     */
    List<AccessTokenCacheItem> getAllAccessTokens(final Telemetry.RequestId telemetryRequestId) {
        final CacheEvent.Builder cacheEventBuilder =
                createAndStartNewCacheEvent(telemetryRequestId, EventName.TOKEN_CACHE_LOOKUP, false);

        final Map<String, String> allAT = (Map<String, String>) mAccessTokenSharedPreference.getAll();
        final List<AccessTokenCacheItem> accessTokenCacheItems = new ArrayList<>(allAT.size());
        for (final String accessTokenValue : allAT.values()) {
            final AccessTokenCacheItem accessTokenCacheItem = mGson.fromJson(accessTokenValue, AccessTokenCacheItem.class);
            accessTokenCacheItems.add(accessTokenCacheItem);
        }

        Telemetry.getInstance().stopEvent(cacheEventBuilder.build());

        Logger.verbose(TAG, null, "Retrieve all the access tokens from cache, the number of access tokens returned is: " + accessTokenCacheItems.size());
        return Collections.unmodifiableList(accessTokenCacheItems);
    }

    /**
     * @return Immutable List of all the {@link RefreshTokenCacheItem}s.
     */
    List<RefreshTokenCacheItem> getAllRefreshTokens(final Telemetry.RequestId telemetryRequestId) {
        final CacheEvent.Builder cacheEventBuilder =
                createAndStartNewCacheEvent(telemetryRequestId, EventName.TOKEN_CACHE_LOOKUP, true);

        final Map<String, String> allRTs = (Map<String, String>) mRefreshTokenSharedPreference.getAll();
        final List<RefreshTokenCacheItem> refreshTokenCacheItems = new ArrayList<>(allRTs.size());
        for (final String rtValue : allRTs.values()) {
            final RefreshTokenCacheItem refreshTokenCacheItem = mGson.fromJson(rtValue, RefreshTokenCacheItem.class);
            refreshTokenCacheItems.add(refreshTokenCacheItem);
        }

        Telemetry.getInstance().stopEvent(cacheEventBuilder.build());

        Logger.verbose(TAG, null, "Retrieve all the refresh tokens, the number of refresh tokens returned is: " + refreshTokenCacheItems.size());
        return Collections.unmodifiableList(refreshTokenCacheItems);
    }

    /**
     * @param clientId The client id to query the refresh token.
     * @return Immutable List of the {@link RefreshTokenCacheItem}s matching the given client id.
     */
    List<RefreshTokenCacheItem> getAllRefreshTokensForGivenClientId(final String clientId,
                                                                    final Telemetry.RequestId telemetryRequestId) {
        final List<RefreshTokenCacheItem> allRTs = getAllRefreshTokens(telemetryRequestId);

        final List<RefreshTokenCacheItem> allRTsForApp = new ArrayList<>(allRTs.size());
        for (final RefreshTokenCacheItem refreshTokenCacheItem : allRTs) {
            if (clientId.equals(refreshTokenCacheItem.getClientId())) {
                allRTsForApp.add(refreshTokenCacheItem);
            }
        }

        Logger.verbose(TAG, null, "Retrieve all the refresh tokens for given client id: " + clientId + "; Returned refresh token number is " + allRTsForApp.size());
        return Collections.unmodifiableList(allRTsForApp);
    }

    List<AccessTokenCacheItem> getAllAccessTokensForGivenClientId(final String clientId,
                                                                  final Telemetry.RequestId telemetryRequestId) {
        final List<AccessTokenCacheItem> allATs = getAllAccessTokens(telemetryRequestId);
        final List<AccessTokenCacheItem> allATsForApp = new ArrayList<>(allATs.size());
        for (final AccessTokenCacheItem accessTokenCacheItem : allATs) {
            if (clientId.equals(accessTokenCacheItem.getClientId())) {
                allATsForApp.add(accessTokenCacheItem);
            }
        }

        return Collections.unmodifiableList(allATsForApp);
    }
}
