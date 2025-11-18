# MSAL Android Code Snippets

This directory contains code snippets demonstrating how to use the Microsoft Authentication Library (MSAL) for Android. Each snippet is provided in both Java and Kotlin.

## Available Snippets

### Initialization
- **[msal_initialization.java](./msal_initialization.java)** / **[msal_initialization.kt](./msal_initialization.kt)** - Initialize MSAL PublicClientApplication in single or multiple account mode

### Multiple Account Mode
- **[acquire_token.java](./acquire_token.java)** / **[acquire_token.kt](./acquire_token.kt)** - Acquire tokens interactively (initial sign-in)
- **[get_accounts.java](./get_accounts.java)** / **[get_accounts.kt](./get_accounts.kt)** - Get list of signed-in accounts
- **[acquire_token_silent.java](./acquire_token_silent.java)** / **[acquire_token_silent.kt](./acquire_token_silent.kt)** - Acquire tokens silently (refresh)
- **[remove_account.java](./remove_account.java)** / **[remove_account.kt](./remove_account.kt)** - Remove an account (sign out)

### Single Account Mode
- **[sign_in.java](./sign_in.java)** / **[sign_in.kt](./sign_in.kt)** - Sign in a user
- **[sign_in_again.java](./sign_in_again.java)** / **[sign_in_again.kt](./sign_in_again.kt)** - Re-authenticate the current user
- **[get_current_account.java](./get_current_account.java)** / **[get_current_account.kt](./get_current_account.kt)** - Get the currently signed-in account
- **[acquire_token_silent.java](./acquire_token_silent.java)** / **[acquire_token_silent.kt](./acquire_token_silent.kt)** - Acquire tokens silently (refresh)
- **[sign_out_account.java](./sign_out_account.java)** / **[sign_out_account.kt](./sign_out_account.kt)** - Sign out the current user

### Advanced
- **[acquire_token_with_device_code_flow.java](./acquire_token_with_device_code_flow.java)** / **[acquire_token_with_device_code_flow.kt](./acquire_token_with_device_code_flow.kt)** - Device Code Flow (not recommended, use only in niche scenarios)

## Usage Examples

### Multiple Account Mode

#### Create PublicClientApplication

```java
IMultipleAccountPublicClientApplication mMultipleAccountApp = null;
private List<IAccount> mAccounts;
private static final List<String> SCOPES = Collections.singletonList("User.Read"); // Basic Microsoft Graph scope

// Create PCA from config file
PublicClientApplication.createMultipleAccountPublicClientApplication(
    context,
    R.raw.auth_config, // Reference the json file in your app
    new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
        @Override
        public void onCreated(IMultipleAccountPublicClientApplication application) {
            mMultipleAccountPCA = application;
            // Do something post initialization, like notifying a callback or calling getAccounts()
        }

        @Override
        public void onError(MsalException exception) {
            // Handle error during initialization
        }
    }
);
```

#### Acquire Token (Sign In)

```java
// Acquire Token (Equivalent to Sign In in Single Account Mode)
AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
    .withScopes(SCOPES)
    .startAuthorizationFromActivity(activity)
    // .withPrompt(Prompt.LOGIN) // Use Prompt.LOGIN to force interactive authentication, even if user is signed in
    .withCallback(getAuthInteractiveCallback())
    .build();

// Acquire token using the parameters
mMultipleAccountApp.acquireToken(parameters);
```

#### Authentication Callback

```java
// An example implementation of the callback
private AuthenticationCallback getAuthInteractiveCallback() {
    return new AuthenticationCallback() {
        @Override
        public void onSuccess(IAuthenticationResult authenticationResult) {
            /* Successfully got a token, use it to call a protected resource */
            String accessToken = authenticationResult.getAccessToken();
            // Record account used to acquire token
            mFirstAccount = authenticationResult.getAccount();
        }
        @Override
        public void onError(MsalException exception) {
            if (exception instanceof MsalClientException) {
                //An exception from the client (MSAL)
            } else if (exception instanceof MsalServiceException) {
                //An exception from the server
            }
        }
        @Override
        public void onCancel() {
            /* User canceled the authentication */
        }
    };
}
```

#### Get Signed-In Accounts

```java
// Get list of signed-in accounts
mMultipleAccountApp.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
    @Override
    public void onTaskCompleted(List<IAccount> accounts) {
        if (accounts != null) {
            // Store accounts list
            mAccounts = accounts;
            // Process accounts
            if (!accounts.isEmpty()) {
                for (IAccount account : accounts) {
                    String username = account.getUsername();
                    // Use account as needed
                }
            }
        } else {
            // No accounts signed in
        }
    }

    @Override
    public void onError(MsalException exception) {
        // Handle error loading accounts
    }
});
```

#### Silent Token Acquisition (Async)

```java
// Silent Token Acquisition
AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .fromAuthority(account.getAuthority())
    .withCallback(getAuthInteractiveCallback())
    .build();
mMultipleAccountApp.acquireTokenSilentAsync(silentParameters);
```

#### Silent Token Acquisition (Sync)

