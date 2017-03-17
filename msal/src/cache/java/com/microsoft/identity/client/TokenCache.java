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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MSAL internal representation for token cache.
 */
class TokenCache {
    private static final String TAG = TokenCache.class.getSimpleName();

    private static final int DEFAULT_EXPIRATION_BUFFER = 300;
    private final TokenCacheAccessor mTokenCacheAccessor;

    /**
     * Constructor for {@link TokenCache}.
     *
     * @param context The application context.
     */
    TokenCache(final Context context) {
        mTokenCacheAccessor = new TokenCacheAccessor(context);
    }

    /**
     * Create {@link AccessTokenCacheItem} from {@link TokenResponse} and save it into cache.
     *
     * @throws AuthenticationException if error happens when creating the {@link AccessTokenCacheItem}.
     */
    AccessTokenCacheItem saveAccessToken(final String authority, final String clientId, final TokenResponse response)
            throws AuthenticationException {
        // create the access token cache item
        Logger.info(TAG, null, "Starting to Save access token into cache. Access token will be saved with authority: " + authority
                + "; Client Id: " + clientId + "; Scopes: " + response.getScope());
        final AccessTokenCacheItem newAccessToken = new AccessTokenCacheItem(authority, clientId, response);
        final TokenCacheKey accessTokenCacheKey = newAccessToken.extractTokenCacheKey();

        // check for intersection and delete all the cache entries with intersecting scopes.
        final List<AccessTokenCacheItem> accessTokenCacheItems = mTokenCacheAccessor.getAllAccessTokensForGivenClientId(clientId);
        for (final AccessTokenCacheItem accessTokenCacheItem : accessTokenCacheItems) {
            if (accessTokenCacheKey.matches(accessTokenCacheItem) && MSALUtils.isScopeIntersects(newAccessToken.getScope(), accessTokenCacheItem.getScope())) {
                mTokenCacheAccessor.deleteAccessToken(accessTokenCacheItem);
            }
        }

        mTokenCacheAccessor.saveAccessToken(newAccessToken);
        return newAccessToken;
    }

    /**
     * Create {@link RefreshTokenCacheItem} from {@link TokenResponse} and save it into cache.
     *
     * @throws AuthenticationException if error happens when creating the {@link RefreshTokenCacheItem}.
     */
    void saveRefreshToken(final String authority, final String clientId, final TokenResponse response)
            throws AuthenticationException {
        // if server returns the refresh token back, save it in the cache.
        if (!MSALUtils.isEmpty(response.getRefreshToken())) {
            Logger.info(TAG, null, "Starting to save refresh token into cache. Refresh token will be saved with authority: " + authority
                    + "; Client Id: " + clientId);
            final RefreshTokenCacheItem refreshTokenCacheItem = new RefreshTokenCacheItem(clientId, response);
            mTokenCacheAccessor.saveRefreshToken(refreshTokenCacheItem);
        }
    }

    /**
     * Find access token matching authority, clientid, scope, user in the cache.
     *
     * @param requestParam The {@link AuthenticationRequestParameters} containing the request data to get the token for.
     * @param user         The {@link User} to get the token for.
     * @return The {@link AccessTokenCacheItem} stored in the cache, could be NULL if there is no access token or there are
     * multiple access token token items in the cache.
     */
    AccessTokenCacheItem findAccessToken(final AuthenticationRequestParameters requestParam, final User user) {
        final TokenCacheKey key = TokenCacheKey.createKeyForAT(requestParam.getAuthority().getAuthority(),
                requestParam.getClientId(), requestParam.getScope(), user);
        final List<AccessTokenCacheItem> accessTokenCacheItems = getAccessToken(key);

        if (accessTokenCacheItems.isEmpty()) {
            Logger.info(TAG, requestParam.getRequestContext(), "No access is found for scopes: "
                    + MSALUtils.convertSetToString(requestParam.getScope(), " "));
            if (user != null) {
                Logger.infoPII(TAG, requestParam.getRequestContext(), "User displayable: " + user.getDisplayableId()
                        + " ;User home object id: " + user.getHomeObjectId());
            }
            return null;
        }

        // TODO: If user is not provided for silent request, and there is only one item found in the cache. Should we return it?
        if (accessTokenCacheItems.size() > 1) {
            Logger.verbose(TAG, requestParam.getRequestContext(), "Multiple access tokens are returned, cannot "
                    + "determine which one to return.");
            return null;
        }

        // Since server may return us more scopes, for access token lookup, we need to check if the scope contains all the
        // sopces in the request.
        final AccessTokenCacheItem accessTokenCacheItem = accessTokenCacheItems.get(0);
        if (!isExpired(accessTokenCacheItem.getExpiresOn())) {
            return accessTokenCacheItem;
        }

        Logger.info(TAG, requestParam.getRequestContext(), "Access token is found but it's expired.");
        return null;
    }

