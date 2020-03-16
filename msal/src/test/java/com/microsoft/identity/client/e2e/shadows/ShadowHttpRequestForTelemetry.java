package com.microsoft.identity.client.e2e.shadows;

import com.microsoft.identity.client.e2e.tests.mocked.AcquireTokenMockedTelemetryTest;
import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.HttpResponse;
import com.microsoft.identity.internal.testutils.MockHttpResponse;

import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

@Implements(HttpRequest.class)
public class ShadowHttpRequestForTelemetry {

    // mocking this to avoid accidentally sending malformed requests to the server
    public static HttpResponse sendPost(final URL requestUrl, final Map<String, String> requestHeaders,
                                        final byte[] requestContent, final String requestContentType)
            throws IOException {

        final String correlationId = requestHeaders.get("client-request-id");

        AcquireTokenMockedTelemetryTest.addCorrelationId(correlationId);

        AcquireTokenMockedTelemetryTest.setTelemetryHeaders(requestHeaders);

        return MockHttpResponse.getHttpResponse();
    }
}
