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

import android.util.Pair;

import java.net.URL;

public class HttpEvent extends BaseEvent {

    HttpEvent(final String eventName) {
        setEventName(Names.HTTP_EVENT);
        add(new Pair<>(Properties.EVENT_NAME, eventName));
    }

    void setUserAgent(final String userAgent) {
        setProperty(Properties.HTTP_USER_AGENT, userAgent);
    }

    void setMethod(final String method) {
        setProperty(Properties.HTTP_METHOD, method);
    }

    void setQueryParameters(final String queryParameters) {
        setProperty(Properties.HTTP_QUERY_PARAMETERS, queryParameters);
    }

    void setResponseCode(final int responseCode) {
        setProperty(Properties.HTTP_RESPONSE_CODE, String.valueOf(responseCode));
    }

    void setApiVersion(final String apiVersion) {
        setProperty(Properties.HTTP_API_VERSION, apiVersion);
    }

    void setHttpPath(final URL httpPath) {
        final String authority = httpPath.getAuthority();

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
        setProperty(Properties.HTTP_PATH, logPath.toString());
    }

    void setOauthErrorCode(final String errorCode) {
        setProperty(Properties.OAUTH_ERROR_CODE, errorCode);
    }

    void setRequestIdHeader(final String requestIdHeader) {
        setProperty(Properties.REQUEST_ID_HEADER, requestIdHeader);
    }

}
