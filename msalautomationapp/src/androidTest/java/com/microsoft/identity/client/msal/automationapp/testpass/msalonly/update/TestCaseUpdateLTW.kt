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
package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.update

import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalCustomBrokerInstallationTest
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout
import com.microsoft.identity.client.ui.automation.annotations.LTWTests
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW
import com.microsoft.identity.client.ui.automation.constants.AuthScheme
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import org.junit.Ignore
import org.junit.Test
import java.util.*

// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2516681
@RetryOnFailure
@LTWTests
class TestCaseUpdateLTW : AbstractMsalCustomBrokerInstallationTest() {

    private val mBrokerLTW: BrokerLTW = installOldLtw()

    @Test
    @Throws(Throwable::class)
    fun test_UpdateLTW() {
        val username = mLabAccount.username
        val password = mLabAccount.password

        val msalSdk = MsalSdk()
        val authTestParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .scopes(Arrays.asList(*mScopes))
            .promptParameter(Prompt.LOGIN)
            .authScheme(AuthScheme.BEARER)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val authResult = msalSdk.acquireTokenInteractive(authTestParams, {
            val promptHandlerParameters = PromptHandlerParameters.builder()
                .prompt(PromptParameter.LOGIN)
                .loginHint(username)
                .sessionExpected(false)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .broker(mBrokerLTW)
                .expectingBrokerAccountChooserActivity(false)
                .build()
            AadPromptHandler(promptHandlerParameters)
                .handlePrompt(username, password)
        }, TokenRequestTimeout.MEDIUM)

        // Check if auth result is success
        authResult.assertSuccess()

        // Update the LTW app
        mBrokerLTW.update()
        // start silent token request in MSAL

        val authTestSilentParams = MsalAuthTestParams.builder()
            .activity(mActivity)
            .loginHint(username)
            .scopes(Arrays.asList(*mScopes))
            .authority(authority)
            .authScheme(AuthScheme.BEARER)
            .msalConfigResourceId(configFileResourceId)
            .build()

        val authResultPostUpdate: MsalAuthResult =
            msalSdk.acquireTokenSilent(authTestSilentParams, TokenRequestTimeout.SILENT)
        authResultPostUpdate.assertSuccess()
    }

    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
            .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
            .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    override fun getScopes(): Array<String>? {
        return arrayOf("User.read")
    }

    override fun getAuthority(): String? {
        return mApplication.configuration.defaultAuthority.authorityURL.toString()
    }

    override fun getConfigFileResourceId(): Int {
        return R.raw.msal_config_default
    }
}