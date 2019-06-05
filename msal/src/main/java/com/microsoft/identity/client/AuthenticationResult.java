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

import com.microsoft.identity.common.internal.cache.CacheRecord;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.AccessTokenRecord;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.dto.IdTokenRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MSAL successful authentication result. When auth succeeds, token will be wrapped into the
 * {@link AuthenticationResult} and passed back through the {@link AuthenticationCallback}.
 */
public final class AuthenticationResult implements IAuthenticationResult {
    //Fields for Legacy Cache
    private final String mTenantId;
    private final String mUniqueId;

    private final AccessTokenRecord mAccessToken;
    private final IAccount mAccount;

    public AuthenticationResult(@NonNull final List<ICacheRecord> cacheRecords) {
        final ICacheRecord mostRecentlyAuthorized = cacheRecords.get(0);
        mAccessToken = mostRecentlyAuthorized.getAccessToken();
        mTenantId = mostRecentlyAuthorized.getAccount().getRealm();
        mUniqueId = mostRecentlyAuthorized.getAccount().getHomeAccountId();
        mAccount = AccountAdapter.adapt(cacheRecords).get(0);
    }

    //TODO this is a temporary fix for the AuthenticationResultAdapter.adapt bug to unblock me from debugging.
    AuthenticationResult(@NonNull AccessTokenRecord accessToken,
                         @Nullable String rawIdToken,
                         @NonNull IAccountRecord accountRecord) {
        mAccessToken = accessToken;
        mTenantId = accessToken.getRealm();
        mUniqueId = accessToken.getHomeAccountId();
        CacheRecord cacheRecord = new CacheRecord();
        cacheRecord.setAccount((AccountRecord) accountRecord);
        IdTokenRecord idTokenRecord = new IdTokenRecord();
        idTokenRecord.setSecret(rawIdToken);
        cacheRecord.setIdToken(idTokenRecord);
        List<ICacheRecord> cacheRecordList = new ArrayList<>();
        cacheRecordList.add(cacheRecord);
        mAccount = AccountAdapter.adapt(cacheRecordList).get(0);
    }

    @Override
    @NonNull
    public String getAccessToken() {
        return mAccessToken.getSecret();
    }

    @Override
    @NonNull
    public Date getExpiresOn() {
        final Date expiresOn;

        expiresOn = new Date(
                TimeUnit.SECONDS.toMillis(
                        Long.parseLong(
                                mAccessToken.getExpiresOn()
                        )
                )
        );

        return expiresOn;
    }

    @Override
    @Nullable
    public String getTenantId() {
        return mTenantId;
    }

    @Override
    @NonNull
    public IAccount getAccount() {
        return mAccount;
    }

    @Override
    @NonNull
    public String[] getScope() {
        return mAccessToken.getTarget().split("\\s");
    }
}
