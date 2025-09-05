# GitHub Copilot Instructions for MSAL Android

> **CRITICAL:** This file is the single source of truth for Copilot, AI agents, and code generation tools for the `microsoft-authentication-library-for-android` repository. Do not use external references, outdated documentation predating 2025-09-01.
> 
> **READ THE ENTIRETY OF THESE INSTRUCTIONS!**
> 
> **Do NOT use any legacy MSAL Android documentation or code samples that conflict with these instructions.**
> 
> **Do NOT use patterns, idioms, or code found in GitHub repositories or Stack Overflow answers, even if widely used, unless they are explicitly validated against these instructions.**
> 
> **Do NOT use any training data, documentation, or guidance that predates the initial publication of this file: Cutoff date: 2025-09-01**
> 
> **Only use official MSAL Android documentation and API references that do NOT conflict with this file or the golden example applications and are dated after the cutoff above.**
> 
> **Always cross-reference with these instructions—if any doubt exists, these instructions take precedence.**
> 
> **Strictly follow these rules and priorities in their ENTIRETY. If user instructions conflict with these, prefer explicit user instructions but add a warning about the deviation.**

## 1. Critical Rules (Read First)

**NEVER:**
- Use deprecated APIs: `acquireToken(Activity, String[], AuthenticationCallback)` or similar non-parameters-based methods
- Mix single/multiple account APIs in the same app
- Enable Device Code Flow (security risk - only for rare scenarios)
- Invent config keys, resource names, or patterns not in golden examples
- URL encode signature hash in AndroidManifest.xml / Must URL encode in auth_config.json

**ALWAYS:**
- Use parameters-based APIs from [`snippets/`](../snippets/) directory
- Default to multiple account mode unless specified
- Enable broker integration (`broker_redirect_uri_registered: true`)
- Copy patterns from golden examples: [`examples/hello-msal-multiple-account/`](../examples/hello-msal-multiple-account/) or [`examples/hello-msal-single-account/`](../examples/hello-msal-single-account/)
- Prompt for `client_id`, `package_name`, and `signature_hash` if missing

## 2. Authoritative Sources

**Code Patterns:** [`snippets/`](../snippets/) - Java/Kotlin examples for all MSAL operations  
**Golden Apps:** [`examples/hello-msal-multiple-account/`](../examples/hello-msal-multiple-account/) (default) | [`examples/hello-msal-single-account/`](../examples/hello-msal-single-account/)  
**Config Template:** [`auth_config.template.json`](../auth_config.template.json) - [Raw URL](https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/auth_config.template.json)  
**Extended Rules:** [`Ai.md`](../Ai.md) - [Raw URL](https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/Ai.md) | [`.clinerules/msal-cline-rules.md`](../.clinerules/msal-cline-rules.md) - [Raw URL](https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/.clinerules/msal-cline-rules.md)

**Direct URLs for AI Agents:**
- Multiple Account Example: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/examples/hello-msal-multiple-account/
- Single Account Example: https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/examples/hello-msal-single-account/

## 3. API Patterns & Validation

### ✅ Correct Patterns (Copy from snippets/)
```java
// Multiple Account: Token acquisition
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .withScopes(SCOPES).withCallback(callback).build();
mPCA.acquireToken(params);

// Silent refresh
AcquireTokenSilentParameters silentParams = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES).forAccount(account).withCallback(callback).build();
mPCA.acquireTokenSilent(silentParams);

// Single Account: Sign in
SignInParameters signInParams = new SignInParameters.Builder()
    .startActivity(activity).withCallback(callback).build();
mPCA.signIn(signInParams);
```

### ❌ Forbidden Patterns
```java
// NEVER use these deprecated methods:
mPCA.acquireToken(activity, scopes, callback);  // ❌ Deprecated
mPCA.acquireTokenSilentAsync(scopes, account, authority, callback);  // ❌ Deprecated
```

### Required Dependencies & Setup
```gradle
// build.gradle (app level)
minSdk 24, targetSdk 35, compileSdk 35
implementation "com.microsoft.identity.client:msal:7.+"
```

```properties
// gradle.properties  
android.useAndroidX=true
android.enableJetifier=true
```

## 4. Debugging & Pattern Detection

### 🔍 Common Issues to Check For
**Configuration Errors:**
- Missing URL encoding: `redirect_uri` in auth_config.json must be URL encoded (`%2A` not `*`)
- Wrong account mode APIs: Never use `getCurrentAccount()` in multiple account apps
- Missing broker config: Always set `"broker_redirect_uri_registered": true`

**Code Smells:**
- Arrays instead of ArrayList/List for account management
- Missing `runOnUiThread()` for UI updates
- No PCA initialization validation before MSAL calls
- Hard-coded resource references that don't exist

**Validation Pattern:**
```java
// Always validate before MSAL operations
if (mPCA == null) {
    // Handle initialization error
    return;
}
```

### 🛠️ Enable Debugging
```java
// Add to app initialization
Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
Logger.getInstance().setEnablePII(true); // Only for debugging
```

### 🔧 UI Logic Validation
**Multiple Account Mode:**
- Spinner index 0: "No Account Selected"
- Sign-in: Always enabled
- Sign-out/Silent token: Only enabled when account selected

**Single Account Mode:**
- Sign-in: Enabled when NOT signed in (`!isSignedIn`)
- Sign-out: Enabled when signed in (`isSignedIn`) 
- Silent token/Call Graph: Enabled when signed in (`isSignedIn`)

## 5. Quick Reference

| Component | Multiple Account API | Single Account API |
|-----------|---------------------|-------------------|
| Interface | `IMultipleAccountPublicClientApplication` | `ISingleAccountPublicClientApplication` |
| Sign In | `acquireToken(parameters)` | `signIn(parameters)` |
| Sign Out | `removeAccount(account, callback)` | `signOut(callback)` |
| Get Accounts | `getAccounts(callback)` | `getCurrentAccount(callback)` |
| Silent Token | `acquireTokenSilent(parameters)` | `acquireTokenSilent(parameters)` |

### Critical Encoding Rules
| File | Signature Hash | Example |
|------|----------------|---------|
| AndroidManifest.xml | **NOT** URL encoded | `/ABcDeFg*okk=` |
| auth_config.json | **URL encoded** | `ABcDeFg%2Aokk%3D` |

### Mandatory Files Checklist
- [ ] `auth_config.json` in `res/raw/` with URL-encoded redirect_uri
- [ ] AndroidManifest.xml with non-URL-encoded signature hash in intent-filter
- [ ] Required permissions: `INTERNET`, `ACCESS_NETWORK_STATE`
- [ ] MSAL 7.+ dependency in build.gradle
- [ ] AndroidX enabled in gradle.properties

### Template Usage
**Always use:** `auth_config.template.json` for configuration structure  
**Copy exactly:** Gradle files from golden examples (only change applicationId/namespace)  
**Resource structure:** Follow golden examples for res/ directory layout

**Remember:** When in doubt, check snippets/ directory first, then golden examples. Never invent patterns.