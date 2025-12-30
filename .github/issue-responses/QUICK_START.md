# Quick Start Guide - New Copilot Agent Features

This guide helps reviewers, maintainers, and developers quickly understand and test the new Copilot agent features.

## 🎯 What Changed?

Three new features for automated issue triage:

1. **Version-Aware Triage** - Automatically detects old MSAL versions (>1.5 years) and prompts upgrade
2. **Label Transparency** - Every label now comes with an explanation
3. **PING-COPILOT** - Users can trigger follow-up analysis by commenting `PING-COPILOT: <question>`

## 🚀 Quick Testing

### Test 1: Version Detection (2 minutes)

**Create a test issue:**
```
Title: "Authentication fails with MSAL v6.0.1"
Body: "I'm using MSAL version 6.0.1 and getting errors..."
```

**Expected Result:**
- ✅ `very-old-msal` label applied (orange)
- ✅ Response starts with "⚠️ **Unsupported MSAL Version Detected**"
- ✅ Upgrade instructions included
- ✅ Explanation: "version 6.0.1 (released [date]) is more than 1.5 years old"

### Test 2: Label Transparency (1 minute)

**Create a test issue:**
```
Title: "How do I configure redirect URI?"
Body: "I keep getting redirect mismatch errors..."
```

**Expected Result:**
- ✅ `question` label applied
- ✅ Response includes: "I've labeled this as a `question` because you're asking about..."
- ✅ Clear explanation of why the label was chosen

### Test 3: PING-COPILOT Follow-Up (3 minutes)

**Step 1:** Create any test issue

**Step 2:** Comment:
```
PING-COPILOT: Can you explain broker integration?
```

**Expected Result:**
- ✅ Automated response appears
- ✅ Response starts with "Thanks for the follow-up!"
- ✅ Broker-specific guidance provided
- ✅ PING-COPILOT reminder at end

**Step 3:** Try another follow-up:
```
PING-COPILOT: What if the Authenticator app isn't installed?
```

**Expected Result:**
- ✅ Another automated response
- ✅ Context-aware answer about fallback behavior

## 📋 Files Modified

| File | Purpose | Changes |
|------|---------|---------|
| `.github/copilot-instructions.md` | AI agent instructions | +203 lines: Version triage, label transparency, PING-COPILOT sections |
| `.github/workflows/copilot-issue-response.yml` | Automation workflow | +260 lines: Version detection, label logic, PING-COPILOT handler |
| `.github/issue-responses/customer-communication-guidelines.md` | Response templates | +170 lines: New features section with examples |
| `.github/issue-responses/copilot-features-examples.md` | **NEW** | 614 lines: Comprehensive usage examples |
| `.github/issue-responses/IMPLEMENTATION_SUMMARY.md` | **NEW** | Technical implementation details |
| `.github/issue-responses/README.md` | Directory overview | Updated with new features |

## 🔍 Key Implementation Details

### Version Detection Logic

```javascript
// Detects patterns like: v8.1.1, msal:8.0.2, version 7.1.0
const versionPatterns = [
  /v(\d+\.\d+\.\d+)/i,
  /msal[:\s]+(\d+\.\d+\.\d+)/i,
  /version[:\s]+(\d+\.\d+\.\d+)/i,
  /(\d+\.\d+\.\d+)/
];
```

### Support Window

- **Threshold:** 548 days (1.5 years)
- **Calculation:** `(current_date - release_date) > 548`
- **Current Status (Dec 2025):**
  - ✅ Supported: v7.0.0+ (Aug 2025+)
  - ❌ Unsupported: v6.x and earlier (Jul 2025 and earlier)

### New Labels

| Label | Color | When Applied |
|-------|-------|--------------|
| `very-old-msal` | Orange (ffa500) | Version > 1.5 years old |
| `triage-issue` | Light Blue (c5def5) | Requires engineering investigation |

### PING-COPILOT Trigger

- **Format:** `PING-COPILOT: <any text>`
- **Case-insensitive:** Works with lowercase `ping-copilot:`
- **Extraction:** Captures everything after the colon
- **Frequency:** Can be used multiple times per issue

## 📚 Documentation Structure

```
.github/
├── copilot-instructions.md          ← Main AI instructions (UPDATED)
├── workflows/
│   └── copilot-issue-response.yml   ← Automation (UPDATED)
└── issue-responses/
    ├── README.md                     ← Directory guide (UPDATED)
    ├── common-issues-guide.md        ← Troubleshooting reference
    ├── customer-communication-guidelines.md  ← Response templates (UPDATED)
    ├── copilot-features-examples.md  ← NEW: Usage examples
    └── IMPLEMENTATION_SUMMARY.md     ← NEW: Technical details
```

