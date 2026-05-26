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

package com.microsoft.identity.nativeauth

public class NativeAuthPublicClientApplicationParameters (
    /**
     * The application client id. Cannot be null.
     */
    val clientId: String,
    /**
     * The authorityUrl to be used for the authority.
     */
    val authorityUrl: String,
    /**
     * The challenge types supported for authentication declared by client. Cannot be null.
     */
    val challengeTypes: List<String>,
) {

    /**
     * The capabilities supported for authentication declared by client.
     */
    var capabilities: List<String>? = null

    /**
     *  The redirect URI of the application. Required for using browser.
     */
    var redirectUri: String? = null

    /**
     * An optional interceptor for injecting custom HTTP headers into native auth requests.
     * Only headers with names starting with "x-" are permitted (excluding reserved prefixes
     * such as "x-ms-", "x-client-", "x-broker-", "x-app-").
     * Refer to [NativeAuthRequestInterceptor] for more details.
     */
    var requestInterceptor: NativeAuthRequestInterceptor? = null
}
