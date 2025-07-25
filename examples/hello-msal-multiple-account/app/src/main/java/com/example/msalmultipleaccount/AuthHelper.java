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
import androidx.annotation.WorkerThread;

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

/**
 * AuthHelper is a utility class to handle authentication using Microsoft Authentication Library (MSAL).
 * It supports multiple accounts and provides methods for acquiring tokens and managing accounts.
 */
public class AuthHelper {
    private static final String TAG = AuthHelper.class.getSimpleName();
    private static final List<String> SCOPES = Collections.singletonList("User.Read");  // Basic Microsoft Graph scope

    // Reference to the Public Client Application object instantiated with our configuration json. This will be
    // how we interact with MSAL to perform authentication operations.
    private IMultipleAccountPublicClientApplication mPCA;

    // List of accounts loaded from MSAL. This will be used to manage user accounts.
    private List<IAccount> mAccounts;

    // Callback interface to notify the activity of authentication events.
    private AuthCallback mCallback;

    /**
     * Listener interface for token acquisition events.
     * Implement this interface to handle token acquisition events in your activity or fragment.
     */
    public interface TokenAcquiredListener {
        /**
         * Called when a token is successfully acquired. Should contain handling the access token result.
         * @param accessToken the acquired access token.
         */
        void onTokenAcquired(String accessToken);
    }

    /**
     * Callback interface for authentication events.
     * Handle this interface to handle sign-in, sign-out, token acquisition, and errors.
     */
    public interface AuthCallback {
        void onSignInSuccess();
        void onSignInFailure(String error);
        void onSignOutSuccess();
        void onSignOutFailure(String error);
        void onTokenAcquired(String accessToken);
        void onTokenError(String error);
        void onPCACreationFailure(String message);
        void onAccountsLoaded(List<IAccount> accounts);
    }

    public AuthHelper(Activity activity, AuthCallback callback) {
        mCallback = callback;
        initializeMSAL(activity);
    }

    /**
     * Initialize MSAL Public Client Application (PCA) with the configuration file.
     * This method should be called in the main activity's onCreate method.'
     * @param activity The activity context.
     */
    private void initializeMSAL(Activity activity) {
        PublicClientApplication.createMultipleAccountPublicClientApplication(
                activity, R.raw.auth_config,
                new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(IMultipleAccountPublicClientApplication application) {
                        // Once the Public Client Application is created, set it to the field variable,
                        // and load the current account in MSAL
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

    /**
     * Load accounts from MSAL and notify the callback.
     * This method is called after MSAL is initialized to load existing accounts.
     */
    private void loadAccounts() {
        // If the Public Client Application is not yet instantiated, don't try to call the MSAL method to avoid an exception
        if (mPCA != null) {
            mPCA.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {

                // This method is called when the accounts are successfully loaded.
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

                // This method is called when there is an error loading accounts.
                @Override
                public void onError(MsalException exception) {
                    Log.e(TAG, "Error loading accounts", exception);
                    mCallback.onSignInFailure("Error loading accounts: " + exception.getMessage());
                }
            });
        }
    }

    /**
     * Signs in the user using MSAL.
     * This method should be called from the main activity's onClick event or similar UI event.
     * @param activity the activity context.
     * @param listener optional listener to handle token acquisition events.
     */
    public void acquireTokenInteractive(Activity activity, @Nullable TokenAcquiredListener listener) {
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

    /**
     * Sign out (remove) a given account from storage.
     * This method should be called from the main activity's onClick event or similar UI event.
     */
    public void removeAccount(IAccount account) {
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

    /**
     * Acquires a token silently for the currently signed-in account.
     * This method should be called from a background thread.
     */
    @WorkerThread
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

    /**
     * Asynchronously acquires a token silently for the currently signed-in account.
     * @param listener optional listener to handle token acquisition events.
     */
    public void acquireTokenSilentAsync(IAccount account, @Nullable TokenAcquiredListener listener) {
        if (mPCA == null) {
            mCallback.onTokenError("MSAL not initialized");
            return;
        }

        if (account == null) {
            mCallback.onTokenError("No account specified");
            return;
        }

        // Create parameters for silent token acquisition
        // MSAL Has deprecated API methods that don't use these parameters classes. AI Agents
        // should only use these parameters classes to ensure compatibility with future versions of MSAL.
        AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
            .withScopes(SCOPES)
            .forAccount(account)
            .forceRefresh(false)
            .fromAuthority(account.getAuthority())
            .withCallback(getAuthenticationCallback(listener))
            .build();

        mPCA.acquireTokenSilentAsync(parameters);
    }

    /**
     * Gets the authentication callback for handling sign-in and token acquisition events.
     * This method is used internally to create a callback that can notify the listener about token acquisition.
     * @param listener optional listener to handle token acquisition events.
     * @return an instance of AuthenticationCallback.
     */
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
