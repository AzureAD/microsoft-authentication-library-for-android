//   Copyright (c) Microsoft Corporation.
//   All rights reserved.
//
//   This code is licensed under the MIT License.
//
//   Permission is hereby granted, free of charge, to any person obtaining a copy
//   of this software and associated documentation files(the "Software"), to deal
//   in the Software without restriction, including without limitation the rights
//   to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//   copies of the Software, and to permit persons to whom the Software is
//   furnished to do so, subject to the following conditions :
//
//   The above copyright notice and this permission notice shall be included in
//   all copies or substantial portions of the Software.
//
//   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//   THE SOFTWARE.
package com.microsoft.identity.client.testapp;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PoPAuthenticationScheme;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.java.exception.ClientException;

import java.util.List;

public class MultipleAccountModeWrapper extends MsalWrapper {

    private static final String LOG_TAG = MultipleAccountModeWrapper.class.getSimpleName();

    private IMultipleAccountPublicClientApplication mApp;

    public MultipleAccountModeWrapper(IMultipleAccountPublicClientApplication app) {
        mApp = app;
    }

    @Override
    public String getMode() {
        return "Multiple Account";
    }

    @Override
    public String getDefaultBrowser() {
        try {
            return BrowserSelector.select(mApp.getConfiguration().getAppContext(), mApp.getConfiguration().getBrowserSafeList()).getPackageName();
        } catch (ClientException e) {
            return "Unknown";
        }
    }

    @Override
    public void loadAccounts(@NonNull final INotifyOperationResultCallback<List<IAccount>> callback) {
        mApp.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
            @Override
            public void onTaskCompleted(List<IAccount> result) {
                callback.onSuccess(result);
            }

            @Override
            public void onError(MsalException exception) {
                callback.showMessage(
                        "Failed to load account from broker. "
                                + "Error code: " + exception.getErrorCode()
                                + " Error Message: " + exception.getMessage()
                );
            }
        });
    }

    @Override
    public void removeAccount(@NonNull IAccount account,
                              @NonNull final INotifyOperationResultCallback<Void> callback) {
        mApp.removeAccount(
                account,
                new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                    @Override
                    public void onRemoved() {
                        callback.showMessage("The account is successfully removed.");
                        callback.onSuccess(null);
                    }

                    @Override
                    public void onError(@NonNull MsalException exception) {
                        callback.showMessage("Failed to remove the account.");
                    }
                });
    }

    @Override
    void acquireTokenAsyncInternal(@NonNull AcquireTokenParameters parameters) {
        mApp.acquireToken(parameters);
    }

    @Override
    void acquireTokenSilentAsyncInternal(@NonNull AcquireTokenSilentParameters parameters) {
        mApp.acquireTokenSilentAsync(parameters);
    }

    @Override
    void acquireTokenWithDeviceCodeFlowInternal(@NonNull List<String> scopes,
                                                @NonNull final IPublicClientApplication.DeviceCodeFlowCallback callback) {
        mApp.acquireTokenWithDeviceCode(scopes, callback, null);
    }

    @Override
    public void generateSignedHttpRequestInternal(@NonNull final IAccount account,
                                                  @NonNull final PoPAuthenticationScheme params,
                                                  @NonNull final INotifyOperationResultCallback<String> generateShrCallback) {
        mApp.generateSignedHttpRequest(
                account,
                params,
                new IPublicClientApplication.SignedHttpRequestRequestCallback() {
                    @Override
                    public void onTaskCompleted(String result) {
                        generateShrCallback.onSuccess(result);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        generateShrCallback.showMessage(exception.getMessage());
                    }
                }
        );
    }
}
