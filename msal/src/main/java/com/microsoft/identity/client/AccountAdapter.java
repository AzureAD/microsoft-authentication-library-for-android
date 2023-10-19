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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

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

    private static class GuestAccountFilter implements CacheRecordFilter {

        @Override
        public List<ICacheRecord> filter(@NonNull List<ICacheRecord> records) {
            final List<ICacheRecord> result = new ArrayList<>();

            for (final ICacheRecord cacheRecord : records) {
                final String acctHomeAccountId = cacheRecord.getAccount().getHomeAccountId();
                final String acctLocalAccountId = cacheRecord.getAccount().getLocalAccountId();

                if (acctHomeAccountId != null && acctLocalAccountId != null && !acctHomeAccountId.contains(acctLocalAccountId)) {
                    result.add(cacheRecord);
                }
            }

            return result;
        }
    }

    /**
     * A filter for ICacheRecords that filters out home or guest accounts, based on its
     * constructor initialization.
     */
    private static class HomeAccountFilter implements CacheRecordFilter {

        @Override
        public List<ICacheRecord> filter(@NonNull final List<ICacheRecord> records) {
            final List<ICacheRecord> result = new ArrayList<>();

            for (final ICacheRecord cacheRecord : records) {
                final String acctHomeAccountId = cacheRecord.getAccount().getHomeAccountId();
                final String acctLocalAccountId = cacheRecord.getAccount().getLocalAccountId();
                if (acctLocalAccountId != null && acctHomeAccountId.contains(acctLocalAccountId)) {
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

            return !homeAccountIds.contains(guestAccountHomeAccountId);
        }

        @Override
        public List<ICacheRecord> filter(@NonNull final List<ICacheRecord> records) {
            final List<ICacheRecord> result = new ArrayList<>();

            // First, get the home accounts....
            final List<ICacheRecord> homeRecords =
                    filterCacheRecords(
                            records,
                            new HomeAccountFilter()
                    );

            // Then, get the guest accounts...
            final List<ICacheRecord> guestRecords =
                    filterCacheRecords(
                            records,
                            new GuestAccountFilter()
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
                new HomeAccountFilter()
        );

        // Then, get all of the guest accounts...
        // Note that the guestCacheRecordsWithNoHomeAccount (see below) will be *removed* from this
        // List.
        final List<ICacheRecord> guestCacheRecords = filterCacheRecords(
                allCacheRecords,
                new GuestAccountFilter()
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
                    null,
                    null // home tenant IdToken.... doesn't exist!
            );

            // Set the home oid & home tid of the root, even though we don't have the IdToken...
            // hooray for client_info
            emptyRoot.setId(StringUtil.getTenantInfo(entry.getKey()).getKey());
            emptyRoot.setTenantId(StringUtil.getTenantInfo(entry.getKey()).getValue());
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
                final TenantProfile profile = new TenantProfile(
                        // Intentionally do NOT supply the client info here.
                        // If client info is present, getId() will return the home tenant OID
                        // instead of the OID from the guest tenant.
                        null,
                        getIdToken(cacheRecord)
                );

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
                    final TenantProfile profile = new TenantProfile(
                            // Intentionally do NOT supply the client info here.
                            // If client info is present, getId() will return the home tenant OID
                            // instead of the OID from the guest tenant.
                            null,
                            getIdToken(guestRecord)
                    );
                    profile.setEnvironment(guestRecord.getAccount().getEnvironment());
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
            final IAccount rootAccount;
            rootAccount = new MultiTenantAccount(
                    // Because this is a home account, we'll supply the client info
                    // the uid value is the "id" of the account.
                    // For B2C, this value will contain the policy name appended to the OID.
                    homeCacheRecord.getAccount().getClientInfo(),
                    getIdToken(homeCacheRecord)
            );

            ((MultiTenantAccount) rootAccount).setHomeAccountId(
                    homeCacheRecord.getAccount().getHomeAccountId()
            );

            // Set the tenant_id
            ((MultiTenantAccount) rootAccount).setTenantId(
                    StringUtil.getTenantInfo(
                            homeCacheRecord
                                    .getAccount()
                                    .getHomeAccountId()
                    ).getValue()
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

    @Nullable
    private static IDToken getIdToken(@NonNull final ICacheRecord cacheRecord) {
        final String rawIdToken;

        if (null != cacheRecord.getIdToken()) {
            rawIdToken = cacheRecord.getIdToken().getSecret();
        } else if (null != cacheRecord.getV1IdToken()) {
            rawIdToken = cacheRecord.getV1IdToken().getSecret();
        } else {
            // We have no id_token for this account
            return null;
        }

        try {
            return new IDToken(rawIdToken);
        } catch (ServiceException e) {
            // This should never happen - the IDToken was verified when it was originally
            // returned from the service and saved.
            throw new IllegalStateException("Failed to restore IdToken");
        }
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
        final String methodTag = TAG + ":getAccountInternal";
        final AccountRecord accountToReturn;

        if (!StringUtil.isNullOrEmpty(homeAccountIdentifier)) {
            accountToReturn = oAuth2TokenCache.getAccount(
                    null, // * wildcard
                    clientId,
                    homeAccountIdentifier,
                    realm
            );
        } else {
            Logger.warn(methodTag, "homeAccountIdentifier was null or empty -- invalid criteria");
            accountToReturn = null;
        }

        return accountToReturn;
    }

}
