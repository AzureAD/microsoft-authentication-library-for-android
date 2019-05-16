package com.microsoft.identity.client.testapp;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.UiBehavior;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MsalWrapper {
    private static String PostMsalApplicationLoadedKey = "MsalWrapper_PostMsalApplicationLoaded";

    private static MsalWrapper mSharedInstance;

    public static MsalWrapper getInstance(){
        if (mSharedInstance == null){
            mSharedInstance = new MsalWrapper();
        }

        return mSharedInstance;
    }

    /// Interface of an object to be invoked once the account is successfully loaded. ie. Update UI.
    interface IPostAccountLoaded{
        void onLoaded(List<IAccount> loadedAccount);
    }

    /// Interface of an object to be invoked once MSAL application is successfully initialized.
    interface IMsalApplicationLoaded{
        void onApplicationLoaded();
    }

    /// Acting as a bridge between the result of MsalWrapper's results and the outside world.
    interface INotifyOperationResultCallback{
        void acquireTokenSucceed(IAuthenticationResult result);
        void notify(String message);
    }

    private IPublicClientApplication mApplication;
    private HashMap<String, IPostAccountLoaded> postAccountLoadedJobs = new HashMap<>();
    private List<IAccount> mLoadedAccount;

    public void loadMsalApplication(@NonNull final Context context,
                                    @NonNull final int configFileResourceId,
                                    @NonNull final INotifyOperationResultCallback notifyCallback,
                                    @Nullable final IMsalApplicationLoaded msalApplicationLoaded){
        PublicClientApplication.create(context,
            configFileResourceId,
            new PublicClientApplication.ApplicationCreatedListener() {
                @Override
                public void onCreated(IPublicClientApplication application) {
                    mApplication = application;

                    if (msalApplicationLoaded != null) {
                        registerPostAccountLoadedJob(PostMsalApplicationLoadedKey, new IPostAccountLoaded() {
                            @Override
                            public void onLoaded(List<IAccount> loadedAccount) {
                                msalApplicationLoaded.onApplicationLoaded();
                                deregisterPostAccountLoadedJob(PostMsalApplicationLoadedKey);
                            }
                        });
                    }

                    loadAccountFromBroker(notifyCallback);
                }

                @Override
                public void onError(MsalException exception) {
                    notifyCallback.notify("Failed to load MSAL Application: " + exception.getMessage());
                }
            });
    }

    public void onResume(@NonNull final INotifyOperationResultCallback notifyCallback){
        if (mApplication == null){
            return;
        }

        if (mApplication instanceof ISingleAccountPublicClientApplication){
            // Single account mode caller is responsible for calling this onResume.
            // This is because the account might be modified by other apps when this app is in background.
            loadAccountFromBroker(notifyCallback);
        }
    }

    public void registerPostAccountLoadedJob(String key, IPostAccountLoaded job){
        postAccountLoadedJobs.put(key, job);

        if (mLoadedAccount != null) {
            job.onLoaded(mLoadedAccount);
        }
    }

    public void deregisterPostAccountLoadedJob(String key){
        postAccountLoadedJobs.remove(key);
    }

    public String getPublicApplicationMode(){
        if (mApplication == null){
            // Application is not successfully loaded yet.
            return "Not loaded";
        }

        if (mApplication instanceof ISingleAccountPublicClientApplication){
            return "Single Account (Shared device)";
        }

        return "Multiple Account";
    }

    public void acquireToken(final Activity activity,
                             final AcquireTokenFragment.RequestOptions requestOptions,
                             @NonNull final INotifyOperationResultCallback notifyCallback) {
        if (mApplication == null){
            notifyCallback.notify("Application is not yet loaded");
            return;
        }
        if (mLoadedAccount == null){
            notifyCallback.notify("account is not yet loaded");
            return;
        }

        mApplication.acquireToken(
            activity,
            requestOptions.getScopes().toLowerCase().split(" "),
            requestOptions.getLoginHint(),
            requestOptions.getUiBehavior(),
            null,
            requestOptions.getExtraScopesToConsent() == null ? null : requestOptions.getExtraScopesToConsent().toLowerCase().split(" "),
            null,
            getAuthenticationCallback(notifyCallback));
    }

    public void acquireTokenSilent(final AcquireTokenFragment.RequestOptions requestOptions,
                                   @NonNull final INotifyOperationResultCallback notifyCallback){
        if (mApplication == null){
            notifyCallback.notify("Application is not yet loaded");
            return;
        }
        if (mLoadedAccount == null){
            notifyCallback.notify("account is not yet loaded");
            return;
        }

        IAccount requestAccount = null;
        for (final IAccount account : mLoadedAccount) {
            if (account.getUsername().equalsIgnoreCase(requestOptions.getLoginHint().trim())) {
                requestAccount = account;
                break;
            }
        }

        if (null != requestAccount) {
            mApplication.acquireTokenSilentAsync(
                requestOptions.getScopes().toLowerCase().split(" "),
                requestAccount,
                null,
                requestOptions.forceRefresh(),
                getAuthenticationCallback(notifyCallback));
        } else {
            notifyCallback.notify("No account found matching loginHint");
        }
    }

    private AuthenticationCallback getAuthenticationCallback(@NonNull final INotifyOperationResultCallback notifyCallback) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                loadAccountFromBroker(notifyCallback);
                notifyCallback.acquireTokenSucceed(authenticationResult);
            }

            @Override
            public void onError(MsalException exception) {
                // Check the exception type.
                if (exception instanceof MsalClientException) {
                    // This means errors happened in the sdk itself, could be network, Json parse, etc. Check MsalError.java
                    // for detailed list of the errors.
                    notifyCallback.notify(exception.getMessage());
                } else if (exception instanceof MsalServiceException) {
                    // This means something is wrong when the sdk is communication to the service, mostly likely it's the client
                    // configuration.
                    notifyCallback.notify(exception.getMessage());
                } else if (exception instanceof MsalArgumentException) {
                    notifyCallback.notify(exception.getMessage());
                } else if (exception instanceof MsalUiRequiredException) {
                    // This explicitly indicates that developer needs to prompt the user, it could be refresh token is expired, revoked
                    // or user changes the password; or it could be that no token was found in the token cache.
                    notifyCallback.notify(exception.getMessage());
                }
            }

            @Override
            public void onCancel() {
                notifyCallback.notify("User cancelled the flow.");
            }
        };
    }

    public void removeAccount(@NonNull final String username,
                              @NonNull final INotifyOperationResultCallback notifyCallback){
        if (mApplication == null){
            notifyCallback.notify("Application is not yet loaded.");
            return;
        }
        if (mLoadedAccount == null){
            notifyCallback.notify("account is not yet loaded.");
            return;
        }

        if (mApplication instanceof IMultipleAccountPublicClientApplication){
            final IMultipleAccountPublicClientApplication application = (IMultipleAccountPublicClientApplication)(mApplication);
            for (final IAccount accountToRemove : mLoadedAccount) {
                if (TextUtils.isEmpty(username) || accountToRemove.getUsername().equalsIgnoreCase(username.trim())) {
                    application.removeAccount(
                        accountToRemove,
                        new IPublicClientApplication.AccountRemovedListener() {
                            @Override
                            public void onAccountRemoved(Boolean isSuccess) {
                                if (isSuccess) {
                                    notifyCallback.notify("The account is successfully removed.");

                                    // Reload account list.
                                    loadAccountFromBroker(notifyCallback);
                                } else {
                                    notifyCallback.notify("Failed to remove the account.");
                                }
                            }
                        });
                }
            }
        } else if (mApplication instanceof ISingleAccountPublicClientApplication) {
            final ISingleAccountPublicClientApplication application = (ISingleAccountPublicClientApplication)(mApplication);
            try {
                application.removeCurrentAccount(new IPublicClientApplication.AccountRemovedListener() {
                    @Override
                    public void onAccountRemoved(Boolean isSuccess) {
                        if (isSuccess) {
                            notifyCallback.notify("The account is successfully removed.");

                            // Reload account list.
                            loadAccountFromBroker(notifyCallback);
                        } else {
                            notifyCallback.notify("Account is not removed.");
                        }
                    }
                });
            } catch (MsalClientException e) {
                notifyCallback.notify(e.getMessage());
            }

        }
    }

    // Refresh the cached account list.
    // Once the operation is done, it'll invoke performPostAccountLoadedJobs() to notify every listeners.
    private void loadAccountFromBroker(@NonNull final INotifyOperationResultCallback notifyCallback){
        if (mApplication == null){
            notifyCallback.notify("Application is not yet loaded.");
            return;
        }

        if (mApplication instanceof IMultipleAccountPublicClientApplication) {
            IMultipleAccountPublicClientApplication multipleAcctApp = (IMultipleAccountPublicClientApplication) mApplication;
            multipleAcctApp.getAccounts(new IMultipleAccountPublicClientApplication.AccountsLoadedCallback() {
                @Override
                public void onAccountsLoaded(List<IAccount> accounts) {
                    mLoadedAccount = accounts;
                    performPostAccountLoadedJobs();
                }
            });
        } else if (mApplication instanceof ISingleAccountPublicClientApplication) {
            ISingleAccountPublicClientApplication singleAcctApp = (ISingleAccountPublicClientApplication)mApplication;
            try {
                singleAcctApp.getCurrentAccount(new ISingleAccountPublicClientApplication.CurrentAccountListener() {
                    @Override
                    public void onAccountLoaded(IAccount activeAccount) {
                        mLoadedAccount = new ArrayList<>();
                        if (activeAccount != null) {
                            mLoadedAccount.add(activeAccount);
                        }
                        performPostAccountLoadedJobs();
                    }

                    @Override
                    public void onAccountChanged(IAccount priorAccount, IAccount currentAccount) {
                        notifyCallback.notify("single account is changed from " +
                            (priorAccount == null ? "null" : priorAccount.getUsername()) +
                            " to " +
                            (currentAccount == null ? "null" : currentAccount.getUsername()));
                    }
                });
            } catch (MsalClientException e) {
                notifyCallback.notify(e.getMessage());
            }
        }
    }

    private void performPostAccountLoadedJobs(){
        for (String key: postAccountLoadedJobs.keySet()) {
            IPostAccountLoaded job = postAccountLoadedJobs.get(key);
            job.onLoaded(mLoadedAccount);
        }
    }
}
