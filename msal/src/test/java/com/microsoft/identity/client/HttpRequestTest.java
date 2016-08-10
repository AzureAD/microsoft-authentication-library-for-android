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
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Collections;

/**
 * Tests for {@link HttpRequest}.
 */
public final class HttpRequestTest {
    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    /**
     * Verify the expected exception is thrown when sending get request with null url.
     */
    @Test(expected = NullPointerException.class)
    public void testNullRequestUrl() throws IOException, RetryableException {
        HttpRequest.sendGet(null, Collections.<String, String>emptyMap());
    }

    /**
     * Verify that http get succeed and no retry happens.
     */
    @Test
    public void testHttpGetSucceed() throws IOException {
        // prepare the connection, only one connection will be made.
        final HttpURLConnection mockedSuccessConnection = MockUtil.getMockedConnectionWithSuccessResponse(
                getSuccessResponse());
        HttpUrlConnectionFactory.addMockedConnection(mockedSuccessConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 1);
            final HttpResponse response = sendHttpGet();
            verifySuccessHttpResponse(response);
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final InOrder inOrder = Mockito.inOrder(mockedSuccessConnection);
        inOrder.verify(mockedSuccessConnection).getInputStream();
        inOrder.verify(mockedSuccessConnection, Mockito.never()).getErrorStream();
        inOrder.verify(mockedSuccessConnection).getResponseCode();
        inOrder.verify(mockedSuccessConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http post succeeds and no retry happens.
     */
    @Test
    public void testHttpPostSucceed() throws IOException {
        // prepare the connection, only one connection will be made.
        final HttpURLConnection mockedSuccessConnection = MockUtil.getMockedConnectionWithSuccessResponse(
                getSuccessResponse());
        mockRequestBody(mockedSuccessConnection);
        HttpUrlConnectionFactory.addMockedConnection(mockedSuccessConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 1);
            final HttpResponse response = sendHttpPost();
            verifySuccessHttpResponse(response);
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final InOrder inOrder = Mockito.inOrder(mockedSuccessConnection);
        // default times for verify is 1.
        inOrder.verify(mockedSuccessConnection).getInputStream();
        inOrder.verify(mockedSuccessConnection, Mockito.never()).getErrorStream();
        inOrder.verify(mockedSuccessConnection).getResponseCode();
        inOrder.verify(mockedSuccessConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify the correct response is returned if first network call fails with internal error, and retry succeeds.
     */
    @Test
    public void testHttpPostFailedWith500RetrySucceed() throws IOException {
        // Set up two connections, the first is failed with 500, the second one succeeds.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_INTERNAL_ERROR, getErrorResponse());
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithSuccessResponse(getSuccessResponse());
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            final HttpResponse response = sendHttpPost();
            verifySuccessHttpResponse(response);
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();
        // no HttpResponse is created, no need to verify getHeaderFields.

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that the initial post request failed with {@link HttpURLConnection#HTTP_UNAVAILABLE} and retry succeeds.
     */
    @Test
    public void testHttpPostFailedWith503RetrySucceed() throws IOException {
        // Set up two connections, the first is failed with 503, the second one succeeds.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_UNAVAILABLE, getErrorResponse());
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithSuccessResponse(
                getSuccessResponse());
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            final HttpResponse response = sendHttpPost();
            verifySuccessHttpResponse(response);
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();
        // No HttpResponse created, no interaction on getHeaderFields

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that the initial get request failed with {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT} and retry
     * succeeds.
     */
    @Test
    public void testHttpGetFailedWith504RetrySucceed() throws IOException {
        // Set up two connections, the first is failed with 503, the second one succeeds.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_GATEWAY_TIMEOUT, getErrorResponse());
        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithSuccessResponse(
                getSuccessResponse());

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            final HttpResponse response = sendHttpGet();
            verifySuccessHttpResponse(response);
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that the initial post request failed with {@link SocketTimeoutException} and retry
     * succeeds.
     */
    @Test
    public void testHttpPostFailedWithSocketTimeoutRetrySucceed() throws IOException {
        // Set up two connections, the first is failed with SocketTimeout, the second one succeeds.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithSocketTimeout();
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithSuccessResponse(
                getSuccessResponse());
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            final HttpResponse response = sendHttpPost();
            verifySuccessHttpResponse(response);
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection, Mockito.never()).getErrorStream();
        inOrder.verify(firstConnection, Mockito.never()).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_BAD_REQUEST}, no retry happens.
     */
    @Test
    public void testHttpGetFailedNoRetry() throws IOException {
        final HttpURLConnection mockedFailureConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_REQUEST, getErrorResponse());
        HttpUrlConnectionFactory.addMockedConnection(mockedFailureConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 1);
            final HttpResponse response = sendHttpGet();
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);
            Assert.assertTrue(response.getBody().equals(getErrorResponse()));
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final InOrder inOrder = Mockito.inOrder(mockedFailureConnection);
        inOrder.verify(mockedFailureConnection).getInputStream();
        inOrder.verify(mockedFailureConnection).getErrorStream();
        inOrder.verify(mockedFailureConnection).getResponseCode();
        inOrder.verify(mockedFailureConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED}, no retry happens.
     */
    @Test
    public void testHttpPostFailedNoRetry() throws IOException {
        final HttpURLConnection mockedFailureConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_UNAUTHORIZED, getErrorResponse());
        mockRequestBody(mockedFailureConnection);
        HttpUrlConnectionFactory.addMockedConnection(mockedFailureConnection);

        // send a post request
        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 1);

            final HttpResponse response = sendHttpPost();
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED);
            Assert.assertTrue(response.getBody().equals(getErrorResponse()));
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final InOrder inOrder = Mockito.inOrder(mockedFailureConnection);
        inOrder.verify(mockedFailureConnection).getInputStream();
        inOrder.verify(mockedFailureConnection).getErrorStream();
        inOrder.verify(mockedFailureConnection).getResponseCode();
        inOrder.verify(mockedFailureConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that http get request fails with {@link HttpURLConnection#HTTP_BAD_METHOD} and no response body,
     * no retry happens.
     */
    @Test
    public void testHttpGetFailedNoRetryNoResponseBody() throws IOException {
        final HttpURLConnection mockedFailureConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_METHOD, null);
        HttpUrlConnectionFactory.addMockedConnection(mockedFailureConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 1);
            final HttpResponse response = sendHttpGet();
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getStatusCode() == HttpURLConnection.HTTP_BAD_METHOD);
            Assert.assertTrue(response.getBody().isEmpty());
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final InOrder inOrder = Mockito.inOrder(mockedFailureConnection);
        inOrder.verify(mockedFailureConnection).getInputStream();
        inOrder.verify(mockedFailureConnection).getErrorStream();
        inOrder.verify(mockedFailureConnection).getResponseCode();
        inOrder.verify(mockedFailureConnection).getHeaderFields();
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that initial http post fails with {@link HttpURLConnection#HTTP_INTERNAL_ERROR}(retryable status code
     * 500/503/504), retry fails with {@link HttpURLConnection#HTTP_UNAUTHORIZED}(non retryable status code).
     */
    @Test
    public void testHttpPostFailedWithRetryableStatusCodeRetryFailsWithNonRetryableCode() throws IOException {
        // The first connection fails with retryable status code 500, the retry connection fails with 401.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_INTERNAL_ERROR, getErrorResponse());
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_UNAUTHORIZED, getErrorResponse());
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            final HttpResponse response = sendHttpGet();
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED);
            Assert.assertTrue(response.getBody().equals(getErrorResponse()));
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that initial http post fails with {@link SocketTimeoutException}, retry fails with
     * {@link HttpURLConnection#HTTP_BAD_REQUEST}(non retryable status code).
     */
    @Test
    public void testHttpPostFailedWithSocketTimeoutRetryFailedWithNonRetryableCode() throws IOException {
        // The first connection fails with retryable SocketTimeout, the retry connection fails with 400.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithSocketTimeout();
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_BAD_REQUEST, getErrorResponse());
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            final HttpResponse response = sendHttpGet();
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST);
            Assert.assertTrue(response.getBody().equals(getErrorResponse()));
        } catch (final RetryableException e) {
            Assert.fail();
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection, Mockito.never()).getErrorStream();
        inOrder.verify(firstConnection, Mockito.never()).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that initial http post fails with {@link HttpURLConnection#HTTP_INTERNAL_ERROR}(retryable status code
     * 500/503/504), retry fails with {@link HttpURLConnection#HTTP_UNAVAILABLE}(retryable status code).
     */
    @Test
    public void testPostFailedWithRetryableErrorRetryFailedWithRetryableError() throws IOException {
        // The first connection fails with retryable status code 500, the retry connection fails again with retryable
        // status code 503.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_INTERNAL_ERROR, getErrorResponse());
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_UNAVAILABLE, getErrorResponse());
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            sendHttpPost();
            Assert.fail("Expect AuthenticationException to be thrown.");
        } catch (final RetryableException e) {
            Assert.assertNotNull(e);
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof AuthenticationException);
            final AuthenticationException innerException = (AuthenticationException) e.getCause();
            Assert.assertTrue(innerException.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_SERVER_ERROR));
            Assert.assertTrue(innerException.getMessage().contains(
                    "StatusCode: " + String.valueOf(HttpURLConnection.HTTP_UNAVAILABLE)));
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that initial http post fails with {@link SocketTimeoutException}, retry fails with
     * {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT}(retryable status code).
     */
    @Test
    public void testPostFailedWithSocketTimeoutRetryFailedWithRetryableError() throws IOException {
        // The first connection fails with retryable SocketTimeout, the retry connection fails again with retryable
        // status code 504.
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithSocketTimeout();
        mockRequestBody(firstConnection);

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_GATEWAY_TIMEOUT, getErrorResponse());
        mockRequestBody(secondConnection);

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            sendHttpPost();
            Assert.fail("Expect AuthenticationException to be thrown.");
        } catch (final RetryableException e) {
            Assert.assertNotNull(e);
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof AuthenticationException);
            final AuthenticationException innerException = (AuthenticationException) e.getCause();
            Assert.assertTrue(innerException.getErrorCode().equals(MSALError.RETRY_FAILED_WITH_SERVER_ERROR));
            Assert.assertTrue(innerException.getMessage().contains(
                    "StatusCode: " + String.valueOf(HttpURLConnection.HTTP_GATEWAY_TIMEOUT)));
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);

        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection, Mockito.times(0)).getErrorStream();
        inOrder.verify(firstConnection, Mockito.times(0)).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection).getErrorStream();
        inOrder.verify(secondConnection).getResponseCode();
        inOrder.verify(secondConnection).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that initial http post fails with {@link HttpURLConnection#HTTP_GATEWAY_TIMEOUT}(retryable status code
     * 500/503/504), retry fails with {@link SocketTimeoutException}.
     */
    @Test
    public void testGetFailedWithRetryableCodeRetryFailedWithSocketTimeout() throws IOException {
        // The first connection fails with retryable status code 504, the retry fails SocketTimeout
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithFailureResponse(
                HttpURLConnection.HTTP_GATEWAY_TIMEOUT, getErrorResponse());

        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithSocketTimeout();

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            sendHttpGet();
            Assert.fail("Expect AuthenticationException to be thrown.");
        } catch (final RetryableException e) {
            Assert.assertNotNull(e);
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof  SocketTimeoutException);
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection).getErrorStream();
        inOrder.verify(firstConnection).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.times(0)).getErrorStream();
        inOrder.verify(secondConnection, Mockito.times(0)).getResponseCode();
        inOrder.verify(secondConnection, Mockito.never()).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Verify that initial http post fails with {@link SocketTimeoutException} and retry fails with
     * {@link SocketTimeoutException}.
     */
    @Test
    public void testGetFailedWithSocketTimeoutRetryFailedWithSocketTimeout() throws IOException {
        // The two connections are all failed with SocketTimeout
        final HttpURLConnection firstConnection = MockUtil.getMockedConnectionWithSocketTimeout();
        final HttpURLConnection secondConnection = MockUtil.getMockedConnectionWithSocketTimeout();

        HttpUrlConnectionFactory.addMockedConnection(firstConnection);
        HttpUrlConnectionFactory.addMockedConnection(secondConnection);

        try {
            Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 2);
            sendHttpGet();
            Assert.fail("Expect AuthenticationException to be thrown.");
        } catch (final RetryableException e) {
            Assert.assertNotNull(e);
            Assert.assertNotNull(e.getCause());
            Assert.assertTrue(e.getCause() instanceof  SocketTimeoutException);
        }

        Assert.assertTrue(HttpUrlConnectionFactory.getMockedConnectionCountInQueue() == 0);
        final InOrder inOrder = Mockito.inOrder(firstConnection, secondConnection);
        inOrder.verify(firstConnection).getInputStream();
        inOrder.verify(firstConnection, Mockito.never()).getErrorStream();
        inOrder.verify(firstConnection, Mockito.never()).getResponseCode();

        inOrder.verify(secondConnection).getInputStream();
        inOrder.verify(secondConnection, Mockito.never()).getErrorStream();
        inOrder.verify(secondConnection, Mockito.never()).getResponseCode();
        inOrder.verify(secondConnection, Mockito.never()).getHeaderFields();

        inOrder.verifyNoMoreInteractions();
    }

    void verifySuccessHttpResponse(final HttpResponse httpResponse) {
        Assert.assertNotNull(httpResponse);
        Assert.assertTrue(httpResponse.getStatusCode() == HttpURLConnection.HTTP_OK);
        Assert.assertTrue(httpResponse.getBody().equals(getSuccessResponse()));
    }
    /**
     * Send http get request.
     */
    final HttpResponse sendHttpGet() throws IOException, RetryableException {
        return HttpRequest.sendGet(Util.getValidRequestUrl(), Collections.<String, String>emptyMap());
    }

    /**
     * Send http post request.
     */
    final HttpResponse sendHttpPost() throws IOException, RetryableException {
        return HttpRequest.sendPost(Util.getValidRequestUrl(), Collections.<String, String>emptyMap(),
                "SomeRequestMessage".getBytes(), "application/x-www-form-urlencoded");
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

    private void mockRequestBody(final HttpURLConnection mockedConnection) throws IOException {
        Mockito.when(mockedConnection.getOutputStream()).thenReturn(Mockito.mock(OutputStream.class));
    }
}
