package com.microsoft.identity.client.testapp;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;

import java.util.List;

public class MultipleAccountModeWrapper extends MsalWrapper {

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
}
