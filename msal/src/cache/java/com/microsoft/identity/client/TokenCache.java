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
 * MSAL internal representation for token cache. MS first party apps can use the internal
 * {@link TokenCache#serialize(User)} and {@link TokenCache#deserialize(String)} to import and export family tokens
 * to implement SSO. To prevent confusions among external apps, we don't expose these two methods.
 */
public class TokenCache {
    private static final int DEFAULT_EXPIRATION_BUFFER = 300;
    private final TokenCacheAccessor mTokenCacheAccessor;

    /**
     * Constructor for {@link TokenCache}.
     * @param context The application context.
     */
    TokenCache(final Context context) {
        mTokenCacheAccessor = new TokenCacheAccessor(context);
    }

    /**
     * Create {@link TokenCacheItem} from {@link TokenResponse} and save it into cache.
     * @throws AuthenticationException if error happens when creating the {@link TokenCacheItem}.
     */
    TokenCacheItem saveAccessToken(final String authority, final String clientId, final TokenResponse response)
            throws AuthenticationException {
        // create the access token cache item
        final TokenCacheItem tokenCacheItem = new TokenCacheItem(authority, clientId, response);

        // check for intersection and delete all the cache entries with intersecting scopes.
        final List<TokenCacheItem> accessTokenItems = mTokenCacheAccessor.getAllAccessTokensForGivenClientId(clientId);
        for (final TokenCacheItem accessTokenItem : accessTokenItems) {
            if (MSALUtils.isScopeIntersects(tokenCacheItem.getScope(), accessTokenItem.getScope())) {
                mTokenCacheAccessor.deleteAccessToken(accessTokenItem);
            }
        }

        mTokenCacheAccessor.saveAccessToken(tokenCacheItem);
        return tokenCacheItem;
    }

    /**
     * Create {@link RefreshTokenCacheItem} from {@link TokenResponse} and save it into cache.
     * @throws AuthenticationException if error happens when creating the {@link RefreshTokenCacheItem}.
     */
    void saveRefreshToken(final String authority, final String clientId, final TokenResponse response)
            throws AuthenticationException {
        // if server returns the refresh token back, save it in the cache.
        if (!MSALUtils.isEmpty(response.getRefreshToken())) {
            final RefreshTokenCacheItem refreshTokenCacheItem = new RefreshTokenCacheItem(authority, clientId, response);
            mTokenCacheAccessor.saveRefreshToken(refreshTokenCacheItem);
        }
    }

    /**
     * Find access token matching authority, clientid, scope, user and policy in the cache.
     * @param requestParam The {@link AuthenticationRequestParameters} containing the request data to get the token for.
     * @param user The {@link User} to get the token for.
     * @return The {@link TokenCacheItem} stored in the cache, could be NULL if there is no access token or there are
     * multiple access token token items in the cache.
     */
    TokenCacheItem findAccessToken(final AuthenticationRequestParameters requestParam, final User user) {
        final TokenCacheKey key = TokenCacheKey.createKeyForAT(requestParam.getAuthority().getAuthority(),
                requestParam.getClientId(), requestParam.getScope(), user);
        final List<TokenCacheItem> tokenCacheItems = mTokenCacheAccessor.getAccessToken(key);

        if (tokenCacheItems.isEmpty()) {
            // TODO: log access token not found
            return null;
        }

        // TODO: If user is not provided for silent request, and there is only one item found in the cache. Should we return it?
        if (tokenCacheItems.size() > 1) {
            // TODO: log there are multiple access tokens found, don't know which one to use.
            return null;
        }

        // Access token lookup needs to be a strict match. In the JSON response from token endpoint, server only returns the scope
        // the developer requires the token for. We store the token separately for considerations i.e. MFA.
        final TokenCacheItem tokenCacheItem = tokenCacheItems.get(0);
        if (!isExpired(tokenCacheItem.getExpiresOn())) {
            return tokenCacheItem;
        }

        //TODO: log the access token found is expired.
        return null;
    }

    // All the token AAD returns are multi-scopes. MSAL only support ADFS 2016, which issues multi-scope RT.
    RefreshTokenCacheItem findRefreshToken(final AuthenticationRequestParameters requestParam, final User user)
            throws AuthenticationException {
        final TokenCacheKey key = TokenCacheKey.createKeyForRT(requestParam.getClientId(), user);
        final List<RefreshTokenCacheItem> refreshTokenCacheItems = mTokenCacheAccessor.getRefreshToken(key);

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

    /**
     * Delete refresh token items.
     * @param rtItem The item to delete.
     */
    void deleteRT(final BaseTokenCacheItem rtItem) {
        mTokenCacheAccessor.deleteRefreshToken(rtItem);
    }

    /**
     * An immutable list of signed-in users for the given client id.
     * @param clientId The application client id that is used to retrieve for all the signed in users.
     * @return The list of signed in users for the given client id.
     * @throws AuthenticationException
     */
    List<User> getUsers(final String clientId) throws AuthenticationException {
        if (MSALUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("empty or null clientId");
        }

        final List<RefreshTokenCacheItem> allRefreshTokens = mTokenCacheAccessor.getAllRefreshTokensForGivenClientId(clientId);
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
     * TODO: add the functionality to find the tokens matching the given user and return the serialize blob.
     */
    String serialize(final User user) {
        return "";
    }

    /**
     * Internal API for the sdk to take in the serialized blob and save it into the cache.
     *
     * The sdk will deserialize the input blob into the token cache item and save it into cache.
     * @param serializedBlob
     * TODO: add the functionality to take in the serialized blob and add the item into the cache.
     */
    void deserialize(final String serializedBlob) {

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
}