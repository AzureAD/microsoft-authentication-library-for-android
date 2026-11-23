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
}
