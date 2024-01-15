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
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Arrays

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class NativeAuthPublicClientAppKotlinTest {
    private lateinit var context: Context
    private val CLIENT_ID = "1234"
    // The general format is https://<tenant>.ciamlogin.com/<tenant>.onmicrosoft.com
    // See details here: https://microsoft.sharepoint-df.com/:w:/t/AADIEFtogether/ES9p_7m-qUtEuINcd0UlyekBh6TrWtrMJr12WS_O7BAgww?e=HQybBw
    private val CIAM_AUTHORITY = "https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com"
    private val B2C_AUTHORITY = "https://msidlabb2c.b2clogin.com/tfp/msidlabb2c.onmicrosoft.com/b2c_1_ropc_auth/"
    private val INVALID_AUTHORITY = "https://b2clogin.com"
    private val EMPTY_STRING = ""
    // TODO: Replace the link with https://learn.microsoft.com/ when the document about OAuth 2.0 Direct Interaction Grants is published.
    // The definitions and scopes in OAuth 2.0 Direct Interaction Grants(3.4Challenge Types):
    // https://aaronpk.github.io/oauth-direct-interaction-grant/draft-parecki-oauth-direct-interaction-grant.html#name-authorization-grants
    private val challengeTypes = Arrays.asList("oob", "password")

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
    }

    @After
    fun tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue()
        AndroidTestUtil.removeAllTokens(context)
    }

    @Test
    fun testWorkingInit() {
        runBlocking {
            try {
                val app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    CLIENT_ID,
                    CIAM_AUTHORITY,
                    null,
                    challengeTypes
                )
                Assert.assertNotNull("NAPCA can't be null", app)
            } catch (exception: MsalException) {
                Assert.fail(exception.message)
            }
        }
    }

    @Test
    fun testEmptyClientIdReturnsOnError() {
        runBlocking {
            try {
                val app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    EMPTY_STRING, // Invalid parameters: empty client id should throw exception
                    CIAM_AUTHORITY,
                    null,
                    challengeTypes
                )
                Assert.fail("NAPCA creation did not throw exception")
            } catch (exception: IllegalArgumentException) {
                return@runBlocking
            }
            Assert.fail("Empty client Id string should return error")
        }
    }

    @Test
    fun testEmptyAuthorityReturnsOnError() {
        runBlocking {
            try {
                val app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    CLIENT_ID,
                    EMPTY_STRING, // Invalid parameters: empty authority should throw exception
                    null,
                    challengeTypes
                )
                Assert.fail("NAPCA creation did not throw exception")
            } catch (exception: IllegalArgumentException) {
                return@runBlocking
            }
            Assert.fail("Empty client Id string should return error")
        }
    }

    @Test
    fun testB2CAuthorityReturnsOnError() {
        runBlocking {
            try {
                val app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    CLIENT_ID,
                    B2C_AUTHORITY,
                    null,
                    challengeTypes
                )
                Assert.fail("NAPCA creation did not throw exception")
            } catch (exception: MsalException) {
                // warp NATIVE_AUTH_INVALID_CIAM_AUTHORITY_ERROR into UNKNOWN_ERROR in catch block
                if (exception.errorCode == MsalClientException.UNKNOWN_ERROR)
                    return@runBlocking
            }
            Assert.fail("B2C authority should return error")
        }
    }

    @Test
    fun testInvalidAuthorityReturnsOnError() {
        runBlocking {
            try {
                val app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    CLIENT_ID,
                    INVALID_AUTHORITY,
                    null,
                    challengeTypes
                )
                Assert.fail("NAPCA creation did not throw exception")
            } catch (exception: MsalException) {
                // warp NATIVE_AUTH_INVALID_CIAM_AUTHORITY_ERROR into UNKNOWN_ERROR in catch block
                if (exception.errorCode == MsalClientException.UNKNOWN_ERROR)
                    return@runBlocking
            }
            Assert.fail("Invalid authority string should return error")
        }
    }
}
