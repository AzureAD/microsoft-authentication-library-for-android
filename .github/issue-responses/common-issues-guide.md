# MSAL Android Common Issues - AI Quick Reference

**Purpose:** AI-optimized troubleshooting guide for MSAL Android GitHub issues
**Source:** Analysis of 250+ repository issues (open/closed)
**Format:** Structured entries with symptoms → cause → solution → related issues

---

## Quick Diagnostic Index

**Configuration**: 1.1-1.3 | **Auth Errors**: 2.1-2.2 | **Token Issues**: 3.1-3.2 | **Broker**: 4.1-4.5 | **Build**: 5.1-5.4 | **Crashes**: 6.1-6.5 | **Account Mode**: 7.1-7.2 | **Silent Token**: 8.1-8.4 | **Network**: 9.1-9.3 | **Browser**: 10.1-10.3 | **B2C**: 11.1-11.3

## Error Pattern Quick Lookup

- **"redirect_uri"** → 1.1, 4.2
- **"AADSTS"** error codes → 2.1
- **"BrowserTabActivity"** → 1.3
- **"client_id"** → 1.2
- **Deprecated API warnings** → 3.1
- **"Invalid scope"** → 3.2
- **Broker issues** → 4.1-4.5
- **Android 15** issues → 4.3, 6.3
- **ProGuard/R8** → 5.3
- **display-mask** → 5.4
- **NullPointerException** on PCA → 6.1
- **UI thread errors** → 6.2
- **Fragment errors** → 6.4
- **ANR/freezing** → 6.5
- **ClassCastException** → 7.1
- **Silent token fails** → 8.1
- **"AADB2C90080"** → 8.2, 11.1
- **"No cached accounts"** → 8.3
- **"Unable to perform cloud discovery"** → 9.1
- **"Chrome not found"** → 10.1
- **"SDK cancelled"** → 10.2
- **B2C token refresh** → 11.1

---

## 1. Configuration Issues

### 1.1 Redirect URI Encoding Mismatch

**SYMPTOMS:** `redirect_uri_validation_error`, auth fails with no clear error, "Redirect URI mismatch"

**CAUSE:** Signature hash encoding differs between `auth_config.json` (URL encoded) and `AndroidManifest.xml` (NOT encoded)

**SOLUTION:**
```xml
<!-- AndroidManifest.xml - NOT URL encoded -->
<data android:scheme="msauth" android:host="pkg.name" android:path="/ABcDeFg+okk=" />
```
```json
// auth_config.json - MUST be URL encoded
{"redirect_uri": "msauth://pkg.name/ABcDeFg%2Bokk%3D"}
```

**ENCODING:** `+`→`%2B`, `=`→`%3D`, `/`→`%2F`, `*`→`%2A`

**RELATED:** #1531, #1543, #1550 (Google Play signing)

---

### 1.2 Missing or Incorrect Client ID

**SYMPTOMS:** `MsalClientException: client_id`, PCA init fails, "Application not found"

**CAUSE:** `client_id` in `auth_config.json` doesn't match Azure App Registration

**SOLUTION:** Verify Azure Portal → App Registrations → Overview → Application (client) ID matches config

---

### 1.3 Missing BrowserTabActivity in AndroidManifest.xml

**SYMPTOMS:** Auth starts but never completes, app doesn't receive response, blank screen after credentials

**SOLUTION:**
```xml
<activity android:name="com.microsoft.identity.client.BrowserTabActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="msauth" android:host="PKG" android:path="/HASH" />
    </intent-filter>
</activity>
```

**RELATED:** #1590

---

## 2. Authentication Errors

### 2.1 AADSTS Error Codes

