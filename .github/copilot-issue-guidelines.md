# MSAL Android Issue Troubleshooting Guidelines

> **Purpose:** This document provides consolidated troubleshooting guidance for GitHub Copilot, maintainers, and contributors responding to MSAL Android issues. Based on patterns from 181+ closed issues, it covers frequent errors, root causes, solutions, and response templates.

## Table of Contents

1. [General Troubleshooting Workflow](#general-troubleshooting-workflow)
2. [Authentication Flow Failures](#authentication-flow-failures)
3. [Initialization, Manifest & Configuration Problems](#initialization-manifest--configuration-problems)
4. [Token Acquisition & Refresh Issues](#token-acquisition--refresh-issues)
5. [Broker & Company Portal Problems](#broker--company-portal-problems)
6. [Device, SSO & Registration Issues](#device-sso--registration-issues)
7. [Crashes & ANRs](#crashes--anrs)
8. [Universal Troubleshooting Checklist](#universal-troubleshooting-checklist)
9. [Response Templates](#response-templates)
10. [Common Error Code Reference](#common-error-code-reference)

---

## General Troubleshooting Workflow

When responding to any MSAL Android issue:

1. **Acknowledge & Thank:** Always thank the user for reporting
2. **Gather Information:** Use the [Initial Response Template](#initial-response-template) if details are missing
3. **Identify Category:** Classify the issue (auth flow, config, tokens, broker, crash, etc.)
4. **Check Common Causes:** Review relevant section in this document
5. **Provide Solution:** Give specific steps with code examples when applicable
6. **Reference Documentation:** Link to official docs, examples, or similar resolved issues
7. **Follow Up:** Ask for confirmation that the solution worked

### Key Principles

- **Be Polite & Clear:** Avoid jargon; explain technical terms when needed
- **Be Specific:** Provide exact file paths, line numbers, and code snippets
- **Reference Examples:** Point to [`examples/hello-msal-multiple-account/`](../examples/hello-msal-multiple-account/) or [`examples/hello-msal-single-account/`](../examples/hello-msal-single-account/)
- **Enable Debugging:** Always suggest enabling verbose logging when troubleshooting
- **Security First:** Remind users never to share PII, tokens, or credentials

---

## Authentication Flow Failures

### Symptom: User Cannot Sign In / Authentication Never Completes

**Common Root Causes:**

1. **Redirect URI Mismatch**
   - `auth_config.json` redirect_uri doesn't match Azure AD app registration
   - Signature hash not URL encoded in `auth_config.json` (must be `%2A` not `*`)
   - Signature hash IS URL encoded in `AndroidManifest.xml` (must be raw)

2. **Missing Intent Filter in AndroidManifest.xml**
   - `BrowserTabActivity` not declared or misconfigured
   - Intent filter missing required categories (`DEFAULT`, `BROWSABLE`)

3. **Incorrect Scopes**
   - Requesting scopes not configured in Azure AD
   - Missing `.default` scope for application permissions

**Solutions:**

**Check Redirect URI Encoding:**
```json
// ✅ CORRECT - auth_config.json (URL encoded)
{
  "redirect_uri": "msauth://com.example.app/AbCdEf%2BgHiJk%3D"
}
```

```xml
<!-- ✅ CORRECT - AndroidManifest.xml (NOT URL encoded) -->
<data
    android:scheme="msauth"
    android:host="com.example.app"
    android:path="/AbCdEf+gHiJk=" />
```

**Verify BrowserTabActivity Declaration:**
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
            android:host="com.example.app"
            android:path="/YOUR_SIGNATURE_HASH" />
    </intent-filter>
</activity>
```

**Response Template:**
```markdown
Thank you for reporting this authentication issue. Let's verify your configuration:

1. **Check Redirect URI Encoding:**
   - In `auth_config.json`: Must be URL encoded (e.g., `%2A` for `*`, `%3D` for `=`)
   - In `AndroidManifest.xml`: Must NOT be URL encoded (use raw characters)

2. **Verify AndroidManifest.xml:**
   - Confirm `BrowserTabActivity` is declared with `android:exported="true"`
   - Ensure intent filter includes both `DEFAULT` and `BROWSABLE` categories

3. **Enable Verbose Logging:**
   ```java
   Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
   Logger.getInstance().setEnablePII(true); // Only for debugging
   ```

Please share the relevant parts of your `auth_config.json` and `AndroidManifest.xml` (with PII removed) so we can help further.
```

### Symptom: "Browser Required" / BROWSER_CODE_ERROR

**Root Cause:** No compatible browser installed or browser disabled on device

**Solution:**
```markdown
This error occurs when no compatible browser is available on the device. Solutions:

1. **Ensure a compatible browser is installed** (Chrome, Edge, Firefox, Samsung Internet)
2. **Enable broker authentication** to bypass browser requirement:
   ```json
   {
     "broker_redirect_uri_registered": true
   }
   ```
3. **Install Microsoft Authenticator or Company Portal** for broker-based authentication
4. **For testing:** Enable Chrome or another browser in device settings
```

### Symptom: Authentication Success but Callback Never Called

**Root Cause:** Activity lifecycle issues or missing `onActivityResult` handling

**Solution:**
```java
// Ensure you're not finishing the activity too early
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    // Don't call finish() here - let MSAL handle the result
}

// For Fragment-based flows, ensure Fragment is not detached
// when callback is invoked (use getActivity() != null checks)
```

---

## Initialization, Manifest & Configuration Problems

### Symptom: MsalClientException during MSAL Initialization

**Common Root Causes:**

1. **auth_config.json not found or malformed**
   - File not in `res/raw/` directory
   - Invalid JSON syntax
   - Missing required fields

2. **Client ID mismatch**
   - `client_id` in config doesn't match Azure AD app registration
   - Whitespace or special characters in client_id

3. **Authority URL issues**
   - Incorrect tenant ID or authority format
   - Using personal accounts (MSA) with organizational authority

**Solutions:**

**Verify File Location:**
```
app/
└── src/
    └── main/
        └── res/
            └── raw/
                └── auth_config.json  ← Must be here
```

**Validate JSON Structure:**
```json
{
  "client_id": "YOUR_CLIENT_ID",
  "authorization_user_agent": "DEFAULT",
  "redirect_uri": "msauth://com.example.app/SIGNATURE_HASH",
  "broker_redirect_uri_registered": true,
  "authorities": [
    {
      "type": "AAD",
      "audience": {
        "type": "AzureADMyOrg",
        "tenant_id": "YOUR_TENANT_ID"
      }
    }
  ]
}
```

**Check Initialization Code:**
```java
// ✅ CORRECT - Parameters-based initialization
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
            Log.e(TAG, "Failed to create PCA: " + exception.getMessage());
            // Handle initialization error
        }
    }
);
```

**Response Template:**
```markdown
It looks like MSAL is failing to initialize. Let's check the following:

1. **File Location:** Confirm `auth_config.json` is in `app/src/main/res/raw/` directory
2. **JSON Validity:** Validate your JSON using a linter (no trailing commas, proper quotes)
3. **Required Fields:** Ensure all mandatory fields are present:
   - `client_id`
   - `redirect_uri`
   - `authorities` array
4. **Client ID Format:** Verify the client_id is a valid GUID with no extra spaces

Please share your `auth_config.json` (with sensitive values replaced with placeholders) for review.
```

### Symptom: "Broker Redirect URI Not Registered" Warning

**Root Cause:** Redirect URI not configured in Azure AD app registration

**Solution:**
```markdown
This warning indicates the broker redirect URI hasn't been added to your Azure AD app registration:

1. **Go to Azure Portal** → App Registrations → Your App → Authentication
2. **Add Platform** → Android
3. **Enter Package Name** and **Signature Hash**
4. **Azure will generate the redirect URI** automatically
5. **Save changes**

Alternatively, manually add the redirect URI in format:
```
msauth://com.example.app/YOUR_SIGNATURE_HASH
```

After updating Azure AD, the warning should disappear within a few minutes.
```

### Symptom: AndroidManifest.xml Merge Conflicts

**Root Cause:** Multiple libraries trying to declare the same activity or conflicts with app's manifest

**Solution:**
```xml
<!-- If you need to customize BrowserTabActivity, use tools:replace -->
<activity
    android:name="com.microsoft.identity.client.BrowserTabActivity"
    android:exported="true"
    tools:replace="android:exported">
    <!-- Your intent filters -->
</activity>

<!-- Ensure you have the tools namespace at the top -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.example.app">
```

---

## Token Acquisition & Refresh Issues

### Symptom: acquireTokenSilent Fails with NO_ACCOUNT_FOUND

**Root Cause:** Account doesn't exist in cache or was removed

**Solution:**
```java
// Always check if account exists before silent token acquisition
mPCA.getAccounts(new IPublicClientApplication.LoadAccountsCallback() {
    @Override
    public void onTaskCompleted(List<IAccount> result) {
        if (result.isEmpty()) {
            // No accounts in cache - need interactive sign-in
            performInteractiveSignIn();
        } else {
            // Account exists - proceed with silent token acquisition
            performSilentTokenAcquisition(result.get(0));
        }
    }
    
    @Override
    public void onError(MsalException exception) {
        // Handle error
    }
});
```

**Response Template:**
```markdown
The `NO_ACCOUNT_FOUND` error means MSAL doesn't have a cached account. This can happen if:

1. User hasn't signed in yet
2. Account was removed via `removeAccount()` or `signOut()`
3. Cache was cleared (app data cleared, reinstall, etc.)

**Solution:** Call `acquireToken()` (interactive) instead of `acquireTokenSilent()`:

```java
AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
    .startAuthorizationFromActivity(activity)
    .withScopes(SCOPES)
    .withCallback(getAuthCallback())
    .build();

mPCA.acquireToken(parameters);
```

After successful interactive sign-in, subsequent silent token requests should work.
```

### Symptom: INVALID_SCOPE Error

**Root Cause:** Requesting scopes not granted to the application

**Solution:**
```markdown
This error occurs when requesting scopes that aren't configured in Azure AD:

1. **Go to Azure Portal** → App Registrations → Your App → API Permissions
2. **Verify the requested scopes are listed**
3. **Grant admin consent** if required (for organization-only scopes)
4. **For Microsoft Graph:** Use format `https://graph.microsoft.com/.default` or specific scopes like `User.Read`

**Example:**
```java
private static final String[] SCOPES = {"User.Read", "Mail.Read"};

// Or for all configured scopes:
private static final String[] SCOPES = {"https://graph.microsoft.com/.default"};
```

**Note:** Scope names are case-sensitive. Ensure exact match with Azure AD configuration.
```

### Symptom: Refresh Token Expired / User Must Sign In Again

**Root Cause:** Refresh token expired (after 90 days default) or revoked

**Solution:**
```markdown
Refresh tokens have a limited lifetime (default 90 days for single-factor, shorter for MFA). When they expire:

1. **Expected Behavior:** MSAL will prompt for interactive sign-in automatically
2. **Handle gracefully in your app:**
   ```java
   mPCA.acquireTokenSilent(parameters, new AuthenticationCallback() {
       @Override
       public void onSuccess(IAuthenticationResult result) {
           // Token refreshed successfully
       }
       
       @Override
       public void onError(MsalException exception) {
           if (exception instanceof MsalUiRequiredException) {
               // Refresh token expired - need interactive sign-in
               performInteractiveSignIn();
           }
       }
       
       @Override
       public void onCancel() {
           // User cancelled
       }
   });
   ```

3. **Prevention:** Keep app usage regular (within 90-day window) or implement proactive refresh
```

### Symptom: Token Claims Don't Include Expected Data

**Root Cause:** Optional claims not configured in Azure AD app manifest

**Solution:**
```markdown
To include additional claims in tokens:

1. **Go to Azure Portal** → App Registrations → Your App → Token Configuration
2. **Add optional claim** for the token type you need (ID token, access token)
3. **Common claims:**
   - `email` - User's email address
   - `upn` - User Principal Name
   - `groups` - Group memberships (requires Graph permission)

**Or edit app manifest directly:**
```json
{
  "optionalClaims": {
    "idToken": [
      {
        "name": "email",
        "source": null,
        "essential": false,
        "additionalProperties": []
      }
    ]
  }
}
```

**Access claims in your app:**
```java
IAuthenticationResult result = // ... from acquireToken
IClaimable account = result.getAccount();
Map<String, ?> claims = account.getClaims();
String email = (String) claims.get("email");
```
```

---

## Broker & Company Portal Problems

### Symptom: Broker Not Working / Always Falls Back to Browser

**Common Root Causes:**

1. **Microsoft Authenticator or Company Portal not installed**
2. **Broker redirect URI not registered in Azure AD**
3. **App signature hash mismatch**
4. **Broker authentication disabled in config**

**Solutions:**

**Enable Broker in Configuration:**
```json
{
  "broker_redirect_uri_registered": true,
  "authorization_user_agent": "DEFAULT"  // or "BROKER"
}
```

**Verify Broker Installation:**
```java
// Check if broker is available
boolean isBrokerAvailable = mPCA.isBrokerAvailable();
Log.d(TAG, "Broker available: " + isBrokerAvailable);
```

**Get Correct Signature Hash:**
```bash
# Get signature hash for broker registration
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android | grep SHA1

# Or use this code in your app:
try {
    PackageInfo info = getPackageManager().getPackageInfo(
        getPackageName(), 
        PackageManager.GET_SIGNATURES
    );
    for (Signature signature : info.signatures) {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(signature.toByteArray());
        String hash = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
        Log.d(TAG, "Signature Hash: " + hash);
    }
} catch (Exception e) {
    Log.e(TAG, "Error getting signature", e);
}
```

**Response Template:**
```markdown
For broker authentication to work, you need:

1. **Install Broker App:**
   - Microsoft Authenticator (preferred): [Play Store Link](https://play.google.com/store/apps/details?id=com.azure.authenticator)
   - Company Portal: [Play Store Link](https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal)

2. **Enable in Configuration:**
   ```json
   {
     "broker_redirect_uri_registered": true
   }
   ```

3. **Register Redirect URI in Azure AD:**
   - Go to Azure Portal → Your App → Authentication → Add Platform → Android
   - Enter your package name and signature hash
   - Azure generates the broker redirect URI automatically

4. **Verify Signature Hash Matches:**
   - Use the code snippet above to get your app's signature hash
   - Ensure it matches what's registered in Azure AD (case-sensitive)

After setup, test with:
```java
boolean brokerAvailable = mPCA.isBrokerAvailable();
Log.d(TAG, "Broker available: " + brokerAvailable);
```
```

### Symptom: Company Portal Required but User Doesn't Have Admin Rights

**Root Cause:** Device requires Company Portal for organizational policies, but user cannot install

**Solution:**
```markdown
Company Portal is required when:
- Device is managed by organization (MDM/MAM)
- Conditional Access policies require managed devices
- Organization enforces app protection policies

**Options:**

1. **Contact IT Admin:** Request Company Portal installation or device enrollment
2. **Use Personal Device:** If policy allows, authenticate on personal (unmanaged) device
3. **Request Policy Exception:** For development/testing, ask admin for policy exception
4. **Use Emulator:** Test with emulator that has Company Portal pre-installed

**For Development/Testing:**
- Use the Android Emulator with Company Portal installed
- Or use Microsoft Authenticator as broker (doesn't require device management)
```

### Symptom: BROKER_BIND_FAILURE / Cannot Connect to Broker

**Root Cause:** Broker app not responding or version incompatibility

**Solution:**
```markdown
This error indicates MSAL cannot communicate with the broker. Try:

1. **Update Broker App:**
   - Update Microsoft Authenticator to latest version
   - Or update Company Portal to latest version

2. **Clear Broker Data:**
   - Go to Settings → Apps → Microsoft Authenticator (or Company Portal)
   - Clear Cache and Clear Data
   - Sign in again

3. **Reinstall Broker:**
   - Uninstall Microsoft Authenticator / Company Portal
   - Reinstall from Play Store
   - Sign in and try authentication again

4. **Check Logs:**
   Enable verbose logging to see detailed broker communication:
   ```java
   Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
   Logger.getInstance().setEnablePII(true);
   ```

5. **Fallback to Browser:**
   If broker continues to fail, temporarily disable for testing:
   ```json
   {
     "broker_redirect_uri_registered": false,
     "authorization_user_agent": "WEBVIEW"
   }
   ```
```

---

## Device, SSO & Registration Issues

### Symptom: SSO Not Working Between Apps

**Root Cause:** Apps don't share the same signature or broker not enabled

**Solution:**
```markdown
For SSO to work across apps:

1. **Apps Must Be Signed with Same Certificate:**
   - Use same keystore for all apps
   - Or use same certificate authority signature

2. **Broker Must Be Enabled in All Apps:**
   ```json
   {
     "broker_redirect_uri_registered": true
   }
   ```

3. **Broker App Must Be Installed:**
   - Microsoft Authenticator or Company Portal

4. **All Apps Must Use Same Redirect URI Format:**
   - `msauth://PACKAGE_NAME/SIGNATURE_HASH`
   - Signature hash must match across apps

5. **Use Multiple Account Mode:**
   Single account mode doesn't support SSO across apps well

**Testing SSO:**
```java
// In App A - Sign in
mPCA.acquireToken(params);

// In App B - Should get token silently without prompting
mPCA.acquireTokenSilent(params); // Should succeed if SSO working
```
```

### Symptom: DEVICE_REGISTRATION_NEEDED Error

**Root Cause:** Conditional Access policy requires device to be registered/managed

**Solution:**
```markdown
This error means your organization's Conditional Access policy requires:

1. **Device Registration (Azure AD Join or Hybrid Join)**
   - Corporate-owned devices should be registered with Azure AD
   - Contact IT admin to register device

2. **Or Intune Enrollment (MDM)**
   - Install Company Portal
   - Sign in and complete device enrollment

3. **Or App Protection Policy Compliance**
   - Ensure Company Portal is installed and signed in
   - Check compliance status in Company Portal app

**Cannot Register Device?**
- Contact IT admin for assistance
- Request policy exception for development/testing
- Use compliant device for production testing

**For Developers:**
- Use Azure AD joined development machine
- Or request test tenant without Conditional Access requirements
```

### Symptom: "This App Requires Intune Company Portal"

**Root Cause:** MAM (Mobile Application Management) policy enforced

**Solution:**
```markdown
Your organization requires Intune Company Portal for app protection:

1. **Install Company Portal:**
   [Play Store Link](https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal)

2. **Sign In to Company Portal:**
   - Open Company Portal app
   - Sign in with your organizational account
   - Complete setup wizard

3. **Enable MAM SDK (for developers):**
   If you're developing an app that needs MAM support:
   ```gradle
   dependencies {
       implementation 'com.microsoft.intune.mam:android-sdk:+'
   }
   ```
   
4. **Test Without MAM (development only):**
   - Use test tenant without MAM policies
   - Or request policy exception from IT admin

**Note:** Company Portal is separate from Microsoft Authenticator. Both may be needed.
```

---

## Crashes & ANRs

### Symptom: NullPointerException in MSAL Callback

**Root Cause:** Accessing null objects or callbacks invoked after Activity destroyed

**Solution:**
```java
// ✅ CORRECT - Always check for null and activity state
private final AuthenticationCallback mCallback = new AuthenticationCallback() {
    @Override
    public void onSuccess(IAuthenticationResult result) {
        if (getActivity() == null || isDetached()) {
            return; // Fragment no longer attached
        }
        
        // Update UI on main thread
        requireActivity().runOnUiThread(() -> {
            // Update UI here
        });
    }
    
    @Override
    public void onError(MsalException exception) {
        if (getActivity() == null || isDetached()) {
            return;
        }
        
        Log.e(TAG, "Authentication failed", exception);
        // Handle error
    }
    
    @Override
    public void onCancel() {
        if (getActivity() == null || isDetached()) {
            return;
        }
        
        // Handle cancellation
    }
};

// Store reference to PCA safely
private volatile IMultipleAccountPublicClientApplication mPCA;

// Always check before using
private void useAccount() {
    if (mPCA == null) {
        Log.e(TAG, "PCA not initialized");
        return;
    }
    
    mPCA.getAccounts(/* ... */);
}
```

**Response Template:**
```markdown
To prevent NPEs in MSAL callbacks:

1. **Check Activity/Fragment Lifecycle:**
   ```java
   if (getActivity() == null || isDetached()) {
       return;
   }
   ```

2. **Update UI on Main Thread:**
   ```java
   runOnUiThread(() -> {
       // UI updates here
   });
   ```

3. **Use Weak References for Long-Lived Objects:**
   ```java
   private WeakReference<Activity> mActivityRef;
   ```

4. **Validate PCA Before Use:**
   ```java
   if (mPCA == null) {
       Log.e(TAG, "PCA not initialized");
       return;
   }
   ```

Please share the full stack trace for more specific guidance.
```

### Symptom: ANR (Application Not Responding) During Token Acquisition

**Root Cause:** Blocking main thread with synchronous operations

**Solution:**
```java
// ❌ WRONG - Never call MSAL operations synchronously on main thread
// MSAL doesn't provide synchronous APIs for good reason

// ✅ CORRECT - All MSAL operations are async
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .withScopes(SCOPES)
    .withCallback(new AuthenticationCallback() {
        @Override
        public void onSuccess(IAuthenticationResult result) {
            // Callback runs on background thread
            // Update UI on main thread
            runOnUiThread(() -> {
                updateUI(result);
            });
        }
        
        @Override
        public void onError(MsalException exception) {
            runOnUiThread(() -> {
                handleError(exception);
            });
        }
        
        @Override
        public void onCancel() {
            runOnUiThread(() -> {
                handleCancellation();
            });
        }
    })
    .build();

mPCA.acquireToken(params);
```

**Never Do This:**
```java
// ❌ DON'T attempt to make MSAL synchronous
// This will cause ANR
Thread thread = new Thread(() -> {
    mPCA.acquireToken(params);
});
thread.start();
thread.join(); // Blocks main thread - causes ANR!
```

### Symptom: IllegalStateException: Fragment Not Attached

**Root Cause:** Fragment destroyed before MSAL callback invoked

**Solution:**
```java
// Store Activity reference instead of Fragment context
private WeakReference<Activity> mActivityRef;

@Override
public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    mActivityRef = new WeakReference<>((Activity) context);
}

// Use in MSAL initialization
private void initializeMsal() {
    Activity activity = mActivityRef.get();
    if (activity == null) {
        return;
    }
    
    PublicClientApplication.createMultipleAccountPublicClientApplication(
        activity.getApplicationContext(), // Use Application context
        R.raw.auth_config,
        new IMultipleAccountApplicationCreatedListener() {
            @Override
            public void onCreated(IMultipleAccountPublicClientApplication pca) {
                mPCA = pca;
            }
            
            @Override
            public void onError(MsalException exception) {
                Log.e(TAG, "Failed to create PCA", exception);
            }
        }
    );
}

// Check before updating UI
private void updateUI() {
    Activity activity = mActivityRef.get();
    if (activity == null || activity.isFinishing() || isDetached()) {
        return;
    }
    
    // Safe to update UI
}
```

### Symptom: OutOfMemoryError or Memory Leak

**Root Cause:** Activity/Context references held after destruction

**Solution:**
```markdown
To prevent memory leaks with MSAL:

1. **Use Application Context for PCA Creation:**
   ```java
   PublicClientApplication.create(
       getApplicationContext(), // Not activity context
       R.raw.auth_config,
       listener
   );
   ```

2. **Don't Store Activity References in Static Fields:**
   ```java
   // ❌ WRONG
   private static Activity sActivity;
   
   // ✅ CORRECT
   private WeakReference<Activity> mActivityRef;
   ```

3. **Clean Up in onDestroy:**
   ```java
   @Override
   protected void onDestroy() {
       super.onDestroy();
       mCallback = null;
       mActivityRef.clear();
   }
   ```

4. **Use LeakCanary for Detection:**
   ```gradle
   debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
   ```

5. **Profile Memory Usage:**
   - Use Android Studio Memory Profiler
   - Look for retained Activity instances after rotation/navigation
```

---

## Universal Troubleshooting Checklist

When diagnosing any MSAL Android issue, systematically check:

### ✅ Configuration Checklist

- [ ] **MSAL Version:** Using 7.0.0 or later? Check `build.gradle`
- [ ] **AndroidX Enabled:** `android.useAndroidX=true` in `gradle.properties`
- [ ] **Jetifier Enabled:** `android.enableJetifier=true` in `gradle.properties`
- [ ] **Min SDK:** `minSdk 24` or higher
- [ ] **Internet Permission:** `<uses-permission android:name="android.permission.INTERNET" />` in manifest
- [ ] **Network State Permission:** `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` in manifest

### ✅ auth_config.json Checklist

- [ ] **File Location:** In `res/raw/auth_config.json`
- [ ] **Valid JSON:** No syntax errors, proper quotes, no trailing commas
- [ ] **Client ID:** Valid GUID matching Azure AD app registration
- [ ] **Redirect URI:** URL encoded (use `%2A` for `*`, `%3D` for `=`)
- [ ] **Broker Enabled:** `"broker_redirect_uri_registered": true` (recommended)
- [ ] **Authority:** Correct type (`AAD`, `B2C`, or `MSA`) and tenant ID

### ✅ AndroidManifest.xml Checklist

- [ ] **BrowserTabActivity:** Declared with `android:exported="true"`
- [ ] **Intent Filter:** Includes `VIEW` action, `DEFAULT` and `BROWSABLE` categories
- [ ] **Redirect URI:** Package name matches `android:host`, signature hash matches `android:path`
- [ ] **Signature Hash:** NOT URL encoded (use raw characters like `*` and `=`)
- [ ] **Scheme:** `android:scheme="msauth"`

### ✅ Azure AD Configuration Checklist

- [ ] **App Registered:** Application registered in Azure AD / Azure AD B2C
- [ ] **Redirect URI Added:** Broker redirect URI added to Authentication → Platform configurations → Android
- [ ] **Package Name:** Matches your app's package name exactly
- [ ] **Signature Hash:** Matches your app's signature (get from debug/release keystore)
- [ ] **API Permissions:** Required scopes added and consented (e.g., `User.Read`)
- [ ] **Supported Account Types:** Matches your authority configuration

### ✅ Code Implementation Checklist

- [ ] **Parameters-Based APIs:** Using `AcquireTokenParameters.Builder()` not deprecated methods
- [ ] **Null Checks:** Checking `mPCA != null` before MSAL operations
- [ ] **Account Mode:** Using correct mode (multiple vs. single account) consistently
- [ ] **Error Handling:** Implementing all callback methods (`onSuccess`, `onError`, `onCancel`)
- [ ] **Thread Safety:** Updating UI on main thread with `runOnUiThread()`
- [ ] **Lifecycle Awareness:** Checking activity/fragment state in callbacks

### ✅ Debugging Checklist

- [ ] **Verbose Logging Enabled:**
  ```java
  Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
  Logger.getInstance().setEnablePII(true); // Only for debugging
  ```
- [ ] **Logcat Filtered:** Using filter `tag:MSAL` or `tag:AuthenticationActivity`
- [ ] **Stack Trace:** Full exception stack trace captured
- [ ] **Network Traffic:** Checked using proxy (Fiddler/Charles) if needed
- [ ] **Device/Emulator:** Using supported Android version (API 24+)

### ✅ Common Mistakes to Avoid

- [ ] **Not Using Golden Examples:** Always reference [`examples/`](../examples/) directory
- [ ] **Mixing Account Modes:** Never use `getCurrentAccount()` in multiple account app
- [ ] **Wrong Encoding:** Remember: URL encoded in JSON, NOT encoded in XML
- [ ] **Deprecated APIs:** Avoid non-parameters-based methods
- [ ] **Hardcoded Resources:** Use string resources, not hardcoded client IDs in production
- [ ] **Insufficient Scopes:** Request all needed scopes upfront
- [ ] **Browser Only:** Not enabling broker for better SSO and security

---

## Response Templates

### Initial Response Template

Use this when the issue lacks sufficient information:

```markdown
Thank you for reporting this issue! To help us assist you better, could you please provide:

**Environment Details:**
- [ ] Device model and Android version (e.g., Pixel 5, Android 12)
- [ ] MSAL Android version (check `build.gradle` dependencies)
- [ ] Authentication flow (multiple account or single account mode)
- [ ] Broker app installed? (Microsoft Authenticator / Company Portal)

**Configuration:**
- [ ] Relevant parts of `auth_config.json` (replace sensitive values with placeholders)
- [ ] `BrowserTabActivity` declaration from `AndroidManifest.xml`
- [ ] Package name and how you obtained the signature hash

**Issue Details:**
- [ ] Detailed steps to reproduce the issue
- [ ] Expected behavior vs. actual behavior
- [ ] Complete error message / stack trace
- [ ] Logcat output with MSAL verbose logging enabled:
  ```java
  Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
  Logger.getInstance().setEnablePII(true); // Only for debugging
  ```

**Screenshots:**
- [ ] Any error dialogs or unexpected UI behavior

⚠️ **Important:** Please do NOT include sensitive information like actual tokens, credentials, or PII.

Once we have these details, we'll be able to provide more specific guidance.
```

### Configuration Error Template

```markdown
Thank you for providing those details. This looks like a configuration issue. Let's verify:

**1. Check Redirect URI Encoding:**

In `auth_config.json`, the redirect URI must be URL encoded:
```json
{
  "redirect_uri": "msauth://YOUR.PACKAGE.NAME/YOUR_SIGNATURE%2BHASH%3D"
}
```
Note: `+` becomes `%2B`, `=` becomes `%3D`, `/` becomes `%2F`

In `AndroidManifest.xml`, the signature hash must NOT be URL encoded:
```xml
<data
    android:scheme="msauth"
    android:host="YOUR.PACKAGE.NAME"
    android:path="/YOUR_SIGNATURE+HASH=" />
```

**2. Verify BrowserTabActivity:**

Ensure it's declared correctly:
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
            android:host="YOUR.PACKAGE.NAME"
            android:path="/YOUR_SIGNATURE_HASH" />
    </intent-filter>
</activity>
```

**3. Azure AD Portal:**

- Navigate to Azure Portal → App Registrations → Your App → Authentication
- Under "Platform configurations", ensure Android platform is added with correct package name and signature hash
- The redirect URI shown should match your `auth_config.json` (Azure shows it URL encoded)

Please try these corrections and let us know if the issue persists.
```

### Token Issue Template

```markdown
Thank you for reporting this token acquisition issue. Based on the error, here's what you need to check:

**For `NO_ACCOUNT_FOUND` or `NO_CURRENT_ACCOUNT`:**

This means no account is cached. You need interactive sign-in first:

```java
// Use acquireToken (interactive) not acquireTokenSilent
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .startAuthorizationFromActivity(activity)
    .withScopes(Arrays.asList("User.Read"))
    .withCallback(getAuthenticationCallback())
    .build();

mPCA.acquireToken(params);
```

After successful interactive sign-in, you can use `acquireTokenSilent()` for subsequent requests.

**For `INVALID_SCOPE` error:**

1. Go to Azure Portal → App Registrations → Your App → API Permissions
2. Ensure the scopes you're requesting are added (e.g., `User.Read`, `Mail.Read`)
3. Grant admin consent if required
4. Scope names are case-sensitive - ensure exact match

**For `MsalUiRequiredException`:**

This means the cached token expired and user interaction is needed:

```java
mPCA.acquireTokenSilent(params, new AuthenticationCallback() {
    @Override
    public void onError(MsalException exception) {
        if (exception instanceof MsalUiRequiredException) {
            // Fall back to interactive sign-in
            mPCA.acquireToken(interactiveParams);
        } else {
            // Handle other errors
        }
    }
    
    // ... other callback methods
});
```

Please try the relevant solution above and let us know the result.
```

### Broker Issue Template

```markdown
Thank you for reporting this broker authentication issue. Let's troubleshoot:

**Step 1: Verify Broker App Installation**

Ensure one of these is installed:
- [Microsoft Authenticator](https://play.google.com/store/apps/details?id=com.azure.authenticator) (recommended)
- [Company Portal](https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal)

Check if broker is available:
```java
boolean brokerAvailable = mPCA.isBrokerAvailable();
Log.d(TAG, "Broker available: " + brokerAvailable);
```

**Step 2: Enable Broker in Configuration**

In `auth_config.json`:
```json
{
  "broker_redirect_uri_registered": true,
  "authorization_user_agent": "DEFAULT"
}
```

**Step 3: Register Redirect URI in Azure AD**

1. Go to Azure Portal → App Registrations → Your App → Authentication
2. Click "Add a platform" → "Android"
3. Enter:
   - **Package name:** Your app's package (e.g., `com.example.myapp`)
   - **Signature hash:** Get from keystore (see below)
4. Save changes

**Get Your Signature Hash:**
```bash
# For debug keystore
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Look for "SHA1" line, copy the hash
```

Or get it programmatically in your app:
```java
try {
    PackageInfo info = getPackageManager().getPackageInfo(
        getPackageName(),
        PackageManager.GET_SIGNATURES
    );
    for (Signature signature : info.signatures) {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(signature.toByteArray());
        String hash = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
        Log.d("SignatureHash", hash);
    }
} catch (Exception e) {
    e.printStackTrace();
}
```

**Step 4: Verify Signature Hash Match**

The signature hash in:
- `AndroidManifest.xml` (NOT URL encoded): `/AbCdEf+GhIj=`
- `auth_config.json` (URL encoded): `/AbCdEf%2BGhIj%3D`
- Azure AD portal: Should match your app's actual signature

After these steps, broker authentication should work. If issues persist, please share the output of `isBrokerAvailable()` and any error messages.
```

### Crash Report Template

```markdown
Thank you for reporting this crash. To help us diagnose:

**Immediate Steps:**

1. **Get Full Stack Trace:**
   - Enable verbose logging:
     ```java
     Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
     Logger.getInstance().setEnablePII(true); // Only for debugging
     ```
   - Reproduce the crash
   - Capture full logcat output with filter `tag:MSAL` or `tag:AndroidRuntime`

2. **Check Common Causes:**

   **For NullPointerException:**
   - Verify PCA is initialized before use: `if (mPCA == null) return;`
   - Check activity/fragment lifecycle in callbacks
   - Ensure UI updates on main thread: `runOnUiThread(() -> {...})`

   **For IllegalStateException:**
   - Check fragment attachment: `if (isDetached() || getActivity() == null) return;`
   - Use weak references for long-lived callbacks

   **For OutOfMemoryError:**
   - Use `getApplicationContext()` for PCA creation, not activity context
   - Avoid static references to activities or contexts

3. **Provide Information:**
   - [ ] Full stack trace from crash
   - [ ] Code where crash occurs (relevant snippets)
   - [ ] Steps to reproduce consistently
   - [ ] Device model and Android version
   - [ ] MSAL version

**Common Quick Fixes:**

```java
// Safe callback implementation
private final AuthenticationCallback mCallback = new AuthenticationCallback() {
    @Override
    public void onSuccess(IAuthenticationResult result) {
        if (getActivity() == null || isDetached()) return;
        
        requireActivity().runOnUiThread(() -> {
            // Update UI safely
        });
    }
    
    @Override
    public void onError(MsalException exception) {
        if (getActivity() == null || isDetached()) return;
        
        requireActivity().runOnUiThread(() -> {
            // Handle error safely
        });
    }
    
    @Override
    public void onCancel() {
        if (getActivity() == null || isDetached()) return;
        
        requireActivity().runOnUiThread(() -> {
            // Handle cancellation safely
        });
    }
};
```

Please share the stack trace and we'll provide more specific guidance.
```

### Resolution Confirmation Template

```markdown
Thank you for trying those steps! I'm glad to hear [solution] worked for you.

**Summary of Solution:**
[Brief recap of what fixed the issue]

**To prevent this in the future:**
[Any preventive measures or best practices]

**Additional Resources:**
- [Link to relevant documentation]
- [Link to golden example if applicable]
- [Link to similar resolved issues if applicable]

If you encounter any other issues or have questions, feel free to open a new issue. We're here to help!

**Closing this issue as resolved.** Please reopen if the problem persists.
```

### Escalation Template

Use when the issue requires deeper investigation:

```markdown
Thank you for providing those details. This issue appears to require deeper investigation:

**What we've ruled out:**
- [List what's been checked/verified]

**What we still need:**
- [ ] [Specific logs or configuration details]
- [ ] [Reproduction steps in minimal sample app]
- [ ] [Additional environment information]

**Next Steps:**

1. **Create a Minimal Reproduction:**
   - Start with our golden example: [hello-msal-multiple-account](../examples/hello-msal-multiple-account)
   - Add only the code that triggers the issue
   - Share the minimal sample (if possible)

2. **Capture Detailed Logs:**
   ```java
   Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
   Logger.getInstance().setEnablePII(true); // Only for debugging
   Logger.getInstance().setExternalLogger(message -> {
       // Save logs to file for sharing
       Log.d("MSAL", message);
   });
   ```

3. **Check for Similar Issues:**
   - Search closed issues: [repository issues](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues?q=is%3Aissue)
   - Check Stack Overflow: [msal tag](https://stackoverflow.com/questions/tagged/msal)

**For Production Issues:**
If this is affecting your production app, please open a support ticket with Microsoft through your organization's support channel for faster resolution.

We'll continue investigating and update this issue as we learn more.
```

---

## Common Error Code Reference

Quick reference for frequent MSAL error codes:

| Error Code | Common Cause | Quick Fix |
|------------|--------------|-----------|
| `NO_ACCOUNT_FOUND` | No cached account | Use `acquireToken()` (interactive) first |
| `NO_CURRENT_ACCOUNT` | No account in single-account mode | Call `signIn()` first |
| `INVALID_SCOPE` | Scope not configured in Azure AD | Add scope to API Permissions |
| `INVALID_GRANT` | Token/refresh token invalid | Clear cache, sign in again |
| `BROWSER_CODE_ERROR` | No compatible browser | Install Chrome/Edge or enable broker |
| `BROKER_BIND_FAILURE` | Cannot connect to broker | Update/reinstall Authenticator/Company Portal |
| `DEVICE_REGISTRATION_NEEDED` | Device not registered | Register device with Azure AD or Intune |
| `MSALUiRequiredException` | User interaction needed | Fall back to `acquireToken()` (interactive) |
| `AUTHORITY_VALIDATION_FAILED` | Invalid authority URL | Check authority format in config |
| `REDIRECT_URI_MISMATCH` | Redirect URI config error | Verify encoding and Azure AD registration |

### Getting More Error Details

Always enable verbose logging to see detailed error information:

```java
Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
Logger.getInstance().setEnablePII(true); // Only for debugging

// In error callback
@Override
public void onError(MsalException exception) {
    Log.e(TAG, "Error code: " + exception.getErrorCode());
    Log.e(TAG, "Error message: " + exception.getMessage());
    
    if (exception instanceof MsalClientException) {
        // Client-side error (configuration, network, etc.)
    } else if (exception instanceof MsalServiceException) {
        // Server-side error (Azure AD returned error)
        MsalServiceException serviceEx = (MsalServiceException) exception;
        Log.e(TAG, "HTTP status: " + serviceEx.getHttpStatusCode());
    } else if (exception instanceof MsalUiRequiredException) {
        // User interaction required - not really an error
    }
}
```

---

## Additional Tips

### For Maintainers

When responding to issues:

1. **Search History First:** Check if similar issues were resolved before
2. **Use Templates:** Adapt templates from this guide for consistency
3. **Ask for Specifics:** Don't assume - request exact config/code/logs
4. **Reference Examples:** Always point to `examples/` directory
5. **Close Duplicates:** Link to original issue and close duplicates
6. **Label Appropriately:** Use `bug`, `question`, `documentation`, `config-issue`, etc.
7. **Follow Up:** Check if solution worked before closing

### For Users

When reporting issues:

1. **Search First:** Check if your issue was already reported/resolved
2. **Use Golden Examples:** Test with unmodified example apps first
3. **Provide Complete Info:** Use the checklist from Initial Response Template
4. **Enable Logging:** Always include logs with verbose logging enabled
5. **Redact Secrets:** Replace tokens, client IDs, tenant IDs with placeholders
6. **Minimal Reproduction:** Strip down to simplest code that reproduces issue
7. **Be Responsive:** Reply to maintainer questions promptly

### Best Practices

**For Everyone:**

- **Keep MSAL Updated:** Use latest 7.x version
- **Follow Examples:** Don't deviate from patterns in `examples/` directory
- **Test with Broker:** Always test with Microsoft Authenticator installed
- **Read Documentation:** Check official docs at [docs.microsoft.com](https://docs.microsoft.com/en-us/azure/active-directory/develop/msal-android-overview)
- **Be Patient:** Complex issues may take time to diagnose
- **Be Respectful:** Remember there are humans on both sides

---

## Helpful Resources

### Official Documentation
- [MSAL Android Overview](https://docs.microsoft.com/en-us/azure/active-directory/develop/msal-android-overview)
- [MSAL Android Configuration](https://docs.microsoft.com/en-us/azure/active-directory/develop/msal-configuration)
- [Broker Authentication](https://docs.microsoft.com/en-us/azure/active-directory/develop/msal-android-single-sign-on)

### Repository Resources
- [Multiple Account Example](../examples/hello-msal-multiple-account/)
- [Single Account Example](../examples/hello-msal-single-account/)
- [Code Snippets](../snippets/)
- [Config Template](../auth_config.template.json)
- [Copilot Instructions](./.copilot-instructions.md)

### Community
- [Stack Overflow - MSAL Tag](https://stackoverflow.com/questions/tagged/msal)
- [GitHub Issues](https://github.com/AzureAD/microsoft-authentication-library-for-android/issues)
- [Microsoft Q&A](https://docs.microsoft.com/en-us/answers/topics/azure-ad-msal.html)

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-20  
**Based on:** 181+ closed issues analysis  
**Maintained by:** MSAL Android Team

For questions about this guide, open an issue with the `documentation` label.
