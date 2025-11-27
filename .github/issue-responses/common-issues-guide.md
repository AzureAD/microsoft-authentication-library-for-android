# MSAL Android Common Issues Reference Guide

This guide provides AI agents and support staff with a comprehensive reference for diagnosing and resolving common MSAL Android issues. Use this guide when responding to GitHub issues.

> **Note:** This guide was compiled from analysis of 250+ GitHub issues (both open and closed) from the MSAL Android repository. Issues have been grouped by category to provide consolidated solutions for related problems.

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
9. [Network and Cloud Discovery Issues](#9-network-and-cloud-discovery-issues)
10. [WebView and Browser Issues](#10-webview-and-browser-issues)
11. [Azure AD B2C Specific Issues](#11-azure-ad-b2c-specific-issues)

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
| AADSTS70043 | Refresh token expired due to sign-in frequency | Implement interactive login fallback |
| AADSTS700016 | Application not found | Verify client_id is correct |
| AADSTS90010 | Tenant not found | Verify tenant_id or use "common" for multi-tenant apps |
| AADSTS9002313 | Invalid request, malformed | Check authority URL format and token endpoint |
| AADSTS900384 | JWT signature validation failed | Verify authority URL matches your cloud (e.g., .us for Government) |
| AADB2C90080 | Grant expired | Refresh token expired, need interactive login |

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

// ✅ CORRECT for custom APIs (use your API's application/client ID)
List<String> scopes = Arrays.asList("api://YOUR_API_APPLICATION_CLIENT_ID/access_as_user");
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

### 4.3 Broker Communication Failure on Android 15

**Symptoms:**
- `BrokerCommunicationException: Failed to get result from Broker Content Provider, cursor is null`
- `android.accounts.AuthenticatorException: bind failure`
- Error: `Failed to find provider info for com.azure.authenticator.microsoft.identity.broker`
- Works on older Android versions but fails on Android 15

**Root Cause:**
Android 15 has stricter package visibility requirements that can prevent MSAL from communicating with the broker app (see GitHub issue #2232).

**Solution:**
1. Ensure Microsoft Authenticator is up to date
2. Force stop and restart the Authenticator app
3. If issue persists, clear Authenticator app data and re-add accounts
4. Consider updating to the latest MSAL version which may have fixes for Android 15 compatibility

---

### 4.4 Broker Authentication Hangs or Gets Stuck

**Symptoms:**
- Authenticator app opens but spinner runs indefinitely
- Authentication never completes after entering credentials
- App hangs after broker is killed or force stopped
- No callback received from broker

**Root Cause:**
When the Authenticator app is killed during authentication, MSAL may not receive a proper cancellation signal, leaving the interactive session stuck (see GitHub issues #1396, #1997).

**Solution:**
1. If stuck, restart both apps (yours and Authenticator)
2. Implement a timeout for interactive authentication:
```java
// Add a reasonable timeout handler
new Handler(Looper.getMainLooper()).postDelayed(() -> {
    if (authenticationInProgress) {
        showError("Authentication timed out. Please try again.");
        authenticationInProgress = false;
    }
}, 120000); // 2 minute timeout
```
3. Update to the latest MSAL version which has improved broker handling

---

### 4.5 Company Portal / Intune Integration Issues

**Symptoms:**
- Certificate-based authentication fails with Company Portal
- Phone freezes when selecting certificates
- "Company Portal isn't responding" message
- Works in browser but not in Office apps

**Root Cause:**
Issues with Intune Company Portal's certificate storage or conditional access policies (see GitHub issue #2157).

**Solution:**
1. Update Company Portal to the latest version
2. Re-enroll the device in Intune if problems persist
3. Verify conditional access policies are correctly configured
4. For certificate issues, ensure the certificate is properly installed in the work profile

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
    implementation "com.microsoft.identity.client:msal:8.+"
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
- R8 build fails with "Missing classes detected" error
- Errors mentioning `edu.umd.cs.findbugs.annotations`, `com.google.crypto.tink`, or `net.jcip.annotations`

**Root Cause:**
MSAL has dependencies on optional classes that R8 cannot resolve during minification. This is a known recurring issue (see GitHub issues #1677, #2076, #2289, #2355).

**Solution:**
Add these rules to your `proguard-rules.pro`:
```proguard
# MSAL core classes
-keep class com.microsoft.identity.** { *; }
-keep class com.nimbusds.** { *; }

# Required for R8 compatibility
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign$KeyPair
-dontwarn com.google.crypto.tink.subtle.Ed25519Sign
-dontwarn com.google.crypto.tink.subtle.Ed25519Verify
-dontwarn com.google.crypto.tink.subtle.X25519
-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305
-dontwarn edu.umd.cs.findbugs.annotations.NonNull
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn net.jcip.annotations.**
-dontwarn com.nimbusds.**
```

For MSAL 6.0.0+ or 7.0.0+, you may also need:
```gradle
// Add Tink and SpotBugs annotations if R8 errors persist
implementation("com.google.crypto.tink:tink:1.17.0") {
    exclude group: 'com.google.protobuf'
}
implementation 'com.github.spotbugs:spotbugs-annotations:4.9.3'
```

---

### 5.4 display-mask Dependency Resolution

**Symptoms:**
- `Failed to resolve: com.microsoft.device.display:display-mask:0.3.0`
- Build fails when adding MSAL dependency

**Root Cause:**
The `display-mask` library is hosted in a Microsoft-specific Maven repository that may not be included in your project's repository list (see GitHub issue #1027, #1720).

**Solution:**
Add the Microsoft Maven repository to your `settings.gradle`:
```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url 'https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1'
        }
    }
}
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

### 6.3 Android 15 Edge-to-Edge Display Issues

**Symptoms:**
- Action bar overlaps content in the MSAL WebView on Android 15
- Login form is partially unreadable
- UI elements appear behind system bars

**Root Cause:**
Android 15 (SDK 35) enables edge-to-edge display by default, which affects MSAL's internal activities (see GitHub issues #2204, #2341).

**Solution:**
1. Update to the latest MSAL version which may include fixes
2. If using custom themes, ensure proper insets handling:
```xml
<style name="Theme.MyApp" parent="Theme.MaterialComponents.Light.NoActionBar">
    <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
</style>
```
3. Consider using browser-based authentication (`authorization_user_agent: "BROWSER"`) as a workaround

---

### 6.4 Fragment Transaction Errors

**Symptoms:**
- `IllegalStateException: FragmentManager is already executing transactions`
- `No stored state. Unable to handle response`
- Authentication spinner runs indefinitely when using `withFragment()`

**Root Cause:**
Using `withFragment()` in `AcquireTokenParameters` can cause issues with fragment lifecycle timing (see GitHub issue #1725).

**Solution:**
1. Avoid using `withFragment()` if possible - use `startAuthorizationFromActivity()` instead:
```java
// ✅ RECOMMENDED
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .startAuthorizationFromActivity(requireActivity())
    .withScopes(SCOPES)
    .withCallback(callback)
    .build();
```

2. If you must use `withFragment()`, ensure the fragment is fully resumed:
```java
@Override
public void onResume() {
    super.onResume();
    // Only initiate authentication when fragment is fully ready
    if (pendingAuthentication) {
        startAuthentication();
    }
}
```

---

### 6.5 ANR (Application Not Responding) Issues

**Symptoms:**
- App freezes during MSAL operations
- ANR dialog appears
- Main thread blocked waiting for lock

**Root Cause:**
MSAL performs network operations that can block the main thread if configuration or cloud discovery takes too long (see GitHub issue #1952).

**Solution:**
1. Never initialize PCA on the main thread during startup:
```java
// ❌ DON'T DO THIS
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // This can cause ANR
    mPCA = PublicClientApplication.createSingleAccountPublicClientApplication(this, R.raw.auth_config);
}

// ✅ DO THIS INSTEAD
Executors.newSingleThreadExecutor().execute(() -> {
    PublicClientApplication.createSingleAccountPublicClientApplication(
        context,
        R.raw.auth_config,
        callback
    );
});
```

2. Use the async initialization methods
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

### 8.2 AADB2C90080 - Expired Grant Error

**Symptoms:**
- `MsalUiRequiredException: AADB2C90080: The provided grant has expired`
- Error occurs after refresh token expiry
- User can log in interactively, but subsequent silent calls still fail
- Error shows old token timestamps even after fresh interactive login

**Root Cause:**
This is a common B2C issue where the refresh token has expired due to sign-in frequency policies or token lifetime settings. After interactive login, stale cache entries may still be used (see GitHub issues #1004, #1216, #2043, #2257).

**Solution:**
1. Clear the account before re-authenticating:
```java
// When AADB2C90080 occurs, remove the account first
mPCA.removeAccount(account, new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
    @Override
    public void onRemoved() {
        // Now do interactive login
        acquireTokenInteractively();
    }

    @Override
    public void onError(MsalException exception) {
        // Handle error
    }
});
```

2. For B2C, configure appropriate token lifetimes in Azure Portal
3. Consider implementing proactive token refresh before expiration

---

### 8.3 No Cached Accounts Found

**Symptoms:**
- `MsalClientException: No cached accounts found for the supplied homeAccountId and clientId`
- Silent authentication fails even though user previously signed in
- Account list appears empty after app restart

**Root Cause:**
This can occur with B2C accounts, External ID tenants, or when there's a mismatch between the account's home tenant and the realm (see GitHub issues #1779, #2172).

**Solution:**
1. Verify you're using the correct account from `getAccounts()`:
```java
mPCA.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
    @Override
    public void onTaskCompleted(List<IAccount> accounts) {
        if (accounts.isEmpty()) {
            // No cached accounts, need interactive login
            acquireTokenInteractively();
            return;
        }
        // Find the right account for your policy/authority
        for (IAccount account : accounts) {
            if (accountMatchesPolicy(account)) {
                acquireTokenSilently(account);
                return;
            }
        }
    }
});
```

2. For B2C, ensure you're using the correct policy name and authority:
```java
AcquireTokenSilentParameters params = new AcquireTokenSilentParameters.Builder()
    .fromAuthority(getAuthorityFromPolicyName("B2C_1_signin"))
    .withScopes(scopes)
    .forAccount(account)
    .build();
```

---

### 8.4 Silent Token Performance Issues

**Symptoms:**
- `acquireTokenSilent` takes 100-1000ms even when token is cached
- Network calls made during cache lookup
- Slow app startup due to token acquisition

**Root Cause:**
Silent token acquisition involves validation and potential network operations even when returning cached tokens (see GitHub issue #2097).

**Solution:**
1. Cache the access token in your app for immediate use
2. Only call `acquireTokenSilent` when you need to refresh:
```java
// Check if your locally cached token is still valid
if (localToken != null && !isTokenExpired(localToken)) {
    useToken(localToken);
    return;
}

// Only then call MSAL
mPCA.acquireTokenSilent(params);
```

3. Use token expiration time to proactively refresh before expiry

---

## 9. Network and Cloud Discovery Issues

### 9.1 Unable to Perform Cloud Discovery

**Symptoms:**
- `MsalClientException: Unable to perform cloud discovery`
- `UnknownHostException: Unable to resolve host "login.microsoftonline.com"`
- Authentication fails on certain network conditions

**Root Cause:**
This error occurs when MSAL cannot reach Microsoft's cloud discovery endpoints due to network issues, VPN configurations, proxy settings, or DNS problems (see GitHub issues #1568, #1765, #1696, #1723, #1663).

**Solution:**
1. Verify network connectivity to Microsoft endpoints:
```java
// Check network availability before MSAL calls (API 29+)
ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
Network activeNetwork = cm.getActiveNetwork();
if (activeNetwork != null) {
    NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
    boolean isConnected = capabilities != null && 
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
}
```

2. For VPN/proxy environments:
   - Ensure `login.microsoftonline.com` and `login.live.com` are accessible
   - Configure proxy settings in the device's WiFi settings
   - For emulators, ensure proper proxy configuration in Android Studio

3. Handle cloud discovery failures gracefully:
```java
@Override
public void onError(MsalException exception) {
    if (exception.getMessage().contains("Unable to perform cloud discovery")) {
        showError("Network connection required. Please check your internet connection.");
    }
}
```

---

### 9.2 Power Optimization / Doze Mode Issues

**Symptoms:**
- `MsalClientException: Connection is not available to refresh token because power optimization is enabled`
- Silent token refresh fails when device is in doze mode
- Token refresh works when app is in foreground but fails in background

**Root Cause:**
Android's power optimization features (Doze mode, App Standby) restrict network access for apps running in the background (see GitHub issue #1766).

**Solution:**
1. Request exemption from battery optimization for critical apps:
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
    if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
}
```

2. Perform token refresh when the app is in the foreground
3. Cache tokens appropriately and refresh proactively before they expire

---

### 9.3 SSL/TLS Certificate Issues

**Symptoms:**
- `SSLHandshakeException: Chain validation failed`
- `CertificateException: Chain validation failed`
- "Response is unreliable: its validity interval is out-of-date"

**Root Cause:**
Device clock is incorrect, certificate chain is incomplete, or OCSP response has expired (see GitHub issue #1568).

**Solution:**
1. Verify device date/time is correct (automatic date/time should be enabled)
2. For development, ensure you're not intercepting HTTPS traffic incorrectly
3. Update to the latest MSAL version which includes updated certificate handling

---

## 10. WebView and Browser Issues

### 10.1 Custom Tab / Browser Issues

**Symptoms:**
- "Chrome browser not found" error
- Authentication fails with non-Chrome browsers
- Login page doesn't open or crashes

**Root Cause:**
MSAL requires a compatible browser for authentication. Not all browsers support Custom Tabs correctly (see GitHub issues #1591, #1626, #1509).

**Solution:**
1. Add browser safelist to your `auth_config.json` if needed:
```json
{
  "browser_safelist": [
    {
      "browser_package_name": "com.android.chrome",
      "browser_signature_hashes": ["7fmduHKTdHHrlMvldlEqAIlSfii1tl35bxj1OXN5Ve8c4lU6URVu4xtSHc3BVZxS6WWJnxMDhIfQN0N0K2NDJg=="]
    }
  ]
}
```

2. For devices without Chrome, consider using WebView mode:
```json
{
  "authorization_user_agent": "WEBVIEW"
}
```

**Note:** WebView mode does not support social identity provider logins (Google, Facebook). Use DEFAULT or BROWSER for those scenarios.

---

### 10.2 Authorization Flow Cancellation

**Symptoms:**
- `MsalClientException: Sdk cancelled the auth flow as the app launched a new interactive auth request`
- `MsalClientException: Unable to complete authorization as there is no interactive call in progress`
- Authentication fails when "Don't keep activities" is enabled

**Root Cause:**
Multiple simultaneous authentication requests, activity destruction during auth flow, or improper handling of auth state (see GitHub issues #1561, #1562).

**Solution:**
1. Track authentication state to prevent multiple simultaneous requests:
```java
private AtomicBoolean authenticationInProgress = new AtomicBoolean(false);

public void signIn() {
    if (!authenticationInProgress.compareAndSet(false, true)) {
        Log.w(TAG, "Authentication already in progress");
        return;
    }
    
    AcquireTokenParameters params = new AcquireTokenParameters.Builder()
        .withCallback(new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult result) {
                authenticationInProgress.set(false);
                // Handle success
            }
            
            @Override
            public void onError(MsalException exception) {
                authenticationInProgress.set(false);
                // Handle error
            }
            
            @Override
            public void onCancel() {
                authenticationInProgress.set(false);
                // Handle cancellation
            }
        })
        .build();
    mPCA.acquireToken(params);
}
```

2. For "Don't keep activities" issues, use `singleTask` launch mode:
```xml
<activity
    android:name=".AuthActivity"
    android:launchMode="singleTask">
</activity>
```

---

### 10.3 WebView SSO Issues

**Symptoms:**
- User has to re-authenticate in WebView after MSAL sign-in
- SSO doesn't work between native MSAL and WebView content
- Different sessions between MSAL and app WebViews

**Root Cause:**
MSAL and WebView use different cookie stores. The authentication session in MSAL is not automatically shared with app WebViews (see GitHub issue #1582).

**Solution:**
1. Use the access token from MSAL to authenticate API calls from WebView
2. For web content requiring SSO, consider using the system browser instead of embedded WebView
3. Pass authentication tokens to web content via URL parameters or headers (securely)

---

## 11. Azure AD B2C Specific Issues

### 11.1 B2C Token Refresh Failures

**Symptoms:**
- `AADB2C90080: The provided grant has expired`
- Silent token acquisition fails after some time
- Works with interactive login but fails on subsequent silent calls

**Root Cause:**
B2C refresh tokens have different lifetime policies than standard Azure AD. Additionally, there can be cache mismatches between policies (see GitHub issues #1004, #1216, #2043, #2257, #1547).

**Solution:**
1. Clear the account before re-authenticating on refresh token expiry:
```java
@Override
public void onError(MsalException exception) {
    if (exception.getMessage().contains("AADB2C90080")) {
        // Remove the account and force interactive login
        mPCA.removeAccount(account, new RemoveAccountCallback() {
            @Override
            public void onRemoved() {
                acquireTokenInteractively();
            }
            
            @Override
            public void onError(MsalException e) {
                // Handle error
            }
        });
    }
}
```

2. Configure appropriate token lifetimes in Azure B2C portal
3. Match the authority URL exactly when acquiring tokens silently

---

### 11.2 B2C Policy Mismatch

**Symptoms:**
- "No cached accounts found for the supplied homeAccountId"
- Account from one policy not found when using another
- Silent acquisition fails even after successful interactive login

**Root Cause:**
B2C accounts are specific to the policy used during sign-in. Using a different policy or authority URL for silent acquisition will fail to find the cached account (see GitHub issues #1779, #1567, #1598).

**Solution:**
1. Match the policy name in silent token requests:
```java
// Helper to get the B2C policy from an account
private String getB2CPolicyNameFromAccount(IAccount account) {
    // Extract policy from account.getClaims() or account.getAuthority()
}

// Use matching authority for silent acquisition
AcquireTokenSilentParameters params = new AcquireTokenSilentParameters.Builder()
    .fromAuthority(getAuthorityFromPolicyName(policyName))
    .withScopes(scopes)
    .forAccount(account)
    .build();
```

2. Store the policy name used during interactive login
3. Search for accounts matching the desired policy before silent acquisition

---

### 11.3 B2C Custom Claims Issues

**Symptoms:**
- Claims null or missing after authentication
- `account.getClaims()` returns null for federated users
- ID token doesn't contain expected custom claims

**Root Cause:**
For B2C with federated identity providers, claims may not be returned if not configured in the user flow or custom policy (see GitHub issue #1491).

**Solution:**
1. Configure claims in Azure B2C user flow or custom policy
2. Add required claims as application claims in B2C
3. For federated users, ensure claims passthrough is configured
4. Check both ID token and access token for claims:
```java
@Override
public void onSuccess(IAuthenticationResult result) {
    // Check ID token claims
    Map<String, ?> claims = result.getAccount().getClaims();
    
    // If claims are in access token, decode it
    String accessToken = result.getAccessToken();
    // Decode JWT to access claims
}
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

## Referenced GitHub Issues

This guide was compiled from analysis of 250+ common issues reported in this repository. The following issues were particularly influential in shaping this documentation:

### R8/ProGuard Issues
- [#1540](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1540) - Google Play reporting "Unsafe cipher mode" for StorageHelper
- [#1677](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1677) - Missing classes when R8 minify is enabled
- [#1695](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1695) - Defined multiple times when R8 minify is enabled
- [#2076](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2076) - Missing classes detected while running R8
- [#2289](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2289) - Android MSAL 6.0.0 + obfuscation issues
- [#2355](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2355) - R8 minification fails on net.jcip.annotations

### Silent Token/Refresh Issues
- [#1004](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1004) - AADB2C90080: The provided grant has expired
- [#1216](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1216) - MsalExceptionAdapter.java line 64
- [#1505](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1505) - Account set on silent operation parameters is NULL
- [#1547](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1547) - Missing clear docs on B2C token refresh
- [#1699](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1699) - AcquireTokenSilently gives old access token
- [#1779](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1779) - No cached accounts found for the supplied homeAccountId
- [#1790](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1790) - acquireTokenSilentAsync does not refresh the account token
- [#2043](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2043) - Error on MsalExceptionAdapter.java line 73
- [#2097](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2097) - Silent token acquisition performance issues
- [#2172](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2172) - External ID - acquireTokenSilentAsync - No cached accounts
- [#2257](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2257) - acquireTokenSilentAsync fails with AADB2C90080

### Broker Issues
- [#1396](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1396) - Broker authentication hangs
- [#1570](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1570) - COMPANY_PORTAL_REQUIRED error
- [#1737](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1737) - MSAL login failed with Company Portal auto enrollment
- [#1842](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1842) - Android 13 background broker issues
- [#1952](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1952) - ANR as main thread waits for the lock in IO thread
- [#1997](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1997) - Broker activity destroyed
- [#2026](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2026) - Broker issues on Android 14
- [#2157](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2157) - Company Portal / Intune integration issues
- [#2232](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2232) - Broker communication failure on Android 15

### Configuration and Dependency Issues
- [#1027](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1027) - Failed to resolve: com.microsoft.device.display:display-mask:0.3.0
- [#1531](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1531) - Generating signature hash for Google Play signing
- [#1543](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1543) - Google Play Internal Testing error with App Store signing
- [#1550](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1550) - Signature Hash with Google Play Signing
- [#1590](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1590) - BrowserTabActivity is missing
- [#1648](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1648) - "Are you trying to connect to" prompt
- [#1720](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1720) - Azure Active Directory's library display-mask error
- [#1722](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1722) - Multiple apps listening for URL scheme

### Network and Cloud Discovery Issues
- [#1568](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1568) - Unable to perform cloud discovery on Android 12
- [#1663](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1663) - io_error after migrating
- [#1696](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1696) - Unable to open BrowserTabActivity with VPN
- [#1723](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1723) - Unable to perform cloud discovery on API >21
- [#1765](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1765) - Unable to perform cloud discovery - UnknownHostException
- [#1766](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1766) - Cannot refresh token due to power optimization

### WebView and Browser Issues
- [#1509](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1509) - Support for Mi Browser
- [#1561](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1561) - Sdk cancelled the auth flow
- [#1562](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1562) - Unable to complete authorization
- [#1581](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1581) - withFragment does not work
- [#1582](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1582) - SSO is asking for login again in WebView
- [#1591](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1591) - Chrome is the only option
- [#1626](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1626) - Chrome browser not found error for Android 11
- [#1734](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1734) - authorization_user_agent stuck at webview

### B2C Specific Issues
- [#1491](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1491) - Claims data is null for federated users
- [#1567](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1567) - No cached accounts for B2C
- [#1598](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1598) - No cached accounts found for homeAccountId
- [#1749](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1749) - Invalid JSON 'claims' value encountered

### Android 15/SDK 35 Issues
- [#2204](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2204) - Edge-to-edge display issues
- [#2341](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/2341) - Android 15 compatibility

### Runtime and Crash Issues
- [#1539](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1539) - Service not registered: CustomTabsManager
- [#1631](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1631) - BrokerActivity destroyed indefinitely
- [#1725](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1725) - Fragment transaction errors
- [#1764](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1764) - Failed resolution of io/opentelemetry/api/trace/Span
- [#1792](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues/1792) - Crash after Activity.onMAMCreate()

---

*This guide is maintained by the MSAL Android team. For issues not covered here, please create a detailed bug report.*
