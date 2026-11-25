# Claims Request

## Introduction

You can request specific claims be returned in the id_token and userinfo endpoint responses.  In addition the Microsoft identity platform also allows specific claims to be returned in access tokens.  

The claims request parameter is described in detail in the following section of the OpenId Connect specification: [Requesting Claims using the "claims" Request Parameter](https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter)

MSAL provides objects for creating, searializing and deserializing claims requests.

> Microsoft identity platform Conditional Access results in the possibility of resources (APIs) requesting specific claims in their 401 WWW-Authenticate header/challenge.  MSAL provides a helper class from parsing a claims request parameter from the "claims" directive in the WWW-authenticate header value.

## Class Diagram

![Claims Request Parameter](https://www.lucidchart.com/publicSegments/view/99a5cbfd-7b04-4693-bb11-52bb506c6932/image.png)

## Native Auth V1 MFA and registration state

V1 sign-in keeps the original claims request through in-memory MFA and strong-authentication
registration (JIT), including retries and rechallenges. A captured request with no claims
is different from a state whose original request is unknown.

The existing Parcelable format is unchanged and does **not** persist that request context.
After a real Parcel roundtrip, both previously saved and newly saved MFA/JIT states therefore
return `invalid_state` when used, even if the original request had no claims. The same applies
to MFA/JIT states created directly through their public constructors without request provenance.
The app must discard the state and explicitly restart sign-in with the intended claims;
the SDK does not retry automatically or silently continue without them.

This change does not add saved-state migration or client-configuration rebinding. It does not
change the code/password/continuation Parcel formats or Native Auth V2.

## Links to Code

### Classes

- [ClaimsRequest](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/claims/ClaimsRequest.java)
- [RequestedClaim](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/claims/RequestedClaim.java)
- [RequestedClaimAdditionalInformation](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/claims/RequestedClaimAdditionalInformation.java)
- [WWWAuthenticateHeader](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/msal/src/main/java/com/microsoft/identity/client/claims/WWWAuthenticateHeader.java)