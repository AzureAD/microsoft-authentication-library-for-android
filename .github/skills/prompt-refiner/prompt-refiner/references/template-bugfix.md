# Prompt Template: Bug Fix

Use this template when investigating and fixing bugs, crashes, or unexpected behavior.

## Template

```markdown
## Objective
Fix [bug/issue] where [symptom] occurs when [condition].

## Observed Behavior
- **What happens:** [Describe the bug]
- **Expected:** [What should happen]
- **Repro steps:** [How to reproduce]
- **Frequency:** [Always / Sometimes / Rare]

## Context
- **Affected area:** [Screen/feature/flow]
- **First noticed:** [When - release, commit, date]
- **User impact:** [Severity - blocking, degraded, cosmetic]

## Constraints
- Identify root cause before proposing fix
- Minimize change scope - fix the bug, don't refactor
- No breaking changes to existing behavior
- Add regression test to prevent recurrence

## Investigation Steps
1. [Where to look first]
2. [What to trace]
3. [How to reproduce locally]

## Acceptance Criteria
- [ ] Root cause identified with evidence
- [ ] Fix addresses root cause (not just symptom)
- [ ] Existing tests still pass
- [ ] New test added covering this case
- [ ] No regressions in related functionality
- [ ] Compile check passes: `.\gradlew [module]:compileProductionDebugKotlin`

## Output Format
1. Root cause analysis
2. Proposed fix with file paths
3. Regression test to add
4. Verification steps
```

## Examples

### Crash Bug
```markdown
## Objective
Fix crash in [FeatureActivity] when user [action] with [condition].

## Observed Behavior
- **What happens:** App crashes with NullPointerException
- **Expected:** [Expected behavior]
- **Repro steps:**
  1. Open [screen]
  2. [Action]
  3. App crashes
- **Frequency:** Always when [condition]

## Context
- **Affected area:** [Feature] flow
- **First noticed:** After [version/commit]
- **User impact:** Blocking - users cannot complete [task]
- **Stack trace:**
  ```
  java.lang.NullPointerException: ...
      at com.microsoft.authenticator.[Class].[method]([File].kt:123)
  ```

## Constraints
- Identify why the null occurs, don't just add null checks everywhere
- Preserve existing behavior for non-null cases
- Add test that would have caught this

## Investigation Steps
1. Find the crash location from stack trace
2. Trace where the null value originates
3. Determine why it's null in this scenario
4. Check if this is a race condition, missing initialization, or bad data

## Acceptance Criteria
- [ ] Root cause identified (why is it null?)
- [ ] Fix prevents null at source (not just null check at crash site)
- [ ] Unit test added that reproduces the scenario
- [ ] No new crashes in related flows
- [ ] Compile check passes

## Output Format
Root cause → Fix → Test → Verification steps
```

### Logic Bug
```markdown
## Objective
Fix incorrect [behavior] where [wrong thing] happens instead of [right thing].

## Observed Behavior
- **What happens:** [Wrong behavior]
- **Expected:** [Correct behavior]
- **Repro steps:** [Steps]
- **Frequency:** [When it occurs]

## Context
- **Affected area:** [Component]
- **First noticed:** [When]
- **User impact:** [Impact description]

## Constraints
- Understand the intended logic before changing
- Check if this is a regression (was it ever correct?)
- Verify fix doesn't break other code paths

## Investigation Steps
1. Find the logic that produces wrong result
2. Trace inputs to understand why wrong path is taken
3. Check for off-by-one, wrong comparison, missing condition
4. Review recent changes to this area

## Acceptance Criteria
- [ ] Incorrect logic identified
- [ ] Fix produces correct behavior for all cases
- [ ] Edge cases considered (null, empty, boundary values)
- [ ] Test added covering the bug scenario
- [ ] Existing tests still pass

## Output Format
Analysis → Root cause → Fix → Test cases
```

### UI Bug
```markdown
## Objective
Fix UI issue where [visual problem] appears in [location/condition].

## Observed Behavior
- **What happens:** [Visual description - overlap, wrong color, missing element]
- **Expected:** [Correct appearance]
- **Repro steps:** [How to see it]
- **Affected configurations:** [Light/dark mode, screen sizes, languages]

## Context
- **Affected screen:** [Screen name]
- **Component:** [Composable/View name]
- **User impact:** [Cosmetic / Confusing / Blocking]

## Constraints
- Use CommonColors.kt for any color fixes
- Maintain accessibility (contrast, touch targets)
- Test both light and dark mode
- Check RTL layout if text-related

## Investigation Steps
1. Identify the composable/view responsible
2. Check modifier order, constraints, theme usage
3. Test in both light/dark mode
4. Test on different screen sizes

## Acceptance Criteria
- [ ] Visual issue resolved in all affected configurations
- [ ] Light mode correct
- [ ] Dark mode correct
- [ ] Accessibility maintained
- [ ] No regressions in related UI

## Output Format
Problem location → Fix → Before/after description → Test checklist
```

## Key Constraints for Bug Fixes

Always include root cause requirement and regression prevention:

```markdown
- Identify root cause before proposing fix (don't just mask symptoms)
- Add regression test that would have caught this bug
- Minimize change scope - fix the bug, don't refactor unrelated code
- Verify fix doesn't break other code paths
```
