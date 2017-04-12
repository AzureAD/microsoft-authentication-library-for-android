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

/**
 * MSAL Internal class representing all the parameters set per request.
 */
final class AuthenticationRequestParameters {
    private static final String TAG = AuthenticationRequestParameters.class.getSimpleName();

    private final TokenCache mTokenCache;
    private final Set<String> mScope = new HashSet<>();
    private final String mClientId;
    private final RequestContext mRequestContext;

    private Authority mAuthority;
    private String mRedirectUri;
    private String mLoginHint;
    private String mExtraQueryParam;
    private UiBehavior mUiBehavior;
    private User mUser;

    /**
     * Creates new {@link AuthenticationRequestParameters}.
     */
    private AuthenticationRequestParameters(final Authority authority, final TokenCache tokenCache, final Set<String> scope,
                                            final String clientId, final RequestContext requestContext) {
        // Every acquireToken API call should contain correlation id.
        if (requestContext == null || requestContext.getCorrelationId() == null) {
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
        mRequestContext = requestContext;
    }

    /**
     * Creates the {@link AuthenticationRequestParameters} with all the given parameters.
     */
    static AuthenticationRequestParameters create(final Authority authority, final TokenCache tokenCache, final Set<String> scope,
                                                  final String clientId, final String redirectUri, final String loginHint,
                                                  final String extraQueryParam, final UiBehavior uiBehavior, final User user,
                                                  final RequestContext requestContext) {
        final AuthenticationRequestParameters requestParameters = new AuthenticationRequestParameters(authority, tokenCache, scope,
                clientId, requestContext);
        requestParameters.setRedirectUri(redirectUri);
        requestParameters.setLoginHint(loginHint);
        requestParameters.setExtraQueryParam(extraQueryParam);
        requestParameters.setUIBehavior(uiBehavior);
        requestParameters.setUser(user);

        return requestParameters;
    }

    /**
     * Creates the {@link AuthenticationRequestParameters} with all the given parameters.
     */
    static AuthenticationRequestParameters create(final Authority authority, final TokenCache tokenCache, final Set<String> scope,
                                                  final String clientId, final RequestContext requestContext) {
        return new AuthenticationRequestParameters(authority, tokenCache, scope, clientId, requestContext);
    }

    Authority getAuthority() {
        return mAuthority;
    }

    void setAuthority(final String authorityString, final boolean isAuthorityValidationOn) {
        mAuthority = Authority.createAuthority(authorityString, isAuthorityValidationOn);
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

    UiBehavior getUiBehavior() {
        return mUiBehavior;
    }

    private void setUIBehavior(final UiBehavior uiBehavior) {
        mUiBehavior = uiBehavior;
    }

    User getUser() {
        return mUser;
    }

    private void setUser(final User user) {
        mUser = user;
    }

    RequestContext getRequestContext() {
        return mRequestContext;
    }
}
