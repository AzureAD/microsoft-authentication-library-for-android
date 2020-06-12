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
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.client.IMicrosoftAuthService;
import com.microsoft.identity.client.exception.BrokerCommunicationException;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthServiceFuture;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.BrokerEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.BrokerStartEvent;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class BrokerAuthServiceStrategy extends BrokerBaseStrategy {
    private static final String TAG = BrokerAuthServiceStrategy.class.getSimpleName();

    /**
     * Get the intent for the broker interactive request
     *
     * @param parameters
     * @return
     */
    @WorkerThread
    Intent getBrokerAuthorizationIntent(@NonNull final InteractiveTokenCommandParameters parameters,
                                        @Nullable final String negotiatedBrokerProtocolVersion)
            throws BaseException {
        final String methodName = ":getBrokerAuthorizationIntent";
        Logger.verbose(TAG + methodName, "Get the broker authorization intent from auth service.");
        Intent interactiveRequestIntent;
        interactiveRequestIntent = getBrokerAuthorizationIntentFromAuthService(parameters);
        return completeInteractiveRequestIntent(
                interactiveRequestIntent,
                parameters,
                negotiatedBrokerProtocolVersion
        );
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
            throws BaseException {

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
        } catch (final RemoteException | InterruptedException | ExecutionException e) {
            final String errorDescription;
            if (e instanceof RemoteException) {
                errorDescription = "RemoteException occurred while attempting to invoke remote service";
            } else {
                errorDescription = "Exception occurred while awaiting (get) return of MicrosoftAuthService";
            }

            Logger.error(TAG + methodName, errorDescription, e);
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.IO_ERROR)
                            .putErrorDescription(e.getMessage()));

            throw new BrokerCommunicationException(errorDescription, e);
        } catch (final BaseException e) {
            Logger.error(TAG + methodName, e.getMessage(), e);
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(e.getErrorCode())
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

    @WorkerThread
    String hello(@NonNull final CommandParameters parameters) throws BaseException {
        return performAuthServiceOperation(parameters.getAndroidApplicationContext(),
                new AuthServiceOperation<String>() {
                    @Override
                    public String perform(IMicrosoftAuthService service) throws RemoteException, ClientException {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForHello(parameters);
                        String negotiatedProtocolVersion = mResultAdapter.verifyHelloFromResultBundle(
                                service.hello(requestBundle)
                        );
                        return negotiatedProtocolVersion;
                    }

                    @Override
                    public String getOperationName() {
                        return ":helloWithMicrosoftAuthService";
                    }
                });
    }

    private Intent getBrokerAuthorizationIntentFromAuthService(@NonNull final InteractiveTokenCommandParameters parameters)
            throws BaseException {
        return performAuthServiceOperation(parameters.getAndroidApplicationContext(),
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
    AcquireTokenResult acquireTokenSilent(@NonNull final SilentTokenCommandParameters parameters,
                                          @Nullable final String negotiatedBrokerProtocolVersion)
            throws BaseException {
        return performAuthServiceOperation(parameters.getAndroidApplicationContext(),
                new AuthServiceOperation<AcquireTokenResult>() {
                    @Override
                    public AcquireTokenResult perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForAcquireTokenSilent(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                        return mResultAdapter.getAcquireTokenResultFromResultBundle(
                                service.acquireTokenSilently(requestBundle)
                        );
                    }

                    @Override
                    public String getOperationName() {
                        return ":acquireTokenSilentWithAuthService";
                    }
                });
    }

    @WorkerThread
    protected List<ICacheRecord> getBrokerAccounts(@NonNull final CommandParameters parameters,
                                                   @Nullable final String negotiatedBrokerProtocolVersion)
            throws BaseException {
        return performAuthServiceOperation(parameters.getAndroidApplicationContext(),
                new AuthServiceOperation<List<ICacheRecord>>() {
                    @Override
                    public List<ICacheRecord> perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForGetAccounts(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                        return mResultAdapter.getAccountsFromResultBundle(
                                service.getAccounts(requestBundle)
                        );

                    }

                    @Override
                    public String getOperationName() {
                        return ":getBrokerAccountsWithAuthService";
                    }
                });
    }

    @WorkerThread
    protected void removeBrokerAccount(@NonNull final RemoveAccountCommandParameters parameters,
                                       @Nullable final String negotiatedBrokerProtocolVersion)
            throws BaseException {
        performAuthServiceOperation(parameters.getAndroidApplicationContext(),
                new AuthServiceOperation<Void>() {
                    @Override
                    public Void perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForRemoveAccount(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                        mResultAdapter.verifyRemoveAccountResultFromBundle(
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

    @WorkerThread
    protected boolean getDeviceMode(@NonNull CommandParameters parameters,
                                    @Nullable final String negotiatedBrokerProtocolVersion) throws BaseException {
        return performAuthServiceOperation(parameters.getAndroidApplicationContext(),
                new AuthServiceOperation<Boolean>() {
                    @Override
                    public Boolean perform(IMicrosoftAuthService service) throws BaseException, RemoteException {
                        return mResultAdapter.getDeviceModeFromResultBundle(
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
    protected List<ICacheRecord> getCurrentAccountInSharedDevice(@NonNull final CommandParameters parameters,
                                                                 @Nullable final String negotiatedBrokerProtocolVersion) throws BaseException {
        return performAuthServiceOperation(parameters.getAndroidApplicationContext(),
                new AuthServiceOperation<List<ICacheRecord>>() {
                    @Override
                    public List<ICacheRecord> perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForGetAccounts(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                        return mResultAdapter.getAccountsFromResultBundle(
                                service.getCurrentAccount(requestBundle)
                        );
                    }

                    @Override
                    public String getOperationName() {
                        return ":getCurrentAccountInSharedDeviceWithAuthService";
                    }
                });
    }

    @WorkerThread
    protected void signOutFromSharedDevice(@NonNull final RemoveAccountCommandParameters parameters,
                                           @Nullable final String negotiatedBrokerProtocolVersion) throws BaseException {
        performAuthServiceOperation(parameters.getAndroidApplicationContext(),
                new AuthServiceOperation<Void>() {
                    @Override
                    public Void perform(IMicrosoftAuthService service) throws RemoteException, BaseException {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForRemoveAccountFromSharedDevice(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                        mResultAdapter.verifyRemoveAccountResultFromBundle(
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
}
