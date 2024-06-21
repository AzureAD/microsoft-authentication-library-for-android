// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.tests.network.nativeauth

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.microsoft.identity.client.Logger
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.e2e.tests.IPublicClientApplicationTest
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.internal.controllers.CommandDispatcherHelper
import com.microsoft.identity.internal.testutils.TestUtils
import com.microsoft.identity.internal.testutils.labutils.LabConstants
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

// TODO: move to "PAUSED". A work in RoboTestUtils will be needed though.
@LooperMode(LooperMode.Mode.LEGACY)
@RunWith(RobolectricTestRunner::class)
abstract class NativeAuthPublicClientApplicationAbstractTest : IPublicClientApplicationTest {
    companion object{
        const val SHARED_PREFERENCES_NAME = "com.microsoft.identity.client.account_credential_cache"
    }

    private lateinit var context: Context
    private lateinit var activity: Activity
    lateinit var application: INativeAuthPublicClientApplication

    override fun getConfigFilePath(): String {
        return "" // Not needed for native auth flows
    }

    @Before
    open fun setup() {
        context = ApplicationProvider.getApplicationContext()
        activity = Mockito.mock(Activity::class.java)
        Mockito.`when`(activity.applicationContext).thenReturn(context)
        Logger.getInstance().setEnableLogcatLog(true)
        Logger.getInstance().setEnablePII(true)
        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE)
        CommandDispatcherHelper.clear()
    }

    @After
    open fun cleanup() {
        // remove everything from cache after test ends
        TestUtils.clearCache(SHARED_PREFERENCES_NAME)
    }

    fun getSafePassword(): String {
        val query = LabUserQuery()
        query.federationProvider = LabConstants.FederationProvider.CIAM_CUD
        query.signInAudience = LabConstants.SignInAudience.AZURE_AD_MY_ORG
        val credential = LabUserHelper.getCredentials(query)
        return credential.password
    }

    fun setupPCA(config: NativeAuthTestConfig.Config) {
        val challengeTypes = listOf("password", "oob")

        try {
            application = PublicClientApplication.createNativeAuthPublicClientApplication(
                context,
                config.client_id,
                config.authority_url,
                null,
                challengeTypes
            )
        } catch (e: MsalException) {
            Assert.fail(e.message)
        }
    }
}
