package com.example.msalmultipleaccount;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalClientException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AuthHelper {
    private static final String TAG = AuthHelper.class.getSimpleName();
    private static final List<String> SCOPES = Collections.singletonList("User.Read");  // Basic Microsoft Graph scope

    private IMultipleAccountPublicClientApplication mPCA;
    private List<IAccount> mAccounts;
    private AuthCallback mCallback;

    public interface AuthCallback {
        void onSignInSuccess();
        void onSignInFailure(String error);
        void onSignOutSuccess();
        void onSignOutFailure(String error);
        void onTokenAcquired(String accessToken);
        void onTokenError(String error);
        void onDeviceCodeReceived(String message);
        void onPCACreationFailure(String message);
        void onAccountsLoaded(List<IAccount> accounts);
    }

    public AuthHelper(Activity activity, AuthCallback callback) {
        mCallback = callback;
        initializeMSAL(activity);
    }

    private void initializeMSAL(Activity activity) {
        PublicClientApplication.createMultipleAccountPublicClientApplication(
                activity, R.raw.auth_config,
                new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(IMultipleAccountPublicClientApplication application) {
                        mPCA = application;
                        loadAccounts();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, "Failed to create MSAL application", exception);
                        mCallback.onPCACreationFailure(exception.getMessage());
                    }
                });
    }

    private void loadAccounts() {
        if (mPCA != null) {
            mPCA.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
                @Override
                public void onTaskCompleted(List<IAccount> accounts) {
                    mAccounts = accounts;
                    mCallback.onAccountsLoaded(accounts);
                    if (accounts != null && !accounts.isEmpty()) {
                        Log.d(TAG, accounts.size() + " account(s) loaded");
                    } else {
                        Log.d(TAG, "No accounts loaded");
                    }
                }

                @Override
                public void onError(MsalException exception) {
                    Log.e(TAG, "Error loading accounts", exception);
                    mCallback.onSignInFailure("Error loading accounts: " + exception.getMessage());
                }
            });
        }
    }

    public void signIn(Activity activity) {
        if (mPCA == null) {
            mCallback.onSignInFailure("MSAL not initialized");
            return;
        }

        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(SCOPES)
            .withCallback(getAuthenticationCallback())
            .build();
        mPCA.acquireToken(parameters);
    }

    public void signOut(IAccount account) {
        if (mPCA == null) {
            mCallback.onSignOutFailure("MSAL not initialized");
            return;
        }

        if (account != null) {
            mPCA.removeAccount(account, new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                @Override
                public void onRemoved() {
                    loadAccounts(); // Refresh account list
                    mCallback.onSignOutSuccess();
                }

                @Override
                public void onError(@NonNull MsalException exception) {
                    mCallback.onSignOutFailure(exception.getMessage());
                }
            });
        }
    }

    public void acquireTokenSilently(IAccount account) {
        if (mPCA == null) {
            mCallback.onTokenError("MSAL not initialized");
            return;
        }

        if (account == null) {
            mCallback.onTokenError("No account specified");
            return;
        }

        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
            .withScopes(SCOPES)
            .forAccount(account)
            .forceRefresh(false)
            .withCallback(getAuthenticationCallback())
            .build();

       try {
           mPCA.acquireTokenSilent(parameters);
       } catch (MsalException | InterruptedException e) {
           mCallback.onTokenError("Error acquiring token silently: " + e.getMessage());
       }
    }

    public void acquireTokenWithDeviceCode() {
        if (mPCA == null) {
            mCallback.onTokenError("MSAL not initialized");
            return;
        }

        mPCA.acquireTokenWithDeviceCode(
                SCOPES,
                new IPublicClientApplication.DeviceCodeFlowCallback() {
                    @Override
                    public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message, @NonNull Date sessionExpirationDate) {
                        Activity activity = (Activity) mCallback;
                        activity.runOnUiThread(() -> {
                            mCallback.onDeviceCodeReceived(message);
                        });
                    }

                    @Override
                    public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                        if (authResult.getAccessToken() != null) {
                            loadAccounts(); // Refresh account list after new sign-in
                            Activity activity = (Activity) mCallback;
                            activity.runOnUiThread(() -> {
                                mCallback.onTokenAcquired(authResult.getAccessToken());
                                mCallback.onSignInSuccess();
                            });
                        } else {
                            Activity activity = (Activity) mCallback;
                            activity.runOnUiThread(() -> {
                                mCallback.onSignInFailure("No access token received");
                            });
                        }
                    }

                    @Override
                    public void onError(@NonNull MsalException exception) {
                        Activity activity = (Activity) mCallback;
                        activity.runOnUiThread(() -> {
                            mCallback.onSignInFailure(exception.getMessage());
                        });
                    }
                });
    }

    private AuthenticationCallback getAuthenticationCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                if (authenticationResult.getAccessToken() != null) {
                    loadAccounts(); // Refresh account list after successful authentication
                    mCallback.onTokenAcquired(authenticationResult.getAccessToken());
                    mCallback.onSignInSuccess();
                }
            }

            @Override
            public void onError(MsalException exception) {
                if (exception instanceof MsalClientException) {
                    mCallback.onSignInFailure("Client error: " + exception.getMessage());
                } else {
                    mCallback.onSignInFailure(exception.getMessage());
                }
            }

            @Override
            public void onCancel() {
                mCallback.onSignInFailure("Authentication cancelled");
            }
        };
    }

    public List<IAccount> getAccounts() {
        return mAccounts;
    }

    public boolean hasAccounts() {
        return mAccounts != null && !mAccounts.isEmpty();
    }
}
