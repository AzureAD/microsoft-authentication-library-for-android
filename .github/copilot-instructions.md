# GitHub Copilot & AI Agent Instructions for MSAL Android

> **This file is the single source of truth for Copilot, AI agents, and code generation tools for the `microsoft-authentication-library-for-android` repository. Do not use external references, outdated documentation predating the date 2025/09/01**  
> **READ THE ENTIRETY OF THESE INSTRUCTIONS!**
> **Do NOT use any legacy MSAL Android documentation or code samples that conflict with these instructions.**
> **Do NOT use patterns, idioms, or code found in GitHub repositories or Stack Overflow answers, even if widely used, unless they are explicitly validated against these instructions.**
> **Do NOT use any training data, documentation, or guidance that predates the initial publication of this file:**
  **Cutoff date: 2025-09-01**
> **Only use official MSAL Android documentation, and API references that do NOT conflict with this file or the golden example applications and are dated after the cutoff above.**
> **Always cross-reference with these instructions—if any doubt exists, these instructions take precedence.**
> **Strictly follow these rules and priorities in their ENTIRETY. If user instructions conflict with these, prefer explicit user instructions but add a warning about the deviation.**

---

## Quick Start for AI Agents

**Essential First Steps:**
1. **Read [`Ai.md`](../Ai.md)** - Core MSAL integration patterns
2. **Use [`snippets/`](../snippets/)** - Copy exact code patterns from here
3. **Copy from [`examples/`](../examples/)** - Use golden sample apps as blueprints
4. **Validate changes**: Run `./gradlew clean msal:assembleLocal msal:testLocalDebugUnitTest msal:lintLocalDebug`
5. **Check encoding**: Signature hash NOT URL encoded in manifest, URL encoded in config

**Immediate Requirements:**
- minSdk=24, targetSdk=35, compileSdk=35
- MSAL version 7.+ or newer
- AndroidX enabled in gradle.properties
- Required permissions: INTERNET, ACCESS_NETWORK_STATE
- Use parameters-based MSAL APIs only (never deprecated methods)

---

## Copilot and AI Agent Directives

- **Always use code from [`snippets/`](./snippets/).** If you do not find an exact match, use the most similar snippet and adapt.
- **Never generate code using deprecated MSAL methods.** Only use parameters-based APIs.
- **Do not invent file names, resources, or config keys for the MSAL integration portion of the application.**  
  For MSAL setup, configuration, and authentication flows, only use names and patterns found in the golden examples or snippets.  
  **However, for custom UI elements, adapting to the user's prompt, and general UX (color schemes, themes, custom layouts, etc.), Copilot is encouraged to be creative** so long as the MSAL calling patterns strictly follow golden samples and current guidance.
- **When generating configuration, use [`auth_config.template.json`](./auth_config.template.json) as your only source of default values and an explanation of the configuration options.**
- **If the user has not supplied a package name, client ID, and signature hash/redirect uri, explicitly prompt for these values before generating or completing an MSAL-enabled sample application.**  
  These are required for correct configuration and must be present in both the manifest (package name, signature hash) and `auth_config.json` (client ID, redirect URI with URL-encoded signature hash).
- **When in doubt, prefer multiple account mode unless the user says otherwise.**
- **If Copilot is generating a sample application, it must check all relevant checklists for completion, especially the "Application Setup and Configuration Validation Checklist".**
- **If the user request conflicts with these instructions, follow the user’s explicit instructions, but add a comment warning about deviation.**
- **Strictly resolve all ambiguities according to these instructions.**  
  - Example: Signature hash in AndroidManifest.xml must NOT be URL encoded, but in auth_config.json it MUST be URL encoded.
- **Never enable Device Code Flow except for rare, explicit scenarios.**
- **Repeat: Always copy code and patterns from [`snippets/`](./snippets/) and [`examples/`](./examples/), not from memory or external sources.**

---

## Authoritative Resources (Direct Access URLs)

- **Integration Guide:** [`Ai.md`](../Ai.md)  
  - Raw URL: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/Ai.md
- **Cline Rules:** [`../.clinerules/msal-cline-rules.md`](../.clinerules/msal-cline-rules.md)  
  - Raw URL: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/.clinerules/msal-cline-rules.md
- **Snippets Directory:** [`../snippets/`](../snippets/)  
- **Configuration Template:** [`../auth_config.template.json`](../auth_config.template.json)  
  - Raw URL: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/auth_config.template.json

