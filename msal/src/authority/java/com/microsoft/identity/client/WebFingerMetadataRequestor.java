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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

class WebFingerMetadataRequestor
        extends AbstractMetadataRequestor<WebFingerMetadata, WebFingerMetadataRequestParameters> {

    /**
     * Tag used for logging.
     */
    private static final String TAG = WebFingerMetadataRequestor.class.getSimpleName();

    WebFingerMetadataRequestor(final RequestContext requestContext) {
        super(requestContext);
    }

    @Override
    WebFingerMetadata requestMetadata(final WebFingerMetadataRequestParameters webFingerMetadataRequestParameters)
            throws MsalServiceException, MsalClientException {
        final URL domain = webFingerMetadataRequestParameters.getDomain();
        final DrsMetadata drsMetadata = webFingerMetadataRequestParameters.getDrsMetadata();
        Logger.verbosePII(TAG, getRequestContext(), "Validating authority for auth endpoint: " + domain.toString());
        try {
            // create the URL
            URL webFingerUrl = buildWebFingerUrl(domain, drsMetadata);

            // make the request
            final HttpResponse webResponse = HttpRequest.sendGet(webFingerUrl, Collections.EMPTY_MAP, getRequestContext());

            // get the status code
            final int statusCode = webResponse.getStatusCode();

            if (HttpURLConnection.HTTP_OK != statusCode) { // check 200 OK
                // non-200 codes mean not valid/trusted
                // TODO: will error code returned from web finger request? if so we should parse the response
                throw new MsalServiceException(MsalServiceException.SERVICE_NOT_AVAILABLE, webResponse.getBody(), webResponse.getStatusCode(), null);
            }

            // parse the response
            return parseMetadata(webResponse);
        } catch (final IOException e) {
            throw new MsalClientException(MsalClientException.IO_ERROR, "Received io exception: " + e.getMessage(), e);
        }
    }

    @Override
    WebFingerMetadata parseMetadata(final HttpResponse response) throws MsalClientException {
        // Initialize the metadata container
        final WebFingerMetadata webFingerMetadata = new WebFingerMetadata();

        final String responseBody = response.getBody();

        try {
            // Grab the response json
            final JSONObject responseJson = new JSONObject(responseBody);

            // Parse-out the subject
            webFingerMetadata.setSubject(
                    responseJson.getString(WebFingerMetadata.JSON_KEY_SUBJECT)
            );

            // Grab the array of links
            final JSONArray jsonLinkArr = responseJson
                    .getJSONArray(WebFingerMetadata.JSON_KEY_LINKS);

            for (int ii = 0; ii < jsonLinkArr.length(); ii++) {
                // Grab the JSONObject
                JSONObject jsonLink = jsonLinkArr.getJSONObject(ii);

                // Init a native container for it
                Link linkElement = new Link();

                // Set the rel field
                linkElement.setRel(jsonLink.getString(Link.JSON_KEY_REL));

                // set the href field
                linkElement.setHref(jsonLink.getString(Link.JSON_KEY_HREF));

                // Add this element to native container
                webFingerMetadata.getLinks().add(linkElement);
            }
        } catch (final JSONException e) {
            throw new MsalClientException(MsalClientException.JSON_PARSE_FAILURE);
        }

        return webFingerMetadata;
    }

    /**
     * Create the URL used to retrieve the WebFinger metadata.
     *
     * @param resource    the resource to verify
     * @param drsMetadata the {@link DrsMetadata} to consult
     * @return the URL of the WebFinger document
     * @throws MalformedURLException if the URL could not be constructed
     */
    static URL buildWebFingerUrl(final URL resource, final DrsMetadata drsMetadata)
            throws MalformedURLException {
        final URL passiveAuthEndpoint = new URL(
                drsMetadata
                        .getIdentityProviderService()
                        .getPassiveAuthEndpoint()
        );

        // build the url
        final StringBuilder webFingerUrlBuilder =
                new StringBuilder("https://")
                        .append(passiveAuthEndpoint.getHost())
                        .append("/.well-known/webfinger?resource=")
                        .append(resource.toString());

        final String webFingerUrl = webFingerUrlBuilder.toString();

        return new URL(webFingerUrl);
    }
}
