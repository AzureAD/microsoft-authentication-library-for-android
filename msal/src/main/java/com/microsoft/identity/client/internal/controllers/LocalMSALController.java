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
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStatus;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationStrategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.microsoft.identity.common.adal.internal.net.HttpWebRequest.throwIfNetworkNotAvailable;

public class LocalMSALController extends BaseController {

    private static final String TAG = LocalMSALController.class.getSimpleName();

    private AuthorizationStrategy mAuthorizationStrategy = null;
    private AuthorizationRequest mAuthorizationRequest = null;

    @Override
    public AcquireTokenResult acquireToken(@NonNull final AcquireTokenOperationParameters parameters)
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
        addDefaultScopes(parameters);

        logParameters(TAG, parameters);

        //0) Get known authority result
        throwIfNetworkNotAvailable(parameters.getAppContext());
        Authority.KnownAuthorityResult authorityResult = Authority.getKnownAuthorityResult(parameters.getAuthority());

        //0.1 If not known throw resulting exception
        if (!authorityResult.getKnown()) {
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(authorityResult.getClientException())
                            .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
            );

            throw authorityResult.getClientException();
        }

        //1) Get oAuth2Strategy for Authority Type
        final OAuth2Strategy oAuth2Strategy = parameters.getAuthority().createOAuth2Strategy();

        //2) Request authorization interactively
        final AuthorizationResult result = performAuthorizationRequest(oAuth2Strategy, parameters);
        acquireTokenResult.setAuthorizationResult(result);

        logResult(TAG, result);

        if (result.getAuthorizationStatus().equals(AuthorizationStatus.SUCCESS)) {
            //3) Exchange authorization code for token
            final TokenResult tokenResult = performTokenRequest(
                    oAuth2Strategy,
                    mAuthorizationRequest,
                    result.getAuthorizationResponse(),
                    parameters
            );

            acquireTokenResult.setTokenResult(tokenResult);

            if (tokenResult != null && tokenResult.getSuccess()) {
                //4) Save tokens in token cache
                final List<ICacheRecord> records = saveTokens(
                        oAuth2Strategy,
                        mAuthorizationRequest,
                        tokenResult.getTokenResponse(),
                        parameters.getTokenCache()
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

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenResult)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_INTERACTIVE)
        );

        return acquireTokenResult;
    }

    private AuthorizationResult performAuthorizationRequest(@NonNull final OAuth2Strategy strategy,
                                                            @NonNull final AcquireTokenOperationParameters parameters)
            throws ExecutionException, InterruptedException, ClientException {

        throwIfNetworkNotAvailable(parameters.getAppContext());

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
            @NonNull final AcquireTokenSilentOperationParameters parameters)
            throws IOException, ClientException, ArgumentException {
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
        addDefaultScopes(parameters);

        final OAuth2TokenCache tokenCache = parameters.getTokenCache();

        final AccountRecord targetAccount = getCachedAccountRecord(parameters);

        final OAuth2Strategy strategy = parameters.getAuthority().createOAuth2Strategy();

        final List<ICacheRecord> cacheRecords = tokenCache.loadWithAggregatedAccountData(
                parameters.getClientId(),
                TextUtils.join(" ", parameters.getScopes()),
                targetAccount
        );

        // The first element is the 'fully-loaded' CacheRecord which may contain the AccountRecord,
        // AccessTokenRecord, RefreshTokenRecord, and IdTokenRecord... (if all of those artifacts exist)
        // subsequent CacheRecords represent other profiles (projections) of this principal in
        // other tenants. Those tokens will be 'sparse', meaning that their AT/RT will not be loaded
        final ICacheRecord fullCacheRecord = cacheRecords.get(0);

        if (accessTokenIsNull(fullCacheRecord)
                || refreshTokenIsNull(fullCacheRecord)
                || parameters.getForceRefresh()) {
            if (!refreshTokenIsNull(fullCacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                Logger.verbose(
                        TAG + methodName,
                        "No access token found, but RT is available."
                );

                renewAccessToken(
                        parameters,
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
                    parameters,
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
    public List<ICacheRecord> getAccounts(@NonNull final OperationParameters parameters) {
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_GET_ACCOUNTS)
        );

        final List<ICacheRecord> accountsInCache =
                parameters
                        .getTokenCache()
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
    public boolean removeAccount(@NonNull final OperationParameters parameters) {
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
                .getTokenCache()
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
    public boolean getDeviceMode(OperationParameters parameters) throws Exception {
        final String methodName = ":getDeviceMode";

        final String errorMessage = "LocalMSALControler is not eligible to use the broker. Do not check sharedDevice mode and return false immediately.";
        com.microsoft.identity.common.internal.logging.Logger.error(TAG + methodName, errorMessage, null);

        return false;
    }

    @Override
    public List<ICacheRecord> getCurrentAccount(OperationParameters parameters) throws Exception {
        return getAccounts(parameters);
    }

    @Override
    public boolean removeCurrentAccount(OperationParameters parameters) throws Exception {
        return removeAccount(parameters);
    }
}
