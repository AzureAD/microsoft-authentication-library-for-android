# MSAL Automation App Instrumented Test Case Compact List

Test cases in `msalautomationapp` grouped by directory. Shows planned combinations and tests to be dropped

Total change in test count is 115 -> 77 (-33% decrease in total test cases, we will not exactly see a 1:1 decrease in execution time because some of these tests will now run longer)

---

## broker/atpop/ (4) -> (2)
- Combine Non-Joined AtPop
  - TestCase1922511: [Non-Joined] Acquire PoP token interactive followed by Silent
  - TestCase1922527: [Non-Joined] Generate SHR
  - Reasoning: Generate SHR after the silent request
- Combine Joined AtPop
  - TestCase1922513: [Joined] Acquire PoP token interactive followed by Silent
  - TestCase1922515: [Joined] Generate SHR
  - Reasoning: Generate SHR after the silent request

## broker/atpop/update/ (4) -> (0, v5 scenarios no longer applicable)
- Dropped
  - TestCase1922531: [Non-Joined][Update-old-to-V5] Acquire PoP token Silent
  - TestCase1922549: [Non-Joined][Update-old-to-V5] Generate SHR
  - TestCase1922530: [Joined][Update-old-to-V5] Acquire PoP token Silent
  - TestCase1922547: [Joined][Update-old-to-V5] Generate SHR

## broker/brokerapi/ (6) -> (4)
- Combine
  - TestCase1561136: Get Broker Accounts
  - TestCase1561137: Remove Broker Account
- TestCase1561087: BrokerHost Flight Settings
- TestCase1561652: SSO Token Requests
- TestCase1600567: Non-Allowed Broker App API Access
- Drop Test, seems redundant with other DCF covergae in automation and manual pass
  - TestCase2110359: Check DCF Option UI (Join Tenant)

## broker/crosscloud/ (4) (No possibility for combination, flows could be impacted by left over states between AT calls)
- TestCase1400731: [Joined] Guest Support: Interactive and Silent Auth (Cross Cloud)
- TestCase1420494: Acquire token for cross cloud guest account (with broker)
- TestCase1592465: Acquire Token from Cross Cloud after Home Cloud
- TestCase1592510: Acquire Token from Home Cloud after Cross Cloud

## broker/dcf/ (3) -> (1)
- Combine All three dcf tests
  - TestCase2836426: 'Sign In from Other Device' Option Not Available Without Parameter
  - TestCase2828864: Check 'Sign In from Other Device' Option (Azure Cloud)
  - TestCase2828868: Check 'Sign In from Other Device' Option (US Gov)
  - Reasoning: First try without parameter, then try Azure Cloud, then US Gov

## broker/flw/ (7) (No changes, attempts at combination resulted in inconsistent and failing tests)
- TestCase833511: Shared Device Registration with Non-Admin Account should fail
- TestCase833513: End My Shift - End My Shift - In Shared device mode, only account from the same tenant should be able to acquire token.
- TestCase833514: End My Shift - In Shared device mode, an account signed in through App A can be used by App B.
- TestCase833515: End My Shift - In Shared device mode, global sign out should work.
- TestCase833516: End My Shift - In Shared device mode, there can be only one sign-in account.
- TestCase833517: End My Shift - Account Sign Out Notification
- TestCase2495140: End My Shift - In Shared device mode, global sign out should wait/cancel existing silent requests

## broker/foci/ (1) (Only one test, no combinations)
- TestCase833544: FOCI SSO with Outlook and Word (Non-joined)

## broker/joined/ (7) -> (3)
- Combine
  - TestCase1561125: In-line WPJ with DeviceId Claim (Joined)
  - TestCase833558: Broker Delete Account via Account Manager
- Drop, already covered in 1561125
  - TestCase1561171: Prompt.LOGIN (Joined)
- Combine
  - TestCase832430: [Joined][MSAL] Acquire Token + Acquire Token Silent with resource (Prompt.SELECT_ACCOUNT)
  - TestCase1561151: Password Change (Joined)
- Combine
  - TestCase714567: Device Registration via Settings Page
  - TestCase796050: Add Account in Account Chooser Activity

