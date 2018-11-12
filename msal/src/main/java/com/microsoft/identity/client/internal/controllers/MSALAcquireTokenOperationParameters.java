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
package com.microsoft.identity.client.internal.controllers;

import android.app.Activity;
import android.util.Pair;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.internal.authorities.Authority;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class MSALAcquireTokenOperationParameters extends MSALOperationParameters {

    private Activity mActivity;
    private String mLoginHint;
    private UiBehavior mUIBehavior;
    private List<Pair<String, String>> mExtraQueryStringParameters;
    private List<String> mExtraScopesToConsent;

    public AuthorizationAgent getAuthorizationAgent() {
        return mAuthorizationAgent;
    }

    public void setAuthorizationAgent(AuthorizationAgent authorizationAgent) {
        mAuthorizationAgent = authorizationAgent;
    }

    private AuthorizationAgent mAuthorizationAgent;

    public Activity getActivity() {
        return mActivity;
    }

    public void setActivity(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public UiBehavior getUIBehavior() {
        return mUIBehavior;
    }

    public void setUIBehavior(UiBehavior mUIBehavior) {
        this.mUIBehavior = mUIBehavior;
    }

    public List<Pair<String, String>> getExtraQueryStringParameters() {
        return mExtraQueryStringParameters;
    }

    public void setExtraQueryStringParameters(List<Pair<String, String>> mExtraQueryStringParameters) {
        this.mExtraQueryStringParameters = mExtraQueryStringParameters;
    }

    public List<String> getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    public void setExtraScopesToConsent(List<String> mExtraScopesToConsent) {
        this.mExtraScopesToConsent = mExtraScopesToConsent;
    }

    public void setLoginHint(String loginHint) {
        this.mLoginHint = loginHint;
    }

    public String getLoginHint() {
        return this.mLoginHint;
    }

    public static MSALAcquireTokenOperationParameters createMsalAcquireTokenOperationParameters(AcquireTokenParameters acquireTokenParameters, PublicClientApplicationConfiguration publicClientApplicationConfiguration){
        final String methodName = ":createMsalAcquireTokenOperationParameters";
        final MSALAcquireTokenOperationParameters msalAcquireTokenOperationParameters = new MSALAcquireTokenOperationParameters();

        if (StringUtil.isEmpty(acquireTokenParameters.getAuthority())) {
            if(acquireTokenParameters.getAccount() != null){
                msalAcquireTokenOperationParameters.setAuthority(getRequestAuthority(acquireTokenParameters.getAccount(), publicClientApplicationConfiguration));
            }else{
                msalAcquireTokenOperationParameters.setAuthority(publicClientApplicationConfiguration.getDefaultAuthority());
            }

        } else {
            msalAcquireTokenOperationParameters.setAuthority(Authority.getAuthorityFromAuthorityUrl(acquireTokenParameters.getAuthority()));
        }

        if (msalAcquireTokenOperationParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
            AzureActiveDirectoryAuthority aadAuthority = (AzureActiveDirectoryAuthority) msalAcquireTokenOperationParameters.getAuthority();
            aadAuthority.setMultipleCloudsSupported(publicClientApplicationConfiguration.getMultipleCloudsSupported());
        }

        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                methodName,
                "Using authority: [" + msalAcquireTokenOperationParameters.getAuthority().getAuthorityUri() + "]"
        );

        msalAcquireTokenOperationParameters.setScopes(new ArrayList<>(acquireTokenParameters.getScopes()));
        msalAcquireTokenOperationParameters.setClientId(publicClientApplicationConfiguration.getClientId());
        msalAcquireTokenOperationParameters.setRedirectUri(publicClientApplicationConfiguration.getRedirectUri());
        msalAcquireTokenOperationParameters.setActivity(acquireTokenParameters.getActivity());

        if(acquireTokenParameters.getAccount() != null){
            msalAcquireTokenOperationParameters.setLoginHint(acquireTokenParameters.getAccount().getUsername());
            msalAcquireTokenOperationParameters.setAccount(acquireTokenParameters.getAccountRecord());
        }else{
            msalAcquireTokenOperationParameters.setLoginHint(acquireTokenParameters.getLoginHint());
        }

        msalAcquireTokenOperationParameters.setTokenCache(publicClientApplicationConfiguration.getOAuth2TokenCache());
        msalAcquireTokenOperationParameters.setExtraQueryStringParameters(acquireTokenParameters.getExtraQueryStringParameters());
        msalAcquireTokenOperationParameters.setExtraScopesToConsent(acquireTokenParameters.getExtraScopesToConsent());
        msalAcquireTokenOperationParameters.setAppContext(publicClientApplicationConfiguration.getAppContext());

        if (null != publicClientApplicationConfiguration.getAuthorizationAgent()) {
            msalAcquireTokenOperationParameters.setAuthorizationAgent(publicClientApplicationConfiguration.getAuthorizationAgent());
        } else {
            msalAcquireTokenOperationParameters.setAuthorizationAgent(AuthorizationAgent.DEFAULT);
        }

        if (acquireTokenParameters.getUIBehavior() == null) {
            msalAcquireTokenOperationParameters.setUIBehavior(UiBehavior.SELECT_ACCOUNT);
        } else {
            msalAcquireTokenOperationParameters.setUIBehavior(acquireTokenParameters.getUIBehavior());
        }

        return msalAcquireTokenOperationParameters;
    }


    private static Authority getRequestAuthority(final IAccount account, final PublicClientApplicationConfiguration publicClientApplicationConfiguration) {
        String requestAuthority = null;
        Authority authority;

        // For a B2C request, the silent request will use the passed-in authority string from client app.
        if (publicClientApplicationConfiguration.getDefaultAuthority() instanceof AzureActiveDirectoryB2CAuthority) {
            requestAuthority = publicClientApplicationConfiguration.getDefaultAuthority().getAuthorityURL().toString();
        }

        // If the request is not a B2C request or the passed-in authority is not a valid URL.
        // MSAL will construct the request authority based on the account info.
        if (requestAuthority == null) {
            requestAuthority = Authority.getAuthorityFromAccount(account);
        }

        if(requestAuthority == null){
            authority = publicClientApplicationConfiguration.getDefaultAuthority();
        }else{
            authority = Authority.getAuthorityFromAuthorityUrl(requestAuthority);
        }

        return authority;
    }
}
