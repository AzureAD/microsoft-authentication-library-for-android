# MSAL Automation App Instrumented Test Case Summary (Combined/Optimized)

This document outlines the proposed combined test structure for msalautomationapp (src/androidTest), reducing redundant setup and improving runtime efficiency. Each combined test is named using the "TestCaseNUMBERAndNUMBER" convention where applicable. All original coverage is preserved.

---

## Test Count Comparison

| Directory                 | Original Test Count | Combined Test Count |
|---------------------------|---------------------|---------------------|
| broker/atpop/             | 4                   | 2                   |
| broker/atpop/update/      | 4                   | 2                   |
| broker/brokerapi/         | 6                   | 6                   |
| broker/crosscloud/        | 4                   | 2                   |
| broker/dcf/               | 3                   | 2                   |
| broker/flw/               | 7                   | 3                   |
| broker/foci/              | 1                   | 1                   |
| broker/joined/            | 7                   | 3                   |
| broker/ltw/               | 16                  | 16                  |
| broker/ltw/TransferToken/ | 1                   | 1                   |
| broker/mam/               | 5                   | 4                   |
| broker/mdm/               | 2                   | 1                   |
| broker/msa/               | 4                   | 2                   |
| broker/mwpj/              | 11                  | 5                   |
| broker/nestedAppAuth/     | 5                   | 3                   |
| broker/nonjoined/         | 9                   | 4                   |
| broker/update/            | 3                   | 3                   |
| broker/usgov/             | 5                   | 2                   |
| broker/wpj/               | 3                   | 3                   |
| msalonly/atpop/           | 2                   | 1                   |
| msalonly/basic/           | 6                   | 4                   |
| msalonly/crosscloud/      | 3                   | 2                   |
| msalonly/usgov/           | 4                   | 2                   |
| **Total**                 | **115**             | **74**              |

---

## broker/atpop/ (4 tests → 2 combined)

### TestCase1922511And1922527: [Non-Joined] Acquire PoP Token Interactive, Silent, and Generate SHR
- **Covers:** TestCase1922511, TestCase1922527
- **Steps:**
  1. Acquire PoP token interactively (non-joined device)
  2. Validate AT is PoP
  3. Acquire PoP token silently
  4. Validate silent AT is PoP
  5. Generate SHR (Signed HTTP Request)
  6. Validate SHR is PoP
- **Details:**
  - Combines interactive/silent PoP token acquisition and SHR generation for non-joined device

### TestCase1922513And1922515: [Joined] Acquire PoP Token Interactive, Silent, and Generate SHR
- **Covers:** TestCase1922513, TestCase1922515
- **Steps:**
  1. Register device (join)
  2. Acquire PoP token interactively
  3. Validate AT is PoP
  4. Acquire PoP token silently
  5. Validate silent AT is PoP
  6. Generate SHR (Signed HTTP Request)
  7. Validate SHR is PoP
- **Details:**
  - Combines interactive/silent PoP token acquisition and SHR generation for joined device

---

## broker/atpop/update/ (4 tests → 2 combined)

### TestCase1922530And1922547: [Joined][Update-old-to-V5] Acquire PoP Token Silent and Generate SHR
- **Covers:** TestCase1922530, TestCase1922547
- **Steps:**
  1. Register device (joined)
  2. Acquire PoP token interactively (Prompt.LOGIN)
  3. Validate AT is PoP
  4. Update broker app
  5. Acquire PoP token silently
  6. Validate silent AT is PoP
  7. Generate SHR (Signed HTTP Request)
  8. Validate SHR is PoP
- **Details:**
  - Combines interactive/silent PoP token acquisition and SHR generation after broker update on a joined device

