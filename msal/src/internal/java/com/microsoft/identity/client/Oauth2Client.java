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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * MSAL internal class for handling the interaction with {@link HttpRequest}, parse the JSON response and create
 * {@link TokenResponse}.
 */
final class Oauth2Client {
    private static final String TAG = Oauth2Client.class.getSimpleName();
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_ACCEPT_VALUE = "application/json";

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

    /**
     * Send post request to get token with the given authority. Authority will hold the token endpoint.
     */
    TokenResponse getToken(final Authority authority) throws IOException, RetryableException,
            AuthenticationException {

        return executeHttpRequest(HttpRequest.REQUEST_METHOD_POST, authority.getTokenEndpoint(), TokenResponse.class);
    }

    /**
     * Discover the AAD instance with the given instance discovery endpoint.
     */
    InstanceDiscoveryResponse discoveryAADInstance(final URL instanceDiscoveryEndpoint) throws IOException,
            RetryableException, AuthenticationException {

        return executeHttpRequest(HttpRequest.REQUEST_METHOD_GET, instanceDiscoveryEndpoint.toString(), InstanceDiscoveryResponse.class);
    }

    /**
     * Perform tenant discovery to get authorize endpoint and token endpoint.
     */
    TenantDiscoveryResponse discoverEndpoints(final URL openIdConfigurationEndpoint) throws IOException,
            RetryableException, AuthenticationException {

        return executeHttpRequest(HttpRequest.REQUEST_METHOD_GET, openIdConfigurationEndpoint.toString(), TenantDiscoveryResponse.class);
    }

    /**
     * Execute the http request.
     */
    private <T extends BaseOauth2Response> T executeHttpRequest(final String requestMethod, final String endpoint, final Class clazz)
            throws IOException, RetryableException, AuthenticationException {
        // append query parameter to the endpoint first
        final URL endpointWithQP;
        try {
            endpointWithQP = new URL(MSALUtils.appendQueryParameterToUrl(endpoint, mQueryParameters));
        } catch (final MalformedURLException e) {
            throw new AuthenticationException(MSALError.SERVER_ERROR, "Malformed endpoint url " + e.getMessage(), e);
        }

        // add common headers
        addHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
        addHeader(OauthConstants.OauthHeader.CORRELATION_ID_IN_RESPONSE, "true");

        final HttpResponse response;
        if (HttpRequest.REQUEST_METHOD_GET.equals(requestMethod)) {
            response = HttpRequest.sendGet(endpointWithQP, mHeader);
        } else {
            response = HttpRequest.sendPost(endpointWithQP, mHeader,
                    buildRequestMessage(mBodyParameters), POST_CONTENT_TYPE);
        }

        return parseRawResponse(response, clazz);
    }

    private <T extends BaseOauth2Response> T parseRawResponse(final HttpResponse httpResponse, final Class clazz) throws AuthenticationException {
        // verify the correlation id in the httpResponse headers before parsing the httpResponse body.
        verifyCorrelationIdInResponseHeaders(httpResponse);

        final Map<String, String> responseItems = parseResponseItems(httpResponse);


        if (httpResponse.getStatusCode() == HttpURLConnection.HTTP_OK) {
            return (T) BaseOauth2Response.createSuccessResponse(responseItems);
        }

        final BaseOauth2Response errorResponse;
        try {
            errorResponse = BaseOauth2Response.createErrorResponse(responseItems);
        } catch (final JSONException e) {
            throw new AuthenticationException(MSALError.JSON_PARSE_FAILURE, "Fail to parse Json", e);
        }

        try {
            final Constructor<T> constructor = clazz.getDeclaredConstructor(BaseOauth2Response.class);
            return constructor.newInstance(errorResponse);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("Unable to find generic type constructor.", e);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("Unable to create new instance.", e);
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("Unable to create new instance.", e);
        } catch (final InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to create new instance.", e);
        }
    }

    private Map<String, String> parseResponseItems(final HttpResponse response) throws AuthenticationException {
        if (MSALUtils.isEmpty(response.getBody())) {
            // TODO: Discuss in this case, should we create a concrete error with status code, indicating it's server error.
            throw new AuthenticationException(MSALError.SERVER_ERROR, "statusCode: " + response.getStatusCode());
        }

        final Map<String, String> responseItems;
        try {
            responseItems = MSALUtils.extractJsonObjectIntoMap(response.getBody());
        } catch (final JSONException e) {
            throw new AuthenticationException(MSALError.JSON_PARSE_FAILURE, "Fail to parse JSON", e);
        }

        return responseItems;
    }

    private byte[] buildRequestMessage(final Map<String, String> bodyParameters) throws UnsupportedEncodingException {
        final Set<String> requestBodyEntries = new HashSet<>();
        final Set<Map.Entry<String, String>> bodyEntries = bodyParameters.entrySet();
        for (Map.Entry<String, String> bodyEntry : bodyEntries) {
            requestBodyEntries.add(bodyEntry.getKey() + "=" + MSALUtils.urlEncode(bodyEntry.getValue()));
        }

        final String requestMessage = requestBodyEntries.isEmpty() ? "" : MSALUtils.convertSetToString(requestBodyEntries, "&");
        return requestMessage.getBytes(MSALUtils.ENCODING_UTF8);
    }

    private void verifyCorrelationIdInResponseHeaders(final HttpResponse response) {
        final UUID correlationIdInRequest = UUID.fromString(mHeader.get(
                OauthConstants.OauthHeader.CORRELATION_ID));

        final Map<String, List<String>> responseHeader = response.getHeaders();
        if (responseHeader == null
                || !responseHeader.containsKey(OauthConstants.OauthHeader.CORRELATION_ID_IN_RESPONSE)) {
            // TODO: Logger.w(TAG, "response doesn't contain headers or header doesn't include correlation id");
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
}
