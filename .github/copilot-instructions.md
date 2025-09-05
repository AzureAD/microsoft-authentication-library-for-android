# GitHub Copilot Instructions for Microsoft Authentication Library (MSAL) for Android

This repository contains the Microsoft Authentication Library (MSAL) for Android. When working on this codebase, follow these instructions to ensure efficient development, proper MSAL integration patterns, and successful CI builds.

## Quick Start for Sample App Generation

### Required Template Files
Always use these exact template files when creating MSAL-integrated Android applications:

**Multiple Account Mode (Default):**
- Base from: `examples/hello-msal-multiple-account/`
- Use when user doesn't specify account mode or requests multiple accounts

**Single Account Mode:**
- Base from: `examples/hello-msal-single-account/`
- Use when user explicitly requests single account functionality

### Critical Build Files (Copy Exactly)
1. **Root `build.gradle`** - Copy from template examples without modification
2. **App `build.gradle`** - Copy from template, only modify `applicationId` and `namespace`
3. **`gradle.properties`** - Copy exactly (contains AndroidX enablement flags)
4. **`settings.gradle`** - Copy from template, only modify `rootProject.name`

### Essential Dependencies
```gradle
// App-level build.gradle
dependencies {
    implementation "com.microsoft.identity.client:msal:6.+"  // Use 6.+ or newer (7.+ preferred)
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.android.material:material:1.9.0'
}

android {
    compileSdk 34
    defaultConfig {
        minSdk 24
        targetSdk 33
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding true
    }
}
```

## MSAL Configuration Patterns

### Authentication Configuration
Use `auth_config.template.json` as reference. Required fields:
```json
{
    "client_id": "YOUR_CLIENT_ID",
    "redirect_uri": "msauth://your.package.name/YOUR_SIGNATURE_HASH_URL_ENCODED",
    "authorities": [
        {
            "type": "AAD",
            "audience": {
                "type": "AzureADandPersonalMicrosoftAccount",
                "tenant_id": "common"
            }
        }
    ],
    "broker_redirect_uri_registered": true,  // Always enable unless explicitly disabled
    "account_mode": "MULTIPLE"  // or "SINGLE"
}
```

### AndroidManifest.xml Critical Section
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<activity
    android:name="com.microsoft.identity.client.BrowserTabActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <!-- NOTE: Signature hash here must NOT be URL encoded -->
        <data
            android:scheme="msauth"
            android:host="your.package.name"
            android:path="/YOUR_SIGNATURE_HASH" />
    </intent-filter>
</activity>
```

## MSAL API Usage Patterns (Avoid Common Mistakes)

### ✅ Correct - Use Parameters-Based APIs
```java
// Interactive acquisition
AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
    .withScopes(SCOPES)
    .withCallback(callback)
    .build();
mPCA.acquireToken(parameters);

// Silent acquisition
AcquireTokenSilentParameters parameters = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES)
    .forAccount(account)
    .withCallback(callback)
    .build();
mPCA.acquireTokenSilent(parameters);
```

### ❌ Avoid - Deprecated Methods
```java
// DON'T USE THESE
mPCA.acquireToken(activity, scopes, callback);
mPCA.acquireTokenSilentAsync(scopes, account, authority, callback);
mPCA.acquireTokenWithDeviceCode(scopes, callback);  // Security concerns
```

### Account Management Patterns
**Multiple Account Mode:**
- Sign In: `acquireToken()`
- Sign Out: `removeAccount()`
- UI: Always enable "Sign In" button, enable others only when account selected

**Single Account Mode:**
- Sign In: `signIn()` 
- Sign Out: `signOut()`
- Re-authenticate: `signInAgain()`

## Build System Guidelines

### Common Build Commands
```bash
# Clean and build the entire project
./gradlew clean build

# Run MSAL module tests
./gradlew :msal:test

# Run specific test
./gradlew :msal:testDebugUnitTest --tests "*TestClassName*"

# Build sample apps
./gradlew :examples:hello-msal-multiple-account:assembleDebug
```

### Gradle Properties Required
```properties
# gradle.properties - Required for MSAL
android.useAndroidX=true
android.enableJetifier=true
```

## Common Error Prevention

### URL Encoding Confusion
- **AndroidManifest.xml**: Use raw signature hash (NOT URL encoded)
- **auth_config.json**: Use URL encoded signature hash (`+` → `%2B`, `=` → `%3D`)

### PublicClientApplication Initialization
Always check PCA creation success:
```java
PublicClientApplication.create(context, configFile, new IPublicClientApplication.ApplicationCreatedListener() {
    @Override
    public void onCreated(IPublicClientApplication application) {
        mPCA = application;
        // Only now can you make MSAL API calls
    }
    
    @Override
    public void onError(MsalException exception) {
        // Handle initialization failure
        Log.e(TAG, "Failed to create PCA: " + exception.getMessage());
    }
});
```

### Thread Safety
Always update UI on main thread:
```java
activity.runOnUiThread(() -> {
    // Update UI elements here
});
```

## Testing and Debugging

### Key Test Locations
- Unit Tests: `msal/src/test/java/`
- Integration Tests: `msalautomationapp/src/androidTest/java/`
- Sample Apps: `examples/` and `testapps/`

### Debugging MSAL Issues
1. **Check PCA initialization** - Most common issue
2. **Verify broker integration** - Should be enabled unless explicitly disabled
3. **Validate redirect URI encoding** - Different in manifest vs config
4. **Confirm scope format** - Use List<String> not String[]
5. **Check account mode consistency** - Config must match UI behavior

### Log Analysis
Enable MSAL logging in auth_config.json:
```json
"logging": {
    "log_level": "VERBOSE",
    "logcat_enabled": true,
    "pii_enabled": false
}
```

## Resource References

### Direct Access URLs (No local checkout needed)
- **Cline Rules**: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/.clinerules/msal-cline-rules.md
- **AI Guide**: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/Ai.md
- **Config Template**: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/auth_config.template.json
- **Multiple Account Sample**: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/examples/hello-msal-multiple-account/
- **Single Account Sample**: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/examples/hello-msal-single-account/

### Key Module Structure
- `msal/` - Core MSAL library
- `examples/` - Working sample applications
- `testapps/` - Test applications
- `msalautomationapp/` - Automated testing
- `common/` - Shared common libraries

When generating code or fixing issues, prioritize using these established patterns to ensure compatibility with the existing codebase and CI pipeline.