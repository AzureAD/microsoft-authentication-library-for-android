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

package com.microsoft.identity.client.internal.telemetry;

import com.microsoft.identity.client.internal.MsalUtils;

import java.net.URL;

import static com.microsoft.identity.client.internal.telemetry.EventConstants.EventProperty;

/**
 * Internal class for HttpEvent telemetry data.
 */
public final class HttpEvent extends Event {

    private HttpEvent(Builder builder) {
        super(builder);
        setProperty(EventProperty.HTTP_USER_AGENT, builder.mUserAgent);
        setProperty(EventProperty.HTTP_METHOD, builder.mHttpMethod);
        setProperty(EventProperty.HTTP_QUERY_PARAMETERS, builder.mQueryParams);
        setProperty(EventProperty.HTTP_API_VERSION, builder.mApiVersion);
        setProperty(EventProperty.OAUTH_ERROR_CODE, builder.mOAuthErrorCode);
        setProperty(EventProperty.REQUEST_ID_HEADER, builder.mRequestIdHeader);
        if (null != builder.mHttpPath) {
            setHttpPath(builder.mHttpPath);
        }
        setProperty(EventProperty.HTTP_RESPONSE_CODE, String.valueOf(builder.mResponseCode));
    }

    /**
     * Convenience method for {@link HttpEvent#sanitizeUrlForTelemetry}.
     *
     * @param url the {@link URL} to sanitize.
     * @return the sanitized URL.
     */
    public static String sanitizeUrlForTelemetry(final String url) {
        final URL urlToSanitize = MsalUtils.getUrl(url);
        return urlToSanitize == null ? null : sanitizeUrlForTelemetry(urlToSanitize);
    }

    /**
     * Sanitizes {@link URL} of tenant identifiers. B2C authorities are treated as null.
     *
     * @param url the URL to sanitize.
     * @return the sanitized URL.
     */
    public static String sanitizeUrlForTelemetry(final URL url) {
        final String authority = url.getAuthority();
        final String[] splitArray = url.getPath().split("/");

        final StringBuilder logPath = new StringBuilder();
        logPath.append(url.getProtocol())
                .append("://")
                .append(authority)
                .append('/');

        // we do not want to send tenant information
        // index 0 is blank
        // index 1 is tenant
        for (int i = 2; i < splitArray.length; i++) {
            logPath.append(splitArray[i]);
            logPath.append('/');
        }

        return logPath.toString();
    }

    private void setHttpPath(final URL httpPath) {
        setProperty(EventProperty.HTTP_PATH, sanitizeUrlForTelemetry(httpPath));
    }

    public String getUserAgent() {
        return getProperty(EventProperty.HTTP_USER_AGENT);
    }

    public String getHttpMethod() {
        return getProperty(EventProperty.HTTP_METHOD);
    }

    public String getQueryParameters() {
        return getProperty(EventProperty.HTTP_QUERY_PARAMETERS);
    }

    public String getApiVersion() {
        return getProperty(EventProperty.HTTP_API_VERSION);
    }

    public String getOAuthErrorCode() {
        return getProperty(EventProperty.OAUTH_ERROR_CODE);
    }

    public String getRequestIdHeader() {
        return getProperty(EventProperty.REQUEST_ID_HEADER);
    }

    public URL getHttpPath() {
        return MsalUtils.getUrl(getProperty(EventProperty.HTTP_PATH));
    }

    public Integer getResponseCode() {
        return Integer.valueOf(getProperty(EventProperty.HTTP_RESPONSE_CODE));
    }

    /**
     * Builder object for HttpEvents.
     */
    public static class Builder extends Event.Builder<Builder> {

        private String mUserAgent;
        private String mHttpMethod;
        private String mQueryParams;
        private String mApiVersion;
        private String mOAuthErrorCode;
        private String mRequestIdHeader;
        private URL mHttpPath;
        private Integer mResponseCode;

        public Builder() {
            super(EventConstants.EventName.HTTP_EVENT);
        }

        /**
         * Sets the userAgent.
         *
         * @param userAgent the userAgent to set.
         * @return the Builder instance.
         */
        public Builder setUserAgent(final String userAgent) {
            mUserAgent = userAgent;
            return this;
        }

        /**
         * Sets the http method.
         *
         * @param httpMethod the http method to set.
         * @return the Builder instance.
         */
        public Builder setHttpMethod(final String httpMethod) {
            mHttpMethod = httpMethod;
            return this;
        }

        /**
         * Sets the query parameters.
         *
         * @param queryParams the query parameters to set.
         * @return the Builder instance.
         */
        public Builder setQueryParameters(final String queryParams) {
            mQueryParams = queryParams;
            return this;
        }

        /**
         * Sets the api version.
         *
         * @param apiVersion the api version to set.
         * @return the Builder instance.
         */
        public Builder setApiVersion(final String apiVersion) {
            mApiVersion = apiVersion;
            return this;
        }

        /**
         * Sets the OAuthErrorCode.
         *
         * @param oAuthErrorCode the OAuthErrorCode to set.
         * @return the Builder instance.
         */
        public Builder setOAuthErrorCode(final String oAuthErrorCode) {
            mOAuthErrorCode = oAuthErrorCode;
            return this;
        }

        /**
         * Sets the requestId header.
         *
         * @param requestIdHeader the header to set.
         * @return the Builder instance.
         */
        public Builder setRequestIdHeader(final String requestIdHeader) {
            mRequestIdHeader = requestIdHeader;
            return this;
        }

        /**
         * Sets the http path.
         *
         * @param httpPath the path to set.
         * @return the Builder instance.
         */
        public Builder setHttpPath(final URL httpPath) {
            mHttpPath = httpPath;
            return this;
        }

        /**
         * Sets the response code of this request.
         *
         * @param responseCode the resonse code to set.
         * @return the Builder instance.
         */
        public Builder setStatusCode(final Integer responseCode) {
            mResponseCode = responseCode;
            return this;
        }

        /**
         * Constructs a new HttpEvent.
         *
         * @return the newly constucted HttpEvent instance
         */
        @Override
        public HttpEvent build() {
            return new HttpEvent(this);
        }

    }
}
