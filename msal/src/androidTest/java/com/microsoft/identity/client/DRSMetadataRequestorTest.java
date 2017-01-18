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

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DRSMetadataRequestorTest {

    private static final String RESPONSE = "{\n"
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
    public void testRequestMetadata() throws IOException, AuthenticationException {
        final HttpURLConnection mockedSuccessConnection = AndroidTestMockUtil
                .getMockedConnectionWithSuccessResponse(RESPONSE);
        HttpUrlConnectionFactory.addMockedConnection(mockedSuccessConnection);
        DRSMetadataRequestor requestor = new DRSMetadataRequestor();

        DRSMetadata metadata = requestor.requestMetadata(DOMAIN);

        Assert.assertEquals(
                TEST_ADFS,
                metadata.getIdentityProviderService().getPassiveAuthEndpoint()
        );
    }

    @Test(expected = AuthenticationException.class)
    public void testRequestMetadataThrows() throws IOException, AuthenticationException {
        final HttpURLConnection mockedFailedConnection =
                AndroidTestMockUtil.getMockedConnectionWithFailureResponse(400, "Bad Request");
        HttpUrlConnectionFactory.addMockedConnection(mockedFailedConnection);
        DRSMetadataRequestor requestor = new DRSMetadataRequestor();
        // throws Exception (expected)
        requestor.requestMetadata(DOMAIN);
    }

    @Test
    public void testParseMetadata() throws AuthenticationException {
        HttpResponse mockResponse = new HttpResponse(
                200,
                RESPONSE,
                new HashMap<String, List<String>>() {{
                    put(
                            HttpConstants.HeaderField.CONTENT_TYPE,
                            Collections.singletonList(HttpConstants.MediaType.APPLICATION_JSON)
                    );
                }}
        );

        DRSMetadata metadata = new DRSMetadataRequestor().parseMetadata(mockResponse);

        Assert.assertEquals(
                TEST_ADFS,
                metadata.getIdentityProviderService().getPassiveAuthEndpoint()
        );
    }

    @Test
    public void testBuildRequestUrlByTypeOnPrem() {
        final String expected = "https://enterpriseregistration.lindft6.com/enrollmentserver/contract?api-version=1.0";
        DRSMetadataRequestor requestor = new DRSMetadataRequestor();
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
        DRSMetadataRequestor requestor = new DRSMetadataRequestor();
        Assert.assertEquals(
                expected,
                requestor.buildRequestUrlByType(
                        DRSMetadataRequestor.Type.CLOUD,
                        DOMAIN
                )
        );
    }
}
