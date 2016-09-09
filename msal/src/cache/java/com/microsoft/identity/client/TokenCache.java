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

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MSAL internal representation for token cache. MS first party apps can use the internal
 * {@link TokenCache#serialize(User)} and {@link TokenCache#deserialize(String)} to import and export family tokens
 * to implement SSO. To prevent confusions among external apps, we don't expose these two methods.
 */
public class TokenCache {
    private final TokenCacheAccessor mTokenCacheAccessor;

    /**
     * Constructor for {@link TokenCache}.
     * @param context The application context.
     */
    TokenCache(final Context context) {
        mTokenCacheAccessor = new TokenCacheAccessor(context);
    }

    /**
     * Save the token response into cache.
     */
    void saveTokenResponse(final String authority, final String clientId, final String policy, final TokenResponse response)
            throws AuthenticationException {

        // create the access token cache item
        final AccessTokenCacheItem accessTokenCacheItem = new AccessTokenCacheItem(authority, clientId, policy, response);
        mTokenCacheAccessor.saveAccessToken(accessTokenCacheItem);

        // if server returns the refresh token back, save it in the cache.
        if (!MSALUtils.isEmpty(response.getRefreshToken())) {
            final RefreshTokenCacheItem refreshTokenCacheItem = new RefreshTokenCacheItem(authority, clientId, policy, response);
            mTokenCacheAccessor.saveRefreshToken(refreshTokenCacheItem);
        }
    }

    /**
     * Get all the access token item with given key.
     * @param key The {@link TokenCacheKey} used to retrieve the access tokens.
     * @return The list of found {@link AccessTokenCacheItem}s.
     */
    List<AccessTokenCacheItem> getAccessTokenItem(final TokenCacheKey key) {
        return mTokenCacheAccessor.getAccessToken(key);
    }

    /**
     * Get all the refresh token items with given key.
     * @param key The {@link TokenCacheKey} used to retrieve the refresh tokens.
     * @return The list of found {@link RefreshTokenCacheItem}s.
     */
    List<RefreshTokenCacheItem> getRefreshTokenItem(final TokenCacheKey key) {
        return mTokenCacheAccessor.getRefreshToken(key);
    }

    /**
     * Delete refresh token items.
     * @param rtItem
     */
    void deleteRT(final TokenCacheItem rtItem) {
        mTokenCacheAccessor.deleteRefreshToken(rtItem);
    }

    /**
     * @return All the {@link AccessTokenCacheItem}s in the cache.
     */
    List<AccessTokenCacheItem> getAllAccessTokens() {
        return mTokenCacheAccessor.getAllAccessTokens();
    }

    /**
     * @return All the {@link RefreshTokenCacheItem}s in the cache.
     */
    List<RefreshTokenCacheItem> getAllRefreshTokens() {
        return mTokenCacheAccessor.getAllRefreshTokens();
    }

    /**
     * Remove all the tokens in the cache.
     */
    void removeAll() {
        mTokenCacheAccessor.removeAll();
    }

    /**
     * An immutable list of signed-in users for the given client id.
     * @param clientId The application client id that is used to retrieve for all the signed in users.
     * @return The list of signed in users for the given client id.
     * @throws AuthenticationException
     */
    List<User> getUsers(final String clientId) throws AuthenticationException {
        if (MSALUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("empty or null clientid");
        }

        final List<RefreshTokenCacheItem> allRefreshTokens = mTokenCacheAccessor.getAllRefreshTokens(clientId);
        final Map<String, User> allUsers = new HashMap<>();
        for (final RefreshTokenCacheItem item : allRefreshTokens) {
            final User user = new User(new IdToken(item.getRawIdToken()));
            user.setClientId(item.getClientId());
            user.setTokenCache(this);
            allUsers.put(item.getHomeObjectId(), user);
        }

        return Collections.unmodifiableList(new ArrayList<>(allUsers.values()));
    }

    /**
     * Internal API for the SDK to serialize the family token cache item for the given user.
     *
     * The sdk will look up family token cache item with the given user id, and serialize the token cache item and
     * return it as a serialized blob.
     * @param user
     * @return
     */
    String serialize(final User user) {
        return "";
    }

    /**
     * Internal API for the sdk to take in the serialized blob and save it into the cache.
     *
     * The sdk will deserialize the input blob into the token cache item and save it into cache.
     * @param serializedBlob
     */
    void deserialize(final String serializedBlob) {

    }
}