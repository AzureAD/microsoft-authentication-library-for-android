package com.example.msaldryrun;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.identity.client.IAccount;

public class MainActivity extends AppCompatActivity implements AuthHelper.AuthCallback {
    private AuthHelper mAuthHelper;
    private Button mSignInButton;
    private Button mSignOutButton;
    private Button mAcquireTokenButton;
    private TextView mLogTextView;
    private TextView mAccountInfoTextView;

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

        // Initialize AuthHelper
        mAuthHelper = new AuthHelper(this, this);

        // Setup click listeners
        mSignInButton.setOnClickListener(v -> mAuthHelper.signIn(this));
        mSignOutButton.setOnClickListener(v -> mAuthHelper.signOut());
        mAcquireTokenButton.setOnClickListener(v -> mAuthHelper.acquireTokenSilently());

        updateAccountInfo();
    }

    private void updateUI(boolean isSignedIn) {
        mSignInButton.setEnabled(!isSignedIn);
        mSignOutButton.setEnabled(isSignedIn);
        mAcquireTokenButton.setEnabled(isSignedIn);
    }

    private void logMessage(String message) {
        String currentLog = mLogTextView.getText().toString();
        mLogTextView.setText(currentLog + "\n" + message);
    }

    private void updateAccountInfo() {
        IAccount account = mAuthHelper.getCurrentAccount();
        if (account != null) {
            String displayInfo = String.format("Signed in as:\n%s\n%s", 
                account.getUsername(),
                account.getAuthority());
            mAccountInfoTextView.setText(displayInfo);
        } else {
            mAccountInfoTextView.setText("No account signed in");
        }
    }

    // AuthHelper.AuthCallback implementation
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
}
