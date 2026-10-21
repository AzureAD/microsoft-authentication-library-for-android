<!-- Copyright (c) Microsoft Corporation. All rights reserved.
     Licensed under the MIT License. -->
# Experimental MFA sample

A dedicated, debug-only Android XML/ViewModel sample for the separate experimental
`:mfa` library. It demonstrates work/school activation enrollment, restored
accounts, pending sign-ins, number matching, approval, and denial. It does **not**
use MSAL OAuth login, a public-client application, a browser login, or a client ID.
No MFA API is being added to an already released MSAL binary.

| Variant | Default application ID | Behavior |
| --- | --- | --- |
| Mock Debug | `com.microsoft.identity.mfa.sample.mock` | Synthetic token and debug fixtures; no Firebase setup, tenant, or network service required |
| Live Debug | `com.microsoft.identity.mfa.sample.live` | Explicitly opts into the experimental service; real Firebase token; no mock controls or fallback |

The namespace is `com.microsoft.identity.mfa.sample`. Distinct app labels and
application IDs let both modes coexist with isolated local state. The sample uses
`project(':mfa')`, selecting the library's `local` flavor. Sample release variants
are disabled; debug fixture support must not be shipped in a production app.
The library is an ordinary Android library, not a fat AAR; consumers must resolve
its declared public dependencies. Publication coordinates and API stability are
not established by this PoC.

## Build and install

Use the repository's existing Android SDK, JDK, Gradle wrapper, and approved
dependency-feed setup. These PowerShell commands run from the MSAL repository
root, not the sample directory:

```powershell
.\gradlew.bat :testapps:mfa:assembleMockDebug :testapps:mfa:assembleLiveDebug
.\gradlew.bat :testapps:mfa:installMockDebug
.\gradlew.bat :testapps:mfa:installLiveDebug

# Discover actual generated APK filenames rather than assuming an output name.
Get-ChildItem .\testapps\mfa\build\outputs\apk -Recurse -Filter *.apk

adb shell am start -n com.microsoft.identity.mfa.sample.mock/com.microsoft.identity.mfa.sample.MainActivity
adb shell am start -n com.microsoft.identity.mfa.sample.live/com.microsoft.identity.mfa.sample.MainActivity
```

Install tasks require a connected authorized emulator/device. Mock requires no
Google services configuration. Live also **compiles without configuration** and
shows an actionable **Firebase setup required** message on opening. Network feed
access is a build prerequisite, not a reason to change repository security policy.

### Existing outer-workspace alternative

When this repository is part of the existing Android multi-repository workspace,
its root build registers this app as `:mfaSample` rather than `:testapps:mfa`.
From that workspace root, use the already-configured dependency feeds:

```powershell
.\gradlew.bat :mfaSample:assembleMockDebug :mfaSample:assembleLiveDebug -PincludeAuthenticatorApp=false
.\gradlew.bat :mfaSample:installMockDebug -PincludeAuthenticatorApp=false
.\gradlew.bat :mfaSample:installLiveDebug -PincludeAuthenticatorApp=false
Get-ChildItem .\msal\testapps\mfa\build\outputs\apk -Recurse -Filter *.apk
```

This is an alternative existing build context, not a repository/feed-security
workaround. If the standalone build receives an approved-feed authentication
error, fix access through the normal process or use the existing configured
workspace; do not weaken repositories or commit credentials.

### Implementation verification

The outer-workspace build produced both sample APKs and the library's local
debug/release AARs. Eight compact JVM smoke tests passed. The release AAR contains
neither debug fixture classes nor Firebase classes. Firebase BoM `35.0.2` resolved
Messaging `26.0.0` through the existing feed.

Generated artifacts relative to the outer workspace:

```text
msal\mfa\build\outputs\aar\mfa-local-debug.aar
msal\mfa\build\outputs\aar\mfa-local-release.aar
msal\testapps\mfa\build\outputs\apk\mock\debug\mfaSample-mock-debug.apk
msal\testapps\mfa\build\outputs\apk\live\debug\mfaSample-live-debug.apk
```

On an Android emulator, Mock completed QR and manual enrollment with a separate
validation injection, retained registration after process restart, approved a
number-matching request, denied a separate request, and surfaced
`SERVICE_REJECTED` for an incorrect number. Live opened without configuration and
showed Firebase setup required with token readiness false.

Follow-up testing with an authorized physical-device setup verified Firebase token
acquisition, live enrollment/confirmation, persisted registration, and pushed
number-matching challenge presentation. The tester also confirmed successful live
denial and browser completion after number-matching approval. Approval used the
explicitly selected Authenticator policy profile. This establishes interoperability
only for the exercised authorized identity/setup, not other app identities or policies.

### Approval policy limits

Sessions expose `canApprove` and immutable `approvalBlockedReasons`. The sample shows
fixed explanations beside a blocked request and disables Approve; the SDK also
enforces the block independently of the UI. Deny remains available.

