// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaDecision
import com.microsoft.identity.mfa.MfaEnrollmentStage
import com.microsoft.identity.mfa.MfaRegistrationParameters
import com.microsoft.identity.mfa.MfaRegistrationResult
import com.microsoft.identity.mfa.MfaSdkException
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
    private val mutableAccounts = MutableStateFlow<List<MfaRegistrationResult>>(emptyList())
    internal val accounts: StateFlow<List<MfaRegistrationResult>> = mutableAccounts
    private val mutableBusy = MutableStateFlow(false)
    internal val busy: StateFlow<Boolean> = mutableBusy
    private val mutableInjecting = MutableStateFlow(false)
    internal val injecting: StateFlow<Boolean> = mutableInjecting
    internal var selectedScenario: Int = 0

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

    internal fun register(input: MfaRegistrationParameters) = operation {
        environment.manager.registerAuthMethod(input)
        mutableAccounts.value = environment.manager.registeredAuthMethods()
        app.report("REGISTERED")
    }

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

    internal fun injectChallenge() = operation {
        environment.mock?.let {
            app.mergeSessions(it.injectChallenge())
            app.report("MOCK_CHALLENGE_INJECTED")
        }
    }

    internal fun injectValidation() {
        val mock = environment.mock ?: return
        if (mutableInjecting.value) return
        mutableInjecting.value = true
        // Registration intentionally waits for this separate operation; never gate it on busy.
        viewModelScope.launch {
            try {
                mock.injectValidation()
                app.report("MOCK_VALIDATION_INJECTED")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                app.reportFailure(error)
            } finally {
                mutableInjecting.value = false
            }
        }
    }

    internal fun resetMock() = operation {
        environment.mock?.let {
            it.reset()
            it.setScenario(it.scenarios[selectedScenario])
            app.clearSessions()
            mutableAccounts.value = environment.manager.registeredAuthMethods()
            app.report("MOCK_RESET")
        }
    }

    internal fun retrySetup() = app.retrySetup()

    private fun operation(block: suspend () -> Unit) {
        if (mutableBusy.value) return
        mutableBusy.value = true
        viewModelScope.launch {
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
