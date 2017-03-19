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

import java.net.URL;
import java.util.Arrays;

import static com.microsoft.identity.client.EventConstants.EventProperty;

/**
 * Internal class for HttpEvent telemetry data.
 */
class HttpEvent extends Event implements IHttpEvent {

    private HttpEvent(Builder builder) {
        super(builder);
        setProperty(EventProperty.HTTP_USER_AGENT, builder.mUserAgent);
        setProperty(EventProperty.HTTP_METHOD, builder.mHttpMethod);
        setProperty(EventProperty.HTTP_QUERY_PARAMETERS, builder.mQueryParams);
        setProperty(EventProperty.HTTP_API_VERSION, builder.mApiVersion);
        setProperty(EventProperty.OAUTH_ERROR_CODE, builder.mOAuthErrorCode);
        setProperty(EventProperty.REQUEST_ID_HEADER, builder.mRequestIdHeader);
        setHttpPath(builder.mHttpPath);
        setProperty(EventProperty.HTTP_RESPONSE_CODE, String.valueOf(builder.mResponseCode));
    }

    private void setHttpPath(final URL httpPath) {
        final String authority = httpPath.getAuthority();
        // only collect telemetry for well-known hosts
        if (!Arrays.asList(AADAuthority.TRUSTED_HOSTS).contains(authority)) {
            return;
        }

        final String[] splitArray = httpPath.getPath().split("/");

        final StringBuilder logPath = new StringBuilder();
        logPath.append(httpPath.getProtocol());
        logPath.append("://");
        logPath.append(authority);
        logPath.append("/");

        // we do not want to send tenant information
        // index 0 is blank
        // index 1 is tenant
        for (int i = 2; i < splitArray.length; i++) {
            logPath.append(splitArray[i]);
            logPath.append("/");
        }
        setProperty(EventProperty.HTTP_PATH, logPath.toString());
    }

    @Override
    public final String getUserAgent() {
        return getProperty(EventProperty.HTTP_USER_AGENT);
    }

    @Override
    public final String getHttpMethod() {
        return getProperty(EventProperty.HTTP_METHOD);
    }

    @Override
    public final String getQueryParameters() {
        return getProperty(EventProperty.HTTP_QUERY_PARAMETERS);
    }

    @Override
    public final String getApiVersion() {
        return getProperty(EventProperty.HTTP_API_VERSION);
    }

    @Override
    public final String getOAuthErrorCode() {
        return getProperty(EventProperty.OAUTH_ERROR_CODE);
    }

    @Override
    public final String getRequestIdHeader() {
        return getProperty(EventProperty.REQUEST_ID_HEADER);
    }

    @Override
    public final URL getHttpPath() {
        return MSALUtils.getUrl(getProperty(EventProperty.HTTP_PATH));
    }

    @Override
    public final Integer getResponseCode() {
        return Integer.valueOf(getProperty(EventProperty.HTTP_RESPONSE_CODE));
    }

    /**
     * Builder object for HttpEvents.
     */
    static class Builder extends Event.Builder<Builder> {

        private String mUserAgent;
        private String mHttpMethod;
        private String mQueryParams;
        private String mApiVersion;
        private String mOAuthErrorCode;
        private String mRequestIdHeader;
        private URL mHttpPath;
        private Integer mResponseCode;

        /**
         * Sets the userAgent.
         *
         * @param userAgent the userAgent to set.
         * @return the Builder instance.
         */
        Builder userAgent(final String userAgent) {
            mUserAgent = userAgent;
            return this;
        }

        /**
         * Sets the http method.
         *
         * @param httpMethod the http method to set.
         * @return the Builder instance.
         */
        Builder httpMethod(final String httpMethod) {
            mHttpMethod = httpMethod;
            return this;
        }

        /**
         * Sets the query parameters.
         *
         * @param queryParams the query parameters to set.
         * @return the Builder instance.
         */
        Builder queryParameters(final String queryParams) {
            mQueryParams = queryParams;
            return this;
        }

        /**
         * Sets the api version.
         *
         * @param apiVersion the api version to set.
         * @return the Builder instance.
         */
        Builder apiVersion(final String apiVersion) {
            mApiVersion = apiVersion;
            return this;
        }

        /**
         * Sets the OAuthErrorCode.
         *
         * @param oAuthErrorCode the OAuthErrorCode to set.
         * @return the Builder instance.
         */
        Builder oAuthErrorCode(final String oAuthErrorCode) {
            mOAuthErrorCode = oAuthErrorCode;
            return this;
        }

        /**
         * Sets the requestId header.
         *
         * @param requestIdHeader the header to set.
         * @return the Builder instance.
         */
        Builder requestIdHeader(final String requestIdHeader) {
            mRequestIdHeader = requestIdHeader;
            return this;
        }

        /**
         * Sets the http path.
         *
         * @param httpPath the path to set.
         * @return the Builder instance.
         */
        Builder httpPath(final URL httpPath) {
            mHttpPath = httpPath;
            return this;
        }

        /**
         * Sets the response code of this request.
         *
         * @param responseCode the resonse code to set.
         * @return the Builder instance.
         */
        Builder responseCode(final Integer responseCode) {
            mResponseCode = responseCode;
            return this;
        }

        /**
         * Constructs a new HttpEvent
         *
         * @return the newly constucted HttpEvent instance
         */
        IHttpEvent build() {
            eventName(EventName.HTTP_EVENT);
            return new HttpEvent(this);
        }

    }

}
