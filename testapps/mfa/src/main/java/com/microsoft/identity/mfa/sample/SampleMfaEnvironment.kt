// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaSdkManager
import kotlinx.coroutines.flow.StateFlow

internal enum class SampleSetup {
    STARTING, FIREBASE_SETUP_REQUIRED, ACQUIRING_TOKEN, TOKEN_ACQUIRED, TOKEN_ACQUISITION_FAILED
}

internal interface SampleMfaEnvironment {
    val manager: MfaSdkManager
    val modeLabel: String
    val setup: StateFlow<SampleSetup>
    val mock: MockControls?
    suspend fun initialize()
    fun prepareNotifications()
    fun notifyPending()
}

internal interface MockControls {
    val scenarios: List<String>
    val operations: StateFlow<List<String>>
    val qrCode: String
    val activationCode: String
    val activationUrl: String
    val signInNumber: String
    fun setScenario(name: String)
    suspend fun injectValidation()
    suspend fun injectChallenge(): List<MfaAuthSession>
    suspend fun reset()
}
