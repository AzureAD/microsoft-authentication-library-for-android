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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.mwpj

import com.microsoft.identity.client.Prompt
import com.microsoft.identity.client.claims.ClaimsRequest
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation
import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerHost
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters
import com.microsoft.identity.client.ui.automation.rules.LoadLabUserTestRule
import com.microsoft.identity.labapi.utilities.client.ILabAccount
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import com.microsoft.identity.labapi.utilities.jwt.JWTParserFactory
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

// Verify WPJ Cert installation on a Non samsung device with Authenticator
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2521768
// Technically this works on Samsung device too (at least Galaxy S6)
// So this should also cover TestCase831570
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2521768
@SupportedBrokers(brokers = [BrokerHost::class])
class TestCase2521768 : AbstractMsalBrokerTest() {

    private lateinit var mSecondLabAccount: ILabAccount

    @get:Rule
    val loadAdditionalLabUserRule: TestRule = LoadLabUserTestRule(TempUserType.BASIC)

    @Test
    fun test_2521768() {
        val username = mLabAccount.username
        val password = mLabAccount.password
        val secondUsername = mSecondLabAccount.username
        val secondPassword = mSecondLabAccount.password
        val brokerHostApp = broker as BrokerHost

        // Make an interactive call with MSAL

        // Make an interactive call with MSAL
        val msalSdk = MsalSdk()
        val authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(listOf(*mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(configFileResourceId)
                .build()

        val authResult = msalSdk.acquireTokenInteractive(
                authTestParams,
                {
                    val promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                            .prompt(PromptParameter.SELECT_ACCOUNT)
                            .loginHint(username)
                            .sessionExpected(false)
                            .consentPageExpected(false)
                            .build()
                    MicrosoftStsPromptHandler(promptHandlerParameters).handlePrompt(username, password)
                },
                TokenRequestTimeout.MEDIUM
        )
        authResult.assertSuccess()

        // Account must be different but home tenant id must be same
        Assert.assertEquals(mSecondLabAccount.homeTenantId, mLabAccount.homeTenantId)
        Assert.assertNotEquals(secondUsername, username)

        brokerHostApp.performDeviceRegistrationMultiple(secondUsername, secondPassword)


        // start silent token request in MSAL
        val authTestSilentParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(listOf(*mScopes))
                .authority(getAuthority())
                .resource(mScopes[0])
                .msalConfigResourceId(configFileResourceId)
                .build()
        val authResult2 = msalSdk.acquireTokenSilent(authTestSilentParams, TokenRequestTimeout.MEDIUM)
        authResult2.assertSuccess()

        val claims = JWTParserFactory.INSTANCE.jwtParser.parseJWT(authResult2.accessToken)
        Assert.assertFalse("Device id clim is present", claims.containsKey("deviceid"))


        // create claims request object
        val claimsRequest = ClaimsRequest()
        val requestedClaimAdditionalInformation = RequestedClaimAdditionalInformation()
        requestedClaimAdditionalInformation.essential = true
        claimsRequest.requestClaimInIdToken("deviceid", requestedClaimAdditionalInformation)

        val authTestSilentParamsWithClaim = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(listOf(*mScopes))
                .authority(getAuthority())
                .resource(mScopes[0])
                .claims(claimsRequest)
                .msalConfigResourceId(configFileResourceId)
                .build()

        val authResult3 = msalSdk.acquireTokenSilent(authTestSilentParamsWithClaim, TokenRequestTimeout.MEDIUM)
        authResult3.assertFailure()
        Assert.assertNotNull("exception message is null" + authResult3.exception, authResult3.exception.message)
        Assert.assertTrue(
                "exception message is not as expected" + authResult3.exception.message,
                authResult3.exception.message!!.contains("AADSTS50187")
        )


        val authTestparamwithclaim = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(listOf(*mScopes))
                .claims(claimsRequest)
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(configFileResourceId)
                .build()


        val authResult4 = msalSdk.acquireTokenInteractive(
                authTestparamwithclaim,
                {
                    val promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                            .prompt(PromptParameter.WHEN_REQUIRED)
                            .loginHint(username)
                            .consentPageExpected(false)
                            .passwordPageExpected(false)
                            .sessionExpected(true)
                            .build()
                    MicrosoftStsPromptHandler(promptHandlerParameters).handlePrompt(username, password)
                },
                TokenRequestTimeout.MEDIUM
        )
        authResult4.assertSuccess()
        val claims2 = JWTParserFactory.INSTANCE.jwtParser.parseJWT(authResult4.accessToken)
        Assert.assertTrue("Device id clim is present", claims2.containsKey("deviceid"))
    }

    /**
     * Get the scopes that can be used for an acquire token test.
     *
     * @return A string array consisting of OAUTH2 Scopes
     */
    override fun getScopes(): Array<String> {
        return arrayOf("User.read")
    }

    /**
     * Get the authority url that can be used for an acquire token test.
     *
     * @return A string representing the url for an authority that can be used as token issuer
     */
    override fun getAuthority(): String {
        return mApplication.configuration.defaultAuthority.authorityURL.toString()
    }

    /**
     * The MSAL config file that should be used to create a PublicClientApplication for the test.
     *
     * @return config file resource id
     */
    override fun getConfigFileResourceId(): Int {
        return R.raw.msal_config_default
    }

    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    @Before
    fun before() {
        mSecondLabAccount = (loadAdditionalLabUserRule as LoadLabUserTestRule).labAccount
    }
}