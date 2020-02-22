package com.microsoft.identity.client.testapp;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;

class RequestOptions
{
    final Constants.AzureActiveDirectoryEnvironment mEnvironment;
    final String mLoginHint;
    final IAccount mAccount;
    final Prompt mPrompt;
    final Constants.UserAgent mUserAgent;
    final String mScope;
    final String mExtraScope;
    final boolean mEnablePII;
    final boolean mForceRefresh;
    final String mAuthority;
    final boolean mUsePop;

    RequestOptions(final Constants.AzureActiveDirectoryEnvironment environment,
                   final String loginHint,
                   final IAccount account,
                   final Prompt prompt,
                   final Constants.UserAgent userAgent,
                   final String scope,
                   final String extraScope,
                   final boolean enablePII,
                   final boolean forceRefresh,
                   final String authority,
                   final boolean usePop) {
        mEnvironment = environment;
        mLoginHint = loginHint;
        mAccount = account;
        mPrompt = prompt;
        mUserAgent = userAgent;
        mScope = scope;
        mExtraScope = extraScope;
        mEnablePII = enablePII;
        mForceRefresh = forceRefresh;
        mAuthority = authority;
        mUsePop = usePop;
    }

    Constants.AzureActiveDirectoryEnvironment getEnvironment() {
        return mEnvironment;
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

    Constants.UserAgent getUserAgent() {
        return mUserAgent;
    }

    String getAuthority() {
        return mAuthority;
    }

    boolean usePop() {
        return mUsePop;
    }
}