## 🎓 For Different Audiences

### For Reviewers
- Focus on: `.github/copilot-instructions.md` (sections 6.1-6.3)
- Key question: "Are the instructions clear and actionable?"
- Test: Create a test issue and verify automated response

### For Maintainers
- Focus on: `.github/workflows/copilot-issue-response.yml`
- Key questions: "Is the logic correct? Are edge cases handled?"
- Test: Try various version formats and edge cases

### For Users
- Focus on: PING-COPILOT mechanism
- Key question: "Is the follow-up support helpful?"
- Test: Use PING-COPILOT with real questions

### For AI Agents (Future Copilot Sessions)
- Primary source: `.github/copilot-instructions.md`
- Examples: `.github/issue-responses/copilot-features-examples.md`
- Templates: `.github/issue-responses/customer-communication-guidelines.md`

## ⚙️ Configuration

### Adjusting Support Window

To change the 1.5 year threshold:

**File:** `.github/workflows/copilot-issue-response.yml`
**Line:** ~95
```javascript
// Current: 548 days (1.5 years)
isVeryOld = versionAgeDays > 548;

// To change to 2 years (730 days):
isVeryOld = versionAgeDays > 730;
```

### Adding New Label Types

**File:** `.github/workflows/copilot-issue-response.yml`
**Section:** `Apply Labels` step

Add to `labelColors` object:
```javascript
const labelColors = {
  'bug': 'd73a4a',
  'very-old-msal': 'ffa500',
  'your-new-label': 'hex-color',  // ← Add here
  // ...
};
```

## 🐛 Troubleshooting

### Version Not Detected
- **Check:** Is version in format `v8.1.1`, `msal:8.1.1`, or `version 8.1.1`?
- **Fix:** Add pattern to `versionPatterns` array

### PING-COPILOT Not Triggering
- **Check:** Is phrase exactly `PING-COPILOT:` (with colon)?
- **Check:** Workflow permissions (needs `issues: write`)
- **Fix:** Verify workflow is enabled and has correct permissions

### Labels Not Applied
- **Check:** Do labels exist in repository?
- **Auto-fix:** Workflow automatically creates missing labels
- **Manual:** Create labels in repo settings if auto-creation fails

### Response Not Posting
- **Check:** GitHub Actions logs for errors
- **Check:** API rate limits
- **Fix:** Review workflow run logs in Actions tab

## 📞 Support

### For Questions About This PR
- Review: `.github/issue-responses/IMPLEMENTATION_SUMMARY.md`
- Examples: `.github/issue-responses/copilot-features-examples.md`
- Contact: PR author or MSAL Android team

### For Future Updates
- Update instructions in: `.github/copilot-instructions.md`
- Update examples in: `.github/issue-responses/copilot-features-examples.md`
- Update workflow logic in: `.github/workflows/copilot-issue-response.yml`

## ✅ Acceptance Criteria

Before merging, verify:

- [ ] All YAML files validate successfully
- [ ] Version detection works with multiple formats
- [ ] PING-COPILOT handler triggers on comments
- [ ] Labels are created automatically if missing
- [ ] Label explanations appear in responses
- [ ] PING-COPILOT footer appears in all initial responses
- [ ] Documentation is comprehensive and accurate
- [ ] Examples cover common scenarios
- [ ] No breaking changes to existing functionality

## 🎉 Benefits Summary

### Immediate Impact
- ✅ Automatic identification of unsupported versions
- ✅ Clearer communication through label explanations
- ✅ Reduced back-and-forth through PING-COPILOT

### Long-term Impact
- ✅ Fewer issues about old versions
- ✅ Better user understanding of triage process
- ✅ Consolidated discussions (less issue spam)
- ✅ Improved first-response quality

## 📅 Next Steps After Merge

1. **Week 1:** Monitor initial usage patterns
2. **Week 2-4:** Collect feedback from users and team
3. **Month 2:** Review metrics and adjust templates
4. **Quarterly:** Update version support threshold as needed

---

**Ready to Review?** Start with `.github/copilot-instructions.md` sections 6.1-6.3, then check the workflow changes!

**Ready to Test?** Create a test issue with "MSAL v6.0.1" and watch the magic happen!

**Questions?** Check `IMPLEMENTATION_SUMMARY.md` or `copilot-features-examples.md` for details!
