# Microsoft Authentication Library (MSAL) for Android

| Documentation                  | Sample Code               | Library Reference | Support |
|-------------------------------|---------------------------|-------------------|---------|
| [MSAL Android documentation](https://learn.microsoft.com/en-us/entra/msal/android/) | &#8226;  [Microsoft Entra ID (workforce samples)](https://learn.microsoft.com/en-us/entra/identity-platform/sample-v2-code?tabs=apptype#mobile)<br/>&#8226; [Microsoft Entra External ID (customer samples)](https://learn.microsoft.com/en-us/entra/external-id/customers/samples-ciam-all?tabs=apptype#mobile)          | [ MSAL Android reference](http://javadoc.io/doc/com.microsoft.identity.client/msal)              | [Get support](README.md#community-help-and-support)     |


[![Version Badge](https://img.shields.io/maven-central/v/com.microsoft.identity.client/msal.svg)](https://repo1.maven.org/maven2/com/microsoft/identity/client/msal/)

## Overview

The Microsoft Authentication Library (MSAL) for Android is an auth SDK that can be used to seamlessly integrate authentication into your apps using industry standard OAuth2 and OpenID Connect protocols. It allows you to sign in users or apps with Microsoft identities. These identities include Microsoft Entra ID work and school accounts, Microsoft personal accounts, social accounts, and customer accounts.

The Microsoft Authentication Library (MSAL) for Android enables developers to acquire tokens from the Microsoft identity platform using OAuth2 and OpenID Connect protocol to authenticate users and access secure web APIs for their Android based applications. The library supports multiple authentication scenarios such as [single sign-on](https://learn.microsoft.com/en-us/entra/msal/android/single-sign-on) (SSO), brokered authentication, and [Conditional Access](https://learn.microsoft.com/en-us/entra/identity-platform/developer-guide-conditional-access-authentication-context).

#### Native authentication support in MSAL

By default, MSAL uses the standard, browser-delegated authentication flow where you rely on the user's browser to handle the sign-in experience. This browser-based experience is the default authentication method for work and school accounts as well as personal Microsoft accounts.

To support Microsoft Entra External ID scenarios, MSAL Android now offers Native authentication that allows you to customize the sign-in experiences within your mobile app. With native authentication, users are guided through a rich, native, mobile-first sign-up and sign-in journey without leaving the app. The native authentication feature is only available for mobile apps on [External ID for customers](https://learn.microsoft.com/en-us/entra/external-id/customers/concept-native-authentication). 

When implementing authentication for mobile apps on External ID, you can choose between browser-delegated authentication and native authentication. In browser-delegated authentication, users are taken to the browser for authentication and then redirected back to the app when the sign-in process is complete. Learn how you can [choose the right authentication option](https://learn.microsoft.com/en-us/entra/external-id/customers/concept-native-authentication#when-to-use-native-authentication) for your mobile app. 

## Getting started

To use MSAL Android in your application, you need to register your application in the Microsoft Entra Admin center and configure your Android project. Since MSAL Android supports both browser-delegated and native authentication experiences, follow the steps in the following tutorials based on your scenario.

* For browser-delegated authentication scenarios, refer to the quickstart, [Sign in users and call Microsoft Graph from an Android app](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-mobile-app-android-sign-in).

* For native authentication scenarios, refer to the Microsoft Entra External ID sample guide, [Tutorial: Prepare your Android app for native authentication](https://learn.microsoft.com/en-us/entra/external-id/customers/tutorial-native-authentication-prepare-android-app).


## Migrating from ADAL

The Azure Active Directory Authentication Library (ADAL) for Android has been deprecated effective June 2023. Follow the [ADAL to MSAL migration guide for Android](https://docs.microsoft.com/azure/active-directory/develop/migrate-android-adal-msal) to avoid putting your app's security at risk.

## Using MSAL Android

### Requirements

- Min SDK Version 16+
- Target SDK Version 33+


### Step 1: Declare dependency on MSAL

Add the following dependencies to your app's build.gradle:

```gradle
dependencies {
    implementation 'com.microsoft.identity.client:msal:5.1.0'
}
maven {
    url 'https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1'
    name 'Duo-SDK-Feed'
}
```

Note: We recommend using the latest version of our library when setting up your application. Check the [MSAL Android Releases](https://github.com/AzureAD/microsoft-authentication-library-for-android/releases) page.

### Step 2: Create your MSAL configuration file

**Browser-delegated authentication:**

Create your configuration file as a "raw" resource in your project. Refer to it using the generated resource identifier when constructing a `PublicClientApplication` instance. If you're registering your app in the Microsoft Entra admin center for the first time, you'll also be provided with the detailed MSAL [Android configuration file](https://learn.microsoft.com/en-us/entra/msal/android/msal-configuration)

```json
{
  "client_id" : "<YOUR_CLIENT_ID>",
  "redirect_uri" : "msauth://<YOUR_PACKAGE_NAME>/<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>",
  "broker_redirect_uri_registered": true,
}
```

In the `redirect_uri`, the `<YOUR_PACKAGE_NAME>` refers to the package name returned by the `context.getPackageName()` method. This package name is the same as the [`application_id`](https://developer.android.com/studio/build/application-id) defined in your `build.gradle` file.

The values above are the minimum required configuration.  MSAL relies on the defaults that ship with the library for all other settings.  Please refer to the [MSAL Android configuration file documentation](https://learn.microsoft.com/en-us/entra/msal/android/msal-configuration) to understand the library defaults.

**Native authentication:** 

1. Right-click res and choose New > Directory. Enter raw as the new directory name and select OK.
1. In this new folder (app > src > main > res > raw), create a new JSON file called auth_config_native_auth.json and paste the following template MSAL Configuration:

```json
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

### Step 3: Configure the AndroidManifest.xml for browser-delegated authentication

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

>NOTE: Please refer to the [MSAL Android FAQ](https://learn.microsoft.com/en-us/entra/msal/android/frequently-asked-questions) for more information on common redirect uri issues.

### Step 4: Create an MSAL PublicClientApplication

**Browser-delegated authentication**

For browser-delegated authentication, you create an instance of the PublicClientApplication, before you can acquire a token silently or interactively.

```java
PublicClientApplication.createMultipleAccountPublicClientApplication(getContext(),
    R.raw.msal_config,
    new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
        @Override
        public void onCreated(IMultipleAccountPublicClientApplication application) {
            mMultipleAccountApp = application;
        }
   }
```

Learn how to [instantiate your client application and acquire tokens](https://learn.microsoft.com/en-us/entra/msal/android/acquire-tokens) in the official MSAL Android documentation. 


**Native authentication**

For native authentication, you create an instance of the client application as follows:

```kotlin
  authClient = PublicClientApplication.createNativeAuthPublicClientApplication( 
      this, 
      R.raw.auth_config_native_auth 
  )
```

Learn more by following the [Native auth Android app tutorial](https://learn.microsoft.com/en-us/entra/external-id/customers/tutorial-native-authentication-prepare-android-app#create-sdk-instance).

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
