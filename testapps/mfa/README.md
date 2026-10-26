<!-- Copyright (c) Microsoft Corporation. All rights reserved.
     Licensed under the MIT License. -->
# Experimental MFA sample

A dedicated, debug-only Android XML/ViewModel sample for the separate experimental
`:mfa` library. It demonstrates work/school activation enrollment, restored
accounts, pending sign-ins, number matching, approval, and denial. It does **not**
use MSAL OAuth login, a public-client application, or a client ID. The optional
real-page demos below let the user sign in normally in Chrome or a separate WebView.
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

## SDK-owned handoffs on real registration pages

These **separate** real-page demos use `MfaEnrollmentHandoff` in the MFA library.
They do not use the controlled-page server, opaque page tickets, QR image import,
or native prestaging. Both entry points feed the **same application-owned
coordinator** and require customer-owned native confirmation before any service
call. They reuse the manager's real `registerAuthMethod`, activation parser,
endpoint policy, validation push, encrypted state and confirmation implementation.
This is feasibility work, not a supported production provisioning contract.

### Actual public SDK API

The APIs are under `com.microsoft.identity.mfa`:

```kotlin
MfaEnrollmentHandoff(
    manager: MfaSdkManager,
    scope: CoroutineScope,
    pendingLifetimeMillis: Long = 120_000
)
// Properties/methods:
val state: StateFlow<MfaHandoffState>
fun stage(activationLink: String): MfaHandoffReceipt
fun confirm(requestId: String): Boolean
fun cancel(requestId: String): Boolean
fun acknowledge(requestId: String): Boolean
fun close()

MfaHandoffIntentAdapter(handoff: MfaEnrollmentHandoff, callbackUri: String)
fun receive(intent: Intent?): MfaHandoffReceipt
// MfaHandoffIntentAdapter.EXTRA_ACTIVATION_LINK:
// "com.microsoft.identity.mfa.ACTIVATION_LINK"

MfaHandoffWebViewConnector(
    handoff: MfaEnrollmentHandoff,
    allowedHttpsOrigins: Set<String>,
    onReceipt: (MfaHandoffReceipt) -> Unit = {}
)
fun attach(webView: WebView) // UI thread, before navigation
fun close()                // UI thread, before WebView destruction
// MfaHandoffWebViewConnector.JAVASCRIPT_OBJECT: "mfaEnrollment"
```

`MfaHandoffState` contains only `phase`, nullable SDK-generated `requestId`,
nullable `source` (`DIRECT`, `BROWSER`, `WEBVIEW`), and nullable `errorCode`.
Phases are `IDLE`, `AWAITING_CONFIRMATION`, `SUBMITTING`, `SUCCEEDED`, `FAILED`,
`CANCELLED`, `EXPIRED`, `OUTCOME_UNKNOWN`, and `DISPOSED`. The request ID is an
opaque correlation handle, not a ticket/activation credential. No account names,
activation URLs/codes or arbitrary exception text appear in these snapshots.

`MfaHandoffReceipt` contains `disposition`, nullable `requestId` (on `STAGED`),
and nullable SDK `errorCode`. Dispositions are `STAGED`, `DUPLICATE`, `BUSY`,
`IGNORED`, `REJECTED`, `CLOSED`, and `LIMIT_REACHED`. Receiving another input cannot
overwrite an awaiting/submitting request. A receipt is never enrollment success.

### Minimal customer wiring: configure once, then render your UX

