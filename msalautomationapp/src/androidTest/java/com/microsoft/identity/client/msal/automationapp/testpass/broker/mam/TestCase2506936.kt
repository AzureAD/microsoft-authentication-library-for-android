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
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.app.TeamsApp
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal
import com.microsoft.identity.client.ui.automation.broker.IMdmAgent
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.ProtectionPolicy
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Test

// TrueMAM: Sign In with Teams and then SignOut and Sign Back In
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2506936
@SupportedBrokers(brokers = [BrokerCompanyPortal::class])
class TestCase2506936 : AbstractMsalBrokerTest(){

    @Test
    fun test_2506936() {
        // Fetch credentials
        val username: String = mLabAccount.username
        val password: String = mLabAccount.password

        val teams = TeamsApp(LocalApkInstaller())
        teams.install()
        teams.launch()
        teams.handleFirstRun()

        val teamsPromptHandlerParameters = FirstPartyAppPromptHandlerParameters.builder()
            .prompt(PromptParameter.SELECT_ACCOUNT)
            .loginHint(username)
            .broker(mBroker)
            .registerPageExpected(true)
            .enrollPageExpected(false)
            .consentPageExpected(false)
            .speedBumpExpected(false)
            .build()

        // Sign in the first time
        teams.addFirstAccount(username, password, teamsPromptHandlerParameters)
        // handle app protection policy in CP i.e. setup PIN when asked
        (mBroker as IMdmAgent).handleAppProtectionPolicy()
        teams.onAccountAdded()
        teams.forceStop() // Teams sometimes seems to like to pop up on screen randomly

        teams.signOut()

        // Sign in again
        val teamsPromptHandlerParameters2 = FirstPartyAppPromptHandlerParameters.builder()
            .prompt(PromptParameter.SELECT_ACCOUNT)
            .loginHint(username)
            .broker(mBroker)
            .registerPageExpected(false)
            .enrollPageExpected(false)
            .consentPageExpected(false)
            .speedBumpExpected(false)
            .secondPasswordPageExpected(true)
            .build()

        teams.addFirstAccount(username, password, teamsPromptHandlerParameters2)
        // handle app protection policy in CP i.e. setup PIN when asked
        (mBroker as IMdmAgent).handleAppProtectionPolicy()
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