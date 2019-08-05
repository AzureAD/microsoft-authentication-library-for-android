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

import androidx.test.runner.AndroidJUnit4;

import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.client.internal.telemetry.EventConstants;
import com.microsoft.identity.client.internal.telemetry.HttpEvent;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

@RunWith(AndroidJUnit4.class)
public class HttpEventTest {

    static final String TEST_USER_AGENT = "a_user_agent";
    static final String TEST_HTTP_METHOD = "GET";
    static final String TEST_QUERY_PARAMS = "?value=1&name=foo";
    static final String TEST_API_VERSION = "v1.0";
    static final String TEST_O_AUTH_ERROR_CODE = "invalid grant";
    static final String TEST_REQUEST_ID_HEADER = "a_request_header";
    static final URL TEST_HTTP_PATH = MsalUtils.getUrl("https://login.microsoftonline.com/");
    static final Integer TEST_HTTP_RESPONSE_CODE = 200;

    static HttpEvent getTestHttpEvent() {
        return getTestHttpEventBuilder(TEST_HTTP_PATH).build();
    }

    static HttpEvent.Builder getTestHttpEventBuilder() {
        return getTestHttpEventBuilder(TEST_HTTP_PATH);
    }

    private static HttpEvent getTestHttpEvent(final URL httpPath) {
        return getTestHttpEventBuilder(httpPath).build();
    }

    static HttpEvent.Builder getTestHttpEventBuilder(final URL httpPath) {
        return new HttpEvent.Builder()
                .setUserAgent(TEST_USER_AGENT)
                .setHttpMethod(TEST_HTTP_METHOD)
                .setQueryParameters(TEST_QUERY_PARAMS)
                .setApiVersion(TEST_API_VERSION)
                .setOAuthErrorCode(TEST_O_AUTH_ERROR_CODE)
                .setRequestIdHeader(TEST_REQUEST_ID_HEADER)
                .setHttpPath(httpPath)
                .setStatusCode(TEST_HTTP_RESPONSE_CODE);
    }

    @Test
    public void testHttpEventInitializes() {
        final HttpEvent httpEvent = getTestHttpEvent();
        Assert.assertEquals(EventConstants.EventName.HTTP_EVENT, httpEvent.getEventName());
        Assert.assertEquals(TEST_USER_AGENT, httpEvent.getUserAgent());
        Assert.assertEquals(TEST_HTTP_METHOD, httpEvent.getHttpMethod());
        Assert.assertEquals(TEST_QUERY_PARAMS, httpEvent.getQueryParameters());
        Assert.assertEquals(TEST_API_VERSION, httpEvent.getApiVersion());
        Assert.assertEquals(TEST_O_AUTH_ERROR_CODE, httpEvent.getOAuthErrorCode());
        Assert.assertEquals(TEST_REQUEST_ID_HEADER, httpEvent.getRequestIdHeader());
        Assert.assertEquals(TEST_HTTP_PATH, httpEvent.getHttpPath());
        Assert.assertEquals(TEST_HTTP_RESPONSE_CODE, httpEvent.getResponseCode());
    }

}
