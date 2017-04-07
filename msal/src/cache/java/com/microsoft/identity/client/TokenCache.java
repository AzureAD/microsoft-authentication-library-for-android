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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(AccessTokenCacheItem.class, new TokenCacheItemDeserializer<AccessTokenCacheItem>())
            .registerTypeAdapter(RefreshTokenCacheItem.class, new TokenCacheItemDeserializer<RefreshTokenCacheItem>())
            .create();

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
     */
    AccessTokenCacheItem saveAccessToken(final String authority, final String clientId, final TokenResponse response)
            throws MsalClientException {
        // create the access token cache item
        Logger.info(TAG, null, "Starting to Save access token into cache. Access token will be saved with authority: " + authority
                + "; Client Id: " + clientId + "; Scopes: " + response.getScope());
        final AccessTokenCacheItem newAccessToken = new AccessTokenCacheItem(authority, clientId, response);
        final AccessTokenCacheKey accessTokenCacheKey = newAccessToken.extractTokenCacheKey();

        // check for intersection and delete all the cache entries with intersecting scopes.
        final List<AccessTokenCacheItem> accessTokenCacheItems = getAllAccessTokensForApp(clientId);
        for (final AccessTokenCacheItem accessTokenCacheItem : accessTokenCacheItems) {
            if (accessTokenCacheKey.matches(accessTokenCacheItem) && MSALUtils.isScopeIntersects(newAccessToken.getScope(),
                    accessTokenCacheItem.getScope())) {
                mTokenCacheAccessor.deleteAccessToken(accessTokenCacheItem.extractTokenCacheKey().toString());
            }
        }

        mTokenCacheAccessor.saveAccessToken(newAccessToken.extractTokenCacheKey().toString(), mGson.toJson(newAccessToken));
        return newAccessToken;
    }

    /**
     * Create {@link RefreshTokenCacheItem} from {@link TokenResponse} and save it into cache.
     */
    void saveRefreshToken(final String authorityHost, final String clientId, final TokenResponse response) throws MsalClientException {
        // if server returns the refresh token back, save it in the cache.
        if (!MSALUtils.isEmpty(response.getRefreshToken())) {
            Logger.info(TAG, null, "Starting to save refresh token into cache. Refresh token will be saved with authority: " + authorityHost
                    + "; Client Id: " + clientId);
            final RefreshTokenCacheItem refreshTokenCacheItem = new RefreshTokenCacheItem(authorityHost, clientId, response);
            mTokenCacheAccessor.saveRefreshToken(refreshTokenCacheItem.extractTokenCacheKey().toString(), mGson.toJson(refreshTokenCacheItem));
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
        final AccessTokenCacheKey key = AccessTokenCacheKey.createTokenCacheKey(requestParam.getAuthority().getAuthority(),
                requestParam.getClientId(), requestParam.getScope(), user);
        final List<AccessTokenCacheItem> accessTokenCacheItems = getAccessTokens(key);

        if (accessTokenCacheItems.isEmpty()) {
            Logger.info(TAG, requestParam.getRequestContext(), "No access is found for scopes: "
                    + MSALUtils.convertSetToString(requestParam.getScope(), " "));
            if (user != null) {
                Logger.infoPII(TAG, requestParam.getRequestContext(), "User displayable: " + user.getDisplayableId()
                        + " ;User unique identifier(Base64UrlEncoded(uid).Base64UrlEncoded(utid)): " + MSALUtils.getUniqueUserIdentifier(
                        user.getUid(), user.getUtid()));
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
        if (!accessTokenCacheItem.isExpired()) {
            return accessTokenCacheItem;
        }

        Logger.info(TAG, requestParam.getRequestContext(), "Access token is found but it's expired.");
        return null;
    }

    AccessTokenCacheItem findAccessTokenItemAuthorityNotProvided(final AuthenticationRequestParameters requestParameters, final User user)
            throws MsalClientException {
        // find AccessTokenItems with scopes, client id and user matching
        final List<AccessTokenCacheItem> accessTokenCacheItems = getAllAccessTokensForApp(requestParameters.getClientId());
        final List<AccessTokenCacheItem> matchingATs = new ArrayList<>();
        for (final AccessTokenCacheItem accessTokenCacheItem : accessTokenCacheItems) {
            if (user.getUserIdentifier().equals(accessTokenCacheItem.getUserIdentifier()) && accessTokenCacheItem.getScope().containsAll(requestParameters.getScope())) {
                matchingATs.add(accessTokenCacheItem);
            }
        }

        if (matchingATs.isEmpty()) {
            Logger.info(TAG, requestParameters.getRequestContext(), "Authority is not provided for the silent request. No access tokens matching the scopes and user exist.");
            return null;
        }

        if (matchingATs.size() > 1) {
            throw new MsalClientException(MSALError.MULTIPLE_CACHE_ENTRY_FOUND, "Authority is not provided for the silent request. There are multiple matching token cache entries found. ");
        }

        final AccessTokenCacheItem tokenCacheItem = matchingATs.get(0);
        if (!tokenCacheItem.isExpired()) {
            return tokenCacheItem;
        }

        requestParameters.setAuthority(tokenCacheItem.getAuthority(), requestParameters.getAuthority().mValidateAuthority);
        return null;
    }

    // All the token AAD returns are multi-scopes. MSAL only support ADFS 2016, which issues multi-scope RT.
    RefreshTokenCacheItem findRefreshToken(final AuthenticationRequestParameters requestParam, final User user) throws MsalClientException {
        final RefreshTokenCacheKey key = RefreshTokenCacheKey.createTokenCacheKey(requestParam.getAuthority().getAuthorityHost(), requestParam.getClientId(), user);
        final List<RefreshTokenCacheItem> refreshTokenCacheItems = getRefreshTokens(key);

        if (refreshTokenCacheItems.size() == 0) {
            Logger.info(TAG, requestParam.getRequestContext(), "No RT was found for the given user.");
            Logger.infoPII(TAG, requestParam.getRequestContext(), "The given user info is: " + user.getDisplayableId() + "; userIdentifier: "
                    + MSALUtils.getUniqueUserIdentifier(user.getUid(), user.getUtid()));
            return null;
        }

        // User info already provided, if there are multiple items found will throw since we don't what
        // is the one we should use.
        if (refreshTokenCacheItems.size() > 1) {
            throw new MsalClientException(MSALError.MULTIPLE_CACHE_ENTRY_FOUND, "Multiple token entries found.");
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

        if (rtItem == null) {
            Logger.warning(TAG, null, "Null refresh token item is passed.");
            return;
        }

        Logger.verbosePII(TAG, null, "Removing refresh token for user: " + rtItem.getDisplayableId() + "; user identifier: "
                + rtItem.getUserIdentifier());
        mTokenCacheAccessor.deleteRefreshToken(rtItem.extractTokenCacheKey().toString());
    }

    /**
     * An immutable list of signed-in users for the given client id.
     *
     * @param clientId The application client id that is used to retrieve for all the signed in users.
     * @return The list of signed in users for the given client id.
     */
    List<User> getUsers(final String environment, final String clientId) throws MsalClientException {
        if (MSALUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("empty or null clientId");
        }

        Logger.verbose(TAG, null, "Retrieve users with the given client id: " + clientId);
        final List<RefreshTokenCacheItem> allRefreshTokensForApp = getAllRefreshTokenForApp(clientId);
        final Map<String, User> allUsers = new HashMap<>();
        for (final RefreshTokenCacheItem item : allRefreshTokensForApp) {
            if (environment.equalsIgnoreCase(item.getEnvironment())) {
                final User user = item.getUser();
                allUsers.put(item.getUserIdentifier(), user);
            }
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

    private boolean tokenMatchesUser(final User user, final BaseTokenCacheItem token) {
        return token.getUserIdentifier().equals(user.getUserIdentifier());
    }

    private void deleteTokenByUser(
            final User user,
            final List<? extends BaseTokenCacheItem> tokens,
            final DeleteTokenAction delegate) {
        for (BaseTokenCacheItem token : tokens) {
            if (tokenMatchesUser(user, token)) {
                Logger.verbosePII(TAG, null, "Remove tokens for user with displayable " + user.getDisplayableId()
                        + "; User identifier: " + user.getUserIdentifier());
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
                getAllRefreshTokens(),
                new DeleteTokenAction() {
                    @Override
                    public void deleteToken(final BaseTokenCacheItem target) {
                        final RefreshTokenCacheItem refreshTokenCacheItem = (RefreshTokenCacheItem) target;
                        mTokenCacheAccessor.deleteRefreshToken(refreshTokenCacheItem.extractTokenCacheKey().toString());
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
                getAllAccessTokens(),
                new DeleteTokenAction() {
                    @Override
                    public void deleteToken(final BaseTokenCacheItem target) {
                        final AccessTokenCacheItem accessTokenCacheItem = (AccessTokenCacheItem) target;
                        mTokenCacheAccessor.deleteAccessToken(accessTokenCacheItem.extractTokenCacheKey().toString());
                    }
                });
    }

    /**
     * @return List of all {@link RefreshTokenCacheItem}s that exist in the cache.
     */
    List<RefreshTokenCacheItem> getAllRefreshTokens() {
        final Collection<String> refreshTokensAsString = mTokenCacheAccessor.getAllRefreshTokens();
        if (refreshTokensAsString == null) {
            Logger.verbose(TAG, null, "No refresh tokens existed in the token cache.");
            return Collections.emptyList();
        }

        final List<RefreshTokenCacheItem> refreshTokenCacheItems = new ArrayList<>(refreshTokensAsString.size());
        for (final String refreshTokenAsString : refreshTokensAsString) {
            final RefreshTokenCacheItem refreshTokenCacheItem = mGson.fromJson(refreshTokenAsString, RefreshTokenCacheItem.class);
            refreshTokenCacheItems.add(refreshTokenCacheItem);
        }

        return refreshTokenCacheItems;
    }

    /**
     * @return List of all {@link AccessTokenCacheItem}s that exist in the cache.
     */
    List<AccessTokenCacheItem> getAllAccessTokens(){
        final Collection<String> accessTokensAsString = mTokenCacheAccessor.getAllAccessTokens();
        if (accessTokensAsString == null) {
            Logger.verbose(TAG, null, "No access tokens existed in the token cache.");
            return Collections.emptyList();
        }

        final List<AccessTokenCacheItem> accessTokenCacheItems = new ArrayList<>(accessTokensAsString.size());
        for (final String accessTokenString : accessTokensAsString) {
            final AccessTokenCacheItem accessTokenCacheItem = mGson.fromJson(accessTokenString, AccessTokenCacheItem.class);
            accessTokenCacheItems.add(accessTokenCacheItem);
        }

        return accessTokenCacheItems;
    }

    /**
     * @param clientId Client id that is used to filter all {@link RefreshTokenCacheItem}s that exist in the cache.
     * @return The unmodifiable List of {@link RefreshTokenCacheItem}s that match the given client id.
     */
   private  List<RefreshTokenCacheItem> getAllRefreshTokenForApp(final String clientId) {
        final List<RefreshTokenCacheItem> allRTs = getAllRefreshTokens();

        final List<RefreshTokenCacheItem> allRTsForApp = new ArrayList<>(allRTs.size());
        for (final RefreshTokenCacheItem refreshTokenCacheItem : allRTs) {
            if (clientId.equalsIgnoreCase(refreshTokenCacheItem.getClientId())) {
                allRTsForApp.add(refreshTokenCacheItem);
            }
        }

        Logger.verbose(TAG, null, "Retrieve all the refresh tokens for given client id: " + clientId + "; Returned refresh token number is " + allRTsForApp.size());
        return Collections.unmodifiableList(allRTsForApp);
    }

    /**
     * @param clientId Client id that is used to filter all {@link AccessTokenCacheItem}s that exist in the cache.
     * @return The unmodifiable List of {@link AccessTokenCacheItem}s that match the given client id.
     */
    private List<AccessTokenCacheItem> getAllAccessTokensForApp(final String clientId) {
        final List<AccessTokenCacheItem> allATs = getAllAccessTokens();
        final List<AccessTokenCacheItem> allATsForApp = new ArrayList<>(allATs.size());
        for (final AccessTokenCacheItem accessTokenCacheItem : allATs) {
            if (clientId.equalsIgnoreCase(accessTokenCacheItem.getClientId())) {
                allATsForApp.add(accessTokenCacheItem);
            }
        }

        return Collections.unmodifiableList(allATsForApp);
    }

    /**
     * Look up refresh tokens with the given {@link RefreshTokenCacheKey}. Refresh token item has to match environment,
     * client id and user identifier.
     */
    private List<RefreshTokenCacheItem> getRefreshTokens(final RefreshTokenCacheKey refreshTokenCacheKey) {
        final List<RefreshTokenCacheItem> allRTs = getAllRefreshTokens();

        final List<RefreshTokenCacheItem> foundRTs = new ArrayList<>();
        for (final RefreshTokenCacheItem refreshTokenCacheItem : allRTs) {
            if (refreshTokenCacheKey.matches(refreshTokenCacheItem)) {
                foundRTs.add(refreshTokenCacheItem);
            }
        }

        Logger.verbose(TAG, null, "Retrieve refresh tokens for the given cache key");
        Logger.verbosePII(TAG, null, "Key used to retrieve refresh tokens is: " + refreshTokenCacheKey.toString());
        return foundRTs;
    }

    /**
     * For access token item, authority, clientid, user identifier has to be matched. Scopes in the item has to contain all
     * the scopes in the key.
     */
    private List<AccessTokenCacheItem> getAccessTokens(final AccessTokenCacheKey tokenCacheKey) {
        final List<AccessTokenCacheItem> accessTokens = getAllAccessTokens();
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
}