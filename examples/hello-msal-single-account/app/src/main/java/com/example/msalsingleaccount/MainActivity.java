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

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.identity.client.IAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * MainActivity for the MSAL Single Account example app.
 * Demonstrates sign-in, sign-out, token acquisition, and calling Microsoft Graph API.
 */
public class MainActivity extends AppCompatActivity implements AuthHelper.AuthCallback {

    // UI elements
    private AuthHelper mAuthHelper;
    private Button mSignInButton;
    private Button mSignOutButton;
    private Button mAcquireTokenSilentButton;
    private Button mAcquireTokenDeviceCodeButton;
    private Button mCallGraphButton;
    private TextView mLogTextView;
    private TextView mAccountInfoTextView;

    /**
     * Called when the activity is first created.
     * Initializes the UI and sets up click listeners for buttons.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        mCallGraphButton = findViewById(R.id.callGraphButton);
        mSignInButton = findViewById(R.id.signInButton);
        mSignOutButton = findViewById(R.id.signOutButton);
        mAcquireTokenSilentButton = findViewById(R.id.acquireTokenSilentButton);
        mLogTextView = findViewById(R.id.logTextView);
        mAccountInfoTextView = findViewById(R.id.accountInfoTextView);
        mAcquireTokenDeviceCodeButton = findViewById(R.id.acquireTokenDeviceCodeButton);

        // Initialize AuthHelper
        mAuthHelper = new AuthHelper(this, this);

        // Setup click listeners

        // Map button to call Sign In Method
        mSignInButton.setOnClickListener(v -> mAuthHelper.signIn(this, null));

        // Map button to call Sign Out Method
        mSignOutButton.setOnClickListener(v -> mAuthHelper.signOut());

        // Map button to call Acquire Token Silent Method
        mAcquireTokenSilentButton.setOnClickListener(v ->

                // Standard acquireTokenSilent must be run on a background (or worker) thread.
                // To run on the main thread, use acquireTokenSilentAsync.
                CompletableFuture.runAsync(() -> {
                    mAuthHelper.acquireTokenSilent();
                })
        );

        // Map button to call Acquire Token Device Code Method
        mAcquireTokenDeviceCodeButton.setOnClickListener(v -> mAuthHelper.acquireTokenWithDeviceCode());

        // Map button to call Graph API with the signed in user
        mCallGraphButton.setOnClickListener(v -> {
            IAccount selectedAccount = mAuthHelper.getCurrentAccount();
            if (selectedAccount != null) {
                callGraphAPI();
            }
        });

        // Once the methods have been mapped to buttons, update the account information UI
        updateAccountInfo();
    }

    /**
     * Updates the UI based on the sign-in state.
     * The UI updates will be ran on the Ui Thread.
     * @param isSignedIn whether or not there's an account signed in.
     */
    private void updateUI(boolean isSignedIn) {
        runOnUiThread(() -> {
            mSignInButton.setEnabled(!isSignedIn);
            mAcquireTokenDeviceCodeButton.setEnabled(!isSignedIn);
            mSignOutButton.setEnabled(isSignedIn);
            mAcquireTokenSilentButton.setEnabled(isSignedIn);
            mCallGraphButton.setEnabled(isSignedIn);
        });
    }

    /**
     * Logs a message to the log TextView.
     * The UI updates will be ran on the Ui Thread.
     * @param message the message to log.
     */
    private void logMessage(String message) {
        runOnUiThread(() -> {
            String currentLog = mLogTextView.getText().toString();
            mLogTextView.setText(currentLog + "\n" + message);
        });
    }

    /**
     * Updates the account information TextView with the current account details.
     * The UI updates will be ran on the Ui Thread.
     */
    private void updateAccountInfo() {
        runOnUiThread(() -> {
            IAccount account = mAuthHelper.getCurrentAccount();
            if (account != null) {
                String displayInfo = String.format("Signed in as:\n%s",
                    account.getUsername());
                mAccountInfoTextView.setText(displayInfo);
            } else {
                mAccountInfoTextView.setText("No account signed in");
            }
        });
    }

    /**
     * Uses GraphHelper to call the Microsoft Graph API to retrieve user information.
     */
    private void callGraphAPI() {
        mAuthHelper.acquireTokenSilentAsync(accessToken -> {
            GraphHelper.callGraphAPI(accessToken, new GraphHelper.GraphCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    try {
                        String displayName = data.getString("displayName");
                        String officeLocation = data.getString("officeLocation");
                        logMessage("Graph API Success!\nDisplay Name: " + displayName +
                                "\nOffice: " + officeLocation);
                    } catch (JSONException e) {
                        logMessage("Error parsing Graph response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(String error) {
                    logMessage("Graph API Error: " + error);
                }
            });
        });
    }

    // AuthHelper.AuthCallback implementation
    // These methods are called from AuthHelper to communicate MSAL operation results, and handle UI updates in response
    @Override
    public void onSignInSuccess() {
        logMessage("Signed in successfully");
        updateUI(true);
        updateAccountInfo();
    }

    @Override
    public void onSignInFailure(String error) {
        logMessage("Sign in failed: " + error);
        updateUI(false);
        updateAccountInfo();
    }

    @Override
    public void onSignOutSuccess() {
        logMessage("Signed out successfully");
        updateUI(false);
        updateAccountInfo();
    }

    @Override
    public void onSignOutFailure(String error) {
        logMessage("Sign out failed: " + error);
        updateAccountInfo();
    }

    @Override
    public void onTokenAcquired(String accessToken) {
        String truncatedToken = accessToken.substring(0, Math.min(accessToken.length(), 20)) + "...";
        logMessage("Token acquired: " + truncatedToken);
    }

    @Override
    public void onTokenError(String error) {
        logMessage("Token acquisition failed: " + error);
    }

    @Override
    public void onDeviceCodeReceived(String message) {
        logMessage("Device code initiated.\n" + message);
    }

    @Override
    public void onPCACreationFailure(String message) {
        logMessage("Failed to create MSAL application: " + message);
    }

    @Override
    public void onInitialAccountLoaded(boolean accountLoaded) {
        updateUI(accountLoaded);
        updateAccountInfo();
    }
}
