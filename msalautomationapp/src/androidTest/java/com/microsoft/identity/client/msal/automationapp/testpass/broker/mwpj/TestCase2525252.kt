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
import com.microsoft.identity.client.ui.automation.ICustomBrokerInstallationTest
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers
import com.microsoft.identity.client.ui.automation.broker.BrokerHost
import com.microsoft.identity.client.ui.automation.broker.ITestBroker
import com.microsoft.identity.labapi.utilities.client.ILabAccount
import com.microsoft.identity.labapi.utilities.client.LabQuery
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment
import com.microsoft.identity.labapi.utilities.constants.TempUserType
import com.microsoft.identity.labapi.utilities.constants.UserType
import com.microsoft.identity.labapi.utilities.jwt.JWTParserFactory
import org.junit.Assert
import org.junit.Before
import org.junit.Test

// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2525252
// TODO
@SupportedBrokers(brokers = [BrokerHost::class])
@LocalBrokerHostDebugUiTest
class TestCase2525252 : AbstractMsalBrokerTest() , ICustomBrokerInstallationTest {
    private lateinit var mUsGovAccount: ILabAccount
    private lateinit var mBrokerHostApp: BrokerHost

    //@Test
    fun test_2525252() {
        // Register tenant using legacy API
        mBrokerHostApp.performDeviceRegistrationLegacyApp(mLabAccount.username, mLabAccount.password)

        //Upgrade broker
        updateBrokerHostApp()
        mBrokerHostApp.enableMultipleWpj()

        // Test other APIs
        //Get all records
        val deviceRegistrationRecords = mBrokerHostApp.multipleWpjApiFragment.allRecords
        Assert.assertEquals(1, deviceRegistrationRecords.size)
        val record = deviceRegistrationRecords[0]
        Assert.assertEquals(mLabAccount.homeTenantId, record["TenantId"])
        Assert.assertEquals(mLabAccount.username, record["Upn"])

        //Get record by tenantId
        val recordByTenantId = mBrokerHostApp.multipleWpjApiFragment.getRecordByTenantId(mLabAccount.homeTenantId)
        Assert.assertEquals(record, recordByTenantId)

        //Get record by upn
        val recordByUpn = mBrokerHostApp.multipleWpjApiFragment.getRecordByUpn(mLabAccount.username)
        Assert.assertEquals(record, recordByUpn)

        //Get device token
        val deviceToken = mBrokerHostApp.multipleWpjApiFragment.getDeviceToken(mLabAccount.username)
        val claims = JWTParserFactory.INSTANCE.jwtParser.parseJWT(deviceToken)
        Assert.assertTrue(claims.containsKey("deviceid"))
        Assert.assertEquals(record["DeviceId"], claims["deviceid"])

        //Install certificate
        mBrokerHostApp.multipleWpjApiFragment.installCertificate(mLabAccount.username)

        //Get device state
        val deviceState = mBrokerHostApp.multipleWpjApiFragment.getDeviceState(mLabAccount.username)
        Assert.assertTrue(deviceState.contains("DEVICE_VALID"))

        // Unregister device
        mBrokerHostApp.multipleWpjApiFragment.unregister(mLabAccount.username)
        Assert.assertEquals(0, mBrokerHostApp.multipleWpjApiFragment.allRecords.size)

        //Get Blob
        val blob = mBrokerHostApp.multipleWpjApiFragment.getBlob(mLabAccount.homeTenantId)
        val claims2 = JWTParserFactory.INSTANCE.jwtParser.parseJWT(blob)
        Assert.assertTrue(claims2.containsKey("nonce"))
    }

    override fun getLabQuery(): LabQuery {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .build()
    }

    override fun getTempUserType(): TempUserType? {
        return null
    }

    private fun getUsGovLabQuery(): LabQuery {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .azureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT)
                .build()
    }

    @Before
    fun before() {
        mUsGovAccount = mLabClient.getLabAccount(getUsGovLabQuery())
        mBrokerHostApp = broker as BrokerHost
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

    override fun getBroker(): ITestBroker {
        if (mBroker == null) {
            mBroker = installOldBrokerHost()
        }
        return mBroker
    }

    private fun updateBrokerHostApp() {
        val brokerHost = BrokerHost()
        brokerHost.install()
        mBroker = brokerHost
        mBrokerHostApp = mBroker as BrokerHost
    }

}
