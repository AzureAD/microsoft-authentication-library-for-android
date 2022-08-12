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
package com.microsoft.identity.client.e2e.tests.mocked;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.e2e.shadows.ShadowMockAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowOpenIdProviderConfigurationClient;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.common.internal.controllers.CommandDispatcherHelper;
import com.microsoft.identity.common.java.eststelemetry.PublicApiId;
import com.microsoft.identity.common.java.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.java.eststelemetry.SchemaConstants;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.http.HttpRequestInterceptor;
import com.microsoft.identity.http.HttpRequestMatcher;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.mocks.MockServerResponse;
import com.microsoft.identity.shadow.ShadowHttpClient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static com.microsoft.identity.internal.testutils.TestConstants.Authorities.AAD_MOCK_AUTHORITY_HTTP_RESPONSE;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_MOCK_TEST_CONFIG_FILE_PATH;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAndroidSdkStorageEncryptionManager.class,
        ShadowMockAuthority.class,
        ShadowPublicClientApplicationConfiguration.class,
        ShadowHttpClient.class,
        ShadowOpenIdProviderConfigurationClient.class
})
public class AcquireTokenMockedTelemetryTest extends AcquireTokenAbstractTest {

    private static Map<String, String> sTelemetryHeaders;
    private static List<String> sCorrelationIdList = new ArrayList<>();
    private final HttpRequestMatcher postRequestMatcher = HttpRequestMatcher.builder()
            .isPOST()
            .build();

    @Override
    public String[] getScopes() {
        return TestConstants.Scopes.USER_READ_SCOPE;
    }

    @Override
    public String getAuthority() {
        return AAD_MOCK_AUTHORITY_HTTP_RESPONSE;
    }

    @Override
    public String getConfigFilePath() {
        return MULTIPLE_ACCOUNT_MODE_MOCK_TEST_CONFIG_FILE_PATH;
    }

    public static void setTelemetryHeaders(final Map<String, String> headers) {
        sTelemetryHeaders = headers;
    }

    public static void addCorrelationId(final String correlationId) {
        sCorrelationIdList.add(correlationId);
    }

    private void mockWithResponse(final HttpResponse httpResponse) {
        mockHttpClient.intercept(postRequestMatcher,
                new HttpRequestInterceptor() {
                    @Override
                    public HttpResponse performIntercept(
                            @NonNull HttpClient.HttpMethod httpMethod,
                            @NonNull URL requestUrl,
                            @NonNull Map<String, String> requestHeaders,
                            @Nullable byte[] requestContent) throws IOException {
                        final String correlationId = requestHeaders.get("client-request-id");

                        AcquireTokenMockedTelemetryTest.addCorrelationId(correlationId);

                        AcquireTokenMockedTelemetryTest.setTelemetryHeaders(requestHeaders);

                        return httpResponse;
                    }
                });
    }

    @Before
    public void setup() {
        sTelemetryHeaders = null;
        sCorrelationIdList.clear();
        EstsTelemetry.getInstance().clear();
        super.setup();
    }

    @Test
    public void testServerSideTelemetry() {
        final String username = "fake@test.com";

        CommandDispatcherHelper.clear();

        // successful interactive request
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        int networkRequestIndex = 0;

        mockWithResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireToken(parameters);
        flushScheduler();

        // assert telem
        String expectedCurrent = "2|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + ",0|,,,,,,,,,,,";
        String expectedLast = "2|0|||1";
        assertTelemetry(expectedCurrent, expectedLast);

        CommandDispatcherHelper.clear();

        // successful silent request - served from cache
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                .build();

        mockWithResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();

        CommandDispatcherHelper.clear();

        // successful silent request - served from cache
        mockWithResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();

        CommandDispatcherHelper.clear();

        // failed silent request - goes to token endpoint - invalid grant
        final AcquireTokenSilentParameters silentParametersForceRefreshInvalidGrant = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.failureSilentCallback("invalid_grant"))
                .build();

        networkRequestIndex++;

        mockWithResponse(MockServerResponse.getMockTokenFailureInvalidGrantResponse());
        mApplication.acquireTokenSilentAsync(silentParametersForceRefreshInvalidGrant);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + ",1|,,,,,,1,1,1,1,0,1";
        expectedLast = "2|2|||1";
        assertTelemetry(expectedCurrent, expectedLast);

        CommandDispatcherHelper.clear();

