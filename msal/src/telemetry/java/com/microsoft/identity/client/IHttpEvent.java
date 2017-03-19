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

package com.microsoft.identity.client;

import java.net.URL;

/**
 * Internal interface for HttpEvent telemetry data.
 */
interface IHttpEvent extends IEvent {

    /**
     * Gets the user agent.
     *
     * @return the user agent to get.
     */
    String getUserAgent();

    /**
     * Gets the http method.
     *
     * @return the http method to get.
     */
    String getHttpMethod();

    /**
     * Gets the query parameters.
     *
     * @return the query parameters to get.
     */
    String getQueryParameters();

    /**
     * Gets the api version.
     *
     * @return the api version to get.
     */
    String getApiVersion();

    /**
     * Gets the oauth error code.
     *
     * @return the oauth error code to get.
     */
    String getOAuthErrorCode();

    /**
     * Gets the RequestId header.
     *
     * @return the RequestId header to get
     */
    String getRequestIdHeader();

    /**
     * Gets the http path of this event.
     *
     * @return the http path to get.
     */
    URL getHttpPath();

    /**
     * Gets the http status line (code) of the call matching this request.
     *
     * @return the status line (code) to get.
     */
    Integer getResponseCode();
}
