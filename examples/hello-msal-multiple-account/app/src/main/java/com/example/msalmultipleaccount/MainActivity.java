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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AuthHelper.AuthCallback {
    private AuthHelper mAuthHelper;
    private Button mSignInButton;
    private Button mSignOutButton;
    private Button mAcquireTokenButton;
    private Button mAcquireTokenDeviceCodeButton;
    private TextView mLogTextView;
    private TextView mAccountInfoTextView;
    private Spinner mAccountSpinner;
    private List<IAccount> mAccounts;
    private ArrayAdapter<String> mAccountAdapter;
    private boolean mFirstLaunch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        mSignInButton = findViewById(R.id.signInButton);
        mSignOutButton = findViewById(R.id.signOutButton);
        mAcquireTokenButton = findViewById(R.id.acquireTokenButton);
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
        mSignInButton.setOnClickListener(v -> mAuthHelper.signIn(this));
        mSignOutButton.setOnClickListener(v -> {
            IAccount selectedAccount = getSelectedAccount();
            if (selectedAccount != null) {
                mAuthHelper.signOut(selectedAccount);
            }
        });
        mAcquireTokenButton.setOnClickListener(v -> {
            IAccount selectedAccount = getSelectedAccount();
            if (selectedAccount != null) {
                mAuthHelper.acquireTokenSilently(selectedAccount);
            }
        });
        mAcquireTokenDeviceCodeButton.setOnClickListener(v -> mAuthHelper.acquireTokenWithDeviceCode());

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

        updateUI();
    }

    private void updateUI() {
        boolean hasAccounts = mAuthHelper.hasAccounts();
        mSignInButton.setEnabled(true);  // Always enabled to allow signing in new accounts
        mAcquireTokenDeviceCodeButton.setEnabled(true);  // Always enabled for device code flow
        mSignOutButton.setEnabled(hasAccounts && getSelectedAccount() != null);  // Only enabled when an account is selected
        mAcquireTokenButton.setEnabled(hasAccounts && getSelectedAccount() != null);  // Only enabled when an account is selected
        mAccountSpinner.setEnabled(hasAccounts);
    }

    private void updateAccountSpinner(List<IAccount> accounts) {
        mAccounts = accounts;
        List<String> accountNames = new ArrayList<>();
        accountNames.add("No Account Selected");  // Add option for no selection
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
    }

    private IAccount getSelectedAccount() {
        int position = mAccountSpinner.getSelectedItemPosition();
        // Return null if "No Account Selected" is chosen (position 0) or invalid position
        if (position <= 0 || position > mAccounts.size()) {
            return null;
        }
        // Adjust position by -1 since we added "No Account Selected" at the beginning
        return mAccounts.get(position - 1);
    }

    private void logMessage(String message) {
        String currentLog = mLogTextView.getText().toString();
        mLogTextView.setText(currentLog + "\n" + message);
    }

    private void updateAccountInfo() {
        IAccount account = getSelectedAccount();
        if (account != null) {
            String displayInfo = String.format("Selected account:\n%s",
                account.getUsername());
            mAccountInfoTextView.setText(displayInfo);
        } else {
            mAccountInfoTextView.setText("No account selected");
        }
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
