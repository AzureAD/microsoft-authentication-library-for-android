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

import com.microsoft.identity.common.java.providers.oauth2.OAuth2RequestInterceptor
import java.net.URL

/**
 * An interceptor that is called before each native auth network request, allowing
 * the application to inject custom HTTP header fields.
 *
 * All custom header field names must start with the "x-" prefix.
 * The prefixes "x-ms-", "x-client-", "x-broker-", and "x-app-" are reserved and must not be used.
 * Headers that violate these rules will be ignored by the SDK.
 */
interface NativeAuthRequestInterceptor : OAuth2RequestInterceptor {

    /**
     * Called before each native auth network request to retrieve additional HTTP header fields.
     *
     * This callback executes synchronously on the thread performing the request (typically a
     * background/network thread), so implementations must be thread-safe and return quickly.
     * Inspect [requestUrl] to determine the request endpoint and conditionally apply headers.
     * Any exception thrown from this method will propagate to the caller and fail the request.
     *
     * @param requestUrl The URL of the outgoing request.
     * @return A map of header field names to values to inject, or null if no additional headers are needed.
     */
    override fun additionalHeaders(requestUrl: URL): Map<String, String>?
}
