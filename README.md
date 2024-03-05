# Microsoft Authentication Library (MSAL) for Android

| Documentation                  | Sample Code               | Library Reference | Support |
|-------------------------------|---------------------------|-------------------|---------|
| [MSAL Android documentation](https://learn.microsoft.com/en-us/entra/msal/android/) | [Microsoft Entra ID (workforce samples)](https://learn.microsoft.com/en-us/entra/identity-platform/sample-v2-code?tabs=apptype#mobile) <br/> [Microsoft Entra External ID (customer samples)](https://learn.microsoft.com/en-us/entra/identity-platform/sample-v2-code?tabs=apptype#mobile)          | [ MSAL Android reference](http://javadoc.io/doc/com.microsoft.identity.client/msal)              | [Get support](README.md#community-help-and-support)     |

## Overview

MSAL Android is a library that enables Android applications to authenticate users with Microsoft identity platform (formerly Azure Active Directory) and access protected web APIs using OAuth2 and OpenID Connect protocols.  The Microsoft Authentication Library (MSAL) for Android enables developers to acquire security tokens from the Microsoft identity platform to authenticate users and access secure web APIs for their Android based applications.
 
MSAL Android supports multiple authentication scenarios, such as single sign-on (SSO), conditional access, and brokered authentication. MSAL Android also provides native authentication APIs that allow applications to implement a native experience with end-to-end customizable flows. 

[![Version Badge](https://img.shields.io/maven-central/v/com.microsoft.identity.client/msal.svg)](https://repo1.maven.org/maven2/com/microsoft/identity/client/msal/)

## Migrating from ADAL

The Azure Active Directory Authentication Library (ADAL) for Android was deprecated on June 2023. Follow the [ADAL to MSAL migration guide for Android](https://docs.microsoft.com/azure/active-directory/develop/migrate-android-adal-msal) to avoid putting your app's security at risk.. 

## Getting started

To use MSAL Android in your application, you need to register your application in the Microsoft Entra Admin center and configure your Android project. Since MSAL Android supports both browser-delegated and native authentication experiences, follow the steps in the following tutorials based on your scenario.

* For browser-delegated scenarios, refer to the quickstart, [Sign in users and call Microsoft Graph from an Android app](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-mobile-app-android-sign-in).

* For Native Authentication scenarios, refer to the Microsoft Entra External ID sample guide, [Run Android Kotlin sample app](https://review.learn.microsoft.com/en-us/entra/external-id/customers/tutorial-native-authentication-prepare-android-app?branch=release-native-auth-public-preview).


## Using MSAL Android

### Requirements

- Min SDK Version 16+
- Target SDK Version 33+


### Step 1: Declare dependency on MSAL

Add the following dependencies to your app's build.gradle:

**For browser-delegated authentication:**

```gradle
dependencies {
    implementation 'com.microsoft.identity.client:msal:4.9.+'
}
```

Please also add the following lines to your repositories section in your gradle script:

```gradle
maven { 
    url 'https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1' 
}
```

**For Native authentication:**


```java
dependencies {
implementation 'com.microsoft.identity.client:msal:5.1.0'
}
maven {
            url 'https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1'
            name 'Duo-SDK-Feed'
        }
```

### Step 2: Create your MSAL configuration file

**Browser-delegated authentication:**

It's simplest to create your configuration file as a "raw" resource file in your project resources. You'll be able to refer to this using the generated resource identifier when constructing an instance of PublicClientApplication. If you are registering your app in the Microsoft Entra admin center for the first time, you will also be provided with the detailed MSAL [Android configuration file](https://learn.microsoft.com/en-us/entra/msal/android/msal-configuration)

```javascript
{
  "client_id" : "<YOUR_CLIENT_ID>",
  "redirect_uri" : "msauth://<YOUR_PACKAGE_NAME>/<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>",
  "broker_redirect_uri_registered": true,
}
```

In the `redirect_uri`, the `<YOUR_PACKAGE_NAME>` refers to the package name returned by the `context.getPackageName()` method. This package name is the same as the [`application_id`](https://developer.android.com/studio/build/application-id) defined in your `build.gradle` file.

The values above are the minimum required configuration.  MSAL relies on the defaults that ship with the library for all other settings.  Please refer to the [MSAL Android configuration file documentation](https://learn.microsoft.com/en-us/entra/msal/android/msal-configuration) to understand the library defaults.

**For Native authentication:** 

1. Right-click res and choose New > Directory. Enter raw as the new directory name and select OK.
1. In this new folder (app > src > main > res > raw), create a new JSON file called auth_config_native_auth.json and paste the following template MSAL Configuration:

```
{ 
  "client_id": "Enter_the_Application_Id_Here", 
  "authorities": [ 
    { 
      "type": "CIAM", 
      "authority_url": "https://Enter_the_Tenant_Subdomain_Here.ciamlogin.com/Enter_the_Tenant_Subdomain_Here.onmicrosoft.com/" 
    } 
  ], 
  "challenge_types": ["oob"], 
  "logging": { 
    "pii_enabled": false, 
    "log_level": "INFO", 
    "logcat_enabled": true 
  } 
 }
```

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

>NOTE: Please refer to the [frequently asked questions](https://learn.microsoft.com/en-us/entra/msal/android/frequently-asked-questions) for more information on common redirect uri issues.

### Step 4: Create an MSAL PublicClientApplication

>NOTE: In this example we are creating an instance of MultipleAccountPublicClientApplication, which is designed to work with apps that allow multiple accounts to be used within the same application. If you would like to use SingleAccount mode, refer to the [single vs. multi account documentation](https://docs.microsoft.com/azure/active-directory/develop/single-multi-account). You can also check out the [quickstart](https://docs.microsoft.com/azure/active-directory/develop/quickstart-v2-android) for examples of how this is used.

1.  Create a new MultipleAccountPublicClientApplication instance.

```Java

String[] scopes = {"User.Read"};
IMultipleAccountPublicClientApplication mMultipleAccountApp = null;
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
        public void onSuccess(IAuthenticationResult authenticationResult) {
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
    Before getting a token silently for the account used to previously acquire a token interactively, we recommend that you verify that the account is still present in the local cache or on the device in case of brokered auth

    Let's use the synchronous methods here which can only be invoked from a Worker thread
*/

//On a worker thread
IAccount account = mMultipleAccountApp.getAccount(mFirstAccount.getId());

if(account != null){
    //Now that we know the account is still present in the local cache or not the device (broker authentication)

    //Request token silently
    String[] newScopes = {"Calendars.Read"};
    
    String authority = mMultipleAccountApp.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

    //Use default authority to request token from pass null
    IAuthenticationResult result = mMultipleAccountApp.acquireTokenSilent(newScopes, account, authority);
}

```

## ProGuard
MSAL uses reflection and generic type information stored in `.class` files at runtime to support various persistence and serialization related functionalities. Accordingly, library support for minification and obfuscation is limited. A default configuration is shipped with this library; please [file an issue](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/new/choose) if you find any issues.

## Community Help and Support

We use [StackOverflow](http://stackoverflow.com/questions/tagged/msal) with the community to provide support. You should browse existing issues to see if someone has asked about your issue before. If there are workable solutions to your issue then try out those solutions. If not, ask your question and let the community help you out. We're part of the community too and watch for new questions. We help with answers when the community cannot give you a solution.

If you find and bug or have a feature request, please raise the issue on [GitHub Issues](../../issues).

## Submit Feedback
We'd like your thoughts on this library. Please complete [this short survey](https://forms.office.com/r/3J8pAAqAcj).

## Contribute

We enthusiastically welcome contributions and feedback. You should [clone the repo and start contributing now](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki/MSAL-Contributing).

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Android Studio Build Requirement
Please note that this project uses [Lombok](https://projectlombok.org/) internally and while using Android Studio you will need to install [Lombmok Plugin](https://plugins.jetbrains.com/plugin/6317-lombok) to get the project to build successfully within Android Studio.


## Recommendation

MSAL is a security library. It controls how users sign-in and access services. We recommend you always take the latest version of our library in your app when you can. We use [semantic versioning](http://semver.org) so you can control the risk of updating your app. For example, always downloading the latest minor version number (e.g. x.*y*.x) ensures you get the latest security and feature enhanements with the assurance that our API surface area has not changed. You can always see the latest version and release notes under the [Releases](https://github.com/AzureAD/microsoft-authentication-library-for-android/releases) tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services, please report the issue to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as you can provide. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to [GitHub Issues](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues) or any other public site. We will contact you shortly after receiving your issue report. We encourage you to get new security incident notifications by visiting [Microsoft technical security notifications](https://technet.microsoft.com/en-us/security/dd252948) to subscribe to Security Advisory Alerts.


Copyright (c) Microsoft Corporation.  All rights reserved. Licensed under the MIT License (the "License");
