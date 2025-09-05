# GitHub Copilot Instructions for MSAL Android

## Repository Overview
This is the Microsoft Authentication Library (MSAL) for Android - a complex multi-module Android library project. Before making any changes, understand the modular structure and existing AI guidance.

### Essential Reading
**ALWAYS review these files before starting any work:**
- [`Ai.md`](../Ai.md) - Comprehensive AI agent guide with direct resource links
- [`.clinerules/msal-cline-rules.md`](../.clinerules/msal-cline-rules.md) - Detailed implementation rules
- [`README.md`](../README.md) - Repository overview and AI development resources

**Reference Branch:** For additional sample app generation examples, see branch `fadi/copilot-1` which contains enhanced sample app generation instructions.

## Repository Structure & Navigation

### Key Modules
```
├── msal/                    # Core MSAL library source code
├── examples/                # Golden reference applications
│   ├── hello-msal-multiple-account/   # Multi-account mode example
│   └── hello-msal-single-account/     # Single-account mode example
├── testapps/               # Test applications
├── common/                 # Shared authentication library (submodule)
├── docs/                   # Documentation
└── snippets/               # Code snippet examples
```

### Critical Files for Understanding
- `auth_config.template.json` - Configuration template and defaults
- `examples/*/app/src/main/java/*/MainActivity.java` - Reference implementations
- `msal/src/main/java/com/microsoft/identity/client/` - Core API classes

## Build System Understanding

### Prerequisites Check
Before running builds, verify:
```bash
# Check Android SDK and tools are available
echo $ANDROID_HOME
which adb

# Verify network access to Google Maven and other repositories
curl -I https://dl.google.com/dl/android/maven2/
```

### Safe Build Commands
```bash
# Clean build (safest option)
./gradlew clean assembleDebug

# Run specific module tests
./gradlew :msal:testDebugUnitTest

# Build examples only
./gradlew :examples:hello-msal-multiple-account:assembleDebug
```

### Common Build Failures & Solutions

**Network/Dependency Issues:**
- Build failures with Google Maven - use `--offline` mode if dependencies are cached
- AGP version conflicts - check `build.gradle` for Android Gradle Plugin version compatibility

**Android SDK Issues:**
- Missing SDK components - install via Android Studio or sdkmanager
- Target SDK mismatches - verify compileSdk, targetSdk alignment across modules

**Memory Issues:**
- Large builds may require: `./gradlew build -Xmx4g`

## MSAL-Specific Debugging Patterns

### Common API Misuse Patterns
1. **Using Deprecated Methods:**
   ```java
   // ❌ WRONG - Deprecated
   mPCA.acquireToken(activity, scopes, callback);
   
   // ✅ CORRECT - Parameters-based API
   AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
       .withScopes(scopes)
       .withCallback(callback)
       .build();
   mPCA.acquireToken(parameters);
   ```

2. **Incorrect Account Mode Usage:**
   ```java
   // ❌ WRONG - signIn() in multiple account mode
   // ✅ CORRECT - Use acquireToken() for multiple account mode
   // ✅ CORRECT - Use signIn() only for single account mode
   ```

3. **URL Encoding Confusion:**
   ```xml
   <!-- AndroidManifest.xml - NOT URL encoded -->
   <data android:path="/YOUR_SIGNATURE_HASH" />
   ```
   ```json
   // auth_config.json - MUST be URL encoded
   { "redirect_uri": "msauth://package/YOUR_SIGNATURE_HASH%3D" }
   ```

### Debugging Authentication Issues
1. **Check PCA Initialization:**
   ```java
   // Always verify PCA was created successfully
   if (mPCA == null) {
       Log.e(TAG, "PublicClientApplication not initialized");
       return;
   }
   ```

2. **Enable MSAL Logging:**
   ```java
   Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
   Logger.getInstance().setExternalLogger(callback);
   ```

3. **Configuration Validation Checklist:**
   - Client ID format (GUID): `12345678-1234-1234-1234-123456789012`
   - Redirect URI encoding: AndroidManifest (raw) vs auth_config.json (URL-encoded)
   - Authority URL format: `https://login.microsoftonline.com/{tenant}`
   - Account mode alignment: Single vs Multiple account API usage
   - Broker settings: `broker_redirect_uri_registered` should be `true` unless explicitly disabled

### Testing Strategy
```bash
# Run unit tests for specific functionality
./gradlew :msal:testDebugUnitTest --tests="*AcquireToken*"

# Run automation tests (requires connected device/emulator)
./gradlew :msalautomationapp:connectedDebugAndroidTest

# Test example apps
./gradlew :examples:hello-msal-multiple-account:assembleDebug
```

## Code Quality & Standards

### Required Patterns
- Use `ArrayList<>` instead of arrays for better API compatibility
- Initialize member variables in declaration or constructor
- Follow Android naming conventions (`mVariable` for members)
- Handle UI updates on main thread: `activity.runOnUiThread()`

### Resource Management
- Always create required resource files (colors.xml, styles.xml, strings.xml)
- Use semantic color names (`colorPrimary`, not `blue`)
- Implement proper icon resources (adaptive icons with foreground/background)

### Error Handling
```java
// Always implement proper error callbacks
@Override
public void onError(MsalException exception) {
    Log.e(TAG, "Authentication failed", exception);
    // Check exception type: MsalClientException, MsalServiceException, etc.
}
```

## Integration Guidelines

### Working with Examples
- Use `examples/hello-msal-*` as golden references
- Copy gradle files exactly, only modify package names
- Maintain configuration structure from examples

### Configuration Best Practices
- Start with `auth_config.template.json`
- Include only non-default values in final config
- Verify broker settings match security requirements
- Test both multiple and single account modes if applicable

### Common Integration Pitfalls
- Forgetting AndroidX migration settings in gradle.properties
- Missing internet permissions in AndroidManifest.xml
- Incorrect BrowserTabActivity configuration
- Not handling async PCA creation properly

## Performance Considerations
- PCA creation is async - always check completion before API calls
- Use `acquireTokenSilent` before `acquireToken` for better UX
- Cache account information appropriately
- Implement proper loading states for auth operations

Remember: This repository has extensive existing documentation. Always consult `Ai.md` and `.clinerules/` for implementation-specific guidance before making changes.