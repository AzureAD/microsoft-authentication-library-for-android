# (Preview) MSAL Android: Requesting Proof of Possession Protected Access Tokens

>**Important:**<br/>Proof of Possession Protected Access Tokens is a **preview** feature for select limited deployment environments. As such, the below-documented APIs are subject to change or removal.<br/><br/>Developers consuming MSAL in their applications are advised to make use of the `Bearer` authentication scheme (default) due to protocol incompatibilties which may arise due to active and evolving development of this feature.

## What is a PoP/AT?
To understand the purpose and benefits offered by PoP, it is important to understand the security posture of a standard Bearer token.

A standard access token from Azure Active Directory (AAD) is a Bearer token in JWT format corresponding to [RFC-7523](https://tools.ietf.org/html/rfc7523). These tokens can be used by anyone possessing the token to access the audience (`aud`) described in the JWT. Because a Bearer token may be used by anyone in possession of it, it is possible that malicious actors may replay tokens that have been leaked by resources or intercepted over the wire from break-and-inspect proxies against a resource to access protected systems or data.

Proof of Possession (PoP) increases the security posture of these tokens embedding them inside of a JWT envelope and signing (binding) that JWT with RSA key material. The key material is generated on the device which was originally issued the tokens and never leaves it. The resulting JWT is called the Signed HTTP Request (SHR). This binding renders the access token unusable without a recent proof for the target resource endpoint.

## How long is a PoP/AT valid?
By default, Bearer access tokens issued by AAD have a 1 hour validity. The validity of the Signed HTTP Request may be shorter, depending on the resource middleware configuration.

When an SHR is created by the client, a timestamp (`ts`) claim is embedded in the JWT. The resource middleware will, upon receipt of the token, inspect its signature and timestamp to ensure integrity (anti-tamper protection) and validity (non-expiry). By default, the SAL middleware honors a 5 minute validity period meaning that once an SHR been signed by the client it may be used for 5 minutes before the resource will require a newly signed access token.

>**Note:**<br/>`AuthenticationResult#getExpiresOn()` does _not_ indicate the expiry/validity period configured on the resource middleware for Signed HTTP Requests. Instead, it reflects the configured expiry period for access tokens emitted by AAD. As such, this value cannot be used to determine whether or not an SHR will be accepted by the middleware. Because of this, `getExpiresOn()` cannot be used to signal cache freshness/eviction criteria; therefore it is recommended that users of MSAL call `acquireTokenSilent()` each time a new SHR is required.

## Can I continue to use Bearer flows in my application?
Yes! PoP and Bearer flows may be used interchangeably with MSAL and with supported broker versions **so long as the targeted resource supports it**.

## Which devices support PoP?
MSAL supports PoP on Android devices running Android 4.3 Jelly Bean (API 18) or higher subject to OEM/device-specific compatibility requirements and API support.

## Configure MSAL for use with PoP
In order to use PoP with MSAL, the `minimum_required_broker_protocol_version` must be `"4.0"` or higher. If your application only makes use of Bearer flows, `"3.0"` will work.

`app_config.json`
```json
{
  "client_id" : "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0",
  "redirect_uri" : "msauth://com.microsoft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D",
  "broker_redirect_uri_registered": true,
  "minimum_required_broker_protocol_version" : "4.0",
  "authorities" : [
    {
      "type": "AAD",
      "audience": {
        "type": "AzureADandPersonalMicrosoftAccount"
      },
      "default": true
    }
  ]
}
```

### Supply Necessary Parameters to `AcquireToken` & `AcquireTokenSilent`
In order to use PoP, additional arguments are required when creating your `AcquireTokenParameters` and `AcquireTokenSilentParameters`.

In the below example, additional parameters are provided via `withAuthenticationScheme()`.

```java
AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
         .startAuthorizationFromActivity(activity)
         .withScopes(...)
         .withLoginHint(....)
         .withPrompt(...)
         .withCallback(...)
         .withAuthenticationScheme(
             PoPAuthenticationScheme.builder()
                 .withMethod(HttpMethod.GET) // The HTTP method used to request the resource
                 .withUrl(url) // The URL of the resource server
                 .withNonce(nonce) // Optional nonce value
                 .build()
         ).build()

AuthenticationResultFuture result = pca.acquireToken(parameters);
```

## Using the resulting Signed HTTP Request
>Note: The following methods are a _subset_ of those provided via MSAL's `AuthenticationResult`. For a complete look at the methods and accessors provided on this class, consult the javadoc available via GitHub.

#### Get the fully formed `Authorization` header
```java
@Override
public void onSuccess(final IAuthenticationResult authenticationResult) {
    // The below value contains both the SHR and the auth scheme as a prefix
    // ex: PoP eyJhbGciOiJSU(...)
    final String authorizationHeader = authenticationResult.getAuthorizationHeader();
}
```

#### Get the SHR
```java
@Override
public void onSuccess(final IAuthenticationResult authenticationResult) {
    // Uses the existing MSAL method, this method returns the SHR minus any scheme-prefix.
    final String shr = authenticationResult.getAccessToken();
}
```

#### Get the auth scheme
```java
@Override
public void onSuccess(final IAuthenticationResult authenticationResult) {
    // Returns "Bearer", "pop", or whichever scheme was used to acquire this token
    // Note that authentication scheme prefixes are case insensitive
    final String scheme = authenticationResult.getAuthenticationScheme();
}
```

## More info
- JSON Web Tokens - [RFC-7523](https://tools.ietf.org/html/rfc7523)
- A Method for Signing HTTP Requests for OAuth - [OAuth Working Group Draft](https://tools.ietf.org/html/draft-ietf-oauth-signed-http-request-03)
