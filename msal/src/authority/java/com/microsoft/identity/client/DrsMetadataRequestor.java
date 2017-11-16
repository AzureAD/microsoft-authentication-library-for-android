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
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.identity.client.DRSMetadataRequestor.Type.CLOUD;
import static com.microsoft.identity.client.DRSMetadataRequestor.Type.ON_PREM;

class DRSMetadataRequestor extends AbstractMetadataRequestor<DrsMetadata, String> {

    /**
     * Tag used for logging.
     */
    private static final String TAG = DRSMetadataRequestor.class.getSimpleName();

    // DRS doc constants
    private static final String DRS_URL_PREFIX = "https://enterpriseregistration.";
    private static final String CLOUD_RESOLVER_DOMAIN = "windows.net/";

    DRSMetadataRequestor(final RequestContext requestContext) {
        super(requestContext);
    }

    /**
     * The DRS configuration.
     */
    enum Type {
        ON_PREM,
        CLOUD
    }

    @Override
    public DrsMetadata requestMetadata(final String domain) throws MsalClientException, MsalServiceException {
        try {
            return requestOnPrem(domain);
        } catch (UnknownHostException e) {
            return requestCloud(domain);
        }
    }

    /**
     * Requests DRS discovery metadata from on-prem configurations.
     *
     * @param domain the domain to validate
     * @return the DRS discovery metadata
     * @throws UnknownHostException if the on-prem enrollment server cannot be resolved
     * @throws MsalException        if there exists an enrollment/domain mismatch (lack of trust)
     */
    private DrsMetadata requestOnPrem(final String domain)
            throws UnknownHostException, MsalClientException, MsalServiceException {
        Logger.verbose(TAG, getRequestContext(), "Requesting DRS discovery (on-prem)");
        return requestDrsDiscoveryInternal(ON_PREM, domain);
    }

    /**
     * Requests DRS discovery metadata from cloud configurations.
     *
     * @param domain the domain to validate
     * @return the DRS discovery metadata
     * @throws MsalException if there exists an enrollment/domain mismatch (lack of trust)
     *                       or the trust cannot be verified
     */
    private DrsMetadata requestCloud(final String domain) throws MsalClientException, MsalServiceException {
        Logger.verbose(TAG, getRequestContext(), "Requesting DRS discovery (cloud)");
        try {
            return requestDrsDiscoveryInternal(CLOUD, domain);
        } catch (UnknownHostException e) {
            throw new MsalClientException(MsalClientException.IO_ERROR, "Cannot resolve the host. ", e);
        }
    }

    private DrsMetadata requestDrsDiscoveryInternal(final Type type, final String domain)
            throws UnknownHostException, MsalClientException, MsalServiceException {
        final URL requestURL;

        try {
            // create the request URL
            requestURL = new URL(buildRequestUrlByType(type, domain));
        } catch (MalformedURLException e) {
            // DRS metadata URL invalid
            throw new MsalClientException(MsalClientException.UNSUPPORTED_URL, "Drs metadata url is invalid", e);
        }

        // init the headers to use in the request
        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpConstants.HeaderField.ACCEPT, HttpConstants.MediaType.APPLICATION_JSON);
        if (null != getRequestContext().getCorrelationId()) {
            headers.put(
                    OauthConstants.OauthHeader.CORRELATION_ID,
                    getRequestContext().getCorrelationId().toString()
            );
        }

        final DrsMetadata metadata;
        final HttpResponse webResponse;

        // make the request
        try {
            webResponse = HttpRequest.sendGet(requestURL, headers, getRequestContext());
            final int statusCode = webResponse.getStatusCode();
            if (HttpURLConnection.HTTP_OK == statusCode) {
                metadata = parseMetadata(webResponse);
            } else {
                // unexpected status code
                // TODO: Does adfs authority validation returns the error and error description? If so, we should get a list
                // of possible erros.
                throw new MsalServiceException(MsalServiceException.SERVICE_NOT_AVAILABLE, "Unexpected error code: [" + webResponse.getBody() + "]", webResponse.getStatusCode(), null);
            }
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            throw new MsalClientException(MsalClientException.IO_ERROR, "Unexpected error", e);
        }

        return metadata;
    }

    /**
     * Construct the URL used to request the DRS metadata.
     *
     * @param type   enum indicating how the URL should be forged
     * @param domain the domain to use in the request
     * @return the DRS metadata URL to query
     */
    String buildRequestUrlByType(final Type type, final String domain) {
        // All DRS urls begin the same
        StringBuilder requestUrl = new StringBuilder(DRS_URL_PREFIX);

        if (CLOUD == type) {
            requestUrl.append(CLOUD_RESOLVER_DOMAIN).append(domain);
        } else if (ON_PREM == type) {
            requestUrl.append(domain);
        }

        requestUrl.append("/enrollmentserver/contract?api-version=1.0");

        final String requestUrlStr = requestUrl.toString();

        Logger.verbosePII(TAG, getRequestContext(), "Requestor will use DRS url: " + requestUrlStr);

        return requestUrlStr;
    }

    @Override
    DrsMetadata parseMetadata(final HttpResponse response) throws MsalClientException {
        // Initialize the response container
        final DrsMetadata drsMetadata = new DrsMetadata();
        drsMetadata.setIdentityProviderService(new IdentityProviderService());

        // Grab the response to parse
        final String responseBody = response.getBody();

        try {
            // Grab the response json
            final JSONObject responseJson = new JSONObject(responseBody);

            // Grab the provider service object field
            final JSONObject identityProviderService = responseJson
                    .getJSONObject(DrsMetadata.JSON_KEY_IDENTITY_PROVIDER_SERVICE);

            // Parse-out the passive auth endpoint
            final String passiveAuthEndpoint = identityProviderService
                    .getString(IdentityProviderService.JSON_KEY_PASSIVE_AUTH_ENDPOINT);

            // Set this value on our native object
            drsMetadata.getIdentityProviderService()
                    .setPassiveAuthEndpoint(passiveAuthEndpoint);
        } catch (final JSONException e) {
            throw new MsalClientException(MsalClientException.JSON_PARSE_FAILURE, "Failed to parse the Json response", e);
        }

        return drsMetadata;
    }
}