### TestCase1922531And1922549: [Non-Joined][Update-old-to-V5] Acquire PoP Token Silent and Generate SHR
- **Covers:** TestCase1922531, TestCase1922549
- **Steps:**
  1. Acquire PoP token interactively (Prompt.LOGIN)
  2. Validate AT is PoP
  3. Update broker app
  4. Acquire PoP token silently
  5. Validate silent AT is PoP
  6. Generate SHR (Signed HTTP Request)
  7. Validate SHR is PoP
- **Details:**
  - Combines interactive/silent PoP token acquisition and SHR generation after broker update on a non-joined device

---

## broker/brokerapi/ (6 tests)

// These tests have distinct flows and cannot be combined without losing coverage or introducing side effects. List as-is.

### TestCase1561087: BrokerHost Flight Settings
### TestCase1561136: Get Broker Accounts
### TestCase1561137: Remove Broker Account
### TestCase1561652: SSO Token Requests
### TestCase1600567: Non-Allowed Broker App API Access
### TestCase2110359: Check DCF Option UI (Join Tenant)

---

## broker/crosscloud/ (4 tests → 2 combined)

// AI-ERROR: CANNOT BE COMBINED, one is joined the other is not joined.
### TestCase1400731And1420494: [Joined] Guest Support: Interactive/Silent Auth (Cross Cloud) and Acquire Token for Cross Cloud Guest Account
- **Covers:** TestCase1400731, TestCase1420494
- **Steps:**
  1. Load guest user and register device
  2. Acquire token interactively for Lab3
  3. Acquire token interactively for Lab4
  4. Advance device time
  5. Acquire token silently for Lab3
  6. Acquire token silently for Lab4
  7. Validate new access token and profile for guest account
- **Details:**
  - Combines guest account interactive/silent auth across multiple clouds and profile validation

// AI-ERROR: CANNOT BE COMBINED, cannot do this without clearing state between the tests.
### TestCase1592465And1592510: Acquire Token from Cross Cloud after Home Cloud and Home Cloud after Cross Cloud
- **Covers:** TestCase1592465, TestCase1592510
- **Steps:**
  1. Acquire token interactively from home cloud
  2. Attempt silent token from cross cloud (expect exception)
  3. Acquire token interactively from cross cloud
  4. Validate new access token
  5. Acquire token interactively from cross cloud
  6. Attempt silent token from home cloud (expect exception)
  7. Acquire token interactively from home cloud
  8. Validate new access token
- **Details:**
  - Combines token acquisition flow and error handling between home/cross cloud

---

## broker/dcf/ (3 tests → 2 combined)

// AI-ERROR: CAN COMBINE ALL THREE, first try without parameter, then try with azure Cloud, then US Gov
### TestCase2828864And2828868: Check 'Sign In from Other Device' Option (Azure Cloud and US Gov)
- **Covers:** TestCase2828864, TestCase2828868
- **Steps:**
  1. Launch brokered authentication flow (Azure Cloud)
  2. Verify 'Sign In from other device' option is available
  3. Confirm remote login URL is https://microsoft.com/devicelogin
  4. Launch brokered authentication flow (US Gov)
  5. Verify 'Sign In from other device' option is available
  6. Confirm remote login URL is https://microsoft.com/deviceloginus
- **Details:**
  - Combines device code flow option checks for Azure Cloud and US Gov

### TestCase2836426: 'Sign In from Other Device' Option Not Available Without Parameter
- **Covers:** TestCase2836426
- **Steps:**
  1. Launch brokered authentication flow without 'is_remote_login_allowed=true' parameter
  2. Attempt to access sign-in options
  3. Verify 'Sign In from other device' option is NOT present
- **Details:**
  - Ensures device code flow option is gated by query parameter

---

## broker/flw/ (7 tests → 3 combined)

Combination SDM Test
- Setup SDM
- Sign in with cloud user
- Check cloud user can use App A and B (833514)
- Check account from other tenant cannot sign in (833513)
- Try account from same tenant, should fail (833516)
- Verify sign out (833515) and silent request interruption (2495140)

