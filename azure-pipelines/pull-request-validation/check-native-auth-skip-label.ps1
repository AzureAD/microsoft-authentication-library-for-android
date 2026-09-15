# Copyright (c) Microsoft Corporation. All rights reserved.
# Licensed under the MIT License.

$ErrorActionPreference = 'Stop'
$skipLabel = 'skip-native-auth-e2e-tests'

function Write-SkipDecision {
    param([bool]$Skip, [string]$Warning)

    if ($Warning) {
        Write-Output "##vso[task.logissue type=warning]$Warning"
    }
    Write-Output "##vso[task.setvariable variable=SKIP_NATIVE_AUTH_E2E]$($Skip.ToString().ToLowerInvariant())"
}

if ($env:BUILD_REASON -and $env:BUILD_REASON -ne 'PullRequest') {
    Write-Output 'Not a PR build; the Native Auth PR gate does not apply.'
    Write-SkipDecision -Skip $true
    return
}

$repositoryUri = $null
$prNumber = $env:SYSTEM_PULLREQUEST_PULLREQUESTNUMBER
if ($prNumber -notmatch '^[1-9][0-9]*$' -or
    -not [Uri]::TryCreate($env:BUILD_REPOSITORY_URI, [UriKind]::Absolute, [ref]$repositoryUri) -or
    $repositoryUri.Scheme -ne 'https' -or $repositoryUri.Host -ne 'github.com' -or
    $repositoryUri.UserInfo -or $repositoryUri.Query -or $repositoryUri.Fragment) {
    Write-SkipDecision -Skip $false -Warning 'Cannot validate PR metadata. Running Native Auth SSPR tests without a label override.'
    return
}

$repository = $repositoryUri.AbsolutePath.Trim('/') -replace '\.git$', ''
if ($repository -notmatch '^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$') {
    Write-SkipDecision -Skip $false -Warning 'Cannot validate the repository path. Running Native Auth SSPR tests without a label override.'
    return
}

# Reuse checkout's persisted GitHub credential, scoped to this repository rather than
# taking the first credential in git config (which could belong to a submodule).
try {
    $authHeaders = @(git config --local --get-urlmatch http.extraheader $env:BUILD_REPOSITORY_URI 2>$null)
    $gitExitCode = $LASTEXITCODE
} catch [System.Management.Automation.CommandNotFoundException] {
    Write-SkipDecision -Skip $false -Warning 'Git is unavailable for authenticated label lookup. Running Native Auth SSPR tests.'
    return
}

$authorization = @($authHeaders | ForEach-Object {
    if ($_ -match '^AUTHORIZATION:\s*((?:basic|bearer)\s+\S+)\s*$') { $Matches[1] }
})
if ($gitExitCode -ne 0 -or $authorization.Count -ne 1) {
    Write-SkipDecision -Skip $false -Warning 'No unambiguous persisted GitHub credential is available. Running Native Auth SSPR tests; check checkout persistCredentials configuration.'
    return
}

$headers = @{
    Accept = 'application/vnd.github+json'
    Authorization = $authorization[0]
    'User-Agent' = 'azure-pipelines-native-auth-label-check'
}
# The PR response includes the full labels array, avoiding pagination of the labels endpoint.
$url = "https://api.github.com/repos/$repository/pulls/$prNumber"
$maxAttempts = 3
for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    try {
        $pullRequest = Invoke-RestMethod -Method Get -Uri $url -Headers $headers `
            -ConnectionTimeoutSeconds 10 -OperationTimeoutSeconds 10 -MaximumRedirection 0 -ErrorAction Stop
        if ($null -eq $pullRequest -or $pullRequest.labels -isnot [array]) {
            throw [System.IO.InvalidDataException]::new('Expected a PR labels array.')
        }
        foreach ($label in $pullRequest.labels) {
            if ($null -eq $label -or $label.name -isnot [string]) {
                throw [System.IO.InvalidDataException]::new('Expected label names.')
            }
        }

        $skip = @($pullRequest.labels | ForEach-Object { $_.name }) -ccontains $skipLabel
        if ($skip) {
            Write-Output "Confirmed '$skipLabel'; skipping Native Auth SSPR tests."
        } else {
            Write-Output "No '$skipLabel' label; running Native Auth SSPR tests."
        }
        Write-SkipDecision -Skip $skip
        return
    } catch [System.Net.WebException], [System.Net.Http.HttpRequestException],
            [Microsoft.PowerShell.Commands.HttpResponseException], [System.OperationCanceledException],
            [System.TimeoutException], [System.IO.InvalidDataException] {
        # Do not log the raw exception or response body: either may contain sensitive data.
        $failure = $_.Exception.GetType().Name
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $failure = "HTTP $([int]$_.Exception.Response.StatusCode)"
        }
        Write-Output "##vso[task.logissue type=warning]Authenticated label lookup attempt $attempt/$maxAttempts failed ($failure)."
        if ($attempt -lt $maxAttempts) {
            Start-Sleep -Seconds (2 * $attempt)
        }
    }
}

Write-SkipDecision -Skip $false -Warning 'Label lookup failed after bounded retries. Running Native Auth SSPR tests; no skip label could be confirmed.'
