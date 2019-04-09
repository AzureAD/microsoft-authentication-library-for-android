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

import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.common.internal.util.StringUtil.getTenantInfo;

public class AccountAdapter {

    private final String mClientId;
    private final OAuth2TokenCache mOAuth2TokenCache;

    AccountAdapter(@NonNull final OAuth2TokenCache cache,
                   @NonNull final String clientId) {
        mClientId = clientId;
        mOAuth2TokenCache = cache;
    }

    @SuppressWarnings("unchecked")
    public List<Account> adapt(final List<AccountRecord> allAccountRecords) {
        final List<Account> result = new ArrayList<>();

        // First, bucket the AccountRecords on the home_account_id to remove duplicates
        final Map<String, AccountRecord> uniquePrinciples = new HashMap<>();

        for (final AccountRecord record : allAccountRecords) {
            uniquePrinciples.put(record.getHomeAccountId(), record);
        }

        for (final AccountRecord record : uniquePrinciples.values()) {
            final Account account = new Account();
            account.setId(getTenantInfo(record.getHomeAccountId()).first);
            account.setTenantProfiles(new ArrayList<ITenantProfile>());

            final List<IdTokenRecord> idTokens = mOAuth2TokenCache.getIdTokensForAccount(
                    mClientId,
                    record
            );

            final List<ITenantProfile> tenantProfiles = new ArrayList<>();

            for (final IdTokenRecord idTokenRecord : idTokens) {
                final TenantProfile tenantProfile = new TenantProfile();
                tenantProfile.setIdToken(idTokenRecord.getSecret());
                tenantProfile.setTenantId(idTokenRecord.getRealm());
                tenantProfile.setAuthority(idTokenRecord.getAuthority());

                // TODO Freemium
                // TODO isHomeTenant

                tenantProfiles.add(tenantProfile);
            }

            // Add the newly created TenantProfile to the Account
            account.setTenantProfiles(tenantProfiles);
        }

        return result;
    }

}
