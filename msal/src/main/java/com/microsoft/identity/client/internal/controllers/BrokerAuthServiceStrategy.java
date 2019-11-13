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
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.microsoft.identity.client.IMicrosoftAuthService;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthServiceFuture;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.GetDeviceModeCommandContext;
import com.microsoft.identity.common.internal.request.generated.GetDeviceModeCommandParameters;
import com.microsoft.identity.common.internal.request.generated.IAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.InteractiveTokenCommandContext;
import com.microsoft.identity.common.internal.request.generated.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.LoadAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.RemoveAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.RemoveCurrentAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.RemoveCurrentAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.SilentTokenCommandContext;
import com.microsoft.identity.common.internal.request.generated.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.BrokerEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.BrokerStartEvent;
import com.microsoft.identity.common.internal.ui.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.DEFAULT_BROWSER_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ENVIRONMENT;

public class BrokerAuthServiceStrategy extends BrokerBaseStrategy {
    private static final String TAG = BrokerAuthServiceStrategy.class.getSimpleName();

    /**
     * Get the intent for the broker interactive request
     *
     * @param parameters
     * @return
     */
    @WorkerThread
    Intent getBrokerAuthorizationIntent(
            @NonNull final InteractiveTokenCommandContext context,
            @NonNull final InteractiveTokenCommandParameters parameters)

            throws BaseException, InterruptedException, ExecutionException, RemoteException {
        final String methodName = ":getBrokerAuthorizationIntent";
        Logger.verbose(TAG + methodName, "Get the broker authorization intent from auth service.");
        Intent interactiveRequestIntent;
        interactiveRequestIntent = getBrokerAuthorizationIntentFromAuthService(context);
        final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();
        interactiveRequestIntent.putExtra(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                new Gson().toJson(
                        msalBrokerRequestAdapter.brokerRequestFromAcquireTokenParameters(context, parameters),
                        BrokerRequest.class)
        );
        interactiveRequestIntent.putExtra(AuthenticationConstants.Broker.ACCOUNT_NAME, parameters.loginHint());

        return interactiveRequestIntent;
    }

    /**
     * A broker task to be performed. Use in conjunction with performBrokerTask()
     */
    public interface AuthServiceOperation<T> {

        /**
         * Performs a task in this method with the given IMicrosoftAuthService.
         * If the operation doesn't return expected value, the implementer MUST thrown an exception.
         * Otherwise, this operation is considered succeeded.
         * <p>
         * {@link IMicrosoftAuthService}
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
     * @param appContext           app context.
     * @param authServiceOperation the task to be performed.
     */
    private <T> T performAuthServiceOperation(@NonNull final Context appContext,
                                              @NonNull final AuthServiceOperation<T> authServiceOperation)
            throws BaseException, InterruptedException, ExecutionException, RemoteException {

        final String methodName = authServiceOperation.getOperationName();

        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.BOUND_SERVICE)
        );

