Microsoft Authentication Library (MSAL) Preview for Android
==============================================

| [Getting Started](https://docs.microsoft.com/en-us/azure/active-directory/develop/guidedsetups/active-directory-mobileanddesktopapp-android-intro) | [Sample Code](https://github.com/Azure-Samples/active-directory-android-native-v2) | [API Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [Support](README.md#community-help-and-support)
| --- | --- | --- | --- |

The MSAL library for Android gives your app the ability to begin using the [Microsoft Cloud](https://cloud.microsoft.com) by supporting [Microsoft Azure Active Directory](https://azure.microsoft.com/en-us/services/active-directory/) and [Microsoft Accounts](https://account.microsoft.com) in a converged experience using industry standard OAuth2 and OpenID Connect. The library also supports [Azure AD B2C](https://azure.microsoft.com/services/active-directory-b2c/).

[![Version Badge](https://img.shields.io/maven-central/v/com.microsoft.identity.client/msal.svg)](http://repo1.maven.org/maven2/com/microsoft/identity/client/msal/)
[![Build Status](https://travis-ci.org/AzureAD/microsoft-authentication-library-for-android.svg?branch=master)](https://travis-ci.org/AzureAD/microsoft-authentication-library-for-android)

## Important Note about the MSAL Preview

This library is suitable for use in a production environment. We provide the same production level support for this library as we do our current production libraries. During the preview we may make changes to the API, internal cache format, and other mechanisms of this library, which you will be required to take along with bug fixes or feature improvements. This may impact your application. For instance, a change to the cache format may impact your users, such as requiring them to sign in again. An API change may require you to update your code. When we provide the General Availability release we will require you to update to the General Availability version within six months, as applications written using a preview version of library may no longer work.

## Example

```Java
    // Instantiates MSAL Public Client App
    PublicClientApplication myApp = new PublicClientApplication(
                    this.getApplicationContext(),
                    CLIENT_ID);

    // Acquires a token from AzureAD 
    myApp.acquireToken(this, SCOPES, getAuthInteractiveCallback());

    // ...

    // The access token can now be used to access a protected service!
    String accessToken = authenticationResult.getAccessToken();
```

For a full example, checkout the full [code sample](https://github.com/Azure-Samples/active-directory-android-native-v2).

## Installation

### Binaries via Gradle (Recommended way)

Add to your app's Build.Gradle:

```gradle
    repositories {
        mavenCentral()
    }
    dependencies {
        compile('com.microsoft.identity.client:msal:0.1.1') {
            // if your app includes android support
            // libraries or GSON in its dependencies
            // uncomment below

            // exclude group: 'com.android.support', module: 'appcompat-v7'
            // exclude group: 'com.google.code.gson'
        }
    }
```

### AAR package inside libs folder
You can get the AAR file from maven central and drop into **libs** folder in your project.

## Community Help and Support

We use [Stack Overflow](http://stackoverflow.com/questions/tagged/msal) with the community to provide support. We highly recommend you ask your questions on Stack Overflow first and browse existing issues to see if someone has asked your question before. 

If you find and bug or have a feature request, please raise the issue on [GitHub Issues](../../issues). 

To provide a recommendation, visit our [User Voice page](https://feedback.azure.com/forums/169401-azure-active-directory).

## Contribute

We enthusiastically welcome contributions and feedback. You can clone the repo and start contributing now. Read our [Contribution Guide](Contributing.md) for more information.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Security Library

This library controls how users sign-in and access services. We recommend you always take the latest version of our library in your app when possible. We use [semantic versioning](http://semver.org) so you can control the risk associated with updating your app. As an example, always downloading the latest minor version number (e.g. x.*y*.x) ensures you get the latest security and feature enhanements but our API surface remains the same. You can always see the latest version and release notes under the Releases tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services please report it to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as possible. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to GitHub Issues or any other public site. We will contact you shortly upon receiving the information. We encourage you to get notifications of when security incidents occur by visiting [this page](https://technet.microsoft.com/en-us/security/dd252948) and subscribing to Security Advisory Alerts.


Copyright (c) Microsoft Corporation.  All rights reserved. Licensed under the MIT License (the "License");

## Using MSAL

- Make sure you've included MSAL in your app's *build.gradle*.

- Before you can get a token from Azure AD v2.0 or Azure AD B2C, you'll need to register an application. For Azure AD v2.0, use [the app registration portal](https://apps.dev.microsoft.com). For Azure AD B2C, checkout [how to register your app with B2C](https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-app-registration).  

### Requirements
* Android SDK 21+
* [Chrome](https://github.com/Azure-Samples/active-directory-general-docs/blob/master/AndroidEmulator.md) 

### Step 1: Configure the AndroidManifest.xml

1. Give your app Internet permissions

```XML
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

2. Configure your Intent filter, make sure you add your App/Client ID
  
```XML
    <!--Intent filter to capture System Browser calling back to our app after Sign In-->
    <activity
        android:name="com.microsoft.identity.client.BrowserTabActivity">
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="msal<YOUR_CLIENT_ID>"
                android:host="auth" />
        </intent-filter>
    </activity>
```

### Step 2: Instantiate MSAL and Acquire a Token

1.  Create a new PublicClientApplication instance. Make sure to fill in your app/client id

```Java
    PublicClientApplication myApp = new PublicClientApplication(
                    this.getApplicationContext(),
                    CLIENT_ID);
```

2. Acquire a token

```Java
    myApp.acquireToken(this, "User.Read", getAuthInteractiveCallback());
```

### Step 3: Configure the Auth helpers

1. Create an onActivityResult method

```Java
    /* Handles the redirect from the System Browser */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        sampleApp.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }
```

2. Create the getAuthInteractiveCallback method

```Java
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token, use it to call a protected resource */

                String accessToken = authenticationResult.getAccessToken();
            }
            @Override
            public void onError(MsalException exception) { 
                /* Failed to acquireToken */

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }
            @Override
            public void onCancel() {
                /* User canceled the authentication */
            }
        };
    }
```

### Step 4: Use the token!

The access token can now be used in an [HTTP Bearer request](https://github.com/Azure-Samples/active-directory-android-native-v2/blob/master/app/src/main/java/com/danieldobalian/msalandroidapp/MainActivity.java#L152).


