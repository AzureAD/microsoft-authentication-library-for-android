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

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PoPAuthenticationScheme;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.java.exception.ClientException;

import java.util.ArrayList;
import java.util.List;

public class SingleAccountModeWrapper extends MsalWrapper {

    private ISingleAccountPublicClientApplication mApp;

    public SingleAccountModeWrapper(ISingleAccountPublicClientApplication app) {
        mApp = app;
    }

    @Override
    public String getMode() {
        if (mApp.isSharedDevice()) {
            return "Single Account - Shared device";
        }

        return "Single Account - Non-shared device";
    }

    @Override
    public IPublicClientApplication getApp() {
        return mApp;
    }

    @Override
    public String getDefaultBrowser() {
        try {
            return BrowserSelector.select(mApp.getConfiguration().getAppContext(),
                    mApp.getConfiguration().getBrowserSafeList(),
                    mApp.getConfiguration().getPreferredBrowser()).getPackageName();
        } catch (ClientException e) {
            return "Unknown";
        }
    }

    @Override
    public void loadAccounts(final @NonNull INotifyOperationResultCallback<List<IAccount>> callback) {
        mApp.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                List<IAccount> accountList = new ArrayList<>();

                if (activeAccount != null) {
                    accountList.add(activeAccount);
                }

                callback.onSuccess(accountList);
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                callback.showMessage(
                        "signed-in account is changed from "
                                + (null == priorAccount ? "null" : priorAccount.getUsername())
                                + " to "
                                + (null == currentAccount ? "null" : currentAccount.getUsername()));
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
        mApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                callback.showMessage("The account is successfully signed out.");
                callback.onSuccess(null);
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                callback.showMessage("Failed to sign out: " + exception.getMessage());
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
    String getActiveBrokerPkgName(@NonNull Activity activity) {
        return ((PublicClientApplication) mApp).getActiveBrokerPackageName(activity.getApplicationContext());
    }

    @Override
    void acquireTokenWithDeviceCodeFlowInternal(@NonNull List<String> scopes,
                                                @Nullable final ClaimsRequest claimsRequest,
                                                @NonNull final IPublicClientApplication.DeviceCodeFlowCallback callback) {
        mApp.acquireTokenWithDeviceCode(scopes, callback, claimsRequest, null);
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
