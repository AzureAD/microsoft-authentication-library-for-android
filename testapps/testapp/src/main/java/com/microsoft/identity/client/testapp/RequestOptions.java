//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.
package com.microsoft.identity.client.testapp;

import com.microsoft.identity.client.HttpMethod;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;

class RequestOptions
{
    final Constants.ConfigFile mConfigFile;
    final String mLoginHint;
    final IAccount mAccount;
    final Prompt mPrompt;
    final String mScope;
    final String mExtraScope;
    final boolean mEnablePII;
    final boolean mForceRefresh;
    final String mAuthority;
    final Constants.AuthScheme mAuthScheme;
    final HttpMethod mPopHttpMethod;
    final String mPopResourceUrl;

    RequestOptions(final Constants.ConfigFile configFile,
                   final String loginHint,
                   final IAccount account,
                   final Prompt prompt,
                   final String scope,
                   final String extraScope,
                   final boolean enablePII,
                   final boolean forceRefresh,
                   final String authority,
                   final Constants.AuthScheme authScheme,
                   final HttpMethod popHttpMethod,
                   final String popResourceUrl) {
        mConfigFile = configFile;
        mLoginHint = loginHint;
        mAccount = account;
        mPrompt = prompt;
        mScope = scope;
        mExtraScope = extraScope;
        mEnablePII = enablePII;
        mForceRefresh = forceRefresh;
        mAuthority = authority;
        mAuthScheme = authScheme;
        mPopHttpMethod = popHttpMethod;
        mPopResourceUrl = popResourceUrl;
    }

    Constants.ConfigFile getConfigFile() {
        return mConfigFile;
    }

    String getLoginHint() {
        return mLoginHint;
    }

    IAccount getAccount() {
        return mAccount;
    }

    Prompt getPrompt() {
        return mPrompt;
    }

    String getScopes() {
        return mScope;
    }

    String getExtraScopesToConsent() {
        return mExtraScope;
    }

    boolean enablePiiLogging() {
        return mEnablePII;
    }

    boolean forceRefresh() {
        return mForceRefresh;
    }

    String getAuthority() {
        return mAuthority;
    }

    Constants.AuthScheme getAuthScheme() {
        return mAuthScheme;
    }

    HttpMethod getPopHttpMethod() {
        return mPopHttpMethod;
    }

    String getPopResourceUrl() {
        return mPopResourceUrl;
    }
}
