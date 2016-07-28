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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Internal class for create {@link java.net.HttpURLConnection}.
 * For testability, test case could set the mocked {@link java.net.HttpURLConnection} to inject dependency.
 */
final class HttpUrlConnectionFactory {
    private static HttpURLConnection sMockedConnection = null;

    /**
     * Private constructor to prevent the class from being initiated.
     */
    private HttpUrlConnectionFactory() { }

    /**
     * Used by tests to set mocked connection.
     * @param mockedConnection
     */
    static void setMockedConnection(final HttpURLConnection mockedConnection) {
        sMockedConnection = mockedConnection;
    }

    /**
     * Creates the {@link HttpURLConnection} with the given url.
     * @param url The request URL used to create the connection.
     * @return {@link HttpURLConnection} with the provided URL.
     * @throws IOException if it fails to open connection with the provided URL.
     */
    static HttpURLConnection createHttpURLConnection(final URL url) throws IOException {
        if (sMockedConnection != null) {
            return sMockedConnection;
        }

        return (HttpURLConnection) url.openConnection();
    }
}