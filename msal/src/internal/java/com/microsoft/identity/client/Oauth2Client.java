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

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * MSAL internal class for handling the interaction with {@link HttpRequest}, parse the JSON response and create
 * {@link TokenResponse}.
 */
final class Oauth2Client {
    private static final String TAG = Oauth2Client.class.getSimpleName();

    static final String DEFAULT_TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    static final String POST_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final Map<String, String> mBodyParameters = new HashMap<>();
    private final Map<String, String> mQueryParameters = new HashMap<>();
    private final Map<String, String> mHeader = new HashMap<>(PlatformIdHelper.getPlatformIdParameters());

    void addQueryParameter(final String key, final String value) {
        mQueryParameters.put(key, value);
    }

    void addBodyParameter(final String key, final String value) {
        mBodyParameters.put(key, value);
    }

    void addHeader(final String key, final String value) {
        mHeader.put(key, value);
    }

    TokenResponse getToken(final URL authorityUrl) throws IOException, RetryableException,
            AuthenticationException {
        final URL tokenEndpoint = getTokenEndpoint(authorityUrl);
        addHeader("Accept", "application/json");

        final HttpResponse response = HttpRequest.sendPost(tokenEndpoint, mHeader,
                buildRequestMessage(mBodyParameters), POST_CONTENT_TYPE);

        // TODO: device auth challenge should be handled here.

        final UUID correlationIdInRequest = UUID.fromString(mHeader.get(
                OauthConstants.OauthHeader.CORRELATION_ID));
        verifyCorrelationIdInResponseHeaders(response.getHeaders(), correlationIdInRequest);

        return parseRawResponseToTokenResponse(response);
    }

    URL getTokenEndpoint(final URL authorityUrl) throws UnsupportedEncodingException {
        final Set<String> queryStringSet = new HashSet<>();
        for (final Map.Entry<String, String> entry : mQueryParameters.entrySet()) {
            queryStringSet.add(entry.getKey() + "=" + MSALUtils.urlEncode(entry.getValue()));
        }

        final String queryString = queryStringSet.isEmpty() ? "" : "?" + MSALUtils.convertSetToString(queryStringSet, "&");
        final URL tokenEndpoint;
        try {
            tokenEndpoint = new URL(authorityUrl.toString() + DEFAULT_TOKEN_ENDPOINT
                    + queryString);
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Malformed authority URL");
        }

        return tokenEndpoint;
    }

    private byte[] buildRequestMessage(final Map<String, String> bodyParameters) throws UnsupportedEncodingException {
        final Set<String> requestBodyEntries = new TreeSet<>();
        final Set<Map.Entry<String, String>> bodyEntries = bodyParameters.entrySet();
        for (Map.Entry<String, String> bodyEntry : bodyEntries) {
            requestBodyEntries.add(bodyEntry.getKey() + "=" + MSALUtils.urlEncode(bodyEntry.getValue()));
        }

        final String requestMessage = requestBodyEntries.isEmpty() ? "" : MSALUtils.convertSetToString(requestBodyEntries, "&");
        return requestMessage.getBytes(MSALUtils.ENCODING_UTF8);
    }

    private void verifyCorrelationIdInResponseHeaders(final Map<String, List<String>> responseHeader,
                                                      final UUID correlationIdInRequest) {
        if (responseHeader == null
                || !responseHeader.containsKey(OauthConstants.OauthHeader.CORRELATION_ID_IN_RESPONSE)) {
            // TODO: Looger.w(TAG, "response doesn't contain headers or header doesn't include correlation id");
            return;
        }

        final List<String> correlationIdsInHeader = responseHeader.get(
                OauthConstants.OauthHeader.CORRELATION_ID_IN_RESPONSE);
        if (correlationIdsInHeader == null || correlationIdsInHeader.size() == 0) {
            // TODO: Logger.w(TAG, "Correlation id returned is empty");
            return;
        }

        final String correlationIdInHeader = correlationIdsInHeader.get(0);
        if (!MSALUtils.isEmpty(correlationIdInHeader)) {
            try {
                final UUID correlationId = UUID.fromString(correlationIdInHeader);
                //CHECKSTYLE:OFF: checkstyle:EmptyBlock
                if (!correlationId.equals(correlationIdInRequest)) {
                    // TODO: Logger.warn(TAG, "Correlation id is not matching");
                }
            } catch (final IllegalArgumentException e) {
                //CHECKSTYLE:ON: checkstyle:EmptyBlock
                //  UUID.fromString throws IllegalArgumentException if {@code uuid} is not formatted correctly.
                // TODO: Logger.e(TAG, "", e);
            }
        }
    }

    private TokenResponse parseRawResponseToTokenResponse(final HttpResponse response) throws AuthenticationException {
        if (MSALUtils.isEmpty(response.getBody())) {
            // TODO: Discuss in this case, should we create a concrete error with status code, indicating it's server error.
            throw new AuthenticationException(MSALError.SERVER_ERROR, "statusCode: " + response.getStatusCode());
        }

        try {
            final Map<String, String> responseItems = MSALUtils.extractJsonObjectIntoMap(response.getBody());
            if (response.getStatusCode() != HttpURLConnection.HTTP_OK) {
                return TokenResponse.createTokenResponseWithError(responseItems);
            } else {
                return TokenResponse.createSuccessTokenResponse(responseItems);
            }
        } catch (final JSONException e) {
            throw new AuthenticationException(MSALError.JSON_PARSE_FAILURE, "Fail to parse Json", e);
        }
    }
}
