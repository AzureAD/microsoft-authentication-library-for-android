# MSAL Android Issue Response Resources

This directory contains resources for AI agents and team members to effectively respond to GitHub issues.

## Contents

### [common-issues-guide.md](common-issues-guide.md)
A comprehensive reference guide for diagnosing and resolving common MSAL Android issues. Includes:
- Configuration issues (redirect URI encoding, client ID, manifest setup)
- Authentication errors (AADSTS codes, user cancellation)
- Token acquisition issues (deprecated APIs, scope formats)
- Broker integration issues
- Build and dependency issues
- Runtime crashes
- Single vs Multiple account mode issues
- Silent token refresh issues

### [customer-communication-guidelines.md](customer-communication-guidelines.md)
Guidelines for professional and effective communication when responding to GitHub issues. Includes:
- Communication principles and response time expectations
- Response templates for various issue types
- Issue triage guidelines
- Escalation procedures
- Quality checklist
- **NEW (2025):** Version-aware triage, label transparency, and PING-COPILOT follow-up mechanism

### [copilot-features-examples.md](copilot-features-examples.md) ⭐ NEW
Comprehensive examples demonstrating the new Copilot agent features:
- **Version-aware triage:** Automatic detection of unsupported MSAL versions (>1.5 years old)
- **Label transparency:** Clear explanations for every label applied
- **PING-COPILOT mechanism:** User-triggered follow-up analysis for iterative support
- Real-world scenarios and expected responses
- Testing guidelines

## Automated Issue Response

The repository includes a GitHub Actions workflow ([../workflows/copilot-issue-response.yml](../workflows/copilot-issue-response.yml)) that:
- Automatically triages new issues
- Applies appropriate labels (bug, feature-request, question, priority, very-old-msal, triage-issue)
- Posts initial acknowledgment and guidance with label explanations
- Detects common error patterns and provides targeted help
- **NEW:** Detects and responds to PING-COPILOT follow-up comments
- **NEW:** Identifies unsupported MSAL versions and provides upgrade guidance

## Usage

### For AI Agents
1. Reference `common-issues-guide.md` when analyzing issue descriptions
2. Use templates from `customer-communication-guidelines.md` for responses
3. **NEW:** Always check MSAL version and apply version-aware triage rules
4. **NEW:** Include label explanations in every response
5. **NEW:** Add PING-COPILOT trigger instructions to initial responses
6. Follow the diagnostic checklist to request necessary information
7. Link to relevant documentation and code snippets

### For Team Members
1. Review and update these guides as new common issues emerge
2. Ensure templates remain current with API changes
3. Monitor automated responses for accuracy
4. Escalate security issues through proper channels
5. **NEW:** Review `copilot-features-examples.md` for understanding automated triage behavior

### For Users
- **Need follow-up help?** Comment with `PING-COPILOT: <your question>` to trigger additional Copilot analysis
- The agent will analyze your question in the context of the entire issue thread
- You can use PING-COPILOT multiple times as needed

## Related Resources

- [Copilot Instructions](../copilot-instructions.md) - Main AI agent instructions
- [Code Snippets](../../snippets/) - Reference implementations
- [Golden Examples](../../examples/) - Complete working applications
- [Configuration Template](../../auth_config.template.json) - Full configuration options
