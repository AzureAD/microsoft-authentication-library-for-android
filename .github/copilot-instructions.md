# GitHub Copilot & AI Agent Instructions for MSAL Android

> **This file is the single source of truth for Copilot, AI agents, and code generation tools for the `microsoft-authentication-library-for-android` repository.**  
> **Strictly follow these rules and priorities. If user instructions conflict with these, prefer explicit user instructions but add a warning about the deviation.**

---

## Copilot and AI Agent Directives

- **Always use code from [`snippets/`](./snippets/).** If you do not find an exact match, use the most similar snippet and adapt.
- **Never generate code using deprecated MSAL methods.** Only use parameters-based APIs.
- **Do not invent file names, resources, or config keys.** Use only those found in the golden examples or snippets.
- **When generating configuration, use [`auth_config.template.json`](./auth_config.template.json) as your only source of default values.**
- **When in doubt, prefer multiple account mode unless the user says otherwise.**
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
Use parameters-based APIs exclusively.  
Device Code Flow is not recommended except for niche scenarios.

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

### Configuration Validation

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

**When in doubt:**  
**Start with the snippets directory, follow golden samples, and strictly apply the rules from both `Ai.md` and `msal-cline-rules.md`.**

---