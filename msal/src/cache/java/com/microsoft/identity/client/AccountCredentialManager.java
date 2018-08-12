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
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.adal.internal.cache.IStorageHelper;
import com.microsoft.identity.common.adal.internal.cache.StorageHelper;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.ADALOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.AccountCredentialCache;
import com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IAccountCredentialCache;
import com.microsoft.identity.common.internal.cache.ICacheKeyValueDelegate;
import com.microsoft.identity.common.internal.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.internal.cache.ISharedPreferencesFileManager;
import com.microsoft.identity.common.internal.cache.MicrosoftStsAccountCredentialAdapter;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.cache.SharedPreferencesFileManager;
import com.microsoft.identity.common.internal.dto.AccessToken;
import com.microsoft.identity.common.internal.dto.Account;
import com.microsoft.identity.common.internal.dto.Credential;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.dto.IdToken;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftSts;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.AccountCredentialCache.DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES;

/**
 * MSAL internal representation for token cache.
 */
class AccountCredentialManager {
    private static final String TAG = AccountCredentialManager.class.getSimpleName();

    private final TokenCacheAccessor mTokenCacheAccessor;
    private MsalOAuth2TokenCache mCommonCache;

    private Gson mGson = new GsonBuilder()
            .registerTypeAdapter(
                    AccessTokenCacheItem.class,
                    new TokenCacheItemDeserializer<AccessTokenCacheItem>()
            )
            .registerTypeAdapter(
                    RefreshTokenCacheItem.class,
                    new TokenCacheItemDeserializer<RefreshTokenCacheItem>()
            ).create();

    /**
     * Constructor for {@link AccountCredentialManager}.
     *
     * @param context The application context.
     */
    AccountCredentialManager(final Context context) {
        mTokenCacheAccessor = new TokenCacheAccessor(context);
        mCommonCache = initCommonCache(context);
    }

