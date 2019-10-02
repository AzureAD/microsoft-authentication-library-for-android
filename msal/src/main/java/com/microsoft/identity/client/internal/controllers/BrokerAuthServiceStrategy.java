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
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.BrokerEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.BrokerStartEvent;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;
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
    Intent getBrokerAuthorizationIntent(@NonNull final AcquireTokenOperationParameters parameters) throws ClientException {
        final String methodName = ":getBrokerAuthorizationIntent";
        Logger.verbose(TAG + methodName, "Get the broker authorization intent from auth service.");
        Intent interactiveRequestIntent;
        interactiveRequestIntent = getBrokerAuthorizationIntentFromAuthService(parameters);
        final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();
        interactiveRequestIntent.putExtra(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                new Gson().toJson(
                        msalBrokerRequestAdapter.brokerRequestFromAcquireTokenParameters(parameters),
                        BrokerRequest.class)
        );
        interactiveRequestIntent.putExtra(AuthenticationConstants.Broker.ACCOUNT_NAME, parameters.getLoginHint());


        return interactiveRequestIntent;
    }

    private Intent getBrokerAuthorizationIntentFromAuthService(@NonNull final AcquireTokenOperationParameters parameters)
            throws ClientException {
        final String methodName = ":getBrokerAuthorizationIntentFromAuthService";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.BOUND_SERVICE)
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

    @WorkerThread
    AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters parameters) throws BaseException {
        final String methodName = ":acquireTokenSilentWithAuthService";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.BOUND_SERVICE)
        );

        IMicrosoftAuthService service;
        MicrosoftAuthClient client = new MicrosoftAuthClient(parameters.getAppContext());
        MicrosoftAuthServiceFuture future = client.connect();

        try {
            //Do we want a time out here?
            service = future.get();
        } catch (final InterruptedException | ExecutionException e) {
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.IO_ERROR)
                            .putErrorDescription(e.getLocalizedMessage())
            );

            throw new RuntimeException("Exception occurred while awaiting (get) return of MicrosoftAuthService", e);
        }

        try {
            final Bundle requestBundle = getSilentBrokerRequestBundle(parameters);
            final Bundle resultBundle = service.acquireTokenSilently(requestBundle);
            final AcquireTokenResult result = getAcquireTokenResult(resultBundle);

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
                    "RemoteException occurred while attempting to invoke remote service",
                    e
            );
        } finally {
            client.disconnect();
        }
    }

    @WorkerThread
    protected List<ICacheRecord> getBrokerAccounts(@NonNull final OperationParameters parameters)
            throws ClientException, InterruptedException, ExecutionException, RemoteException {
        final String methodName = ":getBrokerAccountsWithAuthService";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.BOUND_SERVICE)
        );

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

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(true)
            );

            return cacheRecords;
        } catch (final ClientException | InterruptedException | ExecutionException | RemoteException e) {
            com.microsoft.identity.common.internal.logging.Logger.error(
                    TAG + methodName,
                    "Exception is thrown when trying to get account from Broker, returning empty list."
                            + e.getMessage(),
                    ErrorStrings.IO_ERROR,
                    e);

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.IO_ERROR)
                            .putErrorDescription(e.getLocalizedMessage())
            );

            throw e;
        } finally {
            client.disconnect();
        }
    }

    static Bundle getRequestBundleForGetAccounts(@NonNull final OperationParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
        requestBundle.putString(ACCOUNT_REDIRECT, parameters.getRedirectUri());
        //Disable the environment and tenantID. Just return all accounts belong to this clientID.
        return requestBundle;
    }

    @WorkerThread
    protected boolean removeBrokerAccount(@NonNull final OperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException {
        final String methodName = ":removeBrokerAccountWithAuthService";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.BOUND_SERVICE)
        );

        IMicrosoftAuthService service;
        final MicrosoftAuthClient client = new MicrosoftAuthClient(parameters.getAppContext());

        try {
            final MicrosoftAuthServiceFuture authServiceFuture = client.connect();

            service = authServiceFuture.get();

            Bundle requestBundle = getRequestBundleForRemoveAccount(parameters);
            service.removeAccount(requestBundle);
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(true)
            );

            return true;
        } catch (final BaseException | InterruptedException | ExecutionException | RemoteException e) {
            com.microsoft.identity.common.internal.logging.Logger.error(
                    TAG + methodName,
                    "Exception is thrown when trying to get target account."
                            + e.getMessage(),
                    ErrorStrings.IO_ERROR,
                    e);
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.IO_ERROR)
                            .putErrorDescription(e.getLocalizedMessage())
            );

            throw e;
        } finally {
            client.disconnect();
        }
    }

    static Bundle getRequestBundleForRemoveAccount(@NonNull final OperationParameters parameters) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, parameters.getClientId());
        if (null != parameters.getAccount()) {
            requestBundle.putString(ENVIRONMENT, parameters.getAccount().getEnvironment());
            requestBundle.putString(ACCOUNT_HOME_ACCOUNT_ID, parameters.getAccount().getHomeAccountId());
        }

        return requestBundle;
    }
}
