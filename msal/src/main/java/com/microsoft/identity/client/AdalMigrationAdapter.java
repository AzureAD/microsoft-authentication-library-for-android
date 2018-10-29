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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.cache.ADALTokenCacheItem;
import com.microsoft.identity.common.internal.cache.ADALUserInfo;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectory;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryAccount;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts tokens from the ADAL cache format to the MSAL (common schema) format.
 */
public class AdalMigrationAdapter implements IMigrationAdapter<MicrosoftAccount, MicrosoftRefreshToken> {

    /**
     * Object lock to prevent multiple threads from running migration simultaneously.
     */
    private static final Object sLock = new Object();

    /**
     * The log tag of this class.
     */
    private static final String TAG = AdalMigrationAdapter.class.getSimpleName();

    /**
     * The cache-key component, unique to MRRT tokens.
     */
    private static final String MRRT_FLAG = "$y$";

    /**
     * The cache-key component, unique to FOCI tokens.
     */
    private static final String FOCI_FLAG = "$foci-";

    /**
     * The name of the SharedPreferences file used by this class for tracking migration state.
     */
    private static final String MIGRATION_STATUS_SHARED_PREFERENCES =
            "com.microsoft.identity.client.migration_status";

    /**
     * The migration-state cache-key used to persist/check whether or not migration has occurred or not.
     */
    private static final String KEY_MIGRATION_STATUS = "adal-migration-complete";

    /**
     * The SharedPreferences used to tracking migration state.
     */
    private final SharedPreferences mSharedPrefs;

    /**
     * Force-override to initiate migration, even if it's already happened before.
     */
    private final boolean mForceMigration;

    /**
     * Constructs a new AdalMigrationAdapter.
     *
     * @param context Context used to track migration state.
     * @param force   Force migration to occur, even if it has run before.
     */
    public AdalMigrationAdapter(@Nullable final Context context,
                                final boolean force) {
        mSharedPrefs = null != context
                ? context.getSharedPreferences(MIGRATION_STATUS_SHARED_PREFERENCES, Context.MODE_PRIVATE)
                : null;
        mForceMigration = force;
    }

    @Override
    public List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> adapt(Map<String, String> cacheItems) {
        final String methodName = ":adapt";
        final List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> result = new ArrayList<>();

        synchronized (sLock) { // To prevent multiple threads from potentially running migration
            final boolean hasMigrated = getMigrationStatus();

            if (!hasMigrated && !mForceMigration) {
                // Initialize the InstanceDiscoveryMetadata so we know about all the clouds and possible /common endpoints
                final boolean cloudMetadataLoaded = loadCloudDiscoveryMetadata();

                if (cloudMetadataLoaded) {
                    final List<String> commonEndpoints = getCommonEndpoints();
                    Logger.verbose(
                            TAG + methodName,
                            "Identified [" + commonEndpoints.size() + "] common endpoints"
                    );

                    // Convert the JSON to native ADALTokenCacheItem representation, original keys used to key the Map
                    Map<String, ADALTokenCacheItem> nativeCacheItems = deserialize(cacheItems);
                    nativeCacheItems = filterByEndpoint(commonEndpoints, nativeCacheItems);
                    Logger.verbose(
                            TAG + methodName,
                            "Found [" + nativeCacheItems.size() + "] common tokens."
                    );

                    // Split these by clientId, key is client id - secondary key is original TKI key
                    Map<String, Map<String, ADALTokenCacheItem>> nativeCacheItemByClientId = segmentByClientId(nativeCacheItems);

                    for (final Map.Entry<String, Map<String, ADALTokenCacheItem>> entry : nativeCacheItemByClientId.entrySet()) {
                        final Map<String, ADALTokenCacheItem> tokensForClientId = entry.getValue();
                        result.addAll(selectTokensByUser(segmentByUser(tokensForClientId)));
                    }
                }

                // Update the migrated status
                setMigrationStatus(true);
            }
        }

        return result;
    }

    /**
     * Sets the migration-state in the SharedPreferences file.
     *
     * @param hasMigrated The status to set.
     */
    @SuppressLint("ApplySharedPref")
    void setMigrationStatus(boolean hasMigrated) {
        mSharedPrefs.edit().putBoolean(KEY_MIGRATION_STATUS, hasMigrated).commit();
    }

