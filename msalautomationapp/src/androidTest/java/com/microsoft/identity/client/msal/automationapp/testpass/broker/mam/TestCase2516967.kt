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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.mam

import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.app.OutlookApp
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal
import com.microsoft.identity.client.ui.automation.broker.BrokerHost
import com.microsoft.identity.client.ui.automation.broker.IMdmAgent
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.common.java.util.ThreadUtils
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.ProtectionPolicy
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Assert
import org.junit.Test

// Can use Outlook with True MAM account upon re-registration
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2516967
@SupportedBrokers(brokers = [BrokerCompanyPortal::class])
@RetryOnFailure
class TestCase2516967 : AbstractMsalBrokerTest(){

    @Test
    fun test_2516967() {
        // Fetch credentials
        val username: String = mLabAccount.username
        val password: String = mLabAccount.password

        val outlook = OutlookApp(LocalApkInstaller())
        outlook.install()
        outlook.launch()
        outlook.handleFirstRun()

        val promptHandlerParameters = FirstPartyAppPromptHandlerParameters.builder()
            .broker(mBroker)
            .prompt(PromptParameter.SELECT_ACCOUNT)
            .loginHint(username)
            .consentPageExpected(false)
            .sessionExpected(false)
            .expectingBrokerAccountChooserActivity(false)
            .expectingLoginPageAccountPicker(false)
            .registerPageExpected(true)
            .build()

        // add first account in Outlook
        outlook.addFirstAccount(username, password, promptHandlerParameters)
        outlook.onAccountAdded()
        // handle app protection policy in CP i.e. setup PIN when asked
        (mBroker as IMdmAgent).handleAppProtectionPolicy()

        val brokerHost = BrokerHost()
        brokerHost.install()
        brokerHost.wpjLeave()

        // advance clock by more than an hour to expire AT in cache
        settingsScreen.forwardDeviceTimeForOneDay()

        // Log in again in outlook, should get a prompt in the snackbar
        outlook.launch()
        outlook.signInThroughSnackBar(username, password, promptHandlerParameters)

        // Not totally sure what prompt outlook to take the snackbar away, sometimes it still appears after re-authentication
        // We wait a bit and relaunch outlook twice, this seems improve the chance of the snackbar disappearing
        ThreadUtils.sleepSafely(6000, "sleeping", "interrupted sleep")
        outlook.forceStop()
        outlook.launch()
        outlook.forceStop()
        outlook.launch()

        Assert.assertFalse("SIGN IN Button still present", outlook.isSignInSnackBarPresent)
        outlook.confirmAccount(username)
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

    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
            .userType(UserType.CLOUD)
            .protectionPolicy(ProtectionPolicy.TRUE_MAM_CA)
            .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }
}
