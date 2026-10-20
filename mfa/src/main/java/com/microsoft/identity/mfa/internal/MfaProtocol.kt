// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Protocol adapted from public MSAL for Apple, commit 427db06c893b7ea229a3a19dee1cb0011fbf3777.
package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.MfaErrorCode
import com.microsoft.identity.mfa.MfaRegistrationParameters
import com.microsoft.identity.mfa.MfaSdkException
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

internal fun fail(code: MfaErrorCode): Nothing = throw MfaSdkException(code)
internal fun requireMfa(value: Boolean, code: MfaErrorCode = MfaErrorCode.INVALID_RESPONSE) {
    if (!value) fail(code)
}

internal object MfaEndpointPolicy {
    // Service-specific commercial DNS zones only; never trust arbitrary *.microsoft.com.
    private val zones = setOf("mobileappcommunicator.auth.microsoft.com", "phonefactor.net")

    fun validate(value: String): URI {
        try {
            val uri = URI(value)
            val host = uri.host?.lowercase(Locale.ROOT) ?: fail(MfaErrorCode.UNTRUSTED_ENDPOINT)
            // Android and host JVMs can represent a host-only URI path as null or empty.
            val rawPath = uri.rawPath.orEmpty()
            val path = uri.path.orEmpty()
            requireMfa(value.length <= 4096 && uri.scheme == "https" &&
                uri.rawUserInfo == null && uri.rawFragment == null && uri.rawQuery == null &&
                uri.port in setOf(-1, 443) && !host.endsWith(".") &&
                zones.any { host == it || host.endsWith(".$it") } &&
                !rawPath.contains("%") && !path.contains("\\") &&
                uri.normalize().path.orEmpty() == path, MfaErrorCode.UNTRUSTED_ENDPOINT)
            return uri
        } catch (e: MfaSdkException) {
            throw e
        } catch (_: Exception) {
            fail(MfaErrorCode.UNTRUSTED_ENDPOINT)
        }
    }

    fun notification(host: String): URI {
        requireMfa(host.isNotEmpty() && host.none { it in "/\\@:?#%" },
            MfaErrorCode.UNTRUSTED_ENDPOINT)
        return validate("https://$host/pad")
    }
}

internal class MfaActivation(val code: String, val endpoint: URI) {
    val digest: String get() = MfaProof.digest("$code\u0000$endpoint")
}

internal object MfaActivationParser {
    fun parse(input: MfaRegistrationParameters): MfaActivation {
        val pair = when (input) {
            is MfaRegistrationParameters.Activation -> input.code to input.url
            is MfaRegistrationParameters.QrCode -> qr(input.text)
        }
        val code = pair.first.trim()
        requireMfa(code.isNotBlank() && code.toByteArray().size <= 1024 &&
            code.none { it.isISOControl() }, MfaErrorCode.INVALID_ACTIVATION)
        val base = MfaEndpointPolicy.validate(pair.second.trim())
        val endpoint = if (base.path.orEmpty().substringAfterLast("/").equals("PfPaWs.asmx", true)) base
        else MfaEndpointPolicy.validate(base.toString().trimEnd('/') + "/PfPaWs.asmx")
        return MfaActivation(code, endpoint)
    }

    private fun qr(text: String): Pair<String, String> {
        try {
            requireMfa(text.toByteArray().size <= 8192, MfaErrorCode.INVALID_ACTIVATION)
            val uri = URI(text.trim())
            requireMfa(uri.rawUserInfo == null && uri.fragment == null, MfaErrorCode.INVALID_ACTIVATION)
            val fields = LinkedHashMap<String, String>()
            for (part in (uri.rawQuery ?: fail(MfaErrorCode.INVALID_ACTIVATION)).split("&")) {
                val pieces = part.split("=", limit = 2)
                requireMfa(pieces.size == 2, MfaErrorCode.INVALID_ACTIVATION)
                val key = URLDecoder.decode(pieces[0], "UTF-8")
                val value = URLDecoder.decode(pieces[1], "UTF-8")
                requireMfa(fields.put(key, value) == null, MfaErrorCode.INVALID_ACTIVATION)
            }
            val legacy = uri.scheme?.lowercase(Locale.ROOT) in setOf("phonefactor", "msauth") &&
                uri.rawAuthority == "activate_account" && uri.port == -1 &&
                uri.path.orEmpty() in setOf("", "/")
            val commercial = uri.scheme == "https" && uri.host == "login.microsoftonline.com" &&
                uri.port in setOf(-1, 443) && uri.path == "/authenticatorApp/activateAccount" &&
                fields["accountType"] == "mfa"
            requireMfa(legacy || commercial, MfaErrorCode.INVALID_ACTIVATION)
            return (fields["code"] ?: fail(MfaErrorCode.INVALID_ACTIVATION)) to
                (fields["url"] ?: fail(MfaErrorCode.INVALID_ACTIVATION))
        } catch (e: MfaSdkException) {
            throw e
        } catch (_: Exception) {
            fail(MfaErrorCode.INVALID_ACTIVATION)
        }
    }
}