## broker/ltw/ (16) -> (4)
- Keep some basic testing
  - TestCase2584410: Authenticator Has Highest Priority (Auth, LTW, CP) (CLOUD)
  - TestCase2582292: LTW Active Broker Should Not Break SDM MSAL Authenticator Request
  - TestCase2572280: LTW Has Higher Priority Than Company Portal
  - Combine
    - TestCase3029738: Sign in with AAD and MSA account (LTW)
    - TestCase2572249: SSO After LTW Uninstall if Authenticator Present
- Drop, unnecessary testing
  - TestCase2582294: LTW Active Broker Should Not Break Legacy WPJ Authenticator Request
  - TestCase2582297: LTW Active Broker Should Not Break Multiple WPJ API from Legacy Broker Test App
  - TestCase2582290: LTW Active Broker Should Not Break Non-SDM MSAL Authenticator Request
  - TestCase2582291: LTW Active Broker Should Not Break Non-SDM MSAL CP Request
  - TestCase2572283: LTW Has Higher Priority Than Company Portal - Case 2
  - TestCase2572294: Authenticator Has Highest Priority (LTW, Auth, CP)
  - TestCase2584409: Authenticator Has Highest Priority (LTW, CP, Auth)
  - TestCase2584411: Authenticator Has Highest Priority (Auth, CP, LTW)
  - TestCase2584412: Authenticator Has Highest Priority (CP, Auth, LTW)
  - TestCase2584414: Authenticator Has Highest Priority (CP, LTW, Auth)
  - TestCase2571361: SSO if Company Portal Installed After LTW

## broker/ltw/TransferToken/ (1) (Only one test, no combinations)
- TestCase3026421: Transfer Token Generation and Restore

## broker/mam/ (5) -> (3)
- Combine
  - TestCase2516571: TrueMAM: Broker Required for Outlook, CP vs Authenticator
  - TestCase2516967: TrueMAM: Can Use Outlook After Registration and Re-Registration
- TestCase2506936: TrueMAM: Sign In with Teams, Sign Out, and Sign Back In
- TestCase2798415: Shared Device Mode - TrueMAM: Teams Sign In, Sign Out, and Sign In Again
- Drop Test, no need for password reset as we already test it with a cloud account
  - TestCase850457: [MSAL] Password Reset for MAM_CA Account

## broker/mdm/ (2) (No combinations, these don't seem combinable)
- TestCase831126: [Joined][MDM] Device Admin MDM: MDM Account with Microsoft Outlook and Word
- TestCase833526: [Joined][MSAL] Device Admin MDM: Broker Auth for MDM Account + PKeyAuth Flow

## broker/msa/ (4) -> (3)
- Combine
  - TestCase2637829And2637846: [PRTv3] Brokered Auth for MSA Account - Prompt.Login & Acquire Token Silent
  - TestCase2637853: [PRTv3] Brokered Auth for MSA Account - Select_Account, silent with no login hint
- TestCase2637882: [PRTv3] Brokered Auth for MSA Account - Consumers Authority
- TestCase3007768: [Brokered] Sign Up Flow for MSA Accounts

## broker/mwpj/ (11) (TODO: DISCUSS WITH PEDRO)
- TestCase2519783: [MWPJ] Install WPJ Certificate for Browser Access in Both Registrations
- TestCase2519809: [MWPJ] Unregister 2 WPJ Entries
- TestCase2519833: [MWPJ] Get Records by Tenant ID and UPN
- TestCase2521768: [MWPJ] Account with No PRT Uses Non-Joined Flow
- TestCase2521946: [MWPJ] Device Registration Entry Migration (Same UPN)
- TestCase2521960: [MWPJ] Device Registration Entry Migration (Different UPN - Same Tenant)
- TestCase2563653: [MWPJ] Join with MWPJ API and Get Account with Legacy API
- TestCase2563664: [MWPJ] MWPJ Can Access Both Records, Legacy API Can Access Only the First One
- TestCase2563668: [MWPJ] Legacy Entry Should Work with New Broker (Upgrade Scenario)
- TestCase2578879: [MWPJ] Account with No PRT Uses Non-Joined Flow (PKeyAuth Enabled)
- TestCase2579654: [MWPJ] After Entry Migration, PRT Still Usable Without Extra Prompts

## broker/nestedAppAuth/ (5) -> (4)
- Combine
  - TestCase2688459: Nested App Auth Silent Request
  - TestCase2688460: Nested App Interactive Request After Hub Interactive