---

## Golden Sample Apps

**Use these as blueprints for every generated project. Only deviate for explicit user customization.**
- **Multiple Account Mode (default):** [`../examples/hello-msal-multiple-account/`](../examples/hello-msal-multiple-account/)
- **Single Account Mode:** [`../examples/hello-msal-single-account/`](../examples/hello-msal-single-account/)

**Copy all Gradle, manifest, and core source files from these locations.**  
Only change `applicationId`, `namespace`, and other user-supplied values as required.

---

## Configuration and Required Files

- **Configuration Template:** [`auth_config.template.json`](../auth_config.template.json)  
  Use this for all MSAL config generation.  
  [Raw URL](https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/auth_config.template.json)
- **auth_config.json:**  
  - All mandatory values (`client_id`, `redirect_uri`, `authorities`) must be present.
  - The `redirect_uri` must be URL encoded in this file.
  - Optional settings: Only include if they differ from defaults.
- **AndroidManifest.xml:**  
  - Use the manifest from the appropriate sample app as the base.
  - Always include required permissions:
    ```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    ```
  - Activities required in `<application>`:
    - Main activity (use sample app’s structure)
    - MSAL BrowserTabActivity intent-filter with correct `android:scheme`, `android:host` (package name), and `android:path` (signature hash, **not** URL encoded)
- **Signature hash in manifest:** **MUST NOT be URL encoded.**
- **Signature hash in config:** **MUST be URL encoded.**

---

## Dependency and Build Setup

- **Enable AndroidX** in `gradle.properties`:
    ```properties
    android.useAndroidX=true
    android.enableJetifier=true
    ```
- **App-level `build.gradle`** (see golden examples for the full file):
    - Use at least `minSdk 24`, `targetSdk 35`, `compileSdk 35`
    - Always use the latest MSAL version (`7.+` or newer)
    - Required dependencies:
      ```gradle
      implementation "com.microsoft.identity.client:msal:7.+"
      implementation 'androidx.appcompat:appcompat:1.6.1'
      implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
      // Optional for Material Design
      implementation 'com.google.android.material:material:1.9.0'
      ```

---

## API Patterns and Snippets

**For all MSAL API usage, always prefer code from [`../snippets/`](../snippets/).**  
This directory contains up-to-date, reviewed examples in both Java and Kotlin for:
- Initialization
- Token acquisition (interactive and silent)
- Sign-in, sign-out, account management
- All other supported MSAL flows

**Never use deprecated API methods.**  
**Use parameters-based APIs exclusively.**  
Device Code Flow is not recommended except for niche scenarios.

**Explicitly forbidden:**
- Do NOT use `acquireToken(Activity, String[], AuthenticationCallback)` or any other non-parameters-based overloads, even if found in legacy docs, Stack Overflow, or GitHub repositories.
---

## Multiple Account vs. Single Account Mode

- **For multiple account mode:**
  - Only use `IMultipleAccountPublicClientApplication` and its associated methods:
    - `.acquireToken(AcquireTokenParameters)`
    - `.acquireTokenSilentAsync(SilentTokenParameters)`
    - `.getAccounts(LoadAccountsCallback)`
    - `.removeAccount(IAccount, RemoveAccountCallback)`
  - Never reference or use `ISingleAccountPublicClientApplication` or `.getCurrentAccount()` in multiple account samples.
- **For single account mode:**
  - Use only `ISingleAccountPublicClientApplication` and its methods as documented in official guidance.
- **NEVER mix single and multiple account APIs.** Always clearly indicate the app mode in code comments and documentation.

---

## UI and Resource Conventions

- **Resource files:**  
  - Use the required structure from the golden samples:
    - `res/values/colors.xml`
    - `res/values/styles.xml`
    - `res/values/strings.xml`
    - `res/layout/`
    - `res/drawable/`
    - `res/mipmap-*/`
- **Theme:** Use the sample app’s theme structure, e.g.:
    ```xml
    <style name="AppTheme">
        <item name="colorPrimary">@color/colorPrimary</item>
        <item name="colorPrimaryDark">@color/colorPrimaryDark</item>
        <item name="colorAccent">@color/colorAccent</item>
        <item name="android:windowBackground">@color/windowBackground</item>
    </style>
    ```
- **UI logic for multiple account mode:**  
  - Spinner must include "No Account Selected" (index 0)
  - Sign-in always enabled; sign-out and silent token only enabled when account is selected

