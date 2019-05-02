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
import com.microsoft.identity.common.internal.dto.IdTokenRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.microsoft.identity.common.internal.util.StringUtil.getTenantInfo;

public class AccountAdapter {

    /**
     * Given a List of ICacheRecords, adapt it into a List of IAccounts with corresponding
     * TenantProfiles for each tenant against which a given account has authz'd.
     *
     * @param allCacheRecords The ICacheRecords to inspect.
     * @return A List of IAccounts derived therefrom.
     */
    public static List<IAccount> adapt(final List<ICacheRecord> allCacheRecords) {
        // First, get all of the home accounts
        final List<ICacheRecord> homeCacheRecords = filterHomeCacheRecords(
                allCacheRecords,
                false
        );

        // Then, get all of the guest accounts
        final List<ICacheRecord> guestCacheRecords = filterHomeCacheRecords(
                allCacheRecords,
                true
        );

        // Create the top-level (the "root") IAccount object based on the home accounts
        final List<IAccount> accountList = createAccountRoots(homeCacheRecords);

        // Iterate over the guest accounts, adding the guest profiles as children of their
        // corresponding root. Root correspondence is determined based on home_account_id.
        // home_account_id is a synthetic property derived from <uid>.<utid>
        appendChildEntries(accountList, guestCacheRecords);

        // Finally, return our result as an immutable List
        return Collections.unmodifiableList(accountList);
    }

    /**
     * Given a List of IAccounts and an List of guest-account ICacheRecords, transform the
     * ICacheRecords into TenantProfiles and append them to IAccount's List of TenantProfiles.
     * By convention, the first TenantProfile in the IAccount's List should be the home profile.
     *
     * @param accountList       The List of IAccounts to which any corresponding TenantProfiles
     *                          should be added.
     * @param guestCacheRecords The guest-account ICacheRecords to inspect.
     */
    private static void appendChildEntries(@NonNull final List<IAccount> accountList,
                                           @NonNull final List<ICacheRecord> guestCacheRecords) {
        for (final IAccount account : accountList) {
            final ITenantProfile homeTenantProfile = account.getTenantProfiles().get(0);
            final String homeTenantOid = homeTenantProfile.getId();

            final List<ICacheRecord> accountCacheRecords = filterRecordsMatchingHomeAccountId(
                    homeTenantOid + "." + homeTenantProfile.getTenantId(),
                    guestCacheRecords
            );

            final List<ITenantProfile> profiles = new ArrayList<>(account.getTenantProfiles());

            for (final ICacheRecord accountCacheRecord : accountCacheRecords) {
                // Create a TenantProfile for each...
                final TenantProfile profile = new TenantProfile();

                // If the oid component of the home_account_id matches the local_account_id
                // then this is the home profile.
                profile.setIsHomeTenant(
                        homeTenantOid.equals(
                                accountCacheRecord
                                        .getAccount()
                                        .getLocalAccountId()
                        ) // should always equal false, but showing work anyway...
                );

                final IdTokenRecord idToken = getIdTokenRecord(accountCacheRecord);

                profile.setIdToken(idToken.getSecret());
                profile.setAuthority(idToken.getAuthority());

                profiles.add(profile);
            }

            ((Account) account).setTenantProfiles(profiles);
        }
    }

    /**
     * Given an {@link ICacheRecord}, return its associated IdToken. Preference is for v2 IdToken.
     *
     * @param accountCacheRecord The ICacheRecord to inspect.
     * @return The IdToken contained therein.
     */
    @NonNull
    private static IdTokenRecord getIdTokenRecord(@NonNull final ICacheRecord accountCacheRecord) {
        return null != accountCacheRecord.getIdToken()
                ? accountCacheRecord.getIdToken() // Prefer the v2 idtoken since this is MSAL...
                : accountCacheRecord.getV1IdToken();
    }

    /**
     * Given a home_account_id and List of {@link ICacheRecord}s, return those records which
     * correspond to it.
     *
     * @param homeAccountId     The home_account_id to match.
     * @param guestCacheRecords The ICacheRecords to inspect.
     * @return A List of ICacheRecords filtered by home_account_id. List can be empty, but never null.
     */
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

    /**
     * Given a List of home-account ICacheRecords, create the top-level IAccount objects.
     *
     * @param homeCacheRecords A List of home-account ICacheRecords.
     * @return A List of IAccounts derived from the supplied ICacheRecords.
     */
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

            final IdTokenRecord idToken = getIdTokenRecord(cacheRecord);
            homeTenantProfile.setIdToken(idToken.getSecret());
            homeTenantProfile.setAuthority(idToken.getAuthority());

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