```kotlin
// Application/session ownership; do not capture Activity/WebView in this scope.
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
val manager = MfaSdkManager.create(application, enableExperimentalService = true)
val enrollment = MfaEnrollmentHandoff(manager, scope)
val intents = MfaHandoffIntentAdapter(enrollment, "mfa-poc-live://activate")

// Host Activity: forward onCreate (including recreation) AND onNewIntent.
// Preserve your existing unrelated-intent handling when receipt is IGNORED.
val receipt = intents.receive(incomingIntent)
if (receipt.disposition != MfaHandoffDisposition.IGNORED) {
    incomingIntent.data = null
    incomingIntent.replaceExtras(null as Bundle?)
    incomingIntent.action = null
    setIntent(Intent())
}

// Host owns the existing WebView, lifecycle, navigation, UI and exact allowed origins.
val connector = MfaHandoffWebViewConnector(
    enrollment, setOf("https://mysignins.microsoft.com")
) { receipt -> showSafeInputReceipt(receipt) } // your UX; no raw inputs
try {
    connector.attach(webView)
} catch (error: MfaSdkException) {
    showSafeError(error.code) // including WEBVIEW_UNSUPPORTED; never weaken the checks
}

lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        enrollment.state.collect { state -> renderYourEnrollmentUi(state) }
    }
}
// Your explicit native button handlers, using the currently displayed request's ID:
enrollment.confirm(displayedRequestId)
enrollment.cancel(displayedRequestId)      // only unsubmitted requests can cancel
enrollment.acknowledge(displayedRequestId) // dismiss matching terminal observation

// When the host destroys its WebView:
connector.close()
// Do NOT close the coordinator/cancel its scope when hiding UI or closing a page.
// At session disposal:
enrollment.close() // pending input discarded; submitted operation continues
```

The `showSafeInputReceipt`, `showSafeError` and `renderYourEnrollmentUi` functions
above are customer presentation hooks, not SDK methods. The sample implementations
are `MfaSampleViewModel.portalReceipt`, `MainActivity.openPortal` and
`PortalEnrollmentUi`. No URI parser, origin validator or enrollment state machine
needs copying into the host.

Use one coordinator per manager/session and keep its scope alive through any
submission. The host still initializes push using the existing manager APIs.
The sample gates Continue on token readiness, no token-change block, no other
native enrollment and no active controlled-page request. Receiving a link alone
never calls the service. SDK validation remains independent of sample UI checks.

### Intent and WebView message contracts

For this sample, the exact callback is `mfa-poc-live://activate`, distinct from
the old controlled `/enroll/<id>` route. The SDK verifies action `ACTION_VIEW`,
absence of a selector, exact callback shape/equality, and a bounded **String
extra**, then validates the HTTPS activation link. The payload is not accepted
from callback query parameters. The maximum activation-link size is 8192 UTF-8
bytes. The host declares its own callback filter; the sample does not claim
Microsoft's activation schemes or verified App Link/browser authentication.

The user-clicked browser anchor follows this shape (placeholders only):

```text
intent://activate#Intent;scheme=mfa-poc-live;package=<installed-authorized-package>;S.com.microsoft.identity.mfa.ACTIVATION_LINK=<encodeURIComponent(existing-HTTPS-pairing-link)>;end
```

An origin-allowed page in a connected WebView sends this JSON **string**:

```javascript
window.mfaEnrollment.postMessage(JSON.stringify({
    version: 1, type: "activation", activationLink: existingHttpsPairingLink
}));
```

The SDK checks WebKit's actual `sourceOrigin` and `isMainFrame` before parsing;
a self-declared origin is neither accepted nor trusted. Messages are limited to
16384 UTF-8 bytes with exactly the three named fields, correct primitive types
and no duplicates/trailing content. No wildcard origins, subframe acceptance,
unrestricted `addJavascriptInterface`, UI, page navigation or site-specific DOM
extraction is added to the SDK. `WEB_MESSAGE_LISTENER` must be available, otherwise
`attach` throws `WEBVIEW_UNSUPPORTED`. The library uses the repository's existing
AndroidX WebKit `1.14.0` pin, without upgrading unrelated dependencies.

### Shared demo-only page extraction