833517 is very similar to 833515, but signs out the admin account rather than cloud, not sure if we need both
833511 should be separate as it tests non-admin account registration failure

---

## broker/foci/ (1 test)

### TestCase833544: FOCI SSO with Outlook and Word (Non-joined)
- **Covers:** TestCase833544
- **Steps:**
  1. Disable notifications for Authenticator
  2. Install and launch Outlook, add first account
  3. Confirm account in Outlook
  4. Install and launch Word, handle first run
  5. (Further steps in file...)
- **Details:**
  - Tests FOCI (Family of Client IDs) SSO across Outlook and Word apps for non-joined device

---

## broker/joined/ (7 tests → 3 combined)

### TestCase1561125And832430: In-line WPJ with DeviceId Claim and Acquire Token + Silent with Resource
- **Covers:** TestCase1561125, TestCase832430
- **Steps:**
  1. Create claims request for deviceid in ID token
  2. Acquire token interactively with deviceid claim
  3. Handle prompt and authenticate
  4. Assert success and parse ID token
  5. Acquire token interactively with Prompt.SELECT_ACCOUNT and resource scope
  6. Assert success
  7. Acquire account object
  8. Acquire token silently with same resource and authority
  9. Assert silent token acquisition success
- **Details:**
  - Combines WPJ with deviceid claim and resource-based token acquisition flows for joined device

// Can simplify, first login with Prompt.LOGIN, then reset password
### TestCase1561151And1561171: Password Change and Prompt.LOGIN (Joined)
- **Covers:** TestCase1561151, TestCase1561171
- **Steps:**
  1. Perform device registration
  2. Acquire token interactively (Prompt.SELECT_ACCOUNT)
  3. Wait 1 minute
  4. Reset password for user
  5. Acquire token interactively (Prompt.LOGIN)
  6. Validate both authentications succeed
- **Details:**
  - Combines password reset and Prompt.LOGIN flows for joined device

### TestCase714567And796050: Device Registration via Settings Page and Add Account in Account Chooser Activity
- **Covers:** TestCase714567, TestCase796050
- **Steps:**
  1. Set device settings page usage
  2. Perform device registration via settings page
  3. Create two temp users
  4. Register device with first account
  5. Acquire token interactively (no login hint)
  6. Use Account Chooser Activity to add another account
- **Details:**
  - Combines device registration and account addition flows for joined device

// Can be part of TestCase714567And796050 at the end
### TestCase833558: Broker Delete Account via Account Manager
- **Covers:** TestCase833558
- **Steps:**
  1. Get user account from lab
  2. Perform device registration with lab account
  3. Force stop broker
  4. Create temp account and acquire token
  5. Remove temp account from settings page
  6. Attempt silent token request for temp account (expect no_account_found error)
  7. Remove device registration owner account from settings page
  8. Create temp account and acquire token again
- **Details:**
  - Tests account deletion via Account Manager on a joined device
  - Verifies silent token request fails after account removal and new account can be created

---

## broker/ltw/ (16 tests)

// NOTE: No combinations are made in the ltw directory at this time. Consolidation and optimization of these tests will be performed by human engineers in a future pass. All test cases remain listed individually for now.

---

## broker/ltw/TransferToken/ (1 test)

### TestCase3026421: Transfer Token Generation and Restore
- **Covers:** TestCase3026421
- **Steps:**
  1. Check if EnableGenerateAndStoreTransferTokens flight is enabled
  2. Acquire credentials (username, password)
  3. Build MSAL SDK and authentication parameters
  4. Acquire token interactively (Prompt.LOGIN)
  5. Handle user interaction (login prompt, no session, no consent, no speed bump, broker interaction)
- **Details:**
  - Tests transfer token generation and restore using LTW broker
  - Skips test if required flight is not enabled

---

## broker/mam/ (5 tests → 4 combined)

