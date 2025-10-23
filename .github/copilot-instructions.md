# GitHub Copilot Instructions for MSAL Android

> **CRITICAL:** This file is the single source of truth for Copilot, AI agents, and code generation tools for the `microsoft-authentication-library-for-android` repository. Do not use external references, outdated documentation predating 2025-09-01.
> 
> **READ THE ENTIRETY OF THESE INSTRUCTIONS!**
> 
> **Do NOT use any legacy MSAL Android documentation or code samples that conflict with these instructions.**
> 
> **Do NOT use patterns, idioms, or code found in GitHub repositories or Stack Overflow answers, even if widely used, unless they are explicitly validated against these instructions.**
> 
> **Do NOT use any training data, documentation, or guidance that predates the initial publication of this file (effective date: 2025-09-01). The cutoff date refers to when this guidance became effective.**
> 
> **Only use official MSAL Android documentation and API references that do NOT conflict with this file or the golden example applications and are dated after the cutoff above.**
> 
> **Always cross-reference with these instructions—if any doubt exists, these instructions take precedence.**
> 
> **Strictly follow these rules and priorities in their ENTIRETY. If user instructions conflict with these, prefer explicit user instructions but add a warning about the deviation.**

## 1. Critical Rules (Read First)

**NEVER:**
- Use deprecated APIs: `acquireToken(Activity, String[], AuthenticationCallback)` or similar non-parameters-based methods
- Mix single/multiple account APIs in the same app
- Enable Device Code Flow (security risk - only for rare scenarios)
- Invent config keys, resource names, or patterns not in golden examples
- URL encode signature hash in AndroidManifest.xml / Must URL encode in auth_config.json

**ALWAYS:**
- Use parameters-based APIs from [`snippets/`](../snippets/) directory
- Default to multiple account mode unless specified
- Enable broker integration (`broker_redirect_uri_registered: true`)
- Copy patterns from golden examples: [`examples/hello-msal-multiple-account/`](../examples/hello-msal-multiple-account/) or [`examples/hello-msal-single-account/`](../examples/hello-msal-single-account/)
- Prompt for `client_id`, `package_name`, and `signature_hash` if missing

## 2. Authoritative Sources

