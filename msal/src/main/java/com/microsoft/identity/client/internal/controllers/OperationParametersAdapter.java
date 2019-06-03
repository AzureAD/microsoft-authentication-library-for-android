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
import android.support.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ITenantProfile;
import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.HashSet;
import java.util.Map;

public class OperationParametersAdapter {

    private static final String TAG = OperationParameters.class.getName();

    public static OperationParameters createOperationParameters(
            @NonNull final PublicClientApplicationConfiguration configuration) {
        final OperationParameters parameters = new OperationParameters();
        parameters.setAppContext(configuration.getAppContext());
        parameters.setTokenCache(configuration.getOAuth2TokenCache());
        parameters.setClientId(configuration.getClientId());
        parameters.setRedirectUri(configuration.getRedirectUri());
        parameters.setAuthority(configuration.getDefaultAuthority());
        parameters.setApplicationName(configuration.getAppContext().getPackageName());
        parameters.setApplicationVersion(getPackageVersion(configuration.getAppContext()));
        parameters.setSdkVersion(PublicClientApplication.getSdkVersion());
        return parameters;
    }

    public static AcquireTokenOperationParameters createAcquireTokenOperationParameters(
            @NonNull final AcquireTokenParameters acquireTokenParameters,
            @NonNull final PublicClientApplicationConfiguration publicClientApplicationConfiguration) {

        final String methodName = ":createAcquireTokenOperationParameters";
        final AcquireTokenOperationParameters acquireTokenOperationParameters =
                new AcquireTokenOperationParameters();

        if (StringUtil.isEmpty(acquireTokenParameters.getAuthority())) {
            if (acquireTokenParameters.getAccount() != null) {
                acquireTokenOperationParameters.setAuthority(
                        getRequestAuthority(
                                publicClientApplicationConfiguration
                        )
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

        acquireTokenOperationParameters.setBrowserSafeList(
                publicClientApplicationConfiguration.getBrowserSafeList()
        );

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
            final IAccount account = acquireTokenParameters.getAccount();

            final String username = getUsername(account);

            acquireTokenOperationParameters.setLoginHint(username);
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

    private static String getUsername(@NonNull final IAccount account) {
        // We need to get the username for the account...
        // Since the home account may be null (in the case of guest only), we need to look
        // both at the home and any tenant profiles
        String username = null;

        if (null != account.getClaims()) {
            username = SchemaUtil.getDisplayableId(account.getClaims());
        } else {
            // The home account was null, therefore this must be a multi-tenant account
            final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) account;
            // Any arbitrary tenant profile should work...
            final Map<String, ITenantProfile> tenantProfiles = multiTenantAccount.getTenantProfiles();

            for (final Map.Entry<String, ITenantProfile> profileEntry : tenantProfiles.entrySet()) {
                if (null != profileEntry.getValue().getClaims()) {
                    final String displayableId = SchemaUtil.getDisplayableId(profileEntry.getValue().getClaims());
                    if (!SchemaUtil.MISSING_FROM_THE_TOKEN_RESPONSE.equalsIgnoreCase(displayableId)) {
                        username = displayableId;
                        break;
                    }
                }
            }
        }

        return username;
    }

    public static AcquireTokenSilentOperationParameters createAcquireTokenSilentOperationParameters(
            @NonNull final AcquireTokenSilentParameters acquireTokenSilentParameters,
            @NonNull final PublicClientApplicationConfiguration publicClientApplicationConfiguration,
            @Nullable final String requestEnvironment,
            @Nullable final String requestHomeAccountId) {

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
        } else if (null != acquireTokenSilentParameters.getAccount()) {
            // This will happen when the account exists in broker.
            // We need to construct the AccountRecord object with IAccount.
            // for broker acquireToken request only.
            final IAccount account = acquireTokenSilentParameters.getAccount();
            final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) account;

            final AccountRecord requestAccountRecord = new AccountRecord();
            requestAccountRecord.setEnvironment(requestEnvironment);
            requestAccountRecord.setHomeAccountId(requestHomeAccountId);

            // TODO do you mean the home account? The guest account? Where do I look??
            requestAccountRecord.setUsername(SchemaUtil.getDisplayableId(multiTenantAccount.getClaims()));
            // TODO which tenant do you want?????? Using home for now...
            requestAccountRecord.setLocalAccountId(multiTenantAccount.getId());

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
            @NonNull final PublicClientApplicationConfiguration publicClientApplicationConfiguration) {

        String requestAuthority = null;
        Authority authority;

        // For a B2C request, the silent request will use the passed-in authority string from client app.
        if (publicClientApplicationConfiguration.getDefaultAuthority() instanceof AzureActiveDirectoryB2CAuthority) {
            requestAuthority = publicClientApplicationConfiguration
                    .getDefaultAuthority()
                    .getAuthorityURL()
                    .toString();
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
            @NonNull final PublicClientApplicationConfiguration publicClientApplicationConfiguration) {

        String requestAuthority = null;

        // For a B2C request, the silent request will use the passed-in authority string from client app.
        if (publicClientApplicationConfiguration.getDefaultAuthority() instanceof AzureActiveDirectoryB2CAuthority) {
            requestAuthority = publicClientApplicationConfiguration
                    .getDefaultAuthority()
                    .getAuthorityURL()
                    .toString();
        }

        if (requestAuthority == null) {
            requestAuthority = publicClientApplicationConfiguration
                    .getDefaultAuthority()
                    .getAuthorityURL()
                    .toString();
        }

        return requestAuthority;
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
