// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.MfaSdkException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URISyntaxException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException

internal object MfaDiagnostics {
    fun event(message: String) {
        if (MfaDiagnosticLog.enabled) MfaDiagnosticLog.write(message)
    }

    fun failure(operation: String, error: Exception) {
        if (MfaDiagnosticLog.enabled) event("$operation.failed ${failureSummary(error)}")
    }

    suspend fun <T> trace(operation: String, block: suspend () -> T): T {
        val started = System.nanoTime()
        event("$operation.start")
        try {
            return block().also { event("$operation.complete elapsedMs=${elapsed(started)}") }
        } catch (error: Exception) {
            failure(operation, error)
            event("$operation.end elapsedMs=${elapsed(started)}")
            throw error
        }
    }

    fun elapsed(started: Long): Long = (System.nanoTime() - started) / 1_000_000

    fun failureSummary(error: Exception): String {
        val category = when (error) {
            is MfaSdkException -> error.code.name
            is CancellationException -> "CANCELLED"
            is SocketTimeoutException -> "SOCKET_TIMEOUT"
            is UnknownHostException -> "DNS_FAILURE"
            is SSLException -> "TLS_FAILURE"
            is IOException -> "IO_FAILURE"
            else -> "OTHER_EXCEPTION"
        }
        val service = (error as? MfaSdkException)?.serviceCode ?: "NONE"
        val source = error.stackTrace.firstOrNull {
            it.className.startsWith("com.microsoft.identity.mfa.") &&
                it.fileName in setOf("MfaSdkManager.kt", "MfaProtocol.kt", "MfaTransport.kt",
                    "MfaStore.kt") && it.methodName != "fail" &&
                !it.methodName.startsWith("requireMfa")
        }?.let { "${it.fileName}:${it.lineNumber}" } ?: "UNKNOWN"
        return "code=$category service=$service source=$source"
    }

    fun response(operation: String, root: MfaXml) {
        if (MfaDiagnosticLog.enabled) event(responseSummary(operation, root))
    }

    fun responseSummary(operation: String, root: MfaXml): String {
        fun MfaXml.one(name: String) = children.singleOrNull { it.name == name }
        fun field(parent: MfaXml?, name: String): String {
            val matches = parent?.children?.filter { it.name == name }.orEmpty()
            val value = matches.singleOrNull()
            return "$name.count=${matches.size},blank=${value?.text?.isBlank() == true}," +
                "children=${value?.children?.size ?: 0}"
        }
        return when (operation) {
            "ActivateNew", "ConfirmActivation" -> {
                val body = root.one("Body")
                val result = body?.one(operation + "Response")
                val outcome = result?.one(operation + "Result")
                val category = when (outcome?.text?.trim()?.lowercase(Locale.ROOT)) {
                    null -> "MISSING"
                    "" -> "EMPTY"
                    "true", "1" -> "TRUE"
                    "false", "0" -> "FALSE"
                    "yes" -> "YES"
                    "no" -> "NO"
                    else -> "OTHER"
                }
                val info = result?.one("activationInfo")
                val fields = listOf("Username", "OathTokenSecretKey", "ConfirmationCode",
                    "TenantId", "AzureObjectId").joinToString(" ") { field(info, it) }
                "$operation.schema envelope=${root.name == "Envelope"} " +
                    "soapNamespace=${root.namespace == MfaServiceClient.SOAP} " +
                    "bodyNamespace=${body?.namespace == MfaServiceClient.SOAP} " +
                    "bodyChildren=${body?.children?.size ?: 0} " +
                    "resultNamespace=${result?.namespace == MfaServiceClient.NAMESPACE} " +
                    "outcome=$category outcomeChildren=${outcome?.children?.size ?: 0} " +
                    "activationInfo=${info != null} $fields"
            }
            "PhoneAppValidateDeviceToken" -> {
                val result = root.one("response")?.one("phoneAppValidateDeviceTokenResponse")
                val fields = listOf("username", "accountName", "groupKey", "dosPreventer",
                    "accountValidationResults").joinToString(" ") { field(result, it) }
                "$operation.schema $fields"
            }
            "PhoneAppAuthentication" -> {
                val result = root.one("response")?.one("phoneAppAuthenticationResponse")
                val info = result?.one("phoneAppInfo")
                val names = listOf("phoneAppDetailId", "tenantId", "azureObjectId", "dosPreventer",
                    "replicationScope", "firstEntropyChallenge", "returnLocationData", "isSilent",
                    "isAppLockRequired", "pinChangeRequired", "userCredentialPolicy", "adminPolicies")
                val top = names.joinToString(" ") { field(result, it) }
                val nested = names.joinToString(" ") { field(info, it) }
                val mode = when (info?.attributes?.get("mode")) {
                    "standard" -> "STANDARD"
                    "pin" -> "PIN"
                    null, "" -> "EMPTY"
                    else -> "OTHER"
                }
                "$operation.schema mode=$mode root=[$top] phoneAppInfo=[$nested]"
            }
            else -> "response.schema operation=OTHER"
        }
    }

    fun xmlFieldFailure(expected: String, matches: Int, blank: Boolean = false) {
        val field = expected.takeIf {
            it in setOf("Envelope", "Body", "ActivateNewResponse", "ActivateNewResult",
                "ConfirmActivationResponse", "ConfirmActivationResult", "activationInfo",
                "Username", "OathTokenSecretKey", "ConfirmationCode", "TenantId", "AzureObjectId",
                "phoneAppDetailId", "response", "status", "phoneAppValidateDeviceTokenResponse",
                "username", "accountName", "dosPreventer")
        } ?: "OTHER"
        event("xml.field expected=$field matches=$matches blank=$blank")
    }

    // Describe input shape only. Even an apparently harmless host/path can contain user data.
    fun endpointSummary(value: String): String {
        if (value.length > 4096) return "shape=OVERSIZED"
        val full = value.contains("://")
        val uri = try {
            URI(if (full) value else "https://$value")
        } catch (_: URISyntaxException) {
            return "shape=MALFORMED fullUrl=$full"
        }
        val host = uri.host?.lowercase(Locale.ROOT)
        val zone = when {
            host == null -> "NONE"
            host == "mobileappcommunicator.auth.microsoft.com" ||
                host.endsWith(".mobileappcommunicator.auth.microsoft.com") -> "MAC"
            host == "phonefactor.net" || host.endsWith(".phonefactor.net") -> "PHONEFACTOR"
            else -> "OTHER"
        }
        val port = when (uri.port) { -1 -> "DEFAULT"; 443 -> "HTTPS"; else -> "OTHER" }
        return "shape=${if (full) "URL" else "HOST"} zone=$zone " +
            "https=${uri.scheme == "https"} port=$port path=${!uri.rawPath.isNullOrEmpty()} " +
            "query=${uri.rawQuery != null} fragment=${uri.rawFragment != null} " +
            "userinfo=${uri.rawUserInfo != null} encoded=${value.contains('%')} " +
            "trailingDot=${host?.endsWith('.') == true}"
    }
}
