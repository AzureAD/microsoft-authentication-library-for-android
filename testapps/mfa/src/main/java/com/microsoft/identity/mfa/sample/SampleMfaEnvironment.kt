// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.MfaSdkManager
import kotlinx.coroutines.flow.StateFlow

internal enum class SampleSetup {
    STARTING, FIREBASE_SETUP_REQUIRED, ACQUIRING_TOKEN, TOKEN_ACQUIRED, TOKEN_ACQUISITION_FAILED
}

internal interface SampleMfaEnvironment {
    val manager: MfaSdkManager
    val modeLabel: String
    val setup: StateFlow<SampleSetup>
    suspend fun initialize()
    fun prepareNotifications()
    fun notifyPending()
}
