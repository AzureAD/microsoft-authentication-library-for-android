// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.poc

import com.microsoft.identity.common.logging.Logger
import com.microsoft.identity.mfa.internal.MfaDiagnosticLog
import com.microsoft.identity.mfa.internal.MfaDiagnostics

/** Opt-in, debug-only Logcat diagnostics. Never accepts payloads or arbitrary message text. */
object MfaPocDiagnostics {
    /** Bounded sample lifecycle markers, separate from SDK/service operations. */
    enum class Event {
        SAMPLE_START, FCM_RECEIVED, FCM_NOTIFICATION_PRESENT, FCM_TOKEN_RECEIVED,
        PUSH_DISPATCH, PUSH_EMPTY, PUSH_INVALID, PUSH_COMPLETE, CALLBACK_BUSY, CALLBACK_TIMEOUT,
        TOKEN_ACQUIRED, REGISTER_START, REGISTER_END
    }

    /** Enables the shared Logger's non-PII Logcat output for this debug sample process. */
    @JvmStatic
    fun enable() {
        Logger.setAllowPii(false)
        Logger.setAllowLogcat(true)
        Logger.getInstance().setLogLevel(Logger.LogLevel.INFO)
        MfaDiagnosticLog.enabled = true
        event(Event.SAMPLE_START)
    }

    /** Records only a predefined lifecycle marker under the MfaPoc tag. */
    @JvmStatic
    fun event(event: Event) {
        MfaDiagnostics.event("sample.${event.name}")
    }

    /** Records a bounded error category, never exception messages, bodies or stack traces. */
    @JvmStatic
    fun failure(error: Exception) {
        MfaDiagnostics.failure("sample", error)
    }
}
