

/**
     * Initiates the device code flow for authentication.
     * This method should be called from the main activity's onClick event or similar UI event.
     */
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

                    // This method is called when the device code flow is initiated successfully and user code is received. It's on the application to take this user code and uri,
                    // show it to the user, and allow them to sign in on another device.
                    @Override
                    public void onUserCodeReceived(@NonNull String vUri, @NonNull String userCode, @NonNull String message, @NonNull Date sessionExpirationDate) {
                        mCallback.onDeviceCodeReceived(message);
                    }

                    // This method is called when the token is successfully received after the user has signed in using the device code.
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

                    // This method is called when there is an error during the device code flow.
                    @Override
                    public void onError(@NonNull MsalException exception) {
                        mCallback.onSignInFailure(exception.getMessage());
                    }
                });
    }