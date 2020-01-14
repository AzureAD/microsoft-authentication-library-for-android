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

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.telemetry.events.BrokerEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.BrokerStartEvent;

import java.io.IOException;
import java.util.List;

public class BrokerAccountManagerStrategy extends BrokerBaseStrategy {
    private static final String TAG = BrokerAccountManagerStrategy.class.getSimpleName();

    public interface OperationInfo<T extends OperationParameters, U> {
        /**
         * Performs this account manager operation in this method with the given IMicrosoftAuthService.
         */
        Bundle getRequestBundle(T parameters);

        /**
         * Name of the task (for logging purposes).
         */
        String getMethodName();

        /**
         * Extracts result from the given bundle.
         */
        U getResultFromBundle(Bundle bundle) throws BaseException;
    }

    @SuppressLint("MissingPermission")
    public <T extends OperationParameters, U> U invokeBrokerAccountManagerOperation(final T parameters,
                                                                                    final OperationInfo<T, U> operationInfo) throws BaseException {
        final String methodName = operationInfo.getMethodName();

        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.ACCOUNT_MANAGER)
        );

        final U result;
        try {
            final AccountManager accountManager = AccountManager.get(parameters.getAppContext());
            final AccountManagerFuture<Bundle> resultBundle =
                    accountManager.addAccount(
                            AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE,
                            AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                            null,
                            operationInfo.getRequestBundle(parameters),
                            null,
                            null,
                            getPreferredHandler());

            Logger.verbose(TAG + methodName, "Received result from broker");
            result = operationInfo.getResultFromBundle(resultBundle.getResult());
        } catch (final AuthenticatorException | IOException | OperationCanceledException e) {
            Logger.error(TAG + methodName, e.getMessage(), e);
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.IO_ERROR)
                            .putErrorDescription(e.getMessage()));

            throw new BrokerCommunicationException("Failed to connect to AccountManager");
        } catch (final BaseException e) {
            Logger.error(TAG + methodName, e.getMessage(), e);
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(e.getErrorCode())
                            .putErrorDescription(e.getMessage()));

            throw e;
        }

        Telemetry.emit(
                new BrokerEndEvent()
                        .putAction(methodName)
                        .isSuccessful(true)
        );

        return result;
    }

    @WorkerThread
    @SuppressLint("MissingPermission")
    void hello(@NonNull final OperationParameters parameters)
            throws BaseException {

        if (!BrokerMsalController.isAccountManagerPermissionsGranted(parameters.getAppContext())) {
            throw new BrokerCommunicationException("Account manager permissions are not granted");
        }

        invokeBrokerAccountManagerOperation(parameters, new OperationInfo<OperationParameters, Void>() {
            @Override
            public Bundle getRequestBundle(OperationParameters parameters) {
                final Bundle requestBundle = mRequestAdapter.getRequestBundleForHello(parameters);
                requestBundle.putString(AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                        AuthenticationConstants.BrokerAccountManagerOperation.HELLO);
                return requestBundle;
            }

            @Override
            public String getMethodName() {
                return ":helloWithAccountManager";
            }

            @Override
            public Void getResultFromBundle(Bundle bundle) throws BaseException {
                mResultAdapter.verifyHelloFromResultBundle(bundle);
                return null;
            }
        });
    }

    /**
     * Get the intent for the broker interactive request
     *
     * @param parameters
     * @return
     */
    @WorkerThread
    Intent getBrokerAuthorizationIntent(@NonNull final AcquireTokenOperationParameters parameters) throws BaseException {
        final String methodName = ":getBrokerAuthorizationIntent";
        Logger.verbose(TAG + methodName, "Get the broker authorization intent from Account Manager.");

        return invokeBrokerAccountManagerOperation(parameters,
                new OperationInfo<AcquireTokenOperationParameters, Intent>() {
                    @Override
                    public Bundle getRequestBundle(AcquireTokenOperationParameters parameters) {
                        final Bundle requestBundle = new Bundle();
                        requestBundle.putString(AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                                AuthenticationConstants.BrokerAccountManagerOperation.GET_INTENT_FOR_INTERACTIVE_REQUEST);
                        return requestBundle;
                    }

                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @Override
                    public Intent getResultFromBundle(Bundle bundle) {
                        final Intent interactiveRequestIntent = bundle.getParcelable(AccountManager.KEY_INTENT);
                        return completeInteractiveRequestIntent(interactiveRequestIntent, parameters);

                    }
                });
    }

    @WorkerThread
    AcquireTokenResult acquireTokenSilent(final AcquireTokenSilentOperationParameters parameters)
            throws BaseException {
        return invokeBrokerAccountManagerOperation(parameters,
                new OperationInfo<AcquireTokenSilentOperationParameters, AcquireTokenResult>() {
                    @Override
                    public Bundle getRequestBundle(AcquireTokenSilentOperationParameters parameters) {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForAcquireTokenSilent(parameters);
                        requestBundle.putString(AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                                AuthenticationConstants.BrokerAccountManagerOperation.ACQUIRE_TOKEN_SILENT);
                        return requestBundle;
                    }

                    @Override
                    public String getMethodName() {
                        return ":acquireTokenSilentWithAccountManager";
                    }

                    @Override
                    public AcquireTokenResult getResultFromBundle(Bundle bundle) throws BaseException {
                        return mResultAdapter.getAcquireTokenResultFromResultBundle(bundle);
                    }
                });
    }

    @WorkerThread
    protected List<ICacheRecord> getBrokerAccounts(@NonNull final OperationParameters parameters) throws BaseException {
        return invokeBrokerAccountManagerOperation(parameters,
                new OperationInfo<OperationParameters, List<ICacheRecord>>() {
                    @Override
                    public Bundle getRequestBundle(OperationParameters parameters) {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForGetAccounts(parameters);
                        requestBundle.putString(AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                                AuthenticationConstants.BrokerAccountManagerOperation.GET_ACCOUNTS);
                        return requestBundle;
                    }

                    @Override
                    public String getMethodName() {
                        return ":getBrokerAccountsWithAccountManager";
                    }

                    @Override
                    public List<ICacheRecord> getResultFromBundle(Bundle bundle) throws BaseException {
                        return mResultAdapter.getAccountsFromResultBundle(bundle);
                    }
                });
    }

    @WorkerThread
    protected void removeBrokerAccount(@NonNull final OperationParameters parameters) throws BaseException {
        invokeBrokerAccountManagerOperation(parameters,
                new OperationInfo<OperationParameters, Void>() {
                    @Override
                    public Bundle getRequestBundle(OperationParameters parameters) {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForRemoveAccount(parameters);
                        requestBundle.putString(AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                                AuthenticationConstants.BrokerAccountManagerOperation.REMOVE_ACCOUNT);
                        return requestBundle;
                    }

                    @Override
                    public String getMethodName() {
                        return ":removeBrokerAccountWithAccountManager";
                    }

                    @Override
                    public Void getResultFromBundle(Bundle bundle) throws BaseException {
                        mResultAdapter.verifyRemoveAccountResultFromBundle(bundle);
                        return null;
                    }
                });
    }

    @Override
    boolean getDeviceMode(@NonNull OperationParameters parameters) throws BaseException {
        return invokeBrokerAccountManagerOperation(parameters,
                new OperationInfo<OperationParameters, Boolean>() {
                    @Override
                    public Bundle getRequestBundle(OperationParameters parameters) {
                        final Bundle requestBundle = new Bundle();
                        requestBundle.putString(AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                                AuthenticationConstants.BrokerAccountManagerOperation.GET_DEVICE_MODE);
                        return requestBundle;
                    }

                    @Override
                    public String getMethodName() {
                        return ":getDeviceModeWithAccountManager";
                    }

                    @Override
                    public Boolean getResultFromBundle(Bundle bundle) throws BaseException {
                        return mResultAdapter.getDeviceModeFromResultBundle(bundle);
                    }
                });
    }

    @Override
    List<ICacheRecord> getCurrentAccountInSharedDevice(@NonNull OperationParameters parameters) throws BaseException {
        return invokeBrokerAccountManagerOperation(parameters,
                new OperationInfo<OperationParameters, List<ICacheRecord>>() {
                    @Override
                    public Bundle getRequestBundle(OperationParameters parameters) {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForGetAccounts(parameters);
                        requestBundle.putString(AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                                AuthenticationConstants.BrokerAccountManagerOperation.GET_CURRENT_ACCOUNT);
                        return requestBundle;
                    }

                    @Override
                    public String getMethodName() {
                        return ":getCurrentAccountInSharedDeviceWithAccountManager";
                    }

                    @Override
                    public List<ICacheRecord> getResultFromBundle(Bundle bundle) throws BaseException {
                        return mResultAdapter.getAccountsFromResultBundle(bundle);
                    }
                });
    }

    @Override
    void signOutFromSharedDevice(@NonNull OperationParameters parameters) throws BaseException {
        invokeBrokerAccountManagerOperation(parameters,
                new OperationInfo<OperationParameters, Void>() {
                    @Override
                    public Bundle getRequestBundle(OperationParameters parameters) {
                        final Bundle requestBundle = mRequestAdapter.getRequestBundleForRemoveAccountFromSharedDevice(parameters);
                        requestBundle.putString(AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                                AuthenticationConstants.BrokerAccountManagerOperation.REMOVE_ACCOUNT_FROM_SHARED_DEVICE);
                        return requestBundle;
                    }

                    @Override
                    public String getMethodName() {
                        return ":signOutFromSharedDeviceWithAccountManager";
                    }

                    @Override
                    public Void getResultFromBundle(Bundle bundle) throws BaseException {
                        mResultAdapter.verifyRemoveAccountResultFromBundle(bundle);
                        return null;
                    }
                });
    }

}