The independent credential-policy reader permits recognized enabled MFA push/any
policies, including number matching when the challenge provides the required data.
The default `CONSERVATIVE` profile requires both primary and companion policies
to permit the decision. An explicitly authorized Authenticator-identity experiment
can select `AUTHENTICATOR` using the local build property
`-PmfaSamplePolicyProfile=AUTHENTICATOR`. That profile requires the matching
application ID and evaluates the primary policy, not requirements belonging to
companion applications. Companion data is still structurally validated; platform
and challenge-level requirements remain enforced. The selected profile is shown
in the app. Profile selection and package-name matching do not grant service
authorization or replace the requirement for an authorized package/signing setup.
It does not implement app lock/device verification, approval PIN, required app or
location context, location collection, silent approval, or additional admin policies.
Unknown fields/enums, unresolved policy defaults, ambiguous/malformed input, and
oversized policies are blocked rather than treated as permission to approve.
Policy payloads and the expected browser number are never exposed through this API
or diagnostic output. A supported policy is not proof the service accepted a decision.

## Mock walkthrough

1. Open **MFA PoC - Mock**. Confirm the visible `MOCK` mode and synthetic push-token
   readiness. The initial state has no registered account on a fresh install.
2. Select **HAPPY_PATH**, choose **Load example QR**, then **Register QR text**.
   Inputs clear immediately. At `AWAITING_VALIDATION`, click **Inject enrollment
   validation**. That button remains usable while registration waits. Expect
   `CONFIRMING`, `REGISTERED`, and an account row.
3. Close and reopen the app, then **Refresh accounts / pull requests** to check
   restored registration. The shared manager survives Activity recreation; the
   SDK owns persisted state.
4. Click **Inject sign-in challenge**. Read the number in the **Simulated browser
   panel**, type it into the request, and **Approve**. Expect `APPROVED` and request
   removal. The number is fixture UI data, not an answer exposed by a session.
5. Inject a new request and **Deny**. Expect `DENIED`; do not reuse an approved or
   denied request. Duplicate push/pull results are deduplicated by request GUID.
6. **Reset Mock state**, select **HAPPY_PATH**, load the example manual input, and
   register with **Register manual activation**, injecting validation as above.
7. On fresh/reset fixture state, exercise **REJECTED_ACTIVATION**,
   **TRANSPORT_FAILURE**, and **MALFORMED_RESPONSE**. Check safe failure codes
   instead of successful enrollment. After enrollment, exercise **WRONG_NUMBER**
   and **EXPIRED_REQUEST** with fresh challenges; no confirmed success should be
   shown. **WRONG_NUMBER** deliberately shows `41` in the simulated browser
   panel; enter that number to exercise rejection, rather than the happy-path
   number `42`. For happy-path number-entry validation, also enter a wrong number
   yourself.
8. Rotate/background the Activity while waiting, return, and inject validation.
   Secret text fields are not restored; re-enter any unsubmitted input. Verify
   duplicate/double taps do not issue simultaneous UI decisions.

The Mock-only timeline displays the debug SDK's sanitized operation summaries
(latest 20), not arbitrary request/response bodies. Reset is not available in Live.
Scenario selection is disabled while a UI operation is active. Wait for the
operation's SDK timeout/error before resetting an interrupted enrollment.

## Live setup (local configuration only)

1. Obtain an explicitly authorized SAS test setup and Android package/signing
   identity. A Firebase project, package name, debug signature, or token alone
   does not confer permission to enroll or guarantee SAS can send to that token.
2. Configure the authorized Android app in the approved Firebase project. Place
   its downloaded client configuration only at
   `testapps\mfa\src\live\google-services.json`. This file is locally ignored and
   must not be committed. The Google services plugin is applied conditionally
   when that file exists; no configuration is required for Mock or compilation.
   No service-account keys or message-sending credentials belong in the APK.
3. If an authorized setup requires a different dedicated test application ID,
   supply a local property when building/installing:

   ```powershell
   .\gradlew.bat :testapps:mfa:installLiveDebug "-PmfaSampleApplicationId=your.authorized.test.identity"
   ```

   The placeholder is not an approved identity. Use only an identity explicitly
   provisioned for this experiment, with matching Firebase package configuration
   and authorized signing. Do not copy or impersonate Authenticator's identity,
   Firebase project, or signing material. The override affects Live only.
4. Install Live on a dedicated test device with Google Play services. Open it and
   use **Allow visible notifications** on Android 13+ if desired. On earlier
   Android versions, or if notifications/channels were disabled in system
   settings, manage those settings separately.
5. Confirm `TOKEN_ACQUIRED` and SDK **Token ready: true**. The sample obtains
   `FirebaseMessaging.getInstance().token` and forwards every `onNewToken`
   callback to the same manager, including identical-token callbacks. Retry
   push setup after transient acquisition errors. Tokens are never displayed.