    private MsalOAuth2TokenCache initCommonCache(final Context context) {
        // Init the ADAL cache for SSO-state sync
        final IShareSingleSignOnState adalCache = new ADALOAuth2TokenCache(context);
        List<IShareSingleSignOnState> sharedSsoCaches = new ArrayList<>();
        sharedSsoCaches.add(adalCache);

        // Init the new-schema cache
        final ICacheKeyValueDelegate cacheKeyValueDelegate = new CacheKeyValueDelegate();
        final IStorageHelper storageHelper = new StorageHelper(context);
        final ISharedPreferencesFileManager sharedPreferencesFileManager = new SharedPreferencesFileManager(context, DEFAULT_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES, storageHelper);
        final IAccountCredentialCache accountCredentialCache = new AccountCredentialCache(cacheKeyValueDelegate, sharedPreferencesFileManager);
        final MicrosoftStsAccountCredentialAdapter accountCredentialAdapter = new MicrosoftStsAccountCredentialAdapter();
        final MsalOAuth2TokenCache tokenCache = new MsalOAuth2TokenCache(
                context,
                accountCredentialCache,
                accountCredentialAdapter,
                sharedSsoCaches // TODO wire this up inside of common
        );

        return tokenCache;
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
        // TODO how do I know if Authority Validation is enabled?
        final MicrosoftStsOAuth2Strategy strategy = msSts.createOAuth2Strategy(config);

        // Create the AuthorizationRequest
        final MicrosoftStsAuthorizationRequest authorizationRequest = new MicrosoftStsAuthorizationRequest();
        authorizationRequest.setClientId(clientId);
        authorizationRequest.setScope(new HashSet<>(Arrays.asList(tokenResponse.getScope().split(" "))));
        authorizationRequest.setAuthority(authority);

        try {
            mCommonCache.saveTokens(strategy, authorizationRequest, tokenResponse);
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
        final IAccountCredentialCache accountCredentialCache = mCommonCache.getAccountCredentialCache();
        final List<Credential> accessTokens = accountCredentialCache.getCredentialsFilteredBy(
                user.getUid() + "." + user.getUtid(),
                requestParam.getAuthority().getAuthorityHost(),
                CredentialType.AccessToken,
                requestParam.getClientId(),
                user.getUtid(),
                null // wildcard (*) since scopes expects a String, not a Set
        );

        final List<Credential> credentialsWithMatchingScopes = new ArrayList<>();

        // Iterate over the List returned, the AT we're looking for has all the same scope data...
        for (final Credential credential : accessTokens) {
            final AccessToken accessToken = (AccessToken) credential;
            final String[] scopes = accessToken.getTarget().split(" "); // split on whitespace

            Set<String> scopesInCredential = new HashSet<>(Arrays.asList(scopes));
            Set<String> scopesInRequest = requestParam.getScope();

            // Normalize everything to lowercase
            scopesInCredential = normalizeLowerCase(scopesInCredential);
            scopesInRequest = normalizeLowerCase(scopesInRequest);

            if (scopesInCredential.containsAll(scopesInRequest)) {
                // We have a winner
                credentialsWithMatchingScopes.add(credential);
            }
        }

        if (credentialsWithMatchingScopes.isEmpty()) {
            // TODO log a warning
            return null;
        }

        // TODO what do if >1 result?
        if (credentialsWithMatchingScopes.size() > 1) {
            // TODO log a warning
            return null;
        }

        final AccessToken accessTokenToReturn = (AccessToken) credentialsWithMatchingScopes.get(0);

        // Check it's not expired
        if (!isExpired(accessTokenToReturn)) {
            // Apparently we need an IdToken AND Account in order to return a fully formed AccessTokenCacheItem...
            // This code will not stay for long -- this API is mega weird...
            final List<Credential> idTokens = accountCredentialCache.getCredentialsFilteredBy(
                    accessTokenToReturn.getHomeAccountId(),
                    accessTokenToReturn.getEnvironment(),
                    CredentialType.IdToken,
                    accessTokenToReturn.getClientId(),
                    accessTokenToReturn.getRealm(),
                    null // wildcard (*), as IdTokens have no target
            );

            if (idTokens.isEmpty()) {
                // This shouldn't happen
                // TODO log a warning
            }

            if (idTokens.size() > 1) {
                // This shouldn't happen either
                // TODO log a warning
            }

            final IdToken idToken = (IdToken) idTokens.get(0);
            final AccessTokenCacheItem accessTokenCacheItem = new AccessTokenCacheItem(idToken, accessTokenToReturn);
            accessTokenCacheItem.setUser(user);
            return accessTokenCacheItem;
        }

        return null;
    }

    private boolean isExpired(final AccessToken accessToken) {
        final int DEFAULT_AT_EXPIRATION_BUFFER = 300;

        // Init a Calendar for the current time/date
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, DEFAULT_AT_EXPIRATION_BUFFER);
        final Date validity = calendar.getTime();

        // Init a Date for the accessToken's expiry
        long epoch = Long.valueOf(accessToken.getExpiresOn());
        final Date expiresOn = new Date(epoch * 1000);

        return expiresOn.before(validity);
    }

