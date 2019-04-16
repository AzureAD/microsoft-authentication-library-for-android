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
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.microsoft.identity.client.AccountAdapter;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IMicrosoftAuthService;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.PublicClientApplicationConfiguration;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerResultFuture;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthClient;
import com.microsoft.identity.common.internal.broker.MicrosoftAuthServiceFuture;
import com.microsoft.identity.common.internal.cache.ADALTokenCacheItem;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAccount;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftRefreshToken;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_ENVIRONMENT_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_LOGIN_HINT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;

/**
 * The implementation of MSAL Controller for Broker
 */
public class BrokerMsalController extends BaseController {

    private static final String TAG = BrokerMsalController.class.getSimpleName();

    private BrokerResultFuture mBrokerResultFuture;

    /**
     * ExecutorService to handle background computation.
     */
    private static final ExecutorService sBackgroundExecutor = Executors.newCachedThreadPool();

    @Override
    public AcquireTokenResult acquireToken(AcquireTokenOperationParameters parameters)
            throws InterruptedException, BaseException {

        //Create BrokerResultFuture to block on response from the broker... response will be return as an activity result
        //BrokerActivity will receive the result and ask the API dispatcher to complete the request
        //In completeAcquireToken below we will set the result on the future and unblock the flow.
        mBrokerResultFuture = new BrokerResultFuture();

        //Get the broker interactive parameters intent
        final Intent interactiveRequestIntent = getBrokerAuthorizationIntent(parameters);

        final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();
        interactiveRequestIntent.putExtra(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                msalBrokerRequestAdapter.brokerRequestFromAcquireTokenParameters(parameters)
        );

        //Pass this intent to the BrokerActivity which will be used to start this activity
        final Intent brokerActivityIntent = new Intent(parameters.getAppContext(), BrokerActivity.class);
        brokerActivityIntent.putExtra(BrokerActivity.BROKER_INTENT, interactiveRequestIntent);

        //Start the BrokerActivity
        parameters.getActivity().startActivity(brokerActivityIntent);

        //Wait to be notified of the result being returned... we could add a timeout here if we want to
        final Bundle resultBundle = mBrokerResultFuture.get();

        return getAcquireTokenResult(resultBundle);

    }

    /**
     * Get the intent for the broker interactive request
     *
     * @param request
     * @return
     */
    private Intent getBrokerAuthorizationIntent(@NonNull final AcquireTokenOperationParameters request) throws ClientException {
        Intent interactiveRequestIntent;
        IMicrosoftAuthService service;

        final MicrosoftAuthClient client = new MicrosoftAuthClient(request.getAppContext());
        final MicrosoftAuthServiceFuture authServiceFuture = client.connect();

        try {
            service = authServiceFuture.get();
            interactiveRequestIntent = service.getIntentForInteractiveRequest();

        } catch (RemoteException e) {
            throw new ClientException(ErrorStrings.BROKER_BIND_SERVICE_FAILED,
                    "Exception occurred while attempting to invoke remote service",
                    e);
        } catch (Exception e) {
            throw new ClientException(ErrorStrings.BROKER_BIND_SERVICE_FAILED,
                    "Exception occurred while awaiting (get) return of MicrosoftAuthService",
                    e);
        } finally {
            client.disconnect();
        }

        return interactiveRequestIntent;
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
        mBrokerResultFuture.setResultBundle(data.getExtras());
    }

