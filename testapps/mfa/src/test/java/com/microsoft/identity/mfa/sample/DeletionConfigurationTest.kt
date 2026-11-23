// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import org.junit.Assert.*
import org.junit.Test

class DeletionConfigurationTest {
    private val config = """{
      "client_id":"11111111-1111-1111-1111-111111111111",
      "redirect_uri":"msauth://sample.test/a%2Bb%3D",
      "account_mode":"MULTIPLE","authorization_user_agent":"BROWSER",
      "broker_redirect_uri_registered":false,
      "authorities":[{"type":"AAD","audience":{"type":"AzureADMyOrg",
      "tenant_id":"22222222-2222-2222-2222-222222222222"}}]
    }"""

    @Test fun requiresMatchingPackageAndSigningAndExplicitBrowserMode() {
        assertNull(deletionConfigurationProblem(config, "sample.test", setOf("a+b=")))
        assertNotNull(deletionConfigurationProblem(config, "another.app", setOf("a+b=")))
        assertNotNull(deletionConfigurationProblem(config, "sample.test", setOf("other")))
        for (bad in listOf(config.replace("BROWSER", "WEBVIEW"),
            config.replace("MULTIPLE", "SINGLE"),
            config.replace(":false", ":true"),
            config.replace("AzureADMyOrg", "AzureADandPersonalMicrosoftAccount"),
            config.replace("\"client_id\"", "\"client_secret\""), "{}")) {
            assertNotNull(deletionConfigurationProblem(bad, "sample.test", setOf("a+b=")))
        }
    }

    private val compatibilityConfig = config
        .replace("11111111-1111-1111-1111-111111111111", "4813382a-8fa7-425e-ab75-3b753aab3abb")
        .replace("sample.test/a%2Bb%3D", "com.azure.authenticator/N1jdcbbnKDr0LaFZlqdhXgm2luE%3D")
        .replace("\"AzureADMyOrg\"", "\"AzureADMultipleOrgs\"")
        .replace(",\n      \"tenant_id\":\"22222222-2222-2222-2222-222222222222\"", "")

    @Test fun authenticatorCompatibilityRequiresExplicitModeAndDocumentedDevelopmentIdentity() {
        val hashes = setOf("N1jdcbbnKDr0LaFZlqdhXgm2luE=")
        assertNull(deletionConfigurationProblem(compatibilityConfig, "com.azure.authenticator", hashes, true))
        assertNotNull(deletionConfigurationProblem(compatibilityConfig, "com.azure.authenticator", hashes))
        assertNotNull(deletionConfigurationProblem(compatibilityConfig, "com.azure.authenticator", setOf("other"), true))
        assertNotNull(deletionConfigurationProblem(compatibilityConfig.replace(
            "4813382a-8fa7-425e-ab75-3b753aab3abb", "11111111-1111-1111-1111-111111111111"),
            "com.azure.authenticator", hashes, true))
    }

    @Test fun organizationsAuthorityIsBoundToStoredTenantOnlyForCompatibility() {
        val tenant = "https://login.microsoftonline.com/22222222-2222-2222-2222-222222222222"
        assertEquals(tenant, deletionAcquisitionAuthority("https://login.microsoftonline.com/organizations", tenant, true))
        assertEquals(tenant, deletionAcquisitionAuthority(tenant, tenant, false))
        for ((configured, requested, compatibility) in listOf(
            Triple("https://login.microsoftonline.com/organizations", tenant, false),
            Triple("https://evil.invalid/organizations", tenant, true),
            Triple("https://login.microsoftonline.com/organizations", "https://evil.invalid/tenant", true),
            Triple("https://login.microsoftonline.com/organizations", "$tenant?x=1", true),
            Triple("https://login.microsoftonline.com/organizations", "https://login.microsoftonline.com/common", true))) {
            try {
                deletionAcquisitionAuthority(configured, requested, compatibility)
                fail("Expected configuration rejection")
            } catch (_: com.microsoft.identity.mfa.MfaDeletionException) { }
        }
    }
}
