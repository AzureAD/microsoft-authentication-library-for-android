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

/**
 * HTTP Request Methods as defined in <a href="https://tools.ietf.org/html/rfc7231#section-4.3">RFC-7231/§4.3</a>
 * and <a href="https://tools.ietf.org/html/rfc5789">RFC-5789</a>.
 */
public enum HttpMethod {

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.1">RFC-7231/§4.1</a>
     */
    GET,

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.2">RFC-7231/§4.2</a>
     */
    HEAD,

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.3">RFC-7231/§4.3</a>
     */
    POST,

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.4">RFC-7231/§4.4</a>
     */
    PUT,

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.5">RFC-7231/§4.5</a>
     */
    DELETE,

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.6">RFC-7231/§4.6</a>
     */
    CONNECT,

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.7">RFC-7231/§4.7</a>
     */
    OPTIONS,

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.8">RFC-7231/§4.8</a>
     */
    TRACE,

    /**
     * @see <a href="https://tools.ietf.org/html/rfc5789">RFC-5789</a>
     */
    PATCH
}