    @Override
    public AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters parameters) throws BaseException {
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
            final MsalBrokerRequestAdapter msalBrokerRequestAdapter = new MsalBrokerRequestAdapter();

            final Bundle requestBundle = new Bundle();
            final BrokerRequest brokerRequest = msalBrokerRequestAdapter.
                    brokerRequestFromSilentOperationParameters(parameters);

            requestBundle.putSerializable(AuthenticationConstants.Broker.BROKER_REQUEST_V2, brokerRequest);

            final Bundle resultBundle = service.acquireTokenSilently(requestBundle);

            return getAcquireTokenResult(resultBundle);

        } catch (RemoteException e) {
            throw new ClientException(
                    ErrorStrings.BROKER_BIND_SERVICE_FAILED,
                    "Exception occurred while attempting to invoke remote service",
                    e
            );
        } finally {
            client.disconnect();
        }
    }

    private AcquireTokenResult getAcquireTokenResult(final Bundle resultBundle) throws BaseException {

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
     * This method might be called on an UI thread, since we connect to broker,
     * this needs to be called on background thread.
     */
    public void getBrokerAccounts(final PublicClientApplicationConfiguration configuration,
                                  final PublicClientApplication.BrokerAccountsLoadedCallback callback) {

        final String methodName = ":getBrokerAccounts";
        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                IMicrosoftAuthService service;
                final MicrosoftAuthClient client = new MicrosoftAuthClient(configuration.getAppContext());
                try {
                    final MicrosoftAuthServiceFuture authServiceFuture = client.connect();

                    service = authServiceFuture.get();
                    final Bundle requestBundle = getRequestBundleForGetAccounts(configuration);

                    final List<AccountRecord> accountRecords =
                            MsalBrokerResultAdapter
                                    .getAccountRecordListFromBundle(
                                            service.getAccounts(requestBundle)
                                    );


                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onAccountsLoaded(accountRecords);
                        }
                    });
                } catch (final ClientException | InterruptedException | ExecutionException | RemoteException e) {
                    com.microsoft.identity.common.internal.logging.Logger.error(
                            TAG + methodName,
                            "Exception is thrown when trying to get account from Broker, returning empty list."
                                    + e.getMessage(),
                            ErrorStrings.IO_ERROR,
                            e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onAccountsLoaded(new ArrayList<AccountRecord>());
                        }
                    });
                } finally {
                    client.disconnect();
                }
            }
        }).start();

    }

    private Bundle getRequestBundleForGetAccounts(@NonNull PublicClientApplicationConfiguration configuration) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, configuration.getClientId());
        requestBundle.putString(ACCOUNT_REDIRECT, configuration.getRedirectUri());
        //Disable the environment and tenantID. Just return all accounts belong to this clientID.
        return requestBundle;
    }

    public void removeBrokerAccount(@Nullable final IAccount account,
                                    @NonNull final PublicClientApplicationConfiguration configuration,
                                    @NonNull final PublicClientApplication.AccountsRemovedCallback callback) {
        sBackgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                IMicrosoftAuthService service;
                final MicrosoftAuthClient client = new MicrosoftAuthClient(configuration.getAppContext());

                try {
                    final MicrosoftAuthServiceFuture authServiceFuture = client.connect();

                    service = authServiceFuture.get();

                    Bundle requestBundle = getRequestBundleForRemoveAccount(account, configuration);
                    Bundle resultBundle = service.removeAccount(requestBundle);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onAccountsRemoved(true);
                        }
                    });
                } catch (final BaseException | InterruptedException | ExecutionException | RemoteException e) {
                    //TODO Need to discuss whether to this exception back to AuthenticationCallback
                    com.microsoft.identity.common.internal.logging.Logger.error(
                            TAG,
                            "Exception is thrown when trying to get target account."
                                    + e.getMessage(),
                            ErrorStrings.IO_ERROR,
                            e);
                } finally {
                    client.disconnect();
                }
            }
        });
    }

    private Bundle getRequestBundleForRemoveAccount(@Nullable final IAccount account,
                                                    @NonNull PublicClientApplicationConfiguration configuration) {
        final Bundle requestBundle = new Bundle();
        requestBundle.putString(ACCOUNT_CLIENTID_KEY, configuration.getClientId());
        if (null != account) {
            requestBundle.putString(ACCOUNT_ENVIRONMENT_KEY, account.getEnvironment());
            requestBundle.putString(ACCOUNT_LOGIN_HINT, account.getUsername());
        }

        return requestBundle;
    }
}
