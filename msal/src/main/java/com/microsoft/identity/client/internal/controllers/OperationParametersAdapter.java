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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.microsoft.identity.client.AccountAdapter;
import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AzureActiveDirectoryAccountIdentifier;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashSet;

public class OperationParametersAdapter {

    private static final String TAG = OperationParameters.class.getName();

    public static AcquireTokenOperationParameters createAcquireTokenOperationParameters(
            AcquireTokenParameters acquireTokenParameters,
            PublicClientApplicationConfiguration publicClientApplicationConfiguration) {

        final String methodName = ":createAcquireTokenOperationParameters";
        final AcquireTokenOperationParameters acquireTokenOperationParameters =
                new AcquireTokenOperationParameters();

        if (StringUtil.isEmpty(acquireTokenParameters.getAuthority())) {
            if (acquireTokenParameters.getAccount() != null) {
                acquireTokenOperationParameters.setAuthority(
                        getRequestAuthority(
                                acquireTokenParameters.getAccount(),
                                publicClientApplicationConfiguration)
                );
            } else {
                acquireTokenOperationParameters.
                        setAuthority(
                                publicClientApplicationConfiguration.getDefaultAuthority()
                        );
            }

        } else {
            acquireTokenOperationParameters.setAuthority(
                    Authority.getAuthorityFromAuthorityUrl(
                            acquireTokenParameters.getAuthority()
                    )
            );
        }

        if (acquireTokenOperationParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
            AzureActiveDirectoryAuthority aadAuthority =
                    (AzureActiveDirectoryAuthority) acquireTokenOperationParameters.getAuthority();

            aadAuthority.setMultipleCloudsSupported(
                    publicClientApplicationConfiguration.getMultipleCloudsSupported()
            );
        }
        com.microsoft.identity.common.internal.logging.Logger.verbosePII(
                methodName,
                "Using authority: [" + acquireTokenOperationParameters
                        .getAuthority()
                        .getAuthorityUri() + "]"
        );
        acquireTokenOperationParameters.setScopes(
                new HashSet<>(acquireTokenParameters.getScopes()
                )
        );
        acquireTokenOperationParameters.setClientId(
                publicClientApplicationConfiguration.getClientId()
        );
        acquireTokenOperationParameters.setRedirectUri(
                publicClientApplicationConfiguration.getRedirectUri()
        );
        acquireTokenOperationParameters.setActivity(
                acquireTokenParameters.getActivity()
        );

        if (acquireTokenParameters.getAccount() != null) {
            acquireTokenOperationParameters.setLoginHint(
                    acquireTokenParameters.getAccount().getUsername()
            );
            acquireTokenOperationParameters.setAccount(
                    acquireTokenParameters.getAccountRecord()
            );
        } else {
            acquireTokenOperationParameters.setLoginHint(
                    acquireTokenParameters.getLoginHint()
            );
        }
        acquireTokenOperationParameters.setTokenCache(
                publicClientApplicationConfiguration.getOAuth2TokenCache()
        );
        acquireTokenOperationParameters.setExtraQueryStringParameters(
                acquireTokenParameters.getExtraQueryStringParameters()
        );
        acquireTokenOperationParameters.setExtraScopesToConsent(
                acquireTokenParameters.getExtraScopesToConsent()
        );
        acquireTokenOperationParameters.setAppContext(
                publicClientApplicationConfiguration.getAppContext()
        );
        acquireTokenOperationParameters.setClaimsRequest(
                ClaimsRequest.getJsonStringFromClaimsRequest(
                        acquireTokenParameters.getClaimsRequest()
                )
        );

        if (null != publicClientApplicationConfiguration.getAuthorizationAgent()) {
            acquireTokenOperationParameters.setAuthorizationAgent(
                    publicClientApplicationConfiguration.getAuthorizationAgent()
            );
        } else {
            acquireTokenOperationParameters.setAuthorizationAgent(AuthorizationAgent.DEFAULT);
        }

        if (acquireTokenParameters.getUiBehavior() == null) {
            acquireTokenOperationParameters.setOpenIdConnectPromptParameter(
                    OpenIdConnectPromptParameter.SELECT_ACCOUNT
            );
        } else {
            acquireTokenOperationParameters.setOpenIdConnectPromptParameter(
                    acquireTokenParameters
                            .getUiBehavior()
                            .toOpenIdConnectPromptParameter()
            );
        }

        final Context context = acquireTokenParameters.getActivity().getApplicationContext();

        acquireTokenOperationParameters.setApplicationName(context.getPackageName());

        acquireTokenOperationParameters.setApplicationVersion(getPackageVersion(context));

        acquireTokenOperationParameters.setSdkVersion(PublicClientApplication.getSdkVersion());

        return acquireTokenOperationParameters;
    }

