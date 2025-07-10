//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

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

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AuthHelper {
    private static final String TAG = AuthHelper.class.getSimpleName();
    private static final List<String> SCOPES = Collections.singletonList("User.Read");  // Basic Microsoft Graph scope

    private IMultipleAccountPublicClientApplication mPCA;
    private List<IAccount> mAccounts;
    private AuthCallback mCallback;

    public interface TokenAcquiredListener {
        void onTokenAcquired(String accessToken);
    }

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

    public void signIn(Activity activity, @Nullable TokenAcquiredListener listener) {
        if (mPCA == null) {
            mCallback.onSignInFailure("MSAL not initialized");
            return;
        }

        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
            .startAuthorizationFromActivity(activity)
            .withScopes(SCOPES)
            .withCallback(getAuthenticationCallback(listener))
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

    public void acquireTokenSilent(IAccount account) {
        if (mPCA == null) {
            mCallback.onTokenError("MSAL not initialized");
            return;
        }

        if (account == null) {
            mCallback.onTokenError("No account specified");
            return;
        }

        try {
            AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(SCOPES)
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .build();

            IAuthenticationResult result = mPCA.acquireTokenSilent(parameters);
            String accessToken = result.getAccessToken();
            if (accessToken != null) {
                mCallback.onTokenAcquired(accessToken);
            }
        } catch (MsalException | InterruptedException e) {
            mCallback.onTokenError("Error acquiring token silently: " + e.getMessage());
        }
    }

    public void acquireTokenSilentAsync(IAccount account, @Nullable TokenAcquiredListener listener) {
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
            .fromAuthority(account.getAuthority())
            .withCallback(getAuthenticationCallback(listener))
            .build();

        mPCA.acquireTokenSilentAsync(parameters);
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
                        mCallback.onDeviceCodeReceived(message);
                    }

                    @Override
                    public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                        if (authResult.getAccessToken() != null) {
                            loadAccounts(); // Refresh account list after new sign-in
                            mCallback.onTokenAcquired(authResult.getAccessToken());
                            mCallback.onSignInSuccess();
                        } else {
                            mCallback.onSignInFailure("No access token received");
                        }
                    }

                    @Override
                    public void onError(@NonNull MsalException exception) {
                        mCallback.onSignInFailure(exception.getMessage());
                    }
                });
    }

    private AuthenticationCallback getAuthenticationCallback(@Nullable TokenAcquiredListener listener) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                if (authenticationResult.getAccessToken() != null) {
                    String accessToken = authenticationResult.getAccessToken();
                    loadAccounts(); // Refresh account list after successful authentication
                    mCallback.onTokenAcquired(accessToken);
                    mCallback.onSignInSuccess();
                    
                    // Notify token listener if provided
                    if (listener != null) {
                        listener.onTokenAcquired(accessToken);
                    }
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