        final T result;
        final IMicrosoftAuthService service;
        final MicrosoftAuthClient client = new MicrosoftAuthClient(appContext);
        try {
            //Do we want a time out here?
            final MicrosoftAuthServiceFuture authServiceFuture = client.connect();
            service = authServiceFuture.get();
            result = authServiceOperation.perform(service);
        } catch (final Exception e) {
            final String errorDescription;
            if (e instanceof InterruptedException || e instanceof ExecutionException) {
                errorDescription = "Exception occurred while awaiting (get) return of MicrosoftAuthService";
            } else if (e instanceof RemoteException) {
                errorDescription = "RemoteException occurred while attempting to invoke remote service";
            } else {
                errorDescription = e.getMessage();
            }

            Logger.error(
                    TAG + methodName,
                    errorDescription + " to perform [" + methodName + "]. " + e.getMessage(),
                    e);

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.IO_ERROR)
                            .putErrorDescription(e.getMessage()));

            throw e;
        } finally {
            client.disconnect();
        }

        Telemetry.emit(
                new BrokerEndEvent()
                        .putAction(methodName)
                        .isSuccessful(true)
        );

        return result;
    }

    private Intent getBrokerAuthorizationIntentFromAuthService(
            @NonNull final InteractiveTokenCommandContext context)
            throws BaseException, InterruptedException, ExecutionException, RemoteException {
        return performAuthServiceOperation(context.androidApplicationContext(),
                new AuthServiceOperation<Intent>() {
                    @Override
                    public Intent perform(IMicrosoftAuthService service) throws RemoteException {
                        return service.getIntentForInteractiveRequest();
                    }

                    @Override
                    public String getOperationName() {
                        return ":getBrokerAuthorizationIntentFromAuthService";
                    }
                });
    }

    @WorkerThread
    AcquireTokenResult acquireTokenSilent(
            @NonNull final SilentTokenCommandContext context,
            @NonNull final SilentTokenCommandParameters parameters)
            throws BaseException, InterruptedException, ExecutionException, RemoteException {
        return performAuthServiceOperation(context.androidApplicationContext(),
                new AuthServiceOperation<AcquireTokenResult>() {
                    @Override
                    public AcquireTokenResult perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = getSilentBrokerRequestBundle(context,parameters);
                        final Bundle resultBundle = service.acquireTokenSilently(requestBundle);
                        return getAcquireTokenResult(resultBundle);
                    }

                    @Override
                    public String getOperationName() {
                        return ":acquireTokenSilentWithAuthService";
                    }
                });
    }

    @WorkerThread
    protected List<ICacheRecord> getBrokerAccounts(
            @NonNull final LoadAccountCommandContext context,
            @NonNull final LoadAccountCommandParameters parameters)
            throws BaseException, InterruptedException, ExecutionException, RemoteException {
        return performAuthServiceOperation(context.androidApplicationContext(),
                new AuthServiceOperation<List<ICacheRecord>>() {
                    @Override
                    public List<ICacheRecord> perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = getRequestBundleForGetAccounts(parameters);
                        return MsalBrokerResultAdapter
                                .accountsFromBundle(
                                        service.getAccounts(requestBundle)
                                );

                    }

                    @Override
                    public String getOperationName() {
                        return ":getBrokerAccountsWithAuthService";
                    }
                });
    }

    static Bundle getRequestBundleForGetAccounts(@NonNull final IAccountCommandParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.clientId());
        requestBundle.putString(ACCOUNT_REDIRECT, parameters.redirectUri());
        //Disable the environment and tenantID. Just return all accounts belong to this clientID.
        return requestBundle;
    }

    @WorkerThread
    protected void removeBrokerAccount(
            @NonNull final RemoveAccountCommandContext context,
            @NonNull final RemoveAccountCommandParameters parameters)
            throws BaseException, InterruptedException, ExecutionException, RemoteException {
        performAuthServiceOperation(context.androidApplicationContext(),
                new AuthServiceOperation<Void>() {
                    @Override
                    public Void perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = getRequestBundleForRemoveAccount(parameters);
                        MsalBrokerResultAdapter.verifyRemoveAccountResultFromBundle(
                                service.removeAccount(requestBundle)
                        );

                        return null;
                    }

                    @Override
                    public String getOperationName() {
                        return ":removeBrokerAccountWithAuthService";
                    }
                });
    }

    static Bundle getRequestBundleForRemoveAccount(@NonNull final RemoveAccountCommandParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.clientId());
        if (null != parameters.accountRecord()) {
            requestBundle.putString(ENVIRONMENT, parameters.accountRecord().getEnvironment());
            requestBundle.putString(ACCOUNT_HOME_ACCOUNT_ID, parameters.accountRecord().getHomeAccountId());
        }

        return requestBundle;
    }

    @WorkerThread
    protected boolean getDeviceMode(
            @NonNull GetDeviceModeCommandContext context,
            @NonNull GetDeviceModeCommandParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException {
        return performAuthServiceOperation(context.androidApplicationContext(),
                new AuthServiceOperation<Boolean>() {
                    @Override
                    public Boolean perform(IMicrosoftAuthService service) throws BaseException, RemoteException {
                        return MsalBrokerResultAdapter
                                .deviceModeFromBundle(
                                        service.getDeviceMode()
                                );
                    }

                    @Override
                    public String getOperationName() {
                        return ":getDeviceModeWithAuthService";
                    }
                });
    }

    @WorkerThread
    protected List<ICacheRecord> getCurrentAccountInSharedDevice(
            @NonNull final GetCurrentAccountCommandContext context,
            @NonNull final GetCurrentAccountCommandParameters parameters) throws InterruptedException, ExecutionException, RemoteException, BaseException {
        return performAuthServiceOperation(context.androidApplicationContext(),
                new AuthServiceOperation<List<ICacheRecord>>() {
                    @Override
                    public List<ICacheRecord> perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        return MsalBrokerResultAdapter
                                .accountsFromBundle(
                                        service.getCurrentAccount(
                                                BrokerAuthServiceStrategy.getRequestBundleForGetAccounts(parameters)
                                        ));
                    }

                    @Override
                    public String getOperationName() {
                        return ":getCurrentAccountInSharedDeviceWithAuthService";
                    }
                });
    }

    @WorkerThread
    protected void signOutFromSharedDevice(
            @NonNull final RemoveCurrentAccountCommandContext context,
            @NonNull final RemoveCurrentAccountCommandParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException {
        performAuthServiceOperation(context.androidApplicationContext(),
                new AuthServiceOperation<Void>() {
                    @Override
                    public Void perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = getRequestBundleForRemoveAccountFromSharedDevice(context);
                        MsalBrokerResultAdapter.verifyRemoveAccountResultFromBundle(
                                service.removeAccountFromSharedDevice(requestBundle)
                        );

                        return null;
                    }

                    @Override
                    public String getOperationName() {
                        return ":signOutFromSharedDeviceWithAuthService";
                    }
                });
    }

    private Bundle getRequestBundleForRemoveAccountFromSharedDevice(@NonNull final RemoveCurrentAccountCommandContext context) {
        final Bundle requestBundle = new Bundle();

        try {
            Browser browser = BrowserSelector.select(context.androidApplicationContext(), context.browserSafeList());
            requestBundle.putString(DEFAULT_BROWSER_PACKAGE_NAME, browser.getPackageName());
        } catch (ClientException e) {
            // Best effort. If none is passed to broker, then it will let the OS decide.
            Logger.error(TAG, e.getErrorCode(), e);
        }

        return requestBundle;
    }
}
