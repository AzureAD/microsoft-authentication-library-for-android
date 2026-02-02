# GitHub Issue Triage Workflow - Test Scenarios

This document demonstrates how the improved workflow would handle the reported issues.

## Issue #2423: JsonSyntaxException in Token Cache

**Original Issue Content:**
- Title: "[React Native] MSAL throws JsonSyntaxException: Expected BEGIN_OBJECT but was STRING from Android layer"
- Body contains: "JsonSyntaxException", "Exception", "MSAL Android version: 2.2.+", "Stack Trace", "Expected Behavior"

### Old Behavior (Incorrect)
- ❌ Tagged as: `bug`, `p2-medium`
- ❌ Generic label explanation: "This appears to be a bug report based on the issue description indicating unexpected behavior or errors"
- ❌ Pattern detection in separate comment

### New Behavior (Correct)
**Classification:**
- ✅ Tagged as: `bug`, `p2-medium`
- ✅ Classification reasons: 
  - Strong bug indicators detected: exception, stacktrace
  - MSAL version detected: 2.2.+ (very old)

**Label Explanations (Verbose):**
```
**Labels Applied:**
- `bug`: Classified as a bug report because: Strong bug indicators detected: exception, stacktrace
- `p2-medium`: Priority level assigned - Standard bug priority, will be triaged by the team
- `very-old-msal`: Version 2.2.+ (released YYYY-MM-DD) is XXX days old, exceeding the 1.5 year support window (548 days)
```

**Integrated Pattern Detection:**
```
## 💡 Potential Solutions Detected

Based on your issue description, I've identified some patterns that might help:

### Token Cache Corruption

This appears to be a token cache corruption issue. MSAL should handle corrupted cache gracefully. Possible causes:
- App killed during token save operation
- Concurrent access to MSAL cache
- Storage corruption

**Recommended Actions:**
1. Implement proper error handling for cache operations
2. Fall back to interactive authentication when cache is corrupted
3. Consider clearing cache on this specific error

This may indicate a bug in MSAL's cache recovery logic that should be investigated.
```

**Result:** User gets comprehensive guidance in a single response with specific solutions for their JsonSyntaxException issue.

---

## Issue #2437: Edge Browser Not Recognized

**Original Issue Content:**
- Title: "Edge Not Used for Authentication Flow When Set as Default Browser (MSAL Falls Back to WebView)"
- Body contains: "Microsoft Edge", "not launch", "WebView", "signature hash not match", "fallback", "MSAL Version: 8.1.1"

### Old Behavior (Incorrect)
- ❌ Tagged as: `bug`, `p2-medium`
- ❌ Generic label explanation (same as every other bug)
- ❌ Only basic resources link, no specific guidance

### New Behavior (Correct)
**Classification:**
- ✅ Tagged as: `bug`, `p2-medium`
- ✅ Classification reasons:
  - Unexpected behavior indicators detected: fallback, not recognized
  - Medium bug indicators detected: mismatch

**Label Explanations (Verbose):**
```
**Labels Applied:**
- `bug`: Classified as a bug report because: Unexpected behavior indicators detected: fallback, not recognized; Medium bug indicators detected: mismatch
- `p2-medium`: Priority level assigned - Standard bug priority, will be triaged by the team
```

**Integrated Pattern Detection:**
```
## 💡 Potential Solutions Detected

Based on your issue description, I've identified some patterns that might help:

### Browser Signature Mismatch

MSAL maintains a list of trusted browser signatures for security. The error suggests the browser signature doesn't match MSAL's trusted list.

**Possible Causes:**
- Browser app updated with new signature
- MSAL's trusted signature list is outdated
- Debug vs. Release build signature differences

**Workarounds:**
1. Try using Chrome or Firefox as a workaround
2. Update to the latest MSAL version (may have updated signatures)
3. Update the browser app to the latest version

This may indicate MSAL needs to update its trusted browser signatures.

### Broker Integration

For broker-related issues, ensure:
- `broker_redirect_uri_registered: true` in auth_config.json
- Microsoft Authenticator or Company Portal is installed
- Your signature hash matches Azure App Registration

See our [Common Issues Guide](.github/issue-responses/common-issues-guide.md) for broker integration details.
```

**Result:** User gets targeted solutions for the browser signature issue with specific workarounds and investigation path.

---

## Non-Bug Example: Feature Request

**Issue Content:**
- Title: "Feature request: Add support for device code flow"
- Body: "Would be nice to have device code flow for devices without browsers"

### Classification:
- ✅ Tagged as: `feature-request`, `p3-low`
- ✅ Classification reasons: Feature request indicators found: feature request, would be nice

**Label Explanations:**
```
**Labels Applied:**
- `feature-request`: Classified as a feature request because: Feature request indicators found: feature request, would be nice
- `p3-low`: Priority level assigned - Lower priority, typically for feature requests or questions
```

**Result:** Correctly identified as feature request, not tagged as bug.

---

## Non-Bug Example: Question

**Issue Content:**
- Title: "How to configure MSAL for B2C?"
- Body: "I'm trying to set up B2C authentication but not sure about the configuration"

### Classification:
- ✅ Tagged as: `question`, `p3-low`
- ✅ No false positive bug classification

**Label Explanations:**
```
**Labels Applied:**
- `question`: Classified as a question - seeking clarification or guidance on MSAL usage
- `p3-low`: Priority level assigned - Lower priority, typically for feature requests or questions
```

**Result:** Not incorrectly tagged as a bug just because it mentions "configuration" or similar words.

---

## Summary of Improvements

### ✅ Fixed: Over-classification
- **Before:** Almost every issue tagged as "bug" due to broad keywords like "error", "issue", "fail"
- **After:** Precise detection using strong indicators (exception, crash, stacktrace) and contextual medium indicators (unexpected behavior, mismatch, fallback)

### ✅ Fixed: Verbose Explanations
- **Before:** Generic one-liner: "This appears to be a bug report based on the issue description indicating unexpected behavior or errors"
- **After:** Detailed reasoning: "Classified as a bug report because: Strong bug indicators detected: exception, stacktrace"

### ✅ Fixed: Pattern Solutions
- **Before:** Separate comment (pattern_detection job) or missing entirely
- **After:** Integrated into main response with "💡 Potential Solutions Detected" section including specific solutions for detected patterns

### ✅ New Patterns Added
- JSON cache corruption (for issue #2423)
- Browser signature mismatch (for issue #2437)
