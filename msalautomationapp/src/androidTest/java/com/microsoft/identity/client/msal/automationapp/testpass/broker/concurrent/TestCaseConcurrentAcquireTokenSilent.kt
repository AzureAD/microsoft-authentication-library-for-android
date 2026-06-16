//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.broker.concurrent

import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.claims.ClaimsRequest
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout
import com.microsoft.identity.client.ui.automation.annotations.LongUIAutomationTest
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure
import com.microsoft.identity.client.ui.automation.annotations.StressTest
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler
import com.microsoft.identity.common.java.providers.oauth2.IDToken
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Assert
import org.junit.Test

/**
 * Concurrent stress test for `AcquireTokenSilent` through the broker on a
 * Workplace-Joined (WPJ) device: register the device inline via a `deviceid`
 * claim on a single interactive sign-in, then fire [ITERATIONS] barrier-
 * synchronized waves of [CONCURRENT_THREADS] simultaneous `forceRefresh` silent
 * calls and assert none hangs or errors.
 *
 * Each thread uses a distinct scope so the `CommandDispatcher` can't de-duplicate
 * the simultaneous in-flight commands; the WPJ PRT satisfies every scope silently.
 * [CONCURRENT_THREADS] equals the pool size so every concurrent request is unique.
 */
@SupportedBrokers(brokers = [BrokerMicrosoftAuthenticator::class])
@StressTest
@LongUIAutomationTest
class TestCaseConcurrentAcquireTokenSilent : AbstractMsalBrokerTest() {

    @Test
    fun test_concurrentAcquireTokenSilent_withBroker() {
        val username = mLabAccount.username
        val password = mLabAccount.password

        val msalSdk = MsalSdk()

        // Inline WPJ: a deviceid claim on the interactive sign-in registers the
        // device (broker gets a PRT) and establishes the account in one flow.
        val deviceIdClaims = ClaimsRequest().apply {
            requestClaimInIdToken(
                "deviceid",
                RequestedClaimAdditionalInformation().apply { setEssential(true) },
            )
        }

        val interactiveParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .scopes(listOf(*mScopes))
            .promptParameter(Prompt.LOGIN)
            .claims(deviceIdClaims)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val interactiveResult = msalSdk.acquireTokenInteractive(
            interactiveParams,
            OnInteractionRequired {
                val promptHandlerParameters = PromptHandlerParameters.builder()
                    .prompt(PromptParameter.LOGIN)
                    .loginHint(username)
                    .broker(mBroker)
                    .sessionExpected(false)
                    .registerPageExpected(true)
                    .consentPageExpected(false)
                    .speedBumpExpected(false)
                    .expectingLoginPageAccountPicker(false)
                    .build()

                AadPromptHandler(promptHandlerParameters).handlePrompt(username, password)
            },
            TokenRequestTimeout.MEDIUM,
        )

        interactiveResult.assertSuccess()

        // Confirm the device actually registered (deviceid present in the token).
        val claims = IDToken.parseJWT(interactiveResult.accessToken)
        Assert.assertNotNull("deviceid claim must be present after inline WPJ", claims["deviceid"])

        val account = msalSdk.getAccount(mActivity, configFileResourceId, username)
        Assert.assertNotNull("Account must not be null after a successful interactive sign-in", account)

        // Distinct scope per thread → no command de-duplication.
        val result = ConcurrentAcquireTokenSilentHelper.run(
            CONCURRENT_THREADS,
            ITERATIONS,
            PER_WAVE_TIMEOUT_SECONDS,
        ) { threadIndex, iteration, done, errors ->
            val silentParameters = AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.authority)
                .withScopes(ConcurrentAcquireTokenSilentHelper.scopesForThread(threadIndex))
                .forceRefresh(true)
                .withCallback(object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        done.countDown()
                    }

                    override fun onError(exception: MsalException) {
                        errors.add("Thread $threadIndex iter $iteration [${exception.errorCode}]: $exception")
                        done.countDown()
                    }
                })
                .build()

            mApplication.acquireTokenSilentAsync(silentParameters)
        }

        Assert.assertTrue(
            "Concurrent AcquireTokenSilent got stuck: not all $CONCURRENT_THREADS threads" +
                " completed $ITERATIONS waves (per-wave timeout ${PER_WAVE_TIMEOUT_SECONDS}s)",
            result.allCompleted,
        )
        Assert.assertTrue(
            "Some concurrent AcquireTokenSilent calls failed: ${result.errors}",
            result.errors.isEmpty(),
        )
    }

    override fun getJsonUserType(): UserType = UserType.BASIC

    override fun getTempUserType(): TempUserType? = null

    override fun getScopes(): Array<String> = arrayOf("User.read")

    override fun getAuthority(): String =
        mApplication.configuration.defaultAuthority.toString()

    override fun getConfigFileResourceId(): Int = R.raw.msal_config_default

    companion object {
        /** One thread per pooled scope, so every concurrent request is unique. */
        private val CONCURRENT_THREADS = ConcurrentAcquireTokenSilentHelper.SCOPE_POOL.size
        private const val ITERATIONS = 20
        private const val PER_WAVE_TIMEOUT_SECONDS = 15L
    }
}
