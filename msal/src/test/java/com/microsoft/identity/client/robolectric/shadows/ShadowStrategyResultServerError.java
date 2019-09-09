package com.microsoft.identity.client.robolectric.shadows;

import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.common.internal.net.ObjectMapper;
import com.microsoft.identity.common.internal.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.internal.testutils.strategies.MockTestStrategy;

import org.robolectric.annotation.Implements;

@Implements(MockTestStrategy.class)
public class ShadowStrategyResultServerError {

    public HttpResponse makeHttpResponseFromResponseObject(final Object obj) {
        final TokenErrorResponse errorResponse = createFakeTokenErrorResponse();
        final String httpResponseBody = ObjectMapper.serializeObjectToJsonString(errorResponse);
        HttpResponse httpResponse = new HttpResponse(500, httpResponseBody, null);
        return httpResponse;
    }

    private TokenErrorResponse createFakeTokenErrorResponse() {
        TokenErrorResponse tokenErrorResponse = new TokenErrorResponse();
        tokenErrorResponse.setError("Internal Server Error");
        tokenErrorResponse.setErrorDescription("Oops! Something went wrong :(");
        tokenErrorResponse.setStatusCode(500);
        return tokenErrorResponse;
    }
}
