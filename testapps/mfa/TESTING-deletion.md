<!-- Copyright (c) Microsoft Corporation. All rights reserved.
     Licensed under the MIT License. -->
# Registration deletion PoC: setup and test steps

This is the experimental `:mfa` SDK and **LiveDebug** sample, not the integrated
public API proposal. Only use disposable registrations you own, in an authorized
test tenant. **Verify another working sign-in method before deleting anything.**
Deletion is permanent. It does not sign out MSAL/Native Auth, clear the OAuth
cache, remove other registrations, or undo a completed MFA approval.

## What this build can and cannot demonstrate

The sample starts without local configuration and explains what is missing.
Compilation and JVM tests do **not** establish live tenant/service compatibility.
The default package is `com.microsoft.identity.mfa.sample.live`, minimum Android
version **7.0 / API 24**, target API 35, compile API 36.

There are no fault-injection menus or token-validation bypasses in the APK.
The tables explicitly distinguish **DEVICE** tests from deterministic **JVM**
tests. Do not intercept real tokens to manufacture test errors.

## Local configuration (never commit these files)

### Push/enrollment

1. Follow [the existing live setup](README.md#live-setup-local-configuration-only).
2. Obtain a Firebase client configuration for **this authorized package/signing
   identity**, with supported service-to-Firebase routing. Put it at
   `testapps\mfa\src\live\google-services.json`.
3. Do not reuse a configuration belonging to a different app or change the
   package to impersonate it. A Firebase token alone does not authorize enrollment.
4. Use a Google Play services device and an authorized disposable workforce
   account. Fresh activation inputs and supported approval policy are required.

### Browser OAuth / Graph deletion

1. Have the registration owner configure an Entra **public client** for the
   sample's Android package and actual signing certificate. No client secret is
   needed or accepted. Do not change cloud registrations or grant consent as
   part of running the build.
2. Obtain the real `client_id`, workforce tenant GUID and Android redirect URI
   from that registration. Request the delegated Graph permission
   `UserAuthenticationMethod.ReadWrite` and have its consent/policy requirements
   approved through the tenant's normal process. This PoC requests
   `https://graph.microsoft.com/UserAuthenticationMethod.ReadWrite` and essential
   `amr=ngcmfa` claims; service support for that step-up must be verified.
3. Determine the public signing hash for the **same** debug key used by Gradle.
   For the standard local Android debug key, PowerShell:

   ```powershell
   $pem = & "$env:JAVA_HOME\bin\keytool.exe" -exportcert -rfc `
     -alias androiddebugkey -keystore "$env:USERPROFILE\.android\debug.keystore" `
     -storepass android
   $der = [Convert]::FromBase64String(($pem | Where-Object { $_ -notmatch '^-----' }) -join '')
   $hash = [Convert]::ToBase64String([Security.Cryptography.SHA1]::Create().ComputeHash($der))
   $redirect = 'msauth://com.microsoft.identity.mfa.sample.live/' + [uri]::EscapeDataString($hash)
   $hash
   $redirect
   ```

   These are public registration metadata, not a private signing key. For a
   different signing configuration use its certificate, not the default key.
4. Create the ignored file
   `testapps\mfa\src\live\res\raw\auth_config.json`. Replace every placeholder
   below with approved values; this is **not** a ready-to-use registration:

   ```json
   {
     "client_id": "<actual-client-guid>",
     "redirect_uri": "<encoded-msauth-redirect-for-this-package-and-signature>",
     "account_mode": "MULTIPLE",
     "authorization_user_agent": "BROWSER",
     "broker_redirect_uri_registered": false,
     "authorities": [{
       "type": "AAD",
       "audience": {
         "type": "AzureADMyOrg",
         "tenant_id": "<actual-workforce-tenant-guid>"
       }
     }]
   }
   ```

   **Intentional exception to normal broker guidance:** this approved PoC uses
   standard MSAL **browser-only** acquisition, not broker or Native Auth step-up.
   Runtime checks require that exact mode and a single public-cloud workforce
   tenant. Cross-tenant registrations need a separately configured build.
5. Rebuild. Gradle derives the unencoded manifest redirect path from this local
   JSON; runtime checks compare it against the installed package's signing hash.
   MSAL still performs its own normal initialization/authority/redirect checks.
6. Open the sample. Expect `DELETION CONFIGURED` and push `TOKEN_ACQUIRED`.
   Configuration success is local validation only; consent, service authorization
   and Graph operation support have not yet been proven.

## Build, install and launch

Run from the MSAL worktree root with JDK 17, Android SDK and approved package-feed
credentials. Preserve its pinned Common submodule. Do not modify feeds or paste
tokens into commands, files, issue reports or logs.

```powershell
.\gradlew.bat :mfa:testLocalDebugUnitTest :testapps:mfa:testLiveDebugUnitTest `
  :testapps:mfa:assembleLiveDebug
$apk = Get-ChildItem .\testapps\mfa\build\outputs\apk\live\debug\*.apk |
  Select-Object -First 1
Get-FileHash $apk.FullName -Algorithm SHA256
adb devices
adb install -r $apk.FullName
adb shell am start -n com.microsoft.identity.mfa.sample.live/com.microsoft.identity.mfa.sample.MainActivity
```

Use a supported installed browser such as Chrome with Custom Tabs. On Android
13+, tap **Allow visible notifications** and accept the OS prompt if you want
visible push notifications. Keep Google Play services enabled. Notification
permission is not proof of push delivery.

`install -r` preserves sample state only for a compatible package/signature.
Do **not** uninstall or clear app storage to repair a mismatch without first
recording which remote methods still exist.

### Existing local Gradle credential precedence

If the root build authenticates but subprojects receive feed 401s despite fresh
approved environment credentials, stale user Gradle properties can take
precedence in this PoC base. The verified local build used the following
**temporary, secret-free** initialization script; it changes no feed URLs or
access policy. Provision the two environment variables through approved local
authentication first. Do not print their values.

```powershell
New-Item -ItemType Directory -Force .\build | Out-Null
@'
gradle.projectsLoaded {
    gradle.rootProject.allprojects { p ->
        p.ext.vstsUsername = System.getenv('ENV_VSTS_MVN_CRED_USERNAME')
        p.ext.vstsMavenAccessToken = System.getenv('ENV_VSTS_MVN_CRED_ACCESSTOKEN')
    }
}
'@ | Set-Content .\build\transient-credentials.init.gradle
try {
    .\gradlew.bat -I build\transient-credentials.init.gradle `
      :mfa:testLocalDebugUnitTest :testapps:mfa:testLiveDebugUnitTest `
      :testapps:mfa:assembleLiveDebug :mfa:lintLocalDebug :testapps:mfa:lintLiveDebug
    if ($LASTEXITCODE -ne 0) { throw 'Validation failed; inspect the task output.' }
} finally {
    Remove-Item .\build\transient-credentials.init.gradle
}
```

## Standard deletion procedure

1. Enroll a disposable registration through the existing sample flow and verify
   it under **Accounts and pending requests**. In a separate browser, record the matching
   remote method in that same user's Security info.
2. Leave **Allow browser interaction if silent acquisition requires it** checked.
3. Tap **Delete registration…**, select the account/method (the last eight
   registration-ID characters distinguish similar labels), read the warning,
   and tap **Delete selected registration**.
4. Complete browser authentication using the **same tenant/user**. Do not choose
   another account as a workaround. If step-up targets this sample registration,
   review/approve the challenge normally and enter the browser's number.
5. Observe deletion status separately from push notices:
   `ACQUIRING_TOKEN` → `WAITING_FOR_APPROVAL` → `DELETING` → `SUCCEEDED`.
   Fast stages may not be visibly rendered. **Acquisition** retains
   `SILENT_TOKEN_RECEIVED` or `INTERACTIVE_TOKEN_RECEIVED`.
6. Refresh Security info independently; confirm the selected remote method is
   absent, then tap the sample's **Refresh accounts / pull requests**.
   Other registered methods must remain. Restart the sample and check again.

## DEVICE scenarios

Each destructive case requires a fresh disposable registration, or a different
remaining disposable method. Never use your only available sign-in method.

| Case / prerequisite | Exact actions | Expected UI and local/remote outcome |
|---|---|---|
| Setup absent or incompatible | Install unconfigured APK; open it | `DELETION SETUP REQUIRED`; Delete disabled. No OAuth or Graph calls. Missing Firebase separately shows its setup message. |
| Interactive success; no matching OAuth account cached | Follow standard procedure with interaction checked; sign in as the registered user and satisfy step-up | `INTERACTIVE_TOKEN_RECEIVED`, then `SUCCEEDED`; only selected method removed locally and remotely. |
| Legitimate silent success; compatible cached Graph token satisfying the same claims | After a prior successful browser flow, use another disposable method belonging to the same user/tenant; uncheck interaction and delete | If cache/service permits, no browser opens, `SILENT_TOKEN_RECEIVED`, `SUCCEEDED`. Essential claims may still require fresh interaction; that is not a defect. |
| Silent-to-interactive fallback | Use a fresh app OAuth cache on a separate configured test device, or a known UI-required cached-account condition; check interaction and delete | Silent attempt first; only missing account/UI-required opens browser. Complete it; selected method removed. Do not clear storage on a device holding live sample registrations. |
| Interaction disallowed | With no cached matching OAuth account or a known UI-required condition, uncheck interaction; confirm deletion | `INTERACTION_REQUIRED`, no browser; selected and other registrations unchanged locally/remotely. Check interaction and retry normally. |
| User dismisses browser | Start interactive deletion, then close browser using Back/cancel | `USER_CANCELLED` (or cooperative `CANCELLED`); no DELETE after the cancelled acquisition. Registration retained. Browser-host lifecycle behavior requires device verification. |
| User selects wrong account | In browser, explicitly sign in with another test user in the tenant | Token success for a different identity is rejected as `INVALID_TOKEN`; Graph is not called; selected registration retained. If Entra itself rejects the account, a provider failure is expected instead. No automatic account switching or sign-out. |
| Network unavailable before acquisition | Enable airplane mode before confirming Delete | If acquisition needs network, provider/network failure; no interactive fallback for a network error. A valid cached token can still reach the DELETE stage and produce `OUTCOME_UNKNOWN`; see JVM cases for precise stage isolation. |
| User cancels while acquiring | Start deletion; while browser is open, return via a supported split-screen arrangement and tap **Cancel deletion**; close any remaining browser UI | Local state retained. MSAL's void API cannot forcibly close its browser request; late callbacks are ignored. Another request can show `OPERATION_IN_PROGRESS` until that callback finishes. |
| Same-device step-up | Use supported split-screen with sample and browser, or supported notification/task navigation; start deletion, receive its MFA push, enter number and tap **Approve** | Approval controls/push handling remain available while token is acquired. Foreground sample polls every 3 seconds. DELETE waits for an already-running response on that registration. Browser navigation may cancel acquisition on some devices; record that limitation rather than bypassing MSAL. |
| Deny step-up / unsupported approval policy | Trigger same-device challenge; tap **Deny**, or use an approved test policy the PoC cannot approve | Denial remains available; browser/provider failure leaves registration intact. Unsupported approval policy is not bypassed. Exact Entra error varies. |
| Preserve other methods and restart | Enroll two disposable methods; successfully delete only one; relaunch sample without clearing storage; refresh Security info | Surviving method persists; deleted method remains absent; OAuth/Native Auth sessions are not signed out. |

Manual airplane-mode timing cannot reliably distinguish unsent DELETE from a
lost response after server commit. A timeout is **not proof of rollback**.
Always verify remote state independently before retrying.

## Deterministic JVM scenarios (no service/account required)

Run from the worktree root:

```powershell
.\gradlew.bat :mfa:testLocalDebugUnitTest --tests '*MfaDeletionTest'
.\gradlew.bat :testapps:mfa:testLiveDebugUnitTest --tests '*Deletion*Test'
```

Read pass/fail and individual cases in:
`mfa\build\reports\tests\testLocalDebugUnitTest\index.html` and
`testapps\mfa\build\reports\tests\testLiveDebugUnitTest\index.html`.
The fakes are confined to `src\test*`; these tests do not alter any live method.

| Injected condition | Test / action | Expected effect |
|---|---|---|
| HTTP 204, 404 | `onlyConfirmedRemoteAbsenceRemovesSelectedAccount` | Both remove only selected local method and invalidate its sessions. 404 means remote absence on the correctly constructed endpoint, not a reason to ignore identity validation. |
| HTTP 200, 401, 403, 429, 500, 503 | Same test | `GRAPH_REJECTED` with actual HTTP status; no local cleanup, no automatic retry or automatic browser loop. For real 401/403 fix auth/consent/policy; for 429 respect service throttling before an explicit later retry. |
| Empty/malformed access token; expired, non-Bearer, wrong scope; wrong tenant/object/authority | `rejectsUntrustedTokenMetadataBeforeDelete` | `INVALID_TOKEN`, zero Graph calls, unchanged registrations. Tokens stay opaque; metadata comes from actual library result/profile, never requested identity. |
| Provider failure before DELETE | `providerFailureAndUnknownTransportOutcomeRetainRegistration` | `PROVIDER_FAILED`, zero Graph calls, no cleanup. |
| Network exception during DELETE | Same test | `OUTCOME_UNKNOWN`; keep local state. Remote state deliberately cannot be inferred. |
| Lost response followed by confirmed remote absence | `unknownOutcomeThen404RetryRemovesOnlySelectedMethod` | First attempt retains state; explicit retry returning 404 cleans selected method only. |
| Confirmed remote success, local persistence fails | `cleanupFailureRetriesWithoutProviderOrSecondDelete` | `LOCAL_CLEANUP_REQUIRED`; sessions invalidated, local registration still listed for retry. Retry in same process performs only local cleanup, no token acquisition or second DELETE. |
| Cancel during acquisition | `cancelBeforeDispatchRetainsAndAllowsRetry` | No Graph call; state retained; later retry allowed. |
| Concurrent duplicate delete; cancel while approval response pending | `duplicateDeletionAndCancelledApprovalWaitCannotDispatch` | Duplicate gets `OPERATION_IN_PROGRESS`; waiting cancellation sends no DELETE. |
| Same-registration approval pending during acquisition | `approvalsStayAvailableDuringAcquisitionAndDeleteWaitsForResponse` | Response can run; DELETE waits for its completion. |
| Cancel after dispatch/204 | `cancellationAfterDispatchCannotUndoConfirmedCleanup` | Bounded request completes and local cleanup commits; cancellation cannot restore a remotely deleted method. |
| Cancel after dispatch plus local cleanup failure | `committedCleanupFailureSurvivesCancellationAndRemainsRecoverable` | Cleanup-required state survives cancellation; cleanup-only retry remains possible. |
| Late/duplicate MSAL callbacks | `DeletionCallbackBridgeTest` | Cancelled acquisition cannot consume late token/start DELETE; duplicate callback cannot release another operation's single-flight guard. |
| Home vs guest profile; missing/ambiguous claims | `DeletionIdentityTest` | Use returned token tenant's profile `tid`/`oid`, never home `IAccount.id`; missing/ambiguous metadata rejected. |
| Silent-only and fallback classification | `DeletionTokenAcquisitionTest` | Silent success never opens browser; fallback only for interaction-required and only when allowed; errors never silently switch accounts. |
| Wrong package/signature/config mode | `DeletionConfigurationTest` | Configuration stays disabled; no wrong-client fallback. |

## Recovery and cancellation boundaries

- Before DELETE dispatch, cancellation is cooperative and prevents dispatch.
- Once DELETE is dispatched, its HTTP operation is bounded to 45 seconds and is
  deliberately allowed to finish so a delivered 204/404 is not discarded at a
  coroutine cancellation boundary. Cancelling does not undo remote changes.
- Cleanup-only recovery after persistence failure is remembered **in memory**.
  Keep the process alive and select Delete again. If the process dies while disk
  writes are failing, that recovery marker cannot be guaranteed on disk. Verify
  Security info, then retry with a valid token acquired through an alternate
  working factor; confirmed 404 allows cleanup. Do not claim crash-safe rollback.
- Service failures retain state. A 5xx, timeout, cancellation after dispatch, or
  process kill can leave remote state uncertain. Do not label these as success.
- JVM tests cover controlled errors; Android browser lifecycle, notification
  delivery, approved tenant policies and actual Graph deletion still need the
  DEVICE tests above. No live enrollment/deletion is performed by build/tests.