    /**
     * Gets the migration-state from the SharedPreferences file.
     *
     * @return True, if migration has already happened. False otherwise.
     */
    private boolean getMigrationStatus() {
        return mSharedPrefs.getBoolean(KEY_MIGRATION_STATUS, false);
    }

    /**
     * Splits the provided credentials into a Map, keyed on the clientId of the application to which
     * they are associated.
     *
     * @param nativeCacheItems The cache items to inspect.
     * @return A Map of the provided Credentials, keyed by clientId.
     */
    private Map<String, Map<String, ADALTokenCacheItem>> segmentByClientId(@NonNull final Map<String, ADALTokenCacheItem> nativeCacheItems) {
        // Declare a List to hold the result
        Map<String, Map<String, ADALTokenCacheItem>> result = new HashMap<>();

        // Iterate over the supplied entries
        for (final Map.Entry<String, ADALTokenCacheItem> entry : nativeCacheItems.entrySet()) {
            // Grab the clientId of the current credential.
            final String currentClientId = entry.getValue().getClientId();

            // If this is the first time we have seen it, create a secondary Map for the destination
            // of this credential
            if (null == result.get(currentClientId)) {
                result.put(currentClientId, new HashMap<String, ADALTokenCacheItem>());
            }

            // Add the current credential to the token map
            result.get(currentClientId).put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private Collection<? extends Pair<MicrosoftAccount, MicrosoftRefreshToken>> selectTokensByUser(@NonNull final Map<String, Map<String, ADALTokenCacheItem>> cacheItemsByUniqueId) {
        final List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> result = new ArrayList<>();

        for (final Map.Entry<String, Map<String, ADALTokenCacheItem>> entry : cacheItemsByUniqueId.entrySet()) {
            MicrosoftAccount account;
            MicrosoftRefreshToken msRt;
            Pair<String, ADALTokenCacheItem> refreshTokenPair = null;
            boolean isFrt = true;

            // Try to find the FRT
            refreshTokenPair = findFrt(entry.getValue());

            // If no FRT found, try to find MRRT
            if (null == refreshTokenPair) {
                isFrt = false;
                refreshTokenPair = findMrrt(entry.getValue());
            }

            // If no MRRT found, fallback to RT
            if (null == refreshTokenPair) {
                refreshTokenPair = findRt(entry.getValue());
            }

            if (null != refreshTokenPair) {
                // Create the account to 'own' this RT
                account = createAccount(refreshTokenPair.second);

                if (null != account) {
                    // If we were able to create the account, create the associated RT
                    msRt = createMicrosoftRefreshToken(account, isFrt, refreshTokenPair);

                    if (null != msRt) {
                        // If we have the account and RT, we're done. Move on...
                        result.add(new Pair<>(account, msRt));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Creates a {@link MicrosoftRefreshToken} from the supplied credential data.
     *
     * @param account          The account which will 'own' this token.
     * @param isFrt            True, if the supplied token is an FRT. False otherwise.
     * @param refreshTokenPair The 'legacy' format pair -- the key & value.
     * @return The resulting MicrosoftRefreshToken.
     */
    private MicrosoftRefreshToken createMicrosoftRefreshToken(@NonNull final MicrosoftAccount account,
                                                              final boolean isFrt,
                                                              @NonNull final Pair<String, ADALTokenCacheItem> refreshTokenPair) {
        final String methodName = ":createMicrosoftRefreshToken";

        try {
            final String legacyCacheKey = refreshTokenPair.first;
            final String rawRt = refreshTokenPair.second.getRefreshToken();
            final String clientInfoStr = account.getClientInfo();
            final ClientInfo clientInfo = new ClientInfo(clientInfoStr);
            final String scope = "openid profile offline_access"; // default scopes
            final String clientId = refreshTokenPair.second.getClientId();
            final String environment = new URL(refreshTokenPair.second.getAuthority()).getHost();
            String familyId = null;

            if (isFrt) {
                familyId = legacyCacheKey.substring(
                        legacyCacheKey.lastIndexOf(FOCI_FLAG),
                        legacyCacheKey.length()
                );

                familyId = familyId.replace(FOCI_FLAG, "");
            }

            return new MicrosoftRefreshToken(
                    rawRt,
                    clientInfo,
                    scope,
                    clientId,
                    isFrt,
                    environment,
                    familyId
            );
        } catch (ServiceException | MalformedURLException e) {
            final String errMsg = "Failed to create RefreshToken";
            Logger.error(
                    TAG + methodName,
                    errMsg,
                    null
            );
            Logger.errorPII(
                    TAG + methodName,
                    errMsg,
                    e
            );
            return null;
        }
    }

    /**
     * Creates a {@link MicrosoftAccount} from the supplied {@link ADALTokenCacheItem}.
     *
     * @param refreshToken The credential used to derive the new account.
     * @return The newly created MicrosoftAccount.
     */
    private MicrosoftAccount createAccount(@NonNull final ADALTokenCacheItem refreshToken) {
        final String methodName = ":createAccount";
        try {
            final String rawIdToken = refreshToken.getRawIdToken();
            final String uid = refreshToken.getUserInfo().getUserId();
            final String utid = refreshToken.getTenantId();
            final String environment = new URL(refreshToken.getAuthority()).getHost();

            JsonObject clientInfo = new JsonObject();
            clientInfo.addProperty("uid", uid);
            clientInfo.addProperty("utid", utid);

            final String clientInfoJson = clientInfo.toString();
            final String base64EncodedClientInfo = new String(Base64.encode(clientInfoJson.getBytes(), 0));
            final ClientInfo clientInfoObj = new ClientInfo(base64EncodedClientInfo);
            final IDToken idToken = new IDToken(rawIdToken);

            AzureActiveDirectoryAccount account = new AzureActiveDirectoryAccount(idToken, clientInfoObj);
            account.setEnvironment(environment);

            return account;
        } catch (MalformedURLException | ServiceException e) {
            final String errorMsg = "Failed to create Account";
            Logger.error(
                    TAG + methodName,
                    errorMsg,
                    null
            );
            Logger.errorPII(
                    TAG + methodName,
                    errorMsg,
                    e
            );
            return null;
        }
    }

    /**
     * Selects a non-null RT from the supplied cache items. Order is nondeterministic.
     *
     * @param cacheItems The cache items to inspect.
     * @return A non-null RT and its associated key.
     */
    private Pair<String, ADALTokenCacheItem> findRt(@NonNull final Map<String, ADALTokenCacheItem> cacheItems) {
        for (Map.Entry<String, ADALTokenCacheItem> entry : cacheItems.entrySet()) {
            if (null != entry.getValue().getRefreshToken()) {
                return new Pair<>(entry.getKey(), entry.getValue());
            }
        }

        return null;
    }

    /**
     * Selects a non-null MRRT from the supplied cache items. Order is nondeterministic.
     *
     * @param cacheItems The cache items to inspect.
     * @return A non-null RT and its associated key.
     */
    private Pair<String, ADALTokenCacheItem> findMrrt(@NonNull final Map<String, ADALTokenCacheItem> cacheItems) {
        return findRtKeyVariant(MRRT_FLAG, cacheItems);
    }

    /**
     * Selects a non-null FRT from the supplied cache items. Order is nondeterministic.
     *
     * @param cacheItems The cache items to inspect.
     * @return A non-null RT and its associated key.
     */
    private Pair<String, ADALTokenCacheItem> findFrt(@NonNull final Map<String, ADALTokenCacheItem> cacheItems) {
        return findRtKeyVariant(FOCI_FLAG, cacheItems);
    }

    /**
     * Utility method for finding various token types, based on key metadata.
     *
     * @param keyVariant The key component used to search the supplied cache items.
     * @param cacheItems The cache items to inspect.
     * @return A refresh token matching the specified criteria (and its key).
     */
    private Pair<String, ADALTokenCacheItem> findRtKeyVariant(@NonNull final String keyVariant,
                                                              @NonNull final Map<String, ADALTokenCacheItem> cacheItems) {
        for (Map.Entry<String, ADALTokenCacheItem> entry : cacheItems.entrySet()) {
            if (entry.getKey().contains(keyVariant)) {
                return new Pair<>(entry.getKey(), entry.getValue());
            }
        }

        return null;
    }

    /**
     * Returns a copy of the supplied native cache items, keyed by the oid of the account to which
     * those entries will be associated.
     *
     * @param nativeCacheItems The cache items to inspect.
     * @return The supplied cache items, keyed by oid.
     */
    private Map<String, Map<String, ADALTokenCacheItem>> segmentByUser(@NonNull final Map<String, ADALTokenCacheItem> nativeCacheItems) {
        final Map<String, Map<String, ADALTokenCacheItem>> result = new HashMap<>();

        ADALTokenCacheItem currentTokenCacheItem;
        ADALUserInfo currentUserInfo;
        for (final Map.Entry<String, ADALTokenCacheItem> entry : nativeCacheItems.entrySet()) {
            currentTokenCacheItem = entry.getValue();
            currentUserInfo = currentTokenCacheItem.getUserInfo();

            if (null == result.get(currentUserInfo.getUserId())) {
                result.put(currentUserInfo.getUserId(), new HashMap<String, ADALTokenCacheItem>());
            }

            result.get(currentUserInfo.getUserId()).put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * Filters the supplied list of credentials relative to a List of supplied endpoints.
     *
     * @param endpoints        The endpoints to search for.
     * @param nativeCacheItems The credentials to inspect.
     * @return The filtered credential Map.
     */
    private Map<String, ADALTokenCacheItem> filterByEndpoint(@NonNull final List<String> endpoints,
                                                             @NonNull final Map<String, ADALTokenCacheItem> nativeCacheItems) {
        final Map<String, ADALTokenCacheItem> result = new HashMap<>();

        for (final Map.Entry<String, ADALTokenCacheItem> cacheItemEntry : nativeCacheItems.entrySet()) {
            if (endpoints.contains(cacheItemEntry.getValue().getAuthority())) {
                result.put(cacheItemEntry.getKey(), cacheItemEntry.getValue());
            }
        }

        return result;
    }

    /**
     * Converts the supplied Map of key/value JSON credentials into a Map of key/POJO.
     *
     * @param tokenCacheItems The credentials to inspect.
     * @return The deserialized credentials and their associated keys.
     */
    private Map<String, ADALTokenCacheItem> deserialize(final Map<String, String> tokenCacheItems) {
        final Map<String, ADALTokenCacheItem> result = new HashMap<>();

        final Gson gson = new Gson();
        for (final Map.Entry<String, String> entry : tokenCacheItems.entrySet()) {
            result.put(
                    entry.getKey(),
                    gson.fromJson(entry.getValue(), ADALTokenCacheItem.class)
            );
        }

        return result;
    }

    /**
     * Loads the comprehensive list of 'common' endpoints, based on the loaded InstanceDiscoveryMetadata.
     *
     * @return The complete list of known common endpoints.
     */
    private List<String> getCommonEndpoints() {
        final String protocol = "https://";
        final String pathSeparator = "/";
        final String commonPathSegment = "common";

        // List of our result endpoints...
        final List<String> commonEndpoints = new ArrayList<>();

        // Declare a List to hold the associated Cloud instances
        final List<AzureActiveDirectoryCloud> clouds = AzureActiveDirectory.getClouds();

        String commonHostAlias;
        for (final AzureActiveDirectoryCloud cloud : clouds) {
            for (final String hostAlias : cloud.getHostAliases()) {
                commonHostAlias = hostAlias;

                if (!commonHostAlias.startsWith(protocol)) {
                    commonHostAlias = protocol + commonHostAlias;
                }

                if (!commonHostAlias.endsWith(pathSeparator)) {
                    commonHostAlias = commonHostAlias + pathSeparator;
                }

                if (!commonHostAlias.endsWith(commonPathSegment)) {
                    commonHostAlias = commonHostAlias + commonPathSegment;
                }

                commonEndpoints.add(commonHostAlias);
            }
        }

        return commonEndpoints;
    }

    /**
     * Loads the InstanceDiscoveryMetadata.
     *
     * @return True, if the metadata loads successfully. False otherwise.
     */
    private static boolean loadCloudDiscoveryMetadata() {
        final String methodName = ":loadCloudDiscoveryMetadata";
        boolean succeeded = true;

        if (!AzureActiveDirectory.isInitialized()) {
            try {
                AzureActiveDirectory.performCloudDiscovery();
            } catch (IOException e) {
                Logger.error(
                        TAG + methodName,
                        "Failed to load instance discovery metadata",
                        e
                );
                succeeded = false;
            }
        }

        return succeeded;
    }
}
