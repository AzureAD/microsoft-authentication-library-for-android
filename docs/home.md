# Design documentation for the Microsoft Authentication Library (MSAL) and Common library

## Introduction

This documentation provides orientation for new contributors to understand the design and design goals of the MSAL and the Common library supporting both MSAL and the Microsoft authentication broker libraries.  

> NOTE: The Microsoft authentication broker library is currently private.

## Android Libraries

MSAL consists of 2 Android Libraries/Packages:

- com.microsoft.identity.client.msal
  - github: [https://github.com/AzureAD/microsoft-authentication-library-for-android](https://github.com/AzureAD/microsoft-authentication-library-for-android)
- com.microsoft.identity.common
  - github: [https://github.com/AzureAD/microsoft-authentication-library-common-for-android](https://github.com/AzureAD/microsoft-authentication-library-common-for-android)

Where MSAL depends on common for definition of abstractions and of the identity provider specific implementation.

## Component Overview Diagram

The following component diagram illustrates the components of the library and the dependencies between them.  Abstract components are marked as with the "abstract" annotation.  Each component and group of components will be covered in more detail in separate documents linked below.

![MSAL Component Diagram](https://www.lucidchart.com/publicSegments/view/88a1bca3-18ab-4eac-aa4f-ba85d4d214f6/image.png)

## Component Detailed Documentation

### MSAL & Common

- [Public Client Application Configuration](configuration.md)
- [PublicClientApplication Factory Methods & Subtypes](publicclientapplication.md)
- [MsalException & Subtypes](msalexception.md)
- [Parameters and Command Parameter Adapters](parameters.md)
- [Commands & Command Dispatcher & Throttling](commands.md)
- [Controllers](controllers.md)
- [Identity Providers & Authorities](authorities.md)
- [OAuth2 Strategies](oauth2strategies.md)
- [Authorization Strategies](authorizationstrategies.md)
- [Broker Client Strategies](brokerstrategies.md)
- [Token Cache](tokencache.md)
- [Logging](logging.md)
- [Telemetry](telemetry.md)
- [Unit and Integration Tests with Robolectric](testing.md)
