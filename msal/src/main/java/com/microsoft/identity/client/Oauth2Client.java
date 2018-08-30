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
    private static final String HEADER_ACCEPT = HttpConstants.HeaderField.ACCEPT;
    private static final String HEADER_ACCEPT_VALUE = HttpConstants.MediaType.APPLICATION_JSON;

    static final String POST_CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final Map<String, String> mBodyParameters = new HashMap<>();
    private final Map<String, String> mQueryParameters = new HashMap<>();
    private final Map<String, String> mHeader = new HashMap<>(PlatformIdHelper.getPlatformIdParameters());

    private final RequestContext mRequestContext;

    Oauth2Client(final RequestContext requestContext) {
        mRequestContext = requestContext;
    }

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
     * Send post request to get token with the given authority. AuthorityMetadata will hold the token endpoint.
     */
    TokenResponse getToken(final AuthorityMetadata authority) throws IOException, MsalClientException, MsalServiceException {

        return executeHttpRequest(HttpRequest.REQUEST_METHOD_POST, authority.getTokenEndpoint(), new ParseRawJsonResponseDelegate<TokenResponse>() {
            @Override
            public TokenResponse parseSuccessRawResponse(Map<String, String> responseItems) {
                return TokenResponse.createSuccessTokenResponse(responseItems);
            }

            @Override
            public TokenResponse parseErrorRawResponse(Map<String, String> responseItems, final int statusCode) {
                return TokenResponse.createFailureTokenResponse(responseItems, statusCode);
            }
        });
    }

    /**
     * Discover the AAD instance with the given instance discovery endpoint.
     */
    InstanceDiscoveryResponse discoveryAADInstance(final URL instanceDiscoveryEndpoint) throws IOException, MsalClientException, MsalServiceException {

        return executeHttpRequest(HttpRequest.REQUEST_METHOD_GET, instanceDiscoveryEndpoint.toString(), new ParseRawJsonResponseDelegate<InstanceDiscoveryResponse>() {
            @Override
            public InstanceDiscoveryResponse parseSuccessRawResponse(Map<String, String> responseItems) {
                return InstanceDiscoveryResponse.createSuccessInstanceDiscoveryResponse(responseItems);
            }

            @Override
            public InstanceDiscoveryResponse parseErrorRawResponse(Map<String, String> responseItems, final int statusCode) {
                return new InstanceDiscoveryResponse(BaseOauth2Response.createErrorResponse(responseItems, statusCode));
            }
        });
    }

    /**
     * Perform tenant discovery to get authorize endpoint and token endpoint.
     */
    TenantDiscoveryResponse discoverEndpoints(final URL openIdConfigurationEndpoint) throws IOException, MsalClientException, MsalServiceException {

        return executeHttpRequest(HttpRequest.REQUEST_METHOD_GET, openIdConfigurationEndpoint.toString(), new ParseRawJsonResponseDelegate<TenantDiscoveryResponse>() {
            @Override
            public TenantDiscoveryResponse parseSuccessRawResponse(Map<String, String> responseItems) {
                return TenantDiscoveryResponse.createSuccessTenantDiscoveryResponse(responseItems);
            }

            @Override
            public TenantDiscoveryResponse parseErrorRawResponse(Map<String, String> responseItems, final int statusCode) {
                return new TenantDiscoveryResponse(BaseOauth2Response.createErrorResponse(responseItems, statusCode));
            }
        });
    }

    /**
     * Execute the http request.
     */
    private <T extends BaseOauth2Response> T executeHttpRequest(final String requestMethod, final String endpoint, final ParseRawJsonResponseDelegate<T> delegate)
            throws IOException, MsalServiceException, MsalClientException {
        // append query parameter to the endpoint first
        final URL endpointWithQP = new URL(MsalUtils.appendQueryParameterToUrl(endpoint, mQueryParameters));

        // add common headers
        addHeader(HEADER_ACCEPT, HEADER_ACCEPT_VALUE);
        addHeader(OauthConstants.OauthHeader.CORRELATION_ID_IN_RESPONSE, "true");

        final HttpResponse response;
        if (HttpRequest.REQUEST_METHOD_GET.equals(requestMethod)) {
            response = HttpRequest.sendGet(endpointWithQP, mHeader, mRequestContext);
        } else {
            response = HttpRequest.sendPost(endpointWithQP, mHeader,
                    buildRequestMessage(mBodyParameters), POST_CONTENT_TYPE, mRequestContext);
        }

        return parseRawResponse(response, delegate);
    }

    private <T extends BaseOauth2Response> T parseRawResponse(final HttpResponse httpResponse, final ParseRawJsonResponseDelegate<T> delegate)
            throws MsalServiceException, MsalClientException {
        // verify the correlation id in the httpResponse headers before parsing the httpResponse body.
        verifyCorrelationIdInResponseHeaders(httpResponse);

        final Map<String, String> responseItems = parseResponseItems(httpResponse);

        Logger.info(TAG, mRequestContext, "Http response status code is: " + httpResponse.getStatusCode());
        Logger.verbosePII(TAG, mRequestContext, "HttpResponse body is: " + httpResponse.getBody());

        if (httpResponse.getStatusCode() == HttpURLConnection.HTTP_OK) {
            return delegate.parseSuccessRawResponse(responseItems);
        }

        return delegate.parseErrorRawResponse(responseItems, httpResponse.getStatusCode());
    }

    private Map<String, String> parseResponseItems(final HttpResponse response) throws MsalServiceException, MsalClientException {
        if (MsalUtils.isEmpty(response.getBody())) {
            throw new MsalServiceException(MsalServiceException.SERVICE_NOT_AVAILABLE, "Empty response body", response.getStatusCode(), null);
        }

        final Map<String, String> responseItems;
        try {
            responseItems = MsalUtils.extractJsonObjectIntoMap(response.getBody());
        } catch (final JSONException e) {
            throw new MsalClientException(MsalClientException.JSON_PARSE_FAILURE, "Fail to parse JSON", e);
        }

        return responseItems;
    }

    private byte[] buildRequestMessage(final Map<String, String> bodyParameters) throws UnsupportedEncodingException {
        final Set<String> requestBodyEntries = new HashSet<>();
        final Set<Map.Entry<String, String>> bodyEntries = bodyParameters.entrySet();
        for (Map.Entry<String, String> bodyEntry : bodyEntries) {
            requestBodyEntries.add(bodyEntry.getKey() + "=" + MsalUtils.urlFormEncode(bodyEntry.getValue()));
        }

        final String requestMessage = requestBodyEntries.isEmpty() ? "" : MsalUtils.convertSetToString(requestBodyEntries, "&");
        return requestMessage.getBytes(MsalUtils.ENCODING_UTF8);
    }

    private void verifyCorrelationIdInResponseHeaders(final HttpResponse response) {
        final UUID correlationIdInRequest = UUID.fromString(mHeader.get(
                OauthConstants.OauthHeader.CORRELATION_ID));

        final Map<String, List<String>> responseHeader = response.getHeaders();
        if (responseHeader == null
                || !responseHeader.containsKey(OauthConstants.OauthHeader.CORRELATION_ID_IN_RESPONSE)) {
            Logger.warning(TAG, mRequestContext, "Returned response doesn't have correlation id in the header.");
            return;
        }

        final List<String> correlationIdsInHeader = responseHeader.get(
                OauthConstants.OauthHeader.CORRELATION_ID_IN_RESPONSE);
        if (correlationIdsInHeader == null || correlationIdsInHeader.size() == 0) {
            Logger.warning(TAG, mRequestContext, "Returned correlation id is empty.");
            return;
        }

        final String correlationIdInHeader = correlationIdsInHeader.get(0);
        if (!MsalUtils.isEmpty(correlationIdInHeader)) {
            try {
                final UUID correlationId = UUID.fromString(correlationIdInHeader);
                //CHECKSTYLE:OFF: checkstyle:EmptyBlock
                if (!correlationId.equals(correlationIdInRequest)) {
                    Logger.warning(TAG, mRequestContext, "Returned correlation is: " + correlationId + ", it doesn't match the sent in the "
                            + "request: " + correlationIdInRequest);
                }
            } catch (final IllegalArgumentException e) {
                Logger.error(TAG, mRequestContext, "Returned correlation id is not formatted correctly", e);
            }
        }
    }

    private interface ParseRawJsonResponseDelegate<T extends BaseOauth2Response> {

        /**
         * Interface method for parsing success response.
         */
        T parseSuccessRawResponse(final Map<String, String> responseItems);

        /**
         * Interface method for parsing failure response.
         */
        T parseErrorRawResponse(final Map<String, String> responseItems, final int statusCode);
    }
}