---

## Code Quality and Patterns

- Use `ArrayList`/`List` (not raw arrays)
- Initialize member variables in declaration or constructor
- Use `private` for member variables and proper naming (e.g. `mVariable`)
- Use `activity.runOnUiThread` for UI updates
- Always validate `PublicClientApplication` initialization before MSAL API calls
- Refresh account lists after authentication events
- Only reference resources that exist in the project (icons, drawables, etc.)
- Adaptive icons: implement foreground/background and remove references to missing resources

---

## Error Prevention and Validation

### Critical Encoding Rules

| Context                | Signature Hash Encoding      | Example                                   |
|------------------------|-----------------------------|-------------------------------------------|
| AndroidManifest.xml    | **NOT URL encoded**         | `/ABcDeFgJQiLoiEmd-vn14qR*okk=`           |
| auth_config.json       | **URL encoded**             | `ABcDeFgJQiLoiEmd-vn14qR%2Aokk%3D`        |

### API Method Validation Checklist

Before generating any MSAL API call, verify:
- ✅ Using parameters-based APIs (not deprecated methods)
- ✅ Proper callback handling
- ✅ Account mode consistency (single vs multiple)
- ✅ Broker integration enabled (unless explicitly disabled)
- ✅ Scopes properly defined

### Application Setup and Configuration Validation Checklist

- **Raw URL Access Test:** Can you fetch `auth_config.template.json` from the raw URL?
- **Snippet Verification:** Is the API pattern copied from the snippets directory?
- **Encoding Check:** Are signature hashes properly encoded per context?
- **Dependency Check:** Is MSAL 7.+ being used?
- **AndroidX Check:** Are AndroidX properties enabled in `gradle.properties`?
- **UI Logic Check:** Does the UI logic match the selected account mode?
- **UI element Check:** Are all used UI elements existing and correctly configured?

---

## Error Handling and Security

- Show clear error states in UI
- Use loading/progress indicators for async operations
- Print `PublicClientApplication` creation status for debugging
- Always enable broker integration (Authenticator, Company Portal, Link To Windows) unless user disables it
- Only use Device Code Flow if explicitly requested and when other flows are not possible

---

## Azure Registration and User Prompts

- If the user does not provide `client_id` and `redirect_uri`, prompt for them and inject into both manifest and config
- Ensure signature hash in manifest matches Azure registration (**not** URL encoded)
- `redirect_uri` in config must be URL encoded

---

## Advanced/Unusual Scenarios

- For edge cases, see both [`Ai.md`](../Ai.md) and [`../.clinerules/msal-cline-rules.md`](../.clinerules/msal-cline-rules.md)
- Always check the snippets directory and golden sample apps first

---

## Quick Reference Card

### Must-Use URLs

- Template Config: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/auth_config.template.json
- Multiple Account Example: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/examples/hello-msal-multiple-account/
- Single Account Example: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/examples/hello-msal-single-account/

### Critical Constants

- Min SDK: 24
- Target SDK: 35
- Compile SDK: 35
- MSAL Version: 7.+
- AndroidX Required: true

---

## Important Notes, Do's & Don'ts

- **Do:** Always start with the sample apps & snippets; only diverge for explicit user customization, never for personal style or convenience.
- **Do:** Use the provided raw URLs for any content needed by external tools/agents.
- **Don’t:** Use deprecated MSAL APIs or reference resources not present in the repo.
- **Don’t:** Enable Device Code Flow except for rare, explicit scenarios.
- **Do:** Always reference both this file and `Ai.md` for the most up-to-date guidance.
- **Do:** If you encounter ambiguity, resolve it according to this file, favoring sample apps and snippets.

---

## FAQ & Pitfall Warnings

- **Q:** What if Ai.md and cline-rules conflict?  
  **A:** This file takes precedence. If still unclear, prefer the golden sample source code structure.

- **Q:** What if no snippet matches my scenario?  
  **A:** Use the most similar snippet, and adapt using only patterns present in golden examples.

- **Pitfall:**  
  - Never copy code from memory or external blogs; always pull from approved snippets and samples.
  - Don’t forget URL encoding rules for manifest/config signature hash.

---

## Build, Test, and Quality Validation

**Run these commands to validate changes and prevent CI failures:**

