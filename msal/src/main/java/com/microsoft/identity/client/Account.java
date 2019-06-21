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

import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;

import java.util.Map;

public class Account implements IAccount {

    private final Map<String, ?> mIdTokenClaims;
    private String mHomeOid;
    private String mHomeTenantId;
    private String mEnvironment;

    public Account(@Nullable final IDToken homeTenantIdToken) {
        if (null != homeTenantIdToken) {
            mIdTokenClaims = homeTenantIdToken.getTokenClaims();
        } else {
            mIdTokenClaims = null;
        }
    }

    void setId(@Nullable final String id) {
        mHomeOid = id;
    }

    @NonNull
    @Override
    public String getId() {
        String id;

        if (null != mIdTokenClaims) {
            id = (String) mIdTokenClaims.get(MicrosoftIdToken.OBJECT_ID);
        } else {
            id = mHomeOid;
        }

        return id;
    }

    void setTenantId(@NonNull final String tenantId) {
        mHomeTenantId = tenantId;
    }

    @NonNull
    String getTenantId() {
        return mHomeTenantId;
    }

    @NonNull
    String getHomeAccountId() {
        return getId() + "." + mHomeTenantId;
    }

    void setEnvironment(@NonNull final String environment) {
        mEnvironment = environment;
    }

    @NonNull
    String getEnvironment() {
        return mEnvironment;
    }

    /**
     * Gets the claims associated to this Account's IdToken. In the case of the Microsoft Identity
     * Platform, this value can be null if the home tenant has not been authorized.
     *
     * @return The claims for this Account's IdToken or null, if no IdToken exists.
     */
    @Nullable
    @Override
    public Map<String, ?> getClaims() {
        return mIdTokenClaims;
    }
}
