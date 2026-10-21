// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

import com.microsoft.identity.common.logging.Logger

internal object MfaDiagnosticLog {
    @Volatile
    var enabled = false

    fun write(message: String) {
        Logger.info("MfaPoc", message)
    }
}
