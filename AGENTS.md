<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# MSAL Android: agent entry point

## Context and best practices

- Read [Copilot instructions](.github/copilot-instructions.md) for API patterns,
  configuration, customer support, review rules, and test expectations. This
  entry point adds contributor setup/verification; it does not replace those rules.
- For app integration, use [Ai.md](Ai.md), [snippets](snippets/), the
  [multiple-account example](examples/hello-msal-multiple-account/),
  [single-account example](examples/hello-msal-single-account/), and
  [configuration template](auth_config.template.json).
- `msal/` is the public SDK facade. `common/` is a pinned Git submodule owning
  shared protocol, cache, crypto, and IPC behavior. Preserve public API,
  account-mode, callback/threading, and Common/Broker compatibility contracts.
- `msalautomationapp/` contains cross-app UI automation; `testapps/` contains
  test/sample apps. Do not equate an app build with a successful auth flow.
- Reuse existing guidance and tooling rather than adding duplicate agents or
  skills. Keep changes focused, preserve unrelated work, and never commit
  credentials, private configuration, or test-account data.

## Setup

Run commands from this repository's root, not from `msal/`, `common/`, or the
parent android-complete checkout. The commands below use PowerShell on Windows;
on macOS/Linux use `./gradlew` with equivalent JDK, SDK, and feed configuration.

1. Use **JDK 17**, as configured in the
   [MSAL PR pipeline](azure-pipelines/pull-request-validation/pr-msal.yml).
   Set `JAVA_HOME` for the current shell. Use the checked-in wrapper
   (Gradle 8.11.1), not a globally installed Gradle; check `.\gradlew.bat --version`.
   Java 8 source/bytecode targets are not the required Gradle runtime.
2. Initialize the pinned Common submodule in a fresh checkout:

   ```powershell
   git submodule update --init --recursive
   git submodule status
   ```

   Inspect existing submodule work before updating an established checkout.
   Do not use `--remote` or change the submodule pin merely to make a build pass.
3. Install Android SDK platform 36 and the build tools required by
   [version configuration](gradle/versions.gradle). Configure `ANDROID_HOME`
   or an untracked root `local.properties` containing `sdk.dir`.
4. Obtain Packaging Read access to the NewAndroid feed configured in
   [settings](settings.gradle) and [the root build](build.gradle).
   Provision `ENV_VSTS_MVN_CRED_USERNAME` and `ENV_VSTS_MVN_CRED_ACCESSTOKEN`
   through approved local secret handling before Gradle runs. Do not commit
   tokens, print them, or change feed/repository configuration to bypass access.
