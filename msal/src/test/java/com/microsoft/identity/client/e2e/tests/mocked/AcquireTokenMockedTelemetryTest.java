package com.microsoft.identity.client.e2e.tests.mocked;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.e2e.shadows.ShadowHttpRequestForTelemetry;
import com.microsoft.identity.client.e2e.shadows.ShadowMockAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.common.internal.eststelemetry.PublicApiId;
import com.microsoft.identity.common.internal.eststelemetry.SchemaConstants;
import com.microsoft.identity.internal.testutils.MockHttpResponse;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.mocks.MockServerResponse;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static com.microsoft.identity.internal.testutils.TestConstants.Authorities.AAD_MOCK_AUTHORITY_HTTP_RESPONSE;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_MOCK_TEST_CONFIG_FILE_PATH;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowMockAuthority.class, ShadowHttpRequestForTelemetry.class, ShadowMsalUtils.class})
public class AcquireTokenMockedTelemetryTest extends AcquireTokenAbstractTest {

    private static Map<String, String> sTelemetryHeaders;
    private static List<String> sCorrelationIdList = new ArrayList<>();

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

    @Test
    public void testAcquireTokenSuccess() {
        final String username = "fake@test.com";

        // successful interactive request
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        int networkRequestIndex = 0;

        MockHttpResponse.setHttpResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireToken(parameters);
        flushScheduler();

        // assert telem
        String expectedCurrent = "2|" + PublicApiId.PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + ",0|,,,,,";
        String expectedLast = "2|0|||";
        assertTelemetry(expectedCurrent, expectedLast);

        // successful silent request - served from cache
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                .build();

        MockHttpResponse.setHttpResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();


        // successful silent request - served from cache
        MockHttpResponse.setHttpResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();


        // failed silent request - goes to token endpoint - invalid grant
        final AcquireTokenSilentParameters silentParametersForceRefreshInvalidGrant = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.failureSilentCallback("invalid_grant"))
                .build();

        networkRequestIndex++;

        MockHttpResponse.setHttpResponse(MockServerResponse.getMockTokenFailureInvalidGrantResponse());
        mApplication.acquireTokenSilentAsync(silentParametersForceRefreshInvalidGrant);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + ",1|1,1,1,1,0,1";
        expectedLast = "2|2|||";
        assertTelemetry(expectedCurrent, expectedLast);

        // failed silent request - goes to token endpoint - invalid scope
        final AcquireTokenSilentParameters silentParametersForceRefreshInvalidScope = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.failureSilentCallback("invalid_scope"))
                .build();

        networkRequestIndex++;

        MockHttpResponse.setHttpResponse(MockServerResponse.getMockTokenFailureInvalidScopeResponse());
        mApplication.acquireTokenSilentAsync(silentParametersForceRefreshInvalidScope);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + ",1|1,1,1,1,0,1";
        expectedLast = "2|0|" + PublicApiId.PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS +"," +
                sCorrelationIdList.get(networkRequestIndex - 1) + "|invalid_grant|";
        assertTelemetry(expectedCurrent, expectedLast);

        // failure interactive request - service unavailable
        final AcquireTokenParameters parametersServiceUnavailable = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.failureInteractiveCallback("service_unavailable"))
                .build();

        networkRequestIndex++;

        MockHttpResponse.setHttpResponse(MockServerResponse.getMockTokenFailureServiceUnavailable());
        mApplication.acquireToken(parametersServiceUnavailable);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + ",0|,,,,,";
        expectedLast = "2|0|" + PublicApiId.PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS +"," +
                sCorrelationIdList.get(networkRequestIndex - 1) + "|invalid_scope|";
        assertTelemetry(expectedCurrent, expectedLast);

        // successful interactive request

        networkRequestIndex++;

        MockHttpResponse.setHttpResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireToken(parameters);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + ",0|,,,,,";
        expectedLast = "2|0|" +
                PublicApiId.PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS +"," + sCorrelationIdList.get(networkRequestIndex - 2) +
                "," + PublicApiId.PCA_ACQUIRE_TOKEN_WITH_PARAMETERS + "," + sCorrelationIdList.get(networkRequestIndex - 1) +
                "|invalid_scope,service_unavailable|";
        assertTelemetry(expectedCurrent, expectedLast);

        // successful silent request - goes to token endpoint

        networkRequestIndex++;

        final AcquireTokenSilentParameters silentParametersForceRefreshSuccessful = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .fromAuthority(getAuthority())
                .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                .build();

        MockHttpResponse.setHttpResponse(MockServerResponse.getMockTokenSuccessResponse());
        mApplication.acquireTokenSilentAsync(silentParametersForceRefreshSuccessful);
        flushScheduler();

        // assert telem
        expectedCurrent = "2|" + PublicApiId.PCA_ACQUIRE_TOKEN_SILENT_ASYNC_WITH_PARAMETERS + ",1|1,1,1,1,0,1";
        expectedLast = "2|0|||";
        assertTelemetry(expectedCurrent, expectedLast);
    }

    private void assertTelemetry(final String expectedCurrent, final String expectedLast) {
        final String currentHeader = sTelemetryHeaders.get(SchemaConstants.CURRENT_REQUEST_HEADER_NAME);
        final String lastHeader = sTelemetryHeaders.get(SchemaConstants.LAST_REQUEST_HEADER_NAME);

        Assert.assertEquals(expectedCurrent, currentHeader);
        Assert.assertEquals(expectedLast, lastHeader);
    }
}
