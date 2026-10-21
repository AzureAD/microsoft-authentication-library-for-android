// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import com.google.protobuf.CodedInputStream
import com.microsoft.identity.mfa.MfaApprovalBlockReason
import com.microsoft.identity.mfa.MfaApprovalBlockReason.*
import com.microsoft.identity.mfa.MfaPolicyProfile
import java.io.IOException
import okio.ByteString.Companion.decodeBase64

/** Independent, bounded reader for the credential-policy fields relevant to this PoC. */
internal object MfaCredentialPolicy {
    private const val MAX_BYTES = 16 * 1024
    private class InvalidPolicy(val reason: MfaApprovalBlockReason) : Exception()
    private enum class Section { ROOT, SIGN_IN, COMPANION_SIGN_IN, METADATA, TIMESTAMP, REPORTING, PLATFORM }

    fun blockedReasons(
        encoded: String?, numberMatching: Boolean,
        profile: MfaPolicyProfile = MfaPolicyProfile.CONSERVATIVE
    ): Set<MfaApprovalBlockReason> {
        MfaDiagnostics.event("credentialPolicy.profile=$profile")
        if (encoded.isNullOrEmpty()) return emptySet()
        return try {
            valid(encoded.length <= ((MAX_BYTES + 2) / 3) * 4 &&
                encoded.matches(Regex("[A-Za-z0-9_+/\\-]+={0,2}")))
            val bytes = encoded.decodeBase64()?.toByteArray() ?: throw InvalidPolicy(MALFORMED_POLICY)
            valid(bytes.isNotEmpty() && bytes.size <= MAX_BYTES)
            val blocked = linkedSetOf<MfaApprovalBlockReason>()
            var hasSignInPolicy = false
            readMessage(bytes, Section.ROOT) { field, wire, input ->
                val nested = message(wire, input)
                when (field) {
                    1 -> metadata(nested)
                    2 -> {
                        hasSignInPolicy = true
                        signInPolicy(nested, numberMatching, blocked)
                    }
                    3 -> reporting(nested)
                    // Validate the companion data even when its requirements belong to another profile.
                    4 -> signInPolicy(nested, numberMatching,
                        if (profile == MfaPolicyProfile.CONSERVATIVE) blocked else linkedSetOf(),
                        Section.COMPANION_SIGN_IN)
                    5 -> readMessage(nested, Section.PLATFORM) { key, type, source ->
                        valid(key == 1, UNKNOWN_POLICY)
                        if (scalar(type, source, 1) == 1) blocked.add(APPROVAL_PIN_REQUIRED)
                    }
                    else -> throw InvalidPolicy(UNKNOWN_POLICY)
                }
            }
            if (!hasSignInPolicy) {
                MfaDiagnostics.event("credentialPolicy.blocked cause=MISSING_SIGN_IN_POLICY")
                blocked.add(UNKNOWN_POLICY)
            }
            blocked
        } catch (error: InvalidPolicy) {
            MfaDiagnostics.event("credentialPolicy.rejected reason=${error.reason.name}")
            setOf(error.reason)
        } catch (_: IOException) {
            // Only bounded in-memory protobuf parsing occurs here; never include its exception text.
            MfaDiagnostics.event("credentialPolicy.rejected reason=MALFORMED_PROTOBUF")
            setOf(MALFORMED_POLICY)
        }
    }

    private fun signInPolicy(
        bytes: ByteArray, numberMatching: Boolean, blocked: MutableSet<MfaApprovalBlockReason>,
        section: Section = Section.SIGN_IN
    ) {
        // Proto defaults are device-based push and enabled, not ordinary MFA push.
        var mode = 0
        readMessage(bytes, section) { field, wire, input ->
            when (field) {
                1 -> scalar(wire, input, 1) // Software TOTP setting does not govern this push decision.
                2 -> mode = scalar(wire, input, 3)
                3, 4, 5, 6 -> {
                    when (scalar(wire, input, 2)) {
                        1 -> {
                            MfaDiagnostics.event(
                                "credentialPolicy.blocked cause=SERVICE_DEFAULT section=$section field=$field")
                            blocked.add(UNKNOWN_POLICY)
                        }
                        2 -> when (field) {
                            3 -> blocked.add(APP_CONTEXT_REQUIRED)
                            4 -> blocked.add(LOCATION_CONTEXT_REQUIRED)
                            5 -> if (!numberMatching) blocked.add(NUMBER_MATCHING_DATA_MISSING)
                            6 -> blocked.add(APP_LOCK_REQUIRED)
                        }
                    }
                }
                7 -> scalar(wire, input, 2) // Companion eligibility does not grant this client approval.
                8 -> if (scalar(wire, input, 2) != 0) blocked.add(MFA_DISABLED)
                else -> throw InvalidPolicy(UNKNOWN_POLICY)
            }
        }
        if (mode != 1 && mode != 2) blocked.add(UNSUPPORTED_MODE)
    }

    private fun metadata(bytes: ByteArray) = readMessage(bytes, Section.METADATA) { field, wire, input ->
        when (field) {
            1 -> {
                valid(wire == 2)
                input.readStringRequireUtf8()
            }
            2 -> readMessage(message(wire, input), Section.TIMESTAMP) { key, type, source ->
                valid(type == 0)
                when (key) {
                    1 -> source.readInt64()
                    2 -> valid(source.readInt32() in 0..999_999_999)
                    else -> throw InvalidPolicy(UNKNOWN_POLICY)
                }
            }
            else -> throw InvalidPolicy(UNKNOWN_POLICY)
        }
    }

    private fun reporting(bytes: ByteArray) = readMessage(bytes, Section.REPORTING) { field, wire, input ->
        when (field) {
            1 -> scalar(wire, input, 2)
            2 -> scalar(wire, input, Int.MAX_VALUE)
            else -> throw InvalidPolicy(UNKNOWN_POLICY)
        }
    }

    private fun readMessage(
        bytes: ByteArray, section: Section, consume: (Int, Int, CodedInputStream) -> Unit
    ) {
        val input = CodedInputStream.newInstance(bytes)
        val seen = HashSet<Int>()
        while (!input.isAtEnd) {
            val tag = input.readTag()
            val field = tag ushr 3
            val safeField = if (field in 1..64) field.toString() else "OTHER"
            MfaDiagnostics.event("credentialPolicy.field section=$section field=$safeField wire=${tag and 7}")
            valid(field > 0 && seen.size < 64 && seen.add(field))
            consume(field, tag and 7, input)
        }
    }

    private fun message(wire: Int, input: CodedInputStream): ByteArray {
        valid(wire == 2)
        return input.readByteArray()
    }

    private fun scalar(wire: Int, input: CodedInputStream, max: Int): Int {
        valid(wire == 0)
        val value = input.readUInt64()
        if (max <= 3) {
            val category = if (value in 0..max.toLong()) value.toString() else "OTHER"
            MfaDiagnostics.event("credentialPolicy.enum value=$category")
        }
        valid(value in 0..max.toLong(), UNKNOWN_POLICY)
        return value.toInt()
    }

    private fun valid(value: Boolean, reason: MfaApprovalBlockReason = MALFORMED_POLICY) {
        if (!value) throw InvalidPolicy(reason)
    }
}
