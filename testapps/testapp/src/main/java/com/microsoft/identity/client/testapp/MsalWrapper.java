package com.microsoft.identity.client.testapp;

import android.app.Activity;
import android.content.Context;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalDeclinedScopeException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MsalWrapper {
    private static String PostMsalApplicationLoadedKey = "MsalWrapper_PostMsalApplicationLoaded";

    private static MsalWrapper mSharedInstance;

    public static MsalWrapper getInstance() {
        if (mSharedInstance == null) {
            mSharedInstance = new MsalWrapper();
        }

        return mSharedInstance;
    }

    /// Interface of an object to be invoked once the account is successfully loaded. ie. Update UI.
    interface IPostAccountLoaded {
        void onLoaded(List<IAccount> loadedAccount);
    }

    /// Interface of an object to be invoked once MSAL application is successfully initialized.
    interface IMsalApplicationLoaded {
        void onApplicationLoaded();
    }

    /// Acting as a bridge between the result of MsalWrapper's results and the outside world.
    interface INotifyOperationResultCallback {
        void acquireTokenSucceed(IAuthenticationResult result);

        void notify(String message);
    }

    private IPublicClientApplication mApplication;
    private IMultipleAccountPublicClientApplication mMultiAccountPublicClientApplication;
    private ISingleAccountPublicClientApplication mSingleAccountPublicClientApplication;
    private HashMap<String, IPostAccountLoaded> postAccountLoadedJobs = new HashMap<>();
    private List<IAccount> mLoadedAccount;

    public void loadMsalApplication(@NonNull final Context context,
                                    @NonNull final int configFileResourceId,
                                    @NonNull final INotifyOperationResultCallback notifyCallback,
                                    @Nullable final IMsalApplicationLoaded msalApplicationLoaded) {
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

    public void onResume(@NonNull final INotifyOperationResultCallback notifyCallback) {
        if (mApplication == null) {
            return;
        }

        if (mApplication instanceof ISingleAccountPublicClientApplication) {
            // Single account mode caller is responsible for calling this onResume.
            // This is because the account might be modified by other apps when this app is in background.
            loadAccountFromBroker(notifyCallback);
        }
    }

    public void registerPostAccountLoadedJob(String key, IPostAccountLoaded job) {
        postAccountLoadedJobs.put(key, job);

        if (mLoadedAccount != null) {
            job.onLoaded(mLoadedAccount);
        }
    }

    public void deregisterPostAccountLoadedJob(String key) {
        postAccountLoadedJobs.remove(key);
    }

    public String getPublicApplicationMode() {
        if (mApplication == null) {
            // Application is not successfully loaded yet.
            return "Not loaded";
        }

        if (mApplication instanceof ISingleAccountPublicClientApplication) {
            ISingleAccountPublicClientApplication app = (ISingleAccountPublicClientApplication) mApplication;
            if (app.isSharedDevice()) {
                return "Single Account - Shared device";
            } else {
                return "Single Account - Non-shared device";
            }
        }

        return "Multiple Account";
    }

    public void acquireToken(final Activity activity,
                             final AcquireTokenFragment.RequestOptions requestOptions,
                             @NonNull final INotifyOperationResultCallback notifyCallback) {
        if (mApplication == null) {
            notifyCallback.notify("Application is not yet loaded");
            return;
        }
        if (mLoadedAccount == null) {
            notifyCallback.notify("account is not yet loaded");
            return;
        }

        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")))
                .withLoginHint(requestOptions.getLoginHint())
                .withPrompt(requestOptions.getPrompt())
                .withOtherScopesToAuthorize(Arrays.asList(requestOptions.getExtraScopesToConsent().toLowerCase().split(" ")))
                .withCallback(getAuthenticationCallback(notifyCallback))
                .build();

        mApplication.acquireToken(parameters);
    }

    public void acquireTokenWithResourceId(final Activity activity,
                                           final AcquireTokenFragment.RequestOptions requestOptions,
                                           @NonNull final INotifyOperationResultCallback notifyCallback) {
        if (mApplication == null) {
            notifyCallback.notify("Application is not yet loaded");
            return;
        }
        if (mLoadedAccount == null) {
            notifyCallback.notify("account is not yet loaded");
            return;
        }

        AcquireTokenParameters.Builder builder = new AcquireTokenParameters.Builder();
        AcquireTokenParameters acquireTokenParameters = builder.startAuthorizationFromActivity(activity)
                .withResource(requestOptions.getScopes().toLowerCase().trim())
                .withPrompt(requestOptions.getPrompt())
                .withAuthorizationQueryStringParameters(null)
                .withCallback(getAuthenticationCallback(notifyCallback))
                .withLoginHint(requestOptions.getLoginHint())
                .build();

        mApplication.acquireToken(acquireTokenParameters);
    }

    public void acquireTokenSilentWithResource(final AcquireTokenFragment.RequestOptions requestOptions,
                                               @NonNull final INotifyOperationResultCallback notifyCallback) {
        if (mApplication == null) {
            notifyCallback.notify("Application is not yet loaded");
            return;
        }

        if (mApplication instanceof IMultipleAccountPublicClientApplication) {
            IMultipleAccountPublicClientApplication multipleAcctApp = (IMultipleAccountPublicClientApplication) mApplication;
            multipleAcctApp.getAccount(
                    requestOptions.getLoginHint().trim(),
                    new IMultipleAccountPublicClientApplication.GetAccountCallback() {
                        @Override
                        public void onTaskCompleted(final IAccount account) {
                            if (null != account) {
                                AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
                                AcquireTokenSilentParameters acquireTokenSilentParameters =
                                        builder.withResource(requestOptions.getScopes().toLowerCase().trim())
                                                .forAccount(account)
                                                .forceRefresh(requestOptions.forceRefresh())
                                                .withCallback(getAuthenticationCallback(notifyCallback))
                                                .build();

                                mApplication.acquireTokenSilentAsync(acquireTokenSilentParameters);

                            } else {
                                notifyCallback.notify("No account found matching loginHint");
                            }
                        }

                        @Override
                        public void onError(final MsalException exception) {
                            notifyCallback.notify("No account found matching loginHint");
                        }
                    });
        } else {
            if (mLoadedAccount == null || mLoadedAccount.size() != 1) {
                notifyCallback.notify("account is not yet loaded");
                return;
            }

            AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
            AcquireTokenSilentParameters acquireTokenSilentParameters =
                    builder.withResource(requestOptions.getScopes().toLowerCase().trim())
                            .forAccount(mLoadedAccount.get(0))
                            .forceRefresh(requestOptions.forceRefresh())
                            .withCallback(getAuthenticationCallback(notifyCallback))
                            .build();

            mApplication.acquireTokenSilentAsync(acquireTokenSilentParameters);
        }
    }

    public void acquireTokenSilent(final AcquireTokenFragment.RequestOptions requestOptions,
                                   @NonNull final INotifyOperationResultCallback notifyCallback) {
        if (mApplication == null) {
            notifyCallback.notify("Application is not yet loaded");
            return;
        }

        if (mApplication instanceof IMultipleAccountPublicClientApplication) {
            IMultipleAccountPublicClientApplication multipleAcctApp = (IMultipleAccountPublicClientApplication) mApplication;
            multipleAcctApp.getAccount(
                    requestOptions.getLoginHint().trim(),
                    new IMultipleAccountPublicClientApplication.GetAccountCallback() {
                        @Override
                        public void onTaskCompleted(final IAccount account) {
                            if (account != null) {
                                AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                                        .withScopes(Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")))
                                        .forAccount(account)
                                        .fromAuthority(mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString())
                                        .forceRefresh(requestOptions.forceRefresh())
                                        .withCallback(getAuthenticationCallback(notifyCallback))
                                        .build();
                                mApplication.acquireTokenSilentAsync(parameters);
                            } else {
                                notifyCallback.notify("No account found matching identifier");
                            }
                        }

                        @Override
                        public void onError(final MsalException exception) {
                            notifyCallback.notify("No account found matching identifier");
                        }
                    });
        } else {
            if (mLoadedAccount == null || mLoadedAccount.size() != 1) {
                notifyCallback.notify("account is not yet loaded");
                return;
            }
            AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                    .withScopes(Arrays.asList(requestOptions.getScopes().toLowerCase().split(" ")))
                    .forAccount(mLoadedAccount.get(0))
                    .fromAuthority(mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString())
                    .forceRefresh(requestOptions.forceRefresh())
                    .withCallback(getAuthenticationCallback(notifyCallback))
                    .build();

            mApplication.acquireTokenSilentAsync(parameters);
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
                } else if(exception instanceof MsalDeclinedScopeException){
                    // Declined scope implies that not all scopes requested have been granted.
                    // Developer can either continue with Authentication by calling acquireTokenSilent
                    // using the AcquireTokenSilentParameters in the MsalDeclinedScopeException or fail the authentication
                    mApplication.acquireTokenSilentAsync(
                            ((MsalDeclinedScopeException) exception).getSilentParametersForGrantedScopes()
                    );
                }
            }

            @Override
            public void onCancel() {
                notifyCallback.notify("User cancelled the flow.");
            }
        };
    }

    public void removeAccount(@NonNull final String username,
                              @NonNull final INotifyOperationResultCallback notifyCallback) {
        if (mApplication == null) {
            notifyCallback.notify("Application is not yet loaded.");
            return;
        }
        if (mLoadedAccount == null) {
            notifyCallback.notify("account is not yet loaded.");
            return;
        }

        if (mApplication instanceof IMultipleAccountPublicClientApplication) {
            final IMultipleAccountPublicClientApplication application = (IMultipleAccountPublicClientApplication) (mApplication);
            application.getAccount(username, new IMultipleAccountPublicClientApplication.GetAccountCallback() {
                @Override
                public void onTaskCompleted(IAccount accountToRemove) {
                    application.removeAccount(
                            accountToRemove,
                            new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                                @Override
                                public void onRemoved() {
                                    notifyCallback.notify("The account is successfully removed.");

                                    // Reload account list.
                                    loadAccountFromBroker(notifyCallback);
                                }

                                @Override
                                public void onError(@NonNull MsalException exception) {
                                    exception.printStackTrace();
                                    notifyCallback.notify("Failed to remove the account.");
                                }
                            });
                }

                @Override
                public void onError(MsalException exception) {
                    exception.printStackTrace();
                    notifyCallback.notify("Failed to remove the account.");
                }
            });
        } else if (mApplication instanceof ISingleAccountPublicClientApplication) {
            final ISingleAccountPublicClientApplication application = (ISingleAccountPublicClientApplication) (mApplication);
            application.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
                @Override
                public void onSignOut() {
                    notifyCallback.notify("The account is successfully removed.");

                    // Reload account list.
                    loadAccountFromBroker(notifyCallback);
                }

                @Override
                public void onError(@NonNull MsalException exception) {
                    notifyCallback.notify("Failed to remove the account: " + exception.getMessage());
                }
            });

        }
    }

    // Refresh the cached account list.
    // Once the operation is done, it'll invoke performPostAccountLoadedJobs() to notify every listeners.
    private void loadAccountFromBroker(@NonNull final INotifyOperationResultCallback notifyCallback) {
        if (mApplication == null) {
            notifyCallback.notify("Application is not yet loaded.");
            return;
        }

        if (mApplication instanceof IMultipleAccountPublicClientApplication) {
            IMultipleAccountPublicClientApplication multipleAcctApp = (IMultipleAccountPublicClientApplication) mApplication;
            multipleAcctApp.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
                @Override
                public void onTaskCompleted(List<IAccount> result) {
                    mLoadedAccount = result;
                    performPostAccountLoadedJobs();
                }

                @Override
                public void onError(MsalException exception) {
                    exception.printStackTrace();
                    notifyCallback.notify
                            ("Failed to load account from broker. "
                                    + "Error code: " + exception.getErrorCode()
                                    + " Error Message: " + exception.getMessage()
                            );
                }

            });
        } else if (mApplication instanceof ISingleAccountPublicClientApplication) {
            ISingleAccountPublicClientApplication singleAcctApp = (ISingleAccountPublicClientApplication) mApplication;
            singleAcctApp.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
                @Override
                public void onAccountLoaded(@Nullable IAccount activeAccount) {
                    mLoadedAccount = new ArrayList<>();
                    if (activeAccount != null) {
                        mLoadedAccount.add(activeAccount);
                    }

                    performPostAccountLoadedJobs();
                }

                @Override
                public void onAccountChanged(IAccount priorAccount, IAccount currentAccount) {
                    notifyCallback.notify(
                            "signed-in account is changed from "
                                    + (null == priorAccount ? "null" : priorAccount.getUsername())
                                    + " to "
                                    + (null == currentAccount ? "null" : currentAccount.getUsername())
                    );
                }

                @Override
                public void onError(MsalException exception) {
                    notifyCallback.notify(exception.getMessage());
                }
            });
        }
    }

    private void performPostAccountLoadedJobs() {
        for (String key : postAccountLoadedJobs.keySet()) {
            IPostAccountLoaded job = postAccountLoadedJobs.get(key);
            job.onLoaded(mLoadedAccount);
        }
    }
}
