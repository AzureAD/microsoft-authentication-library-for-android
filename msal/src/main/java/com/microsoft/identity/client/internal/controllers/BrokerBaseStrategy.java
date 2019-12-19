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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.JsonExtensions;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.MsalBrokerRequestAdapter;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.MsalBrokerResultAdapter;
import com.microsoft.identity.common.internal.ui.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_CLIENTID_KEY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_HOME_ACCOUNT_ID;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ACCOUNT_REDIRECT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACCOUNTS;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_DEVICE_MODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.DEFAULT_BROWSER_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.ENVIRONMENT;

abstract class BrokerBaseStrategy {
    protected final MsalBrokerRequestAdapter mRequestAdapter = new MsalBrokerRequestAdapter();

    protected final MsalBrokerResultAdapter mResultAdapter = new MsalBrokerResultAdapter();

    abstract boolean hello(@NonNull final OperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException;

    abstract Intent getBrokerAuthorizationIntent(@NonNull AcquireTokenOperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException;

    abstract AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException;

    abstract List<ICacheRecord> getBrokerAccounts(@NonNull final OperationParameters parameters) throws InterruptedException, ExecutionException, RemoteException, OperationCanceledException, IOException, AuthenticatorException, BaseException;

    abstract void removeBrokerAccount(@NonNull final OperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException;

    abstract boolean getDeviceMode(@NonNull final OperationParameters parameters)throws BaseException, InterruptedException, ExecutionException, RemoteException;

    abstract List<ICacheRecord> getCurrentAccountInSharedDevice(@NonNull final OperationParameters parameters) throws InterruptedException, ExecutionException, RemoteException, OperationCanceledException, IOException, AuthenticatorException, BaseException;

    abstract void signOutFromSharedDevice(@NonNull final OperationParameters parameters) throws BaseException, InterruptedException, ExecutionException, RemoteException;

    Handler getPreferredHandler() {
        if (null != Looper.myLooper() && Looper.getMainLooper() != Looper.myLooper()) {
            return new Handler(Looper.myLooper());
        } else {
            return new Handler(Looper.getMainLooper());
        }
    }

    protected Intent completeInteractiveRequestIntent(@NonNull final Intent interactiveRequestIntent,
                                                      @NonNull final AcquireTokenOperationParameters parameters){
        interactiveRequestIntent.putExtra(
                AuthenticationConstants.Broker.BROKER_REQUEST_V2,
                new Gson().toJson(
                        mRequestAdapter.brokerRequestFromAcquireTokenParameters(parameters),
                        BrokerRequest.class)
        );

        return interactiveRequestIntent;
    }
}
