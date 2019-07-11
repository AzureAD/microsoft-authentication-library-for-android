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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.google.gson.GsonBuilder;
import com.microsoft.identity.client.IMicrosoftAuthService;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.AsyncResult;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.broker.BrokerResultFuture;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthServiceFuture;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.controllers.ExceptionAdapter;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
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
import com.microsoft.identity.common.internal.result.ResultFuture;
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

import static com.microsoft.identity.client.internal.controllers.BrokerBaseStrategy.getAcquireTokenResult;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.DEFAULT_BROWSER_PACKAGE_NAME;

/**
 * The implementation of MSAL Controller for Broker
 */
public class BrokerMsalController extends BaseController {

    private static final String TAG = BrokerMsalController.class.getSimpleName();

    private List<BrokerBaseStrategy> mStrategies = new ArrayList<>();

    private static final String MANIFEST_PERMISSION_GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
    private static final String MANIFEST_PERMISSION_MANAGE_ACCOUNTS = "android.permission.MANAGE_ACCOUNTS";
    private static final String MANIFEST_PERMISSION_USE_CREDENTIALS = "android.permission.USE_CREDENTIALS";

    private BrokerResultFuture mBrokerResultFuture;

    List<BrokerBaseStrategy> getStrategies() {
        return mStrategies;
    }

    void addBrokerStrategy(@NonNull final BrokerBaseStrategy strategy) {
        mStrategies.add(strategy);
    }

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
                        .putApiId(TelemetryEventStrings.Api.BROKER_GET_ACCOUNTS)
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
                        .putApiId(TelemetryEventStrings.Api.BROKER_GET_ACCOUNTS)
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
        initializeBrokerMsalController(parameters);

        Intent interactiveRequestIntent = null;

        for (int ii = 0; ii < getStrategies().size(); ii++) {
            final BrokerBaseStrategy strategy = getStrategies().get(ii);
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Executing with strategy: "
                            + strategy.getClass().getSimpleName()
            );

