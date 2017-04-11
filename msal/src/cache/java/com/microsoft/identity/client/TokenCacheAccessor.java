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

    private static CacheEvent.Builder createNewCacheEventBuilder(final String eventName, final boolean isRT) {
        final CacheEvent.Builder builder = new CacheEvent.Builder(eventName);
        if (isRT) {
            builder.setIsRT(true);
        } else {
            builder.setIsAT(true);
        }
        return builder;
    }

    private static CacheEvent.Builder createAndStartNewCacheEvent(final String telemetryRequestId, final String eventName, final boolean isRT) {
        final CacheEvent.Builder cacheEventBuilder = createNewCacheEventBuilder(eventName, isRT);
        Telemetry.getInstance().startEvent(telemetryRequestId, eventName);
        return cacheEventBuilder;
    }

    /**
     * When storing access token, the key needs to be a strict match.
     */
    void saveAccessToken(final String accessTokenCacheKey, final String accessTokenItem, final RequestContext requestContext) {
        // there shouldn't be any case that this method is called with null/empty key or item
        if (MSALUtils.isEmpty(accessTokenCacheKey) || MSALUtils.isEmpty(accessTokenItem)) {
            throw new IllegalArgumentException("accessTokenCacheKey/accessTokenItem empty or null");
        }

        final CacheEvent.Builder cacheEventBuilder = createAndStartNewCacheEvent(requestContext.getTelemetryRequestId(), EventConstants.EventName.TOKEN_CACHE_WRITE, false);

        final Editor editor = mAccessTokenSharedPreference.edit();
        editor.putString(accessTokenCacheKey, accessTokenItem);
        editor.apply();

        Telemetry.getInstance().stopEvent(requestContext.getTelemetryRequestId(), cacheEventBuilder);
        Logger.verbose(TAG, requestContext, "Access token is saved into cache.");
        Logger.verbosePII(TAG, requestContext, "Access token is saved with key: " + accessTokenCacheKey);
    }

    /**
     * Save the refresh token item.
     */
    void saveRefreshToken(final String refreshTokenCacheKey, final String refreshTokenItem, final RequestContext requestContext) {
        // there shouldn't be any case that this method is called with null/empty key or item
        if (MSALUtils.isEmpty(refreshTokenCacheKey) || MSALUtils.isEmpty(refreshTokenItem)) {
            throw new IllegalArgumentException("refreshTokenCacheKey/refreshTokenItem empty or null");
        }

        final CacheEvent.Builder cacheEventBuilder = createAndStartNewCacheEvent(requestContext.getTelemetryRequestId(), EventConstants.EventName.TOKEN_CACHE_WRITE, true);

        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.putString(refreshTokenCacheKey, refreshTokenItem);
        editor.apply();

        Telemetry.getInstance().stopEvent(requestContext.getTelemetryRequestId(), cacheEventBuilder);
        Logger.verbose(TAG, requestContext, "Refresh token is successfully saved into cache.");
        Logger.verbosePII(TAG, requestContext, "Refresh token is saved with key: " + refreshTokenCacheKey);
    }

    void deleteAccessToken(final String accessTokenKey, final RequestContext requestContext) {
        final CacheEvent.Builder cacheEventBuilder = createAndStartNewCacheEvent(requestContext.getTelemetryRequestId(), EventConstants.EventName.TOKEN_CACHE_DELETE, false);
        final Editor editor = mAccessTokenSharedPreference.edit();
        editor.remove(accessTokenKey);
        editor.apply();
        Telemetry.getInstance().stopEvent(requestContext.getTelemetryRequestId(), cacheEventBuilder);
    }

    /**
     * Delete the refresh token item.
     * @param refreshTokenCacheKey The string value of the refresh token cache item key to remove.
     */
    void deleteRefreshToken(final String refreshTokenCacheKey, final RequestContext requestContext) {
        Logger.verbose(TAG, requestContext, "Remove the given refresh token item.");
        Logger.verbosePII(TAG, requestContext, "Refresh token is deleted with key: " + refreshTokenCacheKey);

        final CacheEvent.Builder cacheEventBuilder = createAndStartNewCacheEvent(requestContext.getTelemetryRequestId(), EventConstants.EventName.TOKEN_CACHE_DELETE, true);
        final Editor editor = mRefreshTokenSharedPreference.edit();
        editor.remove(refreshTokenCacheKey);
        editor.apply();
        Telemetry.getInstance().stopEvent(requestContext.getTelemetryRequestId(), cacheEventBuilder);
    }

    /**
     * @return Immutable List of all the {@link AccessTokenCacheItem}s.
     */
    Collection<String> getAllAccessTokens(final String telemetryRequestId) {
        final CacheEvent.Builder cacheEventBuilder = createAndStartNewCacheEvent(telemetryRequestId, EventConstants.EventName.TOKEN_CACHE_LOOKUP, false);
        final Map<String, String> allAT = (Map<String, String>) mAccessTokenSharedPreference.getAll();
        Telemetry.getInstance().stopEvent(telemetryRequestId, cacheEventBuilder);
        return allAT.values();
    }

    /**
     * @return Immutable List of all the {@link RefreshTokenCacheItem}s.
     */
    Collection<String> getAllRefreshTokens(final String telemetryRequestId) {
        final CacheEvent.Builder cacheEventBuilder = createAndStartNewCacheEvent(telemetryRequestId, EventConstants.EventName.TOKEN_CACHE_LOOKUP, true);
        final Map<String, String> allRTs = (Map<String, String>) mRefreshTokenSharedPreference.getAll();
        Telemetry.getInstance().stopEvent(telemetryRequestId, cacheEventBuilder);
        return allRTs.values();
    }
}