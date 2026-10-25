// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.MfaRegistrationParameters
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal enum class SetupSource { EMBEDDED, BROWSER }

internal enum class SetupPhase(val message: String, val terminal: Boolean = false) {
    PREPARED("Choose Set up on this device to request native confirmation."),
    CONFIRMATION("Waiting for Continue or Cancel in the native app."),
    SUBMITTING("Live enrollment is running. Keep the native app open; do not resubmit."),
    SUCCEEDED("This request completed native live enrollment. This is not production portal continuation.", true),
    NOT_CONFIRMED("Enrollment was not confirmed. Check the native app and service before using fresh input. Do not retry this request.", true),
    NOT_READY("Native push setup is not ready. No enrollment was submitted. Prepare a new request when ready.", true),
    CANCELLED("Cancelled before submission. No enrollment was submitted.", true),
    EXPIRED("This request expired before submission. Prepare a new request with fresh activation input.", true),
    INTERRUPTED("This page session expired during enrollment. The outcome is unknown here; check the native app and service. Do not resubmit.", true)
}

/** Safe, process-local presentation state; never contains activation parameters or account data. */
internal data class SetupSnapshot(
    val id: String,
    val source: SetupSource,
    val phase: SetupPhase
) {
    val path: String get() = "/setup/$id"
    val launchUri: String get() = "mfa-poc-live://enroll/$id"
}

/** One-shot state machine. The monotonic clock is injectable for ordinary JVM tests. */
internal class LiveSetupSession(private val now: () -> Long) {
    private val mutableState = MutableStateFlow<SetupSnapshot?>(null)
    val state: StateFlow<SetupSnapshot?> = mutableState
    private var input: MfaRegistrationParameters? = null
    private var deadline = 0L
    private var inFlightId: String? = null

    @Synchronized
    fun prepare(parameters: MfaRegistrationParameters, source: SetupSource): SetupSnapshot? {
        tick()
        if (mutableState.value != null || inFlightId != null) return null
        input = parameters
        deadline = now() + REQUEST_LIFETIME_MS
        return SetupSnapshot(randomId(), source, SetupPhase.PREPARED).also { mutableState.value = it }
    }

    @Synchronized
    fun current(): SetupSnapshot? {
        tick()
        return mutableState.value
    }

    @Synchronized
    fun canPrepare(): Boolean = current() == null && inFlightId == null

    @Synchronized
    fun receive(uri: String, source: SetupSource): Boolean {
        val current = current() ?: return false
        // Exact canonical equality also rejects userinfo, ports, query, fragments and encoded paths.
        if (current.source != source || uri != current.launchUri ||
            current.phase != SetupPhase.PREPARED) return false
        mutableState.value = current.copy(phase = SetupPhase.CONFIRMATION)
        return true
    }

    @Synchronized
    fun consume(id: String): MfaRegistrationParameters? {
        val current = current() ?: return null
        if (current.id != id || current.phase != SetupPhase.CONFIRMATION || inFlightId != null) return null
        val consumed = input ?: return null
        input = null
        inFlightId = id
        deadline = now() + REQUEST_LIFETIME_MS
        mutableState.value = current.copy(phase = SetupPhase.SUBMITTING)
        return consumed
    }

    @Synchronized
    fun cancel(id: String, notReady: Boolean = false) {
        val current = current() ?: return
        if (current.id == id && current.phase in listOf(SetupPhase.PREPARED, SetupPhase.CONFIRMATION)) {
            terminal(if (notReady) SetupPhase.NOT_READY else SetupPhase.CANCELLED)
        }
    }

    @Synchronized
    fun complete(id: String, success: Boolean) {
        if (inFlightId != id) return
        inFlightId = null
        val current = current() ?: return
        if (current.id == id && current.phase == SetupPhase.SUBMITTING) {
            terminal(if (success) SetupPhase.SUCCEEDED else SetupPhase.NOT_CONFIRMED)
        }
    }

    @Synchronized
    fun clear() {
        input = null
        mutableState.value = null
        // A closed page must neither cancel native enrollment nor unlock a second submission.
    }

    @Synchronized
    fun tick() {
        val current = mutableState.value ?: return
        if (now() < deadline) return
        when {
            current.phase.terminal -> clear()
            current.phase == SetupPhase.SUBMITTING -> terminal(SetupPhase.INTERRUPTED)
            else -> terminal(SetupPhase.EXPIRED)
        }
    }

    private fun terminal(phase: SetupPhase) {
        input = null
        mutableState.value = mutableState.value?.copy(phase = phase)
        deadline = now() + RESULT_LIFETIME_MS
    }

    companion object {
        const val REQUEST_LIFETIME_MS = 5 * 60_000L
        const val RESULT_LIFETIME_MS = 2 * 60_000L

        fun randomId(): String = ByteArray(32).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
