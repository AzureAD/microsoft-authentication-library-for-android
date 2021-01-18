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
package com.microsoft.identity.client.e2e.shadows;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.net.AbstractHttpClient;
import com.microsoft.identity.common.internal.net.HttpClient;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.UrlConnectionHttpClient;
import com.microsoft.identity.internal.testutils.MockHttpRequestInterceptor;
import com.microsoft.identity.internal.testutils.MockHttpResponse;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

@Implements(AbstractHttpClient.class)
public class ShadowHttpClient {

    public HttpResponse method(@NonNull HttpClient.HttpMethod httpMethod,
                               @NonNull URL requestUrl,
                               @NonNull Map<String, String> requestHeaders,
                               @Nullable byte[] requestContent) throws IOException {
        MockHttpRequestInterceptor interceptor = MockHttpResponse.intercept(httpMethod, requestUrl);
        if (interceptor == null) {
            return UrlConnectionHttpClient.getDefaultInstance().method(httpMethod, requestUrl, requestHeaders, requestContent);
        } else {
            return interceptor.method(httpMethod, requestUrl, requestHeaders, requestContent);
        }
    }

    @Implementation
    public HttpResponse put(@NonNull URL requestUrl,
                            @NonNull Map<String, String> requestHeaders,
                            @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.PUT, requestUrl, requestHeaders, requestContent);
    }

    @Implementation
    public HttpResponse patch(@NonNull URL requestUrl,
                              @NonNull Map<String, String> requestHeaders,
                              @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.PATCH, requestUrl, requestHeaders, requestContent);
    }

    @Implementation
    public HttpResponse options(@NonNull URL requestUrl,
                                @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpClient.HttpMethod.OPTIONS, requestUrl, requestHeaders, null);
    }

    @Implementation
    protected HttpResponse post(@NonNull URL requestUrl,
                                @NonNull Map<String, String> requestHeaders,
                                @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.POST, requestUrl, requestHeaders, requestContent);
    }

    @Implementation
    public HttpResponse delete(@NonNull URL requestUrl,
                               @NonNull Map<String, String> requestHeaders,
                               @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.DELETE, requestUrl, requestHeaders, requestContent);
    }

    @Implementation
    public HttpResponse get(@NonNull URL requestUrl,
                            @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpClient.HttpMethod.GET, requestUrl, requestHeaders, null);
    }

    @Implementation
    public HttpResponse head(@NonNull URL requestUrl,
                             @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpClient.HttpMethod.HEAD, requestUrl, requestHeaders, null);
    }

    @Implementation
    public HttpResponse trace(@NonNull URL requestUrl,
                              @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpClient.HttpMethod.TRACE, requestUrl, requestHeaders, null);
    }

}
