# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License.

# Requires Pester 5; run with Invoke-Pester -Path ./check-native-auth-skip-label.Tests.ps1 -CI.
Describe 'Native Auth skip-label gate' {
    BeforeEach {
        $scriptUnderTest = Join-Path $PSScriptRoot 'check-native-auth-skip-label.ps1'
        $originalReason = $env:BUILD_REASON
        $originalPrNumber = $env:SYSTEM_PULLREQUEST_PULLREQUESTNUMBER
        $originalRepositoryUri = $env:BUILD_REPOSITORY_URI
        $env:BUILD_REASON = 'PullRequest'
        $env:SYSTEM_PULLREQUEST_PULLREQUESTNUMBER = '2553'
        $env:BUILD_REPOSITORY_URI = 'https://github.com/AzureAD/test-repository'
        $repository = Join-Path $TestDrive 'repository'
        New-Item -ItemType Directory -Path $repository -Force | Out-Null
        Push-Location $repository
        git init --quiet
        git config --local http.https://github.com/AzureAD/test-repository.extraheader 'AUTHORIZATION: basic test-only-credential'
        git config --local http.https://github.com/unrelated/repository.extraheader 'AUTHORIZATION: basic unrelated-credential'
        Mock Invoke-RestMethod { '{"labels":[]}' | ConvertFrom-Json }
        Mock Start-Sleep {}
    }

    AfterEach {
        Pop-Location
        $env:BUILD_REASON = $originalReason
        $env:SYSTEM_PULLREQUEST_PULLREQUESTNUMBER = $originalPrNumber
        $env:BUILD_REPOSITORY_URI = $originalRepositoryUri
    }

    It 'authenticates with the credential for this repository and runs without the label' {
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]false') {
            throw 'SSPR must run when the skip label is absent.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 1 -Exactly -Scope It -ParameterFilter {
            $Headers.Authorization -eq 'basic test-only-credential' -and
            $Uri -eq 'https://api.github.com/repos/AzureAD/test-repository/pulls/2553' -and
            $ConnectionTimeoutSeconds -eq 10 -and $OperationTimeoutSeconds -eq 10 -and
            $MaximumRedirection -eq 0
        }
        if (($output -join "`n") -match 'test-only-credential|unrelated-credential') {
            throw 'Credentials must not appear in pipeline output.'
        }
    }

    It 'skips only when the exact label is confirmed' {
        Mock Invoke-RestMethod { '{"labels":[{"name":"skip-native-auth-e2e-tests"}]}' | ConvertFrom-Json }
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]true') {
            throw 'The explicit skip label must unblock the PR.'
        }
    }

    It 'does not match a label substring or text elsewhere in the PR' {
        Mock Invoke-RestMethod {
            '{"body":"skip-native-auth-e2e-tests","labels":[{"name":"do-not-skip-native-auth-e2e-tests"}]}' | ConvertFrom-Json
        }
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]false') {
            throw 'Only the exact label can bypass SSPR.'
        }
    }

    It 'warns and runs without making an unauthenticated request when credentials are missing' {
        git config --local --unset http.https://github.com/AzureAD/test-repository.extraheader
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]false' -or
            -not ($output -match 'task.logissue type=warning')) {
            throw 'Missing authentication must warn and leave SSPR enabled.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 0 -Exactly -Scope It
    }

    It 'warns and runs when authentication cannot be used safely' {
        git config --local http.https://github.com/AzureAD/test-repository.extraheader 'AUTHORIZATION: '
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]false' -or
            -not ($output -match 'task.logissue type=warning')) {
            throw 'An empty authorization header must not enable a skip.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 0 -Exactly -Scope It
    }

    It 'retries a failed lookup and honors the label after recovery' {
        $script:requests = 0
        Mock Invoke-RestMethod {
            $script:requests++
            if ($script:requests -eq 1) {
                throw [System.Net.WebException]::new('temporary failure')
            }
            '{"labels":[{"name":"skip-native-auth-e2e-tests"}]}' | ConvertFrom-Json
        }
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]true') {
            throw 'A recovered lookup must honor the confirmed skip label.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 2 -Exactly -Scope It
        Assert-MockCalled Start-Sleep -Times 1 -Exactly -Scope It -ParameterFilter { $Seconds -eq 2 }
    }

    It 'bounds retries and runs on <failure>' -TestCases @(
        @{ failure = 'HTTP 401'; status = 401 }
        @{ failure = 'HTTP 403'; status = 403 }
        @{ failure = 'HTTP 429'; status = 429 }
        @{ failure = 'HTTP 500'; status = 500 }
        @{ failure = 'timeout'; status = 0 }
        @{ failure = 'connection failure'; status = 0 }
    ) {
        param($failure, $status)
        if ($status) {
            $response = [System.Net.Http.HttpResponseMessage]::new([System.Net.HttpStatusCode]$status)
            $requestFailure = [Microsoft.PowerShell.Commands.HttpResponseException]::new('test-only-credential', $response)
        } elseif ($failure -eq 'timeout') {
            $requestFailure = [System.OperationCanceledException]::new('test-only-credential')
        } else {
            $requestFailure = [System.Net.Http.HttpRequestException]::new('test-only-credential')
        }
        Mock Invoke-RestMethod -MockWith ({ throw $requestFailure }.GetNewClosure())
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]false' -or
            -not ($output -match 'task.logissue type=warning')) {
            throw 'Failed lookup must warn and leave SSPR enabled.'
        }
        if (($output -join "`n") -match 'test-only-credential') {
            throw 'Raw exception text must not leak credentials.'
        }
        if ($status -and -not ($output -match "HTTP $status")) {
            throw 'HTTP status must be available for diagnosing the lookup failure.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 3 -Exactly -Scope It
        Assert-MockCalled Start-Sleep -Times 2 -Exactly -Scope It
        Assert-MockCalled Start-Sleep -Times 1 -Exactly -Scope It -ParameterFilter { $Seconds -eq 4 }
        if ($response) { $response.Dispose() }
    }

    It 'runs rather than skips for malformed response <body>' -TestCases @(
        @{ body = 'not json' }
        @{ body = '{"message":"API error"}' }
        @{ body = '{"labels":null}' }
        @{ body = '{"labels":{"name":"skip-native-auth-e2e-tests"}}' }
        @{ body = '{"labels":[null]}' }
        @{ body = '{"labels":[{"name":1}]}' }
    ) {
        param($body)
        $responseBodyText = $body
        Mock Invoke-RestMethod -MockWith ({
            if ($responseBodyText -eq 'not json') { return $responseBodyText }
            $responseBodyText | ConvertFrom-Json
        }.GetNewClosure())
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]false' -or
            -not ($output -match 'task.logissue type=warning')) {
            throw 'An invalid response cannot authorize skipping tests.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 3 -Exactly -Scope It
    }

    It 'does not bypass a PR when its number is missing' {
        $env:SYSTEM_PULLREQUEST_PULLREQUESTNUMBER = ''
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]false' -or
            -not ($output -match 'task.logissue type=warning')) {
            throw 'Missing PR metadata must not silently bypass SSPR.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 0 -Exactly -Scope It
    }

    It 'does not send credentials to an unexpected repository host' {
        $env:BUILD_REPOSITORY_URI = 'https://example.invalid/AzureAD/test-repository'
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]false') {
            throw 'Unexpected repository metadata must leave SSPR enabled.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 0 -Exactly -Scope It
    }

    It 'preserves the explicit non-PR-run exemption' {
        $env:BUILD_REASON = 'Manual'
        $output = @(& $scriptUnderTest)
        if ($output[-1] -ne '##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]true') {
            throw 'Non-PR runs should remain outside the PR-only SSPR gate.'
        }
        Assert-MockCalled Invoke-RestMethod -Times 0 -Exactly -Scope It
    }
}
