Microsoft Authentication Library (MSAL) for Android
==============================================

|[📚Documentation](https://learn.microsoft.com/en-us/azure/active-directory/develop/tutorial-v2-android) | [ 🚀 Getting Started](https://learn.microsoft.com/en-us/azure/active-directory/develop/quickstart-mobile-app-android-sign-in) | [💻 Sample Code](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki/MSAL-Code-Samples)| [ 📖 Library Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [🛠️ Support](README.md#community-help-and-support) | [🌐 Docs Site](https://learn.microsoft.com/en-us/azure/active-directory/develop/scenario-mobile-overview)
| --- | --- | --- | --- | --- | --- |


## Introduction
The Microsoft Authentication Library (MSAL) for Android enables developers to acquire security tokens from the Microsoft identity platform to authenticate users and access secured web APIs for their Android based applications.
The MSAL library for Android gives your app the ability to use the [Microsoft Cloud](https://cloud.microsoft.com) by supporting [Microsoft Azure Active Directory](https://azure.microsoft.com/services/active-directory/) and [Microsoft Personal Accounts](https://account.microsoft.com)  using industry standard OAuth2 and OpenID Connect. The library also supports [Azure AD B2C](https://azure.microsoft.com/services/active-directory-b2c/).

[![Version Badge](https://img.shields.io/maven-central/v/com.microsoft.identity.client/msal.svg)](https://repo1.maven.org/maven2/com/microsoft/identity/client/msal/)

### :exclamation: Migrating from ADAL

ADAL Android was deprecated on June 2023. We do not support ADAL. See the [ADAL to MSAL migration guide for Android](https://docs.microsoft.com/azure/active-directory/develop/migrate-android-adal-msal)

## AI Development Resources

For AI development tools like Cline that access this repository through the GitHub web interface, these resources provide comprehensive guidance for MSAL integration without requiring a local repository clone:

- [AI Assistant Guidelines](Ai.md) - Primary documentation for AI tools, containing core principles and patterns
- [Cline Rules](.clinerules/msal-cline-rules.md) - Essential best practices that Cline must follow for generating MSAL code
- [Example Applications](examples/) - Complete production-quality implementations:
  - [Multiple Account Example](examples/hello-msal-multiple-account/) - Shows multiple account authentication pattern
  - [Single Account Example](examples/hello-msal-single-account/) - Shows single account authentication pattern
- [Code Snippets](snippets/) - Ready-to-use code samples for common MSAL operations
- [Configuration Template](auth_config.template.json) - Reference template showing all required and optional settings

These resources contain all necessary patterns, rules, and examples for AI tools to assist with MSAL integration, providing proper API usage, configuration, and implementation patterns for both multiple and single account scenarios.

## Try out Cline!

We've recently made improvements to our repo to empower AI agents to quickly and correctly generate MSAL-integrated android applications. Feel free to try it out! Use the prompt below as reference, tweak it how you like to get different applications generated for your own needs, and feel free to include UI preferences (Colors, Themes, button placement) as you work with the Cline agent.

### Example Prompt:
Please create a new android application integrated with MSAL named com.example.clinesandboxtest. Place this application in a folder on my desktop. I want this application to have all basic MSAL functionality, only allowing the use of chrome browser and no other browsers. You may use the client id (YOUR CLIENT ID HERE) and redirect_uri (YOUR REDIRECT URI) where applicable.

## Using MSAL

- Before you can get a token from Azure AD v2.0 or Azure AD B2C, you'll need to register an application. To register your app, use [the Azure portal](https://aka.ms/AppRegistrations). For Azure AD B2C, checkout [how to register your app with B2C](https://docs.microsoft.com/azure/active-directory-b2c/active-directory-b2c-app-registration).  

### Requirements

- Min SDK Version 16+
- Target SDK Version 33+


### Step 1: Declare dependency on MSAL

Add to your app's build.gradle:

```gradle
dependencies {
    implementation 'com.microsoft.identity.client:msal:6.+' // Always use latest version (currently 6.+)
}
```

Please also add the following lines to your repositories section in your gradle script:

```gradle
maven { 
    url 'https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1' 
}
```

### Step 2: Create your MSAL configuration file

[Configuration Documentation](https://docs.microsoft.com/azure/active-directory/develop/msal-configuration)

It's simplest to create your configuration file as a "raw" resource file in your project resources.  You'll be able to refer to this using the generated resource identifier when constructing an instance of PublicClientApplication. You can reference the example applications for a direct guide on where to place this file, and what it should look like. If you are registering your app in the portal for the first time, you will also be provided with this config JSON.

```JSON
{
  "client_id" : "<YOUR_CLIENT_ID>",
  "redirect_uri" : "msauth://<YOUR_PACKAGE_NAME>/<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>",
  "authorities": [
    {
      "type": "AAD",
      "audience": {
        "type": "AzureADandPersonalMicrosoftAccount",
        "tenant_id": "common"
      }
    }
  ]
}
```

>NOTE: In the `redirect_uri`, the part `<YOUR_PACKAGE_NAME>` refers to the package name returned by the `context.getPackageName()` method. This package name is the same as the [`application_id`](https://developer.android.com/studio/build/application-id) defined in your `build.gradle` file.

>NOTE: This is the minimum required configuration.  MSAL relies on the defaults that ship with the library for all other settings.  Please refer to the [configuration file documentation](https://docs.microsoft.com/azure/active-directory/develop/msal-configuration) to understand the library defaults.

### Step 3: Configure the AndroidManifest.xml

1. Request the following permissions via the Android Manifest

```XML
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

2. Configure an intent filter in the Android Manifest, using your redirect URI

>NOTE: Failure to include an intent filter matching the redirect URI you specify via configuration will result in a failed interactive token request.  Please double check this!

```XML
    <!--Intent filter to capture authorization code response from the default browser on the device calling back to our app after interactive sign in -->
    <activity
        android:name="com.microsoft.identity.client.BrowserTabActivity">
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data
                android:scheme="msauth"
                android:host="<YOUR_PACKAGE_NAME>"
                android:path="/<YOUR_BASE64_ENCODED_PACKAGE_SIGNATURE>" />
        </intent-filter>
    </activity>
```

>NOTE: Please refer to [this FAQ](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki/MSAL-FAQ#redirect-uri-issues) for more information on common redirect uri issues.

### Step 4: Create an MSAL PublicClientApplication and use MSAL APIs

Android applications communicate with the MSAL library through the PublicSlientApplication class (PCA for short). There are two modes for MSAL applications:

1. **Multiple Account Mode** (Default): Allows multiple accounts to be used within the same application
2. **Single Account Mode**: Restricts the application to use only one account at a time

Select the appropriate mode based on your application's requirements. The examples below demonstrate both modes using the recommended Parameters-based APIs. You can also view a more in-depth example for each account mode in the `examples` directory.

#### Multiple Account Mode

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

```java
// Acquire Token (Equivalent to Sign In in Single Account Mode)
AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
    .withScopes(SCOPES)
    .startAuthorizationFromActivity(activity)
    .withCallback(getAuthInteractiveCallback())
    .build();

// Acquire token using the parameters
mMultipleAccountApp.acquireToken(parameters);
```

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

```java
// Silent Token Acquisition synchronously, must be done in a background thread.
AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .fromAuthority(account.getAuthority())
    .build();
mMultipleAccountApp.acquireTokenSilent(silentParameters);
```

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

#### Single Account Mode

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

```java
// Silent Token Acquisition synchronously, must be done in a background thread.
AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .fromAuthority(account.getAuthority())
    .build();
mSingleAccountApp.acquireTokenSilent(silentParameters);
```

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

All of the above examples can be found in the `snippets` directory, where we've also included jotlin examples.

>**WARNING**: IMPORTANT: Device Code Flow (`acquireTokenWithDeviceCode`) is not recommended due to security concerns in the industry. We include it to support backwards compatibility. Only use this method in niche scenarios where devices lack input methods necessary for interactive authentication. For standard authentication scenarios, use `acquireToken` (for multiple account mode) or `signIn` (for single account mode).

>**IMPORTANT**: 
>- Always use Parameters-based APIs instead of deprecated methods
>- Validate PCA initialization before making any API calls
>- Handle UI updates on the main thread using `activity.runOnUiThread`
>- Refresh account lists after authentication operations
>- Use proper callback interfaces for communication between components

#### Configuration Best Practices

1. **Authentication Configuration**:
   - Enable broker integration for enhanced security and SSO capabilities
   - URL encode special characters in `redirect_uri` within auth_config.json
   - Do NOT URL encode the signature hash in AndroidManifest.xml. Refer to `Step 3` in `Using MSAL` section above for direct examples.

2. **Resource Organization**:
   - Use proper resource naming conventions (e.g., `activity_*`, `fragment_*`)
   - Extract dimensions and strings to resource files
   - Define consistent theme attributes
   - Implement proper view binding

3. **Error Handling**:
   - Validate PCA initialization before API calls
   - Handle and communicate authentication errors appropriately
   - Show clear error states to users
   - Use progress indicators for async operations

## ProGuard
MSAL uses reflection and generic type information stored in `.class` files at runtime to support various persistence and serialization related functionalities. Accordingly, library support for minification and obfuscation is limited. A default configuration is shipped with this library; please [file an issue](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/new/choose) if you find any issues.

## Support

If you have any questions regarding the usage of MSAL Android, please utilize Chat with Copilot for assistance.
If you would like to report any bugs or feature requests, please create a support ticket with your Microsoft representative.

## Contribute

We enthusiastically welcome contributions and feedback. You should [clone the repo and start contributing now](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki/MSAL-Contributing).

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Android Studio Build Requirement
Please note that this project uses [Lombok](https://projectlombok.org/) internally and while using Android Studio you will need to install [Lombok Plugin](https://plugins.jetbrains.com/plugin/6317-lombok) to get the project to build successfully within Android Studio.


## Recommendation

MSAL is a security library. It controls how users sign-in and access services. We recommend you always take the latest version of our library in your app when you can. We use [semantic versioning](http://semver.org) so you can control the risk of updating your app. For example, always downloading the latest minor version number (e.g. x.*y*.x) ensures you get the latest security and feature enhanements with the assurance that our API surface area has not changed. You can always see the latest version and release notes under the [Releases](https://github.com/AzureAD/microsoft-authentication-library-for-android/releases) tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services, please report the issue to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as you can provide. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to [GitHub Issues](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues) or any other public site. We will contact you shortly after receiving your issue report. We encourage you to get new security incident notifications by visiting [Microsoft technical security notifications](https://technet.microsoft.com/en-us/security/dd252948) to subscribe to Security Advisory Alerts.


Copyright (c) Microsoft Corporation.  All rights reserved. Licensed under the MIT License (the "License");
