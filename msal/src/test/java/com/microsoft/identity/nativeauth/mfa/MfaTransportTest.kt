// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.mfa

import com.microsoft.identity.nativeauth.mfa.internal.MfaServiceClient
import com.microsoft.identity.nativeauth.mfa.internal.MfaXml
import com.microsoft.identity.nativeauth.mfa.internal.OkHttpMfaTransport
import com.microsoft.identity.nativeauth.mfa.internal.mfaOperation
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class MfaTransportTest {
    @Test
    fun `503 Retry-After zero never replays a mutation despite queued success`() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "0"))
            val success = MfaTestFixture().soap("ActivateNew", listOf(MfaXml("ActivateNewResult", "true")))
            server.enqueue(MockResponse().setResponseCode(200).setBody(String(success.body, Charsets.UTF_8)))
            server.start()
            // This test-only interceptor connects the real transport to a loopback HTTP server.
            // The product endpoint policy and TLS configuration are unchanged.
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().url(server.url("/sas")).build())
            }.build()
            try {
                val service = MfaServiceClient(OkHttpMfaTransport(client), "com.contoso.mfatest", "1", "16", "Synthetic")
                val result = runCatching {
                    mfaOperation {
                        service.activation("ActivateNew", URI("https://${MfaTestFixture.HOST}/PfPaWs.asmx"),
                            listOf(MfaXml("activationParams")))
                    }
                }
                assertTrue(result.exceptionOrNull() is MfaSdkException)
                assertTrue((result.exceptionOrNull() as MfaSdkException).isGeneralError())
                assertEquals(1, server.requestCount)
                assertEquals("POST", server.takeRequest(5, TimeUnit.SECONDS)?.method)
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }
    }
}
