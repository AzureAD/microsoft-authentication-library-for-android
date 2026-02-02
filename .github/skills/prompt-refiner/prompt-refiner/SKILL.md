---
name: prompt-refiner
description: Refine rough prompts into structured, high-quality prompts. Use this skill when the user has a vague request and wants to turn it into a well-structured prompt with clear objectives, constraints, and acceptance criteria. Triggers include "refine this prompt", "make this prompt better", "structure this request", or "help me write a better prompt".
---

# Prompt Refiner

Transform rough, vague prompts into structured prompts that produce accurate, actionable results.

## References

Use these templates in the `references/` folder based on task type:
- **[template-exploration.md](references/template-exploration.md)** - Understanding code, finding implementations, tracing flows
- **[template-feature.md](references/template-feature.md)** - Implementing new functionality, adding screens
- **[template-bugfix.md](references/template-bugfix.md)** - Investigating and fixing bugs, crashes
- **[template-telemetry.md](references/template-telemetry.md)** - Adding logging, events, instrumentation

## Why This Matters

Vague prompts lead to:
- Hallucinated file names and patterns
- Generic advice instead of specific guidance
- Missing validation steps
- Wasted iteration cycles

Structured prompts lead to:
- Grounded responses with file paths and evidence
- Actionable next steps
- Built-in validation checkpoints
- Faster time-to-value

## Refinement Workflow

### Step 1: Analyze the Rough Prompt

Identify what's missing:
- **Objective**: What is the actual goal? (Often buried or implied)
- **Scope**: What's in/out of bounds?
- **Constraints**: What rules must be followed?
- **Evidence requirements**: Should responses cite files/code?
- **Validation**: How will we know if the answer is correct?

### Step 2: Ask Clarifying Questions (if needed)

Before refining, ask the user 2-3 targeted questions:
- "Is this for new code or modifying existing code?"
- "Should this be behind a feature flag?"
- "What's the risk level? (experimental vs production-critical)"
- "Are there existing patterns in the codebase I should follow?"

### Step 3: Generate the Refined Prompt

Use this template structure:

```markdown
## Objective
[One clear sentence describing the goal]

## Context
[Brief background if needed - what problem this solves, why now]

## Constraints
- [Hard rule 1 - e.g., "Only reference files that exist in the repo"]
- [Hard rule 2 - e.g., "Do not modify existing public APIs"]
- [Hard rule 3 - e.g., "Must be behind a feature flag"]

## Scope
**In scope:**
- [What should be addressed]

**Out of scope:**
- [What should NOT be addressed]

## Acceptance Criteria
- [ ] [Specific, verifiable criterion 1]
- [ ] [Specific, verifiable criterion 2]
- [ ] [Validation step - e.g., "Compile check passes"]

## Output Format
[Specify what the response should look like - file paths, code snippets, plan, etc.]
```

### Step 4: Add Domain-Specific Constraints

Based on the task type, add relevant constraints:

**For code exploration/understanding:**
- "Provide file paths and line numbers for all references"
- "Do not guess patterns—search the codebase first"
- "Show the call flow with actual function names"

**For new feature implementation:**
- "Wrap new functionality behind ExperimentationFeatureFlag"
- "Follow existing patterns in [similar feature area]"
- "Include unit test recommendations"

**For bug fixes:**
- "Identify root cause before proposing fix"
- "List potential regression risks"
- "Include validation steps to confirm fix"

**For refactoring:**
- "No behavioral changes—preserve existing functionality"
- "Show before/after for each change"
- "Ensure all existing tests still pass"

**For telemetry/logging:**
- "No PII in any logged fields"
- "Include local validation approach"
- "Specify sampling/flag configuration"

## Output Format

When refining a prompt, provide:

1. **The refined prompt** (ready to copy/paste)
2. **What was added** (brief explanation of key improvements)
3. **Suggested follow-ups** (what to ask next after getting results)

## Examples

### Example 1: Code Exploration

**Rough prompt:**
> "Where is authentication handled in this app?"

