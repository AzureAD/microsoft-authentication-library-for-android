// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import android.content.Intent
import com.microsoft.identity.mfa.internal.MfaHandoffIntentContract

/**
 * Thin Intent boundary for an app-configured exact callback URI without query or fragment.
 * The activation HTTPS link travels only in [EXTRA_ACTIVATION_LINK], never callback query data.
 * This does not authenticate a browser/caller: native confirmation is still mandatory.
 * The host owns manifest routing and must clear handled Activity intents, including on recreation.
 */
class MfaHandoffIntentAdapter(handoff: MfaEnrollmentHandoff, callbackUri: String) {
    private val contract = MfaHandoffIntentContract(handoff, callbackUri)

    /** Validates an incoming Intent without retaining it, opening UI, navigating, or enrolling. */
    fun receive(intent: Intent?): MfaHandoffReceipt {
        if (intent == null) return MfaHandoffReceipt(MfaHandoffDisposition.IGNORED)
        return try {
            @Suppress("DEPRECATION")
            contract.receive(intent.action, intent.selector != null, intent.dataString,
                intent.extras?.get(EXTRA_ACTIVATION_LINK))
        } catch (_: RuntimeException) {
            MfaHandoffReceipt(MfaHandoffDisposition.REJECTED, errorCode = MfaErrorCode.INVALID_ACTIVATION)
        }
    }

    /** Stable browser handoff contract shared with the app's browser integration. */
    companion object {
        /** A String extra containing at most 8192 UTF-8 bytes of HTTPS activation-link data. */
        const val EXTRA_ACTIVATION_LINK = "com.microsoft.identity.mfa.ACTIVATION_LINK"
    }
}
