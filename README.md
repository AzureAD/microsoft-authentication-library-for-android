# Microsoft Authentication Library (MSAL) for Android
============================================================

| [Getting Started](https://apps.dev.microsoft.com/portal/register-app?appType=mobileAndDesktopApp&appTech=android) | [Sample Code](https://github.com/Azure-Samples/active-directory-android-native-v2) | [API Reference](http://javadoc.io/doc/com.microsoft.identity.client/msal) | [Support](README.md#community-help-and-support)
| --- | --- | --- | --- |

## General
The MSAL library for Android gives your app the ability to begin using the [Microsoft Cloud](https://cloud.microsoft.com) by supporting [Microsoft Azure Active Directory](https://azure.microsoft.com/en-us/services/active-directory/) and [Microsoft Accounts](https://account.microsoft.com) in a converged experience using industry standard OAuth2 and OpenID Connect. The library also supports [Azure AD B2C](https://azure.microsoft.com/services/active-directory-b2c/).

## Example


    PublicClientApplication pApp = new PublicClientApplication(
                    this.getApplicationContext(),
                    CLIENT_ID);
    pApp.acquireToken(getActivity(), SCOPES, getAuthInteractiveCallback());

    // ...

    authenticationResult.getAccessToken();


For a full example of basic usage, checkout our [code sample](https://github.com/Azure-Samples/active-directory-android-native-v2).

### Requirements
* Android SDK 21+
* Chrome 


### Installation

We've made it easy for you to have multiple options to use this library in your Android project.

#### Option 1: Binaries via Gradle (Recommended way)

You can get the binaries from Maven central repo. AAR package can be included as follows in your project in AndroidStudio:

```gradle
repositories {
    mavenCentral()
}
dependencies {
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

#### AAR package inside libs folder
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


