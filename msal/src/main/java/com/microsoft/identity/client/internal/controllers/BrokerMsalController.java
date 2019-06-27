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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.IMicrosoftAuthService;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.broker.BrokerResultFuture;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthServiceFuture;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.ExceptionAdapter;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.ApiStartEvent;
import com.microsoft.identity.common.internal.telemetry.events.BrokerEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.BrokerStartEvent;
import com.microsoft.identity.common.internal.ui.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.internal.util.ICacheRecordGsonAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.DEFAULT_BROWSER_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ENVIRONMENT;

/**
 * The implementation of MSAL Controller for Broker
 */
public class BrokerMsalController extends BaseController {

    private static final String TAG = BrokerMsalController.class.getSimpleName();

    private static final String DATA_USER_INFO = "com.microsoft.workaccount.user.info";
    private static final String DATA_CACHE_RECORD = "com.microsoft.workaccount.cache.record";
    private static final String MANIFEST_PERMISSION_GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
    private static final String MANIFEST_PERMISSION_MANAGE_ACCOUNTS = "android.permission.MANAGE_ACCOUNTS";
    private static final String MANIFEST_PERMISSION_USE_CREDENTIALS = "android.permission.USE_CREDENTIALS";

    private BrokerResultFuture mBrokerResultFuture;

    /**
     * ExecutorService to handle background computation.
     */
    private static final ExecutorService sBackgroundExecutor = Executors.newCachedThreadPool();

