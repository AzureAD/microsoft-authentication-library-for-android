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

import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.adal.internal.net.HttpWebRequest;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAuthority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.dto.RefreshTokenRecord;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftTokenRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.request.generated.CommandContext;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.GetDeviceModeCommandParameters;
import com.microsoft.identity.common.internal.request.generated.IContext;
import com.microsoft.identity.common.internal.request.generated.ITokenRequestParameters;
import com.microsoft.identity.common.internal.request.generated.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.RemoveCurrentAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.CliTelemInfo;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.ApiStartEvent;
import com.microsoft.identity.common.internal.telemetry.events.CacheEndEvent;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.adal.internal.net.HttpWebRequest.throwIfNetworkNotAvailable;

public class LocalMSALController extends BaseController<InteractiveTokenCommandParameters,
        SilentTokenCommandParameters> {

    private static final String TAG = LocalMSALController.class.getSimpleName();

    private AuthorizationStrategy mAuthorizationStrategy = null;
    private AuthorizationRequest mAuthorizationRequest = null;

    @Override
    public AcquireTokenResult acquireToken(
            @NonNull final CommandContext context,
            @NonNull final InteractiveTokenCommandParameters parameters)
            throws ExecutionException, InterruptedException, ClientException, IOException, ArgumentException {
        final String methodName = ":acquireToken";

        Logger.verbose(
                TAG + methodName,
                "Acquiring token..."
        );

        /*
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
        );
        */


        final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        logParameters(TAG, parameters);

        //0) Get known authority result
        throwIfNetworkNotAvailable(context.androidApplicationContext());
        Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parameters.authority());

        //0.1 If not known throw resulting exception
        if (!authorityResult.getKnown()) {
            /*
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(authorityResult.getClientException())
                            .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
            );
             */

            throw authorityResult.getClientException();
        }

        //1) Get oAuth2Strategy for Authority Type
        final OAuth2Strategy oAuth2Strategy = parameters.authority().createOAuth2Strategy();

        // Add default scopes
        InteractiveTokenCommandParameters paramsWithDefaultScopesAdded = parameters.addDefaultScopes(oAuth2Strategy.getDefaultScopes());

        //2) Request authorization interactively
        final AuthorizationResult result = performAuthorizationRequest(oAuth2Strategy, context, paramsWithDefaultScopesAdded);
        acquireTokenResult.setAuthorizationResult(result);

        logResult(TAG, result);

        if (result.getAuthorizationStatus().equals(AuthorizationStatus.SUCCESS)) {
            //3) Exchange authorization code for token
            final TokenResult tokenResult = performTokenRequest(
                    oAuth2Strategy,
                    mAuthorizationRequest,
                    result.getAuthorizationResponse(),
                    context
            );

            acquireTokenResult.setTokenResult(tokenResult);

            if (tokenResult != null && tokenResult.getSuccess()) {
                //4) Save tokens in token cache
                final List<ICacheRecord> records = saveTokens(
                        oAuth2Strategy,
                        mAuthorizationRequest,
                        tokenResult.getTokenResponse(),
                        context.tokenCache()
                );

                // The first element in the returned list is the item we *just* saved, the rest of
                // the elements are necessary to construct the full IAccount + TenantProfile
                final ICacheRecord newestRecord = records.get(0);

                acquireTokenResult.setLocalAuthenticationResult(
                        new LocalAuthenticationResult(
                                newestRecord,
                                records,
                                SdkType.MSAL
                        )
                );
            }
        }

        /*
        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenResult)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
        );
         */

        return acquireTokenResult;
    }

    private AuthorizationResult performAuthorizationRequest(@NonNull final OAuth2Strategy strategy,
                                                            @NonNull final CommandContext context,
                                                            @NonNull final InteractiveTokenCommandParameters parameters)
            throws ExecutionException, InterruptedException, ClientException {

        throwIfNetworkNotAvailable(context.androidApplicationContext());

        mAuthorizationStrategy = AuthorizationStrategyFactory.getInstance()
                .getAuthorizationStrategy(
                        context,
                        parameters
                );
        mAuthorizationRequest = getAuthorizationRequest(strategy, parameters);

        final Future<AuthorizationResult> future = strategy.requestAuthorization(
                mAuthorizationRequest,
                mAuthorizationStrategy
        );

        final AuthorizationResult result = future.get();

        return result;
    }

    @Override
    public void completeAcquireToken(final int requestCode,
                                     final int resultCode,
                                     final Intent data) {
        final String methodName = ":completeAcquireToken";
        Logger.verbose(
                TAG + methodName,
                "Completing acquire token..."
        );

        Telemetry.emit(
                new ApiStartEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
                        .put(TelemetryEventStrings.Key.RESULT_CODE, String.valueOf(resultCode))
                        .put(TelemetryEventStrings.Key.REQUEST_CODE, String.valueOf(requestCode))
        );

        mAuthorizationStrategy.completeAuthorization(requestCode, resultCode, data);

        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
        );
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(
            @NonNull final CommandContext context,
            @NonNull final SilentTokenCommandParameters parameters)
            throws IOException, ClientException, ArgumentException {
        final String methodName = ":acquireTokenSilent";
        Logger.verbose(
                TAG + methodName,
                "Acquiring token silently..."
        );

        /*
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
        );

         */

        final AcquireTokenResult acquireTokenSilentResult = new AcquireTokenResult();

        final OAuth2TokenCache tokenCache = context.tokenCache();

        final AccountRecord targetAccount = getCachedAccountRecord(context, parameters);

        final OAuth2Strategy strategy = parameters.authority().createOAuth2Strategy();

        final SilentTokenCommandParameters parametersWithDefaultScopesAdded = parameters.addDefaultScopes(strategy.getDefaultScopes());

        final List<ICacheRecord> cacheRecords = tokenCache.loadWithAggregatedAccountData(
                parameters.clientId(),
                TextUtils.join(" ", parametersWithDefaultScopesAdded.scopes()),
                targetAccount
        );

        // The first element is the 'fully-loaded' CacheRecord which may contain the AccountRecord,
        // AccessTokenRecord, RefreshTokenRecord, and IdTokenRecord... (if all of those artifacts exist)
        // subsequent CacheRecords represent other profiles (projections) of this principal in
        // other tenants. Those tokens will be 'sparse', meaning that their AT/RT will not be loaded
        final ICacheRecord fullCacheRecord = cacheRecords.get(0);

        if (accessTokenIsNull(fullCacheRecord)
                || refreshTokenIsNull(fullCacheRecord)
                || parameters.forceRefresh()
                || !isRequestAuthorityRealmSameAsATRealm(parameters.authority(), fullCacheRecord.getAccessToken())) {
            if (!refreshTokenIsNull(fullCacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                Logger.verbose(
                        TAG + methodName,
                        "No access token found, but RT is available."
                );

                renewAccessToken(
                        context,
                        parametersWithDefaultScopesAdded,
                        acquireTokenSilentResult,
                        tokenCache,
                        strategy,
                        fullCacheRecord
                );
            } else {
                //TODO need the refactor, should just throw the ui required exception, rather than
                // wrap the exception later in the exception wrapper.
                final ClientException exception = new ClientException(
                        MsalUiRequiredException.NO_TOKENS_FOUND,
                        "No refresh token was found. "
                );

                Telemetry.emit(
                        new ApiEndEvent()
                                .putException(exception)
                                .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
                );

                throw exception;
            }
        } else if (fullCacheRecord.getAccessToken().isExpired()) {
            Logger.warn(
                    TAG + methodName,
                    "Access token is expired. Removing from cache..."
            );
            // Remove the expired token
            tokenCache.removeCredential(fullCacheRecord.getAccessToken());

            Logger.verbose(
                    TAG + methodName,
                    "Renewing access token..."
            );
            // Request a new AT
            renewAccessToken(
                    context,
                    parametersWithDefaultScopesAdded,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    fullCacheRecord
            );
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Returning silent result"
            );
            // the result checks out, return that....
            acquireTokenSilentResult.setLocalAuthenticationResult(
                    new LocalAuthenticationResult(
                            fullCacheRecord,
                            cacheRecords,
                            SdkType.MSAL
                    )
            );
        }

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenSilentResult)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
        );

        return acquireTokenSilentResult;
    }

    @Override
    @WorkerThread
    public List<ICacheRecord> getAccounts(
            @NonNull final CommandContext context,
            @NonNull final LoadAccountCommandParameters parameters) {
        /*
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_GET_ACCOUNTS)
        );
        */

        final List<ICacheRecord> accountsInCache =
                context.tokenCache()
                        .getAccountsWithAggregatedAccountData(
                                null, // * wildcard
                                parameters.clientId()
                        );

        /*
        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_GET_ACCOUNTS)
                        .put(TelemetryEventStrings.Key.ACCOUNTS_NUMBER, Integer.toString(accountsInCache.size()))
                        .put(TelemetryEventStrings.Key.IS_SUCCESSFUL, TelemetryEventStrings.Value.TRUE)
        );
         */

        return accountsInCache;
    }

    @Override
    @WorkerThread
    public boolean removeAccount(
            @NonNull final CommandContext context,
            @NonNull final RemoveAccountCommandParameters parameters) {
        /*
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_REMOVE_ACCOUNT)
        );
         */

        String realm = null;

        if (parameters != null) {
            realm = parameters.accountRecord().getRealm();
        }

        final boolean localRemoveAccountSuccess = !context
                .tokenCache()
                .removeAccount(
                        parameters.accountRecord() == null ? null : parameters.accountRecord().getEnvironment(),
                        parameters.clientId(),
                        parameters.accountRecord() == null ? null : parameters.accountRecord().getHomeAccountId(),
                        realm
                ).isEmpty();

        Telemetry.emit(
                new ApiEndEvent()
                        .put(TelemetryEventStrings.Key.IS_SUCCESSFUL, String.valueOf(localRemoveAccountSuccess))
                        .putApiId(TelemetryEventStrings.Api.LOCAL_REMOVE_ACCOUNT)
        );

        return localRemoveAccountSuccess;
    }

    @Override
    public boolean getDeviceMode(
            @NonNull final CommandContext context,
            @NonNull final GetDeviceModeCommandParameters parameters) throws Exception {
        final String methodName = ":getDeviceMode";

        final String errorMessage = "LocalMSALController is not eligible to use the broker. Do not check sharedDevice mode and return false immediately.";
        com.microsoft.identity.common.internal.logging.Logger.warn(TAG + methodName, errorMessage);

        return false;
    }

    @Override
    public List<ICacheRecord> getCurrentAccount(
            @NonNull final CommandContext context,
            @NonNull final GetCurrentAccountCommandParameters parameters) throws Exception {
        //TODO: Need to convert 1 context to another...
        //return getAccounts(parameters);
        return null;
    }

    @Override
    public boolean removeCurrentAccount(
            @NonNull final CommandContext context,
            @NonNull final RemoveCurrentAccountCommandParameters parameters) throws Exception {

        //TODO: Need to convert 1 context to another
        //return removeAccount(parameters);
        return false;

    }

    @Override
    protected AuthorizationRequest.Builder initializeAuthorizationRequestBuilder(@NonNull AuthorizationRequest.Builder builder,
                                                                                 @NonNull InteractiveTokenCommandParameters parameters) {
        UUID correlationId = null;

        try {
            correlationId = UUID.fromString(DiagnosticContext.getRequestContext().get(DiagnosticContext.CORRELATION_ID));
        } catch (IllegalArgumentException ex) {
            Logger.error(TAG, "correlation id from diagnostic context is not a UUID", ex);
        }

        builder.setClientId(parameters.clientId())
                .setRedirectUri(parameters.redirectUri())
                .setCorrelationId(correlationId);

        // Set the multipleCloudAware and slice fields.
        if (parameters.authority() instanceof AzureActiveDirectoryAuthority) {
            final AzureActiveDirectoryAuthority requestAuthority = (AzureActiveDirectoryAuthority) parameters.authority();
            ((MicrosoftAuthorizationRequest.Builder) builder)
                    .setAuthority(requestAuthority.getAuthorityURL())
                    .setMultipleCloudAware(requestAuthority.mMultipleCloudsSupported)
                    .setSlice(requestAuthority.mSlice);
        }

        if (builder instanceof MicrosoftStsAuthorizationRequest.Builder) {
            ((MicrosoftStsAuthorizationRequest.Builder) builder).setTokenScope(TextUtils.join(" ", parameters.scopes()));
        }

        if (parameters.extraScopesToConsent() != null) {
            parameters.scopes().addAll(parameters.extraScopesToConsent());
        }

        // Add additional fields to the AuthorizationRequest.Builder to support interactive
        builder.setLoginHint(
                parameters.loginHint()
        ).setExtraQueryParams(
                parameters.extraQueryStringParameters()
        ).setPrompt(
                parameters.prompt().toString()
        ).setClaims(
                parameters.claimsRequestJson()
        ).setRequestHeaders(
                parameters.requestHeaders()
        );

        // We don't want to show the SELECT_ACCOUNT page if login_hint is set.
        if (!StringExtensions.isNullOrBlank(parameters.loginHint()) &&
                parameters.prompt() == OpenIdConnectPromptParameter.SELECT_ACCOUNT) {
            builder.setPrompt(null);
        }

        builder.setScope(TextUtils.join(" ", parameters.scopes()));

        return builder;
    }

    @Override
    protected AuthorizationRequest getAuthorizationRequest(@NonNull OAuth2Strategy strategy, @NonNull InteractiveTokenCommandParameters parameters) {
        AuthorizationRequest.Builder builder = strategy.createAuthorizationRequestBuilder(parameters.accountRecord());
        initializeAuthorizationRequestBuilder(builder, parameters);
        return builder.build();
    }

    @Override
    protected TokenResult performTokenRequest(@NonNull OAuth2Strategy strategy,
                                              @NonNull AuthorizationRequest request,
                                              @NonNull AuthorizationResponse response,
                                              @NonNull CommandContext context) throws IOException, ClientException {
        final String methodName = ":performTokenRequest";
        HttpWebRequest.throwIfNetworkNotAvailable(context.androidApplicationContext());

        TokenRequest tokenRequest = strategy.createTokenRequest(request, response);
        logExposedFieldsOfObject(TAG + methodName, tokenRequest);
        tokenRequest.setGrantType(TokenRequest.GrantTypes.AUTHORIZATION_CODE);

        TokenResult tokenResult = strategy.requestToken(tokenRequest);

        logResult(TAG, tokenResult);

        return tokenResult;
    }

    @Override
    protected void renewAccessToken(@NonNull CommandContext context,
                                    @NonNull SilentTokenCommandParameters parameters,
                                    @NonNull AcquireTokenResult acquireTokenSilentResult,
                                    @NonNull OAuth2TokenCache tokenCache,
                                    @NonNull OAuth2Strategy strategy,
                                    @NonNull ICacheRecord cacheRecord) throws IOException, ClientException {
        final String methodName = ":renewAccessToken";
        Logger.info(
                TAG + methodName,
                "Renewing access token..."
        );

        logParameters(TAG, parameters);

        final TokenResult tokenResult = performSilentTokenRequest(strategy, cacheRecord.getRefreshToken(), context, parameters);
        acquireTokenSilentResult.setTokenResult(tokenResult);

        logResult(TAG + methodName, tokenResult);

        if (tokenResult.getSuccess()) {
            Logger.info(
                    TAG + methodName,
                    "Token request was successful"
            );

            final List<ICacheRecord> savedRecords = tokenCache.saveAndLoadAggregatedAccountData(
                    strategy,
                    null,//getAuthorizationRequest(strategy, parameters),
                    tokenResult.getTokenResponse()
            );
            final ICacheRecord savedRecord = savedRecords.get(0);

            // Create a new AuthenticationResult to hold the saved record
            final LocalAuthenticationResult authenticationResult = new LocalAuthenticationResult(
                    savedRecord,
                    savedRecords,
                    SdkType.MSAL
            );

            // Set the client telemetry...
            if (null != tokenResult.getCliTelemInfo()) {
                final CliTelemInfo cliTelemInfo = tokenResult.getCliTelemInfo();
                authenticationResult.setSpeRing(cliTelemInfo.getSpeRing());
                authenticationResult.setRefreshTokenAge(cliTelemInfo.getRefreshTokenAge());
                Telemetry.emit(new CacheEndEvent().putSpeInfo(tokenResult.getCliTelemInfo().getSpeRing()));
            } else {
                // we can't put SpeInfo as the CliTelemInfo is null
                Telemetry.emit(new CacheEndEvent());
            }

            // Set the AuthenticationResult on the final result object
            acquireTokenSilentResult.setLocalAuthenticationResult(authenticationResult);
        }
    }

    @Override
    protected TokenResult performSilentTokenRequest(@NonNull OAuth2Strategy strategy,
                                                    @NonNull RefreshTokenRecord refreshTokenRecord,
                                                    @NonNull IContext context,
                                                    @NonNull ITokenRequestParameters parameters) throws ClientException, IOException {
        final String methodName = ":performSilentTokenRequest";

        Logger.info(
                TAG + methodName,
                "Requesting tokens..."
        );

        HttpWebRequest.throwIfNetworkNotAvailable(context.androidApplicationContext());

        // Check that the authority is known
        final Authority.KnownAuthorityResult authorityResult =
                Authority.getKnownAuthorityResult(parameters.authority());

        if (!authorityResult.getKnown()) {
            throw authorityResult.getClientException();
        }

        final TokenRequest refreshTokenRequest = strategy.createRefreshTokenRequest();
        refreshTokenRequest.setClientId(parameters.clientId());
        refreshTokenRequest.setScope(TextUtils.join(" ", parameters.scopes()));
        refreshTokenRequest.setRefreshToken(refreshTokenRecord.getSecret());
        refreshTokenRequest.setRedirectUri(parameters.redirectUri());

        if (refreshTokenRequest instanceof MicrosoftTokenRequest) {
            ((MicrosoftTokenRequest) refreshTokenRequest).setClaims(parameters.claimsRequestJson());
        }

        return strategy.requestToken(refreshTokenRequest);
    }

    @Override
    protected AccountRecord getCachedAccountRecord(
            @NonNull final CommandContext context,
            @NonNull final SilentTokenCommandParameters parameters) throws ClientException {
        if (parameters.account() == null) {
            throw new ClientException(
                    ErrorStrings.NO_ACCOUNT_FOUND,
                    "No cached accounts found for the supplied homeAccountId and clientId"
            );
        }

        final String clientId = parameters.clientId();
        final String homeAccountId = parameters.account().getHomeAccountId();
        final String localAccountId = parameters.account().getLocalAccountId();

        final AccountRecord targetAccount =
                context.tokenCache()
                        .getAccountByLocalAccountId(
                                null,
                                clientId,
                                localAccountId
                        );

        if (null == targetAccount) {
            Logger.info(
                    TAG,
                    "No accounts found for clientId ["
                            + clientId
                            + ", "
                            + "]",
                    null
            );
            Logger.errorPII(
                    TAG,
                    "No accounts found for clientId, homeAccountId: ["
                            + clientId
                            + ", "
                            + homeAccountId
                            + "]",
                    null
            );
            throw new ClientException(
                    ErrorStrings.NO_ACCOUNT_FOUND,
                    "No cached accounts found for the supplied homeAccountId"
            );
        }

        return targetAccount;
    }
}
