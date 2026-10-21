// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa

import com.microsoft.identity.mfa.internal.requireMfa

/** Experimental policy selection. Selecting a profile does not grant service authorization. */
enum class MfaPolicyProfile {
    /** Require primary and companion policies to permit the decision; the default for other hosts. */
    CONSERVATIVE,
    /** Use the primary policy only for an explicitly authorized Authenticator-identity experiment. */
    AUTHENTICATOR;

    internal fun validateHost(packageName: String) {
        requireMfa(this != AUTHENTICATOR || packageName == "com.azure.authenticator",
            MfaErrorCode.INVALID_CONFIGURATION)
    }
}
