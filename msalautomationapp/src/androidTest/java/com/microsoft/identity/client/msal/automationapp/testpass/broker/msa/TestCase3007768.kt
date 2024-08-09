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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.msa

import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler
import com.microsoft.identity.client.ui.automation.utils.CommonUtils
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Assert
import org.junit.Test
import java.util.AbstractMap
import java.util.Arrays

// [Brokered] Sign up flow for MSA Accounts
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3007768
@SupportedBrokers(brokers = [BrokerMicrosoftAuthenticator::class])
@LocalBrokerHostDebugUiTest
//@RetryOnFailure
class TestCase3007768 : AbstractMsalBrokerTest(){
    @Test
    @Throws(Throwable::class)
    fun test_3007768() {
        // Passing this parameter will enable sign up page
        val extraQP: MutableList<Map.Entry<String, String>> = ArrayList()
        extraQP.add(AbstractMap.SimpleEntry("signup", "1"))

        val msalSdk = MsalSdk()
        val createAccountParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(null)
            .scopes(Arrays.asList(*mScopes))
            .extraQueryParameters(extraQP)
            .promptParameter(Prompt.SELECT_ACCOUNT)
            .msalConfigResourceId(configFileResourceId)
            .build()

        // AcquireToken request should lead to sign up page
        msalSdk.acquireTokenInteractive(createAccountParams, {
            // Do nothing, we're just checking for create account UI
        }, TokenRequestTimeout.SHORT)

        val createAccountText = UiAutomatorUtils.obtainUiObjectWithText("Create account")
        Assert.assertTrue(createAccountText.waitForExists(CommonUtils.FIND_UI_ELEMENT_TIMEOUT))

        // Exit current auth
        UiAutomatorUtils.pressBack()

        // Now call AcquireToken with an existing MSA account
        val username = mLabAccount.username
        val password = mLabAccount.password
        val authTestParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .scopes(Arrays.asList(*mScopes))
            .promptParameter(Prompt.SELECT_ACCOUNT)
            .msalConfigResourceId(configFileResourceId)
            .build()
        val authResult = msalSdk.acquireTokenInteractive(authTestParams, {
            val promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .sessionExpected(false)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .broker(mBroker)
                .expectingBrokerAccountChooserActivity(false)
                .build()
            AadPromptHandler(promptHandlerParameters)
                .handlePrompt(username, password)
        }, TokenRequestTimeout.MEDIUM)
        authResult.assertSuccess()

        // Run Create Account UI Test again, should still see ui even if logged in with another MSA account
        msalSdk.acquireTokenInteractive(createAccountParams, {
            // Do nothing, we're just checking for create account UI
        }, TokenRequestTimeout.SHORT)
        Assert.assertTrue(createAccountText.waitForExists(CommonUtils.FIND_UI_ELEMENT_TIMEOUT))

    }

    override fun getScopes(): Array<String> {
        return arrayOf("User.read")
    }

    override fun getAuthority(): String {
        return mApplication.configuration.defaultAuthority.toString()
    }

    override fun getConfigFileResourceId(): Int {
        return R.raw.msal_config_msa_only
    }

    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
            .userType(UserType.MSA)
            .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null;
    }
}
