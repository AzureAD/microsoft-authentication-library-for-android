# Token Cache

## Introduction

The token cache implementation for MSAL, ADAL and Android broker is found in the Android Common Library.  

## References 

- Universal Cache Schema

## Universal cache and protocol response handling

When looking at the token cache there are really 2 levels to it.  The lower level is based on the universal cache schema referenced above.  The higher level deals with the result of a particular protocol request and mapping that to the universal schema.  

So in effect we get an oAuth 2 Token Response, which in the case of MSAL includes an ID Token and an Access Token and we translate:

ID Token & Client_Info -> Account & Tenant Profile
Access Token -> Credential
Refresh Token -> Credential

## Universal Schema at a high level / OAuth v2 Mapping

- Accounts
  - ID Token of account home tenant
- Tenant Profiles - The representation of 1 account in 1 or more tenants (external account guest/member scenarios)
  - ID Token of the account home tenant
  - ID Token from other tenants that the account is a member of
- Credentials
  - Id Token
  - Access Tokens
  - Refresh Tokens
  - etc....

### Relationships

1 Account has 1 or more credentials (minimum, the `AccountRecord` + `RefreshTokenRecord`)
1 Credential belongs to 1 account

## OAuth 2 Token Cache

This section describes the base class for the higher level of the TokenCache.  The level responsible for translated an oAuth response into universal cache schema artifacts.

There are different implementations of the cache based on the library using it and the scenarios it needs to support.

> NOTE: I'm going to ignore the ADALOAuth2TokenCache in this document. It deals with interacting with the legacy ADAL token cache.

### Abstract base class

The abstract base class for all caches is the OAuth2TokenCache.  The physical schema of the cache in terms of account and credential records is decoupled from the protocol used to acquire the account and credential; however the cache interface knows about oAuth2 token responses and how to convert those to accounts and crentials and persist them to storage.

```java
//Example
/**
     * Saves the credentials and tokens returned by the service to the cache.
     *
     * @param oAuth2Strategy The strategy used to create the token request.
     * @param request        The request used to acquire tokens and credentials.
     * @param response       The response received from the IdP/STS.
     * @return The {@link ICacheRecord} containing the Account + Credentials saved to the cache.
     * @throws ClientException If tokens cannot be successfully saved.
     */
    public abstract ICacheRecord save(final T oAuth2Strategy,
                                      final U request,
                                      final V response) throws ClientException;
```

### MSALOAuth2TokenCache

The simplest version of the cache is the MSALOAuth2TokenCache.  There is one instance of this cache per PublicClientApplication created using MSAL within a client android application.  The important thing to note is that there is 1 cache per public client application identified by it's client id.

### Family of Client Ids (Special Case)

Applications published by Microsoft may be authorized to share refresh tokens with one another.  All applications that are authorized to share tokens share a single MSALOAuth2TokenCache instance.  They effectively share a client id, which in this case is the family id (generally 1 is used to represent the family)

### BrokerOAuth2TokenCache

The broker token cache is effectively a wrapper around individiual caches that exist for each client id and the cache that exists for the Microsoft family of clients.  In addition there's a reference to the application metadata cache which tracks which client id/packagename/signature/calling process UI is a member of the Microsoft familiy.  

## Universal Cache Implementation

### AccountCredentialBase

Abstract base class for accounts and credential objects.  The functionality that is common between all objects in the universal schema is the ability to append additional information to both accounts and credentials hence the methods for getting and setting the additional fields are defined here.

### AccountRecord

"Record" is appended in this case to avoid confusion with the public API account object.  This is where you will find all of the information about an account.  This is the object that will be serialized to storage as an Account.

### Credential

This is an abstract base class for all types of credentials stored in the universal cache.  This includes common fields that exist on all types of credentials.  

### AccessTokenRecord

Inherits from Credential and contains additional fields specific to access tokens.

### RefreshTokenRecord

