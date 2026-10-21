// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.internal

internal object MfaDiagnosticLog {
    const val enabled = false
    @Suppress("UNUSED_PARAMETER")
    fun write(message: String) = Unit
}
