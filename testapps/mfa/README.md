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
| Live Debug | `com.microsoft.identity.mfa.sample.live` | Explicitly opts into the experimental service; real Firebase token and service-confirmed enrollment and decisions |

The namespace is `com.microsoft.identity.mfa.sample`. The sample is live-only and
uses `project(':mfa')`, selecting the library's `local` flavor. Sample release variants
are disabled. Debug-only, non-PII diagnostics remain available.
The library is an ordinary Android library, not a fat AAR; consumers must resolve
its declared public dependencies. Publication coordinates and API stability are
not established by this PoC.

## Build and install

Use the repository's existing Android SDK, JDK, Gradle wrapper, and approved
dependency-feed setup. These PowerShell commands run from the MSAL repository
root, not the sample directory:

```powershell
.\gradlew.bat :testapps:mfa:assembleLiveDebug
.\gradlew.bat :testapps:mfa:installLiveDebug

# Discover actual generated APK filenames rather than assuming an output name.
Get-ChildItem .\testapps\mfa\build\outputs\apk -Recurse -Filter *.apk

adb shell am start -n com.microsoft.identity.mfa.sample.live/com.microsoft.identity.mfa.sample.MainActivity
```

Install tasks require a connected authorized emulator/device. Live
**compiles without configuration** and shows an actionable
**Firebase setup required** message on opening. Network feed
access is a build prerequisite, not a reason to change repository security policy.

### Existing outer-workspace alternative

When this repository is part of the existing Android multi-repository workspace,
its root build registers this app as `:mfaSample` rather than `:testapps:mfa`.
From that workspace root, use the already-configured dependency feeds:

```powershell
.\gradlew.bat :mfaSample:assembleLiveDebug -PincludeAuthenticatorApp=false
.\gradlew.bat :mfaSample:installLiveDebug -PincludeAuthenticatorApp=false
Get-ChildItem .\msal\testapps\mfa\build\outputs\apk -Recurse -Filter *.apk
```

This is an alternative existing build context, not a repository/feed-security
workaround. If the standalone build receives an approved-feed authentication
error, fix access through the normal process or use the existing configured
workspace; do not weaken repositories or commit credentials.

### Previous live-flow verification

Before the live-only cleanup, the outer-workspace build produced the Live sample
APK and the library's local debug/release AARs. Firebase BoM `35.0.2` resolved
Messaging `26.0.0` through the existing feed. Live opened without configuration
and showed Firebase setup required with token readiness false.

Generated artifacts relative to the outer workspace:

```text
msal\mfa\build\outputs\aar\mfa-local-debug.aar
msal\mfa\build\outputs\aar\mfa-local-release.aar
msal\testapps\mfa\build\outputs\apk\live\debug\mfaSample-live-debug.apk
```

Follow-up testing with an authorized physical-device setup verified Firebase token
acquisition, live enrollment/confirmation, persisted registration, and pushed
number-matching challenge presentation. The tester also confirmed successful live
denial and browser completion after number-matching approval. Approval used the
explicitly selected Authenticator policy profile. This establishes interoperability
only for the exercised authorized identity/setup, not other app identities or policies.

These are historical results, not validation of the live-only cleanup or the
controlled same-device page flows below. Independent
parser, endpoint-policy, proof, credential-policy, and diagnostic unit tests remain;
they do not establish live service interoperability.

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

## Live setup (local configuration only)

1. Obtain an explicitly authorized SAS test setup and Android package/signing
   identity. A Firebase project, package name, debug signature, or token alone
   does not confer permission to enroll or guarantee SAS can send to that token.
2. Configure the authorized Android app in the approved Firebase project. Place
   its downloaded client configuration only at
   `testapps\mfa\src\live\google-services.json`. This file is locally ignored and
   must not be committed. The Google services plugin is applied conditionally
   when that file exists; compilation does not require configuration.
   No service-account keys or message-sending credentials belong in the APK.
