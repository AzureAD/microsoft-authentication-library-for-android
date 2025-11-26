# Customer Communication Guidelines for GitHub Issues

This document provides guidelines for AI agents and team members when responding to MSAL Android GitHub issues. Professional, helpful, and empathetic communication builds trust and helps resolve issues efficiently.

---

## Table of Contents

1. [Communication Principles](#communication-principles)
2. [Response Templates](#response-templates)
3. [Issue Triage Guidelines](#issue-triage-guidelines)
4. [Escalation Procedures](#escalation-procedures)
5. [What NOT to Do](#what-not-to-do)

---

## Communication Principles

### Be Professional and Empathetic

- **Acknowledge the issue**: Thank users for reporting and show you understand their frustration
- **Be patient**: Users may not have deep technical knowledge
- **Be respectful**: Avoid condescending language or assumptions about user skill level
- **Be concise**: Provide clear, actionable information without overwhelming

### Response Time Expectations

| Issue Type | Initial Response Target | Full Resolution Target |
|------------|------------------------|------------------------|
| Security Issue | 4 hours | As soon as possible |
| Production Blocker | 24 hours | 3 business days |
| Bug Report | 48 hours | Varies by complexity |
| Feature Request | 72 hours | Backlog prioritization |
| Question | 48 hours | Immediate if possible |

### Key Communication Guidelines

1. **Always respond professionally** - Even if the issue is unclear or the user is frustrated
2. **Provide actionable next steps** - Don't leave users hanging
3. **Reference documentation** - Link to relevant resources when applicable
4. **Set expectations** - Be clear about what can and cannot be done
5. **Follow up** - Check back if you've asked for information

---

## Response Templates

### Initial Acknowledgment

```markdown
Thank you for reporting this issue! We appreciate you taking the time to help us improve MSAL Android.

I'm looking into this now and will provide an update shortly. In the meantime, could you please provide:

1. **MSAL Version**: (e.g., 7.1.0)
2. **Android Version**: (Device API level)
3. **Account Mode**: (Single or Multiple)
4. **Complete error message or stack trace**

This information will help us diagnose the issue more quickly.
```

### Requesting More Information

```markdown
Thank you for your report! To help us investigate this issue further, could you please provide:

**Required Information:**
- [ ] MSAL version you're using
- [ ] Android version and device model
- [ ] Complete error message or stack trace
- [ ] Steps to reproduce the issue
- [ ] Your `auth_config.json` (with sensitive values like client_id redacted)

**Optional but Helpful:**
- [ ] Verbose logs (enable with `Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE)`)
- [ ] Sample code demonstrating the issue

This will help us understand and resolve your issue more effectively.
```

### Known Issue Response

```markdown
Thank you for reporting this! This is a known issue that we're tracking.

**Issue**: [Brief description]

**Workaround**: [If available]
[Provide specific code or configuration changes]

**Status**: [Current status - investigating/in progress/planned for vX.X]

We'll update this issue when we have more information. In the meantime, please try the workaround and let us know if you have any questions.
```

### Configuration Error Response

```markdown
Thank you for reaching out! Based on your description, this appears to be a configuration issue.

**Issue Identified**: [Specific problem]

**Solution**:

[Step-by-step fix]

**Important Notes**:
- The redirect URI in `auth_config.json` must be URL-encoded
- The signature hash in `AndroidManifest.xml` must NOT be URL-encoded
- Ensure your Azure App Registration matches your configuration

Please try these changes and let us know if the issue persists. You can find more details in our [configuration template](../../auth_config.template.json).
```

### Code Example Response

```markdown
Thank you for your question! Here's how to properly implement [feature]:

```java
// Correct implementation
[Code example following current API patterns]
```

**Key Points**:
1. Always use the Parameters-based API (avoid deprecated methods)
2. [Additional guidance specific to the question]
3. [Link to relevant snippet or example]

For a complete working example, please refer to our [golden examples](../../examples/).

Let us know if you have any other questions!
```

### Cannot Reproduce Response

```markdown
Thank you for reporting this issue. I've attempted to reproduce it using the information provided, but I haven't been able to replicate the behavior.

Could you please provide:

1. **Minimal reproduction case**: A simplified version of your code that demonstrates the issue
2. **Environment details**: 
   - Exact MSAL version
   - Android device/emulator details
   - Any relevant device settings (work profile, etc.)
3. **Network conditions**: Any proxies, VPNs, or firewalls in use

Additionally, enabling verbose logging may help:
```java
Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
Logger.getInstance().setEnableLogcatLog(true);
```

This will help us better understand what's happening in your specific environment.
```

### Closing as Resolved

```markdown
Glad we could help resolve this issue! 

**Summary**: [Brief description of the solution]

I'm closing this issue now. If you encounter any further problems, please don't hesitate to open a new issue.

Thank you for using MSAL Android!
```

### Feature Request Response

```markdown
Thank you for this feature request! We appreciate your feedback on how to improve MSAL Android.

**Your Request**: [Summary of the feature]

I've added this to our backlog for team review. While I can't provide a specific timeline, your feedback helps us prioritize future development.

In the meantime, here are some alternatives you might consider:
[If applicable, suggest workarounds or alternative approaches]

We'll update this issue if there are any developments. Thank you for contributing to the improvement of MSAL Android!
```

### Security Issue Response

```markdown
Thank you for reporting this security concern. Security is a top priority for us.

**IMPORTANT**: For security issues, please report them through our [security reporting process](https://github.com/AzureAD/microsoft-authentication-library-for-android#security-reporting) rather than public GitHub issues to protect users while we investigate.

If you've already followed that process, our security team will be in touch.

For general security best practices with MSAL Android:
- Always use the latest version of MSAL
- Enable broker integration for enhanced security
- Never log or store tokens in plain text
- Use HTTPS for all network communications

Thank you for helping keep MSAL Android secure!
```

### Duplicate Issue Response

```markdown
Thank you for reporting this! This issue appears to be related to #[ISSUE_NUMBER].

To keep the discussion in one place, I'm closing this as a duplicate. Please follow #[ISSUE_NUMBER] for updates.

If you believe your issue is different, please let us know and we'll reopen this for further investigation.
```

### Out of Scope Response

```markdown
Thank you for reaching out! However, this issue appears to be related to [Azure AD / Microsoft Graph / Other Service] rather than the MSAL Android library itself.

For assistance with this, please try:
- **Azure AD Issues**: [Azure AD Support](https://azure.microsoft.com/support/)
- **Microsoft Graph Issues**: [Microsoft Graph GitHub](https://github.com/microsoftgraph)
- **General Azure Support**: Contact your Microsoft representative

If you believe this is an MSAL Android issue, please provide additional details about how MSAL is involved, and we'll be happy to investigate further.
```

---

## Issue Triage Guidelines

### Priority Levels

| Priority | Criteria | Action |
|----------|----------|--------|
| P0 - Critical | Security vulnerability, data loss, complete breakage | Immediate escalation to team |
| P1 - High | Production app blocked, major feature broken | Address within 24 hours |
| P2 - Medium | Feature doesn't work as expected, workaround exists | Standard queue |
| P3 - Low | Minor bug, cosmetic issue, enhancement | Backlog |

### Issue Classification

**Bug Reports** - Something isn't working correctly
- Verify with reproduction steps
- Check if it's a known issue
- Determine if it's configuration vs. library issue

**Feature Requests** - New functionality desired
- Assess alignment with MSAL roadmap
- Check if workaround exists
- Add appropriate labels

**Questions** - User needs guidance
- Provide direct answer if possible
- Link to relevant documentation
- Consider if documentation should be updated

**Security Issues** - Potential vulnerability
- Redirect to security reporting process
- Do not discuss details publicly
- Escalate immediately if valid

---

## Escalation Procedures

### When to Escalate

1. **Security vulnerabilities** - Any confirmed security issue
2. **Production-blocking issues** - Issues affecting released apps in production
3. **Complex technical issues** - Problems requiring deep investigation
4. **Repeated issues** - Same problem reported multiple times
5. **Negative sentiment** - User is significantly frustrated

### How to Escalate

1. Add the appropriate priority label
2. Tag the relevant team members
3. Provide a summary of the issue and investigation so far
4. Include all relevant logs and reproduction steps

---

## What NOT to Do

### Never:

1. **Share sensitive information**
   - Don't post client IDs, secrets, or tokens
   - Don't share internal discussion details
   - Don't expose user PII

2. **Make promises about timelines**
   - Don't commit to specific fix dates
   - Don't promise features will be added
   - Use "we're investigating" rather than "we will fix"

3. **Blame the user**
   - Don't be condescending about mistakes
   - Don't assume incompetence
   - Frame feedback constructively

4. **Ignore issues**
   - Always acknowledge receipt
   - Provide status updates
   - Close with resolution or explanation

5. **Discuss internal matters**
   - Don't reference internal tickets by number
   - Don't discuss team dynamics
   - Keep focus on the technical issue

6. **Provide incomplete solutions**
   - Test code before sharing
   - Verify documentation links work
   - Ensure solutions follow current best practices

---

## Quality Checklist

Before responding to any issue, verify:

- [ ] Tone is professional and empathetic
- [ ] Response is clear and actionable
- [ ] Code examples follow current API patterns
- [ ] Links are valid and relevant
- [ ] No sensitive information is exposed
- [ ] Response addresses the actual question
- [ ] Appropriate labels are applied
- [ ] Follow-up is planned if needed

---

*These guidelines are maintained by the MSAL Android team. For questions about specific situations, consult with the team lead.*
