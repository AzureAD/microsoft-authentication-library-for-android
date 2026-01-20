# Code Coverage

## Overview
Code coverage for MSAL Android is generated and published by the Azure Pipeline, not by GitHub Actions.

## Why Not GitHub Actions?
The GitHub Actions workflow was removed because:
1. **Authentication Issues**: Running MSAL tests requires access to internal Azure DevOps artifact feeds
2. **No Access Tokens**: GitHub Actions doesn't have Azure DevOps authentication tokens
3. **Duplicate Infrastructure**: Azure Pipeline already runs tests with proper authentication

## Where Coverage is Generated
Code coverage is now generated in the **PR validation pipeline**:
- **Pipeline**: `azure-pipelines/pull-request-validation/pr-msal.yml`
- **Azure DevOps**: [Pipeline 1328](https://identitydivision.visualstudio.com/Engineering/_build?definitionId=1328)

This pipeline:
- Runs on every PR automatically
- Has proper authentication to internal artifact feeds
- Generates Jacoco coverage reports
- Publishes coverage to Azure DevOps
- Makes coverage available for review

## Viewing Coverage
Coverage results are available in:
1. **Azure DevOps**: View the pipeline run and check the "Code Coverage" tab
2. **Codecov** (if configured): Coverage may also be published to Codecov.io

## Implementation Details
The pr-msal.yml pipeline includes:
- **Test Execution**: Runs all unit tests
- **Coverage Generation**: Generates Jacoco coverage report via `localDebugMsalUnitTestCoverageReport` task
- **Coverage Publishing**: Publishes results using `PublishCodeCoverageResults@1` task

## Previous GitHub Actions Workflow
The previous `.github/workflows/code-coverage.yml` workflow attempted to:
- Run MSAL tests in GitHub Actions
- Generate coverage locally
- Compare PR vs dev branch coverage

This approach was not viable due to authentication requirements for accessing internal dependencies.
