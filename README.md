# Microsoft Authentication Library (MSAL) for Android
===========
## Our new SDK is under development!

MSAL for Android is in active development, but not yet ready. We encourage you to look at our work in progress and provide feedback!

**It should not be used in production environments.**

## General
Microsoft Authentication Library(MSAL) provides easy to use authentication functionality for native mobile apps by taking advantage of Azure Active Directory V2 (serves Microsoft Account (MSA) and AAD) and B2C.

// TODO: once we have msal on maven, should pull in the latest version from maven

[![Maven Central](https://img.shields.io/maven-central/v/com.microsoft.aad/adal.svg)](http://repo1.maven.org/maven2/com/microsoft/aad/adal/)

## Feedback and Help

TODO: fill in all the links.
* Issues - [Github issue list](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues)
* Recommendations -- [UserVoice](https://feedback.azure.com/forums/169401-azure-active-directory)
* Help and Question -- [Stack Overflow](http://stackoverflow.com/questions/tagged/azure-active-directory)
* FAQ -- link

## Quick Start

### Requirements/Prerequisites
* Android SDK version 21+

### Downloaded

We've made it easy for you to have multiple options to use this library in your Android project.

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

To download a copy of source code, visit our [releases page](https://github.com/AzureAD/microsoft-authentication-library-for-android/releases).

#### Option 3: Source via Git

To download the resource using git:

```
git clone https://github.com/AzureAD/microsoft-authentication-library-for-android.git
cd ./microsoft-authentication-library-for-android/src
```

#### Option 4: aar package inside libs folder
You can get the aar file from maven central and drop into **libs** folder in your project.

### Register APP

### How to use the library

1. Update your project AndroidManifest.xml
* The SDK *requires* the app to have the correct intent configured for BrowserTabActivity. Every app integrating the sdk should have the following part configured in manifest
```xml
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

* There are two ways to provide client id and authority, developer can either provide them through the constructor or manifest. To provide them through manifest,
```xml
<meta-data
    android:name="com.microsoft.identity.client.ClientId"
    android:value="YOUR_CLIENT_ID"/>
```

Authority is optional. If authority is not provided, default authority will be used. To provide the authority through manifest,
```xml
<meta-data
    android:name="com.microsoft.identity.client.Authority"
    android:value="authority string"/>
```

2. Create PublicClientApplication.

* The following overload will read client id and authority(if applicable) from manifest. Default authority will be used if value is not provided.
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

3. Acquire token interactively
Authorization code grant will be used, user will be prompted for username and password. If prompt behavior is not forceLogin, cookies will be used if available.

* Call acquireToken API, which requires an activity to be passed in.
* Copy the following code block to forward the authorization code from AuthenticationActivity after user enters credentials. Activity passed in for acquireToken API call needs to include the following code in the activity's onActivityResult
     ```Java
     @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
         super.onActivityResult(requestCode, resultCode, data);
         if (mApplication != null) {
             mApplication.handleInteractiveRequestRedirect(requestCode, resultCode, data);
         }
     }
     ```

4. Acquire token silently
AcquireTokenSilent will use refresh token grant. It will look into the cache and try to get a token without prompting the user.

5. Register callback and correctly handle the callback.
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

6. Diagnostics
You can configure the library to generate log messages that you can use to help diagnose issues. You can configure logging by making the following call to configure callback that MSAL will use to hand off each logs messages as it is.
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

## We Value and Adhere to the Microsoft Open Source Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.



