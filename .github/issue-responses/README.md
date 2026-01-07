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
- Version-aware triage, label transparency, and PING-COPILOT follow-up mechanism (added December 2025)

## Automated Issue Response

The repository includes a GitHub Actions workflow ([../workflows/copilot-issue-response.yml](../workflows/copilot-issue-response.yml)) that:
1. Automatically triages new issues
2. Applies appropriate labels (bug, feature-request, question, priority, very-old-msal, triage-issue)
3. Posts initial acknowledgment and guidance with label explanations
4. Detects common error patterns and provides targeted help
5. Detects and responds to PING-COPILOT follow-up comments (added December 2025)
6. Identifies unsupported MSAL versions and provides upgrade guidance (added December 2025)

## Usage

### For AI Agents
1. Reference `common-issues-guide.md` when analyzing issue descriptions
2. Use templates from `customer-communication-guidelines.md` for responses
3. Always check MSAL version and apply version-aware triage rules (added December 2025)
4. Include label explanations in every response (added December 2025)
5. Add PING-COPILOT trigger instructions to initial responses (added December 2025)
6. Follow the diagnostic checklist to request necessary information
7. Link to relevant documentation and code snippets

### For Team Members
1. Review and update these guides as new common issues emerge
2. Ensure templates remain current with API changes
3. Monitor automated responses for accuracy
4. Escalate security issues through proper channels

### For Users
- **Need follow-up help?** Comment with `PING-COPILOT: <your question>` to trigger additional Copilot analysis
- The agent will analyze your question in the context of the entire issue thread
- You can use PING-COPILOT multiple times as needed

## Related Resources

- [Copilot Instructions](../copilot-instructions.md) - Main AI agent instructions
- [Code Snippets](../../snippets/) - Reference implementations
- [Golden Examples](../../examples/) - Complete working applications
- [Configuration Template](../../auth_config.template.json) - Full configuration options
