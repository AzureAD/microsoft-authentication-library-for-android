Microsoft Authentication Library (MSAL) Preview for Android
==============================================

| [Getting Started](https://docs.microsoft.com/en-us/azure/active-directory/develop/guidedsetups/active-directory-mobileanddesktopapp-android-intro) | [Sample Code](https://github.com/Azure-Samples/active-directory-android-native-v2) | [Library Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [Support](README.md#community-help-and-support)
| --- | --- | --- | --- |

The MSAL library for Android gives your app the ability to use the [Microsoft Cloud](https://cloud.microsoft.com) by supporting [Microsoft Azure Active Directory](https://azure.microsoft.com/en-us/services/active-directory/) and [Microsoft Accounts](https://account.microsoft.com) in a converged experience using industry standard OAuth2 and OpenID Connect. The library also supports [Azure AD B2C](https://azure.microsoft.com/services/active-directory-b2c/).

[![Version Badge](https://img.shields.io/maven-central/v/com.microsoft.identity.client/msal.svg)](http://repo1.maven.org/maven2/com/microsoft/identity/client/msal/)
[![Build Status](https://travis-ci.org/AzureAD/microsoft-authentication-library-for-android.svg?branch=master)](https://travis-ci.org/AzureAD/microsoft-authentication-library-for-android)


##Introduction

### What's new with this library?

** need help filling this in
  -Features lib supports
  -updates from PP to ga
  -ADAL to MSAL changes

### Recommended Usage

* supported devices, API levels (need help here)


## Installation

For a full example, checkout the [quickstart](https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-v2-android) and [code sample](https://github.com/Azure-Samples/ms-identity-android-java).

### Binaries via Gradle (Recommended way)
** This needs to be updated

Add to your app's build.gradle:

```gradle
    dependencies {
        implementation 'com.microsoft.identity.client:msal:1.0.+'
        }
    }
```

### AAR package inside libs folder
You can get the AAR file from maven central (Link??) and drop it into **libs** folder of your project.

## Using MSAL
** I think a lot of this needs to be updated

- Make sure you've included MSAL in your app's *build.gradle*.
- Before you can get a token from Azure AD v2.0 or Azure AD B2C, you'll need to register an application. To register your app, use [the Azure portal](https://aka.ms/AppRegistrationsPreview). For Azure AD B2C, checkout [how to register your app with B2C](https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-app-registration).  

### Requirements
* Android 21+

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
                    R.raw.auth_config);
```

2. Acquire a token

```Java
    myApp.acquireToken(this, SCOPES, getAuthInteractiveCallback());
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

The access token can now be used to [Call an API](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki/Calling-an-API)).

## Running Tests

** NEED

## Community Help and Support

We use [StackOverflow](http://stackoverflow.com/questions/tagged/msal) with the community to provide support. You should browse existing issues to see if someone has asked about your issue before. If there are workable solutions to your issue then try out those solutions. If not, ask your question and let the community help you out. We're part of the community too and watch for new questions. We help with answers when the community cannot give you a solution.

If you find and bug or have a feature request, please raise the issue on [GitHub Issues](../../issues).

## Contribute

We enthusiastically welcome contributions and feedback. You should clone the repo and start contributing now.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Security Library

This library controls how users sign-in and access services. We recommend you always take the latest version of our library in your app when you can. We use [semantic versioning](http://semver.org) so you can control the risk of updating your app. For example, always downloading the latest minor version number (e.g. x.*y*.x) ensures you get the latest security and feature enhanements with the assurance that our API surface area has not changed. You can always see the latest version and release notes under the **Releases** tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services, please report the issue to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as you can provide. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to **GitHub Issues** or any other public site. We will contact you shortly after receiving your issue report. We encourage you to get new security incident notifications by visiting [Microsoft technical security notifications](https://technet.microsoft.com/en-us/security/dd252948) to subscribe to Security Advisory Alerts.


Copyright (c) Microsoft Corporation.  All rights reserved. Licensed under the MIT License (the "License");