3. If an authorized setup requires a different dedicated test application ID,
   supply a local property when building/installing:

   ```powershell
   .\gradlew.bat :testapps:mfa:installLiveDebug "-PmfaSampleApplicationId=your.authorized.test.identity"
   ```

   The placeholder is not an approved identity. Use only an identity explicitly
   provisioned for this experiment, with matching Firebase package configuration
   and authorized signing. Do not copy or impersonate Authenticator's identity,
   Firebase project, or signing material. The override affects the Live application ID.
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
issues, screenshots, test inputs, logs, or chat.

## Live enrollment and decisions

1. Keep Live foregrounded for the first enrollment. Paste a fresh authorized QR
   text or enter its activation URL/code, then register. Both forms clear on
   submit and when leaving the foreground; they are excluded from saved state
   and autofill. Camera scanning is not implemented.
2. Observe `ACTIVATING` → `AWAITING_VALIDATION` → `CONFIRMING` → `REGISTERED`.
   Some short-lived stages may pass too quickly to read. Verify enrollment
   independently in the supported browser/service flow.
3. If waiting persists, inspect the displayed safe stage/error and confirm sender
   routing, authorized identity, and validation delivery. Refresh recovers pending
   sign-ins; it is not a guarantee that a missing enrollment validation can be
   recovered by polling.
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

## Controlled same-device setup pages (live enrollment only)

These are **controlled developer-demo pages**, not integration with a production
security-information portal. They host real UI locally; they do not simulate an
MFA service, generate activation inputs, or return a fabricated registration.
There is still only one `live` flavor. Direct QR/manual registration and pushed
challenge approval/denial remain available.

### Developer preparation for every test

Complete the authorized Live setup above and wait for `TOKEN_ACQUIRED`, **Token
ready: true**, and no token-change block. Obtain **fresh, unused real activation
input for EACH registration test**, including when switching between the two
page options. Do not reuse input from an earlier success, cancellation, failure,
or uncertain result. Do not automatically reset an existing registration to
repeat a demo; coordinate authorized service cleanup separately if needed.

In the native sample, paste either QR text alone, or enter both activation code
and URL with the QR field empty. Do **not** press a direct Register button when
testing a page flow. The selected Open setup action consumes and clears these
native fields, retaining the input only in process memory until native Continue,
Cancel, page expiry, or explicit clearing. This is developer preparation of the
controlled page, not page-supplied activation.

### Embedded page: four steps

1. After preparing fresh native input, select **Open setup in this app**.
2. On the controlled page, select **Set up on this device**.
3. Review **Confirm native live enrollment** and select **Continue** (or
   **Cancel**, which submits nothing). Keep the native app open while enrollment
   runs; do not repeat the action.
4. The same embedded page polls this request's safe status without reloading.
   Success means that this request's live `registerAuthMethod` call returned,
   including activation validation and confirmation. Independently verify the
   service outcome. Closing the page after submission does not undo or cancel
   enrollment; **Return to setup page** can reopen its status while available.

The embedded option requires Android 7/API 24+ and an available Android WebView.
Its exact expected launch navigation is accepted only from the current controlled
page, with a main-frame user gesture and a matching embedded request. There is no
JavaScript interface, injected script bridge, scraping, or production-page POST
replay. Unexpected navigation, file/content access, popups, and third-party
resources are blocked.

### External browser: four steps

1. After preparing **different fresh** native input, select **Open setup in
   browser**. Use an installed browser supporting user-clicked Android `intent:`
   links (for example Chrome); browser-specific restrictions still apply.
2. On the controlled page, select **Set up on this device**. Its package-targeted
   sample link opens native confirmation in this installed application.
3. Review **Confirm native live enrollment**, then **Continue** or **Cancel**.
   Keep the native app foregrounded while the real SDK operation runs.
4. Select **Return to setup page**, or manually resume the original browser tab.
   Both read the same request-specific status. A browser may open a new tab;
   this sample does **not** promise original-tab focus or production portal
   continuation. Independently verify the service outcome.

