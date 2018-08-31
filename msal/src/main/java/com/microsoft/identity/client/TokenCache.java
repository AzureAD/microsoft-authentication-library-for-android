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
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.AccountCredentialCache;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.ICacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftSts;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.AccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;

/**
 * MSAL internal representation for token cache.
 */
public class TokenCache {
    private static final String TAG = TokenCache.class.getSimpleName();

    private static final int DEFAULT_EXPIRATION_BUFFER = 300;
    private final TokenCacheAccessor mTokenCacheAccessor;
    private MsalOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> mCommonCache;

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
        mCommonCache = initCommonCache(context);
    }

    private MsalOAuth2TokenCache<
            MicrosoftStsOAuth2Strategy,
            MicrosoftStsAuthorizationRequest,
            MicrosoftStsTokenResponse,
            MicrosoftAccount,
            MicrosoftRefreshToken> initCommonCache(final Context context) {

        // Init the new-schema cache
        final ICacheKeyValueDelegate cacheKeyValueDelegate = new CacheKeyValueDelegate();
        final IStorageHelper storageHelper = new StorageHelper(context);
        final ISharedPreferencesFileManager sharedPreferencesFileManager = new SharedPreferencesFileManager(context, DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES, storageHelper);
        final IAccountCredentialCache accountCredentialCache = new AccountCredentialCache(cacheKeyValueDelegate, sharedPreferencesFileManager);
        final MicrosoftStsAccountCredentialAdapter accountCredentialAdapter = new MicrosoftStsAccountCredentialAdapter();

        return new MsalOAuth2TokenCache<>(
                context,
                accountCredentialCache,
                accountCredentialAdapter
        );
    }

    OAuth2TokenCache<?, ?, ?> getOAuth2TokenCache() {
        return mCommonCache;
    }

    AccessTokenCacheItem saveTokensToCommonCache(
            final URL authority,
            final String clientId,
            final TokenResponse msalTokenResponse,
            final String correlationId) throws MsalClientException {
        PublicClientApplication.initializeDiagnosticContext(correlationId);
        // TODO where is the displayable id? Why is it missing?
        final AccessTokenCacheItem newAccessToken = new AccessTokenCacheItem(authority.toString(), clientId, msalTokenResponse);

        // Create the AAD instance
        final MicrosoftSts msSts = new MicrosoftSts();

        // Convert the TokenResponse to the Common OM
        final MicrosoftStsTokenResponse tokenResponse = CoreAdapter.asMsStsTokenResponse(msalTokenResponse);
        tokenResponse.setClientId(clientId);

        // Initialize a config for the strategy to consume
        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();

        // Create the OAuth2Strategy
        // TODO how do I know if AuthorityMetadata Validation is enabled?
        final MicrosoftStsOAuth2Strategy strategy = msSts.createOAuth2Strategy(config);

        // Create the AuthorizationRequest
        //TODO need to fill out the null paras once MicrosoftStsOAuth2Configuration implementation complete
        final MicrosoftStsAuthorizationRequest.Builder builder = new MicrosoftStsAuthorizationRequest.Builder<>(clientId,
                null,
                authority,
                tokenResponse.getScope(),
                null,
                null,
                null);

        try {
            mCommonCache.save(strategy, builder.build(), tokenResponse);
        } catch (final ClientException e) {
            // Rethrow
            throw new MsalClientException(e.getErrorCode(), "Failed to save tokens.", e);
        }

        return newAccessToken;
    }

    /**
     * Create {@link AccessTokenCacheItem} from {@link TokenResponse} and save it into cache.
     */
    AccessTokenCacheItem saveAccessToken(final String authority, final String clientId, final TokenResponse response, final RequestContext requestContext)
            throws MsalClientException {
        // create the access token cache item
        Logger.info(TAG, null, "Starting to Save access token into cache.");
        Logger.infoPII(TAG, null, "Access token will be saved with authority: " + authority
                + "; Client Id: " + clientId + "; Scopes: " + response.getScope());

        final AccessTokenCacheItem newAccessToken = new AccessTokenCacheItem(authority, clientId, response);
        final AccessTokenCacheKey accessTokenCacheKey = newAccessToken.extractTokenCacheKey();

        // check for intersection and delete all the cache entries with intersecting scopes.
        final List<AccessTokenCacheItem> accessTokenCacheItems = getAllAccessTokensForApp(clientId, requestContext);
        for (final AccessTokenCacheItem accessTokenCacheItem : accessTokenCacheItems) {
            if (accessTokenCacheKey.matches(accessTokenCacheItem) && MsalUtils.isScopeIntersects(newAccessToken.getScope(),
                    accessTokenCacheItem.getScope())) {
                mTokenCacheAccessor.deleteAccessToken(accessTokenCacheItem.extractTokenCacheKey().toString(), requestContext);
            }
        }

        final String atCacheKey = newAccessToken.extractTokenCacheKey().toString();
        final String atCacheValue = mGson.toJson(newAccessToken);

        mTokenCacheAccessor.saveAccessToken(atCacheKey, atCacheValue, requestContext);
        return newAccessToken;
    }

    /**
     * Create {@link RefreshTokenCacheItem} from {@link TokenResponse} and save it into cache.
     */
    void saveRefreshToken(final String authorityHost, final String clientId, final TokenResponse response, final RequestContext requestContext) throws MsalClientException {
        // if server returns the refresh token back, save it in the cache.
        if (!MsalUtils.isEmpty(response.getRefreshToken())) {
            Logger.info(TAG, requestContext, "Starting to save refresh token into cache.");
            Logger.infoPII(TAG, requestContext, "Refresh token will be saved with authority: " + authorityHost
                    + "; Client Id: " + clientId);
            final RefreshTokenCacheItem refreshTokenCacheItem = new RefreshTokenCacheItem(authorityHost, clientId, response);
            final String rtCacheKey = refreshTokenCacheItem.extractTokenCacheKey().toString();
            final String rtCacheValue = mGson.toJson(refreshTokenCacheItem);
            mTokenCacheAccessor.saveRefreshToken(rtCacheKey, rtCacheValue, requestContext);
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
        final List<AccessTokenCacheItem> accessTokenCacheItems = getAccessTokens(key, requestParam.getRequestContext());

        if (accessTokenCacheItems.isEmpty()) {
            Logger.info(TAG, requestParam.getRequestContext(), "No access is found for scopes: "
                    + MsalUtils.convertSetToString(requestParam.getScope(), " "));
            if (user != null) {
                Logger.infoPII(TAG, requestParam.getRequestContext(), "User displayable: " + user.getDisplayableId()
                        + " ;User unique identifier(Base64UrlEncoded(uid).Base64UrlEncoded(utid)): " + MsalUtils.getUniqueUserIdentifier(
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
        final List<AccessTokenCacheItem> accessTokenCacheItems = getAllAccessTokensForApp(requestParameters.getClientId(), requestParameters.getRequestContext());
        final List<AccessTokenCacheItem> matchingATs = new ArrayList<>();
        for (final AccessTokenCacheItem accessTokenCacheItem : accessTokenCacheItems) {
            if (user.getUserIdentifier().equals(accessTokenCacheItem.getUserIdentifier())) {
                matchingATs.add(accessTokenCacheItem);
            }
        }

        if (matchingATs.isEmpty()) {
            Logger.verbose(TAG, requestParameters.getRequestContext(), "No tokens matching the user exist.");
            return null;
        }

        // match scope
        final List<AccessTokenCacheItem> accessTokenWithScopeMatching = new ArrayList<>();
        for (final AccessTokenCacheItem accessTokenCacheItem : matchingATs) {
            if (accessTokenCacheItem.getScope().containsAll(requestParameters.getScope())) {
                accessTokenWithScopeMatching.add(accessTokenCacheItem);
            }
        }

        // throw if more than one matching items are found.
        if (accessTokenWithScopeMatching.size() > 1) {
            Logger.error(TAG, requestParameters.getRequestContext(), "AuthorityMetadata is not provided for the silent request. Multiple matching tokens were detected.", null);
            throw new MsalClientException(MsalClientException.MULTIPLE_MATCHING_TOKENS_DETECTED, "AuthorityMetadata is not provided for the silent request. There are multiple matching tokens detected. ");
        }

        AccessTokenCacheItem accessTokenCacheItem = null;
        final String authority;
        if (accessTokenWithScopeMatching.size() == 1) {
            accessTokenCacheItem = accessTokenWithScopeMatching.get(0);
            authority = accessTokenCacheItem.getAuthority();
        } else {
            // If no tokens matching the token exists, try to count the unique authorities.
            final Set<String> uniqueAuthorities = new HashSet<>();
            for (final AccessTokenCacheItem tokenCacheItem : matchingATs) {
                uniqueAuthorities.add(tokenCacheItem.getAuthority());
            }

            if (uniqueAuthorities.size() != 1) {
                Logger.error(TAG, requestParameters.getRequestContext(), "AuthorityMetadata is not provided for the silent request. Mutiple authorities found.", null);
                final StringBuilder foundAuthorities = new StringBuilder();
                while (uniqueAuthorities.iterator().hasNext()) {
                    foundAuthorities.append(uniqueAuthorities.iterator().next());
                    foundAuthorities.append("; ");
                }
                Logger.errorPII(TAG, requestParameters.getRequestContext(), "The authorities found in the cache are: " + foundAuthorities.toString(), null);
                throw new MsalClientException(MsalClientException.MULTIPLE_MATCHING_TOKENS_DETECTED, "AuthorityMetadata is not provided for the silent request. There are multiple matching tokens detected. ");
            }

            authority = uniqueAuthorities.iterator().next();
        }

        Logger.verbosePII(TAG, requestParameters.getRequestContext(), "AuthorityMetadata is not provided but found one matching access token item, authority is: " + authority);
        requestParameters.setAuthority(authority, requestParameters.getAuthority().mValidateAuthority);
        if (accessTokenCacheItem != null && !accessTokenCacheItem.isExpired()) {
            return accessTokenCacheItem;
        }

        Logger.verbose(TAG, requestParameters.getRequestContext(), "Access token item found in the cache is already expired.");
        return null;
    }

    // All the token AAD returns are multi-scopes. MSAL only support ADFS 2016, which issues multi-scope RT.
    RefreshTokenCacheItem findRefreshToken(final AuthenticationRequestParameters requestParam, final User user) throws MsalClientException {
        final RefreshTokenCacheKey key = RefreshTokenCacheKey.createTokenCacheKey(requestParam.getAuthority().getAuthorityHost(), requestParam.getClientId(), user);
        final List<RefreshTokenCacheItem> refreshTokenCacheItems = getRefreshTokens(key, requestParam.getRequestContext());

        if (refreshTokenCacheItems.size() == 0) {
            Logger.info(TAG, requestParam.getRequestContext(), "No RT was found for the given user.");
            Logger.infoPII(TAG, requestParam.getRequestContext(), "The given user info is: " + user.getDisplayableId() + "; userIdentifier: "
                    + MsalUtils.getUniqueUserIdentifier(user.getUid(), user.getUtid()));
            return null;
        }

        // User info already provided, if there are multiple items found will throw since we don't what
        // is the one we should use.
        if (refreshTokenCacheItems.size() > 1) {
            throw new MsalClientException(MsalClientException.MULTIPLE_MATCHING_TOKENS_DETECTED, "Multiple tokens were detected.");
        }

        return refreshTokenCacheItems.get(0);
    }

    /**
     * Delete refresh token items.
     *
     * @param rtItem The item to delete.
     */
    void deleteRT(final RefreshTokenCacheItem rtItem, final RequestContext requestContext) {
        Logger.info(TAG, requestContext, "Removing refresh tokens from the cache.");

        if (rtItem == null) {
            Logger.warning(TAG, requestContext, "Null refresh token item is passed.");
            return;
        }

        Logger.verbosePII(TAG, requestContext, "Removing refresh token for user: " + rtItem.getDisplayableId() + "; user identifier: "
                + rtItem.getUserIdentifier());
        mTokenCacheAccessor.deleteRefreshToken(rtItem.extractTokenCacheKey().toString(), requestContext);
    }

    /**
     * An immutable list of signed-in users for the given client id.
     *
     * @param environment
     * @param clientId       The application client id that is used to retrieve for all the signed in users.
     * @param requestContext the RequestContext initiating this call
     * @return The list of signed in users for the given client id.
     */
    List<User> getUsers(final String environment, final String clientId, final RequestContext requestContext) throws MsalClientException {
        if (MsalUtils.isEmpty(clientId)) {
            throw new IllegalArgumentException("empty or null clientId");
        }

        Logger.verbosePII(TAG, requestContext, "Retrieve users with the given client id: " + clientId);
        final List<RefreshTokenCacheItem> allRefreshTokensForApp = getAllRefreshTokenForApp(clientId, requestContext);
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
     * Delegate to handle the deleting of {@link BaseTokenCacheItem}s.
     */
    private interface DeleteTokenAction {

        /**
         * Deletes the supplied token.
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
            final RequestContext requestContext,
            final DeleteTokenAction delegate) {
        for (BaseTokenCacheItem token : tokens) {
            if (tokenMatchesUser(user, token)) {
                Logger.verbosePII(TAG, requestContext, "Remove tokens for user with displayable " + user.getDisplayableId()
                        + "; User identifier: " + user.getUserIdentifier());
                delegate.deleteToken(token);
                return;
            }
        }
    }

    /**
     * Delete the refresh token associated with the supplied {@link User}.
     *
     * @param user the User whose refresh token should be deleted
     */
    void deleteRefreshTokenByUser(final User user, final RequestContext requestContext) {
        deleteTokenByUser(
                user,
                getAllRefreshTokens(requestContext),
                requestContext,
                new DeleteTokenAction() {
                    @Override
                    public void deleteToken(final BaseTokenCacheItem target) {
                        final RefreshTokenCacheItem refreshTokenCacheItem = (RefreshTokenCacheItem) target;
                        mTokenCacheAccessor.deleteRefreshToken(refreshTokenCacheItem.extractTokenCacheKey().toString(), requestContext);
                    }
                });
    }

    /**
     * Delete the access token associated with the supplied {@link User}.
     *
     * @param user the User whose access token should be deleted
     */
    void deleteAccessTokenByUser(final User user, final RequestContext requestContext) {
        deleteTokenByUser(
                user,
                getAllAccessTokens(requestContext),
                requestContext,
                new DeleteTokenAction() {
                    @Override
                    public void deleteToken(final BaseTokenCacheItem target) {
                        final AccessTokenCacheItem accessTokenCacheItem = (AccessTokenCacheItem) target;
                        mTokenCacheAccessor.deleteAccessToken(accessTokenCacheItem.extractTokenCacheKey().toString(), requestContext);
                    }
                });
    }

    /**
     * @return List of all {@link RefreshTokenCacheItem}s that exist in the cache.
     */
    List<RefreshTokenCacheItem> getAllRefreshTokens(final RequestContext requestContext) {
        final Collection<String> refreshTokensAsString = mTokenCacheAccessor.getAllRefreshTokens(requestContext.getTelemetryRequestId());
        if (refreshTokensAsString == null) {
            Logger.verbose(TAG, requestContext, "No refresh tokens existed in the token cache.");
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
    List<AccessTokenCacheItem> getAllAccessTokens(final RequestContext requestContext) {
        final Collection<String> accessTokensAsString = mTokenCacheAccessor.getAllAccessTokens(requestContext.getTelemetryRequestId());
        if (accessTokensAsString == null) {
            Logger.verbose(TAG, requestContext, "No access tokens existed in the token cache.");
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
    private List<RefreshTokenCacheItem> getAllRefreshTokenForApp(final String clientId, final RequestContext requestContext) {
        final List<RefreshTokenCacheItem> allRTs = getAllRefreshTokens(requestContext);

        final List<RefreshTokenCacheItem> allRTsForApp = new ArrayList<>(allRTs.size());
        for (final RefreshTokenCacheItem refreshTokenCacheItem : allRTs) {
            if (clientId.equalsIgnoreCase(refreshTokenCacheItem.getClientId())) {
                allRTsForApp.add(refreshTokenCacheItem);
            }
        }

        Logger.verbosePII(TAG, requestContext, "Retrieve all the refresh tokens for given client id: " + clientId);
        Logger.verbose(TAG, requestContext, "Returned refresh token number is " + allRTsForApp.size());
        return Collections.unmodifiableList(allRTsForApp);
    }

    /**
     * @param clientId Client id that is used to filter all {@link AccessTokenCacheItem}s that exist in the cache.
     * @return The unmodifiable List of {@link AccessTokenCacheItem}s that match the given client id.
     */
    private List<AccessTokenCacheItem> getAllAccessTokensForApp(final String clientId, final RequestContext requestContext) {
        final List<AccessTokenCacheItem> allATs = getAllAccessTokens(requestContext);
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
    private List<RefreshTokenCacheItem> getRefreshTokens(final RefreshTokenCacheKey refreshTokenCacheKey, final RequestContext requestContext) {
        final List<RefreshTokenCacheItem> allRTs = getAllRefreshTokens(requestContext);

        final List<RefreshTokenCacheItem> foundRTs = new ArrayList<>();
        for (final RefreshTokenCacheItem refreshTokenCacheItem : allRTs) {
            if (refreshTokenCacheKey.matches(refreshTokenCacheItem)) {
                foundRTs.add(refreshTokenCacheItem);
            }
        }

        Logger.verbose(TAG, requestContext, "Retrieve refresh tokens for the given cache key");
        Logger.verbosePII(TAG, requestContext, "Key used to retrieve refresh tokens is: " + refreshTokenCacheKey.toString());
        return foundRTs;
    }

    /**
     * For access token item, authority, clientid, user identifier has to be matched. Scopes in the item has to contain all
     * the scopes in the key.
     */
    private List<AccessTokenCacheItem> getAccessTokens(final AccessTokenCacheKey tokenCacheKey, final RequestContext requestContext) {
        final List<AccessTokenCacheItem> accessTokens = getAllAccessTokens(requestContext);
        final List<AccessTokenCacheItem> foundATs = new ArrayList<>();
        for (final AccessTokenCacheItem accessTokenCacheItem : accessTokens) {
            if (tokenCacheKey.matches(accessTokenCacheItem) && accessTokenCacheItem.getScope().containsAll(tokenCacheKey.getScope())) {
                foundATs.add(accessTokenCacheItem);
            }
        }

        Logger.verbose(TAG, requestContext, "Retrieve access tokens for the given cache key.");
        Logger.verbosePII(TAG, requestContext, "Key used to retrieve access tokens is: " + tokenCacheKey);
        return foundATs;
    }
}