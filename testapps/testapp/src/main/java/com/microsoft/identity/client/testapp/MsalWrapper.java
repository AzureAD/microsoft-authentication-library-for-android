package com.microsoft.identity.client.testapp;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.ITenantProfile;
import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalArgumentException;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.common.internal.cache.SchemaUtil;
import com.microsoft.identity.common.internal.controllers.TaskCompletedCallbackWithError;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftIdToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            return "Single Account (Shared device)";
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
                .withUiBehavior(requestOptions.getUiBehavior())
                .withOtherScopesToAuthorize(Arrays.asList(requestOptions.getExtraScopesToConsent().toLowerCase().split(" ")))
                .callback(getAuthenticationCallback(notifyCallback))
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
                .withUiBehavior(requestOptions.getUiBehavior())
                .withAuthorizationQueryStringParameters(null)
                .callback(getAuthenticationCallback(notifyCallback))
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
                    new PublicClientApplication.GetAccountCallback() {
                        @Override
                        public void onTaskCompleted(final IAccount account) {
                            if (null != account) {
                                AcquireTokenSilentParameters.Builder builder = new AcquireTokenSilentParameters.Builder();
                                AcquireTokenSilentParameters acquireTokenSilentParameters =
                                        builder.withResource(requestOptions.getScopes().toLowerCase().trim())
                                                .forAccount(account)
                                                .forceRefresh(requestOptions.forceRefresh())
                                                .callback(getAuthenticationCallback(notifyCallback))
                                                .build();

                                mApplication.acquireTokenSilent(acquireTokenSilentParameters);

                            } else {
                                notifyCallback.notify("No account found matching loginHint");
                            }
                        }

                        @Override
                        public void onError(final Exception exception) {
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
                            .callback(getAuthenticationCallback(notifyCallback))
                            .build();

            mApplication.acquireTokenSilent(acquireTokenSilentParameters);
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
                    new PublicClientApplication.GetAccountCallback() {
                        @Override
                        public void onTaskCompleted(final IAccount account) {
                            if (account != null) {
                                AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                                        .withScopes(Arrays.asList( requestOptions.getScopes().toLowerCase().split(" ")))
                                        .forAccount(account)
                                        //TODO Issue#669
                                        // .fromAuthority(mApplication.getConfiguration().getDefaultAuthority().toString())
                                        .forceRefresh(requestOptions.forceRefresh())
                                        .callback(getAuthenticationCallback(notifyCallback))
                                        .build();
                                mApplication.acquireTokenSilent(parameters);
                            } else {
                                notifyCallback.notify("No account found matching identifier");
                            }
                        }

                        @Override
                        public void onError(final Exception exception) {
                            notifyCallback.notify("No account found matching identifier");
                        }
                    });
        } else {
            if (mLoadedAccount == null || mLoadedAccount.size() != 1) {
                notifyCallback.notify("account is not yet loaded");
                return;
            }
            AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                    .withScopes(Arrays.asList( requestOptions.getScopes().toLowerCase().split(" ")))
                    .forAccount(mLoadedAccount.get(0))
                    .forceRefresh(requestOptions.forceRefresh())
                    .callback(getAuthenticationCallback(notifyCallback))
                    .build();

            mApplication.acquireTokenSilent(parameters);
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
            application.getAccount(username, new PublicClientApplication.GetAccountCallback() {
                @Override
                public void onTaskCompleted(IAccount accountToRemove) {
                    application.removeAccount(
                            accountToRemove,
                            new TaskCompletedCallbackWithError<Boolean, Exception>() {
                                @Override
                                public void onTaskCompleted(Boolean isSuccess) {
                                    if (isSuccess) {
                                        notifyCallback.notify("The account is successfully removed.");

                                        // Reload account list.
                                        loadAccountFromBroker(notifyCallback);
                                    } else {
                                        notifyCallback.notify("Failed to remove the account.");
                                    }
                                }

                                @Override
                                public void onError(Exception exception) {
                                    exception.printStackTrace();
                                    notifyCallback.notify("Failed to remove the account.");
                                }
                            });
                }

                @Override
                public void onError(Exception exception) {
                    exception.printStackTrace();
                    notifyCallback.notify("Failed to remove the account.");
                }
            });
        } else if (mApplication instanceof ISingleAccountPublicClientApplication) {
            final ISingleAccountPublicClientApplication application = (ISingleAccountPublicClientApplication) (mApplication);
            application.signOut(new TaskCompletedCallbackWithError<Boolean, Exception>() {
                @Override
                public void onTaskCompleted(Boolean result) {
                    if (result) {
                        notifyCallback.notify("The account is successfully removed.");

                        // Reload account list.
                        loadAccountFromBroker(notifyCallback);
                    } else {
                        notifyCallback.notify("Account is not removed.");
                    }
                }

                @Override
                public void onError(Exception exception) {
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
            multipleAcctApp.getAccounts(new PublicClientApplication.LoadAccountCallback() {
                @Override
                public void onTaskCompleted(List<IAccount> result) {
                    mLoadedAccount = result;
                    performPostAccountLoadedJobs();
                }

                @Override
                public void onError(Exception exception) {
                    exception.printStackTrace();
                    notifyCallback.notify("Failed to load account from broker");
                }

            });
        } else if (mApplication instanceof ISingleAccountPublicClientApplication) {
            ISingleAccountPublicClientApplication singleAcctApp = (ISingleAccountPublicClientApplication) mApplication;
            singleAcctApp.getCurrentAccount(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
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
                                    + (null == priorAccount ? "null" : getPreferredUsername(priorAccount))
                                    + " to "
                                    + (null == currentAccount ? "null" : getPreferredUsername(currentAccount))
                    );
                }

                @Override
                public void onError(Exception exception) {
                    notifyCallback.notify(exception.getMessage());
                }
            });
        }
    }

    @NonNull
    static String getPreferredUsername(@NonNull final IAccount account) {
        Map<String, ?> claims = null;

        // First inspect the root (the home account) - if there are claims, source the username from here
        if (null != account.getClaims()) {
            claims = account.getClaims();
        } else { // We did not have a home account... fallback on iterating over the guests profiles...
            final MultiTenantAccount multiTenantAccount = (MultiTenantAccount) account;

            for (final ITenantProfile profile : multiTenantAccount.getTenantProfiles().values()) {
                if (null != profile.getClaims()) {
                    claims = profile.getClaims();
                    break;
                }
            }
        }

        return SchemaUtil.getDisplayableId(claims);
    }

    private void performPostAccountLoadedJobs() {
        for (String key : postAccountLoadedJobs.keySet()) {
            IPostAccountLoaded job = postAccountLoadedJobs.get(key);
            job.onLoaded(mLoadedAccount);
        }
    }
}
