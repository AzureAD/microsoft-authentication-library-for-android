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

    static class Builder extends Event.Builder<Builder> {

        private String mUserAgent;
        private String mHttpMethod;
        private String mQueryParams;
        private String mApiVersion;
        private String mOAuthErrorCode;
        private String mRequestIdHeader;
        private URL mHttpPath;
        private Integer mResponseCode;

        Builder userAgent(final String userAgent) {
            mUserAgent = userAgent;
            return this;
        }

        Builder httpMethod(final String httpMethod) {
            mHttpMethod = httpMethod;
            return this;
        }

        Builder queryParameters(final String queryParams) {
            mQueryParams = queryParams;
            return this;
        }

        Builder apiVersion(final String apiVersion) {
            mApiVersion = apiVersion;
            return this;
        }

        Builder oAuthErrorCode(final String oAuthErrorCode) {
            mOAuthErrorCode = oAuthErrorCode;
            return this;
        }

        Builder requestIdHeader(final String requestIdHeader) {
            mRequestIdHeader = requestIdHeader;
            return this;
        }

        Builder httpPath(final URL httpPath) {
            mHttpPath = httpPath;
            return this;
        }

        Builder responseCode(final Integer responseCode) {
            mResponseCode = responseCode;
            return this;
        }

        IHttpEvent build() {
            eventName(EventName.HTTP_EVENT);
            return new HttpEvent(this);
        }

    }

}
