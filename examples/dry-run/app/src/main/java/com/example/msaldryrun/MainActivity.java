package com.example.msaldryrun;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements AuthHelper.AuthCallback {
    private AuthHelper mAuthHelper;
    private Button mSignInButton;
    private Button mSignOutButton;
    private Button mAcquireTokenButton;
    private TextView mLogTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        mSignInButton = findViewById(R.id.signInButton);
        mSignOutButton = findViewById(R.id.signOutButton);
        mAcquireTokenButton = findViewById(R.id.acquireTokenButton);
        mLogTextView = findViewById(R.id.logTextView);

        // Load MSAL configuration
        MSALConfig.getInstance().load(this);

        // Initialize AuthHelper
        mAuthHelper = new AuthHelper(this, this);

        // Setup click listeners
        mSignInButton.setOnClickListener(v -> mAuthHelper.signIn(this));
        mSignOutButton.setOnClickListener(v -> mAuthHelper.signOut());
        mAcquireTokenButton.setOnClickListener(v -> mAuthHelper.acquireTokenSilently());
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

    // AuthHelper.AuthCallback implementation
    @Override
    public void onSignInSuccess() {
        logMessage("Signed in successfully");
        updateUI(true);
    }

    @Override
    public void onSignInFailure(String error) {
        logMessage("Sign in failed: " + error);
        updateUI(false);
    }

    @Override
    public void onSignOutSuccess() {
        logMessage("Signed out successfully");
        updateUI(false);
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
}