    @Override
    public AcquireTokenResult acquireToken(AcquireTokenOperationParameters parameters)
            throws InterruptedException, BaseException {
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.API_BROKER_GET_ACCOUNTS)
        );

        //Create BrokerResultFuture to block on response from the broker... response will be return as an activity result
        //BrokerActivity will receive the result and ask the API dispatcher to complete the request
        //In completeAcquireToken below we will set the result on the future and unblock the flow.
        mBrokerResultFuture = new BrokerResultFuture();

        //Get the broker interactive parameters intent
        final Intent interactiveRequestIntent = getBrokerAuthorizationIntent(parameters);

        //Pass this intent to the BrokerActivity which will be used to start this activity
        final Intent brokerActivityIntent = new Intent(parameters.getAppContext(), BrokerActivity.class);
        brokerActivityIntent.putExtra(BrokerActivity.BROKER_INTENT, interactiveRequestIntent);

        mBrokerResultFuture = new BrokerResultFuture();
        //Start the BrokerActivity
        parameters.getActivity().startActivity(brokerActivityIntent);

        //Wait to be notified of the result being returned... we could add a timeout here if we want to
        final Bundle resultBundle = mBrokerResultFuture.get();

        // For MSA Accounts Broker doesn't save the accounts, instead it just passes the result along,
        // MSAL needs to save this account locally for future token calls.
        saveMsaAccountToCache(resultBundle, (MsalOAuth2TokenCache) parameters.getTokenCache());
        final AcquireTokenResult result = getAcquireTokenResult(resultBundle);

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(result)
                        .putApiId(TelemetryEventStrings.API_BROKER_GET_ACCOUNTS)
        );

        return result;
    }

    /**
     * Get the intent for the broker interactive request
     *
     * @param parameters
     * @return
     */
    private Intent getBrokerAuthorizationIntent(@NonNull final AcquireTokenOperationParameters parameters) throws ClientException {
        final String methodName = ":getBrokerAuthorizationIntent";
        Intent interactiveRequestIntent;

        if (isMicrosoftAuthServiceSupported(parameters.getAppContext())) {
            Logger.verbose(TAG + methodName, "Is microsoft auth service supported? " + "[yes]");
            Logger.verbose(TAG + methodName, "Get the broker authorization intent from auth service.");
            interactiveRequestIntent = getBrokerAuthorizationIntentFromAuthService(parameters);
            final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();
            interactiveRequestIntent.putExtra(
                    AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                    new Gson().toJson(
                            msalBrokerRequestAdapter.brokerRequestFromAcquireTokenParameters(parameters),
                            BrokerRequest.class)
            );
            interactiveRequestIntent.putExtra(AuthenticationConstants.Broker.ACCOUNT_NAME, parameters.getLoginHint());
        } else {
            Logger.verbose(TAG + methodName, "Is microsoft auth service supported? " + "[no]");
            Logger.verbose(TAG + methodName, "Get the broker authorization intent from Account Manager.");
            interactiveRequestIntent = getBrokerAuthorizationIntentFromAccountManager(parameters);
        }

        return interactiveRequestIntent;
    }

    private Intent getBrokerAuthorizationIntentFromAuthService(@NonNull final AcquireTokenOperationParameters parameters)
            throws ClientException {
        final String methodName = ":getBrokerAuthorizationIntentFromAuthService";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
        );
        IMicrosoftAuthService service;
        Intent resultIntent;

        final MicrosoftAuthClient client = new MicrosoftAuthClient(parameters.getAppContext());
        final MicrosoftAuthServiceFuture authServiceFuture = client.connect();

        try {
            service = authServiceFuture.get();
            resultIntent = service.getIntentForInteractiveRequest();
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(true)
            );
        } catch (final RemoteException e) {
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_BIND_SERVICE_FAILED)
                            .putErrorDescription(e.getLocalizedMessage())
            );
            throw new ClientException(ErrorStrings.BROKER_BIND_SERVICE_FAILED,
                    "Exception occurred while attempting to invoke remote service",
                    e);
        } catch (final Exception e) {
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_BIND_SERVICE_FAILED)
                            .putErrorDescription(e.getLocalizedMessage())
            );
            throw new ClientException(ErrorStrings.BROKER_BIND_SERVICE_FAILED,
                    "Exception occurred while awaiting (get) return of MicrosoftAuthService",
                    e);
        } finally {
            client.disconnect();
        }

        return resultIntent;
    }

    @SuppressLint("MissingPermission")
    private Intent getBrokerAuthorizationIntentFromAccountManager(@NonNull final AcquireTokenOperationParameters parameters) throws ClientException {
        final String methodName = ":getBrokerAuthorizationIntentFromAccountManager";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
        );
        Intent intent = null;
        try {
            final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();

            final Bundle requestBundle = new Bundle();
            final BrokerRequest brokerRequest = msalBrokerRequestAdapter.
                    brokerRequestFromAcquireTokenParameters(parameters);

            requestBundle.putString(
                    AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                    new Gson().toJson(brokerRequest, BrokerRequest.class)
            );

            final AccountManager accountManager = AccountManager.get(parameters.getAppContext());
            final AccountManagerFuture<Bundle> result =
                    accountManager.addAccount(
                            AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                            AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                            null,
                            requestBundle,
                            null,
                            null,
                            getPreferredHandler()
                    );

            // Making blocking request here
            Bundle bundleResult = result.getResult();
            // Authenticator should throw OperationCanceledException if
            // token is not available
            intent = bundleResult.getParcelable(AccountManager.KEY_INTENT);
            intent.putExtra(
                    AuthenticationConstants.Broker.CALLER_INFO_UID,
                    Binder.getCallingUid()
            );

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(true)
            );
        } catch (final OperationCanceledException e) {
            Logger.error(
                    TAG + methodName,
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    "Exception thrown when talking to account manager. The broker request cancelled.",
                    e
            );

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                            .putErrorDescription("OperationCanceledException thrown when talking to account manager. The broker request cancelled.")
            );

            throw new ClientException(
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    "OperationCanceledException thrown when talking to account manager. The broker request cancelled.",
                    e
            );
        } catch (final AuthenticatorException e) {
            Logger.error(
                    TAG + methodName,
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    "AuthenticatorException thrown when talking to account manager. The broker request cancelled.",
                    e
            );

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                            .putErrorDescription("AuthenticatorException thrown when talking to account manager. The broker request cancelled.")
            );

            throw new ClientException(
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    "AuthenticatorException thrown when talking to account manager. The broker request cancelled.",
                    e
            );
        } catch (final IOException e) {
            // Authenticator gets problem from webrequest or file read/write
            Logger.error(
                    TAG + methodName,
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    "IOException thrown when talking to account manager. The broker request cancelled.",
                    e
            );

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                            .putErrorDescription("IOException thrown when talking to account manager. The broker request cancelled.")
            );

            throw new ClientException(
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    "IOException thrown when talking to account manager. The broker request cancelled.",
                    e
            );
        }

        return intent;
    }

    private Handler getPreferredHandler() {
        if (null != Looper.myLooper() && Looper.getMainLooper() != Looper.myLooper()) {
            return new Handler(Looper.myLooper());
        } else {
            return new Handler(Looper.getMainLooper());
        }
    }

    /**
     * Get the response from the Broker captured by BrokerActivity.
     * BrokerActivity will pass along the response to the broker controller
     * The Broker controller will map th response into the broker result
     * And signal the future with the broker result to unblock the request.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void completeAcquireToken(int requestCode, int resultCode, Intent data) {
        Telemetry.emit(
                new ApiStartEvent()
                        .putApiId(TelemetryEventStrings.API_BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
                        .put(TelemetryEventStrings.TELEMETRY_KEY_RESULT_CODE, String.valueOf(resultCode))
                        .put(TelemetryEventStrings.TELEMETRY_KEY_REQUEST_CODE, String.valueOf(requestCode))
        );

        mBrokerResultFuture.setResultBundle(data.getExtras());

        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.API_BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
        );
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters parameters) throws BaseException {
        final String methodName = ":acquireTokenSilent";

        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.API_BROKER_ACQUIRE_TOKEN_SILENT)
        );

        AcquireTokenResult acquireTokenResult;

        if (isMicrosoftAuthServiceSupported(parameters.getAppContext())) {
            Logger.verbose(TAG + methodName, "Is microsoft auth service supported? " + "[yes]");
            Logger.verbose(TAG + methodName, "Get the broker authorization intent from auth service.");

            acquireTokenResult = acquireTokenSilentWithAuthService(parameters);
        } else {
            Logger.verbose(TAG + methodName, "Is microsoft auth service supported? " + "[no]");
            Logger.verbose(TAG + methodName, "Get the broker authorization intent from Account Manager.");
            acquireTokenResult = acquireTokenSilentWithAccountManager(parameters);
        }

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenResult)
                        .putApiId(TelemetryEventStrings.API_BROKER_ACQUIRE_TOKEN_SILENT)
        );

        return acquireTokenResult;
    }

    private AcquireTokenResult acquireTokenSilentWithAuthService(AcquireTokenSilentOperationParameters parameters) throws BaseException {
        final String methodName = ":acquireTokenSilentWithAuthService";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
        );

        IMicrosoftAuthService service;

        MicrosoftAuthClient client = new MicrosoftAuthClient(parameters.getAppContext());
        MicrosoftAuthServiceFuture future = client.connect();

        try {
            //Do we want a time out here?
            service = future.get();
        } catch (Exception e) {
            throw new RuntimeException("Exception occurred while awaiting (get) return of MicrosoftAuthService", e);
        }

        try {
            final Bundle requestBundle = getSilentBrokerRequestBundle(parameters);
            final Bundle resultBundle = service.acquireTokenSilently(requestBundle);
            final AcquireTokenResult result = getAcquireTokenResult(resultBundle);

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(true)
            );

            return result;
        } catch (final RemoteException e) {

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_BIND_SERVICE_FAILED)
                            .putErrorDescription("RemoteException occurred while attempting to invoke remote service")
            );

            throw new ClientException(
                    ErrorStrings.BROKER_BIND_SERVICE_FAILED,
                    "Exception occurred while attempting to invoke remote service",
                    e
            );
        } finally {
            client.disconnect();
        }
    }

    /**
     * Get device mode from Broker.
     */
    public void getBrokerDeviceMode(final Context appContext,
                                    final PublicClientApplication.BrokerDeviceModeCallback callback) {

        final String methodName = ":getBrokerAccountMode";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
        );

        final Handler handler = new Handler(Looper.getMainLooper());

        if (!MSALControllerFactory.brokerInstalled(appContext)) {
            final String errorMessage = "Broker app is not installed on the device. Shared device mode requires the broker.";
            com.microsoft.identity.common.internal.logging.Logger.verbose(TAG + methodName, errorMessage, null);
            callback.onGetMode(false);
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorDescription(errorMessage)
            );
            return;
        }

        sBackgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                IMicrosoftAuthService service;
                final MicrosoftAuthClient client = new MicrosoftAuthClient(appContext);
                try {
                    final MicrosoftAuthServiceFuture authServiceFuture = client.connect();

                    service = authServiceFuture.get();

                    final boolean mode =
                            MsalBrokerResultAdapter
                                    .deviceModeFromBundle(
                                            service.getDeviceMode()
                                    );

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onGetMode(mode);
                        }
                    });
                } catch (final ClientException | InterruptedException | ExecutionException | RemoteException e) {
                    final String errorMessage = "Exception is thrown when trying to get current mode from Broker";
                    com.microsoft.identity.common.internal.logging.Logger.error(
                            TAG + methodName,
                            errorMessage,
                            e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(new MsalClientException(MsalClientException.IO_ERROR, errorMessage, e));
                        }
                    });
                } finally {
                    client.disconnect();
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private AcquireTokenResult acquireTokenSilentWithAccountManager(final AcquireTokenSilentOperationParameters parameters)
            throws BaseException {
        // if there is not any user added to account, it returns empty
        final String methodName = ":acquireTokenSilentWithAccountManager";
        Bundle bundleResult = null;
        if (parameters.getAccount() != null) {
            // blocking call to get token from cache or refresh request in
            // background at Authenticator
            try {

                final Bundle requestBundle = getSilentBrokerRequestBundle(parameters);

                // It does not expect activity to be launched.
                // AuthenticatorService is handling the request at
                // AccountManager.
                final AccountManager accountManager = AccountManager.get(parameters.getAppContext());
                final AccountManagerFuture<Bundle> result =
                        accountManager.getAuthToken(
                                getTargetAccount(parameters.getAppContext(), parameters.getAccount()),
                                AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                                requestBundle,
                                false,
                                null, //set to null to avoid callback
                                getPreferredHandler()
                        );

                // Making blocking request here
                Logger.verbose(TAG + methodName, "Received result from broker");
                bundleResult = result.getResult();
            } catch (final OperationCanceledException e) {
                Logger.error(
                        TAG + methodName,
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        "Exception thrown when talking to account manager. The broker request cancelled.",
                        e
                );

                throw new ClientException(
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        "OperationCanceledException thrown when talking to account manager. The broker request cancelled.",
                        e
                );
            } catch (final AuthenticatorException e) {
                Logger.error(
                        TAG + methodName,
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        "AuthenticatorException thrown when talking to account manager. The broker request cancelled.",
                        e
                );

                throw new ClientException(
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        "AuthenticatorException thrown when talking to account manager. The broker request cancelled.",
                        e
                );
            } catch (final IOException e) {
                // Authenticator gets problem from webrequest or file read/write
                Logger.error(
                        TAG + methodName,
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        "IOException thrown when talking to account manager. The broker request cancelled.",
                        e
                );

                throw new ClientException(
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        "IOException thrown when talking to account manager. The broker request cancelled.",
                        e
                );
            }
        }

        return getAcquireTokenResult(bundleResult);
    }

    private Bundle getSilentBrokerRequestBundle(final AcquireTokenSilentOperationParameters parameters) {
        final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();

        final Bundle requestBundle = new Bundle();
        final BrokerRequest brokerRequest = msalBrokerRequestAdapter.
                brokerRequestFromSilentOperationParameters(parameters);

        requestBundle.putString(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                new Gson().toJson(brokerRequest, BrokerRequest.class)
        );

        requestBundle.putInt(
                AuthenticationConstants.Broker.CALLER_INFO_UID,
                Binder.getCallingUid()
        );

        return requestBundle;
    }

    @SuppressLint("MissingPermission")
    private Account getTargetAccount(final Context context, final IAccountRecord accountRecord) {
        final String methodName = ":getTargetAccount";
        Account targetAccount = null;
        final Account[] accountList = AccountManager.get(context).getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        if (accountList != null) {
            for (Account account : accountList) {
                if (account != null && account.name != null && account.name.equalsIgnoreCase(accountRecord.getUsername())) {
                    targetAccount = account;
                }
            }
        }

        return targetAccount;
    }

    private AcquireTokenResult getAcquireTokenResult(@NonNull final Bundle resultBundle) throws BaseException {

        final MsalBrokerResultAdapter resultAdapter = new MsalBrokerResultAdapter();

        if (resultBundle.getBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS)) {
            Logger.verbose(TAG, "Successful result from the broker ");

            final AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
            acquireTokenResult.setLocalAuthenticationResult(
                    resultAdapter.authenticationResultFromBundle(resultBundle)
            );

            return acquireTokenResult;
        }

        Logger.warn(TAG, "Exception returned from broker, retrieving exception details ");

        throw resultAdapter.baseExceptionFromBundle(resultBundle);
    }

    /**
     * Checks if the account returns is a MSA Account and sets single on state in cache
     *
     * @param resultBundle
     * @param msalOAuth2TokenCache
     */
    private void saveMsaAccountToCache(@NonNull final Bundle resultBundle,
                                       @NonNull final MsalOAuth2TokenCache msalOAuth2TokenCache) throws ClientException {
        final String methodName = ":saveMsaAccountToCache";

        final BrokerResult brokerResult =
                new GsonBuilder()
                        .registerTypeAdapter(
                                ICacheRecord.class,
                                new ICacheRecordGsonAdapter()
                        )
                        .create()
                        .fromJson(
                                resultBundle.getString(AuthenticationConstants.Broker.BROKER_RESULT_V2),
                                BrokerResult.class
                        );

        if (resultBundle.getBoolean(AuthenticationConstants.Broker.BROKER_REQUEST_V2_SUCCESS)
                && brokerResult != null &&
                AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID.equalsIgnoreCase(brokerResult.getTenantId())) {
            Logger.info(TAG + methodName, "Result returned for MSA Account, saving to cache");

            try {
                final ClientInfo clientInfo = new ClientInfo(brokerResult.getClientInfo());
                final MicrosoftStsAccount microsoftStsAccount = new MicrosoftStsAccount(
                        new IDToken(brokerResult.getIdToken()),
                        clientInfo
                );
                microsoftStsAccount.setEnvironment(brokerResult.getEnvironment());

                final MicrosoftRefreshToken microsoftRefreshToken = new MicrosoftRefreshToken(
                        brokerResult.getRefreshToken(),
                        clientInfo,
                        brokerResult.getScope(),
                        brokerResult.getClientId(),
                        brokerResult.getEnvironment(),
                        brokerResult.getFamilyId()
                );

                msalOAuth2TokenCache.setSingleSignOnState(microsoftStsAccount, microsoftRefreshToken);
            } catch (ServiceException e) {
                Logger.errorPII(TAG + methodName, "Exception while creating Idtoken or ClientInfo," +
                        " cannot save MSA account tokens", e
                );
                throw new ClientException(ErrorStrings.INVALID_JWT, e.getMessage(), e);
            }
        }

    }

    /**
     * A broker task to be performed. Use in conjunction with performBrokerTask()
     */
    public interface BrokerTask<T> {

        /**
         * Performs a task in this function with the given IMicrosoftAuthService.
         */
        T perform(IMicrosoftAuthService service) throws BaseException, RemoteException;

        /**
         * Name of the task (for logging purposes).
         */
        String getOperationName();
    }

    /**
     * Perform an operation with Broker's MicrosoftAuthService on a background thread.
     *
     * @param appContext    app context.
     * @param callback      a callback function to be invoked to return result/error of the performed task.
     * @param brokerTask    the task to be performed.
     */
    private <T> void performBrokerTask(@NonNull final Context appContext,
                                       @NonNull final TaskCompletedCallbackWithError<T, MsalException> callback,
                                       @NonNull final BrokerTask<T> brokerTask) {

        final Handler handler = new Handler(Looper.getMainLooper());

        sBackgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                IMicrosoftAuthService service;
                final MicrosoftAuthClient client = new MicrosoftAuthClient(appContext);
                try {
                    final MicrosoftAuthServiceFuture authServiceFuture = client.connect();
                    service = authServiceFuture.get();
                    final T result = brokerTask.perform(service);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTaskCompleted(result);
                        }
                    });
                } catch (final BaseException | InterruptedException | ExecutionException | RemoteException e) {
                    com.microsoft.identity.common.internal.logging.Logger.error(
                            TAG + brokerTask.getOperationName(),
                            "Exception is thrown when trying to perform a broker operation:"
                                    + e.getMessage(),
                            e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            BaseException baseException = ExceptionAdapter.baseExceptionFromException(e);
                            callback.onError(MsalExceptionAdapter.msalExceptionFromBaseException(baseException));
                        }
                    });
                } finally {
                    client.disconnect();
                }
            }
        });
    }

    /**
     * Get the currently signed-in account, if there's any.
     * This only works when getBrokerAccountMode() is BROKER_ACCOUNT_MODE_SINGLE_ACCOUNT.
     */
    public void getCurrentAccount(@NonNull final PublicClientApplicationConfiguration configuration,
                                  @NonNull final TaskCompletedCallbackWithError<List<ICacheRecord>, MsalException> callback) {
        final String methodName = ":getCurrentAccount";

        performBrokerTask(
                configuration.getAppContext(),
                callback,
                new BrokerTask<List<ICacheRecord>>() {
                    @Override
                    public List<ICacheRecord> perform(IMicrosoftAuthService service) throws ClientException, RemoteException {
                        return MsalBrokerResultAdapter
                                .accountsFromBundle(
                                        service.getCurrentAccount(getRequestBundleForGetAccounts(OperationParametersAdapter.createOperationParameters(configuration)))
                                );
                    }

                    @Override
                    public String getOperationName() {
                        return methodName;
                    }
                });
    }

    /**
     * Returns list of accounts that has previously been used to acquire token with broker through the calling app.
     * This only works when getBrokerAccountMode() is BROKER_ACCOUNT_MODE_MULTIPLE_ACCOUNT.
     * <p>
     * This method might be called on an UI thread, since we connect to broker,
     * this needs to be called on background thread.
     */
    @Override
    public List<ICacheRecord> getAccounts(@NonNull final OperationParameters parameters)
            throws ClientException, InterruptedException, ExecutionException, RemoteException, OperationCanceledException, IOException, AuthenticatorException {
        final String methodName = ":getBrokerAccounts";
        if (isMicrosoftAuthServiceSupported(parameters.getAppContext())) {
            Logger.verbose(TAG + methodName, "Is microsoft auth service supported? " + "[yes]");
            Logger.verbose(TAG + methodName, "Get the broker accounts from auth service.");
            return getBrokerAccountsWithAuthService(parameters);
        } else {
            Logger.verbose(TAG + methodName, "Is microsoft auth service supported? " + "[no]");
            Logger.verbose(TAG + methodName, "Get the broker accounts from Account Manager.");
            return getBrokerAccountsFromAccountManager(parameters);
        }
    }

    @WorkerThread
    private List<ICacheRecord> getBrokerAccountsWithAuthService(@NonNull final OperationParameters parameters)
            throws ClientException, InterruptedException, ExecutionException, RemoteException {
        final String methodName = ":getBrokerAccountsWithAuthService";
        IMicrosoftAuthService service;
        final MicrosoftAuthClient client = new MicrosoftAuthClient(parameters.getAppContext());
        try {
            final MicrosoftAuthServiceFuture authServiceFuture = client.connect();
            service = authServiceFuture.get();
            final Bundle requestBundle = getRequestBundleForGetAccounts(parameters);

            final List<ICacheRecord> cacheRecords =
                    MsalBrokerResultAdapter
                            .accountsFromBundle(
                                    service.getAccounts(requestBundle)
                            );

            return cacheRecords;
        } catch (final ClientException | InterruptedException | ExecutionException | RemoteException e) {
            com.microsoft.identity.common.internal.logging.Logger.error(
                    TAG + methodName,
                    "Exception is thrown when trying to get account from Broker, returning empty list."
                            + e.getMessage(),
                    ErrorStrings.IO_ERROR,
                    e);
            throw e;
        } finally {
            client.disconnect();
        }
    }

    @WorkerThread
    @SuppressLint("MissingPermission")
    private List<ICacheRecord> getBrokerAccountsFromAccountManager(@NonNull final OperationParameters parameters)
            throws OperationCanceledException, IOException, AuthenticatorException, ClientException {
        final String methodName = ":getBrokerAccountsFromAccountManager";
        final Account[] accountList = AccountManager.get(parameters.getAppContext()).getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        final List<ICacheRecord> cacheRecords = new ArrayList<>();
        Logger.verbose(
                TAG + methodName,
                "Retrieve all the accounts from account manager with broker account type, "
                        + "and the account length is: " + accountList.length
        );

        if (accountList == null || accountList.length == 0) {
            return cacheRecords;
        } else {
            final Bundle bundle = new Bundle();
            bundle.putBoolean(DATA_CACHE_RECORD, true);
            bundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());

            for (final Account eachAccount : accountList) {
                // Use AccountManager Api method to get extended user info

                final AccountManagerFuture<Bundle> result = AccountManager.get(parameters.getAppContext())
                        .updateCredentials(
                                eachAccount,
                                AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                                bundle,
                                null,
                                null,
                                null
                        );

                final Bundle userInfoBundle = result.getResult();
                cacheRecords.addAll(
                        MsalBrokerResultAdapter.accountsFromBundle(userInfoBundle)
                );
            }

            return cacheRecords;
        }
    }

    private Bundle getRequestBundleForGetAccounts(@NonNull final OperationParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
        requestBundle.putString(ACCOUNT_REDIRECT, parameters.getRedirectUri());
        //Disable the environment and tenantID. Just return all accounts belong to this clientID.
        return requestBundle;
    }

    @Override
    @WorkerThread
    public boolean removeAccount(@NonNull final OperationParameters parameters)
            throws BaseException, InterruptedException, ExecutionException, RemoteException {
        final String methodName = ":removeBrokerAccount";
        if (isMicrosoftAuthServiceSupported(parameters.getAppContext())) {
            Logger.verbose(TAG + methodName, "Is microsoft auth service supported? " + "[yes]");
            Logger.verbose(TAG + methodName, "Remove the account(s) from auth service.");
            return removeBrokerAccountWithAuthService(parameters);
        } else {
            Logger.verbose(TAG + methodName, "Is microsoft auth service supported? " + "[no]");
            Logger.verbose(TAG + methodName, "Remove the account(s) from Account Manager.");
            return removeBrokerAccountFromAccountManager(parameters);
        }
    }

    @SuppressLint("MissingPermission")
    private boolean removeBrokerAccountFromAccountManager(@NonNull final OperationParameters parameters) {
        final String methodName = ":removeBrokerAccountFromAccountManager";
        // getAuthToken call will execute in async as well
        Logger.verbose(TAG + methodName, "Try to remove account from account manager.");

        //If account is null, remove all accounts from broker
        //Otherwise, get the target account and remove it from broker
        Account[] accountList = AccountManager.get(parameters.getAppContext()).getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        if (accountList != null && accountList.length > 0) {
            for (final Account eachAccount : accountList) {
                if (parameters.getAccount() == null || eachAccount.name.equalsIgnoreCase(parameters.getAccount().getUsername())) {
                    //create remove request bundle
                    Bundle brokerOptions = new Bundle();
                    brokerOptions.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
                    brokerOptions.putString(ENVIRONMENT, parameters.getAccount().getEnvironment());
                    brokerOptions.putString(ACCOUNT_HOME_ACCOUNT_ID, parameters.getAccount().getHomeAccountId());
                    brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS,
                            AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS_VALUE);
                    AccountManager.get(parameters.getAppContext()).getAuthToken(
                            eachAccount,
                            AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                            brokerOptions,
                            false,
                            null, //set to null to avoid callback
                            getPreferredHandler()
                    );
                }
            }
        }

        return true;
    }

    private boolean removeBrokerAccountWithAuthService(@NonNull final OperationParameters parameters)
            throws BaseException, InterruptedException, ExecutionException, RemoteException {
        final String methodName = ":removeBrokerAccountWithAuthService";

        IMicrosoftAuthService service;
        final MicrosoftAuthClient client = new MicrosoftAuthClient(parameters.getAppContext());

        try {
            final MicrosoftAuthServiceFuture authServiceFuture = client.connect();

            service = authServiceFuture.get();

            Bundle requestBundle = getRequestBundleForRemoveAccount(parameters);
            service.removeAccount(requestBundle);
            return true;
        } catch (final BaseException | InterruptedException | ExecutionException | RemoteException e) {
            com.microsoft.identity.common.internal.logging.Logger.error(
                    TAG + methodName,
                    "Exception is thrown when trying to get target account."
                            + e.getMessage(),
                    ErrorStrings.IO_ERROR,
                    e);
            throw e;
        } finally {
            client.disconnect();
        }
    }

    private Bundle getRequestBundleForRemoveAccount(@NonNull final OperationParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
        if (null != parameters.getAccount()) {
            requestBundle.putString(ENVIRONMENT, parameters.getAccount().getEnvironment());
            requestBundle.putString(ACCOUNT_HOME_ACCOUNT_ID, parameters.getAccount().getHomeAccountId());
        }

        return requestBundle;
    }

    /**
     * Given an account, perform a global sign-out from this shared device (End my shift capability).
     * This will invoke Broker and
     * 1. Remove account from token cache.
     * 2. Remove account from AccountManager.
     * 3. Clear WebView cookies.
     * 4. Sign out from default browser.
     */
    public void removeAccountFromSharedDevice(@NonNull final PublicClientApplicationConfiguration configuration,
                                              @NonNull final TaskCompletedCallbackWithError<Void, MsalException> callback) {
        final String methodName = ":removeAccountFromSharedDevice";

        performBrokerTask(
                configuration.getAppContext(),
                callback,
                new BrokerTask<Void>() {
                    @Override
                    public Void perform(IMicrosoftAuthService service) throws BaseException, RemoteException {
                        final Bundle resultBundle = service.removeAccountFromSharedDevice(
                                getRequestBundleForRemoveAccountFromSharedDevice(configuration)
                        );

                        if (resultBundle == null) {
                            return null;
                        } else {
                            final BrokerResult brokerResult = MsalBrokerResultAdapter.brokerResultFromBundle(resultBundle);
                            com.microsoft.identity.common.internal.logging.Logger.error(
                                    TAG,
                                    "Failed to perform global sign-out."
                                            + brokerResult.getErrorMessage(),
                                    null);

                            throw new MsalClientException(
                                    MsalClientException.UNKNOWN_ERROR,
                                    brokerResult.getErrorMessage());
                        }
                    }

                    @Override
                    public String getOperationName() {
                        return methodName;
                    }
                });
    }

    private Bundle getRequestBundleForRemoveAccountFromSharedDevice(PublicClientApplicationConfiguration configuration) {
        final Bundle requestBundle = new Bundle();

        try {
            Browser browser = BrowserSelector.select(configuration.getAppContext(), configuration.getBrowserSafeList());
            requestBundle.putString(DEFAULT_BROWSER_PACKAGE_NAME, browser.getPackageName());
        } catch (ClientException e) {
            // Best effort. If none is passed to broker, then it will let the OS decide.
            Logger.error(TAG, e.getErrorCode(), e);
        }

        return requestBundle;
    }


    static boolean isMicrosoftAuthServiceSupported(@NonNull final Context context) {
        final MicrosoftAuthClient client = new MicrosoftAuthClient(context);
        final Intent microsoftAuthServiceIntent = client.getIntentForAuthService(context);
        return null != microsoftAuthServiceIntent;
    }

    /**
     * To verify if App gives permissions to AccountManager to use broker.
     * <p>
     * Beginning in Android 6.0 (API level 23), the run-time permission GET_ACCOUNTS is required
     * which need to be requested in the runtime by the calling app.
     * <p>
     * Before Android 6.0, the GET_ACCOUNTS, MANAGE_ACCOUNTS and USE_CREDENTIALS permission is
     * required in the app's manifest xml file.
     *
     * @return true if all required permissions are granted, otherwise return false.
     */
    static boolean isAccountManagerPermissionsGranted(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return isPermissionGranted(context, MANIFEST_PERMISSION_GET_ACCOUNTS);
        } else {
            return isPermissionGranted(context, MANIFEST_PERMISSION_GET_ACCOUNTS)
                    && isPermissionGranted(context, MANIFEST_PERMISSION_MANAGE_ACCOUNTS)
                    && isPermissionGranted(context, MANIFEST_PERMISSION_USE_CREDENTIALS);
        }
    }

    private static boolean isPermissionGranted(@NonNull final Context context,
                                               @NonNull final String permissionName) {
        final String methodName = ":isPermissionGranted";
        final PackageManager pm = context.getPackageManager();
        final boolean isGranted = pm.checkPermission(permissionName, context.getPackageName())
                == PackageManager.PERMISSION_GRANTED;
        Logger.verbose(TAG + methodName, "is " + permissionName + " granted? [" + isGranted + "]");
        return isGranted;
    }
}
