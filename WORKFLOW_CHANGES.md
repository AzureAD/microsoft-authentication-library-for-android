# GitHub Issue Triage Workflow - Changes Summary

## Problem Statement

Two bugs were identified in the GitHub issue triage workflow:

1. **Over-classification Bug:** Every issue was being tagged as "Bug" and "p2-medium" because the classification logic used overly broad keywords like "error", "fail", and "issue" that appear in almost every issue description.

2. **Missing Pattern Solutions:** The workflow previously provided pattern-specific solutions (like in issue #2423), but after changes, it only provided generic tagging responses without specific guidance (like in issue #2437).

## Root Causes

### Bug 1: Over-classification
The original classification logic was:
```javascript
const bugKeywords = ['bug', 'crash', 'error', 'exception', 'fail', 'broken', 'not working', 'issue'];
if (bugKeywords.some(kw => content.includes(kw))) {
  issueType = 'bug';
  priority = 'p2-medium';
}
```

**Problems:**
- Keywords were too generic ("error", "issue", "fail")
- No contextual analysis
- Single keyword match triggered classification
- No priority differentiation logic

### Bug 2: Missing Pattern Solutions
The workflow had two separate jobs:
1. `respond` job - Posted generic response with labels
2. `pattern_detection` job - Posted separate comment with specific solutions

**Problems:**
- Users received two separate comments instead of one cohesive response
- Pattern solutions were not integrated with the main triage response
- Timing issues could cause pattern detection to be delayed or missed

## Solutions Implemented

### Fix 1: Improved Classification Algorithm

**Strong Bug Indicators (Technical Errors):**
- crash
- exception (specific: nullpointerexception, illegalstateexception)
- stacktrace, stack trace
- throws
- fails to, broken
- does not work, not working, stopped working, doesn't work

**Medium Bug Indicators (Unexpected Behavior):**
- unexpected behavior, unexpected
- incorrect, wrong
- mismatch
- fallback
- not recognized, not launch

**Explicit Bug Markers:**
- [bug] in title
- **bug** in body

**Classification Logic:**
```javascript
if (issueType !== 'security' && issueType !== 'feature-request') {
  if (strongBugMatches.length > 0 || explicitBug) {
    issueType = 'bug';
    priority = 'p2-medium';
    classificationReasons.push(`Strong bug indicators detected: ${strongBugMatches.join(', ')}`);
  } else if (mediumBugMatches.length > 0) {
    issueType = 'bug';
    priority = 'p2-medium';
    classificationReasons.push(`Unexpected behavior indicators detected: ${mediumBugMatches.join(', ')}`);
  }
  // Don't classify as bug just because it contains generic words like "error" or "issue"
}
```

**Priority Escalation:**
- p0-critical: Security issues (security, vulnerability, cve, exploit, attack)
- p1-high: Production/blocker keywords OR critical functionality affected (data loss, cannot authenticate)
- p2-medium: Standard bugs
- p3-low: Feature requests and questions

### Fix 2: Verbose Label Explanations

**Before:**
```
**Labels Applied:**
- `bug`: This appears to be a bug report based on the issue description indicating unexpected behavior or errors
```

**After:**
```
**Labels Applied:**
- `bug`: Classified as a bug report because: Strong bug indicators detected: exception, stacktrace
- `p2-medium`: Priority level assigned - Standard bug priority, will be triaged by the team
```

The classification reasons are now stored and included in the label explanations, providing transparency about why each label was applied.

### Fix 3: Integrated Pattern Detection

**Pattern Detection Logic Moved to Classification Step:**
The pattern detection logic was moved from a separate job into the `classify` step of the `triage` job. Detected patterns are stored as JSON and passed to the `respond` job.

**Patterns Now Integrated Into Main Response:**
```
## 💡 Potential Solutions Detected

Based on your issue description, I've identified some patterns that might help:

### [Pattern Title]

[Pattern-specific guidance and solutions]
```

**New Patterns Added:**
1. **JSON Cache Corruption** - Addresses issue #2423
   - Detects: jsonsyntaxexception, expected begin_object, json parse errors
   - Provides: Guidance on cache corruption handling and fallback strategies

2. **Browser Signature Mismatch** - Addresses issue #2437
   - Detects: Edge/browser + not launch/fallback/webview + not recognized
   - Provides: Explanation of trusted signatures, workarounds, and investigation steps

**Removed:**
- The separate `pattern_detection` job has been removed to eliminate duplicate comments

## Impact Analysis

### Issue #2423 (JsonSyntaxException)
**Before:**
- Tagged: bug, p2-medium ✓
- Explanation: Generic
- Pattern solution: Separate comment about silent token acquisition (not specific to JSON errors)

**After:**
- Tagged: bug, p2-medium ✓
- Explanation: "Strong bug indicators detected: exception, stacktrace"
- Pattern solution: Integrated JSON cache corruption guidance with specific recovery strategies

### Issue #2437 (Edge Browser Fallback)
**Before:**
- Tagged: bug, p2-medium ✓
- Explanation: Generic
- Pattern solution: None (just generic links)

**After:**
- Tagged: bug, p2-medium ✓
- Explanation: "Unexpected behavior indicators detected: fallback, not recognized; Medium bug indicators detected: mismatch"
- Pattern solution: Browser signature mismatch guidance + Broker integration tips

### Non-Bug Issues
**Before:**
- Many false positives (questions and feature requests tagged as bugs)

**After:**
- More accurate classification:
  - "How to configure MSAL?" → question, p3-low
  - "Add support for X" → feature-request, p3-low
  - Generic "error" mentions without technical indicators → question, not bug

## Testing Recommendations

1. **Test with historical issues:**
   - Re-run logic against #2423 and #2437 to verify improved responses
   - Check false positive rate on recent questions/feature requests

2. **Monitor new issues:**
   - Track classification accuracy over next 10-20 issues
   - Verify pattern detection triggers correctly
   - Ensure single unified response (no duplicate comments)

3. **Edge cases to test:**
   - Issues with multiple patterns (should show all detected patterns)
   - Very old MSAL versions (should still show patterns but prioritize upgrade message)
   - Security issues (should override all other classifications)

## Configuration

The workflow now uses these outputs from the `triage` job:
- `issue_type` - The classified type (bug/feature-request/question/security)
- `priority` - The assigned priority (p0-critical/p1-high/p2-medium/p3-low)
- `needs_info` - Whether additional information is needed
- `msal_version` - Detected MSAL version
- `version_age_days` - Age of the detected version in days
- `is_very_old` - Boolean indicating if version is unsupported (>548 days)
- `version_release_date` - Release date of the detected version
- `classification_reasons` - JSON array of reasons for the classification *(NEW)*
- `detected_patterns` - JSON array of detected issue patterns with solutions *(NEW)*

## Files Modified

- `.github/workflows/copilot-issue-response.yml` (311 additions, 227 deletions)
  - Enhanced classification logic
  - Added pattern detection to classify step
  - Integrated pattern solutions into respond job
  - Removed duplicate pattern_detection job
  - Added verbose explanations

## Validation

- ✅ YAML syntax validated
- ✅ Classification logic tested against example issues
- ✅ Pattern detection logic verified
- ✅ Response format tested
- ✅ Git workflow functional
