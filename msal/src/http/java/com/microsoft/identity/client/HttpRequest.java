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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Internal class for handling http request.
 */
final class HttpRequest {
    // static constant variables
    private static final String TAG = HttpRequest.class.getSimpleName();

    private static final String HOST = "Host";
    /** The waiting time before doing retry to prevent hitting the server immediately failure. */
    private static final int RETRY_TIME_WAITING_PERIOD_MSEC = 1000;
    private static final int STREAM_BUFFER_SIZE = 1024;

    static final String REQUEST_METHOD_GET = "GET";
    static final String REQUEST_METHOD_POST = "POST";
    static final int CONNECT_TIME_OUT_MSEC = 30000;
    static final int READ_TIME_OUT_MSEC = 30000;

    // class variables
    private final URL mRequestUrl;
    private final byte[] mRequestContent;
    private final String mRequestContentType;
    private final String mRequestMethod;
    private final Map<String, String> mRequestHeaders = new HashMap<>();

    /**
     * Constructor for {@link HttpRequest} with request {@link URL} and request headers.
     * @param requestUrl The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     */
    private HttpRequest(final URL requestUrl, final Map<String, String> requestHeaders, final String requestMethod) {
        this(requestUrl, requestHeaders, requestMethod, null, null);
    }

    /**
     * Constructor for {@link HttpRequest} with request {@link URL}, headers, post message and the request content
     * type.
     * @param requestUrl The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     * @param requestContent Post message sent in the post request.
     * @param requestContentType Request content type.
     */
    private HttpRequest(final URL requestUrl, final Map<String, String> requestHeaders, final String requestMethod,
                        final byte[] requestContent, final String requestContentType) {
        mRequestUrl = requestUrl;

        mRequestHeaders.put(HOST, requestUrl.getAuthority());
        mRequestHeaders.putAll(requestHeaders);

        mRequestMethod = requestMethod;
        mRequestContent = requestContent;
        mRequestContentType = requestContentType;
    }

    /**
     * Send post request {@link URL}, headers, post message and the request content type.
     * @param requestUrl The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     * @param requestContent Post message sent in the post request.
     * @param requestContentType Request content type.
     */
    public static HttpResponse sendPost(final URL requestUrl, final Map<String, String> requestHeaders,
                                        final byte[] requestContent, final String requestContentType)
            throws IOException, MsalServiceException {
        final HttpRequest httpRequest = new HttpRequest(requestUrl, requestHeaders, REQUEST_METHOD_POST,
                requestContent, requestContentType);
        Logger.verbose(TAG, null, "Sending Http Post request.");
        return httpRequest.send();
    }

    /**
     * Send Get request {@link URL} and request headers.
     * @param requestUrl The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     */
    public static HttpResponse sendGet(final URL requestUrl, final Map<String, String> requestHeaders)
            throws IOException, MsalServiceException {
        final HttpRequest httpRequest = new HttpRequest(requestUrl, requestHeaders, REQUEST_METHOD_GET);

        Logger.verbose(TAG, null, "Sending Http Get request.");
        return httpRequest.send();
    }

    /**
     * Send http request.
     */
    private HttpResponse send() throws IOException, MsalServiceException {
        final HttpResponse response;
        try {
            response = sendWithRetry();
        } catch (final SocketTimeoutException socketTimeoutException) {
            throw new MsalServiceException(MSALError.REQUEST_TIMEOUT, "Retry failed again with SocketTimeout", socketTimeoutException);
        }

        if (response != null && isRetryableError(response.getStatusCode())) {
            throw new MsalServiceException(MSALError.SERVICE_NOT_AVAILABLE, "Retry failed again with 500/503/504", response.getStatusCode(), null);
        }

        return response;
    }

    /**
     * Execute the send request, and retry if needed. Retry happens on all the endpoint when receiving
     * {@link SocketTimeoutException} or retryable error 500/503/504.
     */
    private HttpResponse sendWithRetry() throws IOException {
        final HttpResponse httpResponse;
        try {
            httpResponse = executeHttpSend();
        } catch (final SocketTimeoutException socketTimeoutException) {
            // In android, network timeout is thrown as the SocketTimeOutException, we need to catch this and perform
            // retry. If retry also fails with timeout, the socketTimeoutException will be bubbled up
            Logger.verbose(TAG, null, "Request timeout with SocketTimeoutException, will retry one more time.");
            waitBeforeRetry();
            return executeHttpSend();
        }

        if (isRetryableError(httpResponse.getStatusCode())) {
            // retry if we get 500/503/504
            Logger.verbose(TAG, null, "Received retryable status code 500/503/504, will retry one more time.");
            waitBeforeRetry();
            return executeHttpSend();
        }

        return httpResponse;
    }