Inherits from Credential and contains additioonal fields specific to refresh tokens.

### PrimaryRefreshTokenRecord

Inherits from Credential and contains additional fields specific to the primary refresh token.

### IdTokenRecord

Inherits from Credential and contains additional fields specific to id tokens.  

> NOTE: An id token can be validated to authenticate a user.  Generally MSAL is an oAuth library and primarily concerned with authorization and access tokens; however because we perform an id token request along with every access token request.  The id token is also a credential.

### Cache Key/Value Generation

In order to store accounts and credentials in shared preferences first an identifier for the account and credential must be generated and the serialization of the account attributes/properties must be performed.  This is currently handled by the CacheKeyValueDelegate class which implements ICacheKeyValueDelegate interface.

```java
public interface ICacheKeyValueDelegate {
    /**
     * Generate cache key for a specific account.
     *
     * @param account Account
     * @return String
     */
    String generateCacheKey(final AccountRecord account);

    /**
     * Generate cache value for a specific account.
     *
     * @param account Account
     * @return String
     */
    String generateCacheValue(final AccountRecord account);

    /**
     * Generate cache key from the credential.
     *
     * @param credential Credential
     * @return String
     */
    String generateCacheKey(final Credential credential);

    /**
     * Generate cache value from the credential.
     *
     * @param credential Credential
     * @return String
     */
    String generateCacheValue(final Credential credential);

    /**
     * Get the account credential from cache value.
     *
     * @param string String
     * @param t      AccountCredentialBase
     * @param <T>    Generic type
     * @return AccountCredentialBase
     */
    <T extends AccountCredentialBase> T fromCacheValue(final String string, Class<? extends AccountCredentialBase> t); // TODO consider throwing an Exception if parsing fails
```

## Storage

Accounts and credentials are serialized and stored using the lowest common denominator storage method available on Andorid... SharedPreferences.  

### IAccountCredentialCache / AbstractAccountCredentialCache / SharedPreferencesAccountCredentialCache

SharedPreferencesAccountCredentialCache inherits from AbstractAccountCredentialCache which in turn implements IAccountCredentialCache.  

IAccountCredentialCache defines the following methods for CRUD and search/filter operations over the physical cache:

```java
/**
 * Account & Credential cache interface.
 */
public interface IAccountCredentialCache {

    /**
     * Saves the supplied Account in the cache.
     *
     * @param account The Account to save.
     */
    void saveAccount(final AccountRecord account);

    /**
     * Saves the supplied Credential in the cache.
     *
     * @param credential The Credential to save.
     */
    void saveCredential(final Credential credential);

    /**
     * Gets the Account saved for the supplied cache key.
     *
     * @param cacheKey The cache key to use when consulting the cache.
     * @return The saved Account or null if no cache entry exists.
     */
    AccountRecord getAccount(final String cacheKey);

    /**
     * Gets the Credential saved for the supplied cache key.
     *
     * @param cacheKey The cache key to use when consulting the cache.
     * @return The saved Credential or null if no cache entry exists.
     */
    Credential getCredential(final String cacheKey);

    /**
     * Returns all of the Accounts saved in the cache.
     *
     * @return The saved Accounts.
     */
    List<AccountRecord> getAccounts();

    /**
     * Returns all of the Accounts matching the supplied criteria.
     *
     * @param homeAccountId The homeAccountId used to match Account cache keys.
     * @param environment   The environment used to match Account cache keys.
     * @param realm         The realm used to match Account cache keys.
     * @return A mutable List of Accounts matching the supplied criteria.
     */
    List<AccountRecord> getAccountsFilteredBy(
            final String homeAccountId,
            final String environment,
            final String realm
    );

    /**
     * Returns all of the Credentials saved in the cache.
     *
     * @return A mutable List of saved Credentials.
     */
    List<Credential> getCredentials();

    /**
     * Returns all of the Credentials matching the supplied criteria.
     *
     * @param homeAccountId  The homeAccountId used to match Credential cache keys.
     * @param environment    The environment used to match Credential cache keys.
     * @param credentialType The sought CredentialType.
     * @param clientId       The clientId used to match Credential cache keys.
     * @param realm          The realm used to match Credential cache keys.
     * @param target         The target used to match Credential cache keys.
     * @return A mutable List of Credentials matching the supplied criteria.
     */
    List<Credential> getCredentialsFilteredBy(
            final String homeAccountId,
            final String environment,
            final CredentialType credentialType,
            final String clientId,
            final String realm,
            final String target,
            final String authScheme
    );

    /**
     * Returns all of the Credentials matching the supplied criteria from the supplied List.
     * <p>
     * This API added to cut around repeat calls to getCredentials(), which is expensive.
     *
     * @param homeAccountId  The homeAccountId used to match Credential cache keys.
     * @param environment    The environment used to match Credential cache keys.
     * @param credentialType The sought CredentialType.
     * @param clientId       The clientId used to match Credential cache keys.
     * @param realm          The realm used to match Credential cache keys.
     * @param target         The target used to match Credential cache keys.
     * @return A mutable List of Credentials matching the supplied criteria.
     */
    List<Credential> getCredentialsFilteredBy(
            final String homeAccountId,
            final String environment,
            final CredentialType credentialType,
            final String clientId,
            final String realm,
            final String target,
            final String authScheme,
            final List<Credential> inputCredentials
    );

    /**
     * Returns all of the Credentials matching the supplied criteria.
     *
     * @param homeAccountId   The homeAccountId used to match Credential cache keys.
     * @param environment     The environment used to match Credential cache keys.
     * @param credentialType  The sought CredentialType.
     * @param clientId        The clientId used to match Credential cache keys.
     * @param realm           The realm used to match Credential cache keys.
     * @param target          The target used to match Credential cache keys.
     * @param requestedClaims The requested claims used to match Credential cache keys.
     * @return A mutable List of Credentials matching the supplied criteria.
     */
    List<Credential> getCredentialsFilteredBy(
            final String homeAccountId,
            final String environment,
            final CredentialType credentialType,
            final String clientId,
            final String realm,
            final String target,
            final String authScheme,
            final String requestedClaims
    );

    /**
     * Returns all of the Credentials matching the supplied criteria.
     *
     * @param homeAccountId   The homeAccountId used to match Credential cache keys.
     * @param environment     The environment used to match Credential cache keys.
     * @param credentialTypes The sought CredentialTypes.
     * @param clientId        The clientId used to match Credential cache keys.
     * @param realm           The realm used to match Credential cache keys.
     * @param target          The target used to match Credential cache keys.
     * @param requestedClaims The requested claims used to match Credential cache keys.
     * @return A mutable List of Credentials matching the supplied criteria.
     */
    List<Credential> getCredentialsFilteredBy(
            final String homeAccountId,
            final String environment,
            final Set<CredentialType> credentialTypes,
            final String clientId,
            final String realm,
            final String target,
            final String authScheme,
            final String requestedClaims
    );

    /**
     * Removes the supplied Account from the cache.
     *
     * @param accountToRemove The Account to delete.
     * @return True if the Account was deleted. False otherwise.
     */
    boolean removeAccount(final AccountRecord accountToRemove);

    /**
     * Removes the supplied Credential from the cache.
     *
     * @param credentialToRemove The Credential to delete.
     * @return True if the Credential was deleted. False otherwise.
     */
    boolean removeCredential(final Credential credentialToRemove);

    /**
     * Clear the contents of the cache.
     */
    void clearAll();

}
```

> NOTE: All filter methods accept null for different parameters.  Nulls are interpreted as wildcards by the code.  Meaning that not specifying a "target" (scope) for example results in all matches irrespective of target (scope) associated with the credential.