    public static AcquireTokenSilentOperationParameters createAcquireTokenSilentOperationParameters(
            AcquireTokenSilentParameters acquireTokenSilentParameters,
            PublicClientApplicationConfiguration publicClientApplicationConfiguration) {

        final AcquireTokenSilentOperationParameters acquireTokenSilentOperationParameters
                = new AcquireTokenSilentOperationParameters();

        acquireTokenSilentOperationParameters.setAppContext(
                publicClientApplicationConfiguration.getAppContext()
        );
        acquireTokenSilentOperationParameters.setScopes(
                new HashSet<>(acquireTokenSilentParameters.getScopes()
                )
        );
        acquireTokenSilentOperationParameters.setClientId(
                publicClientApplicationConfiguration.getClientId()
        );
        acquireTokenSilentOperationParameters.setTokenCache(
                publicClientApplicationConfiguration.getOAuth2TokenCache()
        );

        if (null != acquireTokenSilentParameters.getAccountRecord()) {
            acquireTokenSilentOperationParameters.setAccount(
                    acquireTokenSilentParameters.getAccountRecord()
            );
        } else if (null != acquireTokenSilentParameters.getAccount()){
            // This will happen when the account exists in broker.
            // We need to construct the AccountRecord object with IAccount.
            // for broker acquireToken request only.
            final IAccount account = acquireTokenSilentParameters.getAccount();
            final AccountRecord requestAccountRecord = new AccountRecord();
            requestAccountRecord.setEnvironment(account.getEnvironment());
            requestAccountRecord.setUsername(account.getUsername());
            requestAccountRecord.setHomeAccountId(account.getHomeAccountIdentifier().getIdentifier());
            requestAccountRecord.setAlternativeAccountId(account.getAccountIdentifier().getIdentifier());
            acquireTokenSilentOperationParameters.setAccount(requestAccountRecord);
        }

        if (StringUtil.isEmpty(acquireTokenSilentParameters.getAuthority())) {
            acquireTokenSilentParameters.setAuthority(
                    getSilentRequestAuthority(
                            acquireTokenSilentParameters.getAccount(),
                            publicClientApplicationConfiguration
                    )
            );
        }
        acquireTokenSilentOperationParameters.setAuthority(
                Authority.getAuthorityFromAuthorityUrl(acquireTokenSilentParameters.getAuthority())
        );
        acquireTokenSilentOperationParameters.setRedirectUri(
                publicClientApplicationConfiguration.getRedirectUri()
        );

        if (acquireTokenSilentOperationParameters.getAuthority() instanceof AzureActiveDirectoryAuthority) {
            AzureActiveDirectoryAuthority aadAuthority =
                    (AzureActiveDirectoryAuthority) acquireTokenSilentOperationParameters.getAuthority();

            aadAuthority.setMultipleCloudsSupported(
                    publicClientApplicationConfiguration.getMultipleCloudsSupported()
            );
        }
        acquireTokenSilentOperationParameters.setClaimsRequest(
                ClaimsRequest.getJsonStringFromClaimsRequest(
                        acquireTokenSilentParameters.getClaimsRequest()
                )
        );

        acquireTokenSilentOperationParameters.setForceRefresh(
                acquireTokenSilentParameters.getForceRefresh()
        );

        final Context context = publicClientApplicationConfiguration.getAppContext();

        acquireTokenSilentOperationParameters.setApplicationName(context.getPackageName());

        acquireTokenSilentOperationParameters.setApplicationVersion(getPackageVersion(context));

        acquireTokenSilentOperationParameters.setSdkVersion(PublicClientApplication.getSdkVersion());

        return acquireTokenSilentOperationParameters;
    }



    private static Authority getRequestAuthority(
            final IAccount account,
            final PublicClientApplicationConfiguration publicClientApplicationConfiguration) {

        String requestAuthority = null;
        Authority authority;

        // For a B2C request, the silent request will use the passed-in authority string from client app.
        if (publicClientApplicationConfiguration.getDefaultAuthority() instanceof AzureActiveDirectoryB2CAuthority) {
            requestAuthority = publicClientApplicationConfiguration
                    .getDefaultAuthority()
                    .getAuthorityURL()
                    .toString();
        }

        // If the request is not a B2C request or the passed-in authority is not a valid URL.
        // MSAL will construct the request authority based on the account info.
        if (requestAuthority == null) {
            requestAuthority = getAuthorityFromAccount(account);
        }

        if (requestAuthority == null) {
            authority = publicClientApplicationConfiguration.getDefaultAuthority();
        } else {
            authority = Authority.getAuthorityFromAuthorityUrl(requestAuthority);
        }

        return authority;
    }

    private static String getSilentRequestAuthority(
            final IAccount account,
            final PublicClientApplicationConfiguration publicClientApplicationConfiguration) {

        String requestAuthority = null;

        // For a B2C request, the silent request will use the passed-in authority string from client app.
        if (publicClientApplicationConfiguration.getDefaultAuthority() instanceof AzureActiveDirectoryB2CAuthority) {
            requestAuthority = publicClientApplicationConfiguration
                    .getDefaultAuthority()
                    .getAuthorityURL()
                    .toString();
        }

        // If the request is not a B2C request or the passed-in authority is not a valid URL.
        // MSAL will construct the request authority based on the account info.
        if (requestAuthority == null) {
            requestAuthority = getAuthorityFromAccount(account);
        }

        if (requestAuthority == null) {
            requestAuthority = publicClientApplicationConfiguration
                    .getDefaultAuthority()
                    .getAuthorityURL()
                    .toString();
        }

        return requestAuthority;
    }

    public static String getAuthorityFromAccount(final IAccount account) {
        final String methodName = ":getAuthorityFromAccount";
        com.microsoft.identity.common.internal.logging.Logger.verbose(
                TAG + methodName,
                "Getting authority from account..."
        );

        final AzureActiveDirectoryAccountIdentifier aadIdentifier;
        if (null != account
                && null != account.getAccountIdentifier()
                && account.getAccountIdentifier() instanceof AzureActiveDirectoryAccountIdentifier
                && null != (aadIdentifier = (AzureActiveDirectoryAccountIdentifier) account.getAccountIdentifier()).getTenantIdentifier()
                && !StringUtil.isEmpty(aadIdentifier.getTenantIdentifier())) {
            return "https://"
                    + account.getEnvironment()
                    + "/"
                    + aadIdentifier.getTenantIdentifier()
                    + "/";
        } else {
            com.microsoft.identity.common.internal.logging.Logger.warn(
                    TAG + methodName,
                    "Account was null..."
            );
        }

        return null;
    }

    private static String getPackageVersion(@NonNull final Context context) {
        final String packageName = context.getPackageName();
        try {
            final PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