**REFERENCE:** [Microsoft Error Codes](https://learn.microsoft.com/en-us/entra/identity-platform/reference-error-codes)

**COMMON CODES:**
- `AADSTS50011`: Redirect URI mismatch → verify exact match in Azure
- `AADSTS50076`: MFA required → user must complete MFA
- `AADSTS50079`: MFA enrollment required → user must enroll
- `AADSTS65001`: Consent required → grant admin/user consent
- `AADSTS70043`: Refresh token expired → use interactive login
- `AADSTS700016`: App not found → verify `client_id`
- `AADSTS90010`: Tenant not found → verify `tenant_id` or use "common"
- `AADSTS9002313`: Invalid request → check authority URL format
- `AADB2C90080`: Grant expired → need interactive login (see 8.2)

**ENABLE LOGGING:**
```java
Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
Logger.getInstance().setEnableLogcatLog(true);
```

---

### 2.2 User Cancellation

**SYMPTOMS:** `MsalUserCancelException` thrown

**CAUSE:** User pressed back or dismissed auth prompt

**SOLUTION:** Handle gracefully in callback
```java
if (exception instanceof MsalUserCancelException) {
    showMessage("Sign in cancelled");
    return;
}
```

---

## 3. Token Acquisition Issues

### 3.1 Deprecated API Usage

**SYMPTOMS:** Deprecation warnings, unexpected behavior, silent failures

**WRONG:**
```java
// ❌ DEPRECATED
mPCA.acquireToken(activity, scopes, callback);
mPCA.acquireTokenSilentAsync(scopes, account, authority, callback);
```

**CORRECT - Multiple Account:**
```java
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .withScopes(Arrays.asList("User.Read"))
    .withCallback(callback)
    .build();
mPCA.acquireToken(params);
```

**CORRECT - Single Account:**
```java
SignInParameters params = new SignInParameters.Builder()
    .startActivity(activity)
    .withCallback(callback)
    .build();
mPCA.signIn(params);
```

---

### 3.2 Scope Format Errors

**SYMPTOMS:** "Invalid scope", scope validation error

**CORRECT:**
```java
// ✅ MS Graph
Arrays.asList("User.Read", "Mail.Read")

// ✅ Custom API (use your API's application/client ID)
Arrays.asList("api://YOUR_API_APP_ID/access_as_user")
```

**WRONG:**
```java
// ❌ Don't include resource prefix for MS Graph
Arrays.asList("https://graph.microsoft.com/User.Read")
```

---

## 4. Broker Integration Issues

### 4.1 Broker Not Available

**SYMPTOMS:** Auth uses browser instead of Authenticator, no SSO across apps

**CAUSE:** Authenticator/Company Portal/Link To Windows not installed, or `broker_redirect_uri_registered: false`

**SOLUTION:**
```json
{"broker_redirect_uri_registered": true}
```
Install Microsoft Authenticator or Company Portal

---

### 4.2 Broker Signature Mismatch

**SYMPTOMS:** "Broker package signature verification failed", falls back to browser

**CAUSE:** Signature hash in Azure doesn't match app's signing certificate

**SOLUTION:**
```bash
keytool -exportcert -alias ALIAS -keystore KEYSTORE.jks | openssl sha1 -binary | openssl base64
```
Update Azure App Registration + `auth_config.json` (URL encoded) + `AndroidManifest.xml` (not encoded)

---

### 4.3 Broker Communication Failure on Android 15

**SYMPTOMS:** `BrokerCommunicationException: cursor is null`, `bind failure`, `Failed to find provider info`, works on older Android

**CAUSE:** Android 15 stricter package visibility requirements

**SOLUTION:**
1. Update Microsoft Authenticator to latest
2. Force stop/restart Authenticator
3. Clear Authenticator data if needed
4. Update to latest MSAL version

**RELATED:** #2232

---

### 4.4 Broker Authentication Hangs or Gets Stuck

**SYMPTOMS:** Authenticator spinner runs forever, no callback, hangs after broker killed

**CAUSE:** Broker activity lifecycle issues, force stop during auth

**SOLUTION:**
1. Implement timeout mechanism
2. Check broker process state before retry
3. Handle `BrokerActivity` destroyed gracefully

**RELATED:** #1396, #1631, #1997

---

### 4.5 Company Portal / Intune Integration Issues

**SYMPTOMS:** `COMPANY_PORTAL_REQUIRED` error, MAM enrollment issues

**CAUSE:** Intune MAM policy requires Company Portal

**SOLUTION:**
1. Ensure Company Portal installed & updated
2. Verify MAM enrollment for user
3. Check app is MAM-enabled in Intune admin center

**RELATED:** #1570, #1737, #1842, #2026, #2157

---

## 5. Build and Dependency Issues

### 5.1 AndroidX Compatibility

**SYMPTOMS:** Build errors, dependency conflicts with AndroidX

**SOLUTION:** Add to `gradle.properties`:
```properties
android.useAndroidX=true
android.enableJetifier=true
```

---

### 5.2 MSAL Version Conflicts

**SYMPTOMS:** Multiple MSAL versions in classpath, unexpected behavior

**SOLUTION:**
```gradle
implementation "com.microsoft.identity.client:msal:8.+"
```
Check `./gradlew dependencies` for conflicts, use `exclude` if needed

---

### 5.3 ProGuard/R8 Issues

**SYMPTOMS:** "Unsafe cipher mode" warning, missing classes, crashes in release builds

**SOLUTION:** Add ProGuard rules:
```proguard
-keepclassmembers class * implements javax.net.ssl.SSLSocketFactory { *; }
-keep class com.microsoft.identity.** { *; }
-keep class com.microsoft.aad.adal.** { *; }
-dontwarn com.microsoft.identity.**
-dontwarn com.nimbusds.**
-dontwarn net.jcip.annotations.**
-dontwarn edu.umd.cs.findbugs.annotations.**
```

**RELATED:** #1540, #1677, #1695, #2076, #2289, #2355

---

### 5.4 display-mask Dependency Resolution

**SYMPTOMS:** `Failed to resolve: com.microsoft.device.display:display-mask:0.3.0`

**CAUSE:** Microsoft dependency repository not accessible

**SOLUTION:** Add to `build.gradle`:
```gradle
maven {
    url 'https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1'
}
```

**RELATED:** #1027, #1648, #1720, #1722

---

## 6. Runtime Crashes

### 6.1 PCA Not Initialized

**SYMPTOMS:** `NullPointerException` on MSAL calls, crash on sign-in, "PCA is null"

**SOLUTION:** Always validate before use:
```java
if (mPCA == null) {
    Log.e(TAG, "PCA not initialized");
    return;
}
```

Initialize asynchronously:
```java
PublicClientApplication.createMultipleAccountPublicClientApplication(
    context, R.raw.auth_config, listener);
```

---

### 6.2 UI Thread Violations

**SYMPTOMS:** `CalledFromWrongThreadException`, "Only original thread can touch views", UI freezes

**SOLUTION:** Update UI on main thread:
```java
activity.runOnUiThread(() -> updateUI(result));
```

---

### 6.3 Android 15 Edge-to-Edge Display Issues

**SYMPTOMS:** Action bar overlaps content, login form unreadable, UI behind system bars

**CAUSE:** Android 15 (SDK 35) edge-to-edge default

**SOLUTION:**
1. Update to latest MSAL
2. Theme config:
```xml
<item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>
```
3. Workaround: Use browser auth (`authorization_user_agent: "BROWSER"`)

**RELATED:** #2204, #2341

---

### 6.4 Fragment Transaction Errors

**SYMPTOMS:** `FragmentManager already executing`, "No stored state", spinner runs forever with `withFragment()`

**CAUSE:** `withFragment()` lifecycle timing issues

**SOLUTION:** Use `startAuthorizationFromActivity()` instead:
```java
// ✅ RECOMMENDED
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .startAuthorizationFromActivity(requireActivity())
    .withScopes(SCOPES)
    .withCallback(callback)
    .build();
```

**RELATED:** #1725

---

### 6.5 ANR (Application Not Responding) Issues

**SYMPTOMS:** App freezes, ANR dialog, main thread blocked

**CAUSE:** PCA initialization or network operations on main thread

**SOLUTION:** Initialize PCA off main thread:
```java
Executors.newSingleThreadExecutor().execute(() -> {
    PublicClientApplication.createSingleAccountPublicClientApplication(
        context, R.raw.auth_config, callback);
});
```

**RELATED:** #1539, #1764, #1792, #1952

---

## 7. Single vs Multiple Account Mode Issues

### 7.1 Wrong API for Account Mode

**SYMPTOMS:** `ClassCastException`, method not found, unexpected behavior

**CAUSE:** Using Single Account API in Multiple Account app or vice versa

**MULTIPLE ACCOUNT APIs:**
```java
IMultipleAccountPublicClientApplication
acquireToken(parameters)
removeAccount(account, callback)
getAccounts(callback)
```

**SINGLE ACCOUNT APIs:**
```java
ISingleAccountPublicClientApplication
signIn(parameters)
signOut(callback)
getCurrentAccount(callback)
```

**RULE:** NEVER mix APIs - choose mode and stick to it

---

### 7.2 Account Mode Configuration Mismatch

**SYMPTOMS:** Wrong interface returned from PCA creation

**SOLUTION:** Match creation method to mode:
```java
// Multiple Account
PublicClientApplication.createMultipleAccountPublicClientApplication(...)

// Single Account
PublicClientApplication.createSingleAccountPublicClientApplication(...)
```

---

## 8. Silent Token Refresh Issues

### 8.1 Silent Token Acquisition Failure

**SYMPTOMS:** Silent token fails despite valid cached tokens, `MsalUiRequiredException`

**CAUSE:** Token expired, refresh token invalid, consent revoked, or wrong authority

**SOLUTION:**
```java
AcquireTokenSilentParameters params = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .fromAuthority(account.getAuthority())  // Critical!
    .withCallback(callback)
    .build();
mPCA.acquireTokenSilent(params);
```

Implement interactive fallback:
```java
if (exception instanceof MsalUiRequiredException) {
    // Fallback to interactive
    mPCA.acquireToken(interactiveParams);
}
```

**RELATED:** #1216, #1505, #1699, #1790, #2043, #2097

---

### 8.2 AADB2C90080 - Expired Grant Error

**SYMPTOMS:** `AADB2C90080: The provided grant has expired` during silent token refresh

**CAUSE:** B2C refresh token expired (default lifetime: 90 days)

**SOLUTION:**
1. Implement interactive login fallback
2. Consider token lifetime policies in Azure
3. Use authority hint in silent params

**RELATED:** #1004, #2257

---

### 8.3 No Cached Accounts Found

**SYMPTOMS:** "No cached accounts found for homeAccountId", silent token fails, `null` account

**CAUSE:** Account removed, cache cleared, wrong `homeAccountId`, or authority mismatch

**SOLUTION:**
1. Verify account exists before silent call:
```java
mPCA.getAccounts(new LoadAccountsCallback() {
    @Override
    public void onTaskCompleted(List<IAccount> accounts) {
        if (accounts.isEmpty()) {
            // Need interactive login
        }
    }
});
```
2. Use correct authority (especially for B2C)
3. Check if `removeAccount()` was called

**RELATED:** #1567, #1598, #1779, #2172

---

### 8.4 Silent Token Performance Issues

**SYMPTOMS:** Silent token takes too long, timeouts, poor UX

**CAUSE:** Network latency, cloud discovery overhead, cache lookup inefficiency

**SOLUTION:**
1. Pre-warm cache on app start
2. Use proper authority to avoid cloud discovery
3. Implement timeout and loading states
4. Consider caching token in-memory for short periods

**RELATED:** #2097

---

## 9. Network and Cloud Discovery Issues

### 9.1 Unable to Perform Cloud Discovery

**SYMPTOMS:** `UnknownHostException`, "Unable to perform cloud discovery", auth fails on network issues

**CAUSE:** Network connectivity issues, VPN blocking, firewall, or DNS problems

**SOLUTION:**
1. Check network connectivity before MSAL calls
2. Handle errors gracefully:
```java
if (exception instanceof MsalClientException && 
    exception.getErrorCode().equals("io_error")) {
    showMessage("Network error. Check connection.");
}
```
3. Use specific authority instead of "common" when possible
4. Verify VPN/firewall allows Azure AD endpoints

**RELATED:** #1568, #1663, #1696, #1723, #1765

---

### 9.2 Power Optimization / Doze Mode Issues

**SYMPTOMS:** Token refresh fails when device in Doze mode, background refresh blocked

**CAUSE:** Android power optimization blocking network during Doze

**SOLUTION:**
1. Use WorkManager for background token refresh
2. Request battery optimization exemption (if justified)
3. Handle failures and retry when device active

**RELATED:** #1766

---

### 9.3 SSL/TLS Certificate Issues

**SYMPTOMS:** SSL handshake failures, certificate validation errors

**CAUSE:** Custom certificate pinning, corporate proxies, outdated Android WebView

**SOLUTION:**
1. Update Android System WebView
2. Check corporate proxy/firewall certificates
3. Verify network security config if using custom certificates

---

## 10. WebView and Browser Issues

### 10.1 Custom Tab / Browser Issues

**SYMPTOMS:** "Chrome browser not found", only Chrome works, Mi Browser unsupported

**CAUSE:** MSAL requires Custom Tabs-compatible browser

**SOLUTION:**
1. Install Chrome or Edge
2. Configure browser preference in `auth_config.json`:
```json
{
    "authorization_user_agent": "WEBVIEW",
    "browser_safelist": [
        {"browser_package_name": "com.android.chrome"},
        {"browser_package_name": "com.microsoft.emmx"}
    ]
}
```

**RELATED:** #1509, #1591, #1626

---

### 10.2 Authorization Flow Cancellation

**SYMPTOMS:** "SDK cancelled the auth flow", "Unable to complete authorization"

**CAUSE:** Browser back button, activity destroyed, or Custom Tabs issue

**SOLUTION:**
1. Handle `MsalUserCancelException`
2. Implement proper activity lifecycle handling
3. Consider using `WEBVIEW` mode if Custom Tabs problematic

**RELATED:** #1561, #1562, #1581

---

### 10.3 WebView SSO Issues

**SYMPTOMS:** SSO asks for login again in WebView, no session persistence

**CAUSE:** WebView doesn't share cookies with system browser

**SOLUTION:**
1. Use `authorization_user_agent: "BROWSER"` for SSO
2. Or use broker for cross-app SSO
3. WebView mode doesn't support SSO by design

**RELATED:** #1582, #1734

---

## 11. Azure AD B2C Specific Issues

### 11.1 B2C Token Refresh Failures

**SYMPTOMS:** Silent token fails in B2C, `AADB2C90080` errors

**CAUSE:** B2C-specific token lifetimes, policy mismatch

**SOLUTION:**
1. Always specify authority with policy:
```java
.fromAuthority("https://tenant.b2clogin.com/tenant.onmicrosoft.com/B2C_1_policy")
```
2. Use same policy for silent token as original login
3. Implement interactive fallback

**RELATED:** #1004, #1547, #2257

---

### 11.2 B2C Policy Mismatch

**SYMPTOMS:** "No cached accounts found", wrong user attributes returned

**CAUSE:** Using different B2C policy for sign-in vs token refresh

**SOLUTION:**
1. Track which policy was used for sign-in
2. Use same policy for subsequent operations
3. Store policy name with account

---

### 11.3 B2C Custom Claims Issues

**SYMPTOMS:** Claims data is null, federated user attributes missing

**CAUSE:** Custom claims not properly configured in B2C policy

**SOLUTION:**
1. Verify claims mapping in B2C user flow
2. Use `IAccount.getClaims()` to retrieve:
```java
Map<String, ?> claims = account.getClaims();
String email = (String) claims.get("emails");
```
3. Check token using [jwt.ms](https://jwt.ms) to verify claims present

**RELATED:** #1491, #1749

---

## Diagnostic Checklist

When triaging issues, request:
1. MSAL version (check if >1.5 years old → unsupported)
2. Android version and device model
3. Account mode (Single/Multiple)
4. Complete error message with stack trace
5. Relevant config files (redacted)
6. Logcat with verbose logging enabled

**Enable Verbose Logging:**
```java
Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
Logger.getInstance().setEnableLogcatLog(true);
Logger.getInstance().setEnablePII(true); // Debug only - never in production
```

---

## Additional Resources

- **Config Template:** [auth_config.template.json](../../auth_config.template.json)
- **Code Examples:** [examples/](../../examples/)
- **MS Error Codes:** https://learn.microsoft.com/en-us/entra/identity-platform/reference-error-codes
- **MSAL Releases:** https://github.com/AzureAD/microsoft-authentication-library-for-android/releases

---

*Guide maintained by MSAL Android team. For unlisted issues, create detailed bug report with diagnostic info.*
