// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.nativeauth.utils

import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.claims.ClaimsRequest
import com.microsoft.identity.client.internal.CommandParametersAdapter
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory
import com.microsoft.identity.common.java.authscheme.AuthenticationSchemeFactory
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.nativeauth.commands.parameters.GetAuthMethodsCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.JITChallengeAuthMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFADefaultChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASelectedDefaultChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASubmitChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.nativeauth.AuthMethod
import com.microsoft.identity.nativeauth.NativeAuthPublicClientApplicationConfiguration

/**
 * NativeAuthCommandParametersAdapter is a helper class to create various Command parameter objects for Native auth.
 */
class NativeAuthCommandParametersAdapter {

    companion object {
        //region signUp commands
        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignUpStartCommand]] of Native Auth when password is provided
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param username email address of the user
         * @param password password of the user
         * @return Command parameter object
         * @throws ClientException
         */
        public fun createSignUpStartCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            username: String,
            password: CharArray?,
            userAttributes: Map<String?, String?>?
        ): SignUpStartCommandParameters? {
            val authority = configuration.defaultAuthority as NativeAuthCIAMAuthority
            return SignUpStartCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .username(username)
                .password(password)
                .challengeType(configuration.getChallengeTypes())
                .userAttributes(userAttributes) // Start of the flow, so there is no correlation ID to use from a previous API response.
                // Set it to a default value.
                .correlationId(DiagnosticContext.INSTANCE.threadCorrelationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitCodeCommand]] of Native Auth.
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param code Out of band code
         * @param continuationToken Continuation token received from the start command
         * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
         * @return Command parameter object
         * @throws ClientException
         */
        public fun createSignUpSubmitCodeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            code: String,
            continuationToken: String,
            correlationId: String
        ): SignUpSubmitCodeCommandParameters? {
            val authority = configuration.defaultAuthority as NativeAuthCIAMAuthority
            return SignUpSubmitCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .code(code)
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignUpResendCodeCommand]] of Native Auth.
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param continuationToken Continuation token received from the start command
         * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
         * @return Command parameter object
         * @throws ClientException
         */
        fun createSignUpResendCodeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            correlationId: String
        ): SignUpResendCodeCommandParameters? {
            val authority = configuration.defaultAuthority as NativeAuthCIAMAuthority
            return SignUpResendCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .challengeType(configuration.getChallengeTypes())
                .authority(authority)
                .continuationToken(continuationToken)
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitUserAttributesCommand]] of Native Auth.
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param continuationToken Continuation token received from the start command
         * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
         * @return Command parameter object
         * @throws ClientException
         */
        fun createSignUpStarSubmitUserAttributesCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            correlationId: String,
            userAttributes: Map<String?, String?>?
        ): SignUpSubmitUserAttributesCommandParameters? {
            val authority = configuration.defaultAuthority as NativeAuthCIAMAuthority
            return SignUpSubmitUserAttributesCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .clientId(configuration.clientId)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .userAttributes(userAttributes!!)
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignUpSubmitPasswordCommand]] of Native Auth.
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param continuationToken Continuation token received from the start command
         * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
         * @param password password for the user
         * @return Command parameter object
         * @throws ClientException
         */
        fun createSignUpSubmitPasswordCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            correlationId: String,
            password: CharArray
        ): SignUpSubmitPasswordCommandParameters? {
            val authority = configuration.defaultAuthority as NativeAuthCIAMAuthority
            return SignUpSubmitPasswordCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .password(password)
                .correlationId(correlationId)
                .build()
        }

        //endregion

        //region signIn commands

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignInStartCommand]] of Native Auth using username and password
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param username email address of the user
         * @param password password of the user
         * @param scopes scopes requested during sign in flow
         * @param claimsRequest claims request object. Nullable object
         * @return Command parameter object
         * @throws ClientException
         */
        @Throws(ClientException::class)
        fun createSignInStartCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            username: String,
            password: CharArray?,
            scopes: List<String?>?,
            claimsRequest: ClaimsRequest?
        ): SignInStartCommandParameters? {
            val authenticationScheme =
                AuthenticationSchemeFactory.createScheme(
                    AndroidPlatformComponentsFactory.createFromContext(
                        configuration.appContext
                    ),
                    null
                )
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            val claimsRequestJson =
                ClaimsRequest.getJsonStringFromClaimsRequest(
                    claimsRequest
                )
            return SignInStartCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .username(username)
                .password(password)
                .authenticationScheme(authenticationScheme)
                .clientId(configuration.clientId)
                .challengeType(configuration.getChallengeTypes())
                .claimsRequestJson(claimsRequestJson)
                .scopes(scopes) // Start of the flow, so there is no correlation ID to use from a previous API response.
                // Set it to a default value.
                .correlationId(DiagnosticContext.INSTANCE.threadCorrelationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignInStartCommand]] of Native Auth using continuation token
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param continuationToken continuation token
         * @param username email address of the user
         * @param correlationId correlation ID to use in the API request, taken from the previous API response in the flow
         * @param scopes scopes requested during sign in flow
         * @param claimsRequest claims request object. Nullable object
         * @return Command parameter object
         * @throws ClientException
         */
        @Throws(ClientException::class)
        fun createSignInWithContinuationTokenCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            username: String,
            correlationId: String,
            scopes: List<String?>?,
            claimsRequest: ClaimsRequest?
        ): SignInWithContinuationTokenCommandParameters? {
            val authenticationScheme =
                AuthenticationSchemeFactory.createScheme(
                    AndroidPlatformComponentsFactory.createFromContext(
                        configuration.appContext
                    ),
                    null
                )
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            val claimsRequestJson =
                ClaimsRequest.getJsonStringFromClaimsRequest(
                    claimsRequest
                )
            return SignInWithContinuationTokenCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .continuationToken(continuationToken)
                .username(username)
                .challengeType(configuration.getChallengeTypes())
                .authenticationScheme(authenticationScheme)
                .claimsRequestJson(claimsRequestJson)
                .scopes(scopes)
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignInSubmitCodeCommand]] of Native Auth
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param code Out of band code
         * @param continuationToken continuation token
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param scopes scopes requested during sign in flow
         * @return Command parameter object
         * @throws ClientException
         */
        @Throws(ClientException::class)
        fun createSignInSubmitCodeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            code: String,
            continuationToken: String,
            correlationId: String,
            scopes: List<String?>?,
            claimsRequestJson: String?
        ): SignInSubmitCodeCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            val authenticationScheme =
                AuthenticationSchemeFactory.createScheme(
                    AndroidPlatformComponentsFactory.createFromContext(
                        configuration.appContext
                    ),
                    null
                )
            return SignInSubmitCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .continuationToken(continuationToken)
                .authenticationScheme(authenticationScheme)
                .challengeType(configuration.getChallengeTypes())
                .code(code)
                .scopes(scopes)
                .correlationId(correlationId)
                .claimsRequestJson(claimsRequestJson)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignInResendCodeCommand]] of Native Auth
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken continuation token
         * @return Command parameter object
         * @throws ClientException
         */
        fun createSignInResendCodeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            correlationId: String,
            continuationToken: String
        ): SignInResendCodeCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            return SignInResendCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .challengeType(configuration.getChallengeTypes())
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.SignInSubmitPasswordCommand]] of Native Auth
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken continuation token
         * @param password  password of the user
         * @param scopes
         * @return Command parameter object
         * @throws ClientException
         */
        @Throws(ClientException::class)
        fun createSignInSubmitPasswordCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            password: CharArray,
            correlationId: String,
            scopes: List<String?>?,
            claimsRequestJson: String?
        ): SignInSubmitPasswordCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            val authenticationScheme =
                AuthenticationSchemeFactory.createScheme(
                    AndroidPlatformComponentsFactory.createFromContext(
                        configuration.appContext
                    ),
                    null
                )
            return SignInSubmitPasswordCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .authenticationScheme(authenticationScheme)
                .continuationToken(continuationToken)
                .password(password)
                .scopes(scopes)
                .challengeType(configuration.getChallengeTypes())
                .correlationId(correlationId)
                .claimsRequestJson(claimsRequestJson)
                .build()
        }

        //endregion

        //region MFA commands

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.MFAChallengeCommand]] of Native Auth
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken continuation token
         * @param scopes scopes requested during sign in flow
         * @return Command parameter object
         * @throws ClientException
         */
        @Throws(ClientException::class)
        fun createMFADefaultChallengeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            correlationId: String,
            scopes: List<String?>?
        ): MFADefaultChallengeCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            val authenticationScheme =
                AuthenticationSchemeFactory.createScheme(
                    AndroidPlatformComponentsFactory.createFromContext(
                        configuration.appContext
                    ),
                    null
                )
            return MFADefaultChallengeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .authenticationScheme(authenticationScheme)
                .continuationToken(continuationToken)
                .scopes(scopes)
                .challengeType(configuration.getChallengeTypes())
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.MFAChallengeCommand]] of Native Auth
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken continuation token
         * @param authMethod the user's authentication method that is used to perform the challenge operation
         * @return Command parameter object
         * @throws ClientException
         */
        @Throws(ClientException::class)
        fun createMFASelectedChallengeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            correlationId: String,
            authMethod: AuthMethod
        ): MFASelectedDefaultChallengeCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            val authenticationScheme =
                AuthenticationSchemeFactory.createScheme(
                    AndroidPlatformComponentsFactory.createFromContext(
                        configuration.appContext
                    ),
                    null
                )
            val authMethodId = authMethod.id
            return MFASelectedDefaultChallengeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .authenticationScheme(authenticationScheme)
                .continuationToken(continuationToken)
                .challengeType(configuration.getChallengeTypes())
                .authMethodId(authMethodId)
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.MFAChallengeCommand]] of Native Auth
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param challenge value of the challenge
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken Continuation token
         * @param scopes scopes requested during sign in flow
         * @return Command parameter object
         */
        @Throws(ClientException::class)
        fun createMFASubmitChallengeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            challenge: String,
            correlationId: String,
            continuationToken: String,
            scopes: List<String?>?
        ): MFASubmitChallengeCommandParameters? {
            val authenticationScheme =
                AuthenticationSchemeFactory.createScheme(
                    AndroidPlatformComponentsFactory.createFromContext(
                        configuration.appContext
                    ),
                    null
                )
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            return MFASubmitChallengeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .authenticationScheme(authenticationScheme)
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .challenge(challenge)
                .scopes(scopes)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .clientId(configuration.clientId)
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.GetAuthMethodsCommand]] of Native Auth
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken Continuation token
         * @return Command parameter object
         */
        fun createGetAuthMethodsCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            correlationId: String
        ): GetAuthMethodsCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            return GetAuthMethodsCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .continuationToken(continuationToken)
                .challengeType(configuration.getChallengeTypes())
                .correlationId(correlationId)
                .build()
        }

        //endregion

        //region JIT commands
        /**
         * Creates command parameter for [[com.microsoft.identity.common.nativeauth.internal.commands.JITChallengeAuthMethodCommand]] of Native Auth
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param verificationContact verification contact
         * @param challengeType challenge type
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken Continuation token
         * @return Command parameter object
         */
        fun createChallengeAuthMethodCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            verificationContact: String,
            challengeType: String,
            correlationId: String,
            continuationToken: String
        ): JITChallengeAuthMethodCommandParameters {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            return JITChallengeAuthMethodCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .verificationContact(verificationContact)
                .authMethodChallengeType(challengeType)
                .continuationToken(continuationToken)
                .clientId(configuration.clientId)
                .correlationId(correlationId)
                .build()
        }
        //endregion

        //region reset password commands

        /**
         * Creates command parameter for [ResetPasswordStartCommand] of Native Auth.
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param username username associated with password change
         * @return Command parameter object
         */
        fun createResetPasswordStartCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            username: String
        ): ResetPasswordStartCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            return ResetPasswordStartCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .username(username)
                .challengeType(configuration.getChallengeTypes())
                .clientId(configuration.clientId) // Start of the flow, so there is no correlation ID to use from a previous API response.
                // Set it to a default value.
                .correlationId(DiagnosticContext.INSTANCE.threadCorrelationId)
                .build()
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
        fun createResetPasswordSubmitCodeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            code: String,
            correlationId: String,
            continuationToken: String
        ): ResetPasswordSubmitCodeCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            return ResetPasswordSubmitCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .code(code)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .clientId(configuration.clientId)
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [ResetPasswordResendCodeCommand] of Native Auth.
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken Continuation token
         * @return Command parameter object
         */
        fun createResetPasswordResendCodeCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            correlationId: String,
            continuationToken: String
        ): ResetPasswordResendCodeCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            return ResetPasswordResendCodeCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .challengeType(configuration.getChallengeTypes())
                .continuationToken(continuationToken)
                .clientId(configuration.clientId)
                .correlationId(correlationId)
                .build()
        }

        /**
         * Creates command parameter for [ResetPasswordSubmitNewPasswordCommandParameters] of Native Auth.
         * @param configuration PCA configuration
         * @param tokenCache token cache for storing results
         * @param correlationId correlation ID to use in the API request, taken from the previous request in the flow
         * @param continuationToken password submit token
         * @return Command parameter object
         */
        fun createResetPasswordSubmitNewPasswordCommandParameters(
            configuration: NativeAuthPublicClientApplicationConfiguration,
            tokenCache: OAuth2TokenCache<*, *, *>,
            continuationToken: String,
            correlationId: String,
            password: CharArray
        ): ResetPasswordSubmitNewPasswordCommandParameters? {
            val authority =
                configuration.defaultAuthority as NativeAuthCIAMAuthority
            return ResetPasswordSubmitNewPasswordCommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(configuration.appContext))
                .applicationName(configuration.appContext.packageName)
                .applicationVersion(CommandParametersAdapter.getPackageVersion(configuration.appContext))
                .clientId(configuration.clientId)
                .isSharedDevice(configuration.isSharedDevice)
                .redirectUri(configuration.redirectUri)
                .oAuth2TokenCache(tokenCache)
                .requiredBrokerProtocolVersion(configuration.requiredBrokerProtocolVersion)
                .sdkType(SdkType.MSAL)
                .sdkVersion(PublicClientApplication.getSdkVersion())
                .powerOptCheckEnabled(configuration.isPowerOptCheckForEnabled)
                .authority(authority)
                .continuationToken(continuationToken)
                .challengeType(configuration.getChallengeTypes())
                .newPassword(password)
                .clientId(configuration.clientId)
                .correlationId(correlationId)
                .build()
        }

        //endregion
    }
}