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
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandContext;
import com.microsoft.identity.common.internal.request.generated.GetCurrentAccountCommandParameters;
import com.microsoft.identity.common.internal.request.generated.GetDeviceModeCommandContext;
import com.microsoft.identity.common.internal.request.generated.GetDeviceModeCommandParameters;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ENVIRONMENT;

public class BrokerAccountManagerStrategy extends BrokerBaseStrategy {
    private static final String TAG = BrokerAccountManagerStrategy.class.getSimpleName();

    private static final String DATA_CACHE_RECORD = "com.microsoft.workaccount.cache.record";

    /**
     * Get the intent for the broker interactive request
     *
     * @param parameters
     * @return
     */
    @WorkerThread
    Intent getBrokerAuthorizationIntent(
            @NonNull final InteractiveTokenCommandContext context,
            @NonNull final InteractiveTokenCommandParameters parameters) throws ClientException {
        final String methodName = ":getBrokerAuthorizationIntent";
        Logger.verbose(TAG + methodName, "Get the broker authorization intent from Account Manager.");
        return getBrokerAuthorizationIntentFromAccountManager(context,parameters);
    }

    @SuppressWarnings("PMD")
    @SuppressLint("MissingPermission")
    private Intent getBrokerAuthorizationIntentFromAccountManager(
            @NonNull final InteractiveTokenCommandContext context,
            @NonNull final InteractiveTokenCommandParameters parameters) throws ClientException {
        final String methodName = ":getBrokerAuthorizationIntentFromAccountManager";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.ACCOUNT_MANAGER)
        );

        Intent intent;
        try {
            final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();

            final Bundle requestBundle = new Bundle();
            final BrokerRequest brokerRequest = msalBrokerRequestAdapter.
                    brokerRequestFromAcquireTokenParameters(context, parameters);
            requestBundle.putInt(
                    AuthenticationConstants.Broker.CALLER_INFO_UID,
                    Binder.getCallingUid()
            );
            requestBundle.putString(
                    AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                    new Gson().toJson(brokerRequest, BrokerRequest.class)
            );

            final AccountManager accountManager = AccountManager.get(context.androidApplicationContext());
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
            final String errorMessage = "OperationCanceledException thrown when talking to account manager. The broker request cancelled.";
            Logger.error(
                    TAG + methodName,
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    errorMessage,
                    e
            );

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                            .putErrorDescription(errorMessage)
            );

            throw new ClientException(
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    errorMessage,
                    e
            );
        } catch (final AuthenticatorException e) {
            final String errorMessage = "AuthenticatorException thrown when talking to account manager. The broker request cancelled.";

            Logger.error(
                    TAG + methodName,
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    errorMessage,
                    e
            );

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                            .putErrorDescription(errorMessage)
            );

            throw new ClientException(
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    errorMessage,
                    e
            );
        } catch (final IOException e) {
            final String errorMessage = "IOException thrown when talking to account manager. The broker request cancelled.";
            // Authenticator gets problem from webrequest or file read/write
            Logger.error(
                    TAG + methodName,
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    errorMessage,
                    e
            );

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                            .putErrorDescription(errorMessage)
            );

            throw new ClientException(
                    ErrorStrings.BROKER_REQUEST_CANCELLED,
                    errorMessage,
                    e
            );
        }

        return intent;
    }

    @WorkerThread
    @SuppressWarnings("PMD")
    @SuppressLint("MissingPermission")
    AcquireTokenResult acquireTokenSilent(
            @NonNull final SilentTokenCommandContext context,
            @NonNull final SilentTokenCommandParameters parameters)
            throws BaseException {
        // if there is not any user added to account, it returns empty
        final String methodName = ":acquireTokenSilentWithAccountManager";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.ACCOUNT_MANAGER)
        );

        Bundle bundleResult = null;
        if (parameters.account() != null) {
            // blocking call to get token from cache or refresh request in
            // background at Authenticator
            try {

                final Bundle requestBundle = getSilentBrokerRequestBundle(context, parameters);

                // It does not expect activity to be launched.
                // AuthenticatorService is handling the request at
                // AccountManager.
                final AccountManager accountManager = AccountManager.get(context.androidApplicationContext());
                final AccountManagerFuture<Bundle> result =
                        accountManager.getAuthToken(
                                getTargetAccount(context.androidApplicationContext(), parameters.account()),
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
                final String errorMsg = "OperationCanceledException thrown when talking to account manager. The broker request cancelled.";
                Logger.error(
                        TAG + methodName,
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        errorMsg,
                        e
                );

                Telemetry.emit(
                        new BrokerEndEvent()
                                .putAction(methodName)
                                .isSuccessful(false)
                                .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                                .putErrorDescription(errorMsg)
                );

                throw new ClientException(
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        errorMsg,
                        e
                );
            } catch (final AuthenticatorException e) {
                final String errorMsg = "AuthenticatorException thrown when talking to account manager. The broker request cancelled.";
                Logger.error(
                        TAG + methodName,
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        errorMsg,
                        e
                );

                Telemetry.emit(
                        new BrokerEndEvent()
                                .putAction(methodName)
                                .isSuccessful(false)
                                .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                                .putErrorDescription(errorMsg)
                );

                throw new ClientException(
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        errorMsg,
                        e
                );
            } catch (final IOException e) {
                final String errorMsg = "IOException thrown when talking to account manager. The broker request cancelled.";
                // Authenticator gets problem from webrequest or file read/write
                Logger.error(
                        TAG + methodName,
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        errorMsg,
                        e
                );

                Telemetry.emit(
                        new BrokerEndEvent()
                                .putAction(methodName)
                                .isSuccessful(false)
                                .putErrorCode(ErrorStrings.BROKER_REQUEST_CANCELLED)
                                .putErrorDescription(errorMsg)
                );

                throw new ClientException(
                        ErrorStrings.BROKER_REQUEST_CANCELLED,
                        errorMsg,
                        e
                );
            }
        }

        return getAcquireTokenResult(bundleResult);
    }

    @WorkerThread
    @SuppressWarnings("PMD")
    @SuppressLint("MissingPermission")
    protected List<ICacheRecord> getBrokerAccounts(
            @NonNull final LoadAccountCommandContext context,
            @NonNull final LoadAccountCommandParameters parameters)
            throws OperationCanceledException, IOException, AuthenticatorException, BaseException {
        final String methodName = ":getBrokerAccountsFromAccountManager";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.ACCOUNT_MANAGER)
        );

        final Account[] accountList = AccountManager.get(context.androidApplicationContext()).getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        final List<ICacheRecord> cacheRecords = new ArrayList<>();
        Logger.verbose(
                TAG + methodName,
                "Retrieve all the accounts from account manager with broker account type, "
                        + "and the account length is: " + accountList.length
        );

        if (accountList == null || accountList.length == 0) {
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorCode(ErrorStrings.NO_ACCOUNT_FOUND)
            );
            return cacheRecords;
        } else {
            final Bundle bundle = new Bundle();
            bundle.putBoolean(DATA_CACHE_RECORD, true);
            bundle.putInt(AuthenticationConstants.Broker.CALLER_INFO_UID,
                    Binder.getCallingUid());
            bundle.putString(ACCOUNT_CLIENTID_KEY, parameters.clientId());

            for (final Account eachAccount : accountList) {
                // Use AccountManager Api method to get extended user info

                final AccountManagerFuture<Bundle> result = AccountManager.get(context.androidApplicationContext())
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

            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(true)
            );

            return cacheRecords;
        }
    }

    @WorkerThread
    @SuppressWarnings("PMD")
    @SuppressLint("MissingPermission")
    protected void removeBrokerAccount(
            @NonNull final RemoveAccountCommandContext context,
            @NonNull final RemoveAccountCommandParameters parameters) {
        final String methodName = ":removeBrokerAccountFromAccountManager";
        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.ACCOUNT_MANAGER)
        );
        // getAuthToken call will execute in async as well
        Logger.verbose(TAG + methodName, "Try to remove account from account manager.");

        //If account is null, remove all accounts from broker
        //Otherwise, get the target account and remove it from broker
        Account[] accountList = AccountManager.get(context.androidApplicationContext()).getAccountsByType(AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE);
        if (accountList != null && accountList.length > 0) {
            for (final Account eachAccount : accountList) {
                if (parameters.accountRecord() == null || eachAccount.name.equalsIgnoreCase(parameters.accountRecord().getUsername())) {
                    //create remove request bundle
                    Bundle brokerOptions = new Bundle();
                    brokerOptions.putString(ACCOUNT_CLIENTID_KEY, parameters.clientId());
                    brokerOptions.putString(ENVIRONMENT, parameters.accountRecord().getEnvironment());
                    brokerOptions.putInt(AuthenticationConstants.Broker.CALLER_INFO_UID,
                            Binder.getCallingUid());
                    brokerOptions.putString(ACCOUNT_HOME_ACCOUNT_ID, parameters.accountRecord().getHomeAccountId());
                    brokerOptions.putString(AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS,
                            AuthenticationConstants.Broker.ACCOUNT_REMOVE_TOKENS_VALUE);
                    AccountManager.get(context.androidApplicationContext()).getAuthToken(
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

        Telemetry.emit(
                new BrokerEndEvent()
                        .putAction(methodName)
                        .isSuccessful(true)
        );
    }

    @Override
    boolean getDeviceMode(
            @NonNull GetDeviceModeCommandContext context,
            @NonNull GetDeviceModeCommandParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException {
        // TODO
        throw new MsalClientException("getDeviceMode() is not yet implemented in BrokerAccountManagerStrategy()");
    }

    @Override
    List<ICacheRecord> getCurrentAccountInSharedDevice(
            @NonNull GetCurrentAccountCommandContext context,
            @NonNull GetCurrentAccountCommandParameters parameters) throws InterruptedException, ExecutionException, RemoteException, OperationCanceledException, IOException, AuthenticatorException, BaseException {
        // TODO
        throw new MsalClientException("getCurrentAccountInSharedDevice() is not yet implemented in BrokerAccountManagerStrategy()");
    }

    @Override
    void signOutFromSharedDevice(
            @NonNull RemoveCurrentAccountCommandContext context,
            @NonNull RemoveCurrentAccountCommandParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException {
        // TODO
        throw new MsalClientException("signOutFromSharedDevice() is not yet implemented in BrokerAccountManagerStrategy()");
    }

}
