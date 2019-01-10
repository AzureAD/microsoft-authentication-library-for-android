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

import java.util.List;

/**
 * Base class for AcquireTokenParameters and AcquireTokenSilentParameters
 */
abstract class TokenParameters {

    private List<String> mScopes;
    private IAccount mAccount;
    private String mAuthority;
    private ClaimsRequest mClaimsRequest;
    private AuthenticationCallback mCallback;
    private AccountRecord mAccountRecord;

    protected TokenParameters(final TokenParameters.Builder builder) {
        mAccount = builder.mAccount;
        mAuthority = builder.mAuthority;
        mCallback = builder.mCallback;
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
    public void setScopes(List<String> scopes) {
        this.mScopes = scopes;
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
    public void setAccount(IAccount account) {
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
     * Optional. Can be passed to override the default authority.
     *
     * @param authority
     */
    public void setAuthority(String authority) {
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

    /**
     * Optional. Can be passed into request specific claims in the id_token and access_token
     *
     * @param claimsRequest
     */
    public void setClaimsRequest(ClaimsRequest claimsRequest) {
        this.mClaimsRequest = claimsRequest;
    }

    /**
     * The Non-null {@link AuthenticationCallback} to receive the result back.
     * 1) If user cancels the flow by pressing the device back button, the result will be sent
     * back via {@link AuthenticationCallback#onCancel()}.
     * 2) If the sdk successfully receives the token back, result will be sent back via
     * {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     * 3) All the other errors will be sent back via
     * {@link AuthenticationCallback#onError(com.microsoft.identity.client.exception.MsalException)}.
     *
     * @return
     */
    public AuthenticationCallback getCallback() {
        return mCallback;
    }

    /**
     * The Non-null {@link AuthenticationCallback} to receive the result back.
     * 1) If user cancels the flow by pressing the device back button, the result will be sent
     * back via {@link AuthenticationCallback#onCancel()}.
     * 2) If the sdk successfully receives the token back, result will be sent back via
     * {@link AuthenticationCallback#onSuccess(IAuthenticationResult)}
     * 3) All the other errors will be sent back via
     * {@link AuthenticationCallback#onError(com.microsoft.identity.client.exception.MsalException)}.
     *
     * @param callback
     */
    public void setCallback(AuthenticationCallback callback) {
        this.mCallback = callback;
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
        private AuthenticationCallback mCallback;

        public B withScopes(List<String> scopes) {
            mScopes = scopes;
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

        public B callback(AuthenticationCallback callback) {
            mCallback = callback;
            return self();
        }

        public abstract B self();

        public abstract TokenParameters build();

    }
}
