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
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerHost
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters
import com.microsoft.identity.labapi.utilities.client.ILabAccount
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import com.microsoft.identity.labapi.utilities.jwt.JWTParserFactory
import org.junit.Assert
import org.junit.Before
import org.junit.Test

// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2578879
// [MWPJ] An account with no PRT use no Joined flow even if the tenant is registered (Pkey AUth enable)
@SupportedBrokers(brokers = [BrokerHost::class])
@LocalBrokerHostDebugUiTest
@RetryOnFailure
class TestCase2578879 : AbstractMsalBrokerTest() {

    private lateinit var mLabAccount2: ILabAccount
    private lateinit var mBrokerHostApp: BrokerHost

    @Test
    fun test_2578879() {
        // Make an interactive call with MSAL using the first account
        val msalSdk = MsalSdk()
        val authTestParamsForInteractiveRequest = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLabAccount.username)
                .scopes(listOf(*mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(configFileResourceId)
                .build()

        val authResult = msalSdk.acquireTokenInteractive(
                authTestParamsForInteractiveRequest,
                {
                    val promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                            .prompt(PromptParameter.SELECT_ACCOUNT)
                            .loginHint(mLabAccount.username)
                            .sessionExpected(false)
                            .consentPageExpected(false)
                            .build()
                    MicrosoftStsPromptHandler(promptHandlerParameters).handlePrompt(mLabAccount.username, mLabAccount.password)
                },
                TokenRequestTimeout.MEDIUM
        )
        authResult.assertSuccess()

        // Using a second account from the same tenant, perform device registration
        mBrokerHostApp.multipleWpjApiFragment.performDeviceRegistration(mLabAccount2.username, mLabAccount2.password)

        // start silent token request in MSAL for the first account, and verify that the device id claim is not present
        // First account uses BrokerLocalController because it doesn't have a PRT, and return AT from cache.
        val authTestParamsForSilentRequest = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLabAccount.username)
                .scopes(listOf(*mScopes))
                .authority(authority)
                .resource(mScopes[0])
                .msalConfigResourceId(configFileResourceId)
                .build()
        val authResult2 = msalSdk.acquireTokenSilent(authTestParamsForSilentRequest, TokenRequestTimeout.MEDIUM)
        authResult2.assertSuccess()
        val claims = JWTParserFactory.INSTANCE.jwtParser.parseJWT(authResult2.accessToken)
        Assert.assertFalse("Device id claim is present", claims.containsKey("deviceid"))

        // start silent token request in MSAL with device id claim for the first account, and verify that an exception is thrown.
        // Note: PkeyAuth is not triggered unless broker_msal version is 9.0 or higher
        val claimsRequest = ClaimsRequest()
        val requestedClaimAdditionalInformation = RequestedClaimAdditionalInformation()
        requestedClaimAdditionalInformation.essential = true
        claimsRequest.requestClaimInIdToken("deviceid", requestedClaimAdditionalInformation)

        val authTestParamsForSilentRequestWithDeviceIdClaim = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLabAccount.username)
                .scopes(listOf(*mScopes))
                .authority(authority)
                .resource(mScopes[0])
                .claims(claimsRequest)
                .msalConfigResourceId(configFileResourceId)
                .build()

        val authResult3 = msalSdk.acquireTokenSilent(
                authTestParamsForSilentRequestWithDeviceIdClaim,
                TokenRequestTimeout.MEDIUM
        )
        authResult3.assertSuccess()
        val claims2 = JWTParserFactory.INSTANCE.jwtParser.parseJWT(authResult3.accessToken)
        Assert.assertTrue("Device id claim is present", claims2.containsKey("deviceid"))
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
        return R.raw.msal_config_pkey_auth_silent
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
        mLabAccount2 = mLabClient.createTempAccount(TempUserType.BASIC)
        Assert.assertEquals(
                "Lab accounts are not in the same tenant",
                mLabAccount2.homeTenantId, mLabAccount.homeTenantId
        )
        Assert.assertNotEquals(
                "Lab accounts are the same",
                mLabAccount2.username, mLabAccount.username
        )
        mBrokerHostApp = broker as BrokerHost
        mBrokerHostApp.enableMultipleWpj()
    }
}
