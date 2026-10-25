// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Protocol adapted from public MSAL for Apple, commit 427db06c893b7ea229a3a19dee1cb0011fbf3777.
package com.microsoft.identity.mfa

import android.content.Context
import android.os.Build
import com.microsoft.identity.mfa.internal.EncryptedMfaStore
import com.microsoft.identity.mfa.internal.MfaActivationParser
import com.microsoft.identity.mfa.internal.MfaCredentialPolicy
import com.microsoft.identity.mfa.internal.MfaDiagnostics
import com.microsoft.identity.mfa.internal.MfaEndpointPolicy
import com.microsoft.identity.mfa.internal.MfaProof
import com.microsoft.identity.mfa.internal.MfaServiceClient
import com.microsoft.identity.mfa.internal.MfaStore
import com.microsoft.identity.mfa.internal.MfaStoredState
import com.microsoft.identity.mfa.internal.MfaXml
import com.microsoft.identity.mfa.internal.OkHttpMfaTransport
import com.microsoft.identity.mfa.internal.fail
import com.microsoft.identity.mfa.internal.requireMfa
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Experimental commercial-cloud activation MFA client. Obtain one process-wide instance with
 * [create]. Live calls are disabled unless explicitly opted in.
 * The host owns push token acquisition, push delivery and number-entry UI.
 */