internal class MfaXml(
    val name: String,
    val text: String = "",
    val attributes: Map<String, String> = emptyMap(),
    val children: List<MfaXml> = emptyList(),
    val namespace: String = ""
) {
    fun child(name: String): MfaXml =
        children.filter { it.name == name }.singleOrNull() ?: fail(MfaErrorCode.INVALID_RESPONSE)

    fun value(name: String): String = child(name).let {
        requireMfa(it.children.isEmpty())
        it.text
    }

    fun optional(name: String): String? = if (children.none { it.name == name }) null else value(name)
    fun required(name: String): String = value(name).also { requireMfa(it.isNotBlank()) }

    fun encoded(): String {
        requireMfa(name.matches(Regex("[A-Za-z_][A-Za-z0-9_.:-]*")))
        val attrs = attributes.entries.sortedBy { it.key }.joinToString("") {
            requireMfa(it.key.matches(Regex("[A-Za-z_][A-Za-z0-9_.:-]*")))
            " ${it.key}=\"${escape(it.value)}\""
        }
        return "<$name$attrs>${escape(text)}${children.joinToString("") { it.encoded() }}</$name>"
    }

    companion object {
        const val LIMIT = 1_048_576
        private fun escape(text: String): String {
            requireMfa(text.none { it.isISOControl() && it !in "\t\r\n" })
            return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;")
        }

        fun parse(bytes: ByteArray): MfaXml {
            try {
                requireMfa(bytes.size <= LIMIT)
                val decoded = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
                requireMfa(!decoded.contains("<!DOCTYPE", true) && !decoded.contains("<!ENTITY", true))
                val factory = SAXParserFactory.newInstance().apply { isNamespaceAware = true }
                val reader = factory.newSAXParser().xmlReader
                // Android's SAX implementation supports disabling both entity features, but not
                // Xerces-specific disallow-doctype. UTF-8 lexical rejection above covers the DTD.
                reader.setFeature("http://xml.org/sax/features/external-general-entities", false)
                reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                val handler = TreeHandler()
                reader.contentHandler = handler
                reader.errorHandler = handler
                reader.setEntityResolver { _, _ -> throw SAXException("External entity prohibited") }
                reader.parse(InputSource(ByteArrayInputStream(bytes)).apply { encoding = "UTF-8" })
                return handler.root ?: fail(MfaErrorCode.INVALID_RESPONSE)
            } catch (e: MfaSdkException) {
                throw e
            } catch (_: Exception) {
                fail(MfaErrorCode.INVALID_RESPONSE)
            }
        }
    }

    private class TreeHandler : DefaultHandler() {
        private class Node(val name: String, val namespace: String, val attrs: Map<String, String>) {
            val text = StringBuilder()
            val children = ArrayList<MfaXml>()
        }
        private val stack = ArrayList<Node>()
        private var count = 0
        var root: MfaXml? = null
        override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
            requireMfa(++count <= 4096 && stack.size < 32 && root == null)
            val values = LinkedHashMap<String, String>()
            for (i in 0 until attrs.length) {
                requireMfa(values.put(attrs.getLocalName(i), attrs.getValue(i)) == null)
            }
            stack.add(Node(localName, uri, values))
        }
        override fun characters(ch: CharArray, start: Int, length: Int) {
            stack.lastOrNull()?.text?.append(ch, start, length)
        }
        override fun endElement(uri: String, localName: String, qName: String) {
            val node = stack.removeAt(stack.lastIndex)
            val result = MfaXml(node.name, node.text.toString(), node.attrs, node.children, node.namespace)
            if (stack.isEmpty()) root = result else stack.last().children.add(result)
        }
        override fun error(e: org.xml.sax.SAXParseException) { throw e }
        override fun fatalError(e: org.xml.sax.SAXParseException) { throw e }
    }
}

internal object MfaProof {
    fun counter(): Long = System.currentTimeMillis() / 30_000
    fun digest(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

    fun validation(secret: String, counter: Long): String {
        requireMfa(counter >= 0 && secret.isNotEmpty() && secret.length <= 1024)
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val key = ArrayList<Byte>()
        var buffer = 0
        var bits = 0
        var padding = false
        for (character in secret.uppercase(Locale.ROOT)) {
            if (character == '=') { padding = true; continue }
            val index = alphabet.indexOf(character)
            requireMfa(!padding && index >= 0)
            buffer = (buffer shl 5) or index
            bits += 5
            if (bits >= 8) {
                bits -= 8
                key.add(((buffer shr bits) and 255).toByte())
            }
            buffer = buffer and ((1 shl bits) - 1)
        }
        requireMfa(key.isNotEmpty() && buffer == 0)
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(), "HmacSHA1"))
        return mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
            .joinToString("") { "%02X".format(it.toInt() and 255) }
    }
}
