# Microsoft Authentication Library (MSAL) for Android
===========
## Our new SDK is under development!

MSAL for Android is in active development, but not yet ready. We encourage you to look at our work in progress and provide feedback!

**It should not be used in production environments.**

## General
Microsoft Authentication Library(MSAL) provides easy to use autentiation functionality for mobile native app by taking advantage of Azure Active Direct V2 (serves Microsoft Account and AAD) and B2C.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Samples and Documentation

## Feedback and Help

* Issues - [Github issue list](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues).
* Recommendations -- uservoice link
* Help and Question -- stackoverflow link
* FAQ -- link

## Quick Start

### Requirements/Prerequisites
* Minimum supported Android SDK version 21
* Git
* AVD image running (API level 21) or higher
* Android SDK with *ALL* packages installed

### Downloaded

We've made it easy for you to have multiple options to use this library in your Android project:

* You can use the source code to import this library into Android Studio and link to your application.
* If using Android Studio, you can use *aar* package format and reference the binaries.

#### Option 1: Binaries via Gradle (Recommended way)

You can get the binaries from Maven central repo. AAR package can be included as follows in your project in AndroidStudio:

```gradle
repositories {
    mavenCentral()
}
dependencies {
    // your dependencies here...
    compile('com.microsoft.identity:msal:0.1.0') {
        // if your app includes android support
        // libraries or Gson in its dependencies
        // exclude that groupId from ADAL's compile
        // task by un-commenting the appropriate
        // line below

        // exclude group: 'com.android.support'
        // exclude group: 'com.google.code.gson'
    }
}
```

#### Option 2: Source Zip

To download a copy of source code click "Download Zip" on the right side of the page or go to the the current available [releases](https://github.com/AzureAD/microsoft-authentication-library-for-android/releases),
and select the release release version and click on the download source.

#### Option 3: Source via Git

To get the source code of he SDK via git just type:

```
git clone https://github.com/AzureAD/microsoft-authentication-library-for-android.git
cd ./microsoft-authentication-library-for-android/src
```

#### Option 4: aar package inside libs folder
You can get the aar file from maven central and drop into *libs* folder in your project.

### Register APP

### How to use the library

1. Follow the prerequisites and make sure that the app minimum supported version is 21+
2. Follow the Download section and pick you the right way to pull in the sdk.
3. update your project AndroidManifest.xml
* The SDK *requires* the app to have the correct intent configured for BrowserTabActivity, every app integrating the sdk should have the following part configured in manifest
```
<activity
    android:name="com.microsoft.identity.client.BrowserTabActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="msal<clientId>"
              android:host="auth" />
    </intent-filter>
</activity>
```

* There are two ways to provide client id and authority, developer can either provide them through the constructor or manifest. To provide them through manifest,
```
<meta-data
    android:name="com.microsoft.identity.client.ClientId"
    android:value="client-id"/>
```

Authority is optional. If authority is not provided, default authority will be used. To provide the authority through manifest,
```
<meta-data
    android:name="com.microsoft.identity.client.Authority"
    android:value="authority string"/>
```

4. Create PublicClientApplication in your main Activity.

* The following overload will read client id and authority(if applicable) from manifest. Authority will the default one if not provided.
```Java
final PublicClientApplication mApplication = new PublicClientApplication(this.getApplicationContext());
```

* The following overload will take client id and use the default authority
```Java
final PublicClientApplication mApplication = new PublicClientApplication(this.getApplicationContext, clientid);
```

* The following overload will take in both client id and authority
```Java
final PublicClientApplication mApplication = new PublicClientApplication(this.getApplicationContext, clientid, authority);
```

5. Acquire token Interactively
Authorization code grant will be used, user will be prompted for username and password. If prompt behavior is not forceLogin, cookies will be used if available.

* Call acquireToken API, which requires an activity to be passed in.
* Copy the following code block to forward the authorization code from AuthenticationActivity after user enters credentials. Activity passed in for acquireToken API call needs to
include the following code in the activity's onActivityResult
     ```Java
     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         if (mApplication != null) {
             mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
         }
     }
     ```

6. Acquire Token silently

AcquireTokenSilent will use refresh token grant. It will look into the cache and try to get a token without prompting the user.

7. Register callback and correctly handle the callback.
```Java
new AuthenticationCallback() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                // AuthenticationResult contains
                // accessToken, expiresOn, rawIdToken, User, tenant id, etc.
            }

            @Override
            public void onError(MsalException exception) {
                // Check the exception type.
                if (exception instanceof MsalClientException) {
                    // This means errors happened in the sdk itself, could be network, Json parse, etc. Check MsalError.java
                    // for detailed list of the errors.
                } else if (exception instanceof MsalServiceException) {
                    // This means something is wrong when the sdk is communication to the service, mostly likely it's the client
                    // configuration.
                } else if (exception instanceof MsalUiRequiredException) {
                    // This explicitly indicates that developer needs to prompt the user, it could be refresh token is expired, revoked
                    // or user changes the password; or it could be that no token was found in the token cache.
                    callAcquireToken(mScopes, mUiBehavior, mLoginHint, mExtraQp, mAdditionalScope);
                }
            }

            @Override
            public void onCancel() {
                showMessage("User cancelled the flow.");
            }
        };
```

8. Diagnostics
You can configure the library to generate log messages that you can use to help diagnose issues. You can configure logging by making the following call to configure callback that MSAL wll use to hand off each logs messages as it's generated.
```Java
Logger.getInstance().setExternalLogger(new ILoggerCallback() {
            @Override
            public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                // contains PII indicates that if the log message contains PII information. If PII logging is
                // disabled, the sdk never returns back logs with Pii.
```

Check out [Diagnostics](link) for more info on logging.

## Security Reporting

If you find a security issue with our libraries or services please report it to [secure@microsoft.com](mailto:secure@microsoft.com) with as much detail as possible. Your submission may be eligible for a bounty through the [Microsoft Bounty](http://aka.ms/bugbounty) program. Please do not post security issues to GitHub Issues or any other public site. We will contact you shortly upon receiving the information. We encourage you to get notifications of when security incidents occur by visiting [this page](https://technet.microsoft.com/en-us/security/dd252948) and subscribing to Security Advisory Alerts.

## License

Copyright (c) Microsoft Corporation.  All rights reserved. Licensed under the MIT License (the "License");

## We Value and Adhere to the Microsoft Open Source Code of Conduct