5. The default JVM suite includes Native Auth tests backed by the mock service,
   even without `-Plabtest`. Obtain approved access and configure `mockApiUrl`
   in user-level Gradle properties outside the checkout before running the
   unfiltered JVM or coverage commands below. Lab/network tests additionally
   need approved test accounts, a lab certificate, and applicable Native Auth
   configuration. CI uses variable groups
   and [Common's certificate setup](common/azure-pipelines/templates/steps/automation-cert.yml).
   Request approved access; keep local certificates and private config outside
   the checkout. Credential provisioning or costly/device runs need approval.

## Fast verification while editing

Build the local-source variant and run JVM/Robolectric tests:

```powershell
.\gradlew.bat :msal:assembleLocalDebug :msal:testLocalDebugUnitTest
```

For a focused test, replace this existing example with the relevant test class:

```powershell
.\gradlew.bat :msal:testLocalDebugUnitTest --tests "com.microsoft.identity.client.PromptTest"
```

Tests live under `msal/src/test/`; device tests under `msal/src/androidTest/`.
Filtered tests are an iteration aid, not full pre-PR verification.

## Before pushing or opening a PR

For documentation-only changes, check relative links and `git diff --check`.
Executable changes require all applicable layers below. Record unavailable
layers explicitly rather than treating a smaller test run as full verification.

### Library build, JVM/Robolectric tests, coverage, and static analysis

```powershell
git diff --check
.\gradlew.bat :msal:assembleLocal
.\gradlew.bat :msal:jacocoTestReport -PcodeCoverageEnabled=true -ProbolectricSdkVersion=33
.\gradlew.bat :msal:lintLocalDebug :msal:spotbugsLocalDebug
```

These match the build/report/lint tasks and SDK used by the
[PR pipeline](azure-pipelines/pull-request-validation/pr-msal.yml).
SpotBugs requires assembled outputs; its task name comes from
[Common's template](common/azure-pipelines/templates/steps/spotbugs.yml).
The coverage task depends on `testLocalDebugUnitTest` and fails if execution
data is missing. Do not publish a missing-data report as successful coverage.

### Network integration and UI-less E2E

The [library build](msal/build.gradle) excludes
`com/microsoft/identity/client/e2e/tests/network` unless **`-Plabtest`** is set.
Default JVM tests include mocked flows but do not validate this network suite.
After approved lab/service setup, enable it explicitly:

```powershell
.\gradlew.bat :msal:jacocoTestReport -PcodeCoverageEnabled=true -ProbolectricSdkVersion=33 -Plabtest
```

Supply the applicable `labSecret`, `mockApiUrl`, and Native Auth configuration
properties through approved local configuration or credentialed CI. This
command does not provision them. The PR pipeline supplies mock/Native Auth
properties but does not currently pass `-Plabtest`; do not assume that pipeline
alone validates the excluded network suite. Confirm the selected CI run enables
the required suite, or report it as not run.

### Library instrumented/device tests

For Android framework, lifecycle, broker-boundary, or integration changes,
run relevant device tests as well as JVM tests. With an approved emulator/device
visible to `adb devices` and its lab certificate already provisioned:

```powershell
.\gradlew.bat :msal:connectedLocalDebugAndroidTest -Psugar=true "-PlabSecret=/data/local/tmp/LabAuth.pfx"
```

The certificate argument is a device path, not a setup command. Alternatively,
use the existing [instrumented PR pipeline](azure-pipelines/pull-request-validation/pr-msal-instrumented.yml),
which provisions the emulator/lab setup through its shared template.

### Automation app and cross-app UI E2E

For automation-app changes, build the local variants:

```powershell
.\gradlew.bat :msalautomationapp:assembleLocal
```

See [automation-app build validation](azure-pipelines/pull-request-validation/pr-msalautomationapp.yml).
For auth-flow or broker-integration changes, also run the affected cross-app UI
scenarios. Discover available tasks with `.\gradlew.bat :msalautomationapp:tasks --all`.
For example, after provisioning the device, lab certificate, and BrokerHost APK:

```powershell
.\gradlew.bat :msalautomationapp:connectedLocalBrokerHostDebugAndroidTest "-PlabSecret=/data/local/tmp/LabAuth.pfx"
```

Use the [Android Auth test-running guide](https://github.com/AzureAD/android-complete/blob/master/docs/Automation/how_to_run_tests.md)
and [lab setup guide](https://github.com/AzureAD/android-complete/blob/master/docs/Automation/labsetup.md)
for device arguments, APK requirements, and broker flavors. Select only approved
scenarios/devices; neither library instrumentation nor assembling the automation
app proves cross-app UI E2E success.

## Failures and reporting

- Check `$LASTEXITCODE` immediately after each native command and stop on
  nonzero status; a later successful check must not hide an earlier failure.
- Use `.\gradlew.bat :msal:tasks --all` to inspect tasks. Task discovery still
  configures the build and needs its dependencies and feed access.
- An offline cache miss is not a nonexistent task. Provision dependencies using
  the configured feed and approved access, then retry; do not silently skip
  checks or enable public repositories as a workaround.
- Feed 401/403, missing JDK/SDK/submodule/device, and unavailable lab/service
  configuration are explicit blockers. Never report these as passing tests.
- Report commands, working directory, exit status, and every failed/not-run
  layer. Documentation and task discovery do not prove build/test success or
  official repository readiness; the latter needs the official reassessment.