```java
// Silent Token Acquisition synchronously, must be done in a background thread.
AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .fromAuthority(account.getAuthority())
    .build();
mMultipleAccountApp.acquireTokenSilent(silentParameters);
```

#### Remove Account (Sign Out)

```java
// Remove Account for Multiple Account Mode (equialent to Sign Out in Single Account Mode)
mMultipleAccountApp.removeAccount(account, new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
    @Override
    public void onRemoved() {
        // Account successfully removed
        // Update UI, clear account-specific data
    }

    @Override
    public void onError(MsalException exception) {
        // Failed to remove account
        // Handle the error
    }
});
```

### Single Account Mode

#### Create PublicClientApplication

```java
ISingleAccountApplication mSingleAccountApp = null;
IAccount mAccount;
private static final List<String> SCOPES = Collections.singletonList("User.Read"); // Basic Microsoft Graph scope

PublicClientApplication.createSingleAccountPublicClientApplication(
    context,
    R.raw.auth_config, // Reference the json file in your app
    new PublicClientApplication.ISingleAccountApplicationCreatedListener() {
        @Override
        public void onCreated(ISingleAccountPublicClientApplication application) {
            mSingleAccountApp = application;
            // Do something post initialization, like notifying a callback or calling getCurrentAccount()
        }

        @Override
        public void onError(MsalException exception) {
            // Handle error during initialization
        }
    }
);
```

#### Sign In

```java
// Sign In
SignInParameters parameters = SignInParameters.builder()
    // .withLoginHint(mUsername) // Can pass user's login hint if available
    .withScopes(scopes)
    .withActivity(activity)
    .withCallback(getAuthInteractiveCallback())
    .build();

mSingleAccountApp.signIn(parameters);
```

#### Sign In Again (Reauthenticate)

```java
// Sign In Again, reauthenticates the current user
final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                // .withPrompt(Prompt.LOGIN) // Will force an interactive reauth if passed
                .withCallback(getNoCurrentAccountExpectedCallback(countDownLatch))
                .build();
mSingleAccountPCA.signInAgain(signInParameters);
```

#### Get Current Account

```java
// Get current signed-in account
mSingleAccountApp.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
    @Override
    public void onAccountLoaded(@Nullable IAccount account) {
        if (account != null) {
            // Store the account for later use
            mAccount = account;
            // Account is signed in
            String username = account.getUsername();
        } else {
            // No account is signed in
        }
    }

    @Override
    public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
        // Account has changed, update UI accordingly
    }

    @Override
    public void onError(@NonNull MsalException exception) {
        // Handle error loading account
    }
});
```

#### Silent Token Acquisition (Async)

```java
// Silent Token Acquisition
AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .fromAuthority(account.getAuthority())
    .withCallback(getAuthInteractiveCallback())
    .build();
mSingleAccountApp.acquireTokenSilentAsync(silentParameters);
```

#### Silent Token Acquisition (Sync)

```java
// Silent Token Acquisition synchronously, must be done in a background thread.
AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .fromAuthority(account.getAuthority())
    .build();
mSingleAccountApp.acquireTokenSilent(silentParameters);
```

#### Sign Out

```java
// Sign Out for Single Account Mode
mSingleAccountApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
    @Override
    public void onSignOut() {
        // Account successfully signed out
        // Update UI to signed-out state
    }

    @Override
    public void onError(MsalException exception) {
        // Failed to sign out
        // Handle the error
    }
});
```

## Important Notes

>**WARNING**: Device Code Flow (`acquireTokenWithDeviceCode`) is not recommended due to security concerns in the industry. We include it to support backwards compatibility. Only use this method in niche scenarios where devices lack input methods necessary for interactive authentication. For standard authentication scenarios, use `acquireToken` (for multiple account mode) or `signIn` (for single account mode).

>**IMPORTANT**: 
>- Always use Parameters-based APIs instead of deprecated methods
>- Validate PCA initialization before making any API calls
>- Handle UI updates on the main thread using `activity.runOnUiThread`
>- Refresh account lists after authentication operations
>- Use proper callback interfaces for communication between components

## Configuration Best Practices

### Authentication Configuration
- Enable broker integration for enhanced security and SSO capabilities
- URL encode special characters in `redirect_uri` within auth_config.json
- Do NOT URL encode the signature hash in AndroidManifest.xml

### Resource Organization
- Use proper resource naming conventions (e.g., `activity_*`, `fragment_*`)
- Extract dimensions and strings to resource files
- Define consistent theme attributes
- Implement proper view binding

### Error Handling
- Validate PCA initialization before API calls
- Handle and communicate authentication errors appropriately
- Show clear error states to users
- Use progress indicators for async operations

## Additional Resources

- [MSAL Android Documentation](https://learn.microsoft.com/en-us/azure/active-directory/develop/tutorial-v2-android)
- [Complete Examples](../examples/)
  - [Multiple Account Mode Example](../examples/hello-msal-multiple-account/)
  - [Single Account Mode Example](../examples/hello-msal-single-account/)
- [Configuration Template](../auth_config.template.json)