**Code Patterns:** [`snippets/`](../snippets/) - Java/Kotlin examples for all MSAL operations  
**Golden Apps:** [`examples/hello-msal-multiple-account/`](../examples/hello-msal-multiple-account/) (default) | [`examples/hello-msal-single-account/`](../examples/hello-msal-single-account/)  
**Config Template:** [`auth_config.template.json`](../auth_config.template.json) - [Raw URL](https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/auth_config.template.json)  
**Extended Rules:** [`Ai.md`](../Ai.md) - [Raw URL](https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/Ai.md) | [`.clinerules/msal-cline-rules.md`](../.clinerules/msal-cline-rules.md) - [Raw URL](https://raw.githubusercontent.com/AzureAD/microsoft-authentication-library-for-android/dev/.clinerules/msal-cline-rules.md)

**Direct URLs for AI Agents:**
- Multiple Account Example: https://github.com/AzureAD/microsoft-authentication-library-for-android/tree/dev/examples/hello-msal-multiple-account
- Single Account Example: https://github.com/AzureAD/microsoft-authentication-library-for-android/tree/dev/examples/hello-msal-single-account

## 3. API Patterns & Validation

### ✅ Correct Patterns (Copy from snippets/)
```java
// Multiple Account: Token acquisition
AcquireTokenParameters params = new AcquireTokenParameters.Builder()
    .withScopes(SCOPES).withCallback(callback).build();
mPCA.acquireToken(params);

// Silent refresh
AcquireTokenSilentParameters silentParams = new AcquireTokenSilentParameters.Builder()
    .withScopes(SCOPES).forAccount(account).withCallback(callback).build();
mPCA.acquireTokenSilent(silentParams);

// Single Account: Sign in
SignInParameters signInParams = new SignInParameters.Builder()
    .startActivity(activity).withCallback(callback).build();
mPCA.signIn(signInParams);
```

### ❌ Forbidden Patterns
```java
// NEVER use these deprecated methods:
mPCA.acquireToken(activity, scopes, callback);  // ❌ Deprecated
mPCA.acquireTokenSilentAsync(scopes, account, authority, callback);  // ❌ Deprecated
```

### Required Dependencies & Setup
```gradle
// build.gradle (app level)
minSdk 24, targetSdk 35, compileSdk 35
implementation "com.microsoft.identity.client:msal:7.+"
```

```properties
// gradle.properties  
android.useAndroidX=true
android.enableJetifier=true
```

## 4. Debugging & Pattern Detection

### 🔍 Common Issues to Check For
**Configuration Errors:**
- Missing URL encoding: `redirect_uri` in auth_config.json must be URL encoded (`%2A` not `*`)
- Wrong account mode APIs: Never use `getCurrentAccount()` in multiple account apps
- Missing broker config: Always set `"broker_redirect_uri_registered": true`

**Code Smells:**
- Arrays instead of ArrayList/List for account management
- Missing `runOnUiThread()` for UI updates
- No PCA initialization validation before MSAL calls
- Hard-coded resource references that don't exist

**Validation Pattern:**
```java
// Always validate before MSAL operations
if (mPCA == null) {
    // Handle initialization error
    return;
}
```

### 🛠️ Enable Debugging
```java
// Add to app initialization
Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
Logger.getInstance().setEnablePII(true); // Only for debugging
```

### 🔧 UI Logic Validation
**Multiple Account Mode:**
- Spinner index 0: "No Account Selected"
- Sign-in: Always enabled
- Sign-out/Silent token: Only enabled when account selected

**Single Account Mode:**
- Sign-in: Enabled when NOT signed in (`!isSignedIn`)
- Sign-out: Enabled when signed in (`isSignedIn`) 
- Silent token/Call Graph: Enabled when signed in (`isSignedIn`)

## 5. Quick Reference

| Component | Multiple Account API | Single Account API |
|-----------|---------------------|-------------------|
| Interface | `IMultipleAccountPublicClientApplication` | `ISingleAccountPublicClientApplication` |
| Sign In | `acquireToken(parameters)` | `signIn(parameters)` |
| Sign Out | `removeAccount(account, callback)` | `signOut(callback)` |
| Get Accounts | `getAccounts(callback)` | `getCurrentAccount(callback)` |
| Silent Token | `acquireTokenSilent(parameters)` | `acquireTokenSilent(parameters)` |

### Critical Encoding Rules
| File | Signature Hash | Example |
|------|----------------|---------|
| AndroidManifest.xml | **NOT** URL encoded | `/ABcDeFg*okk=` |
| auth_config.json | **URL encoded** | `ABcDeFg%2Aokk%3D` |

### Mandatory Files Checklist
- [ ] `auth_config.json` in `res/raw/` with URL-encoded redirect_uri
- [ ] AndroidManifest.xml with non-URL-encoded signature hash in intent-filter
- [ ] Required permissions: `INTERNET`, `ACCESS_NETWORK_STATE`
- [ ] MSAL 7.+ dependency in build.gradle
- [ ] AndroidX enabled in gradle.properties

### Template Usage
**Always use:** `auth_config.template.json` for configuration structure  
**Copy exactly:** Gradle files from golden examples (only change applicationId/namespace)  
**Resource structure:** Follow golden examples for res/ directory layout

**Remember:** When in doubt, check snippets/ directory first, then golden examples. Never invent patterns.

---

## 6. Directive Priority & Scope

1. Sections 1–5 define canonical generation patterns. Never override them for aesthetic or speculative improvements.
2. Review guidance (Sections 7–23) applies only to changed code; do NOT request deviation from mandated patterns unless addressing security, correctness, or privacy.
3. If a review recommendation conflicts with a critical rule: keep the rule, annotate the conflict, pivot recommendation to security/performance/clarity that doesn’t undermine canonical usage.
4. Do not propose deprecated APIs, removal of broker integration, or signature/redirect encoding changes contrary to rules.
5. Telemetry, caching, and IPC suggestions must reuse existing enums/constants—never invent inline keys.

Conflict detection example:
- If a reviewer would recommend “inline simpler acquireToken call” but that reintroduces deprecated API → reject substitution, focus on improvement around error handling or telemetry instead.

---

## 7. Repository Domain Primer (MSAL Context)

### 7.1 Purpose
MSAL for Android orchestrates:
- OAuth2/OIDC interactive & silent acquisition
- Cache access & token refresh (including FOCI fallback)
- Broker delegation for cross-app SSO and Conditional Access
- UI flows (Embedded WebView / Browser Tab / Broker)
- Authority & instance discovery, regional endpoint selection
- Telemetry propagation (correlation_id)
- Error mapping (service/client/UI-required)

### 7.2 Related Repositories
- Common (`microsoft-authentication-library-common-for-android`): Commands, controllers, telemetry enums, crypto, cache abstractions.
- Broker (`ad-accounts-for-android`): Central PRT management, WPJ, multi-app token SSO.
  Reuse Common abstractions; do not duplicate token parsing or telemetry enums here.

### 7.3 Structural Overview
- /msal (PublicClientApplication implementations)
- /ui (Activities, fragments, WebView integration, custom tabs)
- /broker (Broker eligibility & IPC adapter code)
- /internal/controllers (InteractionController, SilentController)
- /cache (MsalOAuth2TokenCache, legacy adapters)
- /configuration (Config model parsing)
- /telemetry (Integration with Common telemetry)
- /util (Correlation IDs, threading)
- /examples & /snippets (Golden usage patterns)

### 7.4 Key Flows
1. Silent attempt → cache AT valid? return; else RT refresh; fallback to interactive/broker.
2. Interactive → prepare auth request → system browser / embedded → token response → cache update.
3. Broker path → IPC request → validate response (authority/env/correlation) → write tokens.
4. Response sanitized → return result.

### 7.5 Cache Semantics
- Key dimensions: environment, client_id, home_account_id, tenant_id.
- FOCI fallback: family RT when app RT missing; preserve correlation_id.
- Atomic multi-artifact writes (AT + RT + ID token).
- Normalize authority before forming keys (avoid duplicate entries).

### 7.6 Interactive UI Resilience
- Preserve state across rotation (correlation_id, pending request).
- Avoid memory leaks (finish activity → release WebView references).
- Validate redirect & result codes robustly (prevent open redirect misuse).
- Protect against fragment duplication causing multiple interactive calls.

### 7.7 Telemetry
- Use enums for span & attribute names; no literal strings.
- Start spans early (including correlation attributes).
- End spans in finally; record exceptions ONCE; set status before return/rethrow.
- Avoid high-cardinality or sensitive values (raw tokens, full claims).

### 7.8 Error Mapping
- Service vs client vs UI-required vs network vs broker-specific.
- Preserve correlation_id in each mapped exception.
- Don’t collapse distinct protocol errors into generic messages (hurts remediation).

### 7.9 High-Impact Risk Areas
- Logging tokens / claims
- Missing signature validation of broker package
- Insecure redirect verification enabling token interception
- Cache race conditions (partial writes)
- Authority spoof acceptance
- Dropped correlation_id across layer boundaries

---

## 8. Security (Umbrella)

Flag:
- Secrets/tokens/claims/keys output in logs, telemetry, or exceptions.
- Deprecated or weak crypto (MD5/SHA1/AES-ECB/static IV).
- Authority or redirect URI not validated before usage.
- Broker IPC trust without signature/package verification.
- Feature flag defaulting to insecure path.

Severity:
- Use `Severity: High –` for exploitable vulnerabilities, secret leaks, auth bypass, token integrity compromise.
- Use `Severity: Medium –` for logic errors exposing partial risk or performance-impacting mis-secure flows.

### 8.1 Cryptography & Key Management
- Ensure secure RNG (`SecureRandom`).
- No static IV/nonce (AES-GCM).
- Avoid logging key material.
- Validate KeyStore retrieval error paths (fail safe).

### 8.2 Privacy & Logging
Never include:
- Raw access/refresh tokens
- ID token claims (UPN, emails)
- Full device identifiers (unless pseudonymized)
  Use hashing/pseudonymization where telemetry correlation needed.

### 8.3 Input Validation
- Validate authority format (https scheme, expected host pattern).
- Validate scopes (non-empty, deduplicated).
- Validate redirect URI exact match to config (no substring leniency).

---

## 9. Concurrency & Thread Safety

Flag:
- Unsynchronized shared mutable cache or configuration objects.
- Double-checked locking missing `volatile`.
- Blocking network/disk calls on main thread (ANR risk).
- Unbounded coroutine/job launches (no throttling or structured scope).
- `GlobalScope` usage (prefer lifecycle or injected scopes).
- Swallowed `CancellationException`.

Fix patterns:
- Use `mutex.withLock { }` or `synchronized(lock)` for compound operations.
- Use immutability (final/val) for configuration snapshots.
- Thread annotations (`@MainThread`, `@WorkerThread`) for clarifying call expectations.

Skip:
- Legit single-thread confined objects.
- Generated code with explicit concurrency wrappers.

---

## 10. Code Correctness & Business Logic

Common pitfalls:
- Silent flow uses expired AT without refresh.
- FOCI fallback incorrectly prioritized (e.g., not attempting family RT).
- Authority canonicalization omitted → duplicate entries.
- Over-broad `catch (Exception)` swallowing service error granularity.
- Nullability mishandled (platform types forced with `!!`).
- Returning internal mutable collections.

Immutability suggestions:
- Add `val`/`final` only when never reassigned (never produce `val final`).
- Don’t convert loop accumulators or builder patterns to immutable.

Annotations:
- Java: `@NonNull/@Nullable` on public surfaces where clarity needed.
- Kotlin: rely on type system; do NOT suggest `@NonNull`.

---

## 11. Performance (Focused Review)

Hot paths:
- Silent token acquisition loops
- Cache reads/writes
- JSON parsing of token responses
- Broker IPC parameter assembly

Flag only if:
- Complexity blow-up (O(N^2) over accounts/scopes)
- Repeated parser or regex reallocation
- Reflection in tight loops
- Unnecessary logging string concatenation in hot loop without guard
- Synchronous blocking I/O in token path

Memory:
- Large temporary buffers created per request (reuse or pool).
- Avoid keeping large token metadata objects alive longer than needed.

Skip:
- Single-run initialization logic
- Cold admin/diagnostic paths

---

## 12. Telemetry & Observability

Rules:
- Span names & attribute keys from enums (Common).
- Correlation ID attribute set at span inception.
- No sensitive values (token strings, raw claims).
- End spans in `finally`; set `StatusCode.OK` or `ERROR` before return or rethrow.
- Avoid micro-spans (trivial methods).

Anti-pattern examples:
```java
span.setAttribute("ipcStrategy", strategyName); // Inline string (disallowed)
span.makeCurrent(); // Use SpanExtension.makeCurrentSpan(...)
```

Adding new telemetry:
1. Confirm no existing enum covers it.
2. Add enum constant with classification & doc.
3. Use low cardinality value set.
4. Provide numeric attributes (counts, durations) rather than JSON blobs.

---

## 13. Testing

Flag absence of tests when:
- New branching logic (positive & negative path untested).
- New fallback chain (silent→broker→interactive).
- Error mapping introduced (service→UI-required).
- Telemetry addition (span & critical attributes).
- Cache mutation/eviction logic changed.
- Concurrency primitive introduced (Mutex/atomic).
- Bug fix lacks regression reproduction test.

Patterns:
- Use fake clock for expiration boundaries.
- Avoid `Thread.sleep`; use coroutine test utilities or latches.
- Deterministic RNG injection for crypto flows.
- Regression test must fail pre-fix & pass post-fix.

Avoid:
- Over-mocking entire pipeline (brittle).
- Testing only logs for logic correctness (unless log semantics contractual).
- Flaky timing assertions without synchronization.

---

## 14. Documentation

Request doc changes only if:
- Public surface changed semantics (params, return behavior).
- Threading / lifecycle side-effects unclear.
- Complex flow (interactive-chaining, broker fallback) lacks summary.
- Existing doc outdated relative to new logic.

Method:
- Quote first line: `Existing doc: "Fetches token silently."`
- Specify missing: expiration logic, fallback triggers, error propagation.
  Avoid generic “Add proper documentation.”

Skip trivial getters/data classes with clear property naming.

---

## 15. License Headers

For new Java/Kotlin source files: ensure standard license header present & correct. Do not paste full text in review comment; just flag absence.

---

## 16. Public API Stability

Flag:
- Signature changes (param removal, type change, reordering) in public classes.
- Visibility reductions (public→internal/private).
- Semantics changes (e.g., default authority resolution mutated).
- Removal of deprecated APIs before declared timeline.

Require:
- PR summary migration notes.
- Changelog entry (MAJOR or MINOR).
- Deprecation annotation + replacement guidance before removal.

Skip:
- Private/internal refactors.
- Implementation-only changes not altering public contract.

---

## 17. Dependency & Version Changes

Flag:
- Downgrade of security-critical libs (OkHttp, crypto stack).
- Major version upgrade lacking release notes reference.
- Wildcard dependency ranges (`1.+`).
- Added dependency causing method count surge (DEX limit risk).
- Transitive duplicates (e.g., multiple telemetry versions).

Recommendations:
- Summarize upgrade impact (“TLS defaults changed—verify custom cipher list.”).
- Suggest BOM usage for alignment.

---

## 18. Resource & Lifecycle Management

Flag:
- Activity/Fragment context stored in static singletons.
- WebView not destroyed on Activity finish.
- Streams/cursors not closed (missing try-with-resources / `use {}`).
- Long-lived coroutines not canceled on lifecycle end.
- Leaked callbacks referencing views beyond lifecycle.

---

## 19. Kotlin–Java Interop & Nullability

Guidance:
- Avoid `!!`; prefer safe calls + early return.
- Explicit overload if Kotlin default params cause Java ambiguity.
- Value classes/sealed types for domain IDs (avoid string confusion).
- Defensive copies for mutable collection exposures.
- Do not recommend adding Java nullability annotations to Kotlin declarations.

---

## 20. Comment Quality & Patch Guidelines (Appendix)

Checklist per comment:
- Targeted to changed code.
- Issue + Impact + Recommendation.
- Severity prefix for High severity.
- Minimal speculation; assumptions clarified.

Patch Safety Checklist:
- Compiles.
- Preserves nullability & synchronization.
- No secret exposure.
- Maintains canonical generation patterns.
- Telemetry span semantics intact.

If any fail → conceptual description, not patch.

Examples:
Security:
```
Severity: High – Raw access token logged.
Issue: Token printed in catch block.
Impact: Credential leakage to log aggregation.
Recommendation: Remove token or replace with hashed form (SHA-256 first 8 chars).
```
Concurrency:
“Race: double-checked lazy init missing volatile; add @Volatile or use synchronized holder.”
Performance:
“Repeated regex compile in loop; precompile Pattern once.”

Invalid Suggestion (Suppress):
“Change to val final authResult” (mixed keywords / non-sensical).

---

## 21. Miscellaneous Guidelines

Use industry security, performance, correctness standards beyond enumerated items.  
Do NOT:
- Flag unchanged legacy absent direct interaction risk.
- Demand broad refactors beyond PR scope unless severe security/correctness issue.
- Contradict established repository style.
- Mix Kotlin + Java keywords.

---

## 22. Quick Security Hardening Checklist (Selective Use)

- No token/claim logging
- Authority validated & canonical
- Broker signature verified
- Correlation ID propagated
- Atomic multi-artifact cache writes
- SecureRandom for crypto operations
- Span ends in finally
- Telemetry keys from enums only
- No static IV/nonce reuse

Apply only if relevant to diff (avoid noise).

---

## 23. Glossary

| Term | Definition |
|------|------------|
| FOCI | Family refresh token shared across related apps |
| Canonical Authority | Normalized host + tenant string used for cache/keying |
| Correlation ID | GUID tracking a request across layers for diagnostics |
| Atomic Update | Single operation writing all token artifacts consistently |
| TOCTOU | Time-of-check to time-of-use race window |
| Open Redirect | Redirect vulnerability from weak validation |
| Inline Key | Hard-coded telemetry key not from enums (disallowed) |

---

## 24. What NOT To Do (Recap)

- Do not refactor golden examples away from mandated patterns without validated defect.
- Do not recommend deprecated API.
- Do not inline telemetry keys.
- Do not encode manifest signature hash.
- Do not produce mixed-language keywords (e.g., `val final`).
- Do not remove broker integration configuration.
- Do not reduce security validation for brevity.

---

Thank you for contributing to MSAL for Android!