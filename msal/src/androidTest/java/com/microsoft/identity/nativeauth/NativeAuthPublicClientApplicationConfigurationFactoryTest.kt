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

package com.microsoft.identity.nativeauth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.microsoft.identity.client.AndroidTestUtil
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory
import com.microsoft.identity.msal.test.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class NativeAuthPublicClientApplicationConfigurationFactoryTest {
    private lateinit var context: Context
    private var cachedUseMockApi: Boolean = false

    @Before
    fun setup() {
        System.setProperty(
            "dexmaker.dexcache",
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .cacheDir
                .path
        )

        System.setProperty(
            "org.mockito.android.target",
            ApplicationProvider
                .getApplicationContext<Context>()
                .cacheDir
                .path
        )

        context = InstrumentationRegistry.getInstrumentation().context.applicationContext
        cachedUseMockApi = BuildValues.shouldUseMockApiForNativeAuth()
    }

    @After
    fun tearDown() {
        BuildValues.setUseMockApiForNativeAuth(cachedUseMockApi)
        HttpUrlConnectionFactory.clearMockedConnectionQueue()
        AndroidTestUtil.removeAllTokens(context)
    }

    @Test
    fun testNativeAuthorityConfigMockApiTrue() {
        runBlocking {
            try {
                var config = NativeAuthPublicClientApplicationConfigurationFactory.initializeNativeAuthConfiguration(
                    context, R.raw.test_native_auth_use_mock_api_true)

                Assert.assertEquals(true, config.useMockAuthority)
                Assert.assertEquals(true, BuildValues.shouldUseMockApiForNativeAuth())
            } catch (exception: MsalException) {
                Assert.fail(exception.message)
            }
        }
    }

    @Test
    fun testNativeAuthorityConfigMockApiFalse() {
        runBlocking {
            try {
                var config = NativeAuthPublicClientApplicationConfigurationFactory.initializeNativeAuthConfiguration(
                    context, R.raw.test_native_auth_use_mock_api_false)

                Assert.assertEquals(false, config.useMockAuthority)
                Assert.assertEquals(false, BuildValues.shouldUseMockApiForNativeAuth())
            } catch (exception: MsalException) {
                Assert.fail(exception.message)
            }
        }
    }
}