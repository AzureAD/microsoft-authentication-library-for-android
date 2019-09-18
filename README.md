Microsoft Authentication Library (MSAL) for Android
==============================================

| [Getting Started](https://docs.microsoft.com/en-us/azure/active-directory/develop/guidedsetups/active-directory-mobileanddesktopapp-android-intro) | [Sample Code](https://github.com/Azure-Samples/active-directory-android-native-v2) | [Library Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [Support](README.md#community-help-and-support)
| --- | --- | --- | --- |

The MSAL library for Android gives your app the ability to use the [Microsoft Cloud](https://cloud.microsoft.com) by supporting [Microsoft Azure Active Directory](https://azure.microsoft.com/en-us/services/active-directory/) and [Microsoft accounts](https://account.microsoft.com) in a converged experience using industry standard OAuth2 and OpenID Connect. The library also supports [Azure AD B2C](https://azure.microsoft.com/services/active-directory-b2c/).

[![Version Badge](https://img.shields.io/maven-central/v/com.microsoft.identity.client/msal.svg)](http://repo1.maven.org/maven2/com/microsoft/identity/client/msal/)
[![Build Status](https://travis-ci.org/AzureAD/microsoft-authentication-library-for-android.svg?branch=master)](https://travis-ci.org/AzureAD/microsoft-authentication-library-for-android)


## Introduction

### What's new?

> Note: I suggest that we link to our docs for many of these things

- MSAL Android is now generally available with MSAL 1.0
- Supported Authorities
  - Microsoft identity platform (also known as the Azure Active Directory v2 Endpoint)
  - Azure Active Directory B2C
- Microsoft authentication broker support
  - Supports enterprise scenarios including:
    - Device Registration
    - Device Management
    - Intune App Protection
    - Device Single Sign On
- Introduction of Single and Multi Account Public Client Applications
- IAccount and access to claims
- Support for synchronous methods from worker threads
- Improved configuration and control of your PublicClientApplication using configuration file
- Implemented using AndroidX

### Migrating from ADAL

You can review the ADAL to MSAL migration guide here

### Migrating from preview versions of MSAL

You can review the MSAL Preview to GA release guide here

### Sample

For a complete running sample [link to new sample]()

## Using MSAL

- Before you can get a token from Azure AD v2.0 or Azure AD B2C, you'll need to register an application. To register your app, use [the Azure portal](https://aka.ms/AppRegistrationsPreview). For Azure AD B2C, checkout [how to register your app with B2C](https://docs.microsoft.com/en-us/azure/active-directory-b2c/active-directory-b2c-app-registration).  

### Requirements

- Android API Level 16+

### Step 1: Declare dependency on MSAL

Add to your app's build.gradle:

```gradle
dependencies {
    implementation 'com.microsoft.identity.client:msal:1.0.+'
}
```

### Step 2: Create your MSAL configuration file

>NOTE: Add link to configuration file documentation here

It's simplest to create your configuration file as a "raw" resoruce file in your project resources.  You'll be able to refer to this using the generated resource identifier when constructing an instance of PublicClientApplication.

```javascript
{
  "client_id" : "0984a7b6-bc13-4141-8b0d-8f767e136bb7",
  "redirect_uri" : "msauth://<YOUR_PACKAGE_NAME>/<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>",
  "broker_redirect_uri_registered": true, 
  //Above is the broker redirect URI format.  If you're upgrading an existing app to the latest MSAL and you are not using the format above.  Set this to false.
}
```

>NOTE: This is the minimum required configuration.  MSAL relies on the defaults that ship with the library for all other settings.  Please refer to the configuration file documentation to understand the library defaults.

### Step 3: Configure the AndroidManifest.xml

1. Request the following permissions via the Android Manifest

```XML
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

3. Configure an intent filter in the Android Manifest, using your redirect URI

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
                android:host="/<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>" />
        </intent-filter>
    </activity>
```

### Step 4: Create an MSAL PublicClientApplication

>NOTE: In this example were creating an instance of MultipleAccountPublicClientApplication which is designed to work with apps that allow multiple accounts to be used within the same application.  For more information on multiple vs. single account public client applications click [here](link)

1.  Create a new MultipleAccountPublicClientApplication instance. 

```Java

String[] scopes = {"User.Read"};
IMulitipleAccountPublicClientApplication mMultipleAccountApp = null;
IAccount mFirstAccount = null;

PublicClientApplication.createMultipleAccountPublicClientApplication(getContext(),
    R.raw.msal_config,
    new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
        @Override
        public void onCreated(IMultipleAccountPublicClientApplication application) {
            mMultipleAccountApp = application;
        }

        @Override
        public void onError(MsalException exception) {
            //Log Exception Here
        }
    });
```

2. Acquire a token interactively

```java

mMultipleAccountApp.acquireToken(this, SCOPES, getAuthInteractiveCallback());

private AuthenticationCallback getAuthInteractiveCallback() {
    return new AuthenticationCallback() {
        @Override
        public void onSuccess(AuthenticationResult authenticationResult) {
            /* Successfully got a token, use it to call a protected resource */
            String accessToken = authenticationResult.getAccessToken();
            // Record account used to acquire token
            mFirstAccount = authenticationResult.getAccount();
        }
        @Override
        public void onError(MsalException exception) {
            if (exception instanceof MsalClientException) {
                //And exception from the client (MSAL)
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

3. Acquire a token silently

```java

/*
    Before getting a token silently for the account used to previously acquire a token interactively.  We recommend that you verify that the account is still present in the local cache or on the device in case of brokered auth

    Let's use the synchronous methods here which can only be invoked from a Worker thread
*/

//On a worker thread
IAccount account = mMultipleAccountApp.getAccount(mFirstAccount.getId());

if(account != null){
    //Now that we know the account is still present in the local cache or not the device (broker authentication)

    //Request token silently
    String[] newScopes = {"Calendars.Read"}
    
    //Use default authority to request token from pass null
    IAuthenticationResult result = mMultipleAccountApp.acquireTokenSilent(newScopes, account, null);
}

```

## Community Help and Support

We use [StackOverflow](http://stackoverflow.com/questions/tagged/msal) with the community to provide support. You should browse existing issues to see if someone has asked about your issue before. If there are workable solutions to your issue then try out those solutions. If not, ask your question and let the community help you out. We're part of the community too and watch for new questions. We help with answers when the community cannot give you a solution.

If you find and bug or have a feature request, please raise the issue on [GitHub Issues](../../issues).

## Contribute

We enthusiastically welcome contributions and feedback. You should clone the repo and start contributing now.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

### Clone

MSAL uses a submodule for a "common" library shared by MSAL, ADAL and the Microsoft authentication library

```cmd
git clone https://github.com/AzureAD/microsoft-authentication-library-for-android.git msal
cd msal
git submodule init
git submodule update
```

### MSAL Flavors

```gradle
 flavorDimensions "main"

productFlavors {
    // The 'local' productFlavor sources common from mavenLocal and is intended to be used
    // during development.
    local {
        dimension "main"
        versionNameSuffix "-local"
    }

    // Intended for nightly builds
    snapshot {
        dimension "main"
    }

    // The 'dist' productFlavor sources common from a central repository and is intended
    // to be used for releases.
    dist {
        dimension "main"
    }
}
```

### Build

```cmd
gradlew assembleLocalDebug
```

### Run Tests

>NOTE: This requires a connected device (physical device or Android Emulator)

```cmd
gradlew connectedLocalDebugAndroidTest
```

## Security Library

This library controls how users sign-in and access services. We recommend you always take the latest version of our library in your app when you can. We use [semantic versioning](http://semver.org) so you can control the risk of updating your app. For example, always downloading the latest minor version number (e.g. x.*y*.x) ensures you get the latest security and feature enhanements with the assurance that our API surface area has not changed. You can always see the latest version and release notes under the **Releases** tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services, please report the issue to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as you can provide. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to **GitHub Issues** or any other public site. We will contact you shortly after receiving your issue report. We encourage you to get new security incident notifications by visiting [Microsoft technical security notifications](https://technet.microsoft.com/en-us/security/dd252948) to subscribe to Security Advisory Alerts.


Copyright (c) Microsoft Corporation.  All rights reserved. Licensed under the MIT License (the "License");