- TestCase2688462: Nested App's Fresh AT Interactive Succeeds, Silent Fails
- TestCase2688468: Nested App Interactive Token Request After Device is WPJd
- TestCase2703171: Nested App Auth with US Gov Account

## broker/nonjoined/ (9) -> (7)
- Combine
  - TestCase850455: [Non-Joined][MSAL] Acquire Token + Acquire Token Silent (Prompt.SELECT_ACCOUNT)
  - TestCase1561169: [Non-Joined][MSAL] Prompt.LOGIN
  - TestCase1561152: [Non-Joined][MSAL] Password Change (bad_token)
- TestCase497069: Broker Auth for non-joined account - select_account
- TestCase2139526: Acquire Token Silent After Policy Change Should Fail
- TestCase1592509: [MSAL] Mooncake: Silent Auth w/o cache w/o MFA w/ Prompt Auto w/ Broker
- TestCase1600592: [Non-Joined] Single-Tenant App Silent Request with Common Authority Should Fail
- TestCase3139972: [Non-Joined][MSAL] Acquire Token W/ Resource + Acquire Token Silent, no loginhint (Prompt.SELECT_ACCOUNT)
- TestCase833546: [MSAL] Broker Auth for Non-Joined Account - Multiple Resources
- TestCase3139972, TestCase850455, TestCase497069 are very similar but test slighlty different scenarios that all seem applicable

## broker/update/ (3) -> (0, dropping these)
- Dropped:
  - TestCaseUpdateAuthenticator: Update Microsoft Authenticator (LTW)
  - TestCaseUpdateCompanyPortal: Update Company Portal (LTW)
  - TestCaseUpdateLTW: Update LTW Broker (LTW)

## broker/usgov/ (5) (No combinations, each scenario requires fresh state)
- TestCase796048: [USGOV][Broker][Non-Joined] Acquire Token with Resource with instance_aware = true
- TestCase796049: [USGOV][Broker][Non-Joined] Acquire Token with Resource
- TestCase938447: [USGOV][Broker][Joined] Acquire token with USGov Authority
- TestCase940421: [USGOV][Broker][Joined] In-line WPJ/MSAL - acquire token with deviceid claim request, and instance_aware=true
- TestCase948676: [USGOV][Broker][Joined] Acquire token with instance_aware=true

## broker/wpj/ (3) -> (2)
- TestCase833547: Broker Add Account via Account Manager
  - TestCase831655 seems like a duplicate of this one, dropping TestCase831655
- TestCase833561: [WPJ] Install WPJ Certificate for Browser Access

## msalonly/atpop/ (2) -> (1)
- Combine, Acquire PoP token interactive followed by Silent, then Generate SHR
  - TestCase1954181: Acquire PoP token interactive followed by Silent
  - TestCase1954183: Generate SHR without broker

## msalonly/basic/ (6) -> (5)
- Combine, call common authority silent after initial sign ins
  - TestCase99652: Interactive Auth with Force Login for Managed Account (with Consent Record)
  - TestCase2016158: Single-Tenant App Silent Request with Common Authority Should Fail
- TestCase99267: Interactive Auth with select_account (no consent record)
- TestCase99563: Silent Auth with force_refresh
- TestCase99274: Interactive Auth with select_account (with consent record)
- TestCase532736: WebView Fallback When All Browsers Disabled (Parameterized)

## msalonly/crosscloud/ (3) (No combinations, each scenario requires fresh state)
- TestCase1420484: Acquire token (Interactive and silent) for cross cloud guest account (Msal Only, Parameterized)
- TestCase1616315: AcquireToken from Cross/foreign Cloud after acquiring token from home cloud (Parameterized)
- TestCase1616316: AcquireToken from home Cloud after acquiring token from cross/foreign cloud (Parameterized)

## msalonly/usgov/ (4) (These seem distinct, no combinations)
- TestCase938365: Acquire token with instance_aware=true, no login hint, and cloud account, WW common authority
- TestCase938367: Acquire token with instance_aware=true, login hint present, cloud account, WW organizations authority
- TestCase938383: Acquire token silent with unexpired RT with USGov authority
- TestCase938384: Acquire token with USGov Authority
