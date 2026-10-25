// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import com.microsoft.identity.mfa.internal.MfaActivationParser
import com.microsoft.identity.mfa.internal.MfaEndpointPolicy
import com.microsoft.identity.mfa.internal.MfaProof
import com.microsoft.identity.mfa.internal.MfaXml
import org.junit.Assert.assertEquals
import org.junit.Test

/** Parser, endpoint policy, and proof checks using fixed test inputs without a service transport. */
class MfaProtocolTest {
    private val activation get() = MfaRegistrationParameters.Activation(CODE, "https://$HOST")

    private fun assertCode(code: MfaErrorCode, block: () -> Unit) {
        try {
            block()
            throw AssertionError("Expected $code")
        } catch (error: MfaSdkException) {
            assertEquals(code, error.code)
        }
    }

    @Test fun notificationSuffixDoesNotRelaxActivationOrAuthorityChecks() {
        val host = HOST
        assertEquals("https://$host/pad", MfaEndpointPolicy.notification(host).toString())
        assertEquals("https://$host/pad?/pad",
            MfaEndpointPolicy.notification("$host/pad?").toString())
        for (invalid in listOf("$host.evil.invalid/pad?x=1", "evil.invalid/pad?x=1",
            "$host@evil.invalid/pad?x=1", "user@$host/pad?x=1", "$host:80/pad?x=1",
            "$host./pad?x=1", "$host/a/../pad?x=1", "$host/%70ad?x=1",
            "$host/pad#fragment", "$host\\evil/pad?x=1", "https://$host/pad?x=1")) {
            assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) { MfaEndpointPolicy.notification(invalid) }
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaActivationParser.parse(MfaRegistrationParameters.Activation(
                CODE, "https://$host/PfPaWs.asmx?x=1"))
        }
    }

    @Test fun parsingAndProofAreBounded() {
        val expectedEndpoint = "https://$HOST/PfPaWs.asmx"
        for (scheme in listOf("phonefactor", "msauth")) {
            val parsed = MfaActivationParser.parse(MfaRegistrationParameters.QrCode(
                "$scheme://activate_account?code=000000000&url=" +
                    "https%3A%2F%2Fpoc.mobileappcommunicator.auth.microsoft.com"))
            assertEquals(CODE, parsed.code)
            assertEquals(expectedEndpoint, parsed.endpoint.toString())
        }
        assertEquals(expectedEndpoint, MfaActivationParser.parse(activation).endpoint.toString())
        assertEquals(expectedEndpoint, MfaActivationParser.parse(MfaRegistrationParameters.Activation(
            CODE, "https://$HOST/")).endpoint.toString())
        assertEquals("75A48A19D4CBE100644E8AC1397EEA747A2D33AB",
            MfaProof.validation("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", 1))
        assertEquals("a<&", MfaXml.parse(MfaXml("root", children = listOf(
            MfaXml("value", "a<&"))).encoded().toByteArray()).value("value"))
        assertCode(MfaErrorCode.INVALID_RESPONSE) {
            MfaXml.parse("<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///not-read'>]><a>&x;</a>".toByteArray())
        }
        assertCode(MfaErrorCode.INVALID_RESPONSE) {
            MfaXml.parse("<a><b>1</b><b>2</b></a>".toByteArray()).value("b")
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaEndpointPolicy.validate("https://www.microsoft.com/pad")
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaEndpointPolicy.validate("https://$HOST.example.invalid/pad")
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaEndpointPolicy.validate("https://$HOST/a/../pad")
        }
        assertCode(MfaErrorCode.UNTRUSTED_ENDPOINT) {
            MfaEndpointPolicy.validate("https://$HOST/%70ad")
        }
        assertCode(MfaErrorCode.INVALID_ACTIVATION) {
            MfaActivationParser.parse(MfaRegistrationParameters.QrCode(
                "phonefactor://activate_account?code=a&code=b&url=x"))
        }
        for (authority in listOf("user@activate_account", "activate_account:443", "activate_account.example")) {
            assertCode(MfaErrorCode.INVALID_ACTIVATION) {
                MfaActivationParser.parse(MfaRegistrationParameters.QrCode(
                    "phonefactor://$authority?code=000000000&url=" +
                        "https%3A%2F%2Fpoc.mobileappcommunicator.auth.microsoft.com"))
            }
        }
    }

    private companion object {
        const val HOST = "poc.mobileappcommunicator.auth.microsoft.com"
        const val CODE = "000000000"
    }
}
