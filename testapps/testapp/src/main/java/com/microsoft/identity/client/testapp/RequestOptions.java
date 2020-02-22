package com.microsoft.identity.client.testapp;

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

    RequestOptions(final Constants.ConfigFile configFile,
                   final String loginHint,
                   final IAccount account,
                   final Prompt prompt,
                   final String scope,
                   final String extraScope,
                   final boolean enablePII,
                   final boolean forceRefresh,
                   final String authority,
                   final Constants.AuthScheme authScheme) {
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
}