        // failed silent request - goes to token endpoint - invalid scope
        final AcquireTokenSilentParameters silentParametersForceRefreshInvalidScope = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.failureSilentCallback("invalid_scope"))
                .build();

        networkRequestIndex++;

        mockWithResponse(MockServerResponse.getMockTokenFailureInvalidScopeResponse());
        mApplication.acquireTokenSilentAsync(silentParametersForceRefreshInvalidScope);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + ",1|,,,,,,1,1,1,1,0,1";
        expectedLast = "2|0|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + "," +
                sCorrelationIdList.get(networkRequestIndex - 1) + "|invalid_grant|1";
        assertTelemetry(expectedCurrent, expectedLast);

        CommandDispatcherHelper.clear();

        // failure interactive request - service unavailable
        final AcquireTokenParameters parametersServiceUnavailable = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.failureInteractiveCallback("service_unavailable"))
                .build();

        networkRequestIndex++;

        mockWithResponse(MockServerResponse.getMockTokenFailureServiceUnavailable());
        mApplication.acquireToken(parametersServiceUnavailable);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + ",0|,,,,,,,,,,,";
        expectedLast = "2|0|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + "," +
                sCorrelationIdList.get(networkRequestIndex - 1) + "|invalid_scope|1";
        assertTelemetry(expectedCurrent, expectedLast);

        CommandDispatcherHelper.clear();

        // successful interactive request

        networkRequestIndex++;

        mockWithResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireToken(parameters);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + ",0|,,,,,,,,,,,";
        expectedLast = "2|0|" +
                PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + "," + sCorrelationIdList.get(networkRequestIndex - 2) +
                "," + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + "," + sCorrelationIdList.get(networkRequestIndex - 1) +
                "|invalid_scope,service_unavailable|1";
        assertTelemetry(expectedCurrent, expectedLast);

        CommandDispatcherHelper.clear();

        // successful silent request - goes to token endpoint

        networkRequestIndex++;

        final AcquireTokenSilentParameters silentParametersForceRefreshSuccessful = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                .build();

        mockWithResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireTokenSilentAsync(silentParametersForceRefreshSuccessful);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + ",1|,,,,,,1,1,1,1,0,1";
        expectedLast = "2|0|||1";
        assertTelemetry(expectedCurrent, expectedLast);

        CommandDispatcherHelper.clear();

        // failed interactive request - service unavailable
        for (int i = 0; i < 50; i++) {
            networkRequestIndex++;
            mockWithResponse(MockServerResponse.getMockTokenFailureServiceUnavailable());
            mApplication.acquireToken(parametersServiceUnavailable);
            flushScheduler();
            CommandDispatcherHelper.clear();
        }

        String actualLastHeader = sTelemetryHeaders.get(SchemaConstants.LAST_REQUEST_HEADER_NAME);
        // Errors are accumulating in headers as not being logged by sts
        Assert.assertTrue(actualLastHeader.length() > 2500 && actualLastHeader.length() < 3000);

        // failed interactive request - service unavailable - do another 50 requests
        for (int i = 0; i < 50; i++) {
            networkRequestIndex++;
            mockWithResponse(MockServerResponse.getMockTokenFailureServiceUnavailable());
            mApplication.acquireToken(parametersServiceUnavailable);
            flushScheduler();
            CommandDispatcherHelper.clear();
        }

        actualLastHeader = sTelemetryHeaders.get(SchemaConstants.LAST_REQUEST_HEADER_NAME);
        // Errors are accumulating in headers as not being logged by sts
        // making sure it doesn't go over 4KB limit
        Assert.assertTrue(actualLastHeader.length() > 3000 && actualLastHeader.length() < 4096);

        // do successful interactive request
        mockWithResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireToken(parameters);
        flushScheduler();

        // most of the data should now be sent
        actualLastHeader = sTelemetryHeaders.get(SchemaConstants.LAST_REQUEST_HEADER_NAME);
        Assert.assertTrue(actualLastHeader.length() > 3000 && actualLastHeader.length() < 4096);

        final String lastParts[] = actualLastHeader.split("|");
        Assert.assertEquals("0", lastParts[lastParts.length - 1]);

        CommandDispatcherHelper.clear();

        // do another successful interactive request
        mockWithResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireToken(parameters);
        flushScheduler();

        // remaining data should now be sent
        actualLastHeader = sTelemetryHeaders.get(SchemaConstants.LAST_REQUEST_HEADER_NAME);
        Assert.assertTrue(actualLastHeader.length() > 1000 && actualLastHeader.length() < 3000);

        CommandDispatcherHelper.clear();

        // do another successful interactive request
        mockWithResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireToken(parameters);
        flushScheduler();

        // all data should now be sent
        // assert telem
        expectedCurrent = "2|" + PublicApiId.MULTIPLE_ACCOUNT_PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + ",0|,,,,,,,,,,,";
        expectedLast = "2|0|||1";
        assertTelemetry(expectedCurrent, expectedLast);

    }

    private void assertTelemetry(final String expectedCurrent, final String expectedLast) {
        final String currentHeader = sTelemetryHeaders.get(SchemaConstants.CURRENT_REQUEST_HEADER_NAME);
        final String lastHeader = sTelemetryHeaders.get(SchemaConstants.LAST_REQUEST_HEADER_NAME);

        Assert.assertEquals(expectedCurrent, currentHeader);
        Assert.assertEquals(expectedLast, lastHeader);
    }
}
