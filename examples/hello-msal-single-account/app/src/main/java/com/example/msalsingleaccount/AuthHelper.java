package com.example.msalsingleaccount;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalClientException;

import java.util.Date;

public class AuthHelper {
    private static final String TAG = AuthHelper.class.getSimpleName();
    private static final String[] SCOPES = {"User.Read"};  // Basic Microsoft Graph scope

    private ISingleAccountPublicClientApplication mPCA;
    private IAccount mAccount;
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
    }

    public AuthHelper(Activity activity, AuthCallback callback) {
        mCallback = callback;
        initializeMSAL(activity);
    }

    private void initializeMSAL(Activity activity) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
                activity, R.raw.auth_config,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        mPCA = application;
                        loadAccount();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, "Failed to create MSAL application", exception);
                        mCallback.onPCACreationFailure(exception.getMessage());
                    }
                });
    }

    private void loadAccount() {
        if (mPCA != null) {
            mPCA.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
                @Override
                public void onAccountLoaded(@Nullable IAccount account) {
                    mAccount = account;
                    if (mAccount != null) {
                        Log.d(TAG, "Account loaded: " + mAccount.getUsername());
                    } else {
                        Log.d(TAG, "No account loaded");
                    }
                }

                @Override
                public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                    mAccount = currentAccount;
                    if (mAccount != null) {
                        Log.d(TAG, "Account changed: " + mAccount.getUsername());
                    } else {
                        Log.d(TAG, "No account loaded after change");
                    }
                }

                @Override
                public void onError(@NonNull MsalException exception) {
                    Log.e(TAG, "Error loading account", exception);
                }
            });
        }
    }

    public void signIn(Activity activity) {
        if (mPCA == null) {
            mCallback.onSignInFailure("MSAL not initialized");
            return;
        }

        mPCA.signIn(activity, null, SCOPES, getAuthenticationCallback());
    }

    public void signOut() {
        if (mPCA == null) {
            mCallback.onSignOutFailure("MSAL not initialized");
            return;
        }

        if (mAccount != null) {
            mPCA.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
                @Override
                public void onSignOut() {
                    mAccount = null;
                    mCallback.onSignOutSuccess();
                }

                @Override
                public void onError(MsalException exception) {
                    mCallback.onSignOutFailure(exception.getMessage());
                }
            });
        }
    }

    public void acquireTokenSilently() {
        if (mPCA == null) {
            mCallback.onTokenError("MSAL not initialized");
            return;
        }

        if (mAccount == null) {
            mCallback.onTokenError("No account signed in");
            return;
        }

        mPCA.acquireTokenSilentAsync(SCOPES, mAccount.getAuthority(), getAuthenticationCallback());
    }

    public void acquireTokenWithDeviceCode() {
        if (mPCA == null) {
            mCallback.onTokenError("MSAL not initialized");
            return;
        }

        // To use device code flow, ensure you have the necessary permissions and configuration in your Azure AD app registration.
        // Enable "Allow public client flows" under the Authentication tab of your app registration.

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
                            mAccount = authResult.getAccount();
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
                    mAccount = authenticationResult.getAccount();
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

    public IAccount getCurrentAccount() {
        return mAccount;
    }

    public boolean isSignedIn() {
        return mAccount != null;
    }
}
