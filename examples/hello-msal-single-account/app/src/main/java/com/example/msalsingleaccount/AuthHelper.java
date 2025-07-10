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

package com.example.msalsingleaccount;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalClientException;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AuthHelper {
    private static final String TAG = AuthHelper.class.getSimpleName();
    private static final List<String> SCOPES = Collections.singletonList("User.Read");  // Basic Microsoft Graph scope

    private ISingleAccountPublicClientApplication mPCA;
    private IAccount mAccount;
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
        void onInitialAccountLoaded(boolean accountLoaded);
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
                        mCallback.onInitialAccountLoaded(true);
                    } else {
                        Log.d(TAG, "No account loaded");
                        mCallback.onInitialAccountLoaded(false);
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

    public void signIn(Activity activity, @Nullable TokenAcquiredListener listener) {
        if (mPCA == null) {
            mCallback.onSignInFailure("MSAL not initialized");
            return;
        }

        SignInParameters parameters = SignInParameters.builder()
                .withScopes(SCOPES)
                .withActivity(activity)
                .withCallback(getAuthenticationCallback(listener))
                .build();

        mPCA.signIn(parameters);
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

    public void acquireTokenSilent(@Nullable TokenAcquiredListener listener) {
        if (mPCA == null) {
            mCallback.onTokenError("MSAL not initialized");
            return;
        }

        if (mAccount == null) {
            mCallback.onTokenError("No account signed in");
            return;
        }

        try {
            AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                    .withScopes(SCOPES)
                    .forAccount(mAccount)
                    .fromAuthority(mAccount.getAuthority())
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

    public void acquireTokenSilentAsync(@Nullable TokenAcquiredListener listener) {
        if (mPCA == null) {
            mCallback.onTokenError("MSAL not initialized");
            return;
        }

        if (mAccount == null) {
            mCallback.onTokenError("No account signed in");
            return;
        }

        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(SCOPES)
                .forAccount(mAccount)
                .forceRefresh(false)
                .fromAuthority(mAccount.getAuthority())
                .withCallback(getAuthenticationCallback(listener))
                .build();

        mPCA.acquireTokenSilentAsync(parameters);
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
                        mCallback.onDeviceCodeReceived(message);
                    }

                    @Override
                    public void onTokenReceived(@NonNull IAuthenticationResult authResult) {
                        if (authResult.getAccessToken() != null) {
                            mAccount = authResult.getAccount();
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
                    mAccount = authenticationResult.getAccount();
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

    public IAccount getCurrentAccount() {
        return mAccount;
    }

    public boolean isSignedIn() {
        return mAccount != null;
    }
}
