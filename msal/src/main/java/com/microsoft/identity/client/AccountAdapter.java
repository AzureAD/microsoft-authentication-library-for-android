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
import android.support.annotation.Nullable;

import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AccountAdapter {

    private static final String TAG = AccountAdapter.class.getSimpleName();

    /**
     * Abstract class representing a filter for ICacheRecords.
     */
    private interface CacheRecordFilter {

        List<ICacheRecord> filter(@NonNull final List<ICacheRecord> records);
    }

    /**
     * A filter for ICacheRecords that filters out home or guest accounts, based on its
     * constructor initialization.
     */
    private static class HomeAccountFilter implements CacheRecordFilter {

        private final boolean mInverseMatch;

        /**
         * Creates a new HomeAccountFilter.
         *
         * @param inverseMatch If true, only guest accounts will be returned. If false, only home.
         */
        HomeAccountFilter(final boolean inverseMatch) {
            mInverseMatch = inverseMatch;
        }

        @Override
        public List<ICacheRecord> filter(@NonNull final List<ICacheRecord> records) {
            final List<ICacheRecord> result = new ArrayList<>();

            for (final ICacheRecord cacheRecord : records) {
                final String acctHomeAccountId = cacheRecord.getAccount().getHomeAccountId();
                final String acctLocalAccountId = cacheRecord.getAccount().getLocalAccountId();

                if ((!mInverseMatch && acctHomeAccountId.contains(acctLocalAccountId))
                        || (mInverseMatch && !acctHomeAccountId.contains(acctLocalAccountId))) {
                    result.add(cacheRecord);
                }
            }

            return result;
        }
    }

    /**
     * A filter which finds guest accounts which have no corresponding home tenant account.
     */
    private static final CacheRecordFilter guestAccountsWithNoHomeTenantAccountFilter = new CacheRecordFilter() {

        private boolean hasNoCorrespondingHomeAccount(@NonNull final ICacheRecord guestRecord,
                                                      @NonNull final List<ICacheRecord> homeRecords) {
            // Init our sought value
            final String guestAccountHomeAccountId = guestRecord.getAccount().getHomeAccountId();

            // Create a List of home_account_ids from the homeRecords...
            final List<String> homeAccountIds = new ArrayList<String>() {{
                for (final ICacheRecord cacheRecord : homeRecords) {
                    add(cacheRecord.getAccount().getHomeAccountId());
                }
            }};

            return homeAccountIds.contains(guestAccountHomeAccountId);
        }

        @Override
        public List<ICacheRecord> filter(@NonNull final List<ICacheRecord> records) {
            final List<ICacheRecord> result = new ArrayList<>();

            // First, get the home accounts....
            final List<ICacheRecord> homeRecords =
                    filterCacheRecords(
                            records,
                            new HomeAccountFilter(false)
                    );

            // Then, get the guest accounts...
            final List<ICacheRecord> guestRecords =
                    filterCacheRecords(
                            records,
                            new HomeAccountFilter(true)
                    );

            // Iterate over the guest accounts and find those which have no associated home account
            for (final ICacheRecord guestRecord : guestRecords) {
                if (hasNoCorrespondingHomeAccount(guestRecord, homeRecords)) {
                    result.add(guestRecord);
                }
            }

            return result;
        }
    };

    /**
     * For a supplied List of ICacheRecords, create each root IAccount based on the home
     * account and then add child-nodes based on any authorized tenants.
     *
     * @param allCacheRecords
     * @return
     */
    @NonNull
    static List<IAccount> adapt(@NonNull final List<ICacheRecord> allCacheRecords) {
        // First, get all of the ICacheRecords for home accounts...
        final List<ICacheRecord> homeCacheRecords = filterCacheRecords(
                allCacheRecords,
                new HomeAccountFilter(false)
        );

        // Then, get all of the guest accounts...
        // Note that the guestCacheRecordsWithNoHomeAccount (see below) will be *removed* from this
        // List.
        final List<ICacheRecord> guestCacheRecords = filterCacheRecords(
                allCacheRecords,
                new HomeAccountFilter(true)
        );

        // Get the guest cache records which have no homeAccount
        final List<ICacheRecord> guestCacheRecordsWithNoHomeAccount = filterCacheRecords(
                allCacheRecords,
                guestAccountsWithNoHomeTenantAccountFilter
        );

        // Remove the guest records that have no corresponding home account from the complete list of
        // guest records...
        guestCacheRecords.removeAll(guestCacheRecordsWithNoHomeAccount);

        final List<IAccount> rootAccounts = createRootAccounts(homeCacheRecords);
        appendChildren(rootAccounts, guestCacheRecords);
        rootAccounts.addAll(
                createIAccountsForGuestsNotSignedIntoHomeTenant(guestCacheRecordsWithNoHomeAccount)
        );

        return rootAccounts;
    }

    @NonNull
    private static List<IAccount> createIAccountsForGuestsNotSignedIntoHomeTenant(
            @NonNull final List<ICacheRecord> guestCacheRecords) {
        // First, bucket the records by homeAccountId to create affinities
        final Map<String, List<ICacheRecord>> bucketedRecords = new HashMap<>();

        for (final ICacheRecord cacheRecord : guestCacheRecords) {
            final String cacheRecordHomeAccountId = cacheRecord.getAccount().getHomeAccountId();

            // Initialize the multi-map
            if (null == bucketedRecords.get(cacheRecordHomeAccountId)) {
                bucketedRecords.put(cacheRecordHomeAccountId, new ArrayList<ICacheRecord>());
            }

            // Add the record to the multi-map
            bucketedRecords.get(cacheRecordHomeAccountId).add(cacheRecord);
        }

        // Declare our result holder...
        final List<IAccount> result = new ArrayList<>();

        // Now that all of the tokens have been bucketed by an account affinity
        // box those into a 'rootless' IAccount
        for (final Map.Entry<String, List<ICacheRecord>> entry : bucketedRecords.entrySet()) {
            // Create our empty root...
            final MultiTenantAccount emptyRoot = new MultiTenantAccount(
                    null // home tenant IdToken.... doesn't exist!
            );

            // Set the home oid & home tid of the root, even though we don't have the IdToken...
            // hooray for client_info
            emptyRoot.setId(StringUtil.getTenantInfo(entry.getKey()).first);
            emptyRoot.setTenantId(StringUtil.getTenantInfo(entry.getKey()).second);
            emptyRoot.setEnvironment( // Look ahead into our CacheRecords to determine the environment
                    entry
                            .getValue()
                            .get(0)
                            .getAccount()
                            .getEnvironment()
            );

            // Create the Map of TenantProfiles to set...
            final Map<String, ITenantProfile> tenantProfileMap = new HashMap<>();

            for (final ICacheRecord cacheRecord : entry.getValue()) {
                final String tenantId = cacheRecord.getAccount().getRealm();
                final String rawIdToken = getIdToken(cacheRecord);
                final TenantProfile profile = new TenantProfile(rawIdToken);

                tenantProfileMap.put(tenantId, profile);
            }

            emptyRoot.setTenantProfiles(tenantProfileMap);
            result.add(emptyRoot);
        }

        return result;
    }

    private static void appendChildren(@NonNull final List<IAccount> rootAccounts,
                                       @NonNull final List<ICacheRecord> guestCacheRecords) {
        // Iterate over the roots, adding the children as we go...
        for (final IAccount account : rootAccounts) {
            // Iterate over the potential children, adding them if they match
            final Map<String, ITenantProfile> tenantProfiles = new HashMap<>();

            for (final ICacheRecord guestRecord : guestCacheRecords) {
                final String guestRecordHomeAccountId = guestRecord.getAccount().getHomeAccountId();

                if (guestRecordHomeAccountId.contains(account.getId())) {
                    final TenantProfile profile = new TenantProfile(getIdToken(guestRecord));
                    tenantProfiles.put(guestRecord.getAccount().getRealm(), profile);
                }
            }

            // Cast the root account for initialization...
            final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) account;
            multiTenantAccount.setTenantProfiles(tenantProfiles);
        }
    }

    @NonNull
    private static List<IAccount> createRootAccounts(
            @NonNull final List<ICacheRecord> homeCacheRecords) {
        final List<IAccount> result = new ArrayList<>();

        for (ICacheRecord homeCacheRecord : homeCacheRecords) {
            // Each IAccount will be initialized as a MultiTenantAccount whether it really is or not...
            // This allows us to cast the results however the caller sees fit...
            final IAccount rootAccount = new MultiTenantAccount(getIdToken(homeCacheRecord));

            // Set the tenant_id
            ((MultiTenantAccount) rootAccount).setTenantId(
                    StringUtil.getTenantInfo(
                            homeCacheRecord
                                    .getAccount()
                                    .getHomeAccountId()
                    ).second
            );

            // Set the environment...
            ((MultiTenantAccount) rootAccount).setEnvironment(
                    homeCacheRecord
                            .getAccount()
                            .getEnvironment()
            );

            result.add(rootAccount);
        }

        return result;
    }

    @NonNull
    private static String getIdToken(@NonNull final ICacheRecord cacheRecord) {
        return null != cacheRecord.getIdToken()
                ? cacheRecord.getIdToken().getSecret()
                : cacheRecord.getV1IdToken().getSecret();
    }

    /**
     * Filters ICacheRecords based on the criteria specified by the {@link CacheRecordFilter}.
     * Results may be empty, but never null.
     *
     * @param allCacheRecords The ICacheRecords to inspect.
     * @param filter          The CacheRecordFilter to consult.
     * @return A List of ICacheRecords matching the supplied filter criteria.
     */
    @NonNull
    private static List<ICacheRecord> filterCacheRecords(
            @NonNull final List<ICacheRecord> allCacheRecords,
            @NonNull final CacheRecordFilter filter) {
        return filter.filter(allCacheRecords);
    }

    @Nullable
    static AccountRecord getAccountInternal(@NonNull final String clientId,
                                            @NonNull OAuth2TokenCache oAuth2TokenCache,
                                            @NonNull final String homeAccountIdentifier,
                                            @Nullable final String realm) {
        final AccountRecord accountToReturn;

        if (!StringUtil.isEmpty(homeAccountIdentifier)) {
            accountToReturn = oAuth2TokenCache.getAccount(
                    null, // * wildcard
                    clientId,
                    homeAccountIdentifier,
                    realm
            );
        } else {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG,
                    "homeAccountIdentifier was null or empty -- invalid criteria"
            );
            accountToReturn = null;
        }

        return accountToReturn;
    }

}