### TestCase2506936: Teams Sign In, Sign Out, and Sign Back In (TrueMAM, Company Portal broker, not shared device mode)
- **Covers:** TestCase2506936
- **Steps:**
  1. Fetch credentials (username, password)
  2. Install and launch Teams app
  3. Sign in to Teams (handle prompt, register page, no enroll/consent/speed bump)
  4. Handle app protection policy in Company Portal (PIN setup)
  5. Force stop Teams, sign out
  6. Sign in again (handle prompt, second password page)
  7. Handle app protection policy again
- **Details:**
  - Teams MAM scenario, Company Portal broker, not shared device mode

### TestCase2798415: Teams Sign In, Sign Out, and Sign Back In (TrueMAM, Authenticator broker, shared device mode)
- **Covers:** TestCase2798415
- **Steps:**
  1. Register device in shared device mode (Authenticator broker)
  2. Install and launch Teams app
  3. Sign in to Teams (handle prompt, register page, no enroll/consent/speed bump)
  4. Handle app protection policy
  5. Force stop Teams, sign out
  6. Sign in again (handle prompt, second password page)
  7. Handle app protection policy again
- **Details:**
  - Teams MAM scenario, Authenticator broker, shared device mode

### TestCase2516571And2516967: Outlook Broker Required, Registration, and Re-Registration (TrueMAM, Company Portal broker)
- **Covers:** TestCase2516571, TestCase2516967
- **Steps:**
  1. Fetch credentials (username, password)
  2. Install and launch Outlook app
  3. Attempt to sign in without broker (expect 'Get the app' dialog)
  4. Install Authenticator, attempt sign in again (expect 'Get the app' dialog)
  5. Sign in and handle app protection policy
  6. Install BrokerHost, perform WPJ leave
  7. Advance device time to expire AT
  8. Sign in again via snackbar prompt, confirm account
- **Details:**
  - Combines broker requirement and registration/re-registration flows for Outlook

// I think we can drop this test, we do we need password reset for basic account and mam_ca?
### TestCase850457: Password Reset for MAM_CA Account
- **Covers:** TestCase850457
- **Steps:**
  1. Fetch credentials (username, password)
  2. Interactive token acquisition (Prompt.SELECT_ACCOUNT)
  3. Wait 1 minute, reset password
  4. Forward device time by one day
- **Details:**
  - Tests password reset flow for MAM_CA account
  - Validates token acquisition before and after password reset

---

## broker/mdm/ (2 tests → 1 combined)

// AI-ERROR: I don't think these can be combined
### TestCase831126And833526: [Joined][MDM] Device Admin MDM: MDM Account with Outlook/Word and Broker Auth + PKeyAuth Flow
- **Covers:** TestCase831126, TestCase833526
- **Steps:**
  1. Fetch credentials (username, password)
  2. Install and launch Outlook app, handle first run
  3. Add first account in Outlook (expect enroll page, second password page, no consent)
  4. If WebCP in WebView is enabled, handle Play Store flow for Company Portal
  5. Interactive token request in MSAL (expect enroll page, decline enroll)
  6. Assert failure (enroll required)
  7. Enroll device with Company Portal
  8. Try another interactive token request in MSAL (after enrollment)
- **Details:**
  - Combines MDM account sign-in and broker authentication/PKeyAuth flow

---

## broker/msa/ (4 tests → 2 combined)

// Run this as 2637853, just add a Prompt.LOGIN AT Call after the second request
// AI-ERROR: Separate Test for TestCase2637882
### TestCase2637829And2637846And2637853And2637882: [PRTv3] Brokered Auth for MSA Account - Prompt.Login, Select_Account, Consumers Authority, Silent
- **Covers:** TestCase2637829, TestCase2637846, TestCase2637853, TestCase2637882
- **Steps:**
  1. Fetch credentials (username, password)
  2. Interactive call: acquire token with Prompt.SELECT_ACCOUNT
  3. Assert success
  4. Silent call: acquire token silently for account (force refresh)
  5. Assert success
  6. Interactive call: acquire token with Prompt.LOGIN
  7. Interactive call: acquire token with consumers authority
  8. Silent call: acquire token silently for account (force refresh)
  9. Second request without login hint (expect account chooser)
  10. Assert success
