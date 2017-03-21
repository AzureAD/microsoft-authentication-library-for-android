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

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

@RunWith(AndroidJUnit4.class)
public class HttpEventTest {

    static final String sTestUserAgent = "a_user_agent";
    static final String sTestHttpMethod = "GET";
    static final String sTestQueryParams = "?value=1&name=foo";
    static final String sTestApiVersion = "v1.0";
    static final String sTestOAuthErrorCode = "invalid grant";
    static final String sTestRequestIdHeader = "a_request_header";
    static final URL sTestHttpPath = MSALUtils.getUrl("https://login.microsoftonline.com/");
    static final Integer sTestHttpResponseCode = 200;

    static IHttpEvent getTestHttpEvent(final Telemetry.RequestId requestId) {
        return getTestHttpEvent(requestId, sTestHttpPath);
    }

    private static IHttpEvent getTestHttpEvent(
            final Telemetry.RequestId requestId,
            final URL httpPath
    ) {
        return new HttpEvent.Builder(requestId)
                .userAgent(sTestUserAgent)
                .httpMethod(sTestHttpMethod)
                .queryParameters(sTestQueryParams)
                .apiVersion(sTestApiVersion)
                .oAuthErrorCode(sTestOAuthErrorCode)
                .requestIdHeader(sTestRequestIdHeader)
                .httpPath(httpPath)
                .statusCode(sTestHttpResponseCode)
                .build();
    }

    @Test
    public void testHttpEventInitializes() {
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        final IHttpEvent httpEvent = getTestHttpEvent(requestId);
        Assert.assertEquals(requestId, httpEvent.getRequestId());
        Assert.assertEquals(EventName.HTTP_EVENT, httpEvent.getEventName());
        Assert.assertEquals(sTestUserAgent, httpEvent.getUserAgent());
        Assert.assertEquals(sTestHttpMethod, httpEvent.getHttpMethod());
        Assert.assertEquals(sTestQueryParams, httpEvent.getQueryParameters());
        Assert.assertEquals(sTestApiVersion, httpEvent.getApiVersion());
        Assert.assertEquals(sTestOAuthErrorCode, httpEvent.getOAuthErrorCode());
        Assert.assertEquals(sTestRequestIdHeader, httpEvent.getRequestIdHeader());
        Assert.assertEquals(sTestHttpPath, httpEvent.getHttpPath());
        Assert.assertEquals(sTestHttpResponseCode, httpEvent.getResponseCode());
    }

    @Test
    public void testOnlyTrustedHostsAddedToEvent() {
        final Telemetry.RequestId requestId = Telemetry.generateNewRequestId();
        final IHttpEvent httpEvent =
                getTestHttpEvent(requestId, MSALUtils.getUrl("https://login.contoso.com/"));
        Assert.assertNull(httpEvent.getHttpPath());
    }
}
