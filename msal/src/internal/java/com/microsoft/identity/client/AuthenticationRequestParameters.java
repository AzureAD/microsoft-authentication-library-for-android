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
    private final String mRedirectUri;
    private final String mPolicy;
    private final boolean mRestrictToSingleUser;
    private final String mLoginHint;
    private final String mExtraQueryParam;
    private final UIOptions mUIOption;
    private final UUID mCorrelationId;

    AuthenticationRequestParameters(final Authority authority, final TokenCache tokenCache, final Set<String> scope,
                                    final String clientId, final String redirectUri, final String policy,
                                    final boolean restrictToSingleUser, final String loginHint,
                                    final String extraQueryParam, final UIOptions uiOptions, final UUID correlationId) {
        mAuthority = authority;
        mTokenCache = tokenCache;
        mScope.addAll(scope);
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mPolicy = policy;
        mRestrictToSingleUser = restrictToSingleUser;
        mLoginHint = loginHint;
        mExtraQueryParam = extraQueryParam;
        mUIOption = uiOptions;
        mCorrelationId = correlationId;
    }

    Authority getAuthority() {
        return mAuthority;
    }

    TokenCache getTokenCache() {
        return mTokenCache;
    }

    Set getScope() {
        return mScope;
    }

    String getClientId() {
        return mClientId;
    }

    String getRedirectUri() {
        return mRedirectUri;
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

    String getExtraQueryParam() {
        return mExtraQueryParam;
    }

    UIOptions getUIOption() {
        return mUIOption;
    }

    UUID getCorrelationId() {
        return mCorrelationId;
    }
}