- **Details:**
  - Combines all major brokered auth flows for MSA account

### TestCase3007768: [Brokered] Sign Up Flow for MSA Accounts
- **Covers:** TestCase3007768
- **Steps:**
  1. Enable sign up page via extra query parameter
  2. Interactive call: acquire token (expect sign up UI)
  3. Assert 'Create' account UI is present
  4. Exit auth, then call AcquireToken with existing MSA account
  5. Assert success
- **Details:**
  - Tests sign up flow for MSA accounts in brokered scenario
  - Validates UI and token acquisition for new and existing accounts

---

## broker/mwpj/ (11 tests → 5 combined)

// Quite a few tests, i'm unfamiliar with MWPJ inner workings, discuss with pedro

### TestCase2519783And2519809And2519833: [MWPJ] Install WPJ Certificate, Unregister 2 WPJ Entries, Get Records by Tenant ID and UPN
- **Covers:** TestCase2519783, TestCase2519809, TestCase2519833
- **Steps:**
  1. Register two accounts from different tenants using Multiple WPJ API
  2. Assert two device registration records exist
  3. Install WPJ certificate for browser access for both registrations
  4. Unregister both accounts
  5. Assert no device registration records remain
  6. Get record by tenant ID for each account and assert correctness
  7. Get record by UPN for each account and assert correctness
- **Details:**
  - Combines WPJ certificate install, unregister, and record retrieval flows

### TestCase2521768And2578879: [MWPJ] Account with No PRT Uses Non-Joined Flow (PKeyAuth Enabled)
- **Covers:** TestCase2521768, TestCase2578879
- **Steps:**
  1. Acquire token interactively with first account (no PRT)
  2. Register second account from same tenant using Multiple WPJ API
  3. Acquire token silently for first account (should use non-joined flow, no deviceid claim)
  4. Parse and validate access token claims
- **Details:**
  - Combines non-joined flow and PKeyAuth enabled scenarios for accounts without PRT

### TestCase2521946And2521960: [MWPJ] Device Registration Entry Migration (Same/Different UPN)
- **Covers:** TestCase2521946, TestCase2521960
- **Steps:**
  1. Register two accounts from different tenants using Multiple WPJ API
  2. Unregister device from legacy space for one account
  3. Verify device is unregistered for legacy API but still registered for MWPJ API
  4. Re-register device with same account using legacy API
  5. Register device with second account (same tenant, different UPN) using legacy API
  6. Assert entry in extended space was replaced with entry from second account
- **Details:**
  - Combines migration scenarios for device registration entries

### TestCase2563653And2563664: [MWPJ] Join with MWPJ API and Get Account with Legacy API, MWPJ Can Access Both Records
- **Covers:** TestCase2563653, TestCase2563664
- **Steps:**
  1. Register tenant with new WPJ API
  2. Use legacy API to get the account
  3. Assert legacy API can retrieve the account
  4. Register tenant A with legacy WPJ API
  5. Register tenant B with new WPJ API
  6. Assert legacy API returns tenant A
  7. Assert new WPJ API returns both tenants
- **Details:**
  - Combines interoperability and record access scenarios

### TestCase2563668And2579654: [MWPJ] Legacy Entry Should Work with New Broker (Upgrade Scenario), After Entry Migration, PRT Still Usable
- **Covers:** TestCase2563668, TestCase2579654
- **Steps:**
  1. Register tenant using legacy broker
  2. Get device ID using legacy broker
  3. Upgrade broker and enable Multiple WPJ
  4. Use new APIs to get all records, by tenantId, by UPN, get device token, install certificate, get device state, unregister device, get blob
  5. Assert all operations succeed and data is consistent
  6. Assert SSO is not broken (PRT is still usable without extra prompts)
  7. Acquire token interactively with deviceid claim
