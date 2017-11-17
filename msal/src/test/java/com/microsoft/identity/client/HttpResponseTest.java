// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.identity.client;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link HttpResponse}.
 */
public final class HttpResponseTest {

    private static final String RESPONSE_BODY = "test response body";

    @Before
    public void setUp() {
        Logger.getInstance().setEnableLogcatLog(false);
    }

    @Test
    public void testHttpResponseWithNullBody() {
        final HttpResponse response = new HttpResponse(HttpURLConnection.HTTP_OK, null,
                Collections.<String, List<String>>emptyMap());
        Assert.assertNull(response.getBody());
        Assert.assertTrue(response.getStatusCode() == HttpURLConnection.HTTP_OK);
        Assert.assertTrue(response.getHeaders().isEmpty());
    }

    @Test
    public void testHttpResponseWithEmptyBody() {
        final HttpResponse response = new HttpResponse(HttpURLConnection.HTTP_OK, "",
                Collections.<String, List<String>>emptyMap());
        Assert.assertNotNull(response.getBody());
        Assert.assertTrue(response.getBody().isEmpty());
    }

    @Test
    public void testHttpResponseWithNullResponseHeaders() {
        final HttpResponse response = new HttpResponse(HttpURLConnection.HTTP_OK, RESPONSE_BODY, null);
        Assert.assertTrue(response.getBody().equals(RESPONSE_BODY));
        Assert.assertNull(response.getHeaders());
    }
}
