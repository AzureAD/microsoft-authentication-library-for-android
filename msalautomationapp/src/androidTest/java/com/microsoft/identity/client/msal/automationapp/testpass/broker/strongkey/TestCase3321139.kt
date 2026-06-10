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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.strongkey

import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Test

// [StrongKey] Signs in with Token Protection-CA account
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3321139
@SupportedBrokers(brokers = [BrokerMicrosoftAuthenticator::class])
class TestCase3321139 : AbstractMsalBrokerTest() {
    @Test
    fun test_3321139_SignInWithTpCaAccount() {
        val username = mLabAccount.username
        val password = mLabAccount.password

        val msalSdk = MsalSdk()

        //acquiring token
        val authTestParams: MsalAuthTestParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .scopes(listOf(*mScopes))
            .promptParameter(Prompt.SELECT_ACCOUNT)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val authResult: MsalAuthResult =
            msalSdk.acquireTokenInteractive(authTestParams, object : OnInteractionRequired {
                override fun handleUserInteraction() {
                    val promptHandlerParameters: PromptHandlerParameters =
                        PromptHandlerParameters.builder()
                            .prompt(PromptParameter.SELECT_ACCOUNT)
                            .loginHint(username)
                            .consentPageExpected(false)
                            .speedBumpExpected(false)
                            .broker(mBroker)
                            .expectingLoginPageAccountPicker(false)
                            .enrollPageExpected(true)
                            .batteryOptimizationIgnoreSystemPromptExpected(true)
                            .build()

                    AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password)
                }
            }, TokenRequestTimeout.MEDIUM)

        authResult.assertSuccess()
    }

    override fun getJsonUserType(): UserType? {
        return UserType.TOKEN_BINDING
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    override fun getScopes(): Array<String> {
        return arrayOf("user.read")
    }

    override fun getAuthority(): String {
        return mApplication.configuration.defaultAuthority.authorityURL.toString()
    }

    override fun getConfigFileResourceId(): Int {
        return R.raw.msal_config_msidlab4
    }
}