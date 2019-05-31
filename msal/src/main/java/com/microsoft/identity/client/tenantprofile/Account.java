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
package com.microsoft.identity.client.tenantprofile;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class Account implements IAccount {

    private static final String TAG = Account.class.getSimpleName();

    private final String mRawIdToken;
    private String mHomeOid;
    private String mHomeTenantId;
    private String mEnvironment;

    public Account(@Nullable final String rawIdToken) {
        mRawIdToken = rawIdToken;
    }

    void setId(@Nullable final String id) {
        mHomeOid = id;
    }

    @Nullable
    @Override
    public String getId() {
        String id = null;

        if (null != mRawIdToken) {
            id = (String) getClaims().get(MicrosoftIdToken.OBJECT_ID);
        } else if (null != mHomeOid) {
            id = mHomeOid;
        }

        return id;
    }

    void setTenantId(@Nullable final String tenantId) {
        mHomeTenantId = tenantId;
    }

    @Nullable
    public String getTenantId() { // TODO make this package private
        return mHomeTenantId;
    }

    @Nullable
    public String getHomeAccountId() { // TODO make this package private
        return getId() + "." + mHomeTenantId;
    }

    void setEnvironment(@NonNull final String environment) {
        mEnvironment = environment;
    }

    @Nullable
    public String getEnvironment() { // TODO Make this package private
        return mEnvironment;
    }

    @Nullable
    @Override
    public Map<String, ?> getClaims() {
        Map<String, ?> claims = null;

        if (null != mRawIdToken) {
            claims = getClaims(mRawIdToken);
        }

        return claims;
    }

    protected static Map<String, ?> getClaims(@NonNull final String rawIdToken) {
        final String methodName = ":getClaims(String)";

        final Map<String, Object> result = new HashMap<>();

        try {
            final JWT jwt = JWTParser.parse(rawIdToken);
            final JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();
            result.putAll(claimsSet.getClaims());
        } catch (ParseException e) {
            Logger.error(
                    TAG + methodName,
                    "Failed to parse IdToken",
                    e
            );
        }

        return result;
    }
}
