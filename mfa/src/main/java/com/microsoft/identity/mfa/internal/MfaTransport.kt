// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.MfaErrorCode
import com.microsoft.identity.mfa.MfaSdkException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class MfaWireRequest(val endpoint: URI, val headers: Map<String, String>, val body: ByteArray)
internal class MfaWireResponse(val statusCode: Int, val body: ByteArray)
internal interface MfaTransport {
    suspend fun send(request: MfaWireRequest): MfaWireResponse
}

internal class OkHttpMfaTransport : MfaTransport {
    private val client = OkHttpClient.Builder()
        .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
        .cookieJar(CookieJar.NO_COOKIES).cache(null)
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS).callTimeout(45, TimeUnit.SECONDS).build()

    override suspend fun send(request: MfaWireRequest): MfaWireResponse {
        MfaEndpointPolicy.validate(request.endpoint.toString())
        val builder = Request.Builder().url(request.endpoint.toString())
            .post(request.body.toRequestBody("text/xml; charset=utf-8".toMediaType()))
        request.headers.forEach { (key, value) -> builder.header(key, value) }
        val call = client.newCall(builder.build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(
                        MfaSdkException(MfaErrorCode.TRANSPORT_FAILURE))
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            val body = it.body ?: fail(MfaErrorCode.INVALID_RESPONSE)
                            requireMfa(body.contentLength() <= MfaXml.LIMIT)
                            val output = ByteArrayOutputStream()
                            body.byteStream().use { stream ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val size = stream.read(buffer)
                                    if (size < 0) break
                                    requireMfa(output.size() + size <= MfaXml.LIMIT)
                                    output.write(buffer, 0, size)
                                }
                            }
                            MfaWireResponse(it.code, output.toByteArray())
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(
                            if (e is MfaSdkException) e else MfaSdkException(MfaErrorCode.TRANSPORT_FAILURE))
                    }
                }
            })
        }
    }
}

internal class MfaServiceClient(
    private val transport: MfaTransport,
    val appName: String,
    val appVersion: String,
    val osVersion: String,
    val deviceName: String
) {
    suspend fun activation(operation: String, endpoint: URI, fields: List<MfaXml>): MfaXml {
        val method = MfaXml(operation, attributes = mapOf("xmlns" to NAMESPACE), children = fields)
        val envelope = MfaXml("soap:Envelope", attributes = mapOf("xmlns:soap" to SOAP),
            children = listOf(MfaXml("soap:Body", children = listOf(method))))
        val response = send(endpoint, envelope, mapOf("SOAPAction" to "$NAMESPACE/$operation"))
        requireMfa(response.name == "Envelope" && response.namespace == SOAP)
        val body = response.child("Body")
        requireMfa(body.namespace == SOAP && body.children.size == 1)
        if (body.children.any { it.name == "Fault" }) {
            throw MfaSdkException(MfaErrorCode.SERVICE_REJECTED, "SOAPFault")
        }
        val result = body.child(operation + "Response")
        requireMfa(result.namespace == NAMESPACE)
        val outcome = result.value(operation + "Result").trim().lowercase()
        requireMfa(outcome in setOf("true", "false", "1", "0"))
        if (outcome == "false" || outcome == "0") {
            val error = result.children.filter { it.name == "error" }
            requireMfa(error.size <= 1)
            val numeric = error.singleOrNull()?.optional("Code")?.trim()
            throw MfaSdkException(MfaErrorCode.SERVICE_REJECTED,
                numeric?.takeIf { it.matches(Regex("[0-9]{1,10}")) } ?: "Rejected")
        }
        return result
    }

    suspend fun phoneApp(
        operation: String,
        endpoint: URI,
        fields: List<MfaXml>,
        headers: Map<String, String> = emptyMap()
    ): MfaXml {
        val tag = operation.replaceFirstChar { it.lowercaseChar() }
        val body = MfaXml("pfpMessage", attributes = mapOf("version" to "1.6"), children = listOf(
            MfaXml("header", children = listOf(MfaXml("source", children = listOf(
                MfaXml("component", attributes = mapOf("type" to "pfsvc", "role" to "master"),
                    children = listOf(MfaXml("host", attributes = mapOf(
                        "ip" to "", "hostname" to "", "serverId" to ""))))
            )))),
            MfaXml("request", attributes = mapOf("request-id" to UUID.randomUUID().toString(),
                "async" to "0", "response-url" to "", "language" to "en"), children = listOf(
                MfaXml(if (tag.endsWith("Request")) tag else tag + "Request", children = fields)
            ))
        ))
        val result = send(endpoint, body, headers + ("x-ms-mac-action" to operation))
        requireMfa(result.name == "pfpMessage" && result.namespace.isEmpty())
        val response = result.child("response")
        if (response.child("status").attributes["disposition"] != "success") {
            throw MfaSdkException(MfaErrorCode.SERVICE_REJECTED, "Rejected")
        }
        return response.child(tag.removeSuffix("Request") + "Response")
    }

    private suspend fun send(endpoint: URI, body: MfaXml, headers: Map<String, String>): MfaXml {
        MfaEndpointPolicy.validate(endpoint.toString())
        val allHeaders = mapOf(
            "Content-Type" to "text/xml; charset=utf-8",
            "client-request-id" to UUID.randomUUID().toString(),
            "AppName" to appName, "AppVersion" to appVersion
        ) + headers
        requireMfa(allHeaders.all { (key, value) ->
            key.matches(Regex("[A-Za-z0-9-]{1,128}")) && value.length <= 4096 &&
                value.all { it in ' '..'~' }
        })
        val response = transport.send(MfaWireRequest(endpoint, allHeaders,
            body.encoded().toByteArray(Charsets.UTF_8)))
        if (response.statusCode != 200) throw MfaSdkException(MfaErrorCode.TRANSPORT_FAILURE, "HTTP")
        return MfaXml.parse(response.body)
    }

    fun context(guid: String, token: String, proof: String = "", needsDos: Boolean): MfaXml =
        MfaXml("phoneAppContext", children = listOf(
            MfaXml("guid", guid), MfaXml("oathCode", proof),
            MfaXml("needDosPreventer", if (needsDos) "yes" else "no"),
            MfaXml("deviceToken", token), MfaXml("version", appVersion), MfaXml("osVersion", osVersion)
        ))

    companion object {
        const val NAMESPACE = "http://www.phonefactor.com/PfPaWs"
        const val SOAP = "http://schemas.xmlsoap.org/soap/envelope/"
    }
}
