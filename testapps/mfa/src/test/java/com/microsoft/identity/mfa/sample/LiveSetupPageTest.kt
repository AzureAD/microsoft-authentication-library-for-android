// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.MfaRegistrationParameters
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

class LiveSetupPageTest {
    private val origin = "http://127.0.0.1:54321"
    private val path = "/setup/${"a".repeat(64)}"

    @Test
    fun acceptsAppOriginatedPageNavigationWithoutFetchUserOrOrigin() {
        val navigation = "Sec-Fetch-Site: cross-site\r\n" +
            "Sec-Fetch-Mode: navigate\r\nSec-Fetch-Dest: document\r\n"
        assertEquals(path, read(
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n" +
                "Referer: android-app://com.azure.authenticator\r\n$navigation"
        ))
        assertEquals(path, read(
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n${navigation}Sec-Fetch-User: ?1\r\n"
        ))
    }

    @Test
    fun crossSiteNavigationExceptionDoesNotPermitForeignOriginOrStatusAccess() {
        val navigation = "Sec-Fetch-Site: cross-site\r\n" +
            "Sec-Fetch-Mode: navigate\r\nSec-Fetch-Dest: document\r\n"
        listOf(
            "GET $path/status HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n$navigation",
            "GET $path HTTP/1.1\r\nHost: foreign.invalid\r\n$navigation",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n${navigation}Origin: https://foreign.invalid\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n${navigation}Origin: null\r\n",
            "GET $path?value=1 HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n$navigation",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nSec-Fetch-Site: cross-site\r\nSec-Fetch-Mode: navigate\r\nSec-Fetch-Dest: iframe\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nSec-Fetch-Site: cross-site\r\nSec-Fetch-Mode: cors\r\nSec-Fetch-Dest: empty\r\n",
            "GET $path/status HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nSec-Fetch-Site: cross-site\r\nSec-Fetch-Mode: cors\r\nSec-Fetch-Dest: empty\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n${navigation}Content-Length: 1\r\n",
            "POST $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n$navigation"
        ).forEach { assertNull(read(it)) }
    }

    @Test
    fun acceptsOnlyExactGetResourcesWithLocalHostAndOrigin() {
        assertEquals(path, read("GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n"))
        assertEquals("$path/status", read(
            "GET $path/status HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nOrigin: $origin\r\n"
        ))
        listOf(
            "POST $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n",
            "GET $path HTTP/1.1\r\nHost: attacker.invalid\r\n",
            "GET $path HTTP/1.1\r\nHost: localhost:54321\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nOrigin: https://foreign.invalid\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nOrigin: null\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nSec-Fetch-Site: cross-site\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nContent-Length: 1\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nTransfer-Encoding: chunked\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nHost: 127.0.0.1:54321\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nExpect: 100-continue\r\n",
            "GET $path?value=1 HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n",
            "GET $origin$path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n",
            "GET /../file HTTP/1.1\r\nHost: 127.0.0.1:54321\r\n",
            "GET $path HTTP/1.1\r\nHost: 127.0.0.1:54321\r\nX-Extra: ${"x".repeat(8192)}\r\n"
        ).forEach { assertNull(read(it)) }
    }

    @Test
    fun pageAndStatusNeverContainNativeActivationInput() {
        val marker = "native-only-unit-test-marker"
        val sessions = LiveSetupSession { 0 }
        for (source in SetupSource.values()) {
            sessions.clear()
            val request = requireNotNull(sessions.prepare(
                MfaRegistrationParameters.Activation(marker, "https://native-only.invalid/$marker"), source
            ))
            val page = SetupPage.render(request, "com.example.authorized")
            assertFalse(page.body.contains(marker))
            assertFalse(page.body.contains("native-only.invalid"))
            assertTrue(page.body.contains("not production portal continuation"))
            assertTrue(page.body.contains("Set up on this device"))
            if (source == SetupSource.BROWSER) {
                assertTrue(page.body.contains("intent://enroll/${request.id}"))
                assertTrue(page.body.contains("package=com.example.authorized;end"))
            } else {
                assertTrue(page.body.contains(request.launchUri))
                assertFalse(page.body.contains("intent://"))
            }
            for (phase in SetupPhase.values()) {
                val status = SetupPage.status(request.copy(phase = phase)).body
                assertFalse(status.contains(marker))
                assertFalse(status.contains("native-only.invalid"))
                assertFalse(status.contains(request.id))
            }
        }
    }

    private fun read(head: String): String? = SetupHttpRequest.read(
        ByteArrayInputStream((head + "\r\n").toByteArray(Charsets.US_ASCII)), origin
    )
}
