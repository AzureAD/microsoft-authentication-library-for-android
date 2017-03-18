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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DRSMetadataRequestorTest {

    private static final RequestContext REQUEST_CONTEXT = new RequestContext(UUID.randomUUID(), "");

    static final String RESPONSE = "{\n"
            +
            "  \"DeviceRegistrationService\": {\n"
            +
            "    \"RegistrationEndpoint\": \"https://fs.lindft6.com/EnrollmentServer/DeviceEnrollmentWebService.svc\",\n"
            +
            "    \"RegistrationResourceId\": \"urn:ms-drs:UUID\",\n"
            +
            "    \"ServiceVersion\": \"1.0\"\n"
            +
            "  },\n"
            +
            "  \"AuthenticationService\": {\n"
            +
            "    \"OAuth2\": {\n"
            +
            "      \"AuthCodeEndpoint\": \"https://fs.lindft6.com/adfs/oauth2/authorize\",\n"
            +
            "      \"TokenEndpoint\": \"https://fs.lindft6.com/adfs/oauth2/token\"\n"
            +
            "    }\n"
            +
            "  },\n"
            +
            "  \"IdentityProviderService\": {\n"
            +
            "    \"PassiveAuthEndpoint\": \"https://fs.lindft6.com/adfs/ls\"\n"
            +
            "  }\n"
            +
            "}";

    private static final String TEST_ADFS = "https://fs.lindft6.com/adfs/ls";

    private static final String DOMAIN = "lindft6.com";

    @Test
    public void testRequestMetadata() throws IOException, MsalException {
        final HttpURLConnection mockedSuccessConnection = AndroidTestMockUtil
                .getMockedConnectionWithSuccessResponse(RESPONSE);
        HttpUrlConnectionFactory.addMockedConnection(mockedSuccessConnection);
        final DRSMetadataRequestor requestor = new DRSMetadataRequestor(REQUEST_CONTEXT);

        final DRSMetadata metadata = requestor.requestMetadata(DOMAIN);

        Assert.assertEquals(
                TEST_ADFS,
                metadata.getIdentityProviderService().getPassiveAuthEndpoint()
        );
    }

    @Test(expected = MsalException.class)
    public void testRequestMetadataThrows() throws IOException, MsalException {
        final HttpURLConnection mockedFailedConnection =
                AndroidTestMockUtil.getMockedConnectionWithFailureResponse(
                        HttpURLConnection.HTTP_BAD_REQUEST, "Bad Request"
                );
        HttpUrlConnectionFactory.addMockedConnection(mockedFailedConnection);
        final DRSMetadataRequestor requestor = new DRSMetadataRequestor(REQUEST_CONTEXT);
        // throws Exception (expected)
        requestor.requestMetadata(DOMAIN);
    }

    @Test
    public void testParseMetadata() throws MsalException {
        final Map<String, List<String>> mockHeaders = new HashMap<>();
        mockHeaders.put(HttpConstants.HeaderField.CONTENT_TYPE,
                Collections.singletonList(HttpConstants.MediaType.APPLICATION_JSON));

        final HttpResponse mockResponse = new HttpResponse(
                HttpURLConnection.HTTP_OK,
                RESPONSE,
                mockHeaders
        );

        final DRSMetadata metadata = new DRSMetadataRequestor(REQUEST_CONTEXT).parseMetadata(mockResponse);

        Assert.assertEquals(
                TEST_ADFS,
                metadata.getIdentityProviderService().getPassiveAuthEndpoint()
        );
    }

    @Test
    public void testBuildRequestUrlByTypeOnPrem() {
        final String expected = "https://enterpriseregistration.lindft6.com/enrollmentserver/contract?api-version=1.0";
        final DRSMetadataRequestor requestor = new DRSMetadataRequestor(REQUEST_CONTEXT);
        Assert.assertEquals(
                expected,
                requestor.buildRequestUrlByType(
                        DRSMetadataRequestor.Type.ON_PREM,
                        DOMAIN
                )
        );
    }

    @Test
    public void testBuildRequestUrlByTypeCloud() {
        final String expected = "https://enterpriseregistration.windows.net/lindft6.com/enrollmentserver/contract?api-version=1.0";
        final DRSMetadataRequestor requestor = new DRSMetadataRequestor(REQUEST_CONTEXT);
        Assert.assertEquals(
                expected,
                requestor.buildRequestUrlByType(
                        DRSMetadataRequestor.Type.CLOUD,
                        DOMAIN
                )
        );
    }
}
