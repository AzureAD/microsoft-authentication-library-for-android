// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.poc

import android.content.Context
import com.microsoft.identity.mfa.MfaAuthSession
import com.microsoft.identity.mfa.MfaSdkManager
import com.microsoft.identity.mfa.internal.EncryptedMfaStore
import com.microsoft.identity.mfa.internal.MfaServiceClient
import java.net.URLEncoder
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Debug-only construction. The transport has no network delegate or fallback. */
object MfaPocFactory {
    private val controllers = HashMap<String, MfaPocController>()

    /** Returns the application-scoped mock controller with an isolated encrypted state file. */
    @JvmStatic
    fun create(context: Context): MfaPocController = synchronized(controllers) {
        val app = context.applicationContext
        controllers.getOrPut(app.packageName) {
            val metadata = MfaSdkManager.applicationClient(app)
            val fixture = FixtureMfaTransport()
            val manager = MfaSdkManager(EncryptedMfaStore(app, "msal-mfa-mock-v1.bin"),
                MfaServiceClient(fixture, metadata.appName, metadata.appVersion,
                    metadata.osVersion, metadata.deviceName), true)
            MfaPocController(manager, fixture)
        }
    }
}

/** Mock-only fixture injection. All visible registration and challenge results still use the SDK. */
class MfaPocController internal constructor(
    /** The real SDK manager configured with a synthetic wire transport. */
    val manager: MfaSdkManager,
    private val fixture: FixtureMfaTransport
) {
    private val mutex = Mutex()

    /** Bounded, sanitized operation names; never includes requests, tokens, or account identifiers. */
    val operations: StateFlow<List<String>> = fixture.operations
    /** Synthetic activation code, not a service credential. */
    val activationCode: String = FixtureMfaTransport.CODE
    /** Synthetic endpoint accepted by policy, intercepted entirely by the fixture transport. */
    val activationUrl: String = "https://${FixtureMfaTransport.HOST}"
    /** Equivalent synthetic legacy QR input. */
    val qrCode: String = "phonefactor://activate_account?code=$activationCode&url=" +
        URLEncoder.encode(activationUrl, "UTF-8")
    /** Tester-only sign-in page number; deliberately incorrect for the WRONG_NUMBER scenario. */
    val signInNumber: String get() =
        if (fixture.scenario == MfaPocScenario.WRONG_NUMBER) "41" else FixtureMfaTransport.EXPECTED_NUMBER

    /** Seeds/restores the synthetic token. Call before performing sample actions. */
    suspend fun initialize() = mutex.withLock {
        manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
    }

    /** Selects the next fixture behavior; does not mutate SDK state or replay submitted decisions. */
    fun setScenario(scenario: MfaPocScenario) { fixture.scenario = scenario }

    /** Delivers validation independently of registerAuthMethod, which is waiting without a lock. */
    suspend fun injectValidation() {
        manager.handlePushNotification(fixture.validationPayload())
    }

    /** Creates and fetches a fresh synthetic number-matching challenge. */
    suspend fun injectChallenge(): List<MfaAuthSession> =
        manager.handlePushNotification(fixture.issue())

    /** Resets only isolated mock state, retaining this controller and manager instance. */
    suspend fun reset() = mutex.withLock {
        manager.resetPocState()
        fixture.reset()
        manager.updatePushRegistration(FixtureMfaTransport.TOKEN)
    }
}
