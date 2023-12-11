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

import com.microsoft.identity.common.nativeauth.java.providers.NativeAuthOAuth2Configuration
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URI
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class NativeAuthOAuth2ConfigurationTest {

    @Test
    fun testNoTrailingSlash() {
        val authorityUrlString = "https://login.microsoftonline.com/samtoso.onmicrosoft.com"
        val authorityUrl = URI(authorityUrlString).toURL()

        val configuration = NativeAuthOAuth2Configuration(
            authorityUrl = authorityUrl,
            clientId = "1234",
            challengeType = "oob password redirect",
            useMockApiForNativeAuth = false
        )

        val signUpEndpoint = configuration.getSignUpStartEndpoint()

        assertEquals(URL("https://login.microsoftonline.com/samtoso.onmicrosoft.com/signup/v1.0/start"), signUpEndpoint)
    }

    @Test
    fun testTrailingSlash() {
        val authorityUrlString = "https://login.microsoftonline.com/samtoso.onmicrosoft.com/"
        val authorityUrl = URI(authorityUrlString).toURL()

        val configuration = NativeAuthOAuth2Configuration(
            authorityUrl = authorityUrl,
            clientId = "1234",
            challengeType = "oob password redirect",
            useMockApiForNativeAuth = false
        )

        val signUpEndpoint = configuration.getSignUpStartEndpoint()

        assertEquals(URL("https://login.microsoftonline.com/samtoso.onmicrosoft.com/signup/v1.0/start"), signUpEndpoint)
    }
}