Open integration questions must be resolved with the authorized service owner:

| Question | Why it matters |
| --- | --- |
| Which SAS environment, test tenant, and work/school account support this experiment? | Determines service acceptance and supported activation endpoints |
| Which Android package and signing identity are permitted? | Dedicated app metadata is not automatically authorized |
| Which Firebase project/sender is routed by SAS? | A valid client token can still be unreachable by the service |
| How are fresh QR/manual activation inputs generated? | Activation inputs are scoped and consumable |
| How are real number-matching and denial challenges triggered? | The result must be independently observed in the browser/service |
| What data-message or notification-plus-data payload contract is used? | Affects validation delivery and tap behavior |
| Does the test profile require unsupported app-lock, PIN, location, or credential policies? | Unsupported policies must fail, not be bypassed |

Configure real inputs locally. Do not place activation codes, tokens, account
identifiers, payload captures, signing keys, or Firebase/service credentials in
issues, screenshots, source fixtures, logs, or chat.

## Live enrollment and decisions

1. Keep Live foregrounded for the first enrollment. Paste a fresh authorized QR
   text or enter its activation URL/code, then register. Both forms clear on
   submit and when leaving the foreground; they are excluded from saved state
   and autofill. Camera scanning is not implemented.
2. Observe `ACTIVATING` → `AWAITING_VALIDATION` → `CONFIRMING` → `REGISTERED`.
   Some short-lived stages may pass too quickly to read. Verify enrollment
   independently in the supported browser/service flow.
3. If waiting persists, inspect the displayed safe stage/error and confirm sender
   routing, authorized identity, and validation delivery. **Do not use Mock as
   evidence of Live success.** Refresh recovers pending sign-ins; it is not a
   guarantee that a missing enrollment validation can be recovered by polling.
4. Relaunch and refresh accounts. Trigger an external test sign-in. If no push
   appears, **Refresh accounts / pull requests**. Enter the number shown by the
   real browser and approve; verify browser completion. Trigger a separate
   request, deny it, and independently verify rejection.
5. Exercise incorrect numbers, expiry, interrupted connectivity, duplicate
   delivery, and rotation/recreation. Only SDK-confirmed decisions are shown as
   `APPROVED`/`DENIED`. `OUTCOME_UNKNOWN` is not failure or success confirmation:
   inspect the external outcome and start a fresh challenge rather than
   submitting the opposite decision.
6. A changed FCM token during or after enrollment yields explicit unsupported
   token-change status. The sample never silently overwrites the enrolled token,
   clears accounts, or resets Live. Coordinate authorized server cleanup and a
   fresh enrollment. Clearing app data/uninstalling is **not deregistration**.

## Push behavior, privacy, and limits

- Firebase Messaging lives only in Live sources/dependencies; Analytics is not
  included. There is one application-owned manager/environment, not one per
  Activity or callback.
- Service data callbacks and notification-tap string extras use the same
  dispatcher. Validation is independent of the screen's busy flag. The service
  is not exported. No lock-screen decision action or automatic approval exists.
- Callback work uses a managed application scope, at most eight concurrent
  callbacks, and a 30-second per-callback deadline. Overload/timeouts are
  surfaced as retry/refresh notices. This is **not** durable background work:
  Android may kill the process after a callback returns. Keep enrollment
  foregrounded; reopen/retry setup and pull pending sign-ins for recovery.
- Live creates the `mfa_requests` notification channel. For handled data
  challenges it can post a generic notification containing no account or number;
  taps open the screen. Background notification-plus-data messages may instead
  be displayed by Firebase, with data handled when tapped. Configure sender
  notification text without personal data. Channel/system permission settings
  still apply.
- Denying Android 13+ notification permission disables visible notifications,
  **not** Firebase token acquisition and not necessarily all data delivery.
  No delivery claim is made from permission status or token readiness.
- Account/session names are displayed only in the foreground UI; no sample
  logging or analytics is performed. Screen capture is disabled and backup is
  disabled. Errors show bounded SDK codes, never arbitrary exception messages,
  raw payloads, activation material, or real tokens.
- Sessions are deduplicated and capped at 32 in-memory prompts. Pull again as
  needed. Pending prompts are not saved by the UI across process death; the SDK
  and service remain authoritative.
- Deferred: production readiness, reliable background delivery/force-stop
  recovery, deregistration/migration, service-side token rotation, MSA, TOTP UI,
  signed-in enrollment, advanced policy enforcement, all-cloud interoperability,
  publishing, and complete Authenticator parity.

Build success verifies packaging, Mock verifies fixture behavior, and a Firebase
token verifies only client setup. Only independently observed Live enrollment
and browser decisions establish interoperability for the specific exercised
authorized setup. The limited checks above do not establish that every manual
scenario has passed or that Live service interoperability works.