            try {
                interactiveRequestIntent = strategy.getBrokerAuthorizationIntent(parameters);
                if (interactiveRequestIntent != null) {
                    break;
                }
            } catch (final Exception exception) {
                if (ii == (getStrategies().size() - 1)) {
                    //throw the exception for the last trying of strategies.
                    throw exception;
                }
            }
        }

        return interactiveRequestIntent;
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
                        .putApiId(TelemetryEventStrings.Api.BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
                        .put(TelemetryEventStrings.Key.RESULT_CODE, String.valueOf(resultCode))
                        .put(TelemetryEventStrings.Key.REQUEST_CODE, String.valueOf(requestCode))
        );

        mBrokerResultFuture.setResultBundle(data.getExtras());

        Telemetry.emit(
                new ApiEndEvent()
                        .putApiId(TelemetryEventStrings.Api.BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE)
        );
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters parameters) throws BaseException {
        final String methodName = ":acquireTokenSilent";
        initializeBrokerMsalController(parameters);

        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_SILENT)
        );

        AcquireTokenResult acquireTokenResult = null;

        for (int ii = 0; ii < getStrategies().size(); ii++) {
            final BrokerBaseStrategy strategy = getStrategies().get(ii);
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Executing with strategy: "
                            + strategy.getClass().getSimpleName()
            );

            try {
                acquireTokenResult = strategy.acquireTokenSilent(parameters);
                if (acquireTokenResult != null) {
                    break;
                }
            } catch (final Exception exception) {
                if (ii == (getStrategies().size() - 1)) {
                    //throw the exception for the last trying of strategies.
                    throw exception;
                }
            }
        }

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(acquireTokenResult)
                        .putApiId(TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_SILENT)
        );

        return acquireTokenResult;
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
        initializeBrokerMsalController(parameters);
        List<ICacheRecord> result = null;

        for (int ii = 0; ii < getStrategies().size(); ii++) {
            final BrokerBaseStrategy strategy = getStrategies().get(ii);
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Executing with strategy: "
                            + strategy.getClass().getSimpleName()
            );

            try {
                result = strategy.getBrokerAccounts(parameters);
                if (!result.isEmpty()) {
                    break;
                }
            } catch (final Exception exception) {
                if (ii == (getStrategies().size() - 1)) {
                    //throw the exception for the last trying of strategies.
                    throw exception;
                }
            }
        }

        return result;
    }


    @Override
    @WorkerThread
    public boolean removeAccount(@NonNull final OperationParameters parameters)
            throws BaseException, InterruptedException, ExecutionException, RemoteException {
        final String methodName = ":removeBrokerAccount";
        initializeBrokerMsalController(parameters);
        boolean result = false;


        for (int ii = 0; ii < getStrategies().size(); ii++) {
            final BrokerBaseStrategy strategy = getStrategies().get(ii);
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Executing with strategy: "
                            + strategy.getClass().getSimpleName()
            );

            try {
                result = strategy.removeBrokerAccount(parameters);
            } catch (final Exception exception) {
                if (ii == (getStrategies().size() - 1)) {
                    //throw the exception for the last trying of strategies.
                    throw exception;
                }
            }
        }

        return result;
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
     * @param appContext app context.
     * @param callback   a callback function to be invoked to return result/error of the performed task.
     * @param brokerTask the task to be performed.
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
                                        service.getCurrentAccount(BrokerAuthServiceStrategy.getRequestBundleForRemoveAccount(OperationParametersAdapter.createOperationParameters(configuration)))
                                );
                    }

                    @Override
                    public String getOperationName() {
                        return methodName;
                    }
                });
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

    @WorkerThread
    static boolean helloWithMicrosoftAuthService(@NonNull final Context applicationContext, @NonNull final OperationParameters parameters) throws ClientException {
        final String methodName = ":helloWithMicrosoftAuthService";
        IMicrosoftAuthService service;
        final MicrosoftAuthClient client = new MicrosoftAuthClient(applicationContext);

        try {
            final MicrosoftAuthServiceFuture authServiceFuture = client.connect();
            service = authServiceFuture.get();
            final Bundle requestBundle = MsalBrokerRequestAdapter.getBrokerHelloBundle(parameters);
            final BrokerResult result = MsalBrokerResultAdapter.brokerResultFromBundle(service.hello(requestBundle));
            if (result == null) {
                return false;
            } else if (result.isSuccess()) {
                return true;
            } else {
                throw new ClientException(result.getErrorCode(), result.getErrorMessage());
            }
        } catch (final InterruptedException | ExecutionException | RemoteException e) {
            com.microsoft.identity.common.internal.logging.Logger.error(
                    TAG + methodName,
                    "Exception is thrown when trying to verify the broker protocol version."
                            + e.getMessage(),
                    ErrorStrings.IO_ERROR,
                    e);
            throw new ClientException(
                    ErrorStrings.IO_ERROR,
                    e.getMessage(),
                    e
            );
        } finally {
            client.disconnect();
        }
    }

    @WorkerThread
    @SuppressLint("MissingPermission")
    static boolean helloWithAccountManager(@NonNull final Context applicationContext, @NonNull final OperationParameters parameters)
            throws ClientException {
        final String methodName = ":helloWithAccountManager";
        final String DATA_HELLO = "com.microsoft.workaccount.hello";
        try {
            Account[] accountList = AccountManager.get(applicationContext).getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
            //get result bundle
            if (accountList.length > 0) {
                final Bundle requestBundle = MsalBrokerRequestAdapter.getBrokerHelloBundle(parameters);
                requestBundle.putString(DATA_HELLO, "true");
                final AccountManagerFuture<Bundle> result = AccountManager.get(parameters.getAppContext())
                        .updateCredentials(
                                accountList[0],
                                AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                                requestBundle,
                                null,
                                null,
                                null
                        );

                final BrokerResult brokerResult = MsalBrokerResultAdapter.brokerResultFromBundle(result.getResult());
                if (result == null) {
                    return false;
                } else if (brokerResult.isSuccess()) {
                    return true;
                } else {
                    throw new ClientException(brokerResult.getErrorCode(), brokerResult.getErrorMessage());
                }
            } else {
                return false;
            }
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

    private void initializeBrokerMsalController(@NonNull final OperationParameters parameters)
            throws ClientException {
        final String methodName = ":initializeBrokerMsalController";
        if (!getStrategies().isEmpty()) {
            mStrategies = new ArrayList<>();
        }

        //check if bound service available
//        if (BrokerMsalController.helloWithMicrosoftAuthService(parameters.getAppContext(), parameters)) {
//            Logger.verbose(TAG + methodName, "Add the broker AuthService strategy.");
//            this.addBrokerStrategy(new BrokerAuthServiceStrategy());
//        }

        //check if account manager available
        if (BrokerMsalController.helloWithAccountManager(parameters.getAppContext(), parameters)) {
            Logger.verbose(TAG + methodName, "Add the account manager strategy.");
            this.addBrokerStrategy(new BrokerAccountManagerStrategy());
        }

        if (getStrategies().isEmpty()) {
            throw new ClientException(
                    ErrorStrings.BROKER_PROTOCOL_VERSION_INCOMPATIBLE,
                    "The protocol versions between the MSAL client app and broker do not compatible. "
            );
        }
    }
}