`src\main\assets\portal-enrollment-demo.js` supplies both demonstrations. It adds
**Set up using custom app** only in the top-level `https://mysignins.microsoft.com`
document and observes SPA changes. It does not change or click Microsoft's
original pairing anchor, decode QR pixels, press Next, or manufacture portal
success. Its click handler reads the **current**, connected, visible **Pair your
account** anchor, requiring exactly one candidate and the supported HTTPS
`login.microsoftonline.com/authenticatorApp/activateAccount` link with exactly
`accountType=mfa`, `code`, and `url`. Missing/ambiguous/stale/unsupported input
is rejected; “different authenticator” Account name/Secret key (TOTP) is not this
push enrollment flow.

Demo extraction is intentionally site/wording-specific (English verified wording).
It is not an SDK DOM contract. The SDK independently validates received input.
The injected panel emits no account/activation data to console, new URL query
parameters, or diagnostic output. One click is allowed per installation of the
panel; after cancellation/failure/uncertainty, obtain fresh input rather than
trying to replay the same request.

### Chrome: inject into the original visible tab over USB

1. Complete the authorized identity/Firebase setup above. Open the actual
   registration flow in Chrome and reach the **Pair your account** step. Keep
   that tab visible and open. Do not prepare native QR/manual fields.
2. With installed adb, authorized USB debugging, and Node with its built-in
   WebSocket (Node 22.4+), run from this repository root:

   ```powershell
   node .\testapps\mfa\tools\inject-portal-demo.mjs `
     --serial "<USB_DEVICE_SERIAL>" `
     --package "<INSTALLED_AUTHORIZED_APP_PACKAGE>" `
     --adb "<PATH_TO_ADB_EXECUTABLE>"
   ```

   `--adb` defaults to installed `adb`; omit `--serial` only when adb can select
   the single intended device. Optional `--socket` defaults to
   `chrome_devtools_remote`. The package is required and has no authorized-app
   identity default. No Node dependencies or installations are used.
3. Tap **Set up using custom app**, then review native confirmation and select
   **Continue** or **Cancel**. Keep the app foregrounded during real enrollment.
4. After this request succeeds, close the native result and use Android
   Back/app switching to the **ORIGINAL Chrome tab**. The app does not reopen
   the portal or promise original-tab focus. Use the website's normal Next and
   independent verification; trigger/approve any real sign-in challenge normally.

The helper only injects the shared demo asset into exactly one visible supported
top-level page. It never activates a background tab. CDP evaluations return only
booleans, not DOM dumps, titles, URLs, QR contents, cookies or tokens. It uses an
OS-assigned adb forward, a 60-second abort deadline and bounded subprocess/CDP
calls, and removes **only its own** forward in `finally`. A forced process kill
can prevent cleanup; normal errors and signals go through cleanup. The observer
persists for the current document; full reload/navigation requires reinjection.
Installing the button itself does not submit enrollment.

### Real-site WebView: keep the same document under native enrollment

1. Select **Open Entra in WebView**. It opens `https://entra.microsoft.com`;
   sign in normally and navigate to the account's security-info/Authenticator
   registration **Pair your account** step. This WebView has its own sign-in
   session: Chrome cookies are not imported, and login/policy restrictions are
   not bypassed.
2. The SDK connector is attached before navigation but exposed only to the
   configured registration origin. The sample injects the same asset after
   top-level navigation reaches that exact origin; SPA changes are observed.
3. Tap **Set up using custom app**, review native confirmation, and select
   **Continue** or **Cancel**. The confirmation/progress/result dialog is a
   native overlay above the **same existing WebView/document**. Enrollment
   does not close, reload, navigate or recreate it.
4. Close/hide the result to return to that page, then use its normal Next and
   verification. Back navigates WebView history; **Close page** closes it.
   Closing the page/overlay does not cancel an already submitted operation.
   **View enrollment status** can reopen the request-local native observation.