- **Details:**
  - Combines upgrade and migration scenarios for legacy entries and PRT usability

---

## broker/nestedAppAuth/ (5 tests → 3 combined)

// Need to discuss with sowmya

### TestCase2688459And2688460: Nested App Auth Silent Request and Interactive Request After Hub Interactive
- **Covers:** TestCase2688459, TestCase2688460
- **Steps:**
  1. Parameterized for UserType (MSA, CLOUD)
  2. Perform interactive token request for hub app
  3. Get account record after hub app AT
  4. Perform silent token request for nested app
  5. Assert silent request succeeds
  6. Perform interactive token request for nested app
  7. Assert interactive request succeeds
- **Details:**
  - Combines silent and interactive token acquisition for nested app after hub app interactive request

### TestCase2688462And2688468: Nested App's Fresh AT Interactive Succeeds, Silent Fails, and Interactive Token Request After Device is WPJd
- **Covers:** TestCase2688462, TestCase2688468
- **Steps:**
  1. Parameterized for UserType (MSA, CLOUD)
  2. Perform interactive token request for nested app
  3. Verify getAccounts returns 0 records after nested app AT
  4. Perform device registration (WPJ)
  5. Perform interactive token request for hub app
  6. Get account record after hub app AT
  7. Perform silent token request for nested app
  8. Assert silent request succeeds
- **Details:**
  - Combines fresh AT interactive/silent fail and WPJ scenarios for nested app

### TestCase2703171: Nested App Auth with US Gov Account
- **Covers:** TestCase2703171
- **Steps:**
  1. Perform interactive token request for hub app in US Gov cloud
  2. Get account record after hub app AT
  3. Perform silent token request for nested app in US Gov cloud
  4. Assert silent request succeeds
- **Details:**
  - Tests nested app authentication flow for US Gov accounts
  - Validates interactive and silent token acquisition in US Gov environment

---

## broker/nonjoined/ (9 tests → 4 combined)

// 850455 (SELECT_ACCOUNT, also seems like duplicate of 1592509) + 1561169 (PROMPT.LOGIN) + 1561152 (PASSWORD RESET)

// 833553 should be alone FEDERATED USER

// 497069 (no resource) + 2139526 (policy) 

// 3139972 should be alone

// 1600592 can be alone

// 833546


### TestCase1561152And2139526: [Non-Joined][MSAL] Password Change (bad_token) and Acquire Token Silent After Policy Change Should Fail
- **Covers:** TestCase1561152, TestCase2139526
- **Steps:**
  1. Acquire token interactively with Prompt.SELECT_ACCOUNT
  2. Reset password for user
  3. Forward device time by 1 day
  4. Attempt silent token request (should fail due to bad token)
  5. Acquire token interactively for SharePoint resource
  6. Change policy to MAM_CA
  7. Wait for policy change to propagate
  8. Forward device time by one day
  9. Attempt silent token request (should fail)
- **Details:**
  - Combines password reset and policy change silent request failure scenarios

### TestCase1561169And3139972: [Non-Joined][MSAL] Prompt.LOGIN and Acquire Token + Silent, no loginhint
- **Covers:** TestCase1561169, TestCase3139972
- **Steps:**
  1. Acquire token interactively with Prompt.SELECT_ACCOUNT
  2. Acquire token interactively again with Prompt.LOGIN
  3. Validate both authentications succeed
  4. Acquire token interactively with Prompt.SELECT_ACCOUNT and no login hint
  5. Acquire token silently for account (force refresh)
  6. Validate silent token acquisition
- **Details:**
  - Combines prompt login and silent acquisition without login hint

