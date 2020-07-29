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
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.commands.DeviceCodeFlowCommand;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.IErrorResponse;
import com.microsoft.identity.common.internal.providers.oauth2.IResult;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.ApiStartEvent;
import com.microsoft.identity.common.internal.ui.AuthorizationStrategyFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.adal.internal.net.HttpWebRequest.throwIfNetworkNotAvailable;

public class LocalMSALController extends BaseController {

    private static final String TAG = LocalMSALController.class.getSimpleName();

    private AuthorizationStrategy mAuthorizationStrategy = null;
    private AuthorizationRequest mAuthorizationRequest = null;

    @Override
    public AcquireTokenResult acquireToken(
            @NonNull final InteractiveTokenCommandParameters parameters)
            throws ExecutionException, InterruptedException, ClientException, IOException, ArgumentException {
        final String methodName = ":acquireToken";

        Logger.verbose(
                TAG + methodName,
                "Acquiring token..."
        );

        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
        );

        final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        //00) Validate MSAL Parameters
        parameters.validate();

        // Add default scopes
        final Set<String> mergedScopes = addDefaultScopes(parameters);

        final InteractiveTokenCommandParameters parametersWithScopes = parameters
                .toBuilder()
                .scopes(mergedScopes)
                .build();

        logParameters(TAG, parametersWithScopes);

