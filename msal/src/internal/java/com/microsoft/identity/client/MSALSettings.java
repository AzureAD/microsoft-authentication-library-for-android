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
 * External Settings API for MSAL.
 */
public enum MsalSettings {
    /**
     * Singleton setting instance.
     */
    INSTANCE;

    /**
     * Default value of read timeout in milliseconds.
     */
    public static final int DEFAULT_READ_TIMEOUT = 30000;
    /**
     * Default value of connect timeout in milliseconds.
     */
    public static final int DEFAULT_CONNECT_TIMEOUT = 30000;

    private int mConnectTimeOut = DEFAULT_READ_TIMEOUT;
    private int mReadTimeOut = DEFAULT_CONNECT_TIMEOUT;

    /**
     * Get the connect timeout.
     *
     * @return connect timeout in milliseconds.
     */
    public int getConnectTimeOut() {
        return mConnectTimeOut;
    }

    /**
     * Sets the maximum time in milliseconds to wait while connecting.
     * Connecting to a server will fail with a SocketTimeoutException if the
     * timeout elapses before a connection is established. Default value is
     * {@value DEFAULT_CONNECT_TIMEOUT} milliseconds.
     *
     * @param timeOutMillis the non-negative connect timeout in milliseconds.
     */
    public void setConnectTimeOut(int timeOutMillis) {
        if (timeOutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeOutMillis");
        }
        this.mConnectTimeOut = timeOutMillis;
    }

    /**
     * Get the read timeout.
     *
     * @return read timeout in milliseconds.
     */
    public int getReadTimeOut() {
        return mReadTimeOut;
    }

    /**
     * Sets the maximum time to wait for an input stream read to complete before
     * giving up. Reading will fail with a SocketTimeoutException if the timeout
     * elapses before data becomes available. The default value is {@value DEFAULT_READ_TIMEOUT}
     * milliseconds.
     *
     * @param timeOutMillis the non-negative read timeout in milliseconds.
     */
    public void setReadTimeOut(int timeOutMillis) {
        if (timeOutMillis < 0) {
            throw new IllegalArgumentException("Invalid timeOutMillis");
        }

        this.mReadTimeOut = timeOutMillis;
    }
}
