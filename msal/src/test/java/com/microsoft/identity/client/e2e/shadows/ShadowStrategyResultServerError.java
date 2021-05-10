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

import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.internal.testutils.strategies.MockTestStrategy;

import org.robolectric.annotation.Implements;

@Implements(MockTestStrategy.class)
public class ShadowStrategyResultServerError {

    // overriding this method from MockTestStrategy class to return a error response in this case
    public HttpResponse makeHttpResponseFromResponseObject(final Object obj) {
        // create a tokenErrorResponse object
        final TokenErrorResponse errorResponse = createMockTokenErrorResponse();
        final String httpResponseBody = ObjectMapper.serializeObjectToJsonString(errorResponse);
        // create http response from error response, use 500 http code
        HttpResponse httpResponse = new HttpResponse(500, httpResponseBody, null);
        return httpResponse;
    }

    // create a mocked token error response
    private TokenErrorResponse createMockTokenErrorResponse() {
        TokenErrorResponse tokenErrorResponse = new TokenErrorResponse();
        tokenErrorResponse.setError("internal_server_error");
        tokenErrorResponse.setErrorDescription("Oops! Something went wrong :(");
        tokenErrorResponse.setStatusCode(500);
        return tokenErrorResponse;
    }
}
