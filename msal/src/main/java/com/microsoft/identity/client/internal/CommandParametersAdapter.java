package com.microsoft.identity.client.internal;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ITenantProfile;
import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.PoPAuthenticationScheme;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.common.AndroidPlatformComponents;
import com.microsoft.identity.common.internal.commands.parameters.AndroidActivityInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.AuthenticationSchemeFactory;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class CommandParametersAdapter {

    private static final String TAG = CommandParametersAdapter.class.getSimpleName();
    public static final String CLIENT_CAPABILITIES_CLAIM = "xms_cc";

    public static CommandParameters createCommandParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache) {

        final CommandParameters commandParameters = CommandParameters.builder()
                .platformComponents(AndroidPlatformComponents.createFromContext(configuration.getAppContext()))
                .applicationName(configuration.getAppContext().getPackageName())
                .applicationVersion(getPackageVersion(configuration.getAppContext()))
                .clientId(configuration.getClientId())
                .isSharedDevice(configuration.getIsSharedDevice())
                .oAuth2TokenCache(tokenCache)
                .redirectUri(configuration.getRedirectUri())
                .requiredBrokerProtocolVersion(configuration.getRequiredBrokerProtocolVersion())
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled())
                .build();

        return commandParameters;
    }

    public static RemoveAccountCommandParameters createRemoveAccountCommandParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final AccountRecord account) {

        final RemoveAccountCommandParameters commandParameters = RemoveAccountCommandParameters.builder()
                .platformComponents(AndroidPlatformComponents.createFromContext(configuration.getAppContext()))
                .applicationName(configuration.getAppContext().getPackageName())
                .applicationVersion(getPackageVersion(configuration.getAppContext()))
                .clientId(configuration.getClientId())
                .isSharedDevice(configuration.getIsSharedDevice())
                .oAuth2TokenCache(tokenCache)
                .redirectUri(configuration.getRedirectUri())
                .requiredBrokerProtocolVersion(configuration.getRequiredBrokerProtocolVersion())
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .account(account)
                .browserSafeList(configuration.getBrowserSafeList())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled())
                .build();

        return commandParameters;
    }

    public static InteractiveTokenCommandParameters createInteractiveTokenCommandParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final AcquireTokenParameters parameters) throws ClientException {

        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponents.createFromContext(parameters.getActivity()),
                parameters.getAuthenticationScheme()
        );

        final Authority authority = getAuthority(configuration, parameters);

        final String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(
                getClaimsRequest(
                        parameters.getClaimsRequest(),
                        configuration,
                        authority
                ));

        final InteractiveTokenCommandParameters commandParameters = AndroidActivityInteractiveTokenCommandParameters
                .builder()
                .activity(parameters.getActivity())
                .platformComponents(AndroidPlatformComponents.createFromActivity(
                        parameters.getActivity(),
                        parameters.getFragment()))
                .applicationName(configuration.getAppContext().getPackageName())
                .applicationVersion(getPackageVersion(configuration.getAppContext()))
                .clientId(configuration.getClientId())
                .isSharedDevice(configuration.getIsSharedDevice())
                .oAuth2TokenCache(tokenCache)
                .redirectUri(configuration.getRedirectUri())
                .requiredBrokerProtocolVersion(configuration.getRequiredBrokerProtocolVersion())
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .browserSafeList(configuration.getBrowserSafeList())
                .authority(authority)
                .claimsRequestJson(claimsRequestJson)
                .forceRefresh(parameters.getClaimsRequest() != null)
                .scopes(new HashSet<>(parameters.getScopes()))
                .extraScopesToConsent(parameters.getExtraScopesToConsent())
                .extraQueryStringParameters(parameters.getExtraQueryStringParameters())
                .loginHint(getLoginHint(parameters))
                .account(parameters.getAccountRecord())
                .authenticationScheme(authenticationScheme)
                .authorizationAgent(getAuthorizationAgent(configuration))
                .brokerBrowserSupportEnabled(getBrokerBrowserSupportEnabled(parameters))
                .prompt(getPromptParameter(parameters))
                .isWebViewZoomControlsEnabled(configuration.isWebViewZoomControlsEnabled())
                .isWebViewZoomEnabled(configuration.isWebViewZoomEnabled())
                .handleNullTaskAffinity(configuration.isHandleNullTaskAffinityEnabled())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled())
                .correlationId(parameters.getCorrelationId())
                .build();

        return commandParameters;
    }

    public static SilentTokenCommandParameters createSilentTokenCommandParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final AcquireTokenSilentParameters parameters) throws ClientException {
        final Authority authority = getAuthority(configuration, parameters);

        final ClaimsRequest claimsRequest = parameters.getClaimsRequest();

        final ClaimsRequest mergedClaimsRequest = getClaimsRequest(
                parameters.getClaimsRequest(),
                configuration,
                authority);

        final String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(
                mergedClaimsRequest
        );

        final boolean forceRefresh = claimsRequest != null || parameters.getForceRefresh();

        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponents.createFromContext(configuration.getAppContext()),
                parameters.getAuthenticationScheme()
        );

        final SilentTokenCommandParameters commandParameters = SilentTokenCommandParameters
                .builder()
                .platformComponents(AndroidPlatformComponents.createFromContext(configuration.getAppContext()))
                .applicationName(configuration.getAppContext().getPackageName())
                .applicationVersion(getPackageVersion(configuration.getAppContext()))
                .clientId(configuration.getClientId())
                .isSharedDevice(configuration.getIsSharedDevice())
                .oAuth2TokenCache(tokenCache)
                .redirectUri(configuration.getRedirectUri())
                .requiredBrokerProtocolVersion(configuration.getRequiredBrokerProtocolVersion())
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .authority(authority)
                .claimsRequestJson(claimsRequestJson)
                .forceRefresh(forceRefresh)
                .account(parameters.getAccountRecord())
                .authenticationScheme(authenticationScheme)
                .scopes(new HashSet<>(parameters.getScopes()))
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled())
                .correlationId(parameters.getCorrelationId())
                .build();

        return commandParameters;
    }

    public static DeviceCodeFlowCommandParameters createDeviceCodeFlowCommandParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull String[] scopes) {

        // TODO: Consider implementing support for PoP

        final Authority authority = configuration.getDefaultAuthority();

        final AbstractAuthenticationScheme authenticationScheme = new BearerAuthenticationSchemeInternal();

        final DeviceCodeFlowCommandParameters commandParameters = DeviceCodeFlowCommandParameters.builder()
                .platformComponents(AndroidPlatformComponents.createFromContext(configuration.getAppContext()))
                .applicationName(configuration.getAppContext().getPackageName())
                .applicationVersion(getPackageVersion(configuration.getAppContext()))
                .clientId(configuration.getClientId())
                .isSharedDevice(configuration.getIsSharedDevice())
                .redirectUri(configuration.getRedirectUri())
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.getRequiredBrokerProtocolVersion())
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled())
                .authenticationScheme(authenticationScheme)
                .scopes(new HashSet<>(Arrays.asList(scopes)))
                .authority(authority)
                .build();

        return commandParameters;
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

    public static ClaimsRequest addClientCapabilitiesToClaimsRequest(ClaimsRequest cr, String clientCapabilities) {

        final ClaimsRequest mergedClaimsRequest = (cr == null) ? new ClaimsRequest() : cr;

        if (clientCapabilities != null) {
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

    private static Authority getAuthority(
            final PublicClientApplicationConfiguration configuration,
            @NonNull final AcquireTokenParameters parameters) {
        Authority authority;

        if (StringUtil.isEmpty(parameters.getAuthority())) {
            if (parameters.getAccount() != null) {
                authority = getRequestAuthority(configuration);
            } else {
                authority = configuration.getDefaultAuthority();
            }
        } else {
            authority = Authority.getAuthorityFromAuthorityUrl(
                    parameters.getAuthority()
            );
        }

        if (authority instanceof AzureActiveDirectoryAuthority) {
            AzureActiveDirectoryAuthority aadAuthority =
                    (AzureActiveDirectoryAuthority) authority;

            aadAuthority.setMultipleCloudsSupported(
                    configuration.getMultipleCloudsSupported()
            );
        }

        return authority;
    }

    private static Authority getAuthority(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final AcquireTokenSilentParameters parameters) {
        final String requestAuthority = parameters.getAuthority();
        final Authority authority = Authority.getAuthorityFromAuthorityUrl(requestAuthority);

        if (authority instanceof AzureActiveDirectoryAuthority) {
            AzureActiveDirectoryAuthority aadAuthority =
                    (AzureActiveDirectoryAuthority) authority;

            aadAuthority.setMultipleCloudsSupported(configuration.getMultipleCloudsSupported());
        }

        return authority;
    }

    private static ClaimsRequest getClaimsRequest(
            @NonNull final ClaimsRequest requestedClaims,
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final Authority authority
    ) {
        if (authority instanceof AzureActiveDirectoryAuthority) {
            //AzureActiveDirectory supports client capabilities
            return addClientCapabilitiesToClaimsRequest(requestedClaims,
                    configuration.getClientCapabilities());
        } else {
            return requestedClaims;
        }
    }

    private static String getLoginHint(@NonNull final AcquireTokenParameters parameters) {
        if (parameters.getAccount() != null) {
            final IAccount account = parameters.getAccount();

            return getUsername(account);
        } else {
            return parameters.getLoginHint();
        }
    }

    private static AuthorizationAgent getAuthorizationAgent(@NonNull final PublicClientApplicationConfiguration configuration) {
        if (configuration.getAuthorizationAgent() != null) {
            return configuration.getAuthorizationAgent();
        } else {
            return AuthorizationAgent.DEFAULT;
        }
    }

    private static boolean getBrokerBrowserSupportEnabled(@NonNull final AcquireTokenParameters parameters) {
        final String methodName = ":getBrokerBrowserSupportEnabled";

        // Special case only for Intune COBO app, where they use Intune AcquireTokenParameters (an internal class)
        // to set browser support in broker to share SSO from System WebView login.
        if (parameters instanceof IntuneAcquireTokenParameters) {
            boolean brokerBrowserEnabled = ((IntuneAcquireTokenParameters) parameters)
                    .isBrokerBrowserSupportEnabled();
            Logger.info(TAG + methodName,
                    " IntuneAcquireTokenParameters instance, broker browser enabled : "
                            + brokerBrowserEnabled
            );
            return brokerBrowserEnabled;
        }

        return false;
    }

    private static OpenIdConnectPromptParameter getPromptParameter(@NonNull final AcquireTokenParameters parameters) {
        if (parameters.getPrompt() == null) {
            return OpenIdConnectPromptParameter.SELECT_ACCOUNT;
        } else {
            return parameters.getPrompt().toOpenIdConnectPromptParameter();
        }
    }

    /**
     * Constructs the {@link GenerateShrCommandParameters} for the supplied args.
     *
     * @param clientConfig     The configuration of our current app.
     * @param oAuth2TokenCache Our local token cache.
     * @param homeAccountId    The home_account_id of the user for whom we're signing.
     * @param popParameters    The pop params to embed in the resulting SHR.
     * @return The fully-formed command params.
     */
    public static GenerateShrCommandParameters createGenerateShrCommandParameters(
            @NonNull final PublicClientApplicationConfiguration clientConfig,
            @NonNull final OAuth2TokenCache oAuth2TokenCache,
            @NonNull final String homeAccountId,
            @NonNull final PoPAuthenticationScheme popParameters) {
        final Context context = clientConfig.getAppContext();
        return GenerateShrCommandParameters.builder()
                .platformComponents(AndroidPlatformComponents.createFromContext(context))
                .applicationName(context.getPackageName())
                .applicationVersion(getPackageVersion(context))
                .clientId(clientConfig.getClientId())
                .isSharedDevice(clientConfig.getIsSharedDevice())
                .redirectUri(clientConfig.getRedirectUri())
                .oAuth2TokenCache(oAuth2TokenCache)
                .requiredBrokerProtocolVersion(clientConfig.getRequiredBrokerProtocolVersion())
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(clientConfig.isPowerOptCheckForEnabled())
                .homeAccountId(homeAccountId)
                .popParameters(popParameters)
                .build();
    }
}
