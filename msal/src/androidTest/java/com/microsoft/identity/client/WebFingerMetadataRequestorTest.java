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
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class WebFingerMetadataRequestorTest {

    static final String RESPONSE = "{\n"
            +
            "  \"subject\": \"https://fs.lindft6.com\",\n"
            +
            "  \"links\": [\n"
            +
            "    {\n"
            +
            "      \"rel\": \"http://schemas.microsoft.com/rel/trusted-realm\",\n"
            +
            "      \"href\": \"https://fs.lindft6.com\"\n"
            +
            "    }\n"
            +
            "  ]\n"
            +
            "}";

    private static final String DOMAIN = "https://fs.lindft6.com";

    private static final RequestContext REQUEST_CONTEXT = new RequestContext(UUID.randomUUID(), "");

    private static final DRSMetadata DRS_METADATA = new DRSMetadata();

    static {
        IdentityProviderService identityProviderService = new IdentityProviderService();
        identityProviderService.setPassiveAuthEndpoint(DOMAIN + "/adfs/ls");
        DRS_METADATA.setIdentityProviderService(identityProviderService);
    }

    @Test
    public void testRequestMetadata() throws IOException, AuthenticationException {
        final HttpURLConnection mockedSuccessfulConnection = AndroidTestMockUtil
                .getMockedConnectionWithSuccessResponse(RESPONSE);
        HttpUrlConnectionFactory.addMockedConnection(mockedSuccessfulConnection);

        WebFingerMetadataRequestor requestor = new WebFingerMetadataRequestor(REQUEST_CONTEXT);

        WebFingerMetadataRequestParameters parameters = new WebFingerMetadataRequestParameters(
                new URL(DOMAIN),
                DRS_METADATA
        );

        WebFingerMetadata metadata = requestor.requestMetadata(parameters);

        Assert.assertEquals("https://fs.lindft6.com", metadata.getSubject());
        Assert.assertNotNull(metadata.getLinks());
        Assert.assertEquals(1, metadata.getLinks().size());
        Assert.assertEquals(
                "http://schemas.microsoft.com/rel/trusted-realm",
                metadata.getLinks().get(0).getRel()
        );
        Assert.assertEquals(
                "https://fs.lindft6.com",
                metadata.getLinks().get(0).getHref()
        );
    }

    @Test
    public void testRequestMetadataThrows() throws IOException, AuthenticationException {
        final HttpURLConnection mockedFailedConnection = AndroidTestMockUtil
                .getMockedConnectionWithFailureResponse(HttpURLConnection.HTTP_BAD_REQUEST, RESPONSE);
        HttpUrlConnectionFactory.addMockedConnection(mockedFailedConnection);

        WebFingerMetadataRequestor requestor = new WebFingerMetadataRequestor(REQUEST_CONTEXT);

        WebFingerMetadataRequestParameters parameters = new WebFingerMetadataRequestParameters(
                new URL(DOMAIN),
                DRS_METADATA
        );

        try {
            WebFingerMetadata metadata = requestor.requestMetadata(parameters);
        } catch (AuthenticationException e) {
            // should throw
            return;
        }
    }

    @Test
    public void testParseMetadata() throws AuthenticationException {
        final Map<String, List<String>> mockHeaders = new HashMap<>();
        mockHeaders.put(HttpConstants.HeaderField.CONTENT_TYPE,
                Collections.singletonList(HttpConstants.MediaType.APPLICATION_JSON));

        HttpResponse response = new HttpResponse(
                HttpURLConnection.HTTP_OK,
                RESPONSE,
                mockHeaders
        );

        WebFingerMetadata metadata = new WebFingerMetadataRequestor(REQUEST_CONTEXT).parseMetadata(response);

        Assert.assertEquals("https://fs.lindft6.com", metadata.getSubject());
        Assert.assertNotNull(metadata.getLinks());
        Assert.assertEquals(1, metadata.getLinks().size());
        Assert.assertEquals(
                "http://schemas.microsoft.com/rel/trusted-realm",
                metadata.getLinks().get(0).getRel()
        );
        Assert.assertEquals(
                "https://fs.lindft6.com",
                metadata.getLinks().get(0).getHref()
        );
    }

    @Test
    public void testBuildWebFingerUrl() throws MalformedURLException {
        final URL expected = new URL("https://fs.lindft6.com/.well-known/webfinger?resource=https://fs.lindft6.com");
        final URL wfURL = WebFingerMetadataRequestor.buildWebFingerUrl(new URL(DOMAIN), DRS_METADATA);
        Assert.assertEquals(expected, wfURL);
    }

}
