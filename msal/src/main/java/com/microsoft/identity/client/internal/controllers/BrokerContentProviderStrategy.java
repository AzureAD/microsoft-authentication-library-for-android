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
import android.util.Base64;

import com.microsoft.identity.client.exception.BrokerCommunicationException;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
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
import com.microsoft.identity.common.internal.util.ParcelableUtil;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.ACQUIRE_TOKEN_INTERACTIVE_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.ACQUIRE_TOKEN_SILENT_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.AUTHORITY;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.CONTENT_SCHEME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.GET_ACCOUNTS_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.GET_CURRENT_ACCOUNT_SHARED_DEVICE_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.GET_DEVICE_MODE_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.HELLO_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.REMOVE_ACCOUNTS_PATH;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.SIGN_OUT_FROM_SHARED_DEVICE_PATH;

/**
 * Class to invoke active broker's BrokerContentProvider to perform Broker operations from MSAL.
 * Implements {@link BrokerBaseStrategy}
 * Defines an interface ContentProviderOperation to perform this operation.
 */
public class BrokerContentProviderStrategy extends BrokerBaseStrategy {

    private static final String TAG = BrokerContentProviderStrategy.class.getName();

    public interface ContentProviderOperation<T extends CommandParameters, U> {

        /**
         * Constructs and returns a data string for the request to the broker from the CommandParameters.
         */
        @Nullable
        Bundle getRequestBundle(T parameters);

        /**
         * Name of task for logging and telemetry purposes
         */
        @NonNull
        String getMethodName();

        /**
         * Uri path for specific broker operation
         */
        @NonNull
        String getUriPath();

        /**
         * Constructs and returns the result from the request bundle returned by the broker.
         */
        @NonNull
        U getResultFromBundle(Bundle resultBundle) throws BaseException;
    }

