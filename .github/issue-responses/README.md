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

## Automated Issue Response

The repository includes a GitHub Actions workflow ([../workflows/copilot-issue-response.yml](../workflows/copilot-issue-response.yml)) that:
- Automatically triages new issues
- Applies appropriate labels (bug, feature-request, question, priority)
- Posts initial acknowledgment and guidance
- Detects common error patterns and provides targeted help

## Usage

### For AI Agents
1. Reference `common-issues-guide.md` when analyzing issue descriptions
2. Use templates from `customer-communication-guidelines.md` for responses
3. Follow the diagnostic checklist to request necessary information
4. Link to relevant documentation and code snippets

### For Team Members
1. Review and update these guides as new common issues emerge
2. Ensure templates remain current with API changes
3. Monitor automated responses for accuracy
4. Escalate security issues through proper channels

## Related Resources

- [Copilot Instructions](../copilot-instructions.md) - Main AI agent instructions
- [Code Snippets](../../snippets/) - Reference implementations
- [Golden Examples](../../examples/) - Complete working applications
- [Configuration Template](../../auth_config.template.json) - Full configuration options
