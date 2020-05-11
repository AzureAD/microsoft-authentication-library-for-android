// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.internal.controllers;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.google.gson.Gson;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.broker.BrokerRequest;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACTIVITY_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.ACQUIRE_TOKEN_INTERACTIVE_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.ACQUIRE_TOKEN_SILENT_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.AUTHORITY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.CONTENT_SCHEME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.GET_ACCOUNTS_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.GET_CURRENT_ACCOUNT_SHARED_DEVICe_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.GET_DEVICE_MODE_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.HELLO_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.REMOVE_ACCOUNTS_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.SIGN_OUT_FROM_SHARED_DEVICE_PATH;

public class BrokerContentProviderStrategy extends BrokerBaseStrategy {

    @Override
    String hello(@NonNull CommandParameters parameters) throws BaseException {
        final String helloString = mRequestAdapter.getRequestStringForHello(parameters);
        final Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                getContentProviderURI(parameters.getAndroidApplicationContext(), HELLO_PATH),
                null,
                helloString,
                null,
                null
        );
        String negotiatedBrokerProtocolVersion = null;

        if(cursor != null) {
            Bundle resultBundle = cursor.getExtras();
            negotiatedBrokerProtocolVersion = mResultAdapter.verifyHelloFromResultBundle(resultBundle);
            cursor.close();
        }
        return negotiatedBrokerProtocolVersion;
    }

    @Override
    Intent getBrokerAuthorizationIntent(@NonNull InteractiveTokenCommandParameters parameters,
                                        @Nullable String negotiatedBrokerProtocolVersion) throws BaseException {
        final Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                getContentProviderURI(parameters.getAndroidApplicationContext(), ACQUIRE_TOKEN_INTERACTIVE_PATH),
                null,
                null,
                null,
                null
                );
        final Bundle requestBundle = mRequestAdapter.getRequestBundleForAcquireTokenInteractive(
                parameters,
                negotiatedBrokerProtocolVersion
        );
        Intent interactiveRequestIntent = new Intent();
        if(cursor != null) {
            Bundle resultBundle = cursor.getExtras();
            interactiveRequestIntent.putExtras(requestBundle);
            interactiveRequestIntent.putExtras(resultBundle);
            interactiveRequestIntent.setPackage(resultBundle.getString(BROKER_PACKAGE_NAME));
            interactiveRequestIntent.setClassName(
                    resultBundle.getString(BROKER_PACKAGE_NAME, ""),
                    resultBundle.getString(BROKER_ACTIVITY_NAME, "")
            );
            interactiveRequestIntent.putExtra(
                    AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY,
                    negotiatedBrokerProtocolVersion
            );
            cursor.close();
        }
        return interactiveRequestIntent;
    }

    @Override
    AcquireTokenResult acquireTokenSilent(@NonNull SilentTokenCommandParameters parameters,
                                          @Nullable String negotiatedBrokerProtocolVersion) throws BaseException {
        final BrokerRequest brokerRequest = mRequestAdapter.brokerRequestFromSilentOperationParameters(parameters);
        final String requestString = new Gson().toJson(brokerRequest);
        Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                getContentProviderURI(parameters.getAndroidApplicationContext(), ACQUIRE_TOKEN_SILENT_PATH),

                new String[]{negotiatedBrokerProtocolVersion},
                requestString,
                null,
                null
        );
        AcquireTokenResult acquireTokenResult = null;

        if(cursor != null){
            final Bundle resultBundle = cursor.getExtras();
            acquireTokenResult = mResultAdapter.getAcquireTokenResultFromResultBundle(resultBundle);
            cursor.close();
        }
        return acquireTokenResult;

    }

    @Override
    List<ICacheRecord> getBrokerAccounts(@NonNull CommandParameters parameters,
                                         @Nullable String negotiatedBrokerProtocolVersion) throws BaseException {
        final String requestString = mRequestAdapter.getRequestStringForGetAccounts(parameters);

        Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                getContentProviderURI(parameters.getAndroidApplicationContext(), GET_ACCOUNTS_PATH),

                new String[]{negotiatedBrokerProtocolVersion},
                requestString,
                null,
                null
        );
        List<ICacheRecord> recordList = new ArrayList<>();
        // TODO : What if cursor is null, log?
        if(cursor !=null){
            final Bundle resultBundle = cursor.getExtras();
            recordList = mResultAdapter.getAccountsFromResultBundle(resultBundle);
            cursor.close();
        }
        return  recordList;

    }

    @Override
    void removeBrokerAccount(@NonNull RemoveAccountCommandParameters parameters,
                             @Nullable String negotiatedBrokerProtocolVersion) throws BaseException {
        final String requestString = mRequestAdapter.getRequestStringForRemoveAccount(parameters);

        Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                getContentProviderURI(parameters.getAndroidApplicationContext(), REMOVE_ACCOUNTS_PATH),
                new String[]{negotiatedBrokerProtocolVersion},
                requestString,
                null,
                null
        );
        if(cursor != null){
            final Bundle resultBundle = cursor.getExtras();
            mResultAdapter.verifyRemoveAccountResultFromBundle(resultBundle);
        }
    }

    @Override
    boolean getDeviceMode(@NonNull CommandParameters parameters,
                          @Nullable String negotiatedBrokerProtocolVersion) throws BaseException {

        boolean isSharedDevice = false;
        final Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                getContentProviderURI(parameters.getAndroidApplicationContext(), GET_DEVICE_MODE_PATH),
                new String[]{negotiatedBrokerProtocolVersion},
                "",
                null,
                null
        );
        if(cursor !=null){
            final Bundle resultBundle = cursor.getExtras();
            isSharedDevice = mResultAdapter.getDeviceModeFromResultBundle(resultBundle);
            cursor.close();
        }
        return isSharedDevice;
    }

    @Override
    List<ICacheRecord> getCurrentAccountInSharedDevice(@NonNull CommandParameters parameters,
                                                       @Nullable String negotiatedBrokerProtocolVersion) throws BaseException {

        final String requestString = mRequestAdapter.getRequestStringForGetAccounts(parameters);

        final Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                getContentProviderURI(parameters.getAndroidApplicationContext(), GET_CURRENT_ACCOUNT_SHARED_DEVICe_PATH),
                new String[]{negotiatedBrokerProtocolVersion},
                requestString,
                null,
                null
        );
        List<ICacheRecord> recordList = new ArrayList<>();
        // TODO : What if cursor is null, log?
        if(cursor !=null){
            final Bundle resultBundle = cursor.getExtras();
            recordList = mResultAdapter.getAccountsFromResultBundle(resultBundle);
            cursor.close();
        }
        return  recordList;
    }

    @Override
    void signOutFromSharedDevice(@NonNull RemoveAccountCommandParameters parameters,
                                 @Nullable String negotiatedBrokerProtocolVersion) throws BaseException {
        final String requestString = mRequestAdapter.getRequestStringForSharedDeviceSignOut(parameters);
        final Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                getContentProviderURI(parameters.getAndroidApplicationContext(), SIGN_OUT_FROM_SHARED_DEVICE_PATH),
                new String[]{negotiatedBrokerProtocolVersion},
                requestString,
                null,
                null
        );
        if(cursor !=null){
            final Bundle resultBundle = cursor.getExtras();
            mResultAdapter.verifyRemoveAccountResultFromBundle(resultBundle);
            cursor.close();
        }
    }

    private Uri getContentProviderURI(@NonNull final Context context, @NonNull final String path){
        final BrokerValidator brokerValidator = new BrokerValidator(context);
        final String activeBrokerPackage = brokerValidator.getCurrentActiveBrokerPackageName(context);
        final String authority = activeBrokerPackage + "." + AUTHORITY;
        return Uri.parse(CONTENT_SCHEME + authority + path);
    }
}