The browser link uses `mfa-poc-live://enroll/<random-request-id>` through an
`intent:` anchor targeted to the current application package, including an
explicitly authorized package override. It does not register Microsoft's
activation schemes and is **not a verified App Link** or browser-origin
authentication. An unguessable live request, source/canonical URI checks, and
explicit native confirmation are the demo's boundary. Foreign, malformed,
expired, duplicated, or already-used links never invoke enrollment; page intents
are handled separately from notification extras.

### Lifetime, outcomes, and privacy

- Exactly one page request can be prepared. Clear the current demo before a new
  one; a submitting SDK operation continues to block replacement even if its
  page is cleared. Existing account status and unrelated push notices never
  indicate page success. Only the matching SDK call's return does.
- A prepared request expires after five minutes; submission has a separate
  five-minute page-observation window. A terminal result remains available for
  two minutes. Expiring or clearing a submitting page **does not cancel, roll
  back, or replay** its SDK call. An expired observation is explicitly uncertain,
  not a claim of enrollment failure.
- Cancelling native confirmation or closing an embedded page before submission
  discards the staged input. Closing an external browser tab is not observable
  natively: use native **Clear setup page**, or let it expire.
- Rotation disposes Activity-owned dialogs/WebViews; in-process correlation and
  running native enrollment remain application-owned. Process death invalidates
  all page requests. Nothing is restored or automatically resubmitted. If a
  page becomes unavailable, check the native app/service before preparing fresh
  input. Android background execution is not guaranteed.
- The server binds only `127.0.0.1` on an OS-assigned port, with four concurrent
  sockets, an 8 KiB request-head limit, bounded reads, and connection-close
  responses. Only exact request page/status GET routes exist; there are no
  credential, mutation, filesystem, proxy, or MFA backend routes.
- HTML/status contain fixed messages and page correlation identifiers, never
  activation input, account names, tokens, SDK exceptions, or service payloads.
  There is no CORS grant, and foreign Origin/Host requests are rejected.
  Responses are no-store/no-referrer with a restrictive CSP and no third-party
  assets. URLs may remain in browser history but contain no activation values.
- The sample-only network-security configuration allows cleartext solely for
  `127.0.0.1`. System TLS trust and the SDK's HTTPS endpoint validation remain;
  no user-CA/proxy override or public domain is needed.
- This exercises real FCM, ActivateNew, validation, and ConfirmActivation only
  when the user explicitly continues with authorized fresh input. Verified
  domain links and production portal handoff/continuation remain out of scope.

### Focused checks

The pure session/URI/HTTP/page tests check one-shot consumption, duplicates,
foreign inputs, expiry, stale results, and native-input exclusion. They contain
no fake MFA backend or runtime fixture, and do not establish live interoperability.
From the existing outer-workspace build context:

```powershell
.\gradlew.bat :mfaSample:testLiveDebugUnitTest :mfaSample:assembleLiveDebug -PincludeAuthenticatorApp=false
```

For the standalone MSAL build, substitute `:testapps:mfa` for `:mfaSample`.
Build success is not a real-device/browser acceptance result. Exercise each path
separately with fresh input on the authorized device.

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
- Account/session names are displayed only in the foreground UI. Debug diagnostics
  emit bounded, non-PII lifecycle and SDK events; no analytics is performed.
  Screen capture is disabled and backup is disabled. Errors show bounded SDK codes,
  never arbitrary exception messages, raw payloads, activation material, or real tokens.
- Sessions are deduplicated and capped at 32 in-memory prompts. Pull again as
  needed. Pending prompts are not saved by the UI across process death; the SDK
  and service remain authoritative.
- Deferred: production readiness, reliable background delivery/force-stop
  recovery, deregistration/migration, service-side token rotation, MSA, TOTP UI,
  signed-in enrollment, advanced policy enforcement, all-cloud interoperability,
  publishing, and complete Authenticator parity.

Build success verifies packaging, and a Firebase token verifies only client setup.
Only independently observed Live enrollment and browser decisions establish
interoperability for the specific exercised
authorized setup. The limited checks above do not establish that every manual
scenario has passed or that Live service interoperability works.
