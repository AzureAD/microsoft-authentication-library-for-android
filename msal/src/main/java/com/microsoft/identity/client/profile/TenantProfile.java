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

import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

public class TenantProfile implements ITenantProfile {

    private static final String TAG = TenantProfile.class.getSimpleName();

    /**
     * The raw IdToken - will be parsed.
     */
    private String mRawIdToken;

    /**
     * The authority - could be a String? Do we need any of these methods?
     */
    private String mAuthority;

    private boolean mIsHomeTenant;

    @NonNull
    @Override
    public String getId() {
        return (String) getClaims().get(MicrosoftIdToken.OBJECT_ID);
    }

    @NonNull
    @Override
    public String getTenantId() {
        return (String) getClaims().get(MicrosoftIdToken.TENANT_ID);
    }

    void setIdToken(final String rawIdToken) {
        mRawIdToken = rawIdToken;
    }

    void setAuthority(final String authority) {
        mAuthority = authority;
    }

    @NonNull
    @Override
    public String getAuthority() {
        return mAuthority;
    }

    @NonNull
    @Override
    public Map<String, ?> getClaims() {
        return getClaims(mRawIdToken);
    }

    public void setIsHomeTenant(boolean isHomeTenant) {
        mIsHomeTenant = isHomeTenant;
    }

    @Override
    public boolean isHomeTenant() {
        return mIsHomeTenant;
    }

    private static Map<String, ?> getClaims(@NonNull final String rawIdToken) {
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
