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

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.java.authorities.CIAMAuthority;
import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftIdToken;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import java.util.Map;

import static com.microsoft.identity.common.java.util.SchemaUtil.MISSING_FROM_THE_TOKEN_RESPONSE;

public class Account implements IAccount {

    private static final String TAG = Account.class.getSimpleName();

    private final Map<String, ?> mIdTokenClaims;
    private final String mRawIdToken;
    private String mClientInfo;
    private String mHomeOid;
    private String mHomeTenantId;
    private String mEnvironment;
    private String mHomeAccountId;

    public Account(
            @Nullable final String clientInfo,
            @Nullable final IDToken homeTenantIdToken) {
        mClientInfo = clientInfo;

        if (null != homeTenantIdToken) {
            mIdTokenClaims = homeTenantIdToken.getTokenClaims();
            mRawIdToken = homeTenantIdToken.getRawIDToken();
        } else {
            mIdTokenClaims = null;
            mRawIdToken = null;
        }
    }

    void setId(@Nullable final String id) {
        mHomeOid = id;
    }

    @NonNull
    @Override
    public String getId() {
        final String methodTag = TAG + ":getId";
        String id;

        ClientInfo clientInfo = null;

        if (null != mClientInfo) { // This property should only exist for home accounts...
            try {
                clientInfo = new ClientInfo(mClientInfo);
            } catch (final MsalClientException e) {
                Logger.error(
                        methodTag,
                        "Failed to parse ClientInfo",
                        e
                );
            }
        }

        if (null != clientInfo) {
            id = clientInfo.getUniqueIdentifier();
        } else if (null != mIdTokenClaims) {
            id = (String) mIdTokenClaims.get(MicrosoftIdToken.OBJECT_ID);
        } else {
            id = mHomeOid;
        }

        if (StringUtil.isEmpty(id)) {
            // This could happen because the ID token that we have for WPJ accounts may not contain
            // oid claim because we used to not ask for PROFILE scope in that token. 
            com.microsoft.identity.common.logging.Logger.warn(
                    methodTag,
                    "Unable to get account id from either ClientInfo or IdToken. Attempting to obtain from home account id."
            );
            id = com.microsoft.identity.common.java.util.StringUtil.getUIdFromHomeAccountId(mHomeAccountId);
        }

        if (StringUtil.isEmpty(id)) {
            com.microsoft.identity.common.logging.Logger.warn(
                    methodTag,
                    "Account ID is empty. Returning MISSING_FROM_THE_TOKEN_RESPONSE."
            );

            // must return something because the method is marked as non-null
            id = MISSING_FROM_THE_TOKEN_RESPONSE;
        }

        return id;
    }

    void setTenantId(@NonNull final String tenantId) {
        mHomeTenantId = tenantId;
    }

    @NonNull
    @Override
    public String getTenantId() {
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

    @Nullable
    @Override
    public String getIdToken() {
        return mRawIdToken;
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

    @NonNull
    @Override
    public String getUsername() {
        if (null != getClaims()) {
            return SchemaUtil.getDisplayableId(getClaims());
        }

        return MISSING_FROM_THE_TOKEN_RESPONSE;
    }

    @Override
    @NonNull
    public String getAuthority() {
        // If the environment shows CIAM, we should return an authority of format https://tenant.ciamlogin.com/tenant.onmicrosoft.com
        if (getEnvironment().contains("ciamlogin.com")) {
            // Call static method in CIAMAuthority to create the full authority uri
            return CIAMAuthority.getFullAuthorityUrlFromAuthorityWithoutPath(getEnvironment());
        }
        // TODO: The below logic only works for the case of AAD. We need to refactor this once we
        //  make a proper fix for B2C
        if (null != getClaims()) {
            final String iss = (String) getClaims().get("iss");
            if (!StringUtil.isEmpty(iss)) {
                return iss;
            }
        }

        return MISSING_FROM_THE_TOKEN_RESPONSE;
    }

    public void setHomeAccountId(@NonNull final String homeAccountId) {
        mHomeAccountId = homeAccountId;
    }
}
