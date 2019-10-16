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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IClaimable;
import com.microsoft.identity.client.ITenantProfile;
import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static com.microsoft.identity.common.internal.authorities.AllAccounts.ALL_ACCOUNTS_TENANT_ID;
import static com.microsoft.identity.common.internal.authorities.AnyPersonalAccount.ANY_PERSONAL_ACCOUNT_TENANT_ID;
import static com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience.ORGANIZATIONS;
import static com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken.TENANT_ID;

public class OperationParametersAdapter {

    private static final String TAG = OperationParametersAdapter.class.getSimpleName();
    public static final String CLIENT_CAPABILITIES_CLAIM = "XMS_CC";

    public static OperationParameters createOperationParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache cache) {
        final OperationParameters parameters = new OperationParameters();
        parameters.setAppContext(configuration.getAppContext());
        parameters.setTokenCache(cache);
        parameters.setBrowserSafeList(configuration.getBrowserSafeList());
        parameters.setIsSharedDevice(configuration.getIsSharedDevice());
        parameters.setClientId(configuration.getClientId());
        parameters.setRedirectUri(configuration.getRedirectUri());
        parameters.setAuthority(configuration.getDefaultAuthority());
        parameters.setApplicationName(configuration.getAppContext().getPackageName());
        parameters.setApplicationVersion(getPackageVersion(configuration.getAppContext()));
        parameters.setSdkVersion(PublicClientApplication.getSdkVersion());
        parameters.setRequiredBrokerProtocolVersion(configuration.getRequiredBrokerProtocolVersion());
        return parameters;
    }

    public static AcquireTokenOperationParameters createAcquireTokenOperationParameters(
            @NonNull final AcquireTokenParameters acquireTokenParameters,
            @NonNull final PublicClientApplicationConfiguration publicClientApplicationConfiguration,
            @NonNull final OAuth2TokenCache cache) {

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
            //AzureActiveDirectory supports client capabilities
            ClaimsRequest mergedClaimsRequest = addClientCapabilitiesToClaimsRequest(acquireTokenParameters.getClaimsRequest(),
                                                    publicClientApplicationConfiguration.getClientCapabilities());
            acquireTokenOperationParameters.setClaimsRequest(
                    ClaimsRequest.getJsonStringFromClaimsRequest(
                            mergedClaimsRequest
                    )
            );

            if(acquireTokenParameters.getClaimsRequest() != null){
                acquireTokenOperationParameters.setForceRefresh(true);
            }

        }else{
            //B2C doesn't support client capabilities
            acquireTokenOperationParameters.setClaimsRequest(
                    ClaimsRequest.getJsonStringFromClaimsRequest(
                            acquireTokenParameters.getClaimsRequest()
                    )
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
        acquireTokenOperationParameters.setTokenCache(cache);
        acquireTokenOperationParameters.setExtraQueryStringParameters(
                acquireTokenParameters.getExtraQueryStringParameters()
        );
        acquireTokenOperationParameters.setExtraScopesToConsent(
                acquireTokenParameters.getExtraScopesToConsent()
        );
        acquireTokenOperationParameters.setAppContext(
                publicClientApplicationConfiguration.getAppContext()
        );


        if (null != publicClientApplicationConfiguration.getAuthorizationAgent()) {
            acquireTokenOperationParameters.setAuthorizationAgent(
                    publicClientApplicationConfiguration.getAuthorizationAgent()
            );
        } else {
            acquireTokenOperationParameters.setAuthorizationAgent(AuthorizationAgent.DEFAULT);
        }

        if (acquireTokenParameters.getPrompt() == null || acquireTokenParameters.getPrompt() == Prompt.WHEN_REQUIRED) {
            acquireTokenOperationParameters.setOpenIdConnectPromptParameter(
                    OpenIdConnectPromptParameter.SELECT_ACCOUNT
            );
        } else {
            acquireTokenOperationParameters.setOpenIdConnectPromptParameter(
                    acquireTokenParameters
                            .getPrompt()
                            .toOpenIdConnectPromptParameter()
            );
        }

        final Context context = acquireTokenParameters.getActivity().getApplicationContext();
        acquireTokenOperationParameters.setApplicationName(context.getPackageName());
        acquireTokenOperationParameters.setApplicationVersion(getPackageVersion(context));
        acquireTokenOperationParameters.setSdkVersion(PublicClientApplication.getSdkVersion());

        return acquireTokenOperationParameters;
    }

    public static ClaimsRequest addClientCapabilitiesToClaimsRequest(ClaimsRequest cr, String clientCapabilities){

        final ClaimsRequest mergedClaimsRequest = (cr == null) ? new ClaimsRequest() : cr;

        if(clientCapabilities != null) {
            //Add client capabilities to existing claims request
            RequestedClaimAdditionalInformation info = new RequestedClaimAdditionalInformation();
            String[] capabilities = clientCapabilities.split(",");
            info.setValues(new ArrayList<Object>(Arrays.asList(capabilities)));
            mergedClaimsRequest.requestClaimInAccessToken(CLIENT_CAPABILITIES_CLAIM, info);
        }

        return mergedClaimsRequest;
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
            @NonNull final PublicClientApplicationConfiguration pcaConfig,
            @NonNull final OAuth2TokenCache cache) {
        final Context context = pcaConfig.getAppContext();
        final String requestAuthority = acquireTokenSilentParameters.getAuthority();
        final Authority authority = Authority.getAuthorityFromAuthorityUrl(requestAuthority);
        final ClaimsRequest claimsRequest = acquireTokenSilentParameters.getClaimsRequest();
        String jsonClaimsRequest = ClaimsRequest.getJsonStringFromClaimsRequest(claimsRequest);

        final AcquireTokenSilentOperationParameters atsOperationParams = new AcquireTokenSilentOperationParameters();
        atsOperationParams.setAppContext(pcaConfig.getAppContext());
        atsOperationParams.setScopes(new HashSet<>(acquireTokenSilentParameters.getScopes()));
        atsOperationParams.setClientId(pcaConfig.getClientId());
        atsOperationParams.setTokenCache(cache);
        atsOperationParams.setAuthority(authority);
        atsOperationParams.setApplicationName(context.getPackageName());
        atsOperationParams.setApplicationVersion(getPackageVersion(context));
        atsOperationParams.setSdkVersion(PublicClientApplication.getSdkVersion());
        atsOperationParams.setForceRefresh(acquireTokenSilentParameters.getForceRefresh());
        atsOperationParams.setRedirectUri(pcaConfig.getRedirectUri());
        atsOperationParams.setAccount(acquireTokenSilentParameters.getAccountRecord());

        if (atsOperationParams.getAuthority() instanceof AzureActiveDirectoryAuthority) {
            AzureActiveDirectoryAuthority aadAuthority =
                    (AzureActiveDirectoryAuthority) atsOperationParams.getAuthority();

            aadAuthority.setMultipleCloudsSupported(pcaConfig.getMultipleCloudsSupported());

            ClaimsRequest mergedClaimsRequest = addClientCapabilitiesToClaimsRequest(claimsRequest, pcaConfig.getClientCapabilities());
            //This business logic likely shouldn't be here, but this is the most convenient place I could find
            if(claimsRequest != null){
               atsOperationParams.setForceRefresh(true);
            }
            jsonClaimsRequest = ClaimsRequest.getJsonStringFromClaimsRequest(mergedClaimsRequest);
        }
        atsOperationParams.setClaimsRequest(jsonClaimsRequest);

        return atsOperationParams;
    }

    /**
     * For a tenant, assert that claims exist for it. This is a convenience function for throwing
     * exceptions & logging.
     *
     * @param tenantId The tenantId for which claims are sought.
     * @param claimable   The claims, which may be null - if they are, an {@link IllegalStateException}
     *                 is thrown.
     */
    public static void validateClaimsExistForTenant(@NonNull final String tenantId,
                                                    @Nullable final IClaimable claimable)
            throws MsalClientException {
        final String methodName = ":validateClaimsExistForTenant";

        if (null == claimable || null == claimable.getClaims()) {
            final String errMsg = "Attempting to authorize for tenant: "
                    + tenantId
                    + " but no matching account was found.";

            Logger.warn(
                    TAG + methodName,
                    errMsg
            );

            throw new MsalClientException(errMsg);
        }
    }

    public static boolean isAccountHomeTenant(@Nullable final Map<String, ?> claims,
                                              @NonNull final String tenantId) {
        boolean isAccountHomeTenant = false;

        if (null != claims && !claims.isEmpty()) {
            isAccountHomeTenant = claims.get(TENANT_ID).equals(tenantId);
        }

        return isAccountHomeTenant;
    }

    public static boolean isHomeTenantEquivalent(@NonNull final String tenantId) {
        return tenantId.equalsIgnoreCase(ALL_ACCOUNTS_TENANT_ID)
                || tenantId.equalsIgnoreCase(ANY_PERSONAL_ACCOUNT_TENANT_ID)
                || tenantId.equalsIgnoreCase(ORGANIZATIONS);
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
