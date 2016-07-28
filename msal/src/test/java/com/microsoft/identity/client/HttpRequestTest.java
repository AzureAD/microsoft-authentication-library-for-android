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

package com.microsoft.identity.client;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Collections;

/**
 * Tests for {@link HttpRequest}.
 */
public final class HttpRequestTest {
    static final String REQUEST_METHOD_GET = "GET";
    static final String REQUEST_METHOD_POST = "POST";

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.setMockedConnection(null);
    }

    /**
     * Verify the expected exception is thrown when sending get request with null url.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNullRequestUrl() throws IOException, MSALAuthenticationException {
        HttpRequest.sendGet(null, Collections.<String, String>emptyMap());
    }

    /**
     * Verify the expected exception is thrown when request url is not using http or https protocol.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testNonHttpRequestUrl() throws IOException, MSALAuthenticationException {
        HttpRequest.sendGet(new URL("file://a.com"), Collections.<String, String>emptyMap());
    }

    /**
     * Verify that http get succeed and no retry happens.
     */
    @Test
    public void testHttpGetSucceed() throws IOException {
        new HttpRequestPositiveTestCase() {

            @Override
            void mockHttpUrlConnectionGetStream(HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream())
                    .thenReturn(Util.createInputStream(getSuccessResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            }

            @Override
            int getNetworkCallTimes() {
                return 1;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_GET;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(HttpURLConnection mockedConnection) throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(1)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(0)).getErrorStream();
            }
        }.performTest();
    }

    /**
     * Verify that http post succeeds and no retry happens.
     */
    @Test
    public void testHttpPostSucceed() throws IOException {
        new HttpRequestPositiveTestCase() {

            @Override
            void mockHttpUrlConnectionGetStream(HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream())
                        .thenReturn(Util.createInputStream(getSuccessResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            }

            @Override
            int getNetworkCallTimes() {
                return 1;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(HttpURLConnection mockedConnection) throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(1)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(0)).getErrorStream();
            }
        }.performTest();
    }

    /**
     * Verify the correct response is returned if first network call fails with internal error, and retry succeeds.
     */
    @Test
    public void testHttpPostFailedWith500RetrySucceed() throws IOException {
        new HttpRequestPositiveTestCase() {

            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection) throws IOException {
                assert mockedConnection != null;

                Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class).thenReturn(
                        Util.createInputStream(getSuccessResponse()));
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR,
                        HttpURLConnection.HTTP_OK);
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(HttpURLConnection mockedConnection) throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
            }
        }.performTest();
    }

    /**
     * Verify that the initial post request failed with {@link HttpURLConnection#HTTP_UNAVAILABLE} and retry succeeds.
     */
    @Test
    public void testHttpPostFailedWith503RetrySucceed() throws IOException {
        new HttpRequestPositiveTestCase() {

            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class).thenReturn(
                        Util.createInputStream(getSuccessResponse()));
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAVAILABLE,
                        HttpURLConnection.HTTP_OK);
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
            }
        }.performTest();
    }

    /**
     * Verify that the initial post request failed with {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT} and retry
     * succeeds.
     */
    @Test
    public void testHttpPostFailedWith504RetrySucceed() throws IOException {
        new HttpRequestPositiveTestCase() {

            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class).thenReturn(
                        Util.createInputStream(getSuccessResponse()));
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT,
                        HttpURLConnection.HTTP_OK);
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
            }
        }.performTest();
    }

    /**
     * Verify that the initial post request failed with {@link SocketTimeoutException} and retry
     * succeeds.
     */
    @Test
    public void testHttpPostFailedWithSocketTimeoutRetrySucceed() throws IOException {
        new HttpRequestPositiveTestCase() {

            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(SocketTimeoutException.class).thenReturn(
                        Util.createInputStream(getSuccessResponse()));
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(0)).getErrorStream();
            }
        }.performTest();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_BAD_REQUEST}, no retry happens.
     */
    @Test
    public void testHttpGetFailedNoRetry() throws IOException {
        new HttpRequestRetryFailedWithNonRetryableErrorCode() {

            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
            }

            @Override
            int getNetworkCallTimes() {
                return 1;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_GET;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(1)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNotNull(httpResponse);
                Assert.assertTrue(httpResponse.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);
                Assert.assertTrue(httpResponse.getResponseBody().equals(getErrorResponse()));
            }
        }.performTest();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED}, no retry happens.
     */
    @Test
    public void testHttpPostFailedNoRetry() throws IOException {
        new HttpRequestRetryFailedWithNonRetryableErrorCode() {

            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);
            }

            @Override
            int getNetworkCallTimes() {
                return 1;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(1)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNotNull(httpResponse);
                Assert.assertTrue(httpResponse.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED);
                Assert.assertTrue(httpResponse.getResponseBody().equals(getErrorResponse()));
            }
        }.performTest();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_BAD_METHOD} and no response body,
     * no retry happens.
     */
    @Test
    public void testHttpGetFailedNoRetryNoResponseBody() throws IOException {
        new HttpRequestRetryFailedWithNonRetryableErrorCode() {
            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(null);
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_METHOD);
            }

            @Override
            int getNetworkCallTimes() {
                return 1;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(1)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNotNull(httpResponse);
                Assert.assertTrue(httpResponse.getStatusCode() == HttpURLConnection.HTTP_BAD_METHOD);
                Assert.assertTrue(httpResponse.getResponseBody().isEmpty());
            }
        }.performTest();
    }

    /**
     * Verify that initial http post fails with {@link HttpURLConnection#HTTP_INTERNAL_ERROR}(retryable status code
     * 500/503/504), retry fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED}(non retryable status code).
     */
    @Test
    public void testHttpPostFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws IOException {
        new HttpRequestRetryFailedWithNonRetryableErrorCode() {
            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(IOException.class, IOException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(
                        Util.createInputStream(getErrorResponse()), Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(
                        HttpURLConnection.HTTP_INTERNAL_ERROR, HttpURLConnection.HTTP_UNAUTHORIZED);
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(2)).getErrorStream();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNotNull(httpResponse);
                Assert.assertTrue(httpResponse.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED);
                Assert.assertTrue(httpResponse.getResponseBody().equals(getErrorResponse()));
            }
        }.performTest();
    }

    /**
     * Verify that initial http post fails with {@link SocketTimeoutException}, retry fails with
     * {@link HttpURLConnection#HTTP_BAD_REQUEST}(non retryable status code).
     */
    @Test
    public void testHttpPostFailedWithSocketTimeoutRetryFailedWithNonRetryableCode() throws IOException {
        new HttpRequestRetryFailedWithNonRetryableErrorCode() {
            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(
                        SocketTimeoutException.class, IOException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(
                        Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_BAD_REQUEST);
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getResponseCode();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNotNull(httpResponse);
                Assert.assertTrue(httpResponse.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);
                Assert.assertTrue(httpResponse.getResponseBody().equals(getErrorResponse()));
            }
        }.performTest();
    }

    /**
     * Verify that initial http post fails with {@link HttpURLConnection#HTTP_INTERNAL_ERROR}(retryable status code
     * 500/503/504), retry fails with {@link HttpURLConnection#HTTP_UNAVAILABLE}(retryable status code).
     */
    @Test
    public void testPostFailedWithRetryableErrorRetryFailedWithRetryableError() throws IOException {
        new BaseHttpRequestTestCase() {
            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(
                        IOException.class, IOException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()),
                        Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(
                        HttpURLConnection.HTTP_INTERNAL_ERROR, HttpURLConnection.HTTP_UNAVAILABLE);
            }

            @Override
            HttpResponse makeHttpRequestCall() throws IOException {
                try {
                    sendHttpPost();
                    Assert.fail("Expect MSALAuthenticationException to be thrown.");
                } catch (final MSALAuthenticationException e) {
                    Assert.assertNotNull(e);
                    Assert.assertTrue(e.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_SERVER_ERROR));
                    Assert.assertTrue(e.getMessage().contains(
                            "StatusCode: " + String.valueOf(HttpURLConnection.HTTP_UNAVAILABLE)));
                }

                return null;
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(2)).getErrorStream();
                Mockito.verify(mockedConnection, Mockito.times(2)).getResponseCode();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNull(httpResponse);
            }
        }.performTest();
    }

    /**
     * Verify that initial http post fails with {@link SocketTimeoutException}, retry fails with
     * {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT}(retryable status code).
     */
    @Test
    public void testPostFailedWithSocketTimeoutRetryFailedWithRetryableError() throws IOException {
        new BaseHttpRequestTestCase() {
            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(
                        SocketTimeoutException.class, IOException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
            }

            @Override
            HttpResponse makeHttpRequestCall() throws IOException {
                try {
                    sendHttpPost();
                    Assert.fail("Expect MSALAuthenticationException to be thrown.");
                } catch (final MSALAuthenticationException e) {
                    Assert.assertNotNull(e);
                    Assert.assertTrue(e.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_SERVER_ERROR));
                    Assert.assertTrue(e.getMessage().contains(
                            "StatusCode: " + String.valueOf(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
                }

                return null;
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_POST;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getResponseCode();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNull(httpResponse);
            }
        }.performTest();
    }

    /**
     * Verify that initial http post fails with {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT}(retryable status code
     * 500/503/504), retry fails with {@link SocketTimeoutException}.
     */
    @Test
    public void testGetFailedWithRetryableCodeRetryFailedWithSocketTimeout() throws IOException {
        new BaseHttpRequestTestCase() {

            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(
                        IOException.class, SocketTimeoutException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
            }

            @Override
            HttpResponse makeHttpRequestCall() throws IOException {

                try {
                    sendHttpGet();
                    Assert.fail("Expect MSALAuthenticationException to be thrown.");
                } catch (final MSALAuthenticationException e) {
                    Assert.assertNotNull(e);
                    Assert.assertTrue(e.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_NETWORK_TIME_OUT));
                    Assert.assertNotNull(e.getCause());
                    Assert.assertTrue(e.getCause() instanceof SocketTimeoutException);
                }

                return null;
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_GET;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getErrorStream();
                Mockito.verify(mockedConnection, Mockito.times(1)).getResponseCode();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNull(httpResponse);
            }
        }.performTest();
    }

    /**
     * Verify that initial http post fails with {@link SocketTimeoutException} and retry fails with
     * {@link SocketTimeoutException}.
     */
    @Test
    public void testGetFailedWithSocketTimeoutRetryFailedWithSocketTimeout() throws IOException {
        new BaseHttpRequestTestCase() {
            @Override
            @SuppressWarnings("unchecked")
            void mockHttpUrlConnectionGetStream(HttpURLConnection mockedConnection) throws IOException {
                Mockito.when(mockedConnection.getInputStream()).thenThrow(
                        SocketTimeoutException.class, SocketTimeoutException.class);
                Mockito.when(mockedConnection.getErrorStream()).thenReturn(Util.createInputStream(getErrorResponse()));
                Mockito.when(mockedConnection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
            }

            @Override
            HttpResponse makeHttpRequestCall() throws IOException {
                try {
                    sendHttpGet();
                    Assert.fail("Expect MSALAuthenticationException to be thrown.");
                } catch (final MSALAuthenticationException e) {
                    Assert.assertNotNull(e);
                    Assert.assertTrue(e.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_NETWORK_TIME_OUT));
                    Assert.assertNotNull(e.getCause());
                    Assert.assertTrue(e.getCause() instanceof SocketTimeoutException);
                }

                return null;
            }

            @Override
            int getNetworkCallTimes() {
                return 2;
            }

            @Override
            String getRequestMethod() {
                return REQUEST_METHOD_GET;
            }

            @Override
            void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                    throws IOException {
                Mockito.verify(mockedConnection, Mockito.times(2)).getInputStream();
                Mockito.verify(mockedConnection, Mockito.times(0)).getErrorStream();
                Mockito.verify(mockedConnection, Mockito.times(0)).getResponseCode();
            }

            @Override
            void verifyHttpResponse(final HttpResponse httpResponse) {
                Assert.assertNull(httpResponse);
            }
        }.performTest();
    }

    /**
     * Test case for verifying that the http request succeeds(Retry could happen and succeed)with 200.
     */
    abstract class HttpRequestPositiveTestCase extends BaseHttpRequestTestCase {

        @Override
        final HttpResponse makeHttpRequestCall() throws IOException {
            try {
                if (REQUEST_METHOD_POST.equalsIgnoreCase(getRequestMethod())) {
                    return sendHttpPost();
                } else {
                    return sendHttpGet();
                }
            } catch (final MSALAuthenticationException e) {
                Assert.fail();
            }

            return null;
        }

        @Override
        final void verifyHttpResponse(final HttpResponse httpResponse) {
            Assert.assertNotNull(httpResponse);
            Assert.assertTrue(httpResponse.getStatusCode() == HttpURLConnection.HTTP_OK);
            Assert.assertTrue(httpResponse.getResponseBody().equals(getSuccessResponse()));
        }
    }

    /**
     * Test case for verify that http request fails with non retryable error code.
     */
    abstract class HttpRequestRetryFailedWithNonRetryableErrorCode extends BaseHttpRequestTestCase {

        @Override
        final HttpResponse makeHttpRequestCall() throws IOException {
            try {
                if (REQUEST_METHOD_GET.equalsIgnoreCase(getRequestMethod())) {
                    return sendHttpGet();
                } else {
                    return sendHttpPost();
                }
            } catch (final MSALAuthenticationException e) {
                Assert.fail("unexpected exception.");
            }

            return null;
        }
    }

    /**
     * Base test case for the network request call.
     */
    protected abstract class BaseHttpRequestTestCase {
        abstract void mockHttpUrlConnectionGetStream(final HttpURLConnection mockedConnection) throws IOException;
        abstract HttpResponse makeHttpRequestCall() throws IOException;
        abstract int getNetworkCallTimes();
        abstract String getRequestMethod();
        abstract void verifyInputStreamAndErrorStreamCalledTimes(final HttpURLConnection mockedConnection)
                throws IOException;
        abstract void verifyHttpResponse(final HttpResponse httpResponse);

        protected void performTest() throws IOException {
            final HttpURLConnection mockedUrlConnection = Mockito.mock(HttpURLConnection.class);
            Mockito.when(mockedUrlConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
            mockHttpUrlConnectionGetStream(mockedUrlConnection);
            Util.prepareMockedUrlConnection(mockedUrlConnection);

            final HttpResponse response = makeHttpRequestCall();

            Mockito.verify(mockedUrlConnection, Mockito.times(getNetworkCallTimes())).setRequestMethod(
                    Mockito.refEq(getRequestMethod()));
            verifyInputStreamAndErrorStreamCalledTimes(mockedUrlConnection);

            verifyHttpResponse(response);
        }

        /**
         * Send http get request.
         */
        final HttpResponse sendHttpGet() throws IOException, MSALAuthenticationException {
            return HttpRequest.sendGet(Util.getValidRequestUrl(), Collections.<String, String>emptyMap());
        }

        /**
         * Send http post request.
         */
        final HttpResponse sendHttpPost() throws IOException, MSALAuthenticationException {
            return HttpRequest.sendPost(Util.getValidRequestUrl(), Collections.<String, String>emptyMap(),
                    "SomeRequestMessage".getBytes(), "application/x-www-form-urlencoded");
        }
    }

    /**
     * @return Successful response from server.
     */
    private String getSuccessResponse() {
        return "{\"response\":\"success response\"}";
    }

    /**
     * @return Error response from server.
     */
    private String getErrorResponse() {
        return "{\"response\":\"error response\"}";
    }
}
