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

import com.microsoft.identity.client.msal.automationapp.R
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerHost
import com.microsoft.identity.client.ui.automation.rules.LoadLabUserTestRule
import com.microsoft.identity.labapi.utilities.client.ILabAccount
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

// Verify WPJ Cert installation on a Non samsung device with Authenticator
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/831655
// Technically this works on Samsung device too (at least Galaxy S6)
// So this should also cover TestCase831570
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/831570
@SupportedBrokers(brokers = [BrokerHost::class])
class TestCase2519783 : AbstractMsalBrokerTest() {

    private lateinit var mUsGovLabAccount: ILabAccount

    @get:Rule
    val loadAdditionalLabUserRule: TestRule = LoadLabUserTestRule(getAdditionalLabQuery())

    @Test
    fun test_2519783() {

        val username = mLabAccount.username
        val password = mLabAccount.password
        val usGovUsername = mUsGovLabAccount.username
        val usGovPassword = mUsGovLabAccount.password
        (broker as BrokerHost).performDeviceRegistrationMultiple(username, password)
        (broker as BrokerHost).performDeviceRegistrationMultiple(usGovUsername, usGovPassword)
        val deviceRegistrationRecords = (broker as BrokerHost).allRecords
        (broker as BrokerHost).installCertificateMultiple(deviceRegistrationRecords[0]["TenantId"] as String)
        (broker as BrokerHost).installCertificateMultiple(deviceRegistrationRecords[1]["TenantId"] as String)
    }

    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    private fun getAdditionalLabQuery(): LabQuery {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .azureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT)
                .build()
    }

    @Before
    fun before() {
        mUsGovLabAccount = (loadAdditionalLabUserRule as LoadLabUserTestRule).labAccount
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
}