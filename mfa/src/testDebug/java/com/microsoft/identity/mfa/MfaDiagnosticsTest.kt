// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import com.microsoft.identity.mfa.internal.MfaDiagnostics
import com.microsoft.identity.mfa.internal.MfaXml
import java.net.SocketTimeoutException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MfaDiagnosticsTest {
    @Test fun responseDiagnosticsExposeShapeWithoutValuesOrUnknownNames() {
        val root = MfaXml.parse(("""
            <Envelope xmlns="http://schemas.xmlsoap.org/soap/envelope/"><Body>
            <ActivateNewResponse xmlns="http://www.phonefactor.com/PfPaWs">
            <ActivateNewResult>true</ActivateNewResult>
            <activationInfo><Username>private-user@example.invalid</Username>
            <OathTokenSecretKey>secret-token</OathTokenSecretKey>
            <secret-field>secret-value</secret-field></activationInfo>
            </ActivateNewResponse></Body></Envelope>
        """).trimIndent().toByteArray())
        val summary = MfaDiagnostics.responseSummary("ActivateNew", root)
        assertTrue(summary.contains("soapNamespace=true"))
        assertTrue(summary.contains("resultNamespace=true outcome=TRUE"))
        assertTrue(summary.contains("Username.count=1,blank=false"))
        assertFalse(summary.contains("private-user"))
        assertFalse(summary.contains("secret-"))
        assertFalse(MfaDiagnostics.responseSummary("secret-operation", root).contains("secret-"))
    }

    @Test fun endpointDiagnosticsDescribeShapeWithoutRetainingInput() {
        val bare = MfaDiagnostics.endpointSummary("sample.phonefactor.net")
        assertTrue(bare.contains("shape=HOST zone=PHONEFACTOR"))
        val full = MfaDiagnostics.endpointSummary(
            "https://private-user:secret@sample.phonefactor.net/pad?token=secret#private")
        assertTrue(full.contains("shape=URL zone=PHONEFACTOR"))
        assertTrue(full.contains("path=true query=true fragment=true userinfo=true"))
        for (value in listOf(bare, full,
            MfaDiagnostics.endpointSummary("https://private-user.example.invalid/secret"),
            MfaDiagnostics.endpointSummary("malformed secret%"),
            MfaDiagnostics.endpointSummary("secret".repeat(1000)))) {
            for (secret in listOf("sample.phonefactor.net", "private-user", "secret", "example.invalid")) {
                assertFalse(value.contains(secret))
            }
        }
        assertTrue(MfaDiagnostics.endpointSummary("phonefactor.net.evil.invalid").contains("zone=OTHER"))
    }

    @Test fun errorDiagnosticsDoNotEmitMessagesOrUnboundedServiceCodes() {
        val sdk = MfaDiagnostics.failureSummary(
            MfaSdkException(MfaErrorCode.TRANSPORT_FAILURE, "secret-service-body"))
        assertTrue(sdk.contains("code=TRANSPORT_FAILURE service=NONE"))
        assertFalse(sdk.contains("secret"))
        val transport = MfaDiagnostics.failureSummary(SocketTimeoutException("secret-token"))
        assertTrue(transport.contains("code=SOCKET_TIMEOUT"))
        assertFalse(transport.contains("secret"))
    }
}
