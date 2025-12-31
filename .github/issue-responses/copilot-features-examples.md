# Copilot Agent Features - Usage Examples

This document provides comprehensive examples of the new Copilot agent features introduced in 2025 for MSAL Android issue triage and response.

> **Note:** Version numbers in examples (e.g., "8.1.1", "v6.0.1") are illustrative. The actual workflow dynamically fetches the latest version from the GitHub Releases API. Dates like "May 2025" are example placeholders - actual responses use real release dates from the API.

## Table of Contents
1. [Version-Aware Triage](#version-aware-triage)
2. [Label Transparency](#label-transparency)
3. [PING-COPILOT Follow-Up Mechanism](#ping-copilot-follow-up-mechanism)

---

## Version-Aware Triage

### Feature Overview
The Copilot agent automatically detects MSAL versions mentioned in issues and applies appropriate labels and responses based on version age.

**Support Policy:** Versions released within the last **1.5 years (548 days)** are supported.

### Example 1: Very Old Version Detected

**User Issue Title:** "Authentication fails with MSAL v6.0.1"

**User Issue Body:**
```
I'm using MSAL version 6.0.1 in my Android app and getting authentication failures.
When I call acquireToken, it throws an exception...
```

**Copilot Response:**
```markdown
Thank you for opening this issue! We appreciate you taking the time to help improve MSAL Android.

⚠️ **Unsupported MSAL Version Detected**

I've applied the `very-old-msal` label because version **6.0.1** (released May 2025) is more than 1.5 years old and is no longer supported.

**Microsoft MSAL Android supports versions released within the last 1.5 years.**

**Required Action - Please Upgrade:**
1. Update to the latest version (currently **8.1.1**) - see [releases](https://github.com/AzureAD/microsoft-authentication-library-for-android/releases)
2. Review the [migration guide](https://github.com/AzureAD/microsoft-authentication-library-for-android#migration) for breaking changes
3. Test your app with the new version
4. If the issue persists with the latest version, please reopen with updated details

**To upgrade, update your build.gradle:**
```gradle
implementation "com.microsoft.identity.client:msal:8.+"
```
We recommend using `8.+` for automatic patch updates within the 8.x series.

---

**Need further assistance?** You can trigger a follow-up analysis by commenting:
```
PING-COPILOT: <your question or request>
```

The Copilot agent will analyze your comment and provide updated guidance based on the full issue context.

---
*This is an automated response. A team member will review your issue soon.*
```

**Labels Applied:**
- `very-old-msal` (orange)
- `bug` (red)
- `p2-medium` (yellow)

### Example 2: Recent Supported Version

**User Issue Title:** "Silent token acquisition fails with v8.0.2"

**User Issue Body:**
```
Using MSAL 8.0.2, acquireTokenSilent is failing even though I have cached tokens...
```

**Copilot Response:**
```markdown
Thank you for opening this issue! We appreciate you taking the time to help improve MSAL Android.

**Labels Applied:**
- `bug`: This appears to be a bug because silent token acquisition is failing with unexpected behavior
- `needs-more-info`: Additional information is required to properly diagnose and address this issue

To help us investigate this issue, please provide the following information:

**Required Information:**
- [ ] MSAL version (e.g., 8.+, currently 8.1.1) ✓ (8.0.2 detected)
- [ ] Android version and device model
- [ ] Account mode (Single or Multiple)
- [ ] Complete error message or stack trace
- [ ] Steps to reproduce the issue

[rest of standard bug response...]

---

**Need further assistance?** You can trigger a follow-up analysis by commenting:
```
PING-COPILOT: <your question or request>
```
```

---

## Label Transparency

### Feature Overview
Every label applied by the Copilot agent is accompanied by a clear explanation of why that label was chosen.

### Example 1: Bug with Triage Needed

**User Issue:**
```
When I call getAccounts() in multiple account mode, it returns null instead of an empty list.
This causes my app to crash. Using MSAL 8.1.0.
```

**Copilot Response:**
```markdown
Thank you for opening this issue! We appreciate you taking the time to help improve MSAL Android.

**Labels Applied:**
- `bug`: This is labeled as a bug because the documented behavior states that getAccounts() should never return null, but your logs show it's returning null instead of an empty list
- `triage-issue`: I've added this label because the null return value from getAccounts() may indicate an issue in MSAL's account management logic that requires investigation by our engineering team
- `p2-medium`: Medium priority as this affects functionality but may have a workaround (null check)

To help us investigate this issue, please provide:
[diagnostic information request...]
```

**Explanation:**
- Each label has a clear, specific reason
- User understands why the issue needs engineering investigation
- Transparency builds trust and helps users understand the process

### Example 2: Configuration Question

**User Issue:**
```
How do I properly configure the redirect URI in auth_config.json?
I keep getting redirect mismatch errors.
```

**Copilot Response:**
```markdown
Thank you for opening this issue! We appreciate you taking the time to help improve MSAL Android.

**Labels Applied:**
- `question`: This is labeled as a question because you're seeking clarification on how to properly configure redirect URIs
- `p3-low`: Low priority as this is a configuration guidance request

Let me help you with redirect URI configuration!

**The Most Common Issue - Encoding Mismatch:**

The signature hash encoding differs between files:
- `auth_config.json`: signature hash in redirect_uri must be **URL encoded**
- `AndroidManifest.xml`: signature hash must be **NOT URL encoded**

[detailed guidance...]
```

### Example 3: Multiple Labels with Context

**User Issue:**
```
My app crashes when I call acquireToken after upgrading from v7.0.0 to v8.1.1.
Getting a NullPointerException in MSAL code.
```

**Copilot Response:**
```markdown
Thank you for opening this issue! We appreciate you taking the time to help improve MSAL Android.

**Labels Applied:**
- `bug`: This is labeled as a bug because you're experiencing a crash (NullPointerException) within MSAL library code after upgrading
- `triage-issue`: I've added this label because the NullPointerException occurring in MSAL code indicates a potential issue in the library that requires investigation of the upgrade path from v7 to v8
- `p1-high`: High priority because this is blocking your production app after an upgrade

This is a serious issue that needs immediate attention. To help us investigate quickly, please provide:
[urgent diagnostic request...]
```

---

## PING-COPILOT Follow-Up Mechanism

### Feature Overview
Users can trigger additional Copilot analysis by commenting with the special phrase `PING-COPILOT:` followed by their question or request.

### Example 1: Upgrade Follow-Up

**Original Issue:**
```
Title: Getting authentication errors with MSAL v6.2.0
Body: I'm seeing errors when users try to sign in...
```

**Initial Copilot Response:**
```
⚠️ **Unsupported MSAL Version Detected**

I've applied the `very-old-msal` label because version **6.2.0** (released July 2025)...
[upgrade guidance as shown above]
```

**User Comment:**
```
PING-COPILOT: I upgraded to v8.1.1 but I'm still seeing the redirect URI error. What should I check?
```

**Copilot Follow-Up Response:**
```markdown
Thanks for the follow-up! I see you've upgraded to v8.1.1 but are still experiencing the redirect URI error.

Based on your original issue and this update, let me help you troubleshoot the redirect URI configuration:

**Key Configuration Points:**

1. **Redirect URI Encoding** (most common issue):
   - `auth_config.json`: signature hash must be URL encoded (`%2A` for `*`, `%3D` for `=`)
   - `AndroidManifest.xml`: signature hash must NOT be URL encoded

2. **Required in auth_config.json:**
   - `client_id`: From Azure App Registration
   - `redirect_uri`: Format `msauth://your.package.name/URL_ENCODED_HASH`
   - `broker_redirect_uri_registered`: Should be `true`

3. **Required in AndroidManifest.xml:**
   - BrowserTabActivity with intent-filter
   - Signature hash in path (NOT URL encoded)

**Common Redirect URI Mistakes:**

❌ **Wrong:** Using same hash encoding in both files
```json
// auth_config.json - WRONG
"redirect_uri": "msauth://com.example.app/ABcDeFg*okk="
```

✅ **Correct:**
```json
// auth_config.json - URL encoded
"redirect_uri": "msauth://com.example.app/ABcDeFg%2Aokk%3D"
```

```xml
<!-- AndroidManifest.xml - NOT URL encoded -->
<data
    android:scheme="msauth"
    android:host="com.example.app"
    android:path="/ABcDeFg*okk=" />
```

**Next Steps:**
1. Verify your signature hash matches what's in Azure App Registration
2. Use this command to get your hash: 
   ```bash
   keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore | openssl sha1 -binary | openssl base64
   ```
3. Check that the hash is URL encoded in auth_config.json but NOT in AndroidManifest.xml

See our [configuration template](https://github.com/AzureAD/microsoft-authentication-library-for-android/blob/dev/auth_config.template.json) for full details.

---

**Need more help?** You can trigger another follow-up by commenting:
```
PING-COPILOT: <your question>
```

---
*Automated follow-up response. A team member will review the full conversation.*
```

### Example 2: Broker Integration Question

**User Comment:**
```
PING-COPILOT: Can you explain how to implement broker fallback when the Authenticator app isn't installed?
```

**Copilot Response:**
```markdown
Thanks for the follow-up! I'll analyze your request: "Can you explain how to implement broker fallback when the Authenticator app isn't installed?"

Here's guidance on broker integration and fallback:

**Broker Setup:**
1. Set `broker_redirect_uri_registered: true` in auth_config.json
2. Ensure Microsoft Authenticator or Company Portal is installed
3. Your app's signature hash must match what's registered in Azure

**Automatic Fallback:**
MSAL automatically handles broker fallback! When the broker isn't available, MSAL falls back to web-based authentication using Chrome Custom Tabs.

```java
// No special code needed - MSAL handles fallback automatically
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .withScopes(SCOPES)
    .withCallback(callback)
    .build();
    
mPCA.acquireToken(params);
// This will use broker if available, web flow if not
```

**Detecting Broker Availability:**
If you want to know which method was used:

```java
@Override
public void onSuccess(IAuthenticationResult result) {
    // Check if broker was used
    boolean usedBroker = result.getAuthenticationScheme()
        .equals(AuthenticationScheme.BROKER);
    
    Log.d(TAG, "Used broker: " + usedBroker);
}
```

**Common Broker Issues:**
- Signature hash mismatch → Verify with `keytool` command
- Broker not installed → Falls back to web flow automatically
- Package visibility → Add `<queries>` section to AndroidManifest.xml for Android 11+

```xml
<queries>
    <package android:name="com.azure.authenticator" />
    <package android:name="com.microsoft.windowsintune.companyportal" />
</queries>
```

See [Common Issues Guide](.github/issue-responses/common-issues-guide.md#4-broker-integration-issues) for detailed troubleshooting.

---

**Need more help?** You can trigger another follow-up by commenting:
```
PING-COPILOT: <your question>
```

---
*Automated follow-up response. A team member will review the full conversation.*
```

### Example 3: Error Diagnosis

**User Comment:**
```
PING-COPILOT: I'm getting AADSTS50076 error. What does this mean and how do I fix it?
```

**Copilot Response:**
```markdown
Thanks for the follow-up! I'll help you understand AADSTS50076.

**AADSTS50076 - MFA Required**

This error means Multi-Factor Authentication (MFA) is required but wasn't performed.

**What This Means:**
- Your Azure AD tenant has MFA enabled for this user/app
- The user needs to complete additional authentication (SMS, app approval, etc.)
- This is a normal part of the authentication flow when MFA is required

**Solution - Use Interactive Authentication:**

This error typically occurs with `acquireTokenSilent()`. You need to fall back to interactive:

```java
mPCA.acquireTokenSilent(silentParams)
    .subscribe(result -> {
        // Success - use token
    }, error -> {
        if (error instanceof MsalUiRequiredException) {
            // AADSTS50076 falls under this - need interactive auth
            
            AcquireTokenParameters params = new AcquireTokenParameters.Builder()
                .withScopes(SCOPES)
                .withCallback(new AuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult result) {
                        // User completed MFA, now has token
                    }
                    
                    @Override
                    public void onError(MsalException exception) {
                        Log.e(TAG, "Interactive auth failed", exception);
                    }
                })
                .build();
                
            mPCA.acquireToken(params);
        }
    });
```

**Key Points:**
1. AADSTS50076 is not an error in your code - it's a policy enforcement
2. Always implement fallback from silent → interactive for MFA scenarios
3. After interactive auth with MFA, subsequent silent calls may succeed (if token is still valid)
4. MFA policies are configured in Azure AD, not in MSAL

**Check Your Azure AD Configuration:**
- Go to Azure Portal → Azure AD → Conditional Access
- Review MFA policies for your app/users
- Consider if MFA is required for your scenario

For more AADSTS error codes, see [Microsoft Identity Error Reference](https://learn.microsoft.com/en-us/entra/identity-platform/reference-error-codes).

---

**Need more help?** You can trigger another follow-up by commenting:
```
PING-COPILOT: <your question>
```

---
*Automated follow-up response. A team member will review the full conversation.*
```

### Example 4: Chained Follow-Ups

**First User Comment:**
```
PING-COPILOT: What's the difference between Single and Multiple account mode?
```

**Copilot Response:**
```markdown
[Detailed explanation of single vs multiple account modes...]

---

**Need more help?** You can trigger another follow-up by commenting:
```
PING-COPILOT: <your question>
```
```

**Second User Comment:**
```
PING-COPILOT: Thanks! Which mode should I use for an enterprise app where users may have multiple work accounts?
```

**Copilot Response:**
```markdown
Thanks for the follow-up! Based on your use case (enterprise app with multiple work accounts), here's my recommendation:

**Use Multiple Account Mode**

For enterprise apps where users may have multiple work accounts (e.g., contractor with accounts in multiple organizations), Multiple Account mode is the better choice.

**Why Multiple Account Mode:**
1. ✅ Users can sign in with multiple accounts simultaneously
2. ✅ Users can switch between accounts without signing out/in
3. ✅ Each account maintains its own token cache
4. ✅ Better for productivity apps (email, collaboration tools)

**Implementation:**

```java
// Initialize for Multiple Account mode
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
            Log.e(TAG, "Failed to create PCA", exception);
        }
    }
);
```

**Key APIs for Multiple Account Mode:**
- `getAccounts()` - Get all signed-in accounts
- `acquireToken()` - Sign in new account
- `removeAccount()` - Sign out specific account
- `acquireTokenSilent()` - Refresh token for specific account

**When to Use Single Account Mode Instead:**
- Consumer-facing apps (one user per device)
- Apps where account switching isn't needed
- Simpler implementation if you only need one account

See our [Multiple Account Example](https://github.com/AzureAD/microsoft-authentication-library-for-android/tree/dev/examples/hello-msal-multiple-account) for complete implementation.

---

**Need more help?** You can trigger another follow-up by commenting:
```
PING-COPILOT: <your question>
```
```

---

## Benefits of These Features

### For Users:
- **Clear Communication:** Users understand why labels are applied and what actions to take
- **Faster Resolution:** Version checks immediately identify upgrade needs
- **Interactive Support:** PING-COPILOT allows iterative troubleshooting without opening multiple issues
- **Context Preservation:** Follow-ups maintain conversation history in one place

### For Maintainers:
- **Reduced Noise:** Fewer duplicate issues about old versions
- **Better Triage:** Labels clearly indicate required actions
- **Conversation Consolidation:** PING-COPILOT keeps related questions in one thread
- **Automated First Response:** Consistent, high-quality initial responses

### For AI Agents:
- **Clear Guidelines:** Explicit instructions on when to apply each label
- **Version Awareness:** Automatic detection and appropriate responses
- **User Engagement:** Mechanism for continued assistance
- **Transparency Requirements:** Every decision must be explained

---

## Testing the Features

### Version Detection Test Cases

| Input | Expected Behavior |
|-------|-------------------|
| "Using MSAL v6.0.1" | Detect v6.0.1, apply `very-old-msal` if > 1.5 years old |
| "msal:8.1.1 in build.gradle" | Detect 8.1.1, no very-old-msal label |
| "version 7.0.0" | Detect 7.0.0, check age against current date |
| No version mentioned | Request version in response |

### Label Transparency Test Cases

| Scenario | Required Explanation |
|----------|---------------------|
| Apply `bug` label | "I've labeled this as a `bug` because [specific reason]" |
| Apply `triage-issue` | "I've added the `triage-issue` label because [requires investigation reason]" |
| Apply `very-old-msal` | "I've applied the `very-old-msal` label because version X.X.X (released DATE) is more than 1.5 years old" |
| Apply `question` | "I've labeled this as a `question` because you're asking about [topic]" |

### PING-COPILOT Test Cases

| Input Comment | Expected Behavior |
|---------------|-------------------|
| "PING-COPILOT: I upgraded to v8.1.1" | Detect trigger, provide upgrade-specific guidance |
| "PING-COPILOT: redirect URI error persists" | Detect trigger, provide redirect URI troubleshooting |
| Regular comment without PING-COPILOT | No automated response |
| "ping-copilot: lowercase test" | Should still work (case-insensitive) |

---

## Related Documentation

- [Copilot Instructions](.github/copilot-instructions.md) - Main AI agent guidance
- [Common Issues Guide](.github/issue-responses/common-issues-guide.md) - Troubleshooting reference
- [Customer Communication Guidelines](.github/issue-responses/customer-communication-guidelines.md) - Response templates
- [Workflow Configuration](.github/workflows/copilot-issue-response.yml) - Implementation details