    /**
     * For access token item, authority, clientid, user home oid(if applicable) has to be matched. If user
     * is provided, it also has to be matched. Scope in the cached access token item has to be the exact same with the
     * scopes in the lookup key.
     */
    List<AccessTokenCacheItem> getAccessToken(final TokenCacheKey tokenCacheKey) {
        final List<AccessTokenCacheItem> accessTokens = mTokenCacheAccessor.getAllAccessTokens();
        final List<AccessTokenCacheItem> foundATs = new ArrayList<>();
        for (final AccessTokenCacheItem accessTokenCacheItem : accessTokens) {
            if (tokenCacheKey.matches(accessTokenCacheItem) && accessTokenCacheItem.getScope().containsAll(tokenCacheKey.getScope())) {
                foundATs.add(accessTokenCacheItem);
            }
        }

        Logger.verbose(TAG, null, "Retrieve access tokens for the given cache key.");
        Logger.verbosePII(TAG, null, "Key used to retrieve access tokens is: " + tokenCacheKey);
        return foundATs;
    }

    // All the token AAD returns are multi-scopes. MSAL only support ADFS 2016, which issues multi-scope RT.
    RefreshTokenCacheItem findRefreshToken(final AuthenticationRequestParameters requestParam, final User user)
            throws AuthenticationException {
        final TokenCacheKey key = TokenCacheKey.createKeyForRT(requestParam.getClientId(), user);
        final List<RefreshTokenCacheItem> refreshTokenCacheItems = mTokenCacheAccessor.getRefreshToken(key);

        if (refreshTokenCacheItems.size() == 0) {
            Logger.info(TAG, requestParam.getRequestContext(), "No RT was found for the given user.");
            Logger.infoPII(TAG, requestParam.getRequestContext(), "The given user info is: " + user.getDisplayableId() + "; homeOid: "
                    + user.getHomeObjectId());
            return null;
        }

        // User info already provided, if there are multiple items found will throw since we don't what
        // is the one we should use.
        if (refreshTokenCacheItems.size() > 1) {
            throw new AuthenticationException(MSALError.MULTIPLE_CACHE_ENTRY_FOUND);
        }

        return refreshTokenCacheItems.get(0);
    }

    /**
     * Delete refresh token items.
     *
     * @param rtItem The item to delete.
     */
    void deleteRT(final RefreshTokenCacheItem rtItem) {
        Logger.info(TAG, null, "Removing refresh tokens from the cache.");
        mTokenCacheAccessor.deleteRefreshToken(rtItem);
    }

    /**
     * An immutable list of signed-in users for the given client id.
     *
     * @param clientId The application client id that is used to retrieve for all the signed in users.
     * @return The list of signed in users for the given client id.
     * @throws AuthenticationException
     */
    List<User> getUsers(final String clientId) throws AuthenticationException {
        if (MSALUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("empty or null clientId");
        }

        Logger.verbose(TAG, null, "Retrieve users with the given client id: " + clientId);
        final List<RefreshTokenCacheItem> allRefreshTokens = mTokenCacheAccessor.getAllRefreshTokensForGivenClientId(clientId);
        final Map<String, User> allUsers = new HashMap<>();
        for (final RefreshTokenCacheItem item : allRefreshTokens) {
            final User user = new User(new IdToken(item.getRawIdToken()));
            allUsers.put(item.getHomeObjectId(), user);
        }

        return Collections.unmodifiableList(new ArrayList<>(allUsers.values()));
    }

    /**
     * Delegate to handle the deleting of {@link BaseTokenCacheItem}s
     */
    private interface DeleteTokenAction {

        /**
         * Deletes the supplied token
         *
         * @param target the {@link BaseTokenCacheItem} to delete
         */
        void deleteToken(final BaseTokenCacheItem target);
    }

    private boolean tokenHomeObjectIdMatchesUser(final User user, final BaseTokenCacheItem token) {
        return token.getHomeObjectId().equals(user.getHomeObjectId());
    }

    /**
     * @param expiresOn The expires on to check for.
     * @return True if the given date is already expired, false otherwise.
     */
    private boolean isExpired(final Date expiresOn) {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, DEFAULT_EXPIRATION_BUFFER);
        final Date validity = calendar.getTime();

        return expiresOn != null && expiresOn.before(validity);
    }

    private void deleteTokenByUser(
            final User user,
            final List<? extends BaseTokenCacheItem> tokens,
            final DeleteTokenAction delegate) {
        for (BaseTokenCacheItem token : tokens) {
            if (tokenHomeObjectIdMatchesUser(user, token)) {
                delegate.deleteToken(token);
                return;
            }
        }
    }

    /**
     * Delete the refresh token associated with the supplied {@link User}
     *
     * @param user the User whose refresh token should be deleted
     */
    void deleteRefreshTokenByUser(final User user) {
        deleteTokenByUser(
                user,
                mTokenCacheAccessor.getAllRefreshTokens(),
                new DeleteTokenAction() {
                    @Override
                    public void deleteToken(final BaseTokenCacheItem target) {
                        mTokenCacheAccessor.deleteRefreshToken((RefreshTokenCacheItem) target);
                    }
                });
    }

    /**
     * Delete the access token associated with the supplied {@link User}
     *
     * @param user the User whose access token should be deleted
     */
    void deleteAccessTokenByUser(final User user) {
        deleteTokenByUser(
                user,
                mTokenCacheAccessor.getAllAccessTokens(),
                new DeleteTokenAction() {
                    @Override
                    public void deleteToken(final BaseTokenCacheItem target) {
                        mTokenCacheAccessor.deleteAccessToken((AccessTokenCacheItem) target);
                    }
                });
    }
}