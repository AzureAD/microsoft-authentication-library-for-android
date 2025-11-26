# MSAL Android Common Issues Reference Guide

This guide provides AI agents and support staff with a comprehensive reference for diagnosing and resolving common MSAL Android issues. Use this guide when responding to GitHub issues.

---

## Table of Contents

1. [Configuration Issues](#1-configuration-issues)
2. [Authentication Errors](#2-authentication-errors)
3. [Token Acquisition Issues](#3-token-acquisition-issues)
4. [Broker Integration Issues](#4-broker-integration-issues)
5. [Build and Dependency Issues](#5-build-and-dependency-issues)
6. [Runtime Crashes](#6-runtime-crashes)
7. [Single vs Multiple Account Mode Issues](#7-single-vs-multiple-account-mode-issues)
8. [Silent Token Refresh Issues](#8-silent-token-refresh-issues)

---

## 1. Configuration Issues

### 1.1 Redirect URI Encoding Mismatch

**Symptoms:**
- "Redirect URI mismatch" error during authentication
- Authentication fails with no clear error message
- `MsalClientException` with error code `redirect_uri_validation_error`

**Root Cause:**
The signature hash in `auth_config.json` must be URL encoded, while the same hash in `AndroidManifest.xml` must NOT be URL encoded.

**Solution:**

```xml
<!-- AndroidManifest.xml - NOT URL encoded -->
<data
    android:scheme="msauth" 
    android:host="your.package.name"
    android:path="/ABcDeFg+okk=" />
```

```json
// auth_config.json - MUST be URL encoded
{
    "redirect_uri": "msauth://your.package.name/ABcDeFg%2Bokk%3D"
}
```

**URL Encoding Reference:**
| Character | URL Encoded |
|-----------|-------------|
| `+` | `%2B` |
| `=` | `%3D` |
| `/` | `%2F` |
| `*` | `%2A` |

**Related Documentation:** [Configuration Guide](../../auth_config.template.json)

---

### 1.2 Missing or Incorrect Client ID

**Symptoms:**
- `MsalClientException: client_id` error
- PCA initialization fails
- "Application not found" error from Azure AD

**Root Cause:**
The `client_id` in `auth_config.json` doesn't match the Azure App Registration.

**Solution:**
1. Verify the Client ID in Azure Portal → App Registrations → Your App → Overview
2. Update `auth_config.json`:
```json
{
    "client_id": "YOUR_CORRECT_CLIENT_ID_FROM_AZURE_PORTAL"
}
```

---

### 1.3 Missing BrowserTabActivity in AndroidManifest.xml

**Symptoms:**
- Authentication starts but never completes
- App doesn't receive the authentication response
- Screen goes blank after entering credentials

**Solution:**
Add the BrowserTabActivity to your AndroidManifest.xml:

```xml
<activity
    android:name="com.microsoft.identity.client.BrowserTabActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="msauth" 
            android:host="YOUR_PACKAGE_NAME"
            android:path="/YOUR_SIGNATURE_HASH" />
    </intent-filter>
</activity>
```

---

## 2. Authentication Errors

### 2.1 AADSTS Error Codes

**Common AADSTS Errors and Solutions:**

| Error Code | Description | Solution |
|------------|-------------|----------|
| AADSTS50011 | Redirect URI mismatch | Verify redirect URI matches Azure App Registration exactly (including encoding) |
| AADSTS50076 | MFA required | User needs to complete Multi-Factor Authentication |
| AADSTS50079 | MFA enrollment required | User must enroll in MFA |
| AADSTS65001 | Consent required | User or admin hasn't consented to the app |
| AADSTS70002 | Invalid client secret | Update or regenerate client credentials |
| AADSTS700016 | Application not found | Verify client_id is correct |
| AADSTS90010 | Tenant not found | Verify tenant_id or use "common" for multi-tenant apps |

**How to Find AADSTS Codes:**
Enable verbose logging to capture the full error:
```java
Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
Logger.getInstance().setEnableLogcatLog(true);
```

---

### 2.2 User Cancellation

**Symptoms:**
- `MsalUserCancelException` is thrown
- Authentication callback receives cancellation error

**Root Cause:**
User pressed back button or dismissed the authentication prompt.

**Solution:**
Handle this gracefully in your callback:
```java
@Override
public void onError(MsalException exception) {
    if (exception instanceof MsalUserCancelException) {
        // User cancelled - show appropriate message
        showMessage("Sign in was cancelled");
        return;
    }
    // Handle other errors
}
```

---

## 3. Token Acquisition Issues

### 3.1 Deprecated API Usage

**Symptoms:**
- Compilation warnings about deprecated methods
- Unexpected behavior or crashes
- Token acquisition fails silently

**Root Cause:**
Using deprecated methods like:
```java
// ❌ DEPRECATED - DO NOT USE
mPCA.acquireToken(activity, scopes, callback);
mPCA.acquireTokenSilentAsync(scopes, account, authority, callback);
```

**Solution - Multiple Account Mode:**
```java
// ✅ CORRECT - Use Parameters-based API
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .withScopes(Arrays.asList("User.Read"))
    .withCallback(callback)
    .build();
mPCA.acquireToken(params);
```

**Solution - Single Account Mode:**
```java
// ✅ CORRECT - Use SignInParameters
SignInParameters signInParams = new SignInParameters.Builder()
    .startActivity(activity)
    .withCallback(callback)
    .build();
mPCA.signIn(signInParams);
```

---

### 3.2 Scope Format Errors

**Symptoms:**
- "Invalid scope" error
- Token acquisition fails with scope validation error

**Solution:**
Ensure scopes are formatted correctly:
```java
// ✅ CORRECT
List<String> scopes = Arrays.asList("User.Read", "Mail.Read");

// ❌ INCORRECT - Don't include resource prefix for MS Graph
List<String> scopes = Arrays.asList("https://graph.microsoft.com/User.Read");

// ✅ CORRECT for custom APIs
List<String> scopes = Arrays.asList("api://YOUR_API_CLIENT_ID/access_as_user");
```

---

## 4. Broker Integration Issues

### 4.1 Broker Not Available

**Symptoms:**
- Authentication uses browser instead of Authenticator app
- SSO doesn't work across apps
- Error indicating broker is not available

**Root Cause:**
- Microsoft Authenticator, Company Portal, or Link To Windows not installed
- `broker_redirect_uri_registered` set to `false`

**Solution:**
1. Ensure broker app is installed on the device
2. Verify configuration:
```json
{
    "broker_redirect_uri_registered": true
}
```

---

### 4.2 Broker Signature Mismatch

**Symptoms:**
- "Broker package signature verification failed"
- Authentication falls back to browser

**Root Cause:**
The signature hash registered in Azure doesn't match the app's actual signing certificate.

**Solution:**
1. Get the correct signature hash:
```bash
keytool -exportcert -alias YOUR_KEY_ALIAS -keystore YOUR_KEYSTORE.jks | openssl sha1 -binary | openssl base64
```
2. Update Azure App Registration with the correct hash
3. Update `auth_config.json` (URL encoded) and `AndroidManifest.xml` (not URL encoded)

---

## 5. Build and Dependency Issues

### 5.1 AndroidX Compatibility

**Symptoms:**
- Build fails with duplicate class errors
- "android.support" vs "androidx" conflicts
- jetifier errors

**Solution:**
Ensure these properties are in `gradle.properties`:
```properties
android.useAndroidX=true
android.enableJetifier=true
```

---

### 5.2 MSAL Version Conflicts

**Symptoms:**
- Dependency resolution failures
- Method not found errors at runtime
- Missing class exceptions

**Solution:**
Use the latest MSAL version:
```gradle
dependencies {
    implementation "com.microsoft.identity.client:msal:7.+"
}
```

Ensure `minSdk` is at least 24:
```gradle
android {
    defaultConfig {
        minSdk 24
        targetSdk 35
    }
}
```

---

### 5.3 ProGuard/R8 Issues

**Symptoms:**
- App works in debug but crashes in release
- ClassNotFoundException at runtime
- Serialization/deserialization failures

**Solution:**
Add to your `proguard-rules.pro`:
```proguard
-keep class com.microsoft.identity.** { *; }
-keep class com.nimbusds.** { *; }
-dontwarn com.nimbusds.**
```

---

## 6. Runtime Crashes

### 6.1 PCA Not Initialized

**Symptoms:**
- NullPointerException when calling MSAL methods
- App crashes on sign-in button click
- "PublicClientApplication is null" error

**Solution:**
Always check PCA initialization before use:
```java
private void signIn() {
    if (mPCA == null) {
        Log.e(TAG, "PublicClientApplication not initialized");
        showError("Authentication not ready. Please try again.");
        return;
    }
    // Proceed with sign in
}
```

Initialize PCA asynchronously:
```java
PublicClientApplication.createMultipleAccountPublicClientApplication(
    context,
    R.raw.auth_config,
    new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
        @Override
        public void onCreated(IMultipleAccountPublicClientApplication application) {
            mPCA = application;
        }

        @Override
        public void onError(MsalException exception) {
            Log.e(TAG, "PCA creation failed: " + exception.getMessage());
        }
    }
);
```

---

### 6.2 UI Thread Violations

**Symptoms:**
- "Only the original thread that created a view hierarchy can touch its views"
- CalledFromWrongThreadException
- UI freezes or doesn't update

**Solution:**
Always update UI from the main thread:
```java
@Override
public void onSuccess(IAuthenticationResult authenticationResult) {
    activity.runOnUiThread(() -> {
        updateUI(authenticationResult);
        showMessage("Sign in successful!");
    });
}
```

---

## 7. Single vs Multiple Account Mode Issues

### 7.1 Wrong API for Account Mode

**Symptoms:**
- `MsalClientException: This method is not supported for multiple account mode`
- `getCurrentAccount()` returns null in multiple account app
- Sign in doesn't work as expected

**Solution:**

**Multiple Account Mode:**
```java
// ✅ Use for multiple account
IMultipleAccountPublicClientApplication mPCA;
mPCA.getAccounts(callback);  // Get all accounts
mPCA.acquireToken(params);   // Sign in
mPCA.removeAccount(account, callback);  // Sign out specific account
```

**Single Account Mode:**
```java
// ✅ Use for single account
ISingleAccountPublicClientApplication mPCA;
mPCA.getCurrentAccount(callback);  // Get current account
mPCA.signIn(params);  // Sign in
mPCA.signOut(callback);  // Sign out
```

---

### 7.2 Account Mode Configuration Mismatch

**Symptoms:**
- Unexpected behavior after switching account modes
- Account persistence issues

**Solution:**
Ensure `auth_config.json` matches your code:
```json
{
    "account_mode": "MULTIPLE"  // or "SINGLE"
}
```

---

## 8. Silent Token Refresh Issues

### 8.1 Silent Token Acquisition Failure

**Symptoms:**
- `MsalUiRequiredException` thrown during silent acquisition
- Token cache appears empty
- User has to re-authenticate frequently

**Root Cause:**
- No cached token available
- Refresh token expired
- User consent required for new scopes

**Solution:**
Implement proper fallback to interactive authentication:
```java
AcquireTokenSilentParameters params = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .withCallback(new SilentAuthenticationCallback() {
        @Override
        public void onSuccess(IAuthenticationResult result) {
            // Use the token
        }

        @Override
        public void onError(MsalException exception) {
            if (exception instanceof MsalUiRequiredException) {
                // Fall back to interactive
                acquireTokenInteractively();
            }
        }
    })
    .build();
mPCA.acquireTokenSilent(params);
```

---

## Diagnostic Information to Request

When an issue isn't covered above, ask the user for:

1. **MSAL Version:** `implementation 'com.microsoft.identity.client:msal:X.X.X'`
2. **Android Version:** Device API level and build number
3. **Account Mode:** Single or Multiple
4. **Broker Enabled:** Yes/No and which broker app
5. **Error Message:** Complete error message or exception stack trace
6. **Logs:** Verbose logs with PII enabled (for debugging only)

```java
// Enable verbose logging for diagnostics
Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
Logger.getInstance().setEnablePII(true);  // Only for debugging
Logger.getInstance().setEnableLogcatLog(true);
```

---

## Additional Resources

- [Official MSAL Android Documentation](https://github.com/AzureAD/microsoft-authentication-library-for-android)
- [Code Snippets](../../snippets/) - Reference implementations
- [Golden Examples](../../examples/) - Complete working applications
- [Configuration Template](../../auth_config.template.json) - Full configuration options

---

*This guide is maintained by the MSAL Android team. For issues not covered here, please create a detailed bug report.*
