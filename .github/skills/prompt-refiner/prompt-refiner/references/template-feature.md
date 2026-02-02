# Prompt Template: New Feature Implementation

Use this template when implementing new functionality, adding capabilities, or building new screens/flows.

## Template

```markdown
## Objective
Implement [feature] that [does what] for [who/what].

## Context
[Why this feature is needed - user problem, business requirement, technical debt]

## Constraints
- Wrap behind ExperimentationFeatureFlag.[FLAG_NAME]
- Use existing [patterns/libraries/infrastructure] - do not add new dependencies
- Follow patterns in [similar existing feature]
- No breaking changes to existing [APIs/behavior]

## Scope
**In scope:**
- [Specific capability 1]
- [Specific capability 2]

**Out of scope:**
- [What NOT to build]
- [Future enhancements to defer]

## Technical Requirements
- [Requirement 1 - e.g., "Must work offline"]
- [Requirement 2 - e.g., "Response time < 200ms"]
- [Requirement 3 - e.g., "Support Android API 26+"]

## Acceptance Criteria
- [ ] [Functional criterion 1]
- [ ] [Functional criterion 2]
- [ ] Feature flag integration working
- [ ] Unit tests added for [key logic]
- [ ] Compile check passes: `.\gradlew [module]:compileProductionDebugKotlin`

## Output Format
1. Implementation plan (files to create/modify)
2. Code changes with file paths
3. Test cases to add
```

## Examples

### Adding a Retry Mechanism
```markdown
## Objective
Implement automatic retry for failed API calls with exponential backoff.

## Context
Users experience intermittent failures due to network issues. Automatic retry will improve reliability.

## Constraints
- Wrap behind ExperimentationFeatureFlag.API_RETRY_ENABLED
- Use existing OkHttp client - do not add new HTTP libraries
- Only retry transient errors (5xx, timeout), not client errors (4xx)
- Maximum 3 retries with exponential backoff (1s, 2s, 4s)

## Scope
**In scope:**
- Retry logic with configurable count
- Exponential backoff with jitter
- Logging of retry attempts

**Out of scope:**
- UI indication of retries
- Offline queue/sync
- Per-endpoint retry configuration

## Technical Requirements
- Thread-safe implementation
- Configurable via remote config
- No memory leaks from pending retries

## Acceptance Criteria
- [ ] Retry logic triggers on 5xx and timeout
- [ ] Does not retry on 4xx errors
- [ ] Respects max retry count
- [ ] Backoff timing is correct (1s, 2s, 4s + jitter)
- [ ] Feature flag disables all retry behavior
- [ ] Unit tests cover: success, retry-then-success, max-retries-exceeded
- [ ] Compile check passes

## Output Format
Implementation plan, then code, then tests.
```

### Adding a New Screen (Compose)
```markdown
## Objective
Create a new [ScreenName] screen that [displays/allows] [what].

## Context
[Why this screen is needed]

## Constraints
- Use Jetpack Compose (not XML layouts)
- Colors from CommonColors.kt only
- Strings from strings.xml (no hardcoded text)
- Follow patterns in [similar existing screen]
- Wrap navigation behind ExperimentationFeatureFlag.[FLAG]

## Scope
**In scope:**
- Screen UI with [components]
- ViewModel with [state]
- Navigation from [source]

**Out of scope:**
- [Related screen]
- [Advanced feature]

## Technical Requirements
- Support light/dark mode (via CommonColors)
- Accessible (content descriptions, focusable elements)
- Handle loading/error/empty states

## Acceptance Criteria
- [ ] Screen renders correctly in light and dark mode
- [ ] All strings are localized (in strings.xml)
- [ ] ViewModel unit tests added
- [ ] Navigation works from [source]
- [ ] Feature flag gates access
- [ ] Compile check passes

## Output Format
1. File structure (new files to create)
2. ViewModel implementation
3. Composable implementation
4. Navigation wiring
5. Tests
```

## Key Constraints for Features

Always include feature flag and pattern-following:

```markdown
- Wrap behind ExperimentationFeatureFlag.[FLAG_NAME]
- Follow existing patterns in [similar feature area]
- Use existing infrastructure - do not add new libraries without approval
```
