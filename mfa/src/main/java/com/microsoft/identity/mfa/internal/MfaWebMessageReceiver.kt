// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.microsoft.identity.mfa.MfaEnrollmentHandoff
import com.microsoft.identity.mfa.MfaErrorCode
import com.microsoft.identity.mfa.MfaHandoffDisposition
import com.microsoft.identity.mfa.MfaHandoffReceipt
import com.microsoft.identity.mfa.MfaHandoffSource
import java.io.StringReader
import java.net.URI
import java.util.Locale

internal class MfaWebMessageReceiver(
    private val handoff: MfaEnrollmentHandoff,
    origins: Set<String>
) {
    val allowedOrigins: Set<String> = origins.map {
        origin(it) ?: fail(MfaErrorCode.INVALID_CONFIGURATION)
    }.toSet().also { requireMfa(it.size in 1..16, MfaErrorCode.INVALID_CONFIGURATION) }

    fun receive(message: String?, sourceOrigin: String, isMainFrame: Boolean): MfaHandoffReceipt {
        if (!isMainFrame || origin(sourceOrigin) !in allowedOrigins) return rejected()
        if (message == null || message.length > 16384 || message.toByteArray(Charsets.UTF_8).size > 16384) {
            return rejected()
        }
        return try {
            val values = linkedMapOf<String, String>()
            JsonReader(StringReader(message)).use { reader ->
                reader.isLenient = false
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    requireMfa(name in setOf("version", "type", "activationLink") && name !in values,
                        MfaErrorCode.INVALID_ACTIVATION)
                    requireMfa(reader.peek() == if (name == "version") JsonToken.NUMBER else JsonToken.STRING,
                        MfaErrorCode.INVALID_ACTIVATION)
                    values[name] = reader.nextString()
                }
                reader.endObject()
                requireMfa(reader.peek() == JsonToken.END_DOCUMENT && values.size == 3 &&
                    values["version"] == "1" && values["type"] == "activation", MfaErrorCode.INVALID_ACTIVATION)
            }
            handoff.stage(values.getValue("activationLink"), MfaHandoffSource.WEBVIEW)
        } catch (_: Exception) { rejected() }
    }

    private fun rejected() =
        MfaHandoffReceipt(MfaHandoffDisposition.REJECTED, errorCode = MfaErrorCode.INVALID_ACTIVATION)

    private fun origin(value: String): String? = try {
        val uri = URI(value)
        if (value.length > 512 || uri.scheme != "https" || uri.host == null || uri.rawUserInfo != null ||
            uri.rawQuery != null || uri.rawFragment != null || uri.rawPath.orEmpty().isNotEmpty() ||
            uri.port !in setOf(-1, 443) || uri.host.endsWith(".") || value.contains("*")) null
        else "https://${uri.host.lowercase(Locale.ROOT)}"
    } catch (_: Exception) { null }
}
