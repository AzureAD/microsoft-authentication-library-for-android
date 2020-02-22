package com.microsoft.identity.client.testapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;

import java.util.ArrayList;
import java.util.List;

public class SingleAccountModeWrapper extends MsalWrapper {

    private ISingleAccountPublicClientApplication mApp;

    public SingleAccountModeWrapper(ISingleAccountPublicClientApplication app){
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
    public String getDefaultBrowser() {
        try {
            return BrowserSelector.select(mApp.getConfiguration().getAppContext(), mApp.getConfiguration().getBrowserSafeList()).getPackageName();
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
}
