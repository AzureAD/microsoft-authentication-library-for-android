// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.mfa.sample

import android.content.Context
import com.microsoft.identity.mfa.poc.MfaPocFactory
import com.microsoft.identity.mfa.poc.MfaPocScenario
import kotlinx.coroutines.flow.MutableStateFlow

internal object EnvironmentFactory {
    fun create(context: Context): SampleMfaEnvironment {
        val controller = MfaPocFactory.create(context.applicationContext)
        return object : SampleMfaEnvironment {
            override val manager = controller.manager
            override val modeLabel = "MOCK — synthetic fixtures; no live service"
            override val setup = MutableStateFlow(SampleSetup.STARTING)
            override val mock = object : MockControls {
                override val scenarios = MfaPocScenario.values().map { it.name }
                override val operations = controller.operations
                override val qrCode get() = controller.qrCode
                override val activationCode get() = controller.activationCode
                override val activationUrl get() = controller.activationUrl
                override val signInNumber get() = controller.signInNumber
                override fun setScenario(name: String) =
                    controller.setScenario(MfaPocScenario.valueOf(name))
                override suspend fun injectValidation() = controller.injectValidation()
                override suspend fun injectChallenge() = controller.injectChallenge()
                override suspend fun reset() = controller.reset()
            }

            override suspend fun initialize() {
                controller.initialize()
                setup.value = SampleSetup.TOKEN_ACQUIRED
            }

            override fun prepareNotifications() = Unit
            override fun notifyPending() = Unit
        }
    }
}