### TestCase1592509And1600592: [MSAL] Mooncake: Silent Auth w/o cache w/o MFA w/ Prompt Auto w/ Broker and Single-Tenant App Silent Request with Common Authority Should Fail
- **Covers:** TestCase1592509, TestCase1600592
- **Steps:**
  1. Acquire token interactively for Mooncake account
  2. Advance device clock by 1 day
  3. Acquire token silently after expiring AT
  4. Validate silent token acquisition
  5. Acquire token interactively for single-tenant app
  6. Attempt silent token request with common authority
  7. Validate failure (MsalServiceException)
- **Details:**
  - Combines Mooncake silent auth and single-tenant app silent request failure scenarios

### TestCase497069And833546And850455: Broker Auth for non-joined account - select_account, Multiple Resources, Acquire Token + Silent
- **Covers:** TestCase497069, TestCase833546, TestCase850455
- **Steps:**
  1. Acquire token interactively with Prompt.SELECT_ACCOUNT and login hint
  2. Acquire token interactively again with Prompt.SELECT_ACCOUNT and no login hint
  3. Validate both authentications succeed
  4. Acquire token interactively for resource 1
  5. Acquire token silently for resource 2 (force refresh)
  6. Validate both authentications succeed
  7. Acquire token interactively with Prompt.SELECT_ACCOUNT
  8. Acquire token silently for account (force refresh)
  9. Validate silent token acquisition
- **Details:**
  - Combines select_account, multiple resources, and silent acquisition scenarios for non-joined account

---

## broker/update/ (3 tests)

// These tests have distinct upgrade flows and cannot be combined. List as-is.

### TestCaseUpdateAuthenticator: Update Microsoft Authenticator (LTW)
### TestCaseUpdateCompanyPortal: Update Company Portal (LTW)
### TestCaseUpdateLTW: Update LTW Broker (LTW)

---

## broker/usgov/ (5 tests → 2 combined)

// AI-ERROR: No combinations possible

### TestCase796048And796049: [Non-Joined] Acquire Token with Resource (instance_aware = true/false)
- **Covers:** TestCase796048, TestCase796049
- **Steps:**
  1. Acquire token interactively for a non-joined account with USGov authority and resource.
  2. Parameterize for instance_aware flag (true/false).
  3. Validate token acquisition and returned claims for both cases.
- **Details:**
  - Combines non-joined USGov resource acquisition with and without instance_aware flag.


### TestCase938447And948676And940421: [Joined] Acquire Token with USGov Authority (base, instance_aware, deviceid claim)
- **Covers:** TestCase938447, TestCase948676, TestCase940421
- **Steps:**
  1. Register device (join) with USGov account.
  2. Acquire token interactively for USGov authority (base case).
  3. Acquire token interactively with instance_aware=true.
  4. Acquire token interactively with instance_aware=true and deviceid claim.
  5. Validate token acquisition and returned claims for all cases.
- **Details:**
  - Combines all joined device USGov authority scenarios, including instance_aware and deviceid claim variations.

---

## broker/wpj/ (3 tests)

// These tests have distinct flows and cannot be combined. List as-is.

### TestCase831655: Verify WPJ Cert Installation on Non-Samsung Device with Authenticator
### TestCase833547: Broker Add Account via Account Manager
### TestCase833561: [WPJ] Install WPJ Certificate for Browser Access

---

## msalonly/atpop/ (2 tests → 1 combined)

### TestCase1954181And1954183: Acquire PoP token interactive, silent, and generate SHR (MSAL-only, no broker)
- **Covers:** TestCase1954181, TestCase1954183
- **Steps:**
  1. Acquire PoP token interactively.
  2. Validate AT is PoP.
  3. Acquire PoP token silently.
  4. Validate silent AT is PoP.
  5. Generate SHR (Signed HTTP Request).
  6. Validate SHR is PoP.
- **Details:**
  - Combines PoP token acquisition (interactive/silent) and SHR generation in MSAL-only context, no broker required.

