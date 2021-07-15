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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Base class for AcquireTokenParameters and AcquireTokenSilentParameters
 */
public abstract class TokenParameters {

    private List<String> mScopes;
    private IAccount mAccount;
    private String mAuthority;
    private ClaimsRequest mClaimsRequest;
    private AccountRecord mAccountRecord;
    private AuthenticationScheme mAuthenticationScheme;
    private String mCorrelationId;

    protected TokenParameters(@NonNull final TokenParameters.Builder builder) {
        mAccount = builder.mAccount;
        mAuthority = builder.mAuthority;
        mClaimsRequest = builder.mClaimsRequest;
        mScopes = builder.mScopes;
        mAuthenticationScheme = builder.mAuthenticationScheme;
        mCorrelationId = builder.mCorrelationId;
    }

    /**
     * Gets the {@link AuthenticationScheme}.
     *
     * @return The AuthenticationScheme to get.
     */
    @NonNull
    public AuthenticationScheme getAuthenticationScheme() {
        return mAuthenticationScheme;
    }

    /**
     * The non-null array of scopes to be requested for the access token.
     * MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     *
     * @return
     */
    public List<String> getScopes() {
        return mScopes;
    }

    /**
     * The non-null array of scopes to be requested for the access token.
     * MSAL always sends the scopes 'openid profile offline_access'.  Do not include any of these scopes in the scope parameter.
     *
     * @param scopes
     */
    void setScopes(final List<String> scopes) {
        mScopes = scopes;
    }

    /**
     * Optional. If provided, will be used to force the session continuation.  If user tries to sign in with a different account, error
     * will be returned.
     *
     * @return
     */
    public IAccount getAccount() {
        return mAccount;
    }

    /**
     * Optional. If provided, will be used to force the session continuation.  If user tries to sign in with a different account, error
     * will be returned.
     *
     * @param account
     */
    void setAccount(IAccount account) {
        this.mAccount = account;
    }

    /**
     * Optional. Can be passed to override the default authority.
     *
     * @return
     */
    public String getAuthority() {
        return mAuthority;
    }

    /**
     * Optional for interactive requests; can be passed to override the default authority.
     * Required for silent requests.
     *
     * @param authority
     */
    void setAuthority(String authority) {
        this.mAuthority = authority;
    }

    /**
     * Optional. Can be passed into request specific claims in the id_token and access_token
     *
     * @return
     */
    public ClaimsRequest getClaimsRequest() {
        return mClaimsRequest;
    }

    void setAccountRecord(AccountRecord record) {
        mAccountRecord = record;
    }

    public AccountRecord getAccountRecord() {
        return mAccountRecord;
    }

    /**
     * Gets the correlation id passed to Token Parameters. If specified, MSAL will use this
     * correlation id for the request instead of generating a new one.
     *
     * @return a String representing the correlation id passed to TokenParameters
     */
    public String getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * TokenParameters builder
     *
     * @param <B>
     */
    public static abstract class Builder<B extends TokenParameters.Builder<B>> {

        private List<String> mScopes;
        private IAccount mAccount;
        private String mAuthority;
        private ClaimsRequest mClaimsRequest;
        private AuthenticationScheme mAuthenticationScheme;
        private String mCorrelationId;

        public B withAuthenticationScheme(@NonNull final AuthenticationScheme scheme) {
            mAuthenticationScheme = scheme;
            return self();
        }

        public B withScopes(List<String> scopes) {
            if (null != mScopes) {
                throw new IllegalArgumentException("Scopes is already set.");
            } else if (null == scopes || scopes.isEmpty()) {
                throw new IllegalArgumentException("Empty scopes list.");
            } else {
                mScopes = scopes;
            }

            return self();
        }

        public B forAccount(IAccount account) {
            mAccount = account;
            return self();
        }

        public B fromAuthority(String authorityUrl) {
            mAuthority = authorityUrl;
            return self();
        }

        public B fromAuthority(@NonNull final AzureCloudInstance cloudInstance,
                               @NonNull final AadAuthorityAudience audience,
                               @Nullable final String tenant) {
            if (!TextUtils.isEmpty(tenant)) {
                if (audience != AadAuthorityAudience.AzureAdMyOrg) {
                    throw new IllegalArgumentException(
                            "Audience must be " + AadAuthorityAudience.AzureAdMyOrg + " when tenant is specified"
                    );
                } else {
                    return fromAuthority(cloudInstance, tenant);
                }
            } else if (audience == AadAuthorityAudience.AzureAdMyOrg) {
                if (TextUtils.isEmpty(tenant)) {
                    throw new IllegalArgumentException(
                            "Tenant must be specified when the audience is " + audience
                    );
                } else {
                    mAuthority = cloudInstance.getCloudInstanceUri() + "/" + tenant;
                    return self();
                }
            } else {
                mAuthority = cloudInstance.getCloudInstanceUri() + "/" + audience.getAudienceValue();
                return self();
            }
        }

        public B fromAuthority(@NonNull final AzureCloudInstance cloudInstance,
                               @NonNull final AadAuthorityAudience audience) {
            return fromAuthority(cloudInstance, audience, null);
        }

        public B fromAuthority(@NonNull final AzureCloudInstance cloudInstance,
                               @NonNull final String tenant) {
            mAuthority = cloudInstance.getCloudInstanceUri() + "/" + tenant;
            return self();
        }

        //TODO: Needs it's own builder... possible added here
        public B withClaims(ClaimsRequest claimsRequest) {
            mClaimsRequest = claimsRequest;
            return self();
        }

        public B withResource(final String resource) {
            if (null != mScopes) {
                throw new IllegalArgumentException(
                        "Scopes is already set. Scopes and resources cannot be combined in a single request."
                );
            } else if (StringUtil.isEmpty(resource)) {
                throw new IllegalArgumentException(
                        "Empty resource string."
                );
            } else {
                mScopes = new ArrayList<String>() {{
                    add(resource.toLowerCase(Locale.ROOT).trim() + "/.default");
                }};
            }

            return self();
        }

        public B withCorrelationId(@NonNull final UUID correlationId) {
            mCorrelationId = correlationId.toString();
            return self();
        }

        public abstract B self();

        public abstract TokenParameters build();
    }
}
