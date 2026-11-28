// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Wire shapes adapted from public MSAL for Apple, commit 427db06c893b7ea229a3a19dee1cb0011fbf3777.
package com.microsoft.identity.nativeauth.mfa.internal

import com.microsoft.identity.nativeauth.mfa.MfaAction
import com.microsoft.identity.nativeauth.mfa.MfaAuthSession
import com.microsoft.identity.nativeauth.mfa.MfaDecision
import com.microsoft.identity.nativeauth.mfa.MfaRegistrationParameters
import com.microsoft.identity.nativeauth.mfa.MfaRegistrationResult
import java.net.URI
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class MfaEngine(
    private val store: MfaStore,
    private val client: MfaServiceClient,
    private val validationTimeoutMillis: Long = 120_000
) {
    private val mutex = Mutex()
    private val updates = Mutex()
    private var snapshot: MfaStoredState? = null
    private var storageFailed = false
    private var pending: Pending? = null
    private var updating = false
    private val sessions = HashMap<String, Session>()

    suspend fun updatePushRegistration(token: String) = updates.withLock {
        requireMfa(token.isNotBlank() && token.toByteArray().size <= 4096 &&
            token.none { it.isISOControl() || it.isWhitespace() }, MfaErrorCode.INVALID_CONFIGURATION)
        val accounts = mutex.withLock {
            requireMfa(pending == null, MfaErrorCode.OPERATION_IN_PROGRESS)
            val state = load()
            requireMfa(state.blockedTokenUpdates.isEmpty(), MfaErrorCode.OUTCOME_UNKNOWN)
            commit(state.copy(token = token))
            updating = true
            state.accounts.filter { it.token != token }
        }
        val validationGuids = mutableSetOf<String>()
        try {
            for (account in accounts) {
                currentCoroutineContext().ensureActive()
                val operation = mutex.withLock {
                    val state = load()
                    // This guard survives a lost response, cancellation or process death.
                    commit(state.copy(blockedTokenUpdates = state.blockedTokenUpdates + account.id))
                    Pending(token, MfaEndpointPolicy.service(account.endpoint), account, validationGuids).also { pending = it }
                }
                try {
                    val response = client.phoneApp("phoneAppStartDeviceTokenChangeV2Request",
                        operation.endpoint, listOf(
                            MfaXml("newDeviceToken", token), MfaXml("version", client.appVersion),
                            MfaXml("osVersion", client.osVersion), MfaXml("appPackageName", client.appName),
                            MfaXml("notificationType", "FCM")
                        ), account.headers())
                    numericSuccess(response, "deviceTokenChangeResult", "1")
                    withContext(NonCancellable) {
                        mutex.withLock {
                            operation.startConfirmed = true
                            if (operation.completionConfirmed) persistRotation(operation, account)
                        }
                    }
                    awaitValidation(operation)
                } finally {
                    clearPending(operation)
                }
            }
        } finally {
            withContext(NonCancellable) { mutex.withLock { updating = false } }
        }
    }

    suspend fun register(input: MfaRegistrationParameters): MfaRegistrationResult {
        val activation = MfaActivationParser.parse(input)
        val operation = mutex.withLock {
            requireMfa(pending == null && !updating, MfaErrorCode.OPERATION_IN_PROGRESS)
            val state = load()
            val token = state.token ?: fail(MfaErrorCode.MISSING_PUSH_REGISTRATION)
            requireMfa(state.accounts.size < MfaStoredState.MAX_ACCOUNTS, MfaErrorCode.STORAGE_FAILURE)
            requireMfa(activation.digest !in state.enrollments && activation.legacyDigest !in state.enrollments,
                MfaErrorCode.OUTCOME_UNKNOWN)
            commit(state.copy(enrollments = state.enrollments + activation.digest))
            Pending(token, activation.endpoint).also { pending = it }
        }
        try {
            val response = client.activation("ActivateNew", activation.endpoint, listOf(
                MfaXml("activationParams", children = listOf(
                    MfaXml("ActivationCode", activation.code), MfaXml("DeviceToken", operation.token),
                    MfaXml("DeviceName", client.deviceName), MfaXml("OathCounter", MfaProof.counter().toString()),
                    MfaXml("Version", client.appVersion), MfaXml("PackageName", client.appName)
                ))
            ))
            val info = response.child("activationInfo")
            val username = info.required("Username")
            val secret = info.required("OathTokenSecretKey")
            MfaProof.validation(secret, 0)
            val confirmation = info.required("ConfirmationCode")
            val tenant = info.required("TenantId")
            val objectId = info.required("AzureObjectId")
            val validation = awaitValidation(operation)
            // No service-defined group-key-to-activation binding is established for this client.
            val validationUsername = validation.first.required("username")
            requireMfa(validationUsername.equals(username, true), MfaErrorCode.INVALID_NOTIFICATION)
            val name = validation.first.required("accountName")
            val dos = validation.first.required("dosPreventer")
            val confirmed = client.activation("ConfirmActivation", activation.endpoint,
                listOf(MfaXml("confirmationCode", confirmation)))
            val account = MfaStoredState.Account(confirmed.required("phoneAppDetailId"),
                name, username, secret, tenant, objectId, validation.second.toString(), operation.token, dos,
                groupKey = validation.first.optional("groupKey").orEmpty())
            withContext(NonCancellable) {
                mutex.withLock {
                    requireMfa(pending === operation)
                    val state = load()
                    requireMfa(state.accounts.none { it.id == account.id })
                    commit(state.copy(accounts = state.accounts + account))
                }
            }
            return account.display()
        } finally {
            clearPending(operation)
        }
    }

    suspend fun registrations(): List<MfaRegistrationResult> =
        mutex.withLock { load().accounts.map { it.display() } }

    suspend fun handlePush(payload: Map<String, String>): List<MfaAuthSession> {
        if (payload["type"] !in setOf("auth", "validate")) return emptyList()
        requireMfa(payload.size <= 64 && payload.all { it.key.length <= 128 && it.value.length <= 8192 },
            MfaErrorCode.INVALID_NOTIFICATION)
        val guid = canonicalGuid(payload["guid"], MfaErrorCode.INVALID_NOTIFICATION)
        val endpoint = MfaEndpointPolicy.notification(payload["url"] ?: fail(MfaErrorCode.INVALID_NOTIFICATION))
        if (payload["type"] == "validate") {
            validatePush(guid, endpoint, payload)
            return emptyList()
        }
        val candidates = mutex.withLock {
            load().accounts.filter { URI(it.endpoint).host == endpoint.host }
        }
        requireMfa(candidates.isNotEmpty(), MfaErrorCode.UNKNOWN_ACCOUNT)
        if (candidates.size == 1) return listOf(fetch(guid, endpoint, candidates.single(), "").publicValue)
        var firstFailure: MfaProtocolException? = null
        for (account in candidates) {
            try {
                pull(listOf(account)).singleOrNull { it.guid == guid }?.let { return listOf(it) }
            } catch (error: MfaProtocolException) {
                if (error.code == MfaErrorCode.STORAGE_FAILURE) throw error
                if (firstFailure == null) firstFailure = error
            }
        }
        throw firstFailure ?: MfaProtocolException(MfaErrorCode.INVALID_NOTIFICATION)
    }

    private suspend fun validatePush(guid: String, endpoint: URI, payload: Map<String, String>) {
        val operation = mutex.withLock {
            load()
            val p = pending ?: fail(MfaErrorCode.INVALID_NOTIFICATION)
            requireMfa(!p.attempted, MfaErrorCode.OUTCOME_UNKNOWN)
            requireMfa(endpoint.host == p.endpoint.host, MfaErrorCode.UNTRUSTED_ENDPOINT)
            val scenario = if (p.account == null) "QrRegistration" else "DeviceTokenChangeV2"
            requireMfa((payload["validationScenario"] ?: scenario) == scenario,
                MfaErrorCode.INVALID_NOTIFICATION)
            if (p.account != null) {
                requireMfa(payload["tenantId"] == p.account.tenantId &&
                    payload["oathCounter"]?.toLongOrNull()?.let { it >= 0 } == true,
                    MfaErrorCode.INVALID_NOTIFICATION)
                requireMfa(p.validationGuids.add(guid), MfaErrorCode.INVALID_NOTIFICATION)
            }
            p.attempted = true
            p
        }
        try {
            val account = operation.account
            val response = if (account == null) {
                client.phoneApp("PhoneAppValidateDeviceToken", endpoint, listOf(
                    client.context(guid, operation.token, needsDos = true),
                    MfaXml("validationResult", "yes"), MfaXml("accounts")
                )).also {
                    it.value("username")
                    it.required("accountName")
                    it.required("dosPreventer")
                }
            } else {
                val counter = payload.getValue("oathCounter").toLong()
                client.phoneApp("phoneAppCompleteDeviceTokenChangeV2Request", endpoint, listOf(
                    MfaXml("newDeviceToken", operation.token), MfaXml("version", client.appVersion),
                    MfaXml("osVersion", client.osVersion), MfaXml("accounts", children = listOf(
                        MfaXml("account", children = listOf(
                            MfaXml("azureObjectId", account.objectId), MfaXml("azureTenantId", account.tenantId),
                            MfaXml("oathCode", MfaProof.validation(account.secret, counter)),
                            MfaXml("phoneAppDetailId", account.id)
                        ))
                    ))
                ), account.headers()).also {
                    numericSuccess(it, "deviceTokenChangeResult", "1")
                    val results = it.child("accountValidationResults").children
                    // Exactly one requested account: no duplicate, missing or unrelated outcomes.
                    requireMfa(results.size == 1 && results.single().required("phoneAppDetailId") == account.id)
                    requireMfa(results.single().value("validationResult") == "Success",
                        MfaErrorCode.SERVICE_REJECTED)
                }
            }
            withContext(NonCancellable) {
                mutex.withLock {
                    load()
                    if (account != null && operation.startConfirmed) {
                        // The handler may outlive its waiter. The durable guard still owns this account.
                        persistRotation(operation, account)
                    } else {
                        requireMfa(pending === operation, MfaErrorCode.INVALID_NOTIFICATION)
                    }
                    operation.completionConfirmed = true
                    operation.result.complete(response to endpoint)
                }
            }
        } catch (error: CancellationException) {
            operation.result.completeExceptionally(MfaProtocolException(MfaErrorCode.OUTCOME_UNKNOWN))
            throw error
        } catch (error: Exception) {
            operation.result.completeExceptionally(error)
            throw error
        }
    }

    // Called under mutex once both acknowledgments have arrived, in either order.
    private suspend fun persistRotation(operation: Pending, account: MfaStoredState.Account) {
        val state = load()
        val current = state.accounts.singleOrNull { it.id == account.id }
        requireMfa(current != null && account.id in state.blockedTokenUpdates &&
            current.token == account.token && current.objectId == account.objectId &&
            current.tenantId == account.tenantId && state.token == operation.token, MfaErrorCode.OUTCOME_UNKNOWN)
        commit(state.copy(accounts = state.accounts.map {
            if (it.id == account.id) it.copy(previousToken = it.token, token = operation.token) else it
        }, blockedTokenUpdates = state.blockedTokenUpdates - account.id))
    }

    suspend fun pull(): List<MfaAuthSession> = pull(mutex.withLock { load().accounts })

    private suspend fun pull(accounts: List<MfaStoredState.Account>): List<MfaAuthSession> {
        val found = LinkedHashMap<String, MfaAuthSession>()
        for (account in accounts) {
            val response = client.phoneApp("PhoneAppCheckForAuthentication",
                MfaEndpointPolicy.service(account.endpoint), listOf(
                    MfaXml("dosPreventer", account.dosPreventer),
                    MfaXml("deviceToken", account.token, mapOf("notificationType" to "gcm")),
                    MfaXml("previousDeviceToken", account.previousToken),
                    MfaXml("version", client.appVersion), MfaXml("osVersion", client.osVersion)
                ), account.headers())
            numericSuccess(response, "checkForAuthenticationResult", "1")
            val host = response.value("padUrl")
            if (host.isEmpty()) continue
            requireMfa(response.required("username").equals(account.username, true), MfaErrorCode.UNKNOWN_ACCOUNT)
            val counter = response.required("oathCounter").toLongOrNull() ?: fail(MfaErrorCode.INVALID_RESPONSE)
            val session = fetch("", MfaEndpointPolicy.notification(host), account,
                MfaProof.validation(account.secret, counter)).publicValue
            found[session.guid] = session
        }
        return found.values.toList()
    }

    private suspend fun fetch(
        guid: String, endpoint: URI, expected: MfaStoredState.Account, proof: String,
        refresh: Boolean = false
    ): Session {
        mutex.withLock {
            val state = load()
            requireMfa(guid !in state.submitted, MfaErrorCode.INVALID_SESSION)
            if (!refresh) sessions[guid]?.let {
                requireMfa(it.account.id == expected.id, MfaErrorCode.UNKNOWN_ACCOUNT)
                return it
            }
        }
        val response = client.phoneApp("PhoneAppAuthentication", endpoint,
            listOf(client.context(guid, expected.token, proof, true)), expected.headers())
        val id = canonicalGuid(response.required("guid"), MfaErrorCode.INVALID_RESPONSE)
        requireMfa(guid.isEmpty() || id == guid)
        val info = response.child("phoneAppInfo")
        fun field(name: String): MfaXml? {
            val matches = (response.children + info.children).filter { it.name == name }
            requireMfa(matches.size <= 1)
            return matches.singleOrNull()?.also { requireMfa(it.namespace == response.namespace) }
        }
        fun optional(name: String): String? = field(name)?.let {
            requireMfa(it.children.isEmpty())
            it.text
        }
        fun required(name: String): String = optional(name)?.takeIf { it.isNotBlank() }
            ?: fail(MfaErrorCode.INVALID_RESPONSE)
        requireMfa(required("tenantId").equals(expected.tenantId, true) &&
            required("azureObjectId").equals(expected.objectId, true) &&
            required("phoneAppDetailId") == expected.id, MfaErrorCode.UNKNOWN_ACCOUNT)
        val number = !optional("firstEntropyChallenge").isNullOrEmpty()
        requireMfa(info.attributes["mode"] == "standard", MfaErrorCode.UNSUPPORTED_POLICY)
        for (flag in listOf("returnLocationData", "isSilent", "isAppLockRequired", "pinChangeRequired")) {
            requireMfa(optional(flag)?.lowercase(Locale.ROOT) in setOf("no", "false", "0"),
                MfaErrorCode.UNSUPPORTED_POLICY)
        }
        requireMfa(field("adminPolicies")?.let { it.text.isEmpty() && it.children.isEmpty() } != false,
            MfaErrorCode.UNSUPPORTED_POLICY)
        requireMfa(MfaCredentialPolicy.blockedReasons(optional("userCredentialPolicy"), number).isEmpty(),
            MfaErrorCode.UNSUPPORTED_POLICY)
        // No authoritative ordinary-approval capability exists in the supported wire contract.
        requireMfa(number, MfaErrorCode.UNSUPPORTED_POLICY)
        return mutex.withLock {
            val state = load()
            requireMfa(id !in state.submitted, MfaErrorCode.INVALID_SESSION)
            requireMfa(state.accounts.any { it.id == expected.id }, MfaErrorCode.UNKNOWN_ACCOUNT)
            val existing = sessions[id]
            requireMfa(existing == null || existing.account.id == expected.id)
            if (!refresh && existing != null) return@withLock existing
            requireMfa(existing == null || existing.publicValue.requiresNumberMatching == number,
                MfaErrorCode.UNSUPPORTED_POLICY)
            // A concurrent token update owns token fields; metadata refresh must not overwrite them.
            val latestAccount = state.accounts.single { it.id == expected.id }
            val merged = latestAccount.copy(endpoint = endpoint.toString(),
                replicationScope = optional("replicationScope") ?: latestAccount.replicationScope,
                routingHint = optional("routingHint") ?: latestAccount.routingHint,
                countryCode = optional("countryCode") ?: latestAccount.countryCode,
                dosPreventer = optional("dosPreventer")?.takeIf { it.isNotBlank() } ?: latestAccount.dosPreventer)
            commit(state.copy(accounts = state.accounts.map { if (it.id == expected.id) merged else it }))
            Session(existing?.publicValue ?: MfaAuthSession.create(
                id, expected.id, expected.name, expected.username, number), merged, endpoint).also { sessions[id] = it }
        }
    }

    suspend fun respond(value: MfaAuthSession, action: MfaAction): MfaDecision {
        val original = mutex.withLock {
            val state = load()
            requireMfa(value.guid !in state.submitted, MfaErrorCode.INVALID_SESSION)
            val session = sessions[value.guid] ?: fail(MfaErrorCode.INVALID_NOTIFICATION)
            requireMfa(session.publicValue === value, MfaErrorCode.INVALID_NOTIFICATION)
            if (action is MfaAction.Approve) {
                requireMfa(action.number?.matches(Regex("[0-9]{1,10}")) == true, MfaErrorCode.INVALID_NUMBER)
            }
            session.copy(account = state.accounts.single { it.id == session.account.id })
        }
        // Recheck authoritative requirements before persisting the one-way submission guard.
        val session = fetch(value.guid, original.endpoint, original.account, "", refresh = true)
        mutex.withLock {
            val state = load()
            requireMfa(value.guid !in state.submitted, MfaErrorCode.INVALID_SESSION)
            commit(state.copy(submitted = state.submitted + value.guid))
            sessions.remove(value.guid)
        }
        val approve = action is MfaAction.Approve
        val fields = listOf(
            client.context(value.guid, session.account.token, needsDos = false),
            MfaXml("oathCounter", MfaProof.counter().toString()), MfaXml("completedInteractively", "yes")
        ) + if (action is MfaAction.Approve) listOf(
            MfaXml("pin", attributes = mapOf("stored" to "no")), MfaXml("authenticate", "yes"),
            MfaXml("selectedEntropyNumber", action.number.orEmpty())
        ) else listOf(
            MfaXml("authenticationResult", "2"),
            MfaXml("newDeviceToken", attributes = mapOf("notificationType" to "gcm"))
        )
        val response = client.phoneApp(
            if (approve) "PhoneAppPinValidation" else "PhoneAppAuthenticationResult",
            session.endpoint, fields, session.account.headers())
        numericSuccess(response, if (approve) "validationResult" else "authenticationResultResult",
            if (approve) "6" else "1")
        return if (approve) MfaDecision.APPROVED else MfaDecision.DENIED
    }

    private suspend fun awaitValidation(operation: Pending): Pair<MfaXml, URI> =
        withTimeoutOrNull(validationTimeoutMillis) { operation.result.await() }
            ?: fail(MfaErrorCode.VALIDATION_TIMEOUT)

    private suspend fun clearPending(operation: Pending) = withContext(NonCancellable) {
        mutex.withLock { if (pending === operation) pending = null }
    }

    private suspend fun load(): MfaStoredState {
        requireMfa(!storageFailed, MfaErrorCode.STORAGE_FAILURE)
        return snapshot ?: store.load().also { it.validate(); snapshot = it }
    }

    private suspend fun commit(next: MfaStoredState) = withContext(NonCancellable) {
        requireMfa(!storageFailed, MfaErrorCode.STORAGE_FAILURE)
        next.validate()
        try {
            store.save(next)
        } catch (error: Exception) {
            // The rename may have completed even though a later durability step failed.
            // Neither the old cache nor an automatic reload establishes a safe continuation.
            storageFailed = true
            snapshot = null
            sessions.clear()
            pending?.result?.completeExceptionally(MfaProtocolException(MfaErrorCode.STORAGE_FAILURE))
            throw error
        }
        snapshot = next
    }

    private fun numericSuccess(xml: MfaXml, field: String, success: String) {
        val code = xml.value(field)
        requireMfa(code.matches(Regex("[0-9]{1,10}")))
        requireMfa(code == success, MfaErrorCode.SERVICE_REJECTED)
    }

    private fun canonicalGuid(value: String?, error: MfaErrorCode): String {
        requireMfa(value != null && value.matches(
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")), error)
        return UUID.fromString(value).toString()
    }

    private class Pending(
        val token: String,
        val endpoint: URI,
        val account: MfaStoredState.Account? = null,
        val validationGuids: MutableSet<String> = mutableSetOf(),
        var startConfirmed: Boolean = false,
        var completionConfirmed: Boolean = false,
        var attempted: Boolean = false,
        val result: CompletableDeferred<Pair<MfaXml, URI>> = CompletableDeferred()
    )

    private data class Session(
        val publicValue: MfaAuthSession,
        val account: MfaStoredState.Account,
        val endpoint: URI
    )
}