        //0) Get known authority result
        throwIfNetworkNotAvailable(
                parametersWithScopes.getAndroidApplicationContext(),
                parametersWithScopes.isPowerOptCheckEnabled()
        );

        Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parametersWithScopes.getAuthority());

        //0.1 If not known throw resulting exception
        if (!authorityResult.getKnown()) {
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(authorityResult.getClientException())
                            .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
            );

            throw authorityResult.getClientException();
        }

        // Build up params for Strategy construction
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        strategyParameters.setContext(parametersWithScopes.getAndroidApplicationContext());

        //1) Get oAuth2Strategy for Authority Type
        final OAuth2Strategy oAuth2Strategy = parametersWithScopes
                .getAuthority()
                .createOAuth2Strategy(strategyParameters);


        //2) Request authorization interactively
        final AuthorizationResult result = performAuthorizationRequest(
                oAuth2Strategy,
                parametersWithScopes.getAndroidApplicationContext(),
                parametersWithScopes
        );
        acquireTokenResult.setAuthorizationResult(result);

        logResult(TAG, result);

        if (result.getAuthorizationStatus().equals(AuthorizationStatus.SUCCESS)) {
            //3) Exchange authorization code for token
            final TokenResult tokenResult = performTokenRequest(
                    oAuth2Strategy,
                    mAuthorizationRequest,
                    result.getAuthorizationResponse(),
                    parametersWithScopes
            );

            acquireTokenResult.setTokenResult(tokenResult);

            if (tokenResult != null && tokenResult.getSuccess()) {
                //4) Save tokens in token cache
                final List<ICacheRecord> records = saveTokens(
                        oAuth2Strategy,
                        mAuthorizationRequest,
                        tokenResult.getTokenResponse(),
                        parametersWithScopes.getOAuth2TokenCache()
                );

                // The first element in the returned list is the item we *just* saved, the rest of
                // the elements are necessary to construct the full IAccount + TenantProfile
                final ICacheRecord newestRecord = records.get(0);

                acquireTokenResult.setLocalAuthenticationResult(
                        new LocalAuthenticationResult(
                                finalizeCacheRecordForResult(
                                        newestRecord,
                                        parametersWithScopes.getAuthenticationScheme()
                                ),
                                records,
                                SdkType.MSAL,
                                false
                        )
                );
            }
        }

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenResult)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
        );

        return acquireTokenResult;
    }

    private AuthorizationResult performAuthorizationRequest(@NonNull final OAuth2Strategy strategy,
                                                            @NonNull final Context context,
                                                            @NonNull final InteractiveTokenCommandParameters parameters)
            throws ExecutionException, InterruptedException, ClientException {

        throwIfNetworkNotAvailable(context, parameters.isPowerOptCheckEnabled());

        mAuthorizationStrategy = AuthorizationStrategyFactory.getInstance()
                .getAuthorizationStrategy(
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
            @NonNull final SilentTokenCommandParameters parameters)
            throws IOException, ClientException, ArgumentException, ServiceException {
        final String methodName = ":acquireTokenSilent";
        Logger.verbose(
                TAG + methodName,
                "Acquiring token silently..."
        );

        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
        );

        final AcquireTokenResult acquireTokenSilentResult = new AcquireTokenResult();

        //Validate MSAL Parameters
        parameters.validate();

        // Add default scopes
        final Set<String> mergedScopes = addDefaultScopes(parameters);

        final SilentTokenCommandParameters parametersWithScopes = parameters
                .toBuilder()
                .scopes(mergedScopes)
                .build();

        final OAuth2TokenCache tokenCache = parametersWithScopes.getOAuth2TokenCache();

        final AccountRecord targetAccount = getCachedAccountRecord(parametersWithScopes);

        // Build up params for Strategy construction
        final AbstractAuthenticationScheme authScheme = parametersWithScopes.getAuthenticationScheme();
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        strategyParameters.setContext(parametersWithScopes.getAndroidApplicationContext());

        final OAuth2Strategy strategy = parametersWithScopes.getAuthority().createOAuth2Strategy(strategyParameters);

        final List<ICacheRecord> cacheRecords = tokenCache.loadWithAggregatedAccountData(
                parametersWithScopes.getClientId(),
                TextUtils.join(" ", parametersWithScopes.getScopes()),
                targetAccount,
                authScheme
        );

        // The first element is the 'fully-loaded' CacheRecord which may contain the AccountRecord,
        // AccessTokenRecord, RefreshTokenRecord, and IdTokenRecord... (if all of those artifacts exist)
        // subsequent CacheRecords represent other profiles (projections) of this principal in
        // other tenants. Those tokens will be 'sparse', meaning that their AT/RT will not be loaded
        final ICacheRecord fullCacheRecord = cacheRecords.get(0);

        if (accessTokenIsNull(fullCacheRecord)
                || refreshTokenIsNull(fullCacheRecord)
                || parametersWithScopes.isForceRefresh()
                || !isRequestAuthorityRealmSameAsATRealm(parametersWithScopes.getAuthority(), fullCacheRecord.getAccessToken())
                || !strategy.validateCachedResult(authScheme, fullCacheRecord)) {
            if (!refreshTokenIsNull(fullCacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                Logger.verbose(
                        TAG + methodName,
                        "No access token found, but RT is available."
                );

                renewAccessToken(
                        parametersWithScopes,
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
                    parametersWithScopes,
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
                            finalizeCacheRecordForResult(
                                    fullCacheRecord,
                                    parametersWithScopes.getAuthenticationScheme()
                            ),
                            cacheRecords,
                            SdkType.MSAL,
                            true
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
    public List<ICacheRecord> getAccounts(@NonNull final CommandParameters parameters) {
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_GET_ACCOUNTS)
        );

        final List<ICacheRecord> accountsInCache =
                parameters
                        .getOAuth2TokenCache()
                        .getAccountsWithAggregatedAccountData(
                                null, // * wildcard
                                parameters.getClientId()
                        );

        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_GET_ACCOUNTS)
                        .put(TelemetryEventStrings.Key.ACCOUNTS_NUMBER, Integer.toString(accountsInCache.size()))
                        .put(TelemetryEventStrings.Key.IS_SUCCESSFUL, TelemetryEventStrings.Value.TRUE)
        );

        return accountsInCache;
    }

    @Override
    @WorkerThread
    public boolean removeAccount(
            @NonNull final RemoveAccountCommandParameters parameters) {
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_REMOVE_ACCOUNT)
        );

        String realm = null;

        if (parameters.getAccount() != null) {
            realm = parameters.getAccount().getRealm();
        }

        final boolean localRemoveAccountSuccess = !parameters
                .getOAuth2TokenCache()
                .removeAccount(
                        parameters.getAccount() == null ? null : parameters.getAccount().getEnvironment(),
                        parameters.getClientId(),
                        parameters.getAccount() == null ? null : parameters.getAccount().getHomeAccountId(),
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
    public boolean getDeviceMode(CommandParameters parameters) throws Exception {
        final String methodName = ":getDeviceMode";

        final String errorMessage = "LocalMSALController is not eligible to use the broker. Do not check sharedDevice mode and return false immediately.";
        com.microsoft.identity.common.internal.logging.Logger.warn(TAG + methodName, errorMessage);

        return false;
    }

    @Override
    public List<ICacheRecord> getCurrentAccount(CommandParameters parameters) throws Exception {
        return getAccounts(parameters);
    }

    @Override
    public boolean removeCurrentAccount(RemoveAccountCommandParameters parameters) throws Exception {
        return removeAccount(parameters);
    }

    @Override
    public AuthorizationResult deviceCodeFlowAuthRequest(final DeviceCodeFlowCommandParameters parameters) throws MsalException, ClientException, IOException {
        // Logging start of method
        final String methodName = ":deviceCodeFlowAuthRequest";
        Logger.verbose(
                TAG + methodName,
                "Device Code Flow: Authorizing user code..."
        );

        // Default scopes here
        final Set<String> mergedScopes = addDefaultScopes(parameters);

        final DeviceCodeFlowCommandParameters parametersWithScopes = parameters
                .toBuilder()
                .scopes(mergedScopes)
                .build();

        logParameters(TAG, parametersWithScopes);

        // Start telemetry with LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parametersWithScopes)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE)
        );

        // Create OAuth2Strategy using commandParameters and strategyParameters
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        strategyParameters.setContext(parametersWithScopes.getAndroidApplicationContext());

        final OAuth2Strategy oAuth2Strategy = parametersWithScopes
                .getAuthority()
                .createOAuth2Strategy(strategyParameters);

        // DCF protocol step 1: Get user code
        // Populate global authorization request
        mAuthorizationRequest = getAuthorizationRequest(oAuth2Strategy, parametersWithScopes);

        // Call method defined in oAuth2Strategy to request authorization
        final AuthorizationResult authorizationResult = oAuth2Strategy.getDeviceCode((MicrosoftStsAuthorizationRequest) mAuthorizationRequest, null);

        validateServiceResult(authorizationResult);

        Logger.verbose(
                TAG + methodName,
                "Device Code Flow authorization step finished..."
        );
        logResult(TAG, authorizationResult);

        // End telemetry with LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE
        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE)
        );

        return authorizationResult;
    }

    @Override
    public AcquireTokenResult acquireDeviceCodeFlowToken(
            final AuthorizationResult authorizationResult,
            final DeviceCodeFlowCommandParameters parameters)
            throws MsalException, ClientException, IOException, InterruptedException {

        // Logging start of method
        final String methodName = ":acquireDeviceCodeFlowToken";
        Logger.verbose(
                TAG + methodName,
                "Device Code Flow: Polling for token..."
        );

        // Start telemetry with LOCAL_DEVICE_CODE_FLOW_POLLING
        Telemetry.emit(
                new ApiStartEvent()
                        .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_POLLING)
        );

        // Create empty AcquireTokenResult object
        final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();

        // Assign authorization result
        acquireTokenResult.setAuthorizationResult(authorizationResult);

        // Fetch the Authorization Response
        final MicrosoftStsAuthorizationResponse authorizationResponse = (MicrosoftStsAuthorizationResponse) authorizationResult.getAuthorizationResponse();

        // Create OAuth2Strategy using commandParameters and strategyParameters
        final OAuth2StrategyParameters strategyParameters = new OAuth2StrategyParameters();
        strategyParameters.setContext(parameters.getAndroidApplicationContext());

        final OAuth2Strategy oAuth2Strategy = parameters
                .getAuthority()
                .createOAuth2Strategy(strategyParameters);

        // DCF protocol step 2: Poll for token
        TokenResult tokenResult = null;

        // Create token request outside of loop so it isn't re-created after every loop
        final MicrosoftStsTokenRequest tokenRequest = (MicrosoftStsTokenRequest) oAuth2Strategy.createTokenRequest(
                mAuthorizationRequest,
                authorizationResponse,
                parameters.getAuthenticationScheme()
        );

        // Fetch wait interval
        final int interval = Integer.parseInt(authorizationResponse.getInterval()) * 1000;

        String errorCode = ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_CODE;

        // Loop to send multiple requests checking for token
        while (errorCode.equals(ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_CODE)) {
            errorCode = ""; // Reset error code

            // Execute Token Request
            tokenResult = oAuth2Strategy.requestToken(tokenRequest);

            if (tokenResult.getErrorResponse() != null) {
                errorCode = tokenResult.getErrorResponse().getError();
            }

            if (errorCode.equals(ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_CODE)) {
                // interval is passed through params
                Thread.sleep(interval);
            }
        }

        // Assign token result
        acquireTokenResult.setTokenResult(tokenResult);

        // Validate request success, may throw MsalServiceException
        validateServiceResult(tokenResult);

        // If the token is valid, save it into token cache
        final List<ICacheRecord> records = saveTokens(
                oAuth2Strategy,
                mAuthorizationRequest,
                acquireTokenResult.getTokenResult().getTokenResponse(),
                parameters.getOAuth2TokenCache()
        );

        // Once the token is stored, fetch and assign the authentication result
        final ICacheRecord newestRecord = records.get(0);
        acquireTokenResult.setLocalAuthenticationResult(
                new LocalAuthenticationResult(
                        finalizeCacheRecordForResult(
                                newestRecord,
                                parameters.getAuthenticationScheme()
                        ),
                        records,
                        SdkType.MSAL,
                        false
                )
        );

        logResult(TAG, tokenResult);

        // End telemetry with LOCAL_DEVICE_CODE_FLOW_POLLING
        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenResult)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_DEVICE_CODE_FLOW_POLLING)
        );

        return acquireTokenResult;
    }

    /**
     * Helper method to check if a result object is valid (was a success). If not, an exception will be generated and thrown.
     * This method is called in both parts of the DCF protocol.
     * @param result result object to be checked
     * @throws MsalServiceException MsalServiceException object reflecting error code returned by the result
     */
    private void validateServiceResult(@NonNull final IResult result) throws MsalServiceException {
        // If result was unsuccessful, create an exception
        if (!result.getSuccess()) {
            // Create ServiceException object
            final ServiceException serviceException = createServiceExceptionForDeviceCodeFlow(result.getErrorResponse());

            // Convert ServiceException to MsalServiceException, then throw
            throw new MsalServiceException(
                    serviceException.getErrorCode(),
                    serviceException.getMessage(),
                    serviceException.getHttpStatusCode(),
                    serviceException
            );
        }
    }

    /**
     * Given an error response object, create a serviceException object using the predefined error codes.
     * @param response error response object to be checked
     * @return an exception object
     */
    private ServiceException createServiceExceptionForDeviceCodeFlow(@NonNull final IErrorResponse response) {
        // Based on error code, fetch the error message
        final String errorCode = response.getError();
        final String errorMessage;

        // Check response code against pre-defined error codes
        switch (errorCode) {
            case ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_DECLINED_CODE:
                errorMessage = ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_DECLINED_MESSAGE;
                break;
            case ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_CODE:
                errorMessage = ErrorStrings.DEVICE_CODE_FLOW_EXPIRED_TOKEN_MESSAGE;
                break;
            case AuthenticationConstants.OAuth2ErrorCode.INVALID_GRANT:
                errorMessage = ErrorStrings.DEVICE_CODE_FLOW_INVALID_GRANT_MESSAGE;
                break;
            default:
                errorMessage = ErrorStrings.DEVICE_CODE_FLOW_DEFAULT_ERROR_MESSAGE;
        }

        // Create a ServiceException object and return it
        return new ServiceException(
                errorCode,
                errorMessage,
                ServiceException.DEFAULT_STATUS_CODE,
                null
        );
    }
}
