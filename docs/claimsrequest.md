# Claims Request

## Introduction

You can request specific claims be returned in the id_token and userinfo endpoint responses.  In addition the Microsoft identity platform also allows specific claims to be returned in access tokens.  

The claims request parameter is described in detail in the following section of the OpenId Connect specification: [Requesting Claims using the "claims" Request Parameter](https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter)

MSAL provides objects for creating, searializing and deserializing claims requests.

> Microsoft identity platform Conditional Access results in the possibility of resources (APIs) requesting specific claims in their 401 WWW-Authenticate header/challenge.  MSAL provides a helper class from parsing a claims request parameter from the "claims" directive in the WWW-authenticate header value.

## Class Diagram

![Claims Request Parameter](https://www.lucidchart.com/publicSegments/view/99a5cbfd-7b04-4693-bb11-52bb506c6932/image.png)

## Links to Code

### Classes

- [ClaimsRequest](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/claims/ClaimsRequest.java)
- [RequestedClaim](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/claims/RequestedClaim.java)
- [RequestedClaimAdditionalInformation](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/claims/RequestedClaimAdditionalInformation.java)
- [WWWAuthenticateHeader](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/claims/WWWAuthenticateHeader.java)