---

## msalonly/basic/ (6 tests → 4 combined)

99274 and 99652 can be combined.
99274 seems very close to 99267. 99267 doesn't match ado item.

### TestCase99267And99274And99652: Interactive Auth with select_account/force_login, with/without consent record (parameterized)
- **Covers:** TestCase99267, TestCase99274, TestCase99652
- **Steps:**
  1. Parameterize for prompt type (select_account, force_login) and consent record (present/absent).
  2. Acquire token interactively with specified prompt and consent state.
  3. Validate authentication result and returned claims.
- **Details:**
  - Combines interactive auth flows for select_account and force_login, with and without consent record.

### TestCase99563: Silent Auth with force_refresh
- **Covers:** TestCase99563
- **Steps:**
  1. Acquire token interactively to populate cache.
  2. Acquire token silently with force_refresh.
  3. Validate silent token acquisition and force_refresh behavior.
- **Details:**
  - Tests silent auth with force_refresh in MSAL-only context.

### TestCase2016158: Single-Tenant App Silent Request with Common Authority Should Fail
- **Covers:** TestCase2016158
- **Steps:**
  1. Attempt silent token request for single-tenant app using common authority.
  2. Validate that the request fails as expected.
- **Details:**
  - Negative test for silent request with common authority in single-tenant app.

### TestCase532736: WebView Fallback When All Browsers Disabled (parameterized)
- **Covers:** TestCase532736
- **Steps:**
  1. Disable all browsers on device.
  2. Attempt interactive authentication.
  3. Validate that MSAL falls back to WebView for authentication.
- **Details:**
  - Tests browser fallback logic in MSAL-only context.

---

## msalonly/crosscloud/ (3 tests → 2 combined)

// AI-ERROR: No combinations possible

### TestCase1420484: Acquire token (Interactive and silent) for cross cloud guest account (parameterized)
- **Covers:** TestCase1420484
- **Steps:**
  1. Parameterized for guest account scenarios.
  2. Acquire token interactively for cross cloud guest account.
  3. Acquire token silently for cross cloud guest account.
  4. Validate token acquisition and returned claims.
- **Details:**
  - Canonical scenario for cross cloud guest account token acquisition.

### TestCase1616315And1616316: AcquireToken from Cross/foreign Cloud after Home Cloud and Home Cloud after Cross/foreign Cloud (parameterized for order)
- **Covers:** TestCase1616315, TestCase1616316
- **Steps:**
  1. Parameterized for acquisition order: (a) Home → Cross/foreign, (b) Cross/foreign → Home.
  2. Acquire token interactively for the first cloud.
  3. Acquire token interactively for the second cloud.
  4. Validate token acquisition, cache, and authority switching for both orders.
- **Details:**
  - Combines both token acquisition order scenarios into a single parameterized test, reducing setup and runtime.

---

## msalonly/usgov/ (4 tests → 2 combined)

// AI-ERROR: I Don't think we can combine any of these 4 tests

### TestCase938365And938367: Interactive auth for USGov account with instance_aware (parameterized for login hint and authority)
- **Covers:** TestCase938365, TestCase938367
- **Steps:**
  1. Parameterize for login hint (present/absent) and authority (common/organizations).
  2. Acquire token interactively for USGov account with instance_aware=true.
  3. Validate token acquisition and returned claims for all parameter combinations.
- **Details:**
  - Combines interactive auth flows for USGov account with instance_aware, covering both login hint and authority variations.

### TestCase938384And938383: Interactive + Silent auth with USGov authority
- **Covers:** TestCase938384, TestCase938383
- **Steps:**
  1. Acquire token interactively for USGov authority.
  2. Acquire token silently with unexpired RT for the same account/authority.
  3. Validate both interactive and silent token acquisition.
- **Details:**
  - Combines interactive and silent auth flows for USGov authority.
