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
package com.microsoft.identity.client.internal.controllers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.client.PublicClientApplicationConfiguration
import com.microsoft.identity.client.PublicClientApplicationConfigurationFactory
import com.microsoft.identity.client.e2e.shadows.ShadowBrokerDiscoveryClient
import com.microsoft.identity.client.e2e.shadows.ShadowLegacyBrokerDiscoveryClient
import com.microsoft.identity.common.internal.controllers.BrokerMsalController
import com.microsoft.identity.common.java.authorities.Authority
import com.microsoft.identity.msal.test.R
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowLegacyBrokerDiscoveryClient::class,
    ShadowBrokerDiscoveryClient::class] )
class MSALControllerFactoryTest {

    private lateinit var pcaConfiguration: PublicClientApplicationConfiguration

    @Before
    fun setup() {
        val context : Context = ApplicationProvider.getApplicationContext()
        pcaConfiguration = PublicClientApplicationConfigurationFactory.loadConfiguration(context, R.raw.msal_default_config)
        pcaConfiguration.appContext = context
        Assert.assertTrue(pcaConfiguration.useBroker)
    }
    @Test
    fun testGetControllers() {
        val testAuthority = Authority.getAuthorityFromAuthorityUrl("https://login.microsoftonline.com/common")
        val msalControllerFactory = MSALControllerFactory(pcaConfiguration, testAuthority)
        Assert.assertTrue(msalControllerFactory.brokerEligibleAndInstalled())
        Assert.assertEquals(2, msalControllerFactory.getAllControllers().size)
        Assert.assertTrue(msalControllerFactory.getAllControllers()[0] is BrokerMsalController )
    }

    @Test
    fun testGetDefaultController() {
        val testAuthority = Authority.getAuthorityFromAuthorityUrl("https://login.microsoftonline.com/common")
        val msalControllerFactory = MSALControllerFactory(pcaConfiguration, testAuthority)
        Assert.assertTrue(msalControllerFactory.brokerEligibleAndInstalled())
        Assert.assertTrue(msalControllerFactory.getDefaultController() is BrokerMsalController )
    }
}