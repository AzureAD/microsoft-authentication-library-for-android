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
package com.microsoft.identity.client.internal;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.DeviceCodeFlowParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ITenantProfile;
import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.common.internal.platform.AndroidPlatformUtil;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.nativeauth.AuthMethod;
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration;
import com.microsoft.identity.client.PoPAuthenticationScheme;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.commands.parameters.AndroidActivityInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryB2CAuthority;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.java.authscheme.AuthenticationSchemeFactory;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.constants.FidoConstants;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.GetAuthMethodsCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFAChallengeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASelectedChallengeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASubmitChallengeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordResendCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInResendCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpResendCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters;
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.util.SchemaUtil;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * CommandParametersAdapter is a helper class to create various Command parameter objects.
 */
public class CommandParametersAdapter {

    private static final String TAG = CommandParametersAdapter.class.getSimpleName();
    public static final String CLIENT_CAPABILITIES_CLAIM = "xms_cc";

    public static CommandParameters createCommandParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache) {

        final CommandParameters commandParameters = CommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                AndroidPlatformComponentsFactory.createFromContext(parameters.getActivity()),
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
                .platformComponents(AndroidPlatformComponentsFactory.createFromActivity(
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
                .preferredBrowser(configuration.getPreferredBrowser())
                .browserSafeList(configuration.getBrowserSafeList())
                .authority(authority)
                .claimsRequestJson(claimsRequestJson)
                .forceRefresh(parameters.getClaimsRequest() != null)
                .scopes(new HashSet<>(parameters.getScopes()))
                .extraScopesToConsent(parameters.getExtraScopesToConsent())
                .extraQueryStringParameters(appendToExtraQueryParametersIfWebAuthnCapable(
                        parameters.getExtraQueryStringParameters(),
                        configuration))
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
                .preferredAuthMethod(parameters.getPreferredAuthMethod())
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
                AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()),
                parameters.getAuthenticationScheme()
        );

        final SilentTokenCommandParameters commandParameters = SilentTokenCommandParameters
                .builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled())
                .correlationId(parameters.getCorrelationId())
                .scopes(new HashSet<>(parameters.getScopes()))
                .build();

        return commandParameters;
    }

    /**
     * Adapter method to create DeviceCodeFlowCommandParameters from DeviceCodeFlowParameters
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param parameters deviceCodeFlowParameters
     * @return DeviceCodeFlowCommandParameters
     */
    public static DeviceCodeFlowCommandParameters createDeviceCodeFlowWithClaimsCommandParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final DeviceCodeFlowParameters parameters) {

        final String claimsRequestJson = ClaimsRequest.getJsonStringFromClaimsRequest(parameters.getClaimsRequest());

        final Authority authority = configuration.getDefaultAuthority();

        final AbstractAuthenticationScheme authenticationScheme = new BearerAuthenticationSchemeInternal();

        final DeviceCodeFlowCommandParameters commandParameters = DeviceCodeFlowCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .scopes(new HashSet<>(parameters.getScopes()))
                .authority(authority)
                .claimsRequestJson(claimsRequestJson)
                .correlationId(parameters.getCorrelationId())
                .build();

        return commandParameters;
    }

    public static DeviceCodeFlowCommandParameters createDeviceCodeFlowCommandParameters(
            @NonNull final PublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull List<String> scopes) {

        // TODO: Consider implementing support for PoP

        final Authority authority = configuration.getDefaultAuthority();

        final AbstractAuthenticationScheme authenticationScheme = new BearerAuthenticationSchemeInternal();

        final DeviceCodeFlowCommandParameters commandParameters = DeviceCodeFlowCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .scopes(new HashSet<>(scopes))
                .authority(authority)
                .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignUpStartCommand}] of Native Auth when password is provided
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param username email address of the user
     * @param password password of the user
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignUpStartCommandParameters createSignUpStartCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String username,
            @Nullable final char[] password,
            final Map<String, String> userAttributes) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        return SignUpStartCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .authority(authority)
                .username(username)
                .password(password)
                .challengeType(configuration.getChallengeTypes())
                .userAttributes(userAttributes)
                // Start of the flow, so there is no correlation ID to use from a previous API response.
                // Set it to a default value.
                .correlationId(DiagnosticContext.INSTANCE.getThreadCorrelationId())
                .build();
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitCodeCommand}] of Native Auth.
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param code Out of band code
     * @param continuationToken Continuation token received from the start command
     * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignUpSubmitCodeCommandParameters createSignUpSubmitCodeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String code,
            @NonNull final String continuationToken,
            @NonNull final String correlationId
    ) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        return SignUpSubmitCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .authority(authority)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .code(code)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignUpResendCodeCommand}] of Native Auth.
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param continuationToken Continuation token received from the start command
     * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignUpResendCodeCommandParameters createSignUpResendCodeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String continuationToken,
            @NonNull final String correlationId
    ) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        return SignUpResendCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .challengeType(configuration.getChallengeTypes())
                .authority(authority)
                .continuationToken(continuationToken)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitUserAttributesCommand}] of Native Auth.
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param continuationToken Continuation token received from the start command
     * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignUpSubmitUserAttributesCommandParameters createSignUpStarSubmitUserAttributesCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String continuationToken,
            @NonNull final String correlationId,
            final Map<String, String> userAttributes) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        return SignUpSubmitUserAttributesCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .authority(authority)
                .clientId(configuration.getClientId())
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .userAttributes(userAttributes)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitPasswordCommand}] of Native Auth.
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param continuationToken Continuation token received from the start command
     * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
     * @param password password for the user
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignUpSubmitPasswordCommandParameters createSignUpSubmitPasswordCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String continuationToken,
            @NonNull final String correlationId,
            @NonNull final char[] password) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        return SignUpSubmitPasswordCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .authority(authority)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .password(password)
                .correlationId(correlationId)
                .build();
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignInStartCommand}] of Native Auth using username and password
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param username email address of the user
     * @param password password of the user
     * @param scopes scopes requested during sign in flow
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignInStartCommandParameters createSignInStartCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String username,
            @Nullable final char[] password,
            final List<String> scopes) throws ClientException {
        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()),
                null
        );

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final SignInStartCommandParameters commandParameters = SignInStartCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .authority(authority)
                .username(username)
                .password(password)
                .authenticationScheme(authenticationScheme)
                .clientId(configuration.getClientId())
                .challengeType(configuration.getChallengeTypes())
                .scopes(scopes)
                // Start of the flow, so there is no correlation ID to use from a previous API response.
                // Set it to a default value.
                .correlationId(DiagnosticContext.INSTANCE.getThreadCorrelationId())
                .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignInStartCommand}] of Native Auth using continuation token
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param continuationToken continuation token
     * @param username email address of the user
     * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
     * @param scopes scopes requested during sign in flow
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignInWithContinuationTokenCommandParameters createSignInWithContinuationTokenCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @Nullable final String continuationToken,
            @Nullable final String username,
            @NonNull final String correlationId,
            final List<String> scopes) throws ClientException {
        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()),
                null
        );

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final SignInWithContinuationTokenCommandParameters commandParameters = SignInWithContinuationTokenCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .authority(authority)
                .continuationToken(continuationToken)
                .username(username)
                .challengeType(configuration.getChallengeTypes())
                .authenticationScheme(authenticationScheme)
                .scopes(scopes)
                .correlationId(correlationId)
                .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignInSubmitCodeCommand}] of Native Auth
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param code Out of band code
     * @param continuationToken continuation token
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param scopes scopes requested during sign in flow
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignInSubmitCodeCommandParameters createSignInSubmitCodeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String code,
            @NonNull final String continuationToken,
            @NonNull final String correlationId,
            final List<String> scopes) throws ClientException {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()),
                null
        );

        final SignInSubmitCodeCommandParameters commandParameters = SignInSubmitCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .authority(authority)
                .continuationToken(continuationToken)
                .authenticationScheme(authenticationScheme)
                .challengeType(configuration.getChallengeTypes())
                .code(code)
                .scopes(scopes)
                .correlationId(correlationId)
                .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignInResendCodeCommand}] of Native Auth
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken continuation token
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignInResendCodeCommandParameters createSignInResendCodeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String correlationId,
            @NonNull final String continuationToken) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final SignInResendCodeCommandParameters commandParameters = SignInResendCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                .authority(authority)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .challengeType(configuration.getChallengeTypes())
                .correlationId(correlationId)
                .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.SignInSubmitPasswordCommand}] of Native Auth
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken continuation token
     * @param password  password of the user
     * @param scopes
     * @return Command parameter object
     * @throws ClientException
     */
    public static SignInSubmitPasswordCommandParameters createSignInSubmitPasswordCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String continuationToken,
            @NonNull final char[] password,
            @NonNull final String correlationId,
            final List<String> scopes) throws ClientException {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()),
                null
        );

        final SignInSubmitPasswordCommandParameters commandParameters =
                SignInSubmitPasswordCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                        .authority(authority)
                        .authenticationScheme(authenticationScheme)
                        .continuationToken(continuationToken)
                        .password(password)
                        .scopes(scopes)
                        .challengeType(configuration.getChallengeTypes())
                        .correlationId(correlationId)
                        .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.MFAChallengeCommand}] of Native Auth
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken continuation token
     * @param scopes scopes requested during sign in flow
     * @return Command parameter object
     * @throws ClientException
     */
    public static MFAChallengeCommandParameters createMFADefaultChallengeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String continuationToken,
            @NonNull final String correlationId,
            final List<String> scopes) throws ClientException {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()),
                null
        );

        final MFAChallengeCommandParameters commandParameters =
                MFAChallengeCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                        .authority(authority)
                        .authenticationScheme(authenticationScheme)
                        .continuationToken(continuationToken)
                        .scopes(scopes)
                        .challengeType(configuration.getChallengeTypes())
                        .correlationId(correlationId)
                        .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.MFAChallengeCommand}] of Native Auth
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken continuation token
     * @param authMethod the user's authentication method that is used to perform the challenge operation
     * @return Command parameter object
     * @throws ClientException
     */
    public static MFASelectedChallengeCommandParameters createMFASelectedChallengeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String continuationToken,
            @NonNull final String correlationId,
            @NonNull final AuthMethod authMethod
    ) throws ClientException {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()),
                null
        );

        final String authMethodId = authMethod.getId();

        final MFASelectedChallengeCommandParameters commandParameters =
                MFASelectedChallengeCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                        .authority(authority)
                        .authenticationScheme(authenticationScheme)
                        .continuationToken(continuationToken)
                        .challengeType(configuration.getChallengeTypes())
                        .authMethodId(authMethodId)
                        .correlationId(correlationId)
                        .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.MFAChallengeCommand}] of Native Auth
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param challenge value of the challenge
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken Continuation token
     * @param scopes scopes requested during sign in flow
     * @return Command parameter object
     */
    public static MFASubmitChallengeCommandParameters createMFASubmitChallengeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String challenge,
            @NonNull final String correlationId,
            @NonNull final String continuationToken,
            final List<String> scopes) throws ClientException {
        final AbstractAuthenticationScheme authenticationScheme = AuthenticationSchemeFactory.createScheme(
                AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()),
                null
        );

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final MFASubmitChallengeCommandParameters commandParameters =
                MFASubmitChallengeCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
                        .applicationName(configuration.getAppContext().getPackageName())
                        .applicationVersion(getPackageVersion(configuration.getAppContext()))
                        .authenticationScheme(authenticationScheme)
                        .clientId(configuration.getClientId())
                        .isSharedDevice(configuration.getIsSharedDevice())
                        .redirectUri(configuration.getRedirectUri())
                        .oAuth2TokenCache(tokenCache)
                        .requiredBrokerProtocolVersion(configuration.getRequiredBrokerProtocolVersion())
                        .sdkType(SdkType.MSAL)
                        .sdkVersion(PublicClientApplication.getSdkVersion())
                        .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled())
                        .authority(authority)
                        .challenge(challenge)
                        .scopes(scopes)
                        .challengeType(configuration.getChallengeTypes())
                        .continuationToken(continuationToken)
                        .clientId(configuration.getClientId())
                        .correlationId(correlationId)
                        .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [{@link com.microsoft.identity.common.nativeauth.internal.commands.GetAuthMethodsCommand}] of Native Auth
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken Continuation token
     * @return Command parameter object
     */
    public static GetAuthMethodsCommandParameters createGetAuthMethodsCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String continuationToken,
            @NonNull final String correlationId) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final GetAuthMethodsCommandParameters commandParameters =
                GetAuthMethodsCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                        .authority(authority)
                        .continuationToken(continuationToken)
                        .challengeType(configuration.getChallengeTypes())
                        .correlationId(correlationId)
                        .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [ResetPasswordStartCommand] of Native Auth.
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param username username associated with password change
     * @return Command parameter object
     */
    public static ResetPasswordStartCommandParameters createResetPasswordStartCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String username) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final ResetPasswordStartCommandParameters commandParameters =
                ResetPasswordStartCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                        .authority(authority)
                        .username(username)
                        .challengeType(configuration.getChallengeTypes())
                        .clientId(configuration.getClientId())
                        // Start of the flow, so there is no correlation ID to use from a previous API response.
                        // Set it to a default value.
                        .correlationId(DiagnosticContext.INSTANCE.getThreadCorrelationId())
                        .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [ResetPasswordSubmitCodeCommand] of Native Auth.
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param code out of band code
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken Continuation token
     * @return Command parameter object
     */
    public static ResetPasswordSubmitCodeCommandParameters createResetPasswordSubmitCodeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String code,
            @NonNull final String correlationId,
            @NonNull final String continuationToken) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final ResetPasswordSubmitCodeCommandParameters commandParameters =
                ResetPasswordSubmitCodeCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                        .authority(authority)
                        .code(code)
                        .challengeType(configuration.getChallengeTypes())
                        .continuationToken(continuationToken)
                        .clientId(configuration.getClientId())
                        .correlationId(correlationId)
                        .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [ResetPasswordResendCodeCommand] of Native Auth.
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken Continuation token
     * @return Command parameter object
     */
    public static ResetPasswordResendCodeCommandParameters createResetPasswordResendCodeCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String correlationId,
            @NonNull final String continuationToken) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final ResetPasswordResendCodeCommandParameters commandParameters =
                ResetPasswordResendCodeCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                        .authority(authority)
                        .challengeType(configuration.getChallengeTypes())
                        .continuationToken(continuationToken)
                        .clientId(configuration.getClientId())
                        .correlationId(correlationId)
                        .build();

        return commandParameters;
    }

    /**
     * Creates command parameter for [ResetPasswordSubmitNewPasswordCommandParameters] of Native Auth.
     * @param configuration PCA configuration
     * @param tokenCache token cache for storing results
     * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
     * @param continuationToken password submit token
     * @return Command parameter object
     */
    public static ResetPasswordSubmitNewPasswordCommandParameters createResetPasswordSubmitNewPasswordCommandParameters(
            @NonNull final NativeAuthPublicClientApplicationConfiguration configuration,
            @NonNull final OAuth2TokenCache tokenCache,
            @NonNull final String continuationToken,
            @NonNull final String correlationId,
            @NonNull final char[] password) {

        final NativeAuthCIAMAuthority authority = ((NativeAuthCIAMAuthority) configuration.getDefaultAuthority());

        final ResetPasswordSubmitNewPasswordCommandParameters commandParameters =
                ResetPasswordSubmitNewPasswordCommandParameters.builder()
                        .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.getAppContext()))
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
                        .authority(authority)
                        .continuationToken(continuationToken)
                        .challengeType(configuration.getChallengeTypes())
                        .newPassword(password)
                        .clientId(configuration.getClientId())
                        .correlationId(correlationId)
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

    public static Authority getRequestAuthority(
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
        final String methodTag = TAG + ":getBrokerBrowserSupportEnabled";

        // Special case only for Intune COBO app, where they use Intune AcquireTokenParameters (an internal class)
        // to set browser support in broker to share SSO from System WebView login.
        if (parameters instanceof IntuneAcquireTokenParameters) {
            boolean brokerBrowserEnabled = ((IntuneAcquireTokenParameters) parameters)
                    .isBrokerBrowserSupportEnabled();
            Logger.info(methodTag,
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
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(context))
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

    /**
     * Adds additional query string parameters to original list.
     * @param queryStringParameters extra query string parameters inputted.
     * @param configuration configuration created from MSAL config file.
     * @return combined list of query string parameters.
     */
    @Nullable
    public static List<Map.Entry<String, String>> appendToExtraQueryParametersIfWebAuthnCapable(
            @Nullable final List<Map.Entry<String, String>> queryStringParameters,
            @NonNull final PublicClientApplicationConfiguration configuration) {
        if (queryStringParameters == null && !configuration.isWebauthnCapable()) {
            return null;
        }
        ArrayList<Map.Entry<String, String>> result = queryStringParameters != null ? new ArrayList<>(queryStringParameters) : new ArrayList<>();
        return AndroidPlatformUtil.updateWithOrDeleteWebAuthnParam(result, configuration.isWebauthnCapable());
    }
}
