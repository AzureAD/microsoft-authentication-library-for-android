# Public Client Applications and Factory Methods

## Introduction

The primary interface for using the Microsoft Authentication Library (MSAL) is the PublicClientApplication.  The name comes directly from the OAuth2 specification and refers to clients that cannot securely keep secrets.  All mobile applications are considered public clients from an oAuth perspective.  

MSAL Android divides public client applications into 2 sub-types:

- Single Account Public Client Application - Which is intended to be used by applications that only expect one account to be in use at a time
  - For example: Line of business applications
- Multiple Account Public Client Applications - Which is intended to be used by applications that expect their user to have multiple accounts and to want to use multiple accounts at the same time.
  - For example: Email clients

## Factory Methods

PublicClientApplication includes a number of static methods for constructing different public client applications.  These methods support loading configuration from a JSON configuration file.  They can be called synchronously from a background thread or asynchronously from the main thread.

> Note: Static methods are underlined in UML.  See list of factory methods in the class diagram below.

## Class Diagram

![MSAL Component Diagram](https://www.lucidchart.com/publicSegments/view/5a556d03-3098-45e7-b823-0d1df31fae70/image.png)

## Links to Code

### Interfaces

- [IPublicClientApplication](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/IPublicClientApplication.java)
- [ISingleAccountPublicClientApplication](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/ISingleAccountPublicClientApplication.java)
- [IMultipleAccountPublicClientApplication](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/IMultipleAccountPublicClientApplication.java)
- [ApplicationCreatedListener](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/IPublicClientApplication.java)
- [ISingleAccountApplicationCreatedListener](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/IPublicClientApplication.java)
- [IMultipleAccountApplicationCreatedListener](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/IPublicClientApplication.java)
- [ICurrentAccountResult](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/ICurrentAccountResult.java)
- [GetAccountCallback](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/1763b1cc08b503a99da0875c8c6bf20f5b940f79/msal/src/main/java/com/microsoft/identity/client/IMultipleAccountPublicClientApplication.java)
- [RemoveAccountCallback](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/1763b1cc08b503a99da0875c8c6bf20f5b940f79/msal/src/main/java/com/microsoft/identity/client/IMultipleAccountPublicClientApplication.java)
- [CurrentAccountCallback](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/1763b1cc08b503a99da0875c8c6bf20f5b940f79/msal/src/main/java/com/microsoft/identity/client/ISingleAccountPublicClientApplication.java)
- [SignOutCallback](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/1763b1cc08b503a99da0875c8c6bf20f5b940f79/msal/src/main/java/com/microsoft/identity/client/ISingleAccountPublicClientApplication.java)

## Classes

- [PublicClientApplication](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/PublicClientApplication.java)
- [SingleAccountPublicClientApplication](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/SingleAccountPublicClientApplication.java)
- [MultipleAccountPublicClientApplication](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/MultipleAccountPublicClientApplication.java)