    private HttpResponse executeHttpSend() throws IOException {
        final HttpURLConnection urlConnection = setupConnection();
        urlConnection.setRequestMethod(mRequestMethod);
        setRequestBody(urlConnection, mRequestContent, mRequestContentType);

        InputStream responseStream = null;
        final HttpResponse response;
        try {
            try {
                responseStream = urlConnection.getInputStream();
            } catch (final SocketTimeoutException socketTimeoutException) {
                // SocketTimeoutExcetion is thrown when connection timeout happens. For connection timeout, we want
                // to retry once. Throw the exception to the upper layer, and the upper layer will handle the rety.
                throw socketTimeoutException;
            } catch (final IOException ioException) {
                responseStream = urlConnection.getErrorStream();
            }

            final int statusCode = urlConnection.getResponseCode();
            final String responseBody = responseStream == null ? "" : convertStreamToString(responseStream);
            Logger.verbose(TAG, null, "Returned status code is: " + statusCode);
            response = new HttpResponse(statusCode, responseBody, urlConnection.getHeaderFields());
        } finally {
            safeCloseStream(responseStream);
        }

        return response;
    }

    private HttpURLConnection setupConnection() throws IOException {
        final HttpURLConnection urlConnection = HttpUrlConnectionFactory.createHttpURLConnection(mRequestUrl);
        urlConnection.setRequestProperty("Connection", "close");

        // Apply request headers and update the headers with default attributes first
        final Set<Map.Entry<String, String>> headerEntries = mRequestHeaders.entrySet();
        for (final Map.Entry<String, String> entry : headerEntries) {
            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        urlConnection.setConnectTimeout(CONNECT_TIME_OUT_MSEC);
        urlConnection.setReadTimeout(READ_TIME_OUT_MSEC);
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setUseCaches(false);
        urlConnection.setDoInput(true);

        return urlConnection;
    }

    private static void setRequestBody(final HttpURLConnection connection, final byte[] contentRequest,
                                       final String requestContentType) throws IOException {
        if (contentRequest == null) {
            return;
        }

        connection.setDoOutput(true);

        if (!MSALUtils.isEmpty(requestContentType)) {
            connection.setRequestProperty("Content-Type", requestContentType);
        }

        connection.setRequestProperty("Content-Length", String.valueOf(contentRequest.length));

        // https://developer.android.com/reference/java/net/HttpURLConnection.html. In the Posting Content section
        // recommended: For best performance, you should call either setFixedLengthStreamingMode(int) when the body
        // length is known in advance, or setChunkedStreamingMode(int) when it is not. Otherwise HttpURLConnection
        // will be forced to buffer the complete request body in memory before it is transmitted,
        // wasting (and possibly exhausting) heap and increasing latency.
        connection.setFixedLengthStreamingMode(contentRequest.length);

        OutputStream out = null;
        try {
            out = connection.getOutputStream();
            out.write(contentRequest);
        } finally {
            safeCloseStream(out);
        }
    }

    /**
     * Convert stream into the string.
     *
     * @param inputStream {@link InputStream} to be converted to be a string.
     * @return The converted string
     * @throws IOException Thrown when failing to access inputStream stream.
     */
    private static String convertStreamToString(final InputStream inputStream) throws IOException {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            final char[] buffer = new char[STREAM_BUFFER_SIZE];
            final StringBuilder stringBuilder = new StringBuilder();
            int charsRead;
            while ((charsRead = reader.read(buffer)) > -1) {
                stringBuilder.append(buffer, 0, charsRead);
            }

            return stringBuilder.toString();
        } finally {
            safeCloseStream(inputStream);
        }
    }

    /**
     * Close the stream safely.
     *
     * @param stream stream to be closed
     */
    private static void safeCloseStream(final Closeable stream) {
        if (stream == null) {
            return;
        }

        try {
            stream.close();
        } catch (final IOException e) {
            Logger.error(TAG, null, "Encounter IO exception when trying to close the stream", e);
        }
    }

    /**
     * Check if the given status code is the retryable status code(500/503/504).
     * @param statusCode The status to check.
     * @return True if the status code is 500, 503 or 504, false otherwise.
     */
    private static boolean isRetryableError(final int statusCode) {
        return statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR
                || statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT
                || statusCode == HttpURLConnection.HTTP_UNAVAILABLE;
    }

    /**
     * Having the thread wait for 1 second before doing the retry to avoid hitting server immediately.
     */
    private void waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_TIME_WAITING_PERIOD_MSEC);
        } catch (final InterruptedException interrupted) {
            Logger.info(TAG, null, "Fail the have the thread waiting for 1 second before doing the retry");
        }
    }
}
