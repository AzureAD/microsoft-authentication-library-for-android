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

import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory;

import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Util class for mocking.
 */
public final class AndroidTestMockUtil {
    // private constructor for Util class.
    private AndroidTestMockUtil() {
    }

    static HttpsURLConnection getMockedConnectionWithSuccessResponse(final String message) throws IOException {
        final HttpsURLConnection mockedHttpUrlConnection = getCommonHttpUrlConnection();

        Mockito.when(mockedHttpUrlConnection.getInputStream()).thenReturn(AndroidTestUtil.createInputStream(message));
        Mockito.when(mockedHttpUrlConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);

        return mockedHttpUrlConnection;
    }

    static HttpsURLConnection getMockedConnectionWithFailureResponse(final int statusCode, final String errorMessage)
            throws IOException {
        final HttpsURLConnection mockedHttpUrlConnection = getCommonHttpUrlConnection();

        Mockito.when(mockedHttpUrlConnection.getInputStream()).thenThrow(IOException.class);
        Mockito.when(mockedHttpUrlConnection.getErrorStream()).thenReturn(AndroidTestUtil.createInputStream(errorMessage));
        Mockito.when(mockedHttpUrlConnection.getResponseCode()).thenReturn(statusCode);

        return mockedHttpUrlConnection;
    }

    static HttpsURLConnection getMockedConnectionWithSocketTimeout() throws IOException {
        final HttpsURLConnection mockedUrlConnection = getCommonHttpUrlConnection();

        Mockito.when(mockedUrlConnection.getInputStream()).thenThrow(SocketTimeoutException.class);
        return mockedUrlConnection;
    }

    static HttpsURLConnection getCommonHttpUrlConnection() throws IOException {
        final HttpsURLConnection mockedConnection = Mockito.mock(HttpsURLConnection.class);
        Mockito.doNothing().when(mockedConnection).setConnectTimeout(Mockito.anyInt());
        Mockito.doNothing().when(mockedConnection).setDoInput(Mockito.anyBoolean());
        return mockedConnection;
    }

    static void mockSuccessInstanceDiscovery(final String tenantDiscoveryEndpoint) throws IOException {
        final HttpsURLConnection mockedConnection = getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessInstanceDiscoveryResponse(tenantDiscoveryEndpoint));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }

    static void mockSuccessInstanceDiscoveryAPIVersion1_1() throws IOException {
        final HttpsURLConnection mockedConnection = getMockedConnectionWithSuccessResponse(
                AndroidTestUtil.getSuccessInstanceDiscoveryResponseAPIVersion1_1());
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }

    static void mockFailedGetRequest(int statusCode, final String errorResponse) throws IOException {
        final HttpsURLConnection mockedConnection = getMockedConnectionWithFailureResponse(statusCode, errorResponse);
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }

    static void mockSuccessTenantDiscovery(final String authorizeEndpoint, final String tokenEndpoint) throws IOException {
        final HttpsURLConnection mockedConnection = getMockedConnectionWithSuccessResponse(AndroidTestUtil.getSuccessTenantDiscoveryResponse(authorizeEndpoint, tokenEndpoint));
        HttpUrlConnectionFactory.addMockedConnection(mockedConnection);
    }
}