    private Set<String> normalizeLowerCase(final Set<String> inSet) {
        final Set<String> outSet = new HashSet<>();

        for (final String string : inSet) {
            outSet.add(string.toLowerCase());
        }

        return outSet;
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
     * Gets an immutable List of IAccounts for this app which have RefreshTokens in the cache.
     *
     * @param clientId    The current application.
     * @param environment The current environment.
     * @return An immutable List of IAccounts.
     */
    List<IAccount> getAccounts(final String environment, final String clientId) {
        // Create the result List
        final List<IAccount> accountsForThisApp = new ArrayList<>();

        // Grab a reference to the cache
        final IAccountCredentialCache accountCredentialCache = mCommonCache.getAccountCredentialCache();

        // Get all of the Accounts for this environment
        final List<Account> accountsForAuthority = accountCredentialCache.getAccountsFilteredBy(
                null, // homeAccountId
                environment,
                null // realm
        );

        // Declare a List to hold the MicrosoftStsAccounts
        final List<Account> microsoftStsAccounts = new ArrayList<>();

        // Iterate over the previous List of Accounts for this Authority, filtering by type
        for (final Account account : accountsForAuthority) {
            if (MicrosoftAccount.AUTHORITY_TYPE_V1_V2.equals(account.getAuthorityType())) {
                microsoftStsAccounts.add(account);
            }
        }

        // Grab the Credentials for this app...
        final List<Credential> appCredentials =
                accountCredentialCache.getCredentialsFilteredBy(
                        null, // homeAccountId
                        environment,
                        CredentialType.RefreshToken,
                        clientId,
                        null, // realm
                        null // target
                );

        // For each Account with an associated RT, add it to the result List...
        for (final Account account : microsoftStsAccounts) {
            if (accountHasToken(account, appCredentials)) {
                final com.microsoft.identity.client.Account acctToAdd = transform(account);
                acctToAdd.setCredentialPresent(true);
                accountsForThisApp.add(acctToAdd);
            }
        }

        return Collections.unmodifiableList(accountsForThisApp);
    }

    /**
     * Transforms the supplied {@link Account} into an equivalent
     * {@link com.microsoft.identity.client.Account}.
     *
     * @param accountIn The Account to transform.
     * @return An equivalent Account, acceptable for interapp exposure.
     */
    private com.microsoft.identity.client.Account transform(final Account accountIn) {
        final com.microsoft.identity.client.Account accountOut
                = new com.microsoft.identity.client.Account();

        // Populate fields...
        final IAccountId accountId;
        final IAccountId homeAccountId;

        if (MicrosoftAccount.AUTHORITY_TYPE_V1_V2.equals(accountIn.getAuthorityType())) {
            // This account came from AAD
            accountId = new AzureActiveDirectoryAccountId() {{ // This is the local_account_id
                setIdentifier(accountIn.getLocalAccountId());
                setObjectId(accountIn.getLocalAccountId());
                setTenantId(accountIn.getRealm()); // TODO verify this is the proper field...
            }};

            homeAccountId = new AzureActiveDirectoryAccountId() {{ // This is the home_account_id
                // Grab the homeAccountId
                final String homeAccountIdStr = accountIn.getHomeAccountId();

                // Split it into its constituent pieces <uid>.<utid>
                final String[] components = homeAccountIdStr.split("\\.");

                // Set the full string value as the identifier
                setIdentifier(homeAccountIdStr);

                // Set the uid as the objectId
                setObjectId(components[0]);

                // Set the utid as the tenantId
                setTenantId(components[1]);
            }};
        } else {
            accountId = new AccountId() {{
                setIdentifier(accountIn.getLocalAccountId());
            }};
            homeAccountId = new AccountId() {{
                setIdentifier(accountIn.getHomeAccountId());
            }};
        }

        accountOut.setAccountId(accountId);
        accountOut.setHomeAccountId(homeAccountId);
        accountOut.setUsername(accountIn.getUsername());

        return accountOut;
    }

    /**
     * Evaluates the supplied list of app credentials. Returns true if he provided Account
     * 'owns' any one of these tokens.
     *
     * @param account        The Account whose credential ownership should be evaluated.
     * @param appCredentials The Credentials to evaluate.
     * @return True, if this Account has Credentials. False otherwise.
     */
    private boolean accountHasToken(final Account account,
                                    final List<Credential> appCredentials) {
        final String accountHomeId = account.getHomeAccountId();
        final String accountEnvironment = account.getEnvironment();
        for (final Credential credential : appCredentials) {
            if (accountHomeId.equals(credential.getHomeAccountId())
                    && accountEnvironment.equals(credential.getEnvironment())) {
                return true;
            }
        }

        return false;
    }

    IAccount getAccount(
            final String environment,
            final String clientId,
            final String homeAccountId) {
        final List<IAccount> allAccounts = getAccounts(environment, clientId);

        for (final IAccount account : allAccounts) {
            if (homeAccountId.equals(account.getHomeAccountId().getIdentifier())) {
                return account;
            }
        }

        return null;
    }

    boolean removeCredentialsAndAccountForIAccount(
            final String environment,
            final String clientId,
            final IAccount account) {
        final String methodName = ":remove";

        final IAccount targetAccount;
        if (null == account
                || null == account.getHomeAccountId()
                || null == account.getHomeAccountId().getIdentifier()
                || null == (targetAccount =
                getAccount(
                        environment,
                        clientId,
                        account.getHomeAccountId().getIdentifier()
                ))) {
            return false;
        }

        // Remove this user's AccessToken, RefreshToken, IdToken, and Account entries
        int atsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.AccessToken,
                targetAccount
        );

        int rtsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.RefreshToken,
                targetAccount
        );

