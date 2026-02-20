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
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test

// [StrongKey] WPJ should be registered with strongkey by default - non shared device.
@SupportedBrokers(brokers = [BrokerMicrosoftAuthenticator::class])
@RetryOnFailure
class TestCase3522687 : AbstractMsalBrokerTest() {

    @Test
    fun test_3522687_WpjWithHardwareKeyByDefault() {
        Assume.assumeFalse(
            "performNonSharedWpjWithHardwareKey flight is not enabled, Test will be skipped",
            BuildConfig.COPY_OF_LOCAL_FLIGHTS_FOR_TEST_PURPOSES.contains("performNonSharedWpjWithHardwareKey:false")
        )

        // the actual account we need is tpcaandroid@msidlab4.onmicrosoft.com
        // But for some reason, this is not seachable by the API.
        val account = mLabClient.getLabAccount("idlab@msidlab4.onmicrosoft.com")
        val username = "tpcaandroid@msidlab4.onmicrosoft.com"

        (mBroker as BrokerMicrosoftAuthenticator).setShouldUseDeviceSettingsPage(false)
        mBroker.performDeviceRegistration(username, account.password)

        // Install BrokerHost app
        val brokerHost = BrokerHost()
        brokerHost.install()
        brokerHost.launch()

        // Check that the registration was done with strong keys.
        val deviceRegistrationRecords = brokerHost.multipleWpjApiFragment.allRecords
        Assert.assertEquals(1, deviceRegistrationRecords.size)
        Assert.assertEquals("true", deviceRegistrationRecords[0]["isRegisteredWithStrongKeys"])

        //acquiring token, no password prompt, no wpj upgrade prompt.
        val msalSdk = MsalSdk()
        val authTestParams: MsalAuthTestParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .resource(mScopes[0])
            .promptParameter(Prompt.SELECT_ACCOUNT)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val authResult: MsalAuthResult =
            msalSdk.acquireTokenInteractive(authTestParams, {
                // No action should be required.
            }, TokenRequestTimeout.MEDIUM)

        authResult.assertSuccess()
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