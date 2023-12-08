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
package com.microsoft.identity.client.nativeauth

import com.microsoft.identity.client.NativeAuthPublicClientApplicationConfiguration
import com.microsoft.identity.client.PublicClientApplicationConfiguration.INVALID_REDIRECT_MSG
import com.microsoft.identity.client.configuration.AccountMode
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryB2CAuthority
import com.microsoft.identity.common.java.authorities.NativeAuthCIAMAuthority
import junit.framework.Assert.assertEquals
import junit.framework.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Ignore
class NativeAuthPublicClientApplicationConfigurationTest {

    private val clientId = "1234"
    private val ciamAuthority = "https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com"

    @Test
    fun testMissingClientId() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.setClientId(null)
        try {
            config.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITHOUT_CLIENT_ID_ERROR_CODE, e.errorCode)
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITHOUT_CLIENT_ID_ERROR_MESSAGE, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testEmptyClientId() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.setClientId("")
        try {
            config.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITHOUT_CLIENT_ID_ERROR_CODE, e.errorCode)
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITHOUT_CLIENT_ID_ERROR_MESSAGE, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testEmptyRedirectUri() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.redirectUri = ""
        try {
            config.validateConfiguration()
        } catch (e: IllegalArgumentException) {
            assertEquals(INVALID_REDIRECT_MSG, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testInvalidAccountMode() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.MULTIPLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId), NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        try {
            spyConfig.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_INVALID_ACCOUNT_MODE_CONFIG_ERROR_CODE, e.errorCode)
            assertEquals(MsalClientException.NATIVE_AUTH_INVALID_ACCOUNT_MODE_CONFIG_ERROR_MESSAGE, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testNoAuthority() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(null)
        try {
            spyConfig.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITH_NO_AUTHORITY_ERROR_CODE, e.errorCode)
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITH_NO_AUTHORITY_ERROR_MESSAGE, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testEmptyAuthorityList() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(emptyList())
        try {
            spyConfig.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITH_NO_AUTHORITY_ERROR_CODE, e.errorCode)
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITH_NO_AUTHORITY_ERROR_MESSAGE, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testTooManyAuthorities() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId), NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        try {
            spyConfig.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITH_MULTI_AUTHORITY_ERROR_CODE, e.errorCode)
            assertEquals(MsalClientException.NATIVE_AUTH_USE_WITH_MULTI_AUTHORITY_ERROR_MESSAGE, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testInvalidAuthority() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        whenever(spyConfig.defaultAuthority).thenReturn(AzureActiveDirectoryB2CAuthority(ciamAuthority))
        try {
            spyConfig.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY_ERROR_CODE, e.errorCode)
            assertEquals(MsalClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY_ERROR_MESSAGE, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testSharedDeviceMode() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        whenever(spyConfig.defaultAuthority).thenReturn(NativeAuthCIAMAuthority(ciamAuthority, clientId))
        whenever(spyConfig.isSharedDevice).thenReturn(true)

        try {
            spyConfig.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_SHARED_DEVICE_MODE_ERROR_CODE, e.errorCode)
            assertEquals(MsalClientException.NATIVE_AUTH_SHARED_DEVICE_MODE_ERROR_MESSAGE, e.message)
            return
        }
        // An exception should be thrown
        fail()
    }

    // Challenge types are optional, so no exception should be thrown
    @Test
    fun testMissingChallengeTypes() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        whenever(spyConfig.defaultAuthority).thenReturn(NativeAuthCIAMAuthority(ciamAuthority, clientId))
        whenever(spyConfig.isSharedDevice).thenReturn(false)
        whenever(spyConfig.useBroker).thenReturn(false)
        whenever(spyConfig.getChallengeTypes()).thenReturn(emptyList())
        spyConfig.validateConfiguration()
    }

    @Test
    fun testInvalidChallengeTypes() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        whenever(spyConfig.defaultAuthority).thenReturn(NativeAuthCIAMAuthority(ciamAuthority, clientId))
        whenever(spyConfig.isSharedDevice).thenReturn(false)
        whenever(spyConfig.useBroker).thenReturn(false)
        spyConfig.setChallengeTypes(listOf("lorem"))

        try {
            spyConfig.validateConfiguration()
        } catch (e: MsalClientException) {
            assertEquals(MsalClientException.NATIVE_AUTH_INVALID_CHALLENGE_TYPE_ERROR_CODE, e.errorCode)
            return
        }
        // An exception should be thrown
        fail()
    }

    @Test
    fun testCaseInsensitiveChallengeTypes() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        whenever(spyConfig.defaultAuthority).thenReturn(NativeAuthCIAMAuthority(ciamAuthority, clientId))
        whenever(spyConfig.isSharedDevice).thenReturn(false)
        whenever(spyConfig.useBroker).thenReturn(false)
        spyConfig.setChallengeTypes(listOf("PaSsWoRd", "OoB", "ReDiReCt"))
        spyConfig.validateConfiguration()
    }

    @Test
    fun testMultipleCorrectChallengeTypes() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        whenever(spyConfig.defaultAuthority).thenReturn(NativeAuthCIAMAuthority(ciamAuthority, clientId))
        whenever(spyConfig.isSharedDevice).thenReturn(false)
        whenever(spyConfig.useBroker).thenReturn(false)
        spyConfig.setChallengeTypes(listOf("password", "oob", "redirect"))
        spyConfig.validateConfiguration()
    }

    @Test
    fun testSingleCorrectChallengeTypes() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        whenever(spyConfig.defaultAuthority).thenReturn(NativeAuthCIAMAuthority(ciamAuthority, clientId))
        whenever(spyConfig.isSharedDevice).thenReturn(false)
        whenever(spyConfig.useBroker).thenReturn(false)
        spyConfig.setChallengeTypes(listOf("oob"))
        spyConfig.validateConfiguration()
    }

    @Test
    fun testRepeatedCorrectChallengeTypes() {
        val config = NativeAuthPublicClientApplicationConfiguration()
        config.clientId = clientId
        config.accountMode = AccountMode.SINGLE
        val spyConfig = spy(config)
        whenever(spyConfig.authorities).thenReturn(listOf(NativeAuthCIAMAuthority(ciamAuthority, clientId)))
        whenever(spyConfig.defaultAuthority).thenReturn(NativeAuthCIAMAuthority(ciamAuthority, clientId))
        whenever(spyConfig.isSharedDevice).thenReturn(false)
        whenever(spyConfig.useBroker).thenReturn(false)
        spyConfig.setChallengeTypes(listOf("oob", "oob", "password", "redirect", "redirect", "redirect"))
        spyConfig.validateConfiguration()
    }
}