class MfaSdkManager internal constructor(
    private val store: MfaStore,
    private val client: MfaServiceClient,
    private val enabled: Boolean,
    private val validationTimeoutMillis: Long = 120_000,
    private val policyProfile: MfaPolicyProfile = MfaPolicyProfile.CONSERVATIVE
) {
    private val mutex = Mutex()
    private var snapshot: MfaStoredState? = null
    private var enrolling = false
    private var validationBusy = false
    private var validationSignal: CompletableDeferred<Unit>? = null
    private val sessions = HashMap<String, Session>()
    private val mutableStatus = MutableStateFlow(MfaSdkStatus(MfaEnrollmentStage.IDLE, false, false))

    /** Safe enrollment and push-setup status. Restoration occurs on the first suspend operation. */
    val status: StateFlow<MfaSdkStatus> = mutableStatus.asStateFlow()

    /** Persists an opaque raw Android token; replacement after enrollment is explicitly unsupported. */
    suspend fun updatePushRegistration(deviceToken: String) {
        enabled()
        requireMfa(deviceToken.isNotBlank() && deviceToken.toByteArray().size <= 4096 &&
            deviceToken.none { it.isISOControl() || it.isWhitespace() }, MfaErrorCode.MISSING_PUSH_REGISTRATION)
        mutex.withLock {
            val state = load()
            if (state.token == deviceToken) return
            if (state.pending != null || state.accounts.isNotEmpty()) {
                commit(state.copy(tokenChangeBlocked = true))
                fail(MfaErrorCode.TOKEN_CHANGE_UNSUPPORTED)
            }
            commit(state.copy(token = deviceToken, tokenChangeBlocked = false))
        }
    }

    /**
     * Activates, waits for an independent validation push, then confirms and persists registration.
     * Repeating identical input resumes saved validation; ambiguous service writes are never replayed.
     */
    suspend fun registerAuthMethod(input: MfaRegistrationParameters): MfaRegistrationResult {
        MfaDiagnostics.event("enrollment.enter")
        enabled()
        val activation = MfaActivationParser.parse(input)
        val start = mutex.withLock {
            val state = load()
            requireMfa(!enrolling, MfaErrorCode.OPERATION_IN_PROGRESS)
            val token = state.token ?: fail(MfaErrorCode.MISSING_PUSH_REGISTRATION)
            val existing = state.pending?.let(::discardMismatchedValidation)
            if (existing != state.pending) commit(state.copy(pending = existing))
            if (existing != null) {
                MfaDiagnostics.event("enrollment.resume activation=${existing.activation != null} " +
                    "validation=${existing.validation != null} confirming=${existing.confirming}")
                requireMfa(existing.digest == activation.digest, MfaErrorCode.OPERATION_IN_PROGRESS)
                requireMfa(existing.activation != null && !existing.confirming &&
                    (!existing.validationAttempt || existing.validation != null), MfaErrorCode.OUTCOME_UNKNOWN)
            }
            val pending = existing ?: MfaStoredState.Pending(
                UUID.randomUUID().toString(), activation.digest, token, activation.endpoint.toString())
            if (existing == null) commit(state.copy(pending = pending))
            enrolling = true
            validationSignal = CompletableDeferred<Unit>().also {
                if (pending.validation != null) it.complete(Unit)
            }
            pending to (existing == null)
        }
        val id = start.first.id
        try {
            if (start.second) {
                val response = enrollmentCall("ActivateNew", id, activation.endpoint, listOf(
                    MfaXml("activationParams", children = listOf(
                        MfaXml("ActivationCode", activation.code),
                        MfaXml("DeviceToken", start.first.token), MfaXml("DeviceName", client.deviceName),
                        MfaXml("OathCounter", MfaProof.counter().toString()), MfaXml("Version", client.appVersion)
                    ))
                ))
                // A known response must survive UI cancellation, including the secret needed to resume.
                withContext(NonCancellable) {
                    mutex.withLock {
                        val state = load()
                        val pending = pending(state, id)
                        val correlated = discardMismatchedValidation(pending.copy(activation = response.encoded()))
                        commit(state.copy(pending = correlated))
                        if (correlated.validation != null) validationSignal?.complete(Unit)
                    }
                }
            }
            val info = mutex.withLock {
                val p = pending(load(), id)
                MfaXml.parse(p.activation!!.toByteArray()).child("activationInfo")
            }
            val username = info.required("Username")
            val secret = info.required("OathTokenSecretKey")
            MfaProof.validation(secret, 0)
            val confirmation = info.required("ConfirmationCode")
            val tenantId = info.required("TenantId")
            val objectId = info.required("AzureObjectId")
            val signal = mutex.withLock { validationSignal ?: fail(MfaErrorCode.OPERATION_IN_PROGRESS) }
            if (withTimeoutOrNull(validationTimeoutMillis) { signal.await(); true } != true) {
                MfaDiagnostics.event("enrollment.validationTimeout")
                fail(MfaErrorCode.VALIDATION_TIMEOUT)
            }
            val prepared = mutex.withLock {
                val state = load()
                val p = pending(state, id)
                val validated = MfaXml.parse((p.validation ?: fail(MfaErrorCode.INVALID_NOTIFICATION)).toByteArray())
                requireMfa(validationMatchesUsername(validated, username))
                val name = validated.required("accountName")
                val dos = validated.required("dosPreventer")
                val endpoint = MfaEndpointPolicy.service(p.validationEndpoint
                    ?: fail(MfaErrorCode.INVALID_NOTIFICATION))
                commit(state.copy(pending = p.copy(confirming = true)))
                MfaStoredState.Account("", name, username, secret, tenantId, objectId,
                    endpoint.toString(), p.token, dos, validated.optional("groupKey").orEmpty())
            }
            val confirmed = enrollmentCall("ConfirmActivation", id, activation.endpoint,
                listOf(MfaXml("confirmationCode", confirmation)))
            val account = prepared.copy(id = confirmed.required("phoneAppDetailId"))
            withContext(NonCancellable) {
                mutex.withLock {
                    val state = load()
                    pending(state, id)
                    commit(state.copy(accounts = state.accounts.filterNot { it.id == account.id } + account,
                        pending = null))
                }
            }
            currentCoroutineContext().ensureActive()
            return account.display()
        } finally {
            withContext(NonCancellable) {
                mutex.withLock {
                    enrolling = false
                    validationSignal = null
                }
            }
        }
    }

    /** Restores only SDK-owned service-confirmed registrations. */
    suspend fun registeredAuthMethods(): List<MfaRegistrationResult> {
        enabled()
        return mutex.withLock { load().accounts.map { it.display() } }
    }

    /** Performs one explicit pull; absence of work is distinct from a transport/service failure. */
    suspend fun pullPendingNotifications(): List<MfaAuthSession> {
        enabled()
        val accounts = mutex.withLock { load().accounts }
        val found = ArrayList<MfaAuthSession>()
        for (account in accounts) {
            val endpoint = routedEndpoint(account)
            val response = client.phoneApp("PhoneAppCheckForAuthentication", endpoint, listOf(
                MfaXml("dosPreventer", account.dosPreventer),
                MfaXml("deviceToken", account.token, mapOf("notificationType" to "gcm")),
                MfaXml("previousDeviceToken"), MfaXml("version", client.appVersion),
                MfaXml("osVersion", client.osVersion)
            ), account.headers())
            numericSuccess(response, "checkForAuthenticationResult", "1")
            val host = response.value("padUrl")
            if (host.isEmpty()) continue
            val username = response.required("username")
            val matched = accounts.singleOrNull { username.equals(it.username, true) }
                ?: fail(MfaErrorCode.UNKNOWN_ACCOUNT)
            val counter = response.required("oathCounter").toLongOrNull()
                ?: fail(MfaErrorCode.INVALID_RESPONSE)
            val target = MfaEndpointPolicy.notification(host)
            val session = fetch("", target, matched, MfaProof.validation(matched.secret, counter))
            if (found.none { it.guid == session.guid }) found.add(session)
        }
        return found
    }

    /**
     * Handles a host-forwarded data payload. Validation does not create a sign-in session;
     * authentication notifications must be fetched and associated with a stored registration.
     */
    suspend fun handlePushNotification(
        payload: Map<String, String>
    ): List<MfaAuthSession> = MfaDiagnostics.trace("push.handle") {
        enabled()
        requireMfa(payload.size <= 64 && payload.all { it.key.length <= 128 && it.value.length <= 8192 },
            MfaErrorCode.INVALID_NOTIFICATION)
        val type = payload["type"]
        MfaDiagnostics.event("push.type=${if (type in setOf("auth", "validate")) type else "OTHER"}")
        if (type !in setOf("auth", "validate")) return@trace emptyList()
        val guid = canonicalGuid(payload["guid"], MfaErrorCode.INVALID_NOTIFICATION)
        val endpoint = MfaEndpointPolicy.notification(payload["url"] ?: fail(MfaErrorCode.INVALID_NOTIFICATION))
        if (type == "auth") {
            val accounts = mutex.withLock { load().accounts }
            requireMfa(accounts.isNotEmpty(), MfaErrorCode.UNKNOWN_ACCOUNT)
            var candidate = accounts.firstOrNull { URI(it.endpoint).host == endpoint.host }
            if (candidate == null) {
                for (account in accounts) {
                    if (routedEndpoint(account).host == endpoint.host) { candidate = account; break }
                }
            }
            requireMfa(candidate != null, MfaErrorCode.UNTRUSTED_ENDPOINT)
            return@trace listOf(fetch(guid, endpoint, null, ""))
        }
        validateNotification(guid, endpoint, payload)
        emptyList()
    }

    /** Approves a manager-issued challenge using the user's entered number when required. */
    suspend fun approve(session: MfaAuthSession, number: String = ""): MfaDecision =
        respond(session, true, number)

    /** Denies a manager-issued challenge, independently of approval-only unsupported policies. */
    suspend fun deny(session: MfaAuthSession): MfaDecision = respond(session, false, "")

    private suspend fun validateNotification(guid: String, endpoint: URI, payload: Map<String, String>) {
        MfaDiagnostics.event("validation.enter")
        val p = mutex.withLock {
            val state = load()
            val p = state.pending ?: fail(MfaErrorCode.INVALID_NOTIFICATION)
            requireMfa(!p.confirming && p.validation == null, MfaErrorCode.INVALID_NOTIFICATION)
            requireMfa(!validationBusy, MfaErrorCode.OPERATION_IN_PROGRESS)
            requireMfa(!p.validationAttempt, MfaErrorCode.OUTCOME_UNKNOWN)
            requireMfa((payload["validationScenario"] ?: "QrRegistration") == "QrRegistration",
                MfaErrorCode.INVALID_NOTIFICATION)
            commit(state.copy(pending = p.copy(validationAttempt = true)))
            validationBusy = true
            p
        }
        try {
            val result = try {
                client.phoneApp("PhoneAppValidateDeviceToken", endpoint, listOf(
                    client.context(guid, p.token, needsDos = true),
                    MfaXml("validationResult", "yes"), MfaXml("accounts")
                ))
            } catch (e: CancellationException) { throw e }
            catch (e: MfaSdkException) {
                MfaDiagnostics.failure("validation.beforeOutcomeMapping", e)
                if (e.code == MfaErrorCode.SERVICE_REJECTED) {
                    withContext(NonCancellable) {
                        mutex.withLock {
                            val state = load()
                            commit(state.copy(pending = pending(state, p.id).copy(validationAttempt = false)))
                        }
                    }
                    throw e
                }
                fail(MfaErrorCode.OUTCOME_UNKNOWN)
            }
            validationUsername(result)
            result.required("accountName")
            result.required("dosPreventer")
            withContext(NonCancellable) {
                mutex.withLock {
                    val state = load()
                    val current = pending(state, p.id)
                    current.activation?.let {
                        val username = MfaXml.parse(it.toByteArray()).child("activationInfo").required("Username")
                        if (!validationMatchesUsername(result, username)) {
                            commit(state.copy(pending = current.copy(validationAttempt = false)))
                            fail(MfaErrorCode.INVALID_NOTIFICATION)
                        }
                    }
                    commit(state.copy(pending = current.copy(validation = result.encoded(),
                        validationEndpoint = endpoint.toString())))
                    MfaDiagnostics.event("validation.saved activation=${current.activation != null}")
                    if (current.activation != null) validationSignal?.complete(Unit)
                }
            }
        } finally {
            withContext(NonCancellable) { mutex.withLock { validationBusy = false } }
        }
    }

    private suspend fun enrollmentCall(
        operation: String, id: String, endpoint: URI, fields: List<MfaXml>
    ): MfaXml {
        try {
            val result = client.activation(operation, endpoint, fields)
            if (operation == "ActivateNew") {
                val info = result.child("activationInfo")
                listOf("Username", "ConfirmationCode", "TenantId", "AzureObjectId").forEach { info.required(it) }
                MfaProof.validation(info.required("OathTokenSecretKey"), 0)
            } else {
                result.required("phoneAppDetailId")
            }
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: MfaSdkException) {
            MfaDiagnostics.failure("$operation.beforeOutcomeMapping", e)
            if (e.code == MfaErrorCode.SERVICE_REJECTED &&
                (e.serviceCode == "Rejected" || e.serviceCode?.matches(Regex("[0-9]{1,10}")) == true)) {
                withContext(NonCancellable) {
                    mutex.withLock {
                        val state = load()
                        val p = pending(state, id)
                        commit(state.copy(pending = if (operation == "ActivateNew") null else p.copy(confirming = false)))
                    }
                }
                throw e
            }
            fail(MfaErrorCode.OUTCOME_UNKNOWN)
        }
    }

    private suspend fun fetch(
        guid: String, endpoint: URI, expected: MfaStoredState.Account?, proof: String
    ): MfaAuthSession {
        val state = mutex.withLock {
            val current = load()
            if (guid.isNotEmpty()) {
                requireMfa(guid !in current.submitted, MfaErrorCode.INVALID_SESSION)
                sessions[guid]?.let { return it.publicValue }
            }
            current
        }
        val token = expected?.token ?: state.token ?: fail(MfaErrorCode.MISSING_PUSH_REGISTRATION)
        val response = client.phoneApp("PhoneAppAuthentication", endpoint,
            listOf(client.context(guid, token, proof, true)), expected?.headers().orEmpty())
        val id = canonicalGuid(response.required("guid"), MfaErrorCode.INVALID_RESPONSE)
        requireMfa(guid.isEmpty() || guid == id)
        val info = response.child("phoneAppInfo")
        // Support the service's nested metadata and the original flat PoC response, never both.
        fun field(name: String): MfaXml? {
            val matches = (response.children + info.children).filter { it.name == name }
            requireMfa(matches.size <= 1)
            return matches.singleOrNull()
        }
        fun optional(name: String): String? = field(name)?.let {
            requireMfa(it.children.isEmpty())
            it.text
        }
        fun required(name: String): String = optional(name)?.takeIf { it.isNotBlank() }
            ?: fail(MfaErrorCode.INVALID_RESPONSE)
        fun emptyPolicy(name: String): Boolean = field(name)?.let {
            it.text.isEmpty() && it.children.isEmpty()
        } ?: true
        val registrationId = optional("phoneAppDetailId")
        val tenantId = required("tenantId")
        val objectId = required("azureObjectId")
        val account = state.accounts.singleOrNull {
            (registrationId.isNullOrBlank() || it.id == registrationId) &&
                it.tenantId.equals(tenantId, true) && it.objectId.equals(objectId, true)
        } ?: fail(MfaErrorCode.UNKNOWN_ACCOUNT)
        requireMfa(expected == null || expected.id == account.id, MfaErrorCode.UNKNOWN_ACCOUNT)
        // A fetched challenge may update routing only after its service identity is associated.
        val updated = account.copy(endpoint = endpoint.toString(),
            replicationScope = optional("replicationScope") ?: account.replicationScope,
            routingHint = optional("routingHint") ?: account.routingHint,
            countryCode = optional("countryCode") ?: account.countryCode,
            dosPreventer = optional("dosPreventer")?.takeIf { it.isNotBlank() } ?: account.dosPreventer)
        val number = !optional("firstEntropyChallenge").isNullOrEmpty()
        val blocked = linkedSetOf<MfaApprovalBlockReason>()
        if (info.attributes["mode"] != "standard") blocked.add(MfaApprovalBlockReason.UNSUPPORTED_MODE)
        val flags = mapOf(
            "returnLocationData" to MfaApprovalBlockReason.LOCATION_REQUIRED,
            "isSilent" to MfaApprovalBlockReason.SILENT_REQUEST,
            "isAppLockRequired" to MfaApprovalBlockReason.APP_LOCK_REQUIRED,
            "pinChangeRequired" to MfaApprovalBlockReason.PIN_CHANGE_REQUIRED)
        for ((name, reason) in flags) {
            when (optional(name)?.lowercase(java.util.Locale.ROOT)) {
                null, "", "no", "false", "0" -> Unit
                "yes", "true", "1" -> blocked.add(reason)
                else -> {
                    MfaDiagnostics.event("challenge.policyUnknownXmlFlag field=$name")
                    blocked.add(MfaApprovalBlockReason.UNKNOWN_POLICY)
                }
            }
        }
        blocked.addAll(MfaCredentialPolicy.blockedReasons(
            optional("userCredentialPolicy"), number, policyProfile))
        if (!emptyPolicy("adminPolicies")) blocked.add(MfaApprovalBlockReason.ADMIN_POLICY)
        val supported = blocked.isEmpty()
        MfaDiagnostics.event("challenge.parsed numberMatching=$number supported=$supported")
        MfaDiagnostics.event("challenge.approvalBlockedReasons=${blocked.joinToString(",") { it.name }}")
        val public = MfaAuthSession(id, account.name, account.username, number, blocked)
        return mutex.withLock {
            val latest = load()
            requireMfa(id !in latest.submitted, MfaErrorCode.INVALID_SESSION)
            sessions[id]?.let { return@withLock it.publicValue }
            requireMfa(latest.accounts.any { it.id == account.id }, MfaErrorCode.UNKNOWN_ACCOUNT)
            commit(latest.copy(accounts = latest.accounts.map { if (it.id == account.id) updated else it }))
            sessions[id] = Session(public, updated, endpoint, supported)
            public
        }
    }

    private suspend fun respond(value: MfaAuthSession, approve: Boolean, number: String): MfaDecision {
        enabled()
        val session = mutex.withLock {
            val state = load()
            val session = sessions[value.guid] ?: fail(MfaErrorCode.INVALID_SESSION)
            requireMfa(session.publicValue === value && value.guid !in state.submitted,
                MfaErrorCode.INVALID_SESSION)
            if (approve) {
                requireMfa(session.supported, MfaErrorCode.UNSUPPORTED_POLICY)
                if (value.requiresNumberMatching) {
                    requireMfa(number.matches(Regex("[0-9]{1,10}")), MfaErrorCode.INVALID_NUMBER)
                }
            }
            commit(state.copy(submitted = state.submitted + value.guid))
            session
        }
        val numbered = approve && value.requiresNumberMatching
        val fields = listOf(
            client.context(value.guid, session.account.token, needsDos = false),
            MfaXml("oathCounter", MfaProof.counter().toString()), MfaXml("completedInteractively", "yes")
        ) + if (numbered) listOf(
            MfaXml("pin", attributes = mapOf("stored" to "no")),
            MfaXml("authenticate", "yes"), MfaXml("selectedEntropyNumber", number)
        ) else listOf(
            MfaXml("authenticationResult", if (approve) "1" else "2"),
            MfaXml("newDeviceToken", attributes = mapOf("notificationType" to "gcm"))
        )
        try {
            val response = client.phoneApp(
                if (numbered) "PhoneAppPinValidation" else "PhoneAppAuthenticationResult",
                session.endpoint, fields, session.account.headers())
            numericSuccess(response, if (numbered) "validationResult" else "authenticationResultResult",
                if (numbered) "6" else "1")
            currentCoroutineContext().ensureActive()
            return if (approve) MfaDecision.APPROVED else MfaDecision.DENIED
        } catch (e: CancellationException) { throw e }
        catch (e: MfaSdkException) {
            if (e.code == MfaErrorCode.SERVICE_REJECTED) throw e
            fail(MfaErrorCode.OUTCOME_UNKNOWN)
        }
    }

    private suspend fun routedEndpoint(account: MfaStoredState.Account): URI {
        val endpoint = MfaEndpointPolicy.service(account.endpoint)
        if (account.replicationScope.isEmpty()) return endpoint
        val result = client.phoneApp("PhoneAppGetEndpoints", endpoint, emptyList(), account.headers())
        result.required("version")
        val map = LinkedHashMap<String, URI>()
        for (item in result.child("endpoints").children) {
            val target = MfaEndpointPolicy.service(item.required("url"))
            for (scope in item.required("replicationScopes").split(",")) {
                val key = scope.trim()
                requireMfa(key.isNotEmpty() && map.put(key, target) == null)
            }
        }
        return map[account.replicationScope] ?: endpoint
    }

    private fun numericSuccess(xml: MfaXml, field: String, success: String) {
        val code = xml.value(field)
        requireMfa(code.matches(Regex("[0-9]{1,10}")))
        if (code != success) throw MfaSdkException(MfaErrorCode.SERVICE_REJECTED, code)
    }

    private fun canonicalGuid(value: String?, error: MfaErrorCode): String {
        requireMfa(value != null && value.matches(
            Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")), error)
        return UUID.fromString(value).toString()
    }

    private fun enabled() { requireMfa(enabled, MfaErrorCode.FEATURE_DISABLED) }
    private fun pending(state: MfaStoredState, id: String): MfaStoredState.Pending =
        state.pending?.takeIf { it.id == id } ?: fail(MfaErrorCode.OPERATION_IN_PROGRESS)

    private fun discardMismatchedValidation(p: MfaStoredState.Pending): MfaStoredState.Pending {
        if (p.activation == null || p.validation == null) return p
        val expected = MfaXml.parse(p.activation.toByteArray()).child("activationInfo").required("Username")
        val validation = MfaXml.parse(p.validation.toByteArray())
        // A definitive response for another enrollment is not an ambiguous network outcome.
        // Early validation remains provisional until ActivateNew supplies the expected identity.
        return if (validationMatchesUsername(validation, expected)) p else p.copy(
            validation = null, validationEndpoint = null, validationAttempt = false)
    }

    private fun validationUsername(validation: MfaXml): String =
        validation.value("username").also {
            if (it.isBlank()) validation.required("groupKey")
        }

    private fun validationMatchesUsername(validation: MfaXml, expected: String): Boolean {
        val actual = validationUsername(validation)
        // Some service responses omit the username echo. Identity still comes from ActivateNew;
        // this pending attempt's token-bound validation and ConfirmActivation must both succeed.
        if (actual.isBlank()) {
            MfaDiagnostics.event("validation.identity=ACTIVATION_RESPONSE")
            return true
        }
        return expected.equals(actual, ignoreCase = true)
    }

    // These functions are called under mutex. Save the immutable next snapshot before publishing it.
    private suspend fun load(): MfaStoredState {
        snapshot?.let { return it }
        return store.load().also { it.validate(); snapshot = it; publish(it) }
    }
    private suspend fun commit(next: MfaStoredState) = withContext(NonCancellable) {
        // Cancellation between an atomic disk write and cache publication must not permit replay
        // from stale memory. Only this bounded persistence step is non-cancellable, never HTTP.
        store.save(next)
        snapshot = next
        publish(next)
    }
    private fun publish(state: MfaStoredState) {
        val stage = when {
            state.pending?.confirming == true -> MfaEnrollmentStage.CONFIRMING
            state.pending?.activation != null -> MfaEnrollmentStage.AWAITING_VALIDATION
            state.pending != null -> MfaEnrollmentStage.ACTIVATING
            state.accounts.isNotEmpty() -> MfaEnrollmentStage.REGISTERED
            else -> MfaEnrollmentStage.IDLE
        }
        mutableStatus.value = MfaSdkStatus(stage, state.token != null, state.tokenChangeBlocked)
        MfaDiagnostics.event("state.stage=${stage.name} pending=${state.pending != null} " +
            "tokenReady=${state.token != null} tokenChangeBlocked=${state.tokenChangeBlocked}")
    }

    private class Session(
        val publicValue: MfaAuthSession,
        val account: MfaStoredState.Account,
        val endpoint: URI,
        val supported: Boolean
    )

    companion object {
        private val instances = HashMap<String, MfaSdkManager>()

        /**
         * Gets the application-scoped manager. Experimental live network access defaults to off.
         * The Authenticator policy profile requires an explicitly authorized matching host identity.
         * The profile cannot change on an existing manager; incompatible configuration fails explicitly.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            context: Context, enableExperimentalService: Boolean = false,
            policyProfile: MfaPolicyProfile = MfaPolicyProfile.CONSERVATIVE
        ): MfaSdkManager =
            synchronized(instances) {
                val app = context.applicationContext
                policyProfile.validateHost(app.packageName)
                val key = app.packageName + ":" + enableExperimentalService
                instances.getOrPut(key) {
                    MfaSdkManager(EncryptedMfaStore(app, "msal-mfa-live-v1.bin"),
                        applicationClient(app), enableExperimentalService, policyProfile = policyProfile)
                }.also {
                    requireMfa(it.policyProfile == policyProfile, MfaErrorCode.INVALID_CONFIGURATION)
                }
            }

        internal fun applicationClient(context: Context): MfaServiceClient {
            val version = try {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                @Suppress("DEPRECATION")
                (info.versionName ?: info.versionCode.toString())
            } catch (_: Exception) { fail(MfaErrorCode.INVALID_RESPONSE) }
            return MfaServiceClient(OkHttpMfaTransport(), context.packageName, version,
                Build.VERSION.RELEASE, Build.MODEL)
        }
    }
}
