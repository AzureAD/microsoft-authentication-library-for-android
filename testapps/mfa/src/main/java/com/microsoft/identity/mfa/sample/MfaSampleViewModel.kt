// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaDecision
import com.microsoft.identity.mfa.MfaEnrollmentStage
import com.microsoft.identity.mfa.MfaHandoffDisposition
import com.microsoft.identity.mfa.MfaHandoffPhase
import com.microsoft.identity.mfa.MfaHandoffReceipt
import com.microsoft.identity.mfa.MfaRegistrationParameters
import com.microsoft.identity.mfa.MfaRegistrationResult
import com.microsoft.identity.mfa.MfaSdkException
import com.microsoft.identity.mfa.poc.MfaPocDiagnostics
import com.microsoft.identity.mfa.poc.MfaPocDiagnostics.Event
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Holds screen work without saving activation or decision inputs across process recreation. */
class MfaSampleViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MfaSampleApplication
    internal val environment = app.environment
    internal val status = environment.manager.status
    internal val sessions = app.sessions
    internal val notice = app.notice
    internal val handoff = app.handoff
    internal val portalHandoff = app.portalHandoff
    internal val portalIntentAdapter = app.portalIntentAdapter
    private val mutablePageToOpen = MutableStateFlow<SetupLocation?>(null)
    internal val pageToOpen: StateFlow<SetupLocation?> = mutablePageToOpen
    internal var embeddedPageOpen = false
    private val mutableAccounts = MutableStateFlow<List<MfaRegistrationResult>>(emptyList())
    internal val accounts: StateFlow<List<MfaRegistrationResult>> = mutableAccounts
    private val mutableBusy = app.operationBusy
    internal val busy: StateFlow<Boolean> = mutableBusy

    init {
        viewModelScope.launch {
            try {
                mutableAccounts.value = environment.manager.registeredAuthMethods()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                app.reportFailure(error)
            }
        }
        viewModelScope.launch {
            status.collect {
                if (it.enrollmentStage == MfaEnrollmentStage.REGISTERED) {
                    try {
                        mutableAccounts.value = environment.manager.registeredAuthMethods()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        app.reportFailure(error)
                    }
                }
            }
        }
    }

    private fun pushRegistrationReady(): Boolean = !busy.value &&
        environment.setup.value == SampleSetup.TOKEN_ACQUIRED &&
        status.value.tokenReady && !status.value.tokenChangeBlocked

    internal fun registrationReady(): Boolean = pushRegistrationReady() &&
        portalHandoff.state.value.phase !in setOf(
            MfaHandoffPhase.AWAITING_CONFIRMATION, MfaHandoffPhase.SUBMITTING)

    internal fun portalRegistrationReady(): Boolean = pushRegistrationReady() &&
        handoff.requests.canPrepare() &&
        status.value.enrollmentStage in listOf(MfaEnrollmentStage.IDLE, MfaEnrollmentStage.REGISTERED)

    internal fun confirmPortal(id: String) {
        if (!portalRegistrationReady()) {
            app.report("REGISTRATION_NOT_READY_OR_CONTROLLED_PAGE_ACTIVE")
        } else if (!portalHandoff.confirm(id)) {
            app.report("PORTAL_REQUEST_EXPIRED_OR_ALREADY_USED")
        }
    }

    internal fun portalReceipt(receipt: MfaHandoffReceipt) {
        if (receipt.disposition != MfaHandoffDisposition.IGNORED) {
            app.report("PORTAL_HANDOFF_${receipt.disposition.name}")
        }
    }

    internal fun portalUnavailable(error: Exception) = app.reportFailure(error)

    internal fun pageRegistrationReady(): Boolean = registrationReady() &&
        status.value.enrollmentStage in listOf(MfaEnrollmentStage.IDLE, MfaEnrollmentStage.REGISTERED)

    internal fun register(input: MfaRegistrationParameters) {
        if (!registrationReady() || !handoff.requests.canPrepare()) {
            app.report("REGISTRATION_NOT_READY_OR_PAGE_ACTIVE")
            return
        }
        operation { enroll(input) }
    }

    private suspend fun enroll(input: MfaRegistrationParameters, requestId: String? = null) {
        MfaPocDiagnostics.event(Event.REGISTER_START)
        try {
            environment.manager.registerAuthMethod(input)
            // Only this invocation's return confirms this request, never a restored SDK status.
            requestId?.let { handoff.requests.complete(it, true) }
            mutableAccounts.value = environment.manager.registeredAuthMethods()
            app.report("REGISTERED")
        } catch (error: Exception) {
            requestId?.let { handoff.requests.complete(it, false) }
            throw error
        } finally {
            MfaPocDiagnostics.event(Event.REGISTER_END)
        }
    }

    internal fun preparePage(input: MfaRegistrationParameters, source: SetupSource) {
        if (!pageRegistrationReady() || !handoff.requests.canPrepare()) {
            app.report("REGISTRATION_NOT_READY_OR_PAGE_ACTIVE")
            return
        }
        operation {
            mutablePageToOpen.value = handoff.prepare(input, source)
            app.report("CONTROLLED_PAGE_READY")
        }
    }

    internal fun pageOpened() {
        mutablePageToOpen.value = null
    }

    internal fun receiveSetup(uri: String, source: SetupSource) {
        if (!handoff.requests.receive(uri, source)) app.report("PAGE_REQUEST_INVALID_EXPIRED_OR_ALREADY_USED")
    }

    internal fun confirmSetup(id: String) {
        if (!pageRegistrationReady()) {
            handoff.requests.cancel(id, notReady = true)
            app.report("REGISTRATION_NOT_READY")
            return
        }
        val input = handoff.requests.consume(id) ?: run {
            app.report("PAGE_REQUEST_INVALID_EXPIRED_OR_ALREADY_USED")
            return
        }
        operation { enroll(input, id) }
    }

    internal fun pageUnavailable(id: String) {
        handoff.requests.cancel(id)
        app.report("SETUP_PAGE_UNAVAILABLE_USE_SUPPORTED_BROWSER_OR_WEBVIEW")
    }

    internal fun reportInvalidInput() = app.report("ENTER_ONLY_QR_OR_COMPLETE_MANUAL_ACTIVATION")

    internal fun reportInvalidPage() = app.report("PAGE_REQUEST_INVALID_EXPIRED_OR_ALREADY_USED")

    internal fun refresh() = operation {
        mutableAccounts.value = environment.manager.registeredAuthMethods()
        app.mergeSessions(environment.manager.pullPendingNotifications())
        app.report("REFRESH_COMPLETE")
    }

    internal fun decide(session: MfaAuthSession, approve: Boolean, number: String) = operation {
        val decision = try {
            if (approve) environment.manager.approve(session, number)
            else environment.manager.deny(session)
        } catch (error: MfaSdkException) {
            if (error.code.name == "OUTCOME_UNKNOWN" || error.code.name == "INVALID_SESSION") {
                app.removeSession(session)
            }
            throw error
        }
        app.removeSession(session)
        app.report(if (decision == MfaDecision.APPROVED) "APPROVED" else "DENIED")
    }

    internal fun retrySetup() = app.retrySetup()

    private fun operation(block: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        // Enrollment survives Activity/ViewModel destruction. Process loss is not replayed.
        app.operationScope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                app.reportFailure(error)
            } finally {
                mutableBusy.value = false
            }
        }
    }
}
