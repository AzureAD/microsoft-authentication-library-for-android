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

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure
import com.microsoft.identity.client.ui.automation.app.OutlookApp
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.ProtectionPolicy
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Assert
import org.junit.Test

// Using TrueMAM account will require a broker, and will require CP instead of Authenticator
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2516571
@RetryOnFailure
class TestCase2516571 : AbstractMsalUiTest(){

    @Test
    fun test_2516571() {
        // Fetch credentials
        val username: String = mLabAccount.username
        val password: String = mLabAccount.password

        val outlook = OutlookApp(LocalApkInstaller())
        outlook.install()
        outlook.launch()
        outlook.handleFirstRun()

        val promptHandlerParameters = FirstPartyAppPromptHandlerParameters.builder()
            .broker(null)
            .prompt(PromptParameter.SELECT_ACCOUNT)
            .loginHint(username)
            .consentPageExpected(false)
            .sessionExpected(false)
            .expectingBrokerAccountChooserActivity(false)
            .expectingLoginPageAccountPicker(false)
            .registerPageExpected(false)
            .build()

        // add first account in Outlook
        outlook.addFirstAccount(username, password, promptHandlerParameters)

        // Check for GO TO STORE button
        val intuneRequirementDialogConfirmBtn =
            UiAutomatorUtils.obtainUiObjectWithText("Get the app")
        Assert.assertTrue(intuneRequirementDialogConfirmBtn.exists())

        outlook.forceStop()

        // Test by installing Authenticator, this will remove need for 2516613, also 831545 seems
        // redundant if we're running this test case
        // Install authenticator, we should still see GO TO STORE page to download Company Portal
        val authenticator = BrokerMicrosoftAuthenticator()
        authenticator.install()

        val promptHandlerParametersWithAuthenticator = FirstPartyAppPromptHandlerParameters.builder()
            .broker(authenticator)
            .prompt(PromptParameter.SELECT_ACCOUNT)
            .loginHint(username)
            .consentPageExpected(false)
            .sessionExpected(false)
            .expectingBrokerAccountChooserActivity(false)
            .expectingLoginPageAccountPicker(false)
            .registerPageExpected(true)
            .build()
        // add first account in Outlook
        outlook.launch()
        outlook.addFirstAccount(username, password, promptHandlerParametersWithAuthenticator)

        // Check for GO TO STORE button
        val intuneRequirementDialogConfirmBtnAgain =
            UiAutomatorUtils.obtainUiObjectWithText("GO TO STORE")
        Assert.assertTrue(intuneRequirementDialogConfirmBtnAgain.exists())

        outlook.forceStop()

        // Install Company Portal, should now be able to log in with MAM account
        val companyPortal = BrokerCompanyPortal()
        companyPortal.install()

        // add account in Outlook after CP install
        outlook.launch()

        // Sometimes we get "Found account page", but sometimes it doesn't appear, let's try, and
        // try again by going back to the previous page if it doesn't work
        try {
            outlook.addExistingFirstAccount(username)
        } catch (exception: AssertionError) {
            // Return to starting screen to try again
            UiAutomatorUtils.pressBack()
            outlook.addExistingFirstAccount(username)
        }

        outlook.onAccountAdded()
        companyPortal.handleAppProtectionPolicy()
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
