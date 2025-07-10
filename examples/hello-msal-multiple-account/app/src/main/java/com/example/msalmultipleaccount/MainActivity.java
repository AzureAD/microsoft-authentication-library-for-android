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

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.identity.client.IAccount;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MainActivity for the MSAL Multiple Account example app.
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
    private Spinner mAccountSpinner;
    private List<IAccount> mAccounts;
    private ArrayAdapter<String> mAccountAdapter;
    private boolean mFirstLaunch = true;

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
        mAccountSpinner = findViewById(R.id.accountSpinner);

        // Initialize account adapter
        mAccounts = new ArrayList<>();
        mAccountAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        mAccountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAccountSpinner.setAdapter(mAccountAdapter);

        // Initialize AuthHelper
        mAuthHelper = new AuthHelper(this, this);

        // Setup click listeners
        // Map button to call Sign In Method
        mSignInButton.setOnClickListener(v -> mAuthHelper.acquireTokenInteractive(this, null));

        // Map button to call Sign Out Method
        mSignOutButton.setOnClickListener(v -> {
            IAccount selectedAccount = getSelectedAccount();
            if (selectedAccount != null) {
                mAuthHelper.removeAccount(selectedAccount);
            }
        });

        // Map button to call acquireTokenSilent Method
        mAcquireTokenSilentButton.setOnClickListener(v -> {
            IAccount selectedAccount = getSelectedAccount();
            if (selectedAccount != null) {
                // Standard acquireTokenSilent must be run on a background (or worker) thread.
                // To run on the main thread, use acquireTokenSilentAsync.
                CompletableFuture.runAsync(() -> {
                    mAuthHelper.acquireTokenSilent(selectedAccount);
                });
            }
        });

        // Map button to call Acquire Token Device Code Method
        mAcquireTokenDeviceCodeButton.setOnClickListener(v -> mAuthHelper.acquireTokenWithDeviceCode());

        // Map button to call Graph API with the signed in user
        mCallGraphButton.setOnClickListener(v -> {
            IAccount selectedAccount = getSelectedAccount();
            if (selectedAccount != null) {
                callGraphAPI(selectedAccount);
            }
        });

        // Set up the account spinner to listen for account selection changes.
        // Whenever a new account is selected, update the account info and UI.
        mAccountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAccountInfo();
                updateUI();  // Update button states when a different account is selected
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Once the methods have been mapped to buttons, update the account information UI
        updateUI();
    }

    /**
     * Updates the UI based on the sign-in state.
     * The UI updates will be ran on the Ui Thread.
     */
    private void updateUI() {
        runOnUiThread(() -> {
            boolean hasAccounts = mAuthHelper.hasAccounts();
            mSignInButton.setEnabled(true);  // Always enabled to allow signing in new accounts
            mAcquireTokenDeviceCodeButton.setEnabled(true);  // Always enabled for device code flow
            boolean accountSelected = hasAccounts && getSelectedAccount() != null;
            mSignOutButton.setEnabled(accountSelected);  // Only enabled when an account is selected
            mAcquireTokenSilentButton.setEnabled(accountSelected);  // Only enabled when an account is selected
            mCallGraphButton.setEnabled(accountSelected);  // Only enabled when an account is selected
            mAccountSpinner.setEnabled(hasAccounts);
        });
    }

    /**
     * Updates the account spinner with the list of accounts.
     * The UI updates will be ran on the Ui Thread.
     *
     * @param accounts List of accounts to display in the spinner.
     */
    private void updateAccountSpinner(List<IAccount> accounts) {
        runOnUiThread(() -> {
            mAccounts = accounts;
            List<String> accountNames = new ArrayList<>();
            accountNames.add("No Account Selected");  // Add option for no selection

            // Add each account to the spinner
            for (IAccount account : accounts) {
                accountNames.add(account.getUsername());
            }
            mAccountAdapter.clear();
            mAccountAdapter.addAll(accountNames);
            mAccountAdapter.notifyDataSetChanged();

            // On first launch, if there are accounts, select the first one
            if (mFirstLaunch && !accounts.isEmpty()) {
                mAccountSpinner.setSelection(1);  // Select first account (position 1 since 0 is "No Account Selected")
                mFirstLaunch = false;
            }
        });
    }

    /**
     * Gets the currently selected account from the spinner.
     * Returns null if "No Account Selected" is chosen or if no accounts are available.
     *
     * @return The selected account or null if no account is selected.
     */
    private IAccount getSelectedAccount() {
        int position = mAccountSpinner.getSelectedItemPosition();
        // Return null if "No Account Selected" is chosen (position 0) or invalid position
        if (position <= 0 || position > mAccounts.size()) {
            return null;
        }
        // Adjust position by -1 since we added "No Account Selected" at the beginning
        return mAccounts.get(position - 1);
    }

    /**
     * Logs a message to the log TextView.
     * The UI updates will be ran on the Ui Thread.
     *
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
            IAccount account = getSelectedAccount();
            if (account != null) {
                String displayInfo = String.format("Selected account:\n%s",
                        account.getUsername());
                mAccountInfoTextView.setText(displayInfo);
            } else {
                mAccountInfoTextView.setText("No account selected");
            }
        });
    }

    /**
     * Calls the Microsoft Graph API using the access token from the selected account.
     * Displays the user's display name and office location in the log.
     *
     * @param account The account to use for the Graph API call.
     */
    private void callGraphAPI(IAccount account) {
        mAuthHelper.acquireTokenSilentAsync(account, accessToken -> {
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
    @Override
    public void onSignInSuccess() {
        logMessage("Signed in successfully");
        updateUI();
    }

    @Override
    public void onSignInFailure(String error) {
        logMessage("Sign in failed: " + error);
        updateUI();
    }

    @Override
    public void onSignOutSuccess() {
        logMessage("Signed out successfully");
        updateUI();
    }

    @Override
    public void onSignOutFailure(String error) {
        logMessage("Sign out failed: " + error);
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
    public void onAccountsLoaded(List<IAccount> accounts) {
        updateAccountSpinner(accounts);
        updateAccountInfo();
        updateUI();
    }
}
