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
package com.microsoft.identity.client.controllers;

import android.app.Activity;
import android.content.Context;

import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.User;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;

import java.util.List;

public class MSALAcquireTokenOperationParameters extends MSALOperationParameters {

    private Context mAppContext;
    private MsalOAuth2TokenCache mTokenCache;
    private Activity mActivity;
    private List<String> mScopes;
    private User mUser;
    private UiBehavior mUIBehavior;
    private String mExtraQueryStringParameters;
    private String mExtraScopesToConsent;
    private String authority;
    private String clientId;
    private String redirectUri;


    public Context getAppContext() {
        return mAppContext;
    }

    public void setAppContext(Context mAppContext) {
        this.mAppContext = mAppContext;
    }

    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public List<String> getScopes() {
        return mScopes;
    }

    public void setScopes(List<String> mScopes) {
        this.mScopes = mScopes;
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User mUser) {
        this.mUser = mUser;
    }

    public UiBehavior getUIBehavior() {
        return mUIBehavior;
    }

    public void setUIBehavior(UiBehavior mUIBehavior) {
        this.mUIBehavior = mUIBehavior;
    }

    public String getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    public void setExtraQueryStringParameters(String mExtraQueryStringParameters) {
        this.mExtraQueryStringParameters = mExtraQueryStringParameters;
    }

    public String getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    public void setExtraScopesToConsent(String mExtraScopesToConsent) {
        this.mExtraScopesToConsent = mExtraScopesToConsent;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setTokenCache(MsalOAuth2TokenCache cache) {
        this.mTokenCache = cache;
    }

    public MsalOAuth2TokenCache getTokenCache() {
        return mTokenCache;
    }
}
