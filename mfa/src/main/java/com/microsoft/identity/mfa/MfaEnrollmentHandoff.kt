// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import android.os.SystemClock
import com.microsoft.identity.mfa.internal.MfaActivationParser
import com.microsoft.identity.mfa.internal.requireMfa
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Headless, thread-safe staging and explicit enrollment for HTTPS activation links.
 *
 * Create ONE per manager/session, using an application/session scope with a Job, not a UI scope.
 * Receiving input never enrolls. The host renders confirmation and calls [confirm] or [cancel].
 * Pending credentials expire after [pendingLifetimeMillis] (1..300000 ms; default two minutes).
 * The coordinator retains at most 64 digests, then rejects new inputs until a new session.
 * This is a bounded in-memory replay guard, not a persistent/global ticket or provisioning service.
 *
 * [close] discards pending input but does NOT cancel submitted service work. UI dismissal must not
 * cancel the owning scope. Scope cancellation yields OUTCOME_UNKNOWN and is never retried.
 * Process death loses observation/correlation; restored manager REGISTERED state is not proof that
 * this request succeeded. Managed strings cannot be reliably zeroed; references are discarded.
 */
class MfaEnrollmentHandoff internal constructor(
    private val scope: CoroutineScope,
    private val pendingLifetimeMillis: Long,
    private val elapsedRealtime: () -> Long,
    private val enroll: suspend (MfaRegistrationParameters) -> Unit
) : AutoCloseable {
    /** Creates an app-owned coordinator reusing the manager's real registration and endpoint policy. */
    @JvmOverloads
    constructor(
        manager: MfaSdkManager,
        scope: CoroutineScope,
        pendingLifetimeMillis: Long = 120_000
    ) : this(scope, pendingLifetimeMillis, { SystemClock.elapsedRealtime() },
        { manager.registerAuthMethod(it); Unit })

    private val lock = Any()
    private val mutableState = MutableStateFlow(MfaHandoffState())
    private val digests = LinkedHashSet<String>()
    private var pending: Pending? = null
    private var expiry: Job? = null
    private var closed = false
    private var scopeCompletion: DisposableHandle? = null

    /** Safe, request-specific state, independent of the manager's restored global enrollment stage. */
    val state: StateFlow<MfaHandoffState> = mutableState.asStateFlow()

    init {
        requireMfa(pendingLifetimeMillis in 1..300_000 && scope.coroutineContext[Job] != null,
            MfaErrorCode.INVALID_CONFIGURATION)
        val completion = requireNotNull(scope.coroutineContext[Job]).invokeOnCompletion { close() }
        synchronized(lock) {
            if (closed) completion.dispose() else scopeCompletion = completion
        }
    }

    /** Validates and stages a supported HTTPS link. Callers must never log or persist the link. */
    fun stage(activationLink: String): MfaHandoffReceipt = stage(activationLink, MfaHandoffSource.DIRECT)

    internal fun stage(link: String, source: MfaHandoffSource): MfaHandoffReceipt = synchronized(lock) {
        expire()
        if (closed || !scope.isActive) return receipt(MfaHandoffDisposition.CLOSED)
        val activation = try {
            requireMfa(link.length <= MAX_LINK_BYTES && link.toByteArray(Charsets.UTF_8).size <= MAX_LINK_BYTES &&
                link.none { it.isISOControl() || it.isWhitespace() }, MfaErrorCode.INVALID_ACTIVATION)
            val uri = URI(link)
            requireMfa(uri.scheme == "https" && uri.host == "login.microsoftonline.com" &&
                uri.rawPath == "/authenticatorApp/activateAccount", MfaErrorCode.INVALID_ACTIVATION)
            MfaActivationParser.parse(MfaRegistrationParameters.QrCode(link))
        } catch (error: MfaSdkException) {
            return MfaHandoffReceipt(MfaHandoffDisposition.REJECTED, errorCode = error.code)
        } catch (_: Exception) {
            return MfaHandoffReceipt(MfaHandoffDisposition.REJECTED, errorCode = MfaErrorCode.INVALID_ACTIVATION)
        }
        if (activation.digest in digests) return receipt(MfaHandoffDisposition.DUPLICATE)
        if (mutableState.value.phase in ACTIVE) return receipt(MfaHandoffDisposition.BUSY)
        if (digests.size >= MAX_REQUESTS) return receipt(MfaHandoffDisposition.LIMIT_REACHED)
        val id = UUID.randomUUID().toString()
        digests.add(activation.digest)
        pending = Pending(MfaRegistrationParameters.Activation(activation.code, activation.endpoint.toString()),
            elapsedRealtime() + pendingLifetimeMillis)
        mutableState.value = MfaHandoffState(MfaHandoffPhase.AWAITING_CONFIRMATION, id, source)
        expiry = scope.launch {
            delay(pendingLifetimeMillis)
            synchronized(lock) { if (mutableState.value.requestId == id) expire() }
        }
        MfaHandoffReceipt(MfaHandoffDisposition.STAGED, id)
    }

    /**
     * Submits only the currently awaiting request, once. Returns false for stale/expired IDs.
     * The scope, not the calling Activity, owns submission through its real terminal outcome.
     */
    fun confirm(requestId: String): Boolean = synchronized(lock) {
        expire()
        if (closed || !scope.isActive || !awaiting(requestId)) return false
        val input = pending?.input ?: return false
        discardPending()
        mutableState.value = mutableState.value.copy(phase = MfaHandoffPhase.SUBMITTING)
        val operation = scope.launch {
            try {
                enroll(input)
                coroutineContext.ensureActive()
                finish(requestId, MfaHandoffPhase.SUCCEEDED)
            } catch (error: CancellationException) {
                finish(requestId, MfaHandoffPhase.OUTCOME_UNKNOWN, MfaErrorCode.OUTCOME_UNKNOWN)
                throw error
            } catch (error: MfaSdkException) {
                finish(requestId, if (error.code == MfaErrorCode.OUTCOME_UNKNOWN)
                    MfaHandoffPhase.OUTCOME_UNKNOWN else MfaHandoffPhase.FAILED, error.code)
            } catch (_: Exception) {
                finish(requestId, MfaHandoffPhase.OUTCOME_UNKNOWN, MfaErrorCode.OUTCOME_UNKNOWN)
            }
        }
        // Covers cancellation before the coroutine body starts as well as abnormal completion.
        operation.invokeOnCompletion {
            if (it != null) finish(requestId, MfaHandoffPhase.OUTCOME_UNKNOWN, MfaErrorCode.OUTCOME_UNKNOWN)
        }
        true
    }

    /** Cancels only an unsubmitted matching request; never interrupts an in-flight operation. */
    fun cancel(requestId: String): Boolean = synchronized(lock) {
        expire()
        if (!awaiting(requestId)) return false
        discardPending()
        mutableState.value = mutableState.value.copy(phase = MfaHandoffPhase.CANCELLED)
        true
    }

    /** Clears a matching terminal observation after the host displays it; replay history remains. */
    fun acknowledge(requestId: String): Boolean = synchronized(lock) {
        if (mutableState.value.requestId != requestId || mutableState.value.phase in ACTIVE) return false
        mutableState.value = MfaHandoffState(if (closed) MfaHandoffPhase.DISPOSED else MfaHandoffPhase.IDLE)
        true
    }

    /** Discards staged credentials and rejects further inputs without cancelling submitted work. */
    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            discardPending()
            digests.clear()
            if (mutableState.value.phase in setOf(MfaHandoffPhase.IDLE, MfaHandoffPhase.AWAITING_CONFIRMATION)) {
                mutableState.value = mutableState.value.copy(phase = MfaHandoffPhase.DISPOSED)
            }
        }
        scopeCompletion?.dispose()
    }

    private fun finish(id: String, phase: MfaHandoffPhase, error: MfaErrorCode? = null) = synchronized(lock) {
        if (mutableState.value.requestId == id && mutableState.value.phase == MfaHandoffPhase.SUBMITTING) {
            mutableState.value = mutableState.value.copy(phase = phase, errorCode = error)
        }
    }

    private fun expire() {
        if (pending?.let { elapsedRealtime() >= it.expiresAt } == true) {
            discardPending()
            mutableState.value = mutableState.value.copy(phase = MfaHandoffPhase.EXPIRED)
        }
    }

    private fun discardPending() {
        pending = null
        expiry?.cancel()
        expiry = null
    }

    private fun awaiting(id: String) = mutableState.value.requestId == id &&
        mutableState.value.phase == MfaHandoffPhase.AWAITING_CONFIRMATION

    private fun receipt(disposition: MfaHandoffDisposition) = MfaHandoffReceipt(disposition)

    private class Pending(val input: MfaRegistrationParameters, val expiresAt: Long)

    internal companion object {
        const val MAX_LINK_BYTES = 8192
        private const val MAX_REQUESTS = 64
        private val ACTIVE = setOf(MfaHandoffPhase.AWAITING_CONFIRMATION, MfaHandoffPhase.SUBMITTING)
    }
}
