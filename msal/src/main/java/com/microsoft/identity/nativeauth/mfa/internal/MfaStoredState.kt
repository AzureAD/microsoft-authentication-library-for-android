// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.nativeauth.mfa.internal

import com.google.gson.annotations.SerializedName
import com.microsoft.identity.nativeauth.mfa.MfaRegistrationResult

// Only confirmed accounts and replay guards are durable; pending enrollment is memory-only.
internal data class MfaStoredState(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("token") val token: String? = null,
    @SerializedName("accounts") val accounts: List<Account> = emptyList(),
    @SerializedName("submitted") val submitted: Set<String> = emptySet(),
    @SerializedName("enrollments") val enrollments: Set<String> = emptySet(),
    @SerializedName("blockedTokenUpdates") val blockedTokenUpdates: Set<String> = emptySet()
) {
    override fun toString(): String = "MfaStoredState(<redacted>)"

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
        @SerializedName("previousToken") val previousToken: String = "",
        @SerializedName("groupKey") val groupKey: String = "",
        @SerializedName("replicationScope") val replicationScope: String = "",
        @SerializedName("routingHint") val routingHint: String = "",
        @SerializedName("countryCode") val countryCode: String = ""
    ) {
        override fun toString(): String = "MfaStoredState.Account(<redacted>)"
        fun display() = MfaRegistrationResult(id, name, username)
        fun headers() = mapOf("x-ms-client-tenant-id" to tenantId,
            "x-ms-client-replication-scope" to replicationScope, "x-ms-routing-hint" to routingHint,
            "x-ms-client-tenant-country-code" to countryCode)
    }

    fun validate() {
        requireMfa(version == 1 && accounts.size <= 100 && submitted.size <= 100_000 &&
            enrollments.size <= 100_000, MfaErrorCode.STORAGE_FAILURE)
        requireMfa(token == null || token.isNotBlank() && token.length <= 4096,
            MfaErrorCode.STORAGE_FAILURE)
        requireMfa(accounts.map { it.id }.distinct().size == accounts.size &&
            blockedTokenUpdates.all { id -> accounts.any { it.id == id } },
            MfaErrorCode.STORAGE_FAILURE)
        requireMfa(enrollments.all { it.matches(Regex("[0-9a-f]{64}")) },
            MfaErrorCode.STORAGE_FAILURE)
        accounts.forEach {
            requireMfa(listOf(it.id, it.name, it.username, it.secret, it.tenantId, it.objectId,
                it.endpoint, it.token, it.dosPreventer).all { v -> v.isNotBlank() } &&
                it.token.length <= 4096 && it.previousToken.length <= 4096,
                MfaErrorCode.STORAGE_FAILURE)
            MfaEndpointPolicy.service(it.endpoint)
            MfaProof.validation(it.secret, 0)
        }
    }
}
