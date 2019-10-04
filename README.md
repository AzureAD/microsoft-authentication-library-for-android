Microsoft Authentication Library (MSAL) for Android
==============================================

| [Getting Started](https://docs.microsoft.com/azure/active-directory/develop/quickstart-v2-android) | [Sample Code](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki/MSAL-Code-Samples)| [Library Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [Support](README.md#community-help-and-support) | [Overview](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki)
| --- | --- | --- | --- | --- |

The MSAL library for Android gives your app the ability to use the [Microsoft Cloud](https://cloud.microsoft.com) by supporting [Microsoft Azure Active Directory](https://azure.microsoft.com/services/active-directory/) and [Microsoft accounts](https://account.microsoft.com) in a converged experience using industry standard OAuth2 and OpenID Connect. The library also supports [Azure AD B2C](https://azure.microsoft.com/services/active-directory-b2c/).

[![Version Badge](https://img.shields.io/maven-central/v/com.microsoft.identity.client/msal.svg)](http://repo1.maven.org/maven2/com/microsoft/identity/client/msal/)
[![Build Status](https://travis-ci.org/AzureAD/microsoft-authentication-library-for-android.svg?branch=master)](https://travis-ci.org/AzureAD/microsoft-authentication-library-for-android)


## Introduction

### What's new?

MSAL Android is now generally available with MSAL 1.0!

- Supported Authorities
  - Microsoft [identity platform](https://docs.microsoft.com/azure/active-directory/develop/) (also known as the Azure Active Directory v2 Endpoint)
  - [Azure Active Directory B2C](https://docs.microsoft.com/azure/active-directory-b2c/)
    - [Integrate with B2C](https://docs.microsoft.com/azure/active-directory/develop/msal-android-b2c)
- Microsoft [authentication broker](https://docs.microsoft.com/azure/active-directory/develop/brokered-auth) support
  - Supports enterprise scenarios including:
    - Device Registration
    - Device Management
    - Intune App Protection
    - Device Single Sign On
- Introduction of [Single and Multi Account](https://docs.microsoft.com/azure/active-directory/develop/single-multi-account) Public Client Applications
- IAccount and access to claims. For more info see [Accounts and tenant profiles](https://docs.microsoft.com/azure/active-directory/develop/accounts-overview)
- Enable Single Sign-On with different [authorization agents](https://docs.microsoft.com/azure/active-directory/develop/authorization-agents)
- Support for synchronous methods from worker threads
- Improved [configuration](https://docs.microsoft.com/azure/active-directory/develop/msal-configuration) and control of your PublicClientApplication using configuration file
- AndroidX Compatible

### Migrating from ADAL

See the [ADAL to MSAL migration guide for Android](https://docs.microsoft.com/azure/active-directory/develop/migrate-android-adal-msal)

### Sample

Run the [quickstart](https://docs.microsoft.com/azure/active-directory/develop/quickstart-v2-android) to see how our Java sample works, or checkout this [list of all MSAL sample repos](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki/MSAL-Code-Samples). 

## Using MSAL

- Before you can get a token from Azure AD v2.0 or Azure AD B2C, you'll need to register an application. To register your app, use [the Azure portal](https://aka.ms/AppRegistrations). For Azure AD B2C, checkout [how to register your app with B2C](https://docs.microsoft.com/azure/active-directory-b2c/active-directory-b2c-app-registration).  

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

[Configuration Documentation](https://docs.microsoft.com/azure/active-directory/develop/msal-configuration)

It's simplest to create your configuration file as a "raw" resource file in your project resources.  You'll be able to refer to this using the generated resource identifier when constructing an instance of PublicClientApplication. If you are registering your app in the portal for the first time, you will also be provided with this config JSON.

```javascript
{
  "client_id" : "<YOUR_CLIENT_ID>",
  "redirect_uri" : "msauth://<YOUR_PACKAGE_NAME>/<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>",
  "broker_redirect_uri_registered": true,
}
```

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
                android:path="/<YOUR_BASE64_URL_ENCODED_PACKAGE_SIGNATURE>" />
        </intent-filter>
    </activity>
```

### Step 4: Create an MSAL PublicClientApplication

>NOTE: In this example we are creating an instance of MultipleAccountPublicClientApplication, which is designed to work with apps that allow multiple accounts to be used within the same application. If you would like to use SingleAccount mode, refer to the [single vs. multi account documentation](https://docs.microsoft.com/azure/active-directory/develop/single-multi-account). You can also check out the [quickstart](https://docs.microsoft.com/azure/active-directory/develop/quickstart-v2-android) for examples of how this is used.

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

## Community Help and Support

We use [StackOverflow](http://stackoverflow.com/questions/tagged/msal) with the community to provide support. You should browse existing issues to see if someone has asked about your issue before. If there are workable solutions to your issue then try out those solutions. If not, ask your question and let the community help you out. We're part of the community too and watch for new questions. We help with answers when the community cannot give you a solution.

If you find and bug or have a feature request, please raise the issue on [GitHub Issues](../../issues).

## Contribute

We enthusiastically welcome contributions and feedback. You should [clone the repo and start contributing now](https://github.com/AzureAD/microsoft-authentication-library-for-android/wiki/MSAL-Contributing).

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments. .

## Roadmap
Date | Release | Blog post| Main features
------| ------- | ---------| ---------
09/30/19 | MSAL 1.0.0| | General Availability of MSAL 

## Security Library

This library controls how users sign-in and access services. We recommend you always take the latest version of our library in your app when you can. We use [semantic versioning](http://semver.org) so you can control the risk of updating your app. For example, always downloading the latest minor version number (e.g. x.*y*.x) ensures you get the latest security and feature enhanements with the assurance that our API surface area has not changed. You can always see the latest version and release notes under the [Releases](https://github.com/AzureAD/microsoft-authentication-library-for-android/releases) tab of GitHub.

## Security Reporting

If you find a security issue with our libraries or services, please report the issue to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as you can provide. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to [GitHub Issues](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues) or any other public site. We will contact you shortly after receiving your issue report. We encourage you to get new security incident notifications by visiting [Microsoft technical security notifications](https://technet.microsoft.com/en-us/security/dd252948) to subscribe to Security Advisory Alerts.


Copyright (c) Microsoft Corporation.  All rights reserved. Licensed under the MIT License (the "License");
