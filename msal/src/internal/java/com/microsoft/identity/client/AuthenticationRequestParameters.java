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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * MSAL Internal class representing all the parameters set per request.
 */
final class AuthenticationRequestParameters {
    private final Authority mAuthority;
    private final TokenCache mTokenCache;
    private final Set<String> mScope = new HashSet<>();
    private final String mClientId;
    private final String mPolicy;
    private final boolean mRestrictToSingleUser;
    private final UUID mCorrelationId;

    private String mRedirectUri;
    private String mLoginHint;
    private String mExtraQueryParam;
    private UIOptions mUIOption;

    /**
     * Creates new {@link AuthenticationRequestParameters}.
     */
    private AuthenticationRequestParameters(final Authority authority, final TokenCache tokenCache, final Set<String> scope,
                                            final String clientId, final String policy, final boolean restrictToSingleUser,
                                            final UUID correlationId) {
        // Every acquireToken API call should contain correlation id.
        if (correlationId == null) {
            throw new IllegalArgumentException("correlationId");
        }

        // AuthenticationRequestParameters is created per acquireToken API call, should never be null.
        if (scope == null) {
            throw new IllegalArgumentException("scope");
        }

        if (tokenCache == null) {
            throw new IllegalArgumentException("tokenCache");
        }

        mAuthority = authority;
        mTokenCache = tokenCache;
        mScope.addAll(scope);
        mClientId = clientId;
        mPolicy = policy;
        mRestrictToSingleUser = restrictToSingleUser;
        mCorrelationId = correlationId;
    }

    static AuthenticationRequestParameters create(final Authority authority, final TokenCache tokenCache, final Set<String> scope,
                                                  final String clientId, final String redirectUri, final String policy,
                                                  final boolean restrictToSingleUser, final String loginHint,
                                                  final String extraQueryParam, final UIOptions uiOptions, final UUID correlationId) {
        final AuthenticationRequestParameters requestParameters = new AuthenticationRequestParameters(authority, tokenCache, scope,
                clientId, policy, restrictToSingleUser, correlationId);
        requestParameters.setRedirectUri(redirectUri);
        requestParameters.setLoginHint(loginHint);
        requestParameters.setExtraQueryParam(extraQueryParam);
        requestParameters.setUIOption(uiOptions);

        return requestParameters;
    }

    static AuthenticationRequestParameters create(final Authority authority, final TokenCache tokenCache, final Set<String> scope,
                                                  final String clientId, final String policy, final boolean restrictToSingleUser,
                                                  final UUID correlationId) {
        return new AuthenticationRequestParameters(authority, tokenCache, scope, clientId, policy, restrictToSingleUser,
                correlationId);
    }

    Authority getAuthority() {
        return mAuthority;
    }

    TokenCache getTokenCache() {
        return mTokenCache;
    }

    Set<String> getScope() {
        return mScope;
    }

    String getClientId() {
        return mClientId;
    }

    String getRedirectUri() {
        return mRedirectUri;
    }

    private void setRedirectUri(final String redirectUri) {
        mRedirectUri = redirectUri;
    }

    String getPolicy() {
        return mPolicy;
    }

    boolean getRestrictToSingleUser() {
        return mRestrictToSingleUser;
    }

    String getLoginHint() {
        return mLoginHint;
    }

    private void setLoginHint(final String loginHint) {
        mLoginHint = loginHint;
    }

    String getExtraQueryParam() {
        return mExtraQueryParam;
    }

    private void setExtraQueryParam(final String extraQueryParam) {
        mExtraQueryParam = extraQueryParam;
    }

    UIOptions getUIOption() {
        return mUIOption;
    }

    private void setUIOption(final UIOptions uiOption) {
        mUIOption = uiOption;
    }

    UUID getCorrelationId() {
        return mCorrelationId;
    }
}
