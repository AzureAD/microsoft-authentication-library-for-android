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

import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for AcquireTokenParameters and AcquireTokenSilentParameters
 */
public abstract class TokenParameters {

    private List<String> mScopes;
    private IAccount mAccount;
    private String mAuthority;
    private ClaimsRequest mClaimsRequest;
    private AccountRecord mAccountRecord;

    protected TokenParameters(final TokenParameters.Builder builder) {
        mAccount = builder.mAccount;
        mAuthority = builder.mAuthority;
        mClaimsRequest = builder.mClaimsRequest;
        mScopes = builder.mScopes;
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
    void setScopes(final List<String> scopes){
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
     * TokenParameters builder
     *
     * @param <B>
     */
    public static abstract class Builder<B extends TokenParameters.Builder<B>> {

        private List<String> mScopes;
        private IAccount mAccount;
        private String mAuthority;
        private ClaimsRequest mClaimsRequest;

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

        public B fromAuthority(String authority) {
            mAuthority = authority;
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
                    add(resource.toLowerCase().trim() + "/.default");
                }};
            }

            return self();
        }

        public abstract B self();

        public abstract TokenParameters build();
    }
}
