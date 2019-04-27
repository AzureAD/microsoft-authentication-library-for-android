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
package com.microsoft.identity.client.profile;

import android.support.annotation.NonNull;
import android.util.Pair;

import com.microsoft.identity.common.internal.cache.ICacheRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.microsoft.identity.common.internal.util.StringUtil.getTenantInfo;

public class AccountAdapter {

    //@SuppressWarnings("unchecked")
    public static List<IAccount> adapt(final List<ICacheRecord> allCacheRecords) {

        final List<ICacheRecord> homeCacheRecords = filterHomeCacheRecords(
                allCacheRecords,
                false
        );

        final List<ICacheRecord> guestCacheRecords = filterHomeCacheRecords(
                allCacheRecords,
                true
        );

        final List<IAccount> accountList = createAccountRoots(homeCacheRecords);
        appendChildEntries(accountList, guestCacheRecords);

        return Collections.unmodifiableList(accountList);
    }

    private static void appendChildEntries(@NonNull final List<IAccount> accountList,
                                           @NonNull final List<ICacheRecord> guestCacheRecords) {
        for (final IAccount account : accountList) {
            final ITenantProfile homeTenantProfile = account.getTenantProfiles().get(0);

            final List<ICacheRecord> accountCacheRecords = filterRecordsMatchingHomeAccountId(
                    homeTenantProfile.getId() + "." + homeTenantProfile.getTenantId(),
                    guestCacheRecords
            );

            final List<ITenantProfile> profiles = new ArrayList<>(account.getTenantProfiles());

            for (final ICacheRecord accountCacheRecord : accountCacheRecords) {
                // Create a TenantProfile for each...
                final TenantProfile profile = new TenantProfile();
                profile.setIsHomeTenant(false);

                {
                    // TODO Figure out which IdToken to show...
                    profile.setIdToken(
                            accountCacheRecord
                                    .getIdToken()
                                    .getSecret()
                    );

                    profile.setAuthority(
                            accountCacheRecord
                                    .getIdToken()
                                    .getAuthority()
                    );
                }

                profiles.add(profile);
            }

            ((Account) account).setTenantProfiles(profiles);
        }
    }

    @NonNull
    private static List<ICacheRecord> filterRecordsMatchingHomeAccountId(
            @NonNull final String homeAccountId,
            @NonNull final List<ICacheRecord> guestCacheRecords) {
        final List<ICacheRecord> result = new ArrayList<>();

        for (final ICacheRecord cacheRecord : guestCacheRecords) {
            if (homeAccountId.equals(cacheRecord.getAccount().getHomeAccountId())) {
                result.add(cacheRecord);
            }
        }

        return result;
    }

    private static List<IAccount> createAccountRoots(
            @NonNull final List<ICacheRecord> homeCacheRecords) {
        final List<IAccount> result = new ArrayList<>();

        for (final ICacheRecord cacheRecord : homeCacheRecords) {
            final Pair<String, String> tenantInfo = getTenantInfo(
                    cacheRecord
                            .getAccount()
                            .getHomeAccountId()
            );

            final Account account = new Account();
            account.setId(
                    tenantInfo.first // The OID
            );

            final TenantProfile homeTenantProfile = new TenantProfile();
            homeTenantProfile.setIsHomeTenant(true);

            {
                /*
                if the client is MSAL, give them a v2 id token

                the only special affordance here is B2C which only yields v1 tokens (today)
                 */


                // TODO Figure out how to reconcile what we show here
                // TODO It may be either v1 or v2 (B2C / Broker)
                homeTenantProfile.setIdToken(
                        cacheRecord
                                .getIdToken()
                                .getSecret()
                ); // For now, just assume v2...

                homeTenantProfile.setAuthority(
                        cacheRecord
                                .getIdToken()
                                .getAuthority()
                );
            }

            final List<ITenantProfile> tenantProfiles = new ArrayList<>();
            tenantProfiles.add(homeTenantProfile);
            account.setTenantProfiles(tenantProfiles);

            result.add(account);
        }

        return result;
    }

    /**
     * Returns a subset of the supplied {@link ICacheRecord}s, returning those which are assoc.
     * to a home account.
     *
     * @param allCacheRecords The cache records to filter.
     * @param inverseMatch    Toggle inverse filtering.
     * @return A filtered input subset matching home accounts or, if inverseMatch == true, guest accounts.
     */
    @NonNull
    private static List<ICacheRecord> filterHomeCacheRecords(
            @NonNull final List<ICacheRecord> allCacheRecords, boolean inverseMatch) {
        final List<ICacheRecord> result = new ArrayList<>();

        for (final ICacheRecord cacheRecord : allCacheRecords) {
            final String acctHomeAccountId = cacheRecord.getAccount().getHomeAccountId();
            final String acctLocalAccountId = cacheRecord.getAccount().getLocalAccountId();

            if ((!inverseMatch && acctHomeAccountId.contains(acctLocalAccountId))
                    || (inverseMatch && !acctHomeAccountId.contains(acctLocalAccountId))) {
                result.add(cacheRecord);
            }
        }

        return result;
    }

}