    private <T extends CommandParameters, U> U performContentProviderOperation(
            @NonNull final T parameters,
            @NonNull final ContentProviderOperation<T, U> contentProviderOperation) throws BaseException {

        final String methodName = contentProviderOperation.getMethodName();

        Telemetry.emit(
                new BrokerStartEvent()
                        .putAction(methodName)
                        .putStrategy(TelemetryEventStrings.Value.CONTENT_PROVIDER)
        );
        final Uri uri = getContentProviderURI(
                parameters.getAndroidApplicationContext(),
                contentProviderOperation.getUriPath()
        );
        Logger.info(TAG + methodName, "Request to BrokerContentProvider for uri path " +
                contentProviderOperation.getUriPath()
        );

        String marshalledRequestString = null;

        final Bundle requestBundle = contentProviderOperation.getRequestBundle(parameters);
        if (requestBundle != null) {
            byte[] marshalledBytes = ParcelableUtil.marshall(requestBundle);
            marshalledRequestString = Base64.encodeToString(marshalledBytes, 0);
        }

        final Cursor cursor = parameters.getAndroidApplicationContext().getContentResolver().query(
                uri,
                null,
                marshalledRequestString,
                null,
                null
        );

        if (cursor != null) {
            final U result = contentProviderOperation.getResultFromBundle(cursor.getExtras());
            cursor.close();
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(true)
            );
            Logger.info(TAG + methodName, "Received successful result from broker");
            return result;
        } else {
            final String message = "Failed to get result from Broker Content Provider, cursor is null";
            Logger.error(TAG + methodName, message, null);
            Telemetry.emit(
                    new BrokerEndEvent()
                            .putAction(methodName)
                            .isSuccessful(false)
                            .putErrorDescription(message)
            );
            throw new BrokerCommunicationException(message, null);
        }
    }

    @Override
    String hello(@NonNull CommandParameters parameters) throws BaseException {
        final String methodName = "helloWithContentProvider";

        return performContentProviderOperation(parameters, new ContentProviderOperation<CommandParameters, String>() {
            @Override
            public Bundle getRequestBundle(CommandParameters parameters) {
                return mRequestAdapter.getRequestBundleForHello(parameters);
            }

            @Override
            public String getMethodName() {
                return methodName;
            }

            @Override
            public String getUriPath() {
                return HELLO_PATH;
            }

            @Override
            public String getResultFromBundle(Bundle resultBundle) throws BaseException {
                return mResultAdapter.verifyHelloFromResultBundle(resultBundle);
            }
        });
    }

    @Override
    Intent getBrokerAuthorizationIntent(@NonNull final InteractiveTokenCommandParameters parameters,
                                        @Nullable final String negotiatedBrokerProtocolVersion)
            throws BaseException {
        final String methodName = "getBrokerAuthorizationIntentForContentProvider";

        return performContentProviderOperation(
                parameters,
                new ContentProviderOperation<InteractiveTokenCommandParameters, Intent>() {

                    @Override
                    public Bundle getRequestBundle(InteractiveTokenCommandParameters parameters) {
                        return null; // broker returns us an intent based on calling uid , no request string needed
                    }

                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @Override
                    public String getUriPath() {
                        return ACQUIRE_TOKEN_INTERACTIVE_PATH;
                    }

                    @Override
                    public Intent getResultFromBundle(Bundle resultBundle) throws BaseException {
                        return mRequestAdapter.getRequestIntentForAcquireTokenInteractive(
                                resultBundle,
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                    }
                });
    }

    @Override
    AcquireTokenResult acquireTokenSilent(@NonNull final SilentTokenCommandParameters parameters,
                                          @Nullable final String negotiatedBrokerProtocolVersion)
            throws BaseException {
        final String methodName = "acquireTokenSilentWithContentProvider";

        return performContentProviderOperation(
                parameters,
                new ContentProviderOperation<SilentTokenCommandParameters, AcquireTokenResult>() {
                    @Nullable
                    @Override
                    public Bundle getRequestBundle(SilentTokenCommandParameters parameters) {
                        return mRequestAdapter.getRequestBundleForAcquireTokenSilent(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                    }

                    @NonNull
                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @NonNull
                    @Override
                    public String getUriPath() {
                        return ACQUIRE_TOKEN_SILENT_PATH;
                    }

                    @NonNull
                    @Override
                    public AcquireTokenResult getResultFromBundle(Bundle resultBundle) throws BaseException {
                        return mResultAdapter.getAcquireTokenResultFromResultBundle(resultBundle);
                    }
                });
    }

    @Override
    List<ICacheRecord> getBrokerAccounts(@NonNull final CommandParameters parameters,
                                         @Nullable final String negotiatedBrokerProtocolVersion) throws BaseException {
        final String methodName = "getBrokerAccountsWithContentProvider";
        return performContentProviderOperation(
                parameters,
                new ContentProviderOperation<CommandParameters, List<ICacheRecord>>() {
                    @Nullable
                    @Override
                    public Bundle getRequestBundle(CommandParameters parameters) {
                        return mRequestAdapter.getRequestBundleForGetAccounts(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                    }

                    @NonNull
                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @NonNull
                    @Override
                    public String getUriPath() {
                        return GET_ACCOUNTS_PATH;
                    }

                    @NonNull
                    @Override
                    public List<ICacheRecord> getResultFromBundle(Bundle resultBundle) throws BaseException {
                        return mResultAdapter.getAccountsFromResultBundle(resultBundle);
                    }
                });
    }

    @Override
    void removeBrokerAccount(@NonNull final RemoveAccountCommandParameters parameters,
                             @Nullable final String negotiatedBrokerProtocolVersion) throws BaseException {

        final String methodName = "removeBrokerAccountWithContentProvider";

        performContentProviderOperation(
                parameters,
                new ContentProviderOperation<RemoveAccountCommandParameters, Void>() {

                    @Nullable
                    @Override
                    public Bundle getRequestBundle(RemoveAccountCommandParameters parameters) {
                        return mRequestAdapter.getRequestBundleForRemoveAccount(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                    }

                    @NonNull
                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @NonNull
                    @Override
                    public String getUriPath() {
                        return REMOVE_ACCOUNTS_PATH;
                    }

                    @NonNull
                    @Override
                    public Void getResultFromBundle(Bundle resultBundle) throws BaseException {
                        mResultAdapter.verifyRemoveAccountResultFromBundle(resultBundle);
                        return null;
                    }
                });
    }

    @Override
    boolean getDeviceMode(@NonNull final CommandParameters parameters,
                          @Nullable final String negotiatedBrokerProtocolVersion) throws BaseException {
        final String methodName = "getDeviceModeWithContentProvider";
        return performContentProviderOperation(
                parameters,
                new ContentProviderOperation<CommandParameters, Boolean>() {

                    @Nullable
                    @Override
                    public Bundle getRequestBundle(CommandParameters parameters) {
                        return null; // broker returns a boolean to indicate the device mode, no request string needed.
                    }

                    @NonNull
                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @NonNull
                    @Override
                    public String getUriPath() {
                        return GET_DEVICE_MODE_PATH;
                    }

                    @NonNull
                    @Override
                    public Boolean getResultFromBundle(Bundle resultBundle) throws BaseException {
                        return mResultAdapter.getDeviceModeFromResultBundle(resultBundle);
                    }
                });
    }

    @Override
    List<ICacheRecord> getCurrentAccountInSharedDevice(@NonNull final CommandParameters parameters,
                                                       @Nullable final String negotiatedBrokerProtocolVersion)
            throws BaseException {
        final String method = "getCurrentAccountInSharedDeviceWithContentProvider";
        return performContentProviderOperation(
                parameters,
                new ContentProviderOperation<CommandParameters, List<ICacheRecord>>() {

                    @Nullable
                    @Override
                    public Bundle getRequestBundle(CommandParameters parameters) {
                        return mRequestAdapter.getRequestBundleForGetAccounts(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                    }

                    @NonNull
                    @Override
                    public String getMethodName() {
                        return method;
                    }

                    @NonNull
                    @Override
                    public String getUriPath() {
                        return GET_CURRENT_ACCOUNT_SHARED_DEVICE_PATH;
                    }

                    @NonNull
                    @Override
                    public List<ICacheRecord> getResultFromBundle(Bundle resultBundle) throws BaseException {
                        return mResultAdapter.getAccountsFromResultBundle(resultBundle);
                    }
                });
    }

    @Override
    void signOutFromSharedDevice(@NonNull final RemoveAccountCommandParameters parameters,
                                 @Nullable final String negotiatedBrokerProtocolVersion) throws BaseException {
        final String methodName = "signOutFromSharedDeviceWithContentProvider";
        performContentProviderOperation(
                parameters,
                new ContentProviderOperation<RemoveAccountCommandParameters, Void>() {

                    @Nullable
                    @Override
                    public Bundle getRequestBundle(RemoveAccountCommandParameters parameters) {
                        return mRequestAdapter.getRequestBundleForRemoveAccountFromSharedDevice(
                                parameters,
                                negotiatedBrokerProtocolVersion
                        );
                    }

                    @NonNull
                    @Override
                    public String getMethodName() {
                        return methodName;
                    }

                    @NonNull
                    @Override
                    public String getUriPath() {
                        return SIGN_OUT_FROM_SHARED_DEVICE_PATH;
                    }

                    @NonNull
                    @Override
                    public Void getResultFromBundle(Bundle resultBundle) throws BaseException {
                        mResultAdapter.verifyRemoveAccountResultFromBundle(resultBundle);
                        return null;
                    }
                });
    }

    private Uri getContentProviderURI(@NonNull final Context context, @NonNull final String path) {
        final BrokerValidator brokerValidator = new BrokerValidator(context);
        final String activeBrokerPackage = brokerValidator.getCurrentActiveBrokerPackageName();
        final String authority = activeBrokerPackage + "." + AUTHORITY;
        return Uri.parse(CONTENT_SCHEME + authority + path);
    }
}
