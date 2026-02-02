# Prompt Template: Telemetry & Logging

Use this template when adding telemetry events, logging, or instrumentation.

## Template

```markdown
## Objective
Add telemetry to [track/measure/understand] [what] in [feature/flow].

## Context
[Why this telemetry is needed - what question does it answer?]

## Constraints
- **No PII**: Do not log email, phone, username, device ID, IP, or tokens
- Use existing telemetry infrastructure (do not add new logging libraries)
- Events must be behind a feature flag or sampling config
- Each event must answer a specific business/engineering question

## Event Schema
For each event, define:

| Field | Description |
|-------|-------------|
| Event name | Namespaced name (e.g., `feature_action_result`) |
| Purpose | What question does this answer? |
| Fields | Name, type, example value, PII risk |
| Trigger | When exactly is this logged? |

## Events to Add

### Event 1: [event_name]
- **Purpose:** [Question it answers]
- **Trigger:** [When logged]
- **Fields:**
  | Name | Type | Example | PII Risk |
  |------|------|---------|----------|
  | field1 | string | "value" | None |
  | field2 | int | 123 | None |

### Event 2: [event_name]
[Same structure]

## Acceptance Criteria
- [ ] Events defined with full schema
- [ ] No PII in any field (verified)
- [ ] Logging points identified (file paths + functions)
- [ ] Feature flag or sampling configured
- [ ] Local validation documented (how to see logs)
- [ ] Privacy review checklist completed

## Output Format
1. Event definitions (table format)
2. Implementation locations (file paths)
3. Local validation steps
4. Privacy checklist
```

## Examples

### Feature Usage Telemetry
```markdown
## Objective
Add telemetry to track usage patterns for [Feature X] to understand adoption and success rate.

## Context
Product needs to know: How many users try Feature X? How many succeed? Where do they drop off?

## Constraints
- **No PII**: No user identifiers, emails, or device IDs
- Use existing AriaLogger from SharedCoreLibrary
- Behind ExperimentationFeatureFlag.FEATURE_X_TELEMETRY
- Sample at 100% initially, can reduce if volume too high

## Events to Add

### Event 1: feature_x_started
- **Purpose:** Track feature entry rate
- **Trigger:** User opens Feature X screen
- **Fields:**
  | Name | Type | Example | PII Risk |
  |------|------|---------|----------|
  | entry_point | string | "settings" | None |
  | timestamp | long | 1704067200000 | None |

### Event 2: feature_x_completed
- **Purpose:** Measure success rate
- **Trigger:** User successfully completes Feature X flow
- **Fields:**
  | Name | Type | Example | PII Risk |
  |------|------|---------|----------|
  | duration_ms | long | 5432 | None |
  | steps_completed | int | 3 | None |

### Event 3: feature_x_abandoned
- **Purpose:** Understand drop-off points
- **Trigger:** User exits Feature X without completing
- **Fields:**
  | Name | Type | Example | PII Risk |
  |------|------|---------|----------|
  | last_step | string | "confirmation" | None |
  | duration_ms | long | 2100 | None |
  | reason | string | "back_pressed" | None |

## Acceptance Criteria
- [ ] 3 events capture full funnel (start → complete/abandon)
- [ ] No PII in any field
- [ ] Events logged in correct locations
- [ ] Feature flag works (off = no events)
- [ ] Can see events in local debug logs
- [ ] Privacy review: confirmed safe

## Output Format
Event table → Implementation locations → Local test steps → Privacy checklist
```

### Error Telemetry
```markdown
## Objective
Add telemetry to track and categorize errors in [Component] for debugging and alerting.

## Context
We're seeing user reports of failures but don't have visibility into error rates or types.

## Constraints
- **No PII**: No stack traces with variable values, no request bodies, no tokens
- Use error hashing (not full messages) to group similar errors
- Include enough context to debug, not enough to identify users
- Behind sampling config (start at 10%)

## Events to Add

### Event 1: component_error
- **Purpose:** Track error rate and categorization
- **Trigger:** Caught exception in [Component]
- **Fields:**
  | Name | Type | Example | PII Risk |
  |------|------|---------|----------|
  | error_type | string | "NetworkTimeout" | None |
  | error_hash | string | "a1b2c3d4" | None - hash only |
  | component | string | "AuthService" | None |
  | operation | string | "tokenRefresh" | None |
  | http_status | int | 503 | None |
  
- **Explicitly excluded (PII risk):**
  - error_message (may contain user data)
  - stack_trace (may contain file paths with usernames)
  - request_url (may contain tokens)
  - response_body (may contain PII)

## Acceptance Criteria
- [ ] Error categorization is useful for debugging
- [ ] No PII in logged fields (verified with examples)
- [ ] Sampling configured to prevent flood
- [ ] Can query by error_type and component
- [ ] Local validation shows events firing

## Output Format
Event schema → Exclusion list (what NOT to log) → Implementation → Validation
```

## PII Reference: What NOT to Log

Always verify against this list:

| Field Type | Risk | Alternative |
|------------|------|-------------|
| Email | PII | Don't log, or hash |
| Phone number | PII | Don't log |
| Username / Display name | PII | Don't log |
| Device ID | Tracking | Don't log, or hash |
| IP address | Location/Identity | Don't log |
| Full stack trace | May contain PII | Use error hash |
| Request/Response body | May contain credentials | Log operation name only |
| File paths | May contain username | Use relative paths |
| Tokens / Credentials | Security | Never log |
| Account ID | Semi-PII | Hash if needed |

## Key Constraints for Telemetry

Always include explicit PII prohibition:

```markdown
- **No PII**: Do not log email, phone, username, device ID, IP address, or tokens
- Use existing telemetry infrastructure (SharedCoreLibrary logging)
- Behind feature flag or sampling configuration
- Each event answers a specific question (no "log everything")
- Include local validation steps
```
