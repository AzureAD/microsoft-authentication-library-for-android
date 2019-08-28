package com.microsoft.identity.client.robolectric.shadows;

import com.microsoft.identity.common.internal.net.HttpRequest;
import com.microsoft.identity.common.internal.net.HttpResponse;

import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

@Implements(HttpRequest.class)
public class ShadowHttpRequest {

    // mocking this to avoid accidentally sending malformed requests to the server
    public static HttpResponse sendPost(final URL requestUrl, final Map<String, String> requestHeaders,
                                        final byte[] requestContent, final String requestContentType)
            throws IOException {

        throw new IOException("Sending requests to server has been disabled for mocked unit tests");
    }
}
