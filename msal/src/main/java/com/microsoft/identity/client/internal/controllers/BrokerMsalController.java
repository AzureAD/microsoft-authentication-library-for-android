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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.gson.GsonBuilder;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryAudience;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.broker.BrokerResultFuture;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.cache.MsalOAuth2TokenCache;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory.ClientInfo;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAccount;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.ApiStartEvent;
import com.microsoft.identity.common.internal.util.ICacheRecordGsonAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * The implementation of MSAL Controller for Broker
 */
public class BrokerMsalController extends BaseController {

    private static final String TAG = BrokerMsalController.class.getSimpleName();

    private static final String MANIFEST_PERMISSION_MANAGE_ACCOUNTS = "android.permission.MANAGE_ACCOUNTS";

    private BrokerResultFuture mBrokerResultFuture;

    @Override
    public AcquireTokenResult acquireToken(AcquireTokenOperationParameters parameters) throws Exception {
        Telemetry.emit(
                new ApiStartEvent()
                        .putProperties(parameters)
                        .putApiId(TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_INTERACTIVE)
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
        // TODO add fragment support to AccountChooserActivity.
        parameters.getActivity().startActivity(brokerActivityIntent);

        //Wait to be notified of the result being returned... we could add a timeout here if we want to
        final Bundle resultBundle = mBrokerResultFuture.get();

        // For MSA Accounts Broker doesn't save the accounts, instead it just passes the result along,
        // MSAL needs to save this account locally for future token calls.
        saveMsaAccountToCache(resultBundle, (MsalOAuth2TokenCache) parameters.getTokenCache());

        final AcquireTokenResult result;
        try {
            result = new MsalBrokerResultAdapter().getAcquireTokenResultFromResultBundle(resultBundle);
        } catch (BaseException e) {
            Telemetry.emit(
                    new ApiEndEvent()
                            .putException(e)
                            .putApiId(TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_INTERACTIVE)
            );
            throw e;
        }

        Telemetry.emit(
                new ApiEndEvent()
                        .putResult(result)
                        .putApiId(TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_INTERACTIVE)
        );

        return result;
    }

    /**
     * Info of a broker operation to be performed with available strategies.
     */
    private interface BrokerOperationInfo<T extends OperationParameters, U> {
        /**
         * Performs this broker operation in this method with the given IMicrosoftAuthService.
         */
        @Nullable
        U perform(BrokerBaseStrategy strategy, T parameters) throws Exception;

        /**
         * Name of the task (for logging purposes).
         */
        String getMethodName();

        /**
         * Name of the telemetry API event associated to this strategy task.
         * If this value returns null, no telemetry event will be emitted.
         */
        @Nullable
        String getTelemetryApiName();

        /**
         * A method that will be invoked before the success event is emitted.
         * If the calling operation wants to put any value in the success event, put it here.
         */
        void putValueInSuccessEvent(ApiEndEvent event, U result);
    }

    /**
     * A generic method that would initialize and iterate through available strategies.
     * It will return a result immediately if any of the strategy succeeds, or throw an exception if all of the strategies fails.
     */
    private <T extends OperationParameters, U> U invokeBrokerOperation(@NonNull final T parameters,
                                                                       @NonNull final BrokerOperationInfo<T, U> strategyTask)
            throws Exception {

        if (strategyTask.getTelemetryApiName() != null) {
            Telemetry.emit(
                    new ApiStartEvent()
                            .putProperties(parameters)
                            .putApiId(strategyTask.getTelemetryApiName())
            );
        }

        final List<BrokerBaseStrategy> strategies = getStrategies();

        U result = null;
        for (int ii = 0; ii < strategies.size(); ii++) {
            final BrokerBaseStrategy strategy = strategies.get(ii);
            try {
                com.microsoft.identity.common.internal.logging.Logger.info(
                        TAG + strategyTask.getMethodName(),
                        "Executing with strategy: "
                                + strategy.getClass().getSimpleName()
                );

                strategy.hello(parameters);
                result = strategyTask.perform(strategy, parameters);
                if (result != null) {
                    break;
                }
            } catch (final BrokerCommunicationException exception){
                // continue
            } catch (final Exception exception){
                if (strategyTask.getTelemetryApiName() != null) {
                    Telemetry.emit(
                            new ApiEndEvent()
                                    .putException(exception)
                                    .putApiId(strategyTask.getTelemetryApiName())
                    );
                }
                throw exception;
            }
        }

        // This means that we've tried every strategies...
        if (result == null) {
            final BrokerCommunicationException exception = new BrokerCommunicationException("MSAL failed to communicate to Broker.");
            if (strategyTask.getTelemetryApiName() != null) {
                Telemetry.emit(
                        new ApiEndEvent()
                                .putException(exception)
                                .putApiId(strategyTask.getTelemetryApiName())
                );
            }
            throw exception;
        }

        if (strategyTask.getTelemetryApiName() != null) {
            final ApiEndEvent successEvent = new ApiEndEvent()
                    .putApiId(strategyTask.getTelemetryApiName())
                    .isApiCallSuccessful(Boolean.TRUE);
            strategyTask.putValueInSuccessEvent(successEvent, result);
            Telemetry.emit(successEvent);
        }

        return result;
    }


    // The order matters.
    private List<BrokerBaseStrategy> getStrategies() {
        final List<BrokerBaseStrategy> strategies = new ArrayList<>();
        strategies.add(new BrokerAuthServiceStrategy());
        strategies.add(new BrokerAccountManagerStrategy());
        return strategies;
    }

    /**
     * Get the intent for the broker interactive request
     *
     * @param parameters
     * @return
     */
    private Intent getBrokerAuthorizationIntent(@NonNull final AcquireTokenOperationParameters parameters) throws Exception {
        return invokeBrokerOperation(parameters,
                new BrokerOperationInfo<AcquireTokenOperationParameters, Intent>() {
                    @Nullable
                    @Override
                    public Intent perform(BrokerBaseStrategy strategy, AcquireTokenOperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException {
                        return strategy.getBrokerAuthorizationIntent(parameters);
                    }

                    @Override
                    public String getMethodName() {
                        return ":getBrokerAuthorizationIntent";
                    }

                    @Nullable
                    @Override
                    public String getTelemetryApiName() {
                        return null;
                    }

                    @Override
                    public void putValueInSuccessEvent(ApiEndEvent event, Intent result) {
                    }
                });
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
    public AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters parameters) throws Exception {
        return invokeBrokerOperation(parameters,
                new BrokerOperationInfo<AcquireTokenSilentOperationParameters, AcquireTokenResult>() {
                    @Nullable
                    @Override
                    public AcquireTokenResult perform(BrokerBaseStrategy strategy, AcquireTokenSilentOperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException {
                        return strategy.acquireTokenSilent(parameters);
                    }

                    @Override
                    public String getMethodName() {
                        return ":acquireTokenSilent";
                    }

                    @Nullable
                    @Override
                    public String getTelemetryApiName() {
                        return TelemetryEventStrings.Api.BROKER_ACQUIRE_TOKEN_SILENT;
                    }

                    @Override
                    public void putValueInSuccessEvent(ApiEndEvent event, AcquireTokenResult result) {
                        event.putResult(result);
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
    public List<ICacheRecord> getAccounts(@NonNull final OperationParameters parameters) throws Exception {
        return invokeBrokerOperation(parameters,
                new BrokerOperationInfo<OperationParameters, List<ICacheRecord>>() {
                    @Nullable
                    @Override
                    public List<ICacheRecord> perform(BrokerBaseStrategy strategy, OperationParameters parameters) throws RemoteException, InterruptedException, ExecutionException, AuthenticatorException, IOException, OperationCanceledException, BaseException {
                        return strategy.getBrokerAccounts(parameters);
                    }

                    @Override
                    public String getMethodName() {
                        return ":getBrokerAccounts";
                    }

                    @Nullable
                    @Override
                    public String getTelemetryApiName() {
                        return TelemetryEventStrings.Api.BROKER_GET_ACCOUNTS;
                    }

                    @Override
                    public void putValueInSuccessEvent(ApiEndEvent event, List<ICacheRecord> result) {
                        event.put(TelemetryEventStrings.Key.ACCOUNTS_NUMBER, Integer.toString(result.size()));
                    }
                });
    }

    @Override
    @WorkerThread
    public boolean removeAccount(@NonNull final OperationParameters parameters) throws Exception {
        return invokeBrokerOperation(parameters,
                new BrokerOperationInfo<OperationParameters, Boolean>() {
                    @Nullable
                    @Override
                    public Boolean perform(BrokerBaseStrategy strategy, OperationParameters parameters) throws InterruptedException, ExecutionException, BaseException, RemoteException {
                        strategy.removeBrokerAccount(parameters);
                        return true;
                    }

                    @Override
                    public String getMethodName() {
                        return ":removeAccount";
                    }

                    @Nullable
                    @Override
                    public String getTelemetryApiName() {
                        return TelemetryEventStrings.Api.BROKER_REMOVE_ACCOUNT;
                    }

                    @Override
                    public void putValueInSuccessEvent(ApiEndEvent event, Boolean result) {
                    }
                });
    }

    @Override
    @WorkerThread
    public boolean getDeviceMode(@NonNull final OperationParameters parameters) throws Exception {
        return invokeBrokerOperation(parameters,
                new BrokerOperationInfo<OperationParameters, Boolean>() {
                    @Nullable
                    @Override
                    public Boolean perform(BrokerBaseStrategy strategy, OperationParameters parameters) throws Exception {
                        return strategy.getDeviceMode(parameters);
                    }

                    @Override
                    public String getMethodName() {
                        return ":getDeviceMode";
                    }

                    @Nullable
                    @Override
                    public String getTelemetryApiName() {
                        return TelemetryEventStrings.Api.GET_BROKER_DEVICE_MODE;
                    }

                    @Override
                    public void putValueInSuccessEvent(ApiEndEvent event, Boolean result) {
                        event.put(TelemetryEventStrings.Key.IS_DEVICE_SHARED, Boolean.toString(result));
                    }
                });
    }

    @Override
    public List<ICacheRecord> getCurrentAccount(OperationParameters parameters) throws Exception {
        final String methodName = ":getCurrentAccount";

        if (!parameters.getIsSharedDevice()) {
            Logger.verbose(TAG + methodName, "Not a shared device, invoke getAccounts() instead of getCurrentAccount()");
            return getAccounts(parameters);
        }

        return invokeBrokerOperation(parameters,
                new BrokerOperationInfo<OperationParameters, List<ICacheRecord>>() {
                    @Nullable
                    @Override
                    public List<ICacheRecord> perform(BrokerBaseStrategy strategy, OperationParameters parameters) throws Exception {
                        return strategy.getCurrentAccountInSharedDevice(parameters);
                    }

                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @Nullable
                    @Override
                    public String getTelemetryApiName() {
                        return TelemetryEventStrings.Api.BROKER_GET_CURRENT_ACCOUNT;
                    }

                    @Override
                    public void putValueInSuccessEvent(ApiEndEvent event, List<ICacheRecord> result) {
                        event.put(TelemetryEventStrings.Key.ACCOUNTS_NUMBER, Integer.toString(result.size()));
                    }
                });
    }

    @Override
    public boolean removeCurrentAccount(OperationParameters parameters) throws Exception {
        final String methodName = ":removeCurrentAccount";

        if (!parameters.getIsSharedDevice()) {
            Logger.verbose(TAG + methodName, "Not a shared device, invoke removeAccount() instead of removeCurrentAccount()");
            return removeAccount(parameters);
        }

        /**
         * Given an account, perform a global sign-out from this shared device (End my shift capability).
         * This will invoke Broker and
         * 1. Remove account from token cache.
         * 2. Remove account from AccountManager.
         * 3. Clear WebView cookies.
         *
         * If everything succeeds on the broker side, it will then
         * 4. Sign out from default browser.
         */
        return invokeBrokerOperation(parameters,
                new BrokerOperationInfo<OperationParameters, Boolean>() {
                    @Nullable
                    @Override
                    public Boolean perform(BrokerBaseStrategy strategy, OperationParameters parameters) throws InterruptedException, ExecutionException, BaseException, RemoteException {
                        strategy.signOutFromSharedDevice(parameters);
                        return true;
                    }

                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @Nullable
                    @Override
                    public String getTelemetryApiName() {
                        return TelemetryEventStrings.Api.BROKER_REMOVE_ACCOUNT_FROM_SHARED_DEVICE;
                    }

                    @Override
                    public void putValueInSuccessEvent(ApiEndEvent event, Boolean result) {
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
     * Before Android 6.0, the MANAGE_ACCOUNTS permission is
     * required in the app's manifest xml file.
     *
     * @return true if all required permissions are granted, otherwise return false.
     */
    static boolean isAccountManagerPermissionsGranted(@NonNull final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return true;
        } else {
            return isPermissionGranted(context, MANIFEST_PERMISSION_MANAGE_ACCOUNTS);
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