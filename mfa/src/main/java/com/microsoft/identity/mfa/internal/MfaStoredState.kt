// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.mfa.MfaErrorCode
import com.microsoft.identity.mfa.MfaRegistrationResult

// Keep all Gson DTOs nested under this class for precise consumer shrinking rules.
internal data class MfaStoredState(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("token") val token: String? = null,
    @SerializedName("accounts") val accounts: List<Account> = emptyList(),
    @SerializedName("pending") val pending: Pending? = null,
    @SerializedName("submitted") val submitted: Set<String> = emptySet(),
    @SerializedName("tokenChangeBlocked") val tokenChangeBlocked: Boolean = false
) {
    override fun toString(): String = "MfaStoredState(redacted)"

    data class Account(
        @SerializedName("id") val id: String,
        @SerializedName("name") val name: String,
        @SerializedName("username") val username: String,
        @SerializedName("secret") val secret: String,
        @SerializedName("tenantId") val tenantId: String,
        @SerializedName("objectId") val objectId: String,
        @SerializedName("endpoint") val endpoint: String,
        @SerializedName("token") val token: String,
        @SerializedName("dosPreventer") val dosPreventer: String,
        @SerializedName("groupKey") val groupKey: String = "",
        @SerializedName("replicationScope") val replicationScope: String = "",
        @SerializedName("routingHint") val routingHint: String = "",
        @SerializedName("countryCode") val countryCode: String = ""
    ) {
        override fun toString(): String = "MfaStoredState.Account(redacted)"
        fun display() = MfaRegistrationResult(id, name)
        fun headers() = mapOf("x-ms-client-tenant-id" to tenantId,
            "x-ms-client-replication-scope" to replicationScope, "x-ms-routing-hint" to routingHint,
            "x-ms-client-tenant-country-code" to countryCode)
    }

    data class Pending(
        @SerializedName("id") val id: String,
        @SerializedName("digest") val digest: String,
        @SerializedName("token") val token: String,
        @SerializedName("endpoint") val endpoint: String,
        @SerializedName("activation") val activation: String? = null,
        @SerializedName("validation") val validation: String? = null,
        @SerializedName("validationEndpoint") val validationEndpoint: String? = null,
        @SerializedName("validationAttempt") val validationAttempt: Boolean = false,
        @SerializedName("confirming") val confirming: Boolean = false
    ) {
        override fun toString(): String = "MfaStoredState.Pending(redacted)"
    }

    fun validate() {
        requireMfa(version == 1 && accounts.size <= 100 && submitted.size <= 100_000,
            MfaErrorCode.STORAGE_FAILURE)
        requireMfa(token == null || token.isNotBlank() && token.length <= 4096,
            MfaErrorCode.STORAGE_FAILURE)
        requireMfa(accounts.map { it.id }.distinct().size == accounts.size, MfaErrorCode.STORAGE_FAILURE)
        accounts.forEach {
            requireMfa(listOf(it.id, it.name, it.username, it.secret, it.tenantId, it.objectId,
                it.endpoint, it.token, it.dosPreventer).all { v -> v.isNotBlank() } &&
                it.token == token, MfaErrorCode.STORAGE_FAILURE)
            MfaEndpointPolicy.validate(it.endpoint)
            MfaProof.validation(it.secret, 0)
        }
        pending?.let {
            requireMfa(it.id.isNotBlank() && it.token.isNotBlank() &&
                it.digest.matches(Regex("[0-9a-f]{64}")) &&
                it.token == token && (it.validation == null) == (it.validationEndpoint == null) &&
                (!it.confirming || it.activation != null && it.validation != null),
                MfaErrorCode.STORAGE_FAILURE)
            MfaEndpointPolicy.validate(it.endpoint)
            it.validationEndpoint?.let(MfaEndpointPolicy::validate)
            it.activation?.let { xml -> MfaXml.parse(xml.toByteArray()) }
            it.validation?.let { xml -> MfaXml.parse(xml.toByteArray()) }
        }
    }
}
