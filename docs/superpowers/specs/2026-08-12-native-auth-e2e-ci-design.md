# Native Auth E2E CI Design

## Goal

Run all MSAL Native Auth network end-to-end tests on the pull-request branch without enabling unrelated MSAL network tests or changing the existing unit-test and coverage behavior.

## Pipeline Design

Keep the existing `msal:jacocoTestReport` step unchanged. Add a separate Gradle step to the PR-branch testing job in `azure-pipelines/pull-request-validation/pr-msal.yml`.

The new step will:

- run `msal:cleanTestLocalDebugUnitTest` followed by `msal:testLocalDebugUnitTest`;
- pass `-Plabtest` so Gradle does not exclude network E2E tests;
- filter execution to `com.microsoft.identity.client.e2e.tests.network.nativeauth.*`;
- pass `-PnativeAuthConfigString=$(NATIVE_AUTH_CONFIG_STRING)`;
- pass `-PemailProviderPassword=$(EMAIL_PROVIDER_PASSWORD)`; and
- use the `LabAuth` certificate already installed by `automation-cert.yml`.

The filter prevents other network E2E packages from running. Cleaning the test task output ensures the filtered invocation executes even though the existing coverage step previously ran the same underlying unit-test task with different inputs.

## Secret Configuration

Create `EMAIL_PROVIDER_PASSWORD` as a secret Azure DevOps pipeline variable. Its value must be the password used by the Mail.tm accounts referenced by the Native Auth test configuration. The value must never be committed or printed.

The existing `NATIVE_AUTH_CONFIG_STRING` pipeline variable supplies tenant and account configuration. The existing `automation-cert.yml` step obtains `LabAuth` from the `msidlabs` Key Vault and installs it into the agent's Windows certificate store.

## Failure Behavior

The new pipeline step must fail when any Native Auth network E2E test fails or when required configuration is absent. It must not silently skip the package or treat missing credentials as success.

## Verification

The Azure DevOps test results must contain test cases from the `com.microsoft.identity.client.e2e.tests.network.nativeauth` package, including the re-enabled `SSPRTest` methods. The pipeline log must show that `testLocalDebugUnitTest` ran with `labtest` enabled and the Native Auth package filter.
