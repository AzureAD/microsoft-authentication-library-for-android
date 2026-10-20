// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.app.Application
import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaSdkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull

/** Owns the single sample SDK instance and bounded, process-lifetime callback work. */
class MfaSampleApplication : Application() {
    internal lateinit var environment: SampleMfaEnvironment
        private set
    private val applicationJob = SupervisorJob()
    private val callbackScope = CoroutineScope(applicationJob + Dispatchers.Main.immediate)
    private val callbackSlots = Semaphore(8)
    private val mutableSessions = MutableStateFlow<List<MfaAuthSession>>(emptyList())
    private val mutableNotice = MutableStateFlow("STARTING")
    internal val sessions: StateFlow<List<MfaAuthSession>> = mutableSessions
    internal val notice: StateFlow<String> = mutableNotice

    override fun onCreate() {
        super.onCreate()
        environment = EnvironmentFactory.create(this)
        environment.prepareNotifications()
        retrySetup()
    }

    internal fun retrySetup() = callback {
        environment.initialize()
    }

    internal fun forwardToken(token: String) = callback {
        // Even identical callbacks reach the SDK: idempotence belongs to the SDK.
        environment.manager.updatePushRegistration(token)
        report("PUSH_TOKEN_ACCEPTED")
    }

    internal fun dispatchPush(payload: Map<String, String>) {
        if (payload.isEmpty()) return
        if (payload.size > 64 || payload.any { it.key.length > 128 || it.value.length > 4096 } ||
            payload.entries.sumOf { it.key.length + it.value.length } > 16384
        ) {
            report("INVALID_NOTIFICATION")
            return
        }
        val snapshot = payload.toMap()
        callback {
            val received = environment.manager.handlePushNotification(snapshot)
            mergeSessions(received)
            report("PUSH_PROCESSED")
            if (received.isNotEmpty()) environment.notifyPending()
        }
    }

    internal fun mergeSessions(incoming: List<MfaAuthSession>) {
        mutableSessions.value = (mutableSessions.value + incoming)
            .distinctBy { it.guid }.takeLast(32)
    }

    internal fun removeSession(session: MfaAuthSession) {
        mutableSessions.value = mutableSessions.value.filterNot { it.guid == session.guid }
    }

    internal fun clearSessions() {
        mutableSessions.value = emptyList()
    }

    internal fun report(value: String) {
        mutableNotice.value = value
    }

    internal fun reportFailure(error: Exception) {
        if (error is CancellationException) throw error
        // Never expose exception messages, payloads, service text, tokens or activation material.
        report(if (error is MfaSdkException) error.code.name else "SAMPLE_OPERATION_FAILED")
    }

    private fun callback(block: suspend () -> Unit) {
        if (!callbackSlots.tryAcquire()) {
            report("CALLBACK_BUSY_RETRY_SETUP_OR_REFRESH")
            return
        }
        callbackScope.launch {
            try {
                val completed = withTimeoutOrNull(30_000) {
                    block()
                    true
                }
                if (completed == null) report("CALLBACK_TIMEOUT_RETRY_SETUP_OR_REFRESH")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                reportFailure(error)
            } finally {
                callbackSlots.release()
            }
        }
    }

    override fun onTerminate() {
        applicationJob.cancel()
        super.onTerminate()
    }
}
