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
import com.microsoft.identity.client.msal.automationapp.BuildConfig
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerHost
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Assert
import org.junit.Assume
import org.junit.Test

//  [StrongKey] Upgrade from regular WPJ to StrongKey WPJ (via CA)
// https://identitydivision.visualstudio.com/Engineering/_testPlans/define?planId=2007357&suiteId=3321136
@SupportedBrokers(brokers = [BrokerMicrosoftAuthenticator::class])
@RetryOnFailure
class TestCase3321136 : AbstractMsalBrokerTest() {
    @Test
    fun test_3321136_UpgradeFromRegularWpjToStrongKeyWpj() {
        Assume.assumeFalse(
            "performNonSharedWpjWithHardwareKey flight is enabled, Test will be skipped",
            BuildConfig.COPY_OF_LOCAL_FLIGHTS_FOR_TEST_PURPOSES.contains("performNonSharedWpjWithHardwareKey:true")
        )

        val basicUser = mLabClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.TP_CA)

        (mBroker as BrokerMicrosoftAuthenticator).setShouldUseDeviceSettingsPage(false)
        mBroker.performDeviceRegistration(basicUser.username, basicUser.password)

        // Install BrokerHost app
        val brokerHost = BrokerHost()
        brokerHost.install()
        brokerHost.launch()

        // Check that the initial registration is NOT using strong keys (pre-CA state).
        val wpjRecordPreCA = brokerHost.multipleWpjApiFragment.getRecordByUpn(basicUser.username)
        Assert.assertEquals("false", wpjRecordPreCA["isRegisteredWithStrongKeys"])

        val msalSdk = MsalSdk()

        //acquiring token
        val authTestParams: MsalAuthTestParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(basicUser.username)
            .resource(mScopes[0])
            .promptParameter(Prompt.SELECT_ACCOUNT)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val authResult: MsalAuthResult =
            msalSdk.acquireTokenInteractive(authTestParams, object : OnInteractionRequired {
                override fun handleUserInteraction() {
                    val promptHandlerParameters: PromptHandlerParameters =
                        PromptHandlerParameters.builder()
                            .prompt(PromptParameter.SELECT_ACCOUNT)
                            .loginHint(basicUser.username)
                            .passwordPageExpected(false)
                            .consentPageExpected(false)
                            .speedBumpExpected(false)
                            .broker(mBroker)
                            .expectingLoginPageAccountPicker(false)
                            .registerPageExpected(true)
                            .build()

                    AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(basicUser.username, basicUser.password)
                }
            }, TokenRequestTimeout.MEDIUM)

        authResult.assertSuccess()

        brokerHost.launch()

        // Check that the registration was done with strong keys.
        val wpjRecord = brokerHost.multipleWpjApiFragment.getRecordByUpn(basicUser.username)
        Assert.assertEquals("true", wpjRecord["isRegisteredWithStrongKeys"])
        Assert.assertEquals(wpjRecordPreCA["DeviceId"], wpjRecord["DeviceId"]) // device id mustn't change.
    }

    override fun getLabQuery(): LabQuery? {
        return LabQuery.builder()
            .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
            .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    override fun getScopes(): Array<String> {
        return arrayOf("User.read")
    }

    override fun getAuthority(): String {
        return mApplication.configuration.defaultAuthority.authorityURL.toString()
    }

    override fun getConfigFileResourceId(): Int {
        return R.raw.msal_config_default
    }
}