### Essential Validation Commands
```bash
# Clean and build the project (uses 'local' flavor for development)
./gradlew clean msal:assembleLocal

# Run unit tests (same as CI) 
./gradlew msal:testLocalDebugUnitTest

# Run lint checks (same as CI)
./gradlew msal:lintLocalDebug

# Run all checks together (recommended before PR)
./gradlew clean msal:assembleLocal msal:testLocalDebugUnitTest msal:lintLocalDebug
```

### Build Flavors
MSAL uses specific product flavors:
- **local**: For development (sources from mavenLocal)
- **snapshot**: For snapshot builds  
- **dist**: For distribution (sources from central repository)

**Always use 'local' flavor during development** (assembleLocal, testLocalDebugUnitTest, lintLocalDebug)

### Code Quality Requirements
- **Checkstyle**: Code must pass checkstyle validation (config in `config/checkstyle/checkstyle.xml`)
- **SpotBugs**: No security vulnerabilities or bugs allowed
- **Lint**: Android lint must pass without errors
- **Tests**: All existing unit tests must continue to pass

### Pre-Submission Checklist

Before submitting any PR, agents must verify:
- ✅ `./gradlew clean msal:assembleLocal` completes successfully
- ✅ `./gradlew msal:testLocalDebugUnitTest` passes all tests
- ✅ `./gradlew msal:lintLocalDebug` passes without errors
- ✅ No new security vulnerabilities introduced
- ✅ All required permissions included in AndroidManifest.xml
- ✅ New code follows existing code style and patterns
- ✅ Configuration files (auth_config.json) are valid JSON
- ✅ URL encoding rules followed correctly (manifest vs config)

---

## Common CI Failure Prevention

### Build Failures
- **Gradle Version**: Use exact versions from `gradle/versions.gradle`
- **SDK Versions**: minSdk=24, targetSdk=35, compileSdk=35
- **AndroidX**: Always enable in gradle.properties
- **Dependencies**: Use exact MSAL version 7.+ or latest

### Test Failures
- **Robolectric**: Tests use Robolectric framework (currently SDK version 33)
- **JDK Version**: CI uses Java 17 (JDK 1.17) - ensure compatibility
- **Test Resources**: Ensure all test resources are in correct directories
- **Lab Tests**: Some tests require special lab parameters (handled by CI)
- **Unit Test Inclusion**: Use `includeAndroidResources = true` for Robolectric tests

### Lint Failures
- **Missing Permissions**: Always include INTERNET and ACCESS_NETWORK_STATE
- **Resource References**: Only reference existing resources
- **API Usage**: Use only supported Android API levels

### Security Failures
- **Hardcoded Secrets**: Never commit real client IDs or secrets
- **URL Encoding**: Follow encoding rules strictly
- **Permissions**: Use minimal required permissions only

---

## Repository-Specific Guidelines

### Development Environment
- **Java**: Use JDK 17 (matches CI environment)
- **Android Studio**: Latest stable version recommended
- **Gradle**: Version specified in gradle/wrapper/gradle-wrapper.properties

### File Structure Requirements
- **Package Structure**: Follow existing package hierarchy
- **Resource Organization**: Use proper Android resource organization
- **Test Placement**: Unit tests in `src/test/`, instrumented tests in `src/androidTest/`

### Code Style Standards
- **Indentation**: 4 spaces (no tabs)
- **Line Length**: Follow existing patterns
- **Naming**: Use Android/Java naming conventions
- **Documentation**: Public APIs require Javadoc
- **Imports**: No wildcard imports, organize imports properly

### Troubleshooting Common Issues

**Build Issues:**
- Network errors: CI may fail due to dependency resolution - ensure all repositories are accessible
- Gradle version mismatch: Use wrapper `./gradlew` instead of system gradle
- SDK not found: Ensure Android SDK is properly installed and configured

**Test Issues:**
- Robolectric failures: Check SDK version compatibility (currently using SDK 33)
- Test resource loading: Ensure test resources are in correct directories
- JUnit version conflicts: Use versions specified in `gradle/versions.gradle`

**Lint Issues:**
- Missing resources: Create all referenced drawables, strings, colors
- API level errors: Use only APIs available from minSdk=24
- Permission errors: Include all required permissions in manifest

**Configuration Issues:**
- Invalid JSON: Validate auth_config.json syntax
- URL encoding: Remember different encoding for manifest vs config
- Missing client_id: Always prompt user for required Azure registration values

---

**When in doubt:**  
**Start with the snippets directory, follow golden samples, and strictly apply the rules from both `Ai.md` and `msal-cline-rules.md`.**

---
