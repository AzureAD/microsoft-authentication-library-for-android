// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import com.microsoft.identity.mfa.MfaEnrollmentHandoff
import com.microsoft.identity.mfa.MfaErrorCode
import com.microsoft.identity.mfa.MfaHandoffDisposition
import com.microsoft.identity.mfa.MfaHandoffReceipt
import com.microsoft.identity.mfa.MfaHandoffSource
import java.net.URI

internal class MfaHandoffIntentContract(
    private val handoff: MfaEnrollmentHandoff,
    private val callbackUri: String
) {
    private val callback = try {
        URI(callbackUri).also {
            requireMfa(callbackUri.length <= 512 && it.scheme?.matches(Regex("[a-z][a-z0-9+.-]*")) == true &&
                it.host != null && it.rawUserInfo == null && it.port == -1 && it.rawQuery == null &&
                it.rawFragment == null && !it.isOpaque && it.normalize() == it &&
                !callbackUri.contains("%") && !callbackUri.contains("\\"),
                MfaErrorCode.INVALID_CONFIGURATION)
        }
    } catch (_: Exception) {
        fail(MfaErrorCode.INVALID_CONFIGURATION)
    }

    fun receive(action: String?, hasSelector: Boolean, uri: String?, payload: Any?): MfaHandoffReceipt {
        val target = try { uri?.takeIf { it.length <= 512 }?.let(::URI) } catch (_: Exception) { null }
        if (target?.scheme != callback.scheme || target?.host != callback.host) {
            return MfaHandoffReceipt(MfaHandoffDisposition.IGNORED)
        }
        if (action != "android.intent.action.VIEW" || hasSelector || uri != callbackUri || payload !is String) {
            return MfaHandoffReceipt(MfaHandoffDisposition.REJECTED, errorCode = MfaErrorCode.INVALID_ACTIVATION)
        }
        return handoff.stage(payload, MfaHandoffSource.BROWSER)
    }
}