5. When a sign-in push arrives, the sample shows its existing number-entry and
   **Approve**/**Deny** controls in a native dialog above that same WebView.
   Use **Back to website** to read the number, then **Review sign-in** in the
   WebView's native controls to reopen the request. Enter the number yourself
   and choose a decision; the SDK's existing policy checks remain authoritative.
   The dialog shows the operation result. **Back to website** dismisses only
   that dialog, without reloading or closing the page or submitting a decision.

JavaScript and DOM storage are enabled for ordinary portal sign-in; file/content
access, mixed content, popups and geolocation are disabled. HTTPS navigation,
platform SSL validation and `FLAG_SECURE` remain enforced. The existing tightly
restricted loopback WebView is a separate component and is unchanged.

Native automation IDs (in the installed app's resource namespace):
`openEntraWebView`, `portalWebView`, `portalClosePage`, `portalEnrollmentStatus`,
`portalConfirm`, `portalCancelOrHide`, `portalStatus`, `showPortalStatus`,
`portalReviewSignIn`, `portalApprovalStatus`, and `portalBackToWebsite`.
The injected HTML control is `mfa-custom-app-setup`.

### Lifetime, cancellation and observation limitations

- Pending native input expires after two minutes by default; SDK-configurable
  range is 1–300000 milliseconds. Input references are dropped on consumption,
  cancellation, expiry or disposal. In-flight parameters live only for the call;
  managed strings cannot be reliably zeroed.
- Confirmation consumes the request once. Duplicate digests remain blocked for
  the coordinator session; after 64 accepted requests it fails closed with
  `LIMIT_REACHED` instead of accumulating unbounded replay state. A new
  coordinator is not service cleanup or authorization to reuse an activation.
- `SUCCEEDED` is published only after **this** real `registerAuthMethod` returns.
  Restored global `REGISTERED` status only refreshes the accounts UI. It does not
  complete a new handoff. Expected failures retain their `MfaErrorCode`;
  unexpected/interrupted outcomes are `OUTCOME_UNKNOWN`, never auto-retried.
- Scope cancellation propagates to coroutine work and preserves unknown-outcome
  observation. `close()` refuses further staging and discards unsubmitted input,
  but does not cancel submitted work. Hiding UI does neither.
- Activity recreation destroys its WebView (no credential-bearing saved WebView
  state); in-process native enrollment survives. Keeping the same document is
  guaranteed only across the enrollment overlay, not Activity/process death.
  Process death loses coordinator state and must not infer completion from
  restored accounts. No automatic replay or page restoration is performed.
- Every live enrollment, including switching browser/WebView demos, needs fresh
  authorized input. Token rotation, service cleanup, identity/signing permission,
  validation delivery, unsupported policy and background-execution constraints
  above remain unchanged.

### Lightweight automated checks

The SDK unit tests exercise accepted/invalid/duplicate/oversized inputs, exact
Intent contract, actual-source/subframe validation, strict message schema,
one-shot/cancel/expiry/disposal, bounded replay history, stale results, scope
cancellation and secret-free snapshots/errors. Android adapter plumbing is
compiled; actual WebKit messaging/navigation must still be verified on-device.
The Node tests use a small synthetic document model, not a fake service/browser.

```powershell
# Existing outer-workspace test/build context:
.\gradlew.bat :mfa:testLocalDebugUnitTest :mfaSample:testLiveDebugUnitTest `
  :mfaSample:assembleLiveDebug :mfa:assembleLocalRelease -PincludeAuthenticatorApp=false
# Source-only checks from this repository root; no device/browser operation:
node --test .\testapps\mfa\tools\portal-demo.test.mjs
node --check .\testapps\mfa\tools\inject-portal-demo.mjs
node .\testapps\mfa\tools\inject-portal-demo.mjs --help
```

Build/unit checks do **not** establish real-page acceptance. Independently verify
both real pages with fresh inputs, native confirmation, real service return,
unchanged page state and normal portal verification on the authorized device.

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
