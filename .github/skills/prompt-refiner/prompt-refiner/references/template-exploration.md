# Prompt Template: Code Exploration

Use this template when you need to understand unfamiliar code, find where something is implemented, or trace a flow.

## Template

```markdown
## Objective
[Understand/Find/Trace] [specific thing] in the codebase.

## Context
[Why you need this - new to repo, investigating bug, planning feature, etc.]

## Constraints
- Only reference files that exist in the repo (provide file paths + line numbers)
- Do not guess patterns—search the codebase first
- Focus on [primary flow / specific area], not edge cases

## Questions to Answer
1. [Specific question 1 - e.g., "Where is the entry point?"]
2. [Specific question 2 - e.g., "What classes are involved?"]
3. [Specific question 3 - e.g., "How does data flow through?"]

## Acceptance Criteria
- [ ] Entry point(s) identified with file paths
- [ ] Key components listed with responsibilities
- [ ] Call flow documented (what calls what)
- [ ] Relevant config/manifest entries noted

## Output Format
Brief architecture overview, then numbered call flow with file paths.
```

## Examples

### Finding Authentication Flow
```markdown
## Objective
Understand how user authentication is implemented in this app.

## Context
I'm new to this codebase and need to add a new auth provider.

## Constraints
- Only reference files that exist (provide file paths + line numbers)
- Do not guess—search the codebase first
- Focus on the primary login flow, not account recovery or MFA

## Questions to Answer
1. Where does authentication start (UI entry point)?
2. What service/repository handles auth logic?
3. How are tokens stored and refreshed?
4. Where is the auth state managed?

## Acceptance Criteria
- [ ] Login entry point identified
- [ ] Auth service/repository located
- [ ] Token storage mechanism found
- [ ] State management approach documented

## Output Format
Architecture overview, then call flow from UI → service → storage.
```

### Tracing a Data Flow
```markdown
## Objective
Trace how [data type] flows from [source] to [destination].

## Context
Investigating why [data] sometimes appears incorrect in [location].

## Constraints
- Provide file paths for each step in the flow
- Note any transformations or validations along the way
- Flag any async/background processing

## Questions to Answer
1. Where does [data] originate?
2. What transformations occur?
3. Where is it persisted?
4. How does it reach [destination]?

## Acceptance Criteria
- [ ] Complete data flow mapped with file paths
- [ ] Transformations documented
- [ ] Potential failure points identified

## Output Format
Numbered flow diagram with file:line references.
```

## Key Constraints for Exploration

Always include these to get grounded responses:

```markdown
- Only reference files that exist in the repo (provide file paths + line numbers)
- Do not guess patterns—search the codebase first
- Show actual code snippets, not hypothetical examples
```