**Refined prompt:**
```markdown
## Objective
Identify the entry points and core components responsible for authentication in this codebase.

## Constraints
- Only reference files that exist in the repo (provide file paths)
- Focus on the primary authentication flow, not edge cases
- Do not guess—search for actual implementations

## Acceptance Criteria
- [ ] Entry point(s) identified with file paths
- [ ] Key classes/functions listed with their responsibilities
- [ ] Call flow documented (what calls what)
- [ ] Links to relevant configuration files

## Output Format
Provide a brief architecture overview with file paths, then a numbered call flow.
```

**What was added:** Evidence requirements (file paths), scope limitation (primary flow), specific output format.

---

### Example 2: New Feature

**Rough prompt:**
> "Add a retry mechanism for failed API calls"

**Refined prompt:**
```markdown
## Objective
Implement a retry mechanism for failed API calls with configurable retry count and backoff.

## Context
Some API calls fail transiently due to network issues. We need automatic retry with exponential backoff.

## Constraints
- Use existing HTTP client infrastructure (do not add new libraries)
- Wrap behind ExperimentationFeatureFlag.API_RETRY
- Only retry on transient errors (5xx, timeout), not client errors (4xx)
- Maximum 3 retries with exponential backoff (1s, 2s, 4s)

## Scope
**In scope:** Core retry logic, configuration, integration with existing client

**Out of scope:** UI changes, offline handling, request queuing

## Acceptance Criteria
- [ ] Retry logic implemented with configurable count
- [ ] Exponential backoff with jitter
- [ ] Feature flag integration
- [ ] Unit tests for retry scenarios (success after retry, max retries exceeded)
- [ ] Compile check passes: `.\gradlew app:compileProductionDebugKotlin`

## Output Format
1. Implementation plan (which files to modify)
2. Code changes with file paths
3. Test cases to add
```

**What was added:** Specific behavior (which errors to retry), constraints (feature flag, no new libs), concrete acceptance criteria.

---

### Example 3: Telemetry

**Rough prompt:**
> "Add logging for the sign-in flow"

**Refined prompt:**
```markdown
## Objective
Add telemetry events to track sign-in flow success, failure, and duration.

## Constraints
- **No PII**: Do not log email, username, phone, device ID, or tokens
- Use existing telemetry service (SharedCoreLibrary logging)
- Events must be behind a feature flag or sampling config
- Each event must answer a specific business question

## Event Requirements
For each event, define:
- Event name (namespaced: `signin_*`)
- Purpose (what question does this answer?)
- Fields (name, type, example, PII risk)
- Trigger condition

## Acceptance Criteria
- [ ] 2-3 events defined with full schema
- [ ] Logging points identified (file paths + function names)
- [ ] Local validation approach documented
- [ ] No PII in any field
- [ ] Feature flag specified

## Output Format
Event table, then implementation locations, then validation steps.
```

**What was added:** Explicit PII prohibition, schema requirements, validation approach.

## Anti-Patterns to Avoid

When refining prompts, watch for and fix these issues:

| Anti-Pattern | Problem | Fix |
|--------------|---------|-----|
| "Make it good" | Subjective, unmeasurable | Add specific acceptance criteria |
| "Handle all cases" | Unbounded scope | Define in-scope vs out-of-scope |
| "Like other apps do" | Relies on assumptions | Reference specific patterns in THIS codebase |
| "ASAP" | Pressure without clarity | Define actual priority and constraints |
| No validation step | Can't verify correctness | Add "how do we know it's right?" |

## Quick Reference: Constraint Templates

Copy-paste these common constraints as needed:

**Evidence-based responses:**
```
- Only reference files that exist in the repo (provide file paths + line numbers)
- Do not guess patterns—search the codebase first
- Show actual code/config, not hypothetical examples
```

**Safe implementation:**
```
- Wrap new functionality behind ExperimentationFeatureFlag.[FLAG_NAME]
- No breaking changes to existing public APIs
- Follow existing patterns in [similar area of codebase]
```

**Privacy/security:**
```
- No PII in logs (email, phone, name, device ID, tokens)
- No hardcoded secrets or credentials
- Use SecureKeystoreLibrary for sensitive storage
```

**Validation:**
```
- Compile check: `.\gradlew [module]:compileProductionDebugKotlin`
- Existing tests pass: `.\gradlew [module]:test`
- Manual verification steps documented
```