        int idsRemoved = removeCredentialsOfTypeForAccount(
                environment,
                clientId,
                CredentialType.IdToken,
                targetAccount
        );

        int acctsRemoved = removeAccount(
                environment,
                targetAccount
        );

        final String[][] logInfo = new String[][]{
                {"Access tokens", String.valueOf(atsRemoved)},
                {"Refresh tokens", String.valueOf(rtsRemoved)},
                {"Id tokens", String.valueOf(idsRemoved)},
                {"Accounts", String.valueOf(acctsRemoved)}
        };

        for (final String[] tuple : logInfo) {
            com.microsoft.identity.common.internal.logging.Logger.info(
                    TAG + methodName,
                    tuple[0] + " removed: [" + tuple[1] + "]"
            );
        }

        return acctsRemoved >= 1;
    }

    /**
     * Removes Credentials of the supplied type for the supplied Account.
     *
     * @param credentialType The type of Credential to remove.
     * @param targetAccount  The target Account whose Credentials should be removed.
     * @return The number of Credentials removed.
     */
    private int removeCredentialsOfTypeForAccount(
            @NonNull final String environment, // 'authority host'
            @NonNull final String clientId,
            @NonNull final CredentialType credentialType,
            @NonNull final IAccount targetAccount) {
        int credentialsRemoved = 0;

        // Get a reference to the cache
        final IAccountCredentialCache accountCredentialCache = mCommonCache.getAccountCredentialCache();

        // Query it for Credentials matching the supplied targetAccount
        final List<Credential> credentialsToRemove =
                accountCredentialCache.getCredentialsFilteredBy(
                        targetAccount.getHomeAccountId().getIdentifier(),
                        environment,
                        credentialType,
                        clientId,
                        null, // wildcard (*) realm
                        null // wildcard (*) target
                );

        for (final Credential credentialToRemove : credentialsToRemove) {
            if (accountCredentialCache.removeCredential(credentialToRemove)) {
                credentialsRemoved++;
            }
        }

        return credentialsRemoved;
    }

    /**
     * Removes Accounts matching the supplied criteria.
     *
     * @param environment   The authority host target.
     * @param targetAccount The designated account to delete.
     * @return The number of cache entries removed for the supplied criteria.
     */
    private int removeAccount(
            @NonNull final String environment,
            @NonNull final IAccount targetAccount) {
        int accountsRemoved = 0;

        // Grab a reference to the cache
        final IAccountCredentialCache accountCredentialCache = mCommonCache.getAccountCredentialCache();

        // Query it for a list of Accounts matching the supplied targetAccount
        final List<Account> accountsToRemove =
                accountCredentialCache.getAccountsFilteredBy(
                        targetAccount.getHomeAccountId().getIdentifier(),
                        environment,
                        null // wildcard (*) realm
                );

        for (final Account accountToRemove : accountsToRemove) {
            if (accountCredentialCache.removeAccount(accountToRemove)) {
                accountsRemoved++;
            }
        }

        return accountsRemoved;
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

}