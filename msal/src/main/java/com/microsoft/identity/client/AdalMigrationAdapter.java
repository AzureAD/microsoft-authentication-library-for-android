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

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
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

public class AdalMigrationAdapter implements IMigrationAdapter<MicrosoftAccount, MicrosoftRefreshToken> {

    private static final String TAG = AdalMigrationAdapter.class.getSimpleName();
    private static final String MRRT_FLAG = "$y$";
    private static final String FOCI_FLAG = "$foci-";

    private final String mClientId;

    public AdalMigrationAdapter(@NonNull final String clientId) {
        if (StringExtensions.isNullOrBlank(clientId)) {
            throw new IllegalArgumentException("ClientId cannnot be null");
        }

        mClientId = clientId;
    }

    @Override
    public List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> adapt(Map<String, String> cacheItems) {
        final String methodName = ":adapt";
        final List<Pair<MicrosoftAccount, MicrosoftRefreshToken>> result = new ArrayList<>();

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

        return result;
    }

    private Map<String, Map<String, ADALTokenCacheItem>> segmentByClientId(@NonNull final Map<String, ADALTokenCacheItem> nativeCacheItems) {
        Map<String, Map<String, ADALTokenCacheItem>> result = new HashMap<>();

        for (final Map.Entry<String, ADALTokenCacheItem> entry : nativeCacheItems.entrySet()) {
            final String currentClientId = entry.getValue().getClientId();

            if (null == result.get(currentClientId)) {
                result.put(currentClientId, new HashMap<String, ADALTokenCacheItem>());
            }

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
                account = createAccount(refreshTokenPair.second);

                if (null != account) {
                    msRt = createMicrosoftRefreshToken(account, isFrt, refreshTokenPair);

                    if (null != msRt) {
                        result.add(new Pair<>(account, msRt));
                    }
                }
            }
        }

        return result;
    }

    private MicrosoftRefreshToken createMicrosoftRefreshToken(@NonNull final MicrosoftAccount account,
                                                              final boolean isFrt,
                                                              @NonNull final Pair<String, ADALTokenCacheItem> refreshTokenPair) {
        final String methodName = ":createMicrosoftRefreshToken";

        try {
            final String legacyCacheKey = refreshTokenPair.first;
            final String rawRt = refreshTokenPair.second.getRawIdToken();
            final String clientInfoStr = account.getClientInfo();
            final ClientInfo clientInfo = new ClientInfo(clientInfoStr);
            final String scope = "openid profile offline_access";
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

    private Pair<String, ADALTokenCacheItem> findRt(@NonNull final Map<String, ADALTokenCacheItem> cacheItems) {
        for (Map.Entry<String, ADALTokenCacheItem> entry : cacheItems.entrySet()) {
            if (null != entry.getValue().getRefreshToken()) {
                return new Pair<>(entry.getKey(), entry.getValue());
            }
        }

        return null;
    }

    private Pair<String, ADALTokenCacheItem> findMrrt(@NonNull final Map<String, ADALTokenCacheItem> cacheItems) {
        return findRtKeyVariant(MRRT_FLAG, cacheItems);
    }

    private Pair<String, ADALTokenCacheItem> findFrt(@NonNull final Map<String, ADALTokenCacheItem> cacheItems) {
        return findRtKeyVariant(FOCI_FLAG, cacheItems);
    }

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

    private Map<String, ADALTokenCacheItem> filterByEndpoint(@NonNull final List<String> commonEndpoints,
                                                             @NonNull final Map<String, ADALTokenCacheItem> nativeCacheItems) {
        final Map<String, ADALTokenCacheItem> result = new HashMap<>();

        for (final Map.Entry<String, ADALTokenCacheItem> cacheItemEntry : nativeCacheItems.entrySet()) {
            if (commonEndpoints.contains(cacheItemEntry.getValue().getAuthority())) {
                result.put(cacheItemEntry.getKey(), cacheItemEntry.getValue());
            }
        }

        return result;
    }

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
