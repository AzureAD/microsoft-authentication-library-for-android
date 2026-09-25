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
// furnished to do so, subject to the following conditions:
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
package com.microsoft.identity.client.e2e.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class NativeAuthRequestRecorderTest {

    @Test
    fun recordsOnlySanitizedEndpointCategories() {
        val recorder = NativeAuthRequestRecorder()

        recorder.additionalHeaders(URL("https://tenant.ciamlogin.com/tenant/oauth2/v2.0/authorize/challenge?secret=value"))
        recorder.additionalHeaders(URL("https://tenant.ciamlogin.com/tenant/nativeauth/v2/sign-up/start"))
        recorder.additionalHeaders(URL("https://tenant.ciamlogin.com/tenant/nativeauth/v2/verify"))
        recorder.additionalHeaders(URL("https://tenant.ciamlogin.com/tenant/oauth2/v2.0/token"))

        assertEquals(
            listOf(
                NativeAuthEndpointCategory.AUTHORIZE_CHALLENGE,
                NativeAuthEndpointCategory.SIGN_UP,
                NativeAuthEndpointCategory.VERIFY,
                NativeAuthEndpointCategory.TOKEN
            ),
            recorder.snapshot()
        )
        assertTrue(recorder.toString().contains("secret").not())
        assertTrue(recorder.toString().contains("value").not())
    }

    @Test
    fun exposesOnlyObservableSanitizedEndpointCategories() {
        assertEquals(
            listOf(
                "AUTHORIZE_CHALLENGE",
                "SIGN_UP",
                "CHALLENGE",
                "VERIFY",
                "RESEND",
                "TOKEN",
                "OTHER"
            ),
            NativeAuthEndpointCategory.values().map { it.name }
        )
    }

    @Test
    fun callsAfterReturnsAuthorizeChallengeThenTokenDelta() {
        val recorder = NativeAuthRequestRecorder()
        recorder.additionalHeaders(URL("https://tenant/nativeauth/v2/sign-up/start"))
        val snapshotSize = recorder.snapshot().size

        recorder.additionalHeaders(URL("https://tenant/oauth2/v2.0/authorize/challenge"))
        recorder.additionalHeaders(URL("https://tenant/oauth2/v2.0/token"))

        assertEquals(
            listOf(
                NativeAuthEndpointCategory.AUTHORIZE_CHALLENGE,
                NativeAuthEndpointCategory.TOKEN
            ),
            recorder.callsAfter(snapshotSize)
        )
    }

    @Test
    fun callsAfterReturnsSingleVerifyDeltaForSubmitCode() {
        val recorder = NativeAuthRequestRecorder()
        recorder.additionalHeaders(URL("https://tenant/nativeauth/v2/sign-up/challenge"))
        val snapshotSize = recorder.snapshot().size

        recorder.additionalHeaders(URL("https://tenant/nativeauth/v2/verify"))

        val submitCalls = recorder.callsAfter(snapshotSize)
        assertEquals(listOf(NativeAuthEndpointCategory.VERIFY), submitCalls)
        assertEquals(0, submitCalls.count { it == NativeAuthEndpointCategory.TOKEN })
    }
}
