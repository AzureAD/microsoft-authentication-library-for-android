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

import com.microsoft.identity.nativeauth.NativeAuthRequestInterceptor
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

enum class NativeAuthEndpointCategory {
    AUTHORIZE_CHALLENGE,
    SIGN_UP,
    CHALLENGE,
    VERIFY,
    RESEND,
    TOKEN,
    OTHER
}

class NativeAuthRequestRecorder : NativeAuthRequestInterceptor {
    private val calls = CopyOnWriteArrayList<NativeAuthEndpointCategory>()

    override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
        calls += categorize(requestUrl.path.orEmpty())
        return null
    }

    fun snapshot(): List<NativeAuthEndpointCategory> = calls.toList()

    fun callsAfter(snapshotSize: Int): List<NativeAuthEndpointCategory> =
        calls.drop(snapshotSize)

    override fun toString(): String = calls.joinToString(prefix = "[", postfix = "]")

    private fun categorize(path: String): NativeAuthEndpointCategory {
        val normalized = path.lowercase()
        return when {
            normalized.endsWith("/oauth2/v2.0/token") ->
                NativeAuthEndpointCategory.TOKEN
            normalized.contains("authorize") && normalized.contains("challenge") ->
                NativeAuthEndpointCategory.AUTHORIZE_CHALLENGE
            normalized.contains("sign-up") || normalized.contains("signup") ->
                NativeAuthEndpointCategory.SIGN_UP
            normalized.contains("resend") ->
                NativeAuthEndpointCategory.RESEND
            normalized.contains("verify") ->
                NativeAuthEndpointCategory.VERIFY
            normalized.contains("challenge") ->
                NativeAuthEndpointCategory.CHALLENGE
            else ->
                NativeAuthEndpointCategory.OTHER
        }
    }
}
