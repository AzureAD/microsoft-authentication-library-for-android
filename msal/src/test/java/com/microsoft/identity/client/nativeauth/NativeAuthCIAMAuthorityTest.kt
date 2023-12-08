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

import com.microsoft.identity.common.java.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Ignore
class NativeAuthCIAMAuthorityTest {

    private val B2C_AUTHORITY_URL = "https://fabrikamb2c.b2clogin.com/tfp/fabrikamb2c.onmicrosoft.com/b2c_1_susi/"
    private val ADFS_AUTHORITY_URL = "https://somesite.contoso.com/adfs/"
    private val AAD_AUTHORITY_URL = "https://login.microsoftonline.com/common"
    private val CIAM_AUTHORITY_URL = "https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com"
    private val CLIENT_ID = "1234-5678-9123"

    @Test
    fun testB2CAuthorityURLShouldThrowError() {
        try {
            NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
                authorityUrl = B2C_AUTHORITY_URL,
                clientId = CLIENT_ID
            )
        } catch (e: ClientException) {
            assertEquals(ClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY, e.errorCode)
        }
    }

    @Test
    fun testADFSAuthorityURLShouldThrowError() {
        try {
            NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
                authorityUrl = ADFS_AUTHORITY_URL,
                clientId = CLIENT_ID
            )
        } catch (e: ClientException) {
            assertEquals(ClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY, e.errorCode)
        }
    }

    @Test
    fun testInvalidAuthorityWithoutPathURLShouldThrowError() {
        try {
            NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
                authorityUrl = "https://www.microsoft.com",
                clientId = CLIENT_ID
            )
        } catch (e: ClientException) {
            assertEquals(ClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY, e.errorCode)
        }
    }

    @Test
    fun testAADAuthorityShouldThrowError() {
        try {
            NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
                authorityUrl = AAD_AUTHORITY_URL,
                clientId = CLIENT_ID
            )
        } catch (e: ClientException) {
            assertEquals(ClientException.NATIVE_AUTH_INVALID_CIAM_AUTHORITY, e.errorCode)
        }
    }

    @Test
    fun testCIAMAuthorityShouldSucceed() {
        val authority = NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
            authorityUrl = CIAM_AUTHORITY_URL,
            clientId = CLIENT_ID
        )
        Assert.assertEquals(
            CIAM_AUTHORITY_URL,
            authority.authorityUri.toString()
        )
        Assert.assertEquals(
            CLIENT_ID,
            authority.clientId
        )
    }

    @Test
    fun testCIAMAuthorityCreateOAuth2StrategyWithDuplicateChallengeTypes() {
        val authority = NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
            authorityUrl = CIAM_AUTHORITY_URL,
            clientId = CLIENT_ID
        )
        val params = OAuth2StrategyParameters.builder()
            .challengeTypes(listOf("oob", "oob", "password"))
            .build()

        val strategy = authority.createOAuth2Strategy(params)
        assertEquals("oob password redirect", strategy.config.challengeType)
    }

    @Test
    fun testCIAMAuthorityCreateOAuth2StrategyWithSingleExistingDefaultChallengeType() {
        val authority = NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
            authorityUrl = CIAM_AUTHORITY_URL,
            clientId = CLIENT_ID
        )
        val params = OAuth2StrategyParameters.builder()
            .challengeTypes(listOf("redirect"))
            .build()

        val strategy = authority.createOAuth2Strategy(params)
        assertEquals("redirect", strategy.config.challengeType)
    }

    @Test
    fun testCIAMAuthorityCreateOAuth2StrategyWithExistingDefaultChallengeTypes() {
        val authority = NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
            authorityUrl = CIAM_AUTHORITY_URL,
            clientId = CLIENT_ID
        )
        val params = OAuth2StrategyParameters.builder()
            .challengeTypes(listOf("redirect", "oob", "password"))
            .build()

        val strategy = authority.createOAuth2Strategy(params)
        assertEquals("redirect oob password", strategy.config.challengeType)
    }

    @Test
    fun testCIAMAuthorityCreateOAuth2StrategyWithDuplicateDefaultChallengeTypes() {
        val authority = NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
            authorityUrl = CIAM_AUTHORITY_URL,
            clientId = CLIENT_ID
        )
        val params = OAuth2StrategyParameters.builder()
            .challengeTypes(listOf("redirect", "oob", "password", "password", "oob", "redirect"))
            .build()

        val strategy = authority.createOAuth2Strategy(params)
        assertEquals("redirect oob password", strategy.config.challengeType)
    }

    @Test
    fun testCIAMAuthorityCreateOAuth2StrategyWithNoChallengeTypes() {
        val authority = NativeAuthCIAMAuthority.getAuthorityFromAuthorityUrl(
            authorityUrl = CIAM_AUTHORITY_URL,
            clientId = CLIENT_ID
        )
        val params = OAuth2StrategyParameters.builder()
            .challengeTypes(emptyList<String>())
            .build()

        val strategy = authority.createOAuth2Strategy(params)
        assertEquals("redirect", strategy.config.challengeType)
    }
}
