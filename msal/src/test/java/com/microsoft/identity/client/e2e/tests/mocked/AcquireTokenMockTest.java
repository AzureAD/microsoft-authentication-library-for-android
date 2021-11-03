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

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushSchedulerWithDelay;
import static com.microsoft.identity.internal.testutils.TestConstants.Authorities.AAD_MOCK_AUTHORITY;

import static org.junit.Assert.fail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.RoboTestCacheHelper;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.shadows.ShadowMockAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowOpenIdProviderConfigurationClient;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowStrategyResultServerError;
import com.microsoft.identity.client.e2e.shadows.ShadowStrategyResultUnsuccessful;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.e2e.utils.ErrorCodes;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.cache.ICacheRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResponse;
import com.microsoft.identity.internal.testutils.HttpRequestInterceptor;
import com.microsoft.identity.internal.testutils.HttpRequestMatcher;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.internal.testutils.mocks.MockTokenResponse;
import com.microsoft.identity.internal.testutils.shadows.ShadowHttpClient;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAndroidSdkStorageEncryptionManager.class,
            ShadowMockAuthority.class,
            ShadowHttpClient.class,
            ShadowPublicClientApplicationConfiguration.class,
            ShadowOpenIdProviderConfigurationClient.class
        })
public abstract class AcquireTokenMockTest extends AcquireTokenAbstractTest {

    @Override
    public String[] getScopes() {
        return TestConstants.Scopes.USER_READ_SCOPE;
    }

    @Override
    public String getAuthority() {
        return AAD_MOCK_AUTHORITY;
    }

    @Before
    public void setup() {
        super.setup();
        mockHttpClient.intercept(
                HttpRequestMatcher.builder().isPOST().build(),
                new HttpRequestInterceptor() {
                    @Override
                    public HttpResponse performIntercept(
                            @NonNull HttpClient.HttpMethod httpMethod,
                            @NonNull URL requestUrl,
                            @NonNull Map<String, String> requestHeaders,
                            @Nullable byte[] requestContent)
                            throws IOException {
                        throw new IOException(
                                "Sending requests to server has been disabled for mocked unit tests");
                    }
                });
    }

    @Test
    public void testAcquireTokenSuccess() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .fromAuthority(getAuthority())
                        .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenFailureNoScope() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .fromAuthority(getAuthority())
                        .withCallback(
                                AcquireTokenTestHelper.failureInteractiveCallback(
                                        ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE))
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenFailureNoActivity() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .fromAuthority(getAuthority())
                        .withCallback(
                                AcquireTokenTestHelper.failureInteractiveCallback(
                                        ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE))
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    @Test(expected = IllegalStateException.class)
    public void testAcquireTokenFailureNoCallback() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .fromAuthority(getAuthority())
                        .withScopes(Arrays.asList(mScopes))
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowStrategyResultUnsuccessful.class})
    public void testAcquireTokenFailureUnsuccessfulTokenResult() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .fromAuthority(getAuthority())
                        .withCallback(
                                AcquireTokenTestHelper.failureInteractiveCallback(
                                        ErrorCodes.UNKNOWN_ERROR_CODE))
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowStrategyResultServerError.class})
    public void testAcquireTokenFailureServerError() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .fromAuthority(getAuthority())
                        .withCallback(
                                AcquireTokenTestHelper.failureInteractiveCallback(
                                        ErrorCodes.INTERNAL_SERVER_ERROR_CODE))
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSuccessFollowedBySilentSuccess() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .fromAuthority(getAuthority())
                        .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .forAccount(AcquireTokenTestHelper.getAccount())
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .fromAuthority(getAuthority())
                        .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessForceRefresh() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(true)
                        .forAccount(account)
                        .fromAuthority(getAuthority())
                        .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessValidCache() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .fromAuthority(getAuthority())
                        .forAccount(account)
                        .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessExpiredAccessToken() {
        ICacheRecord cacheRecord = createDataInCacheWithExpiredAccessToken(mApplication);
        final String loginHint = cacheRecord.getAccount().getUsername();
        final IAccount account = performGetAccount(mApplication, loginHint);

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .forAccount(account)
                        .fromAuthority(getAuthority())
                        .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentFailureEmptyCache() {
        final IAccount account = loadAccountForTest(mApplication);
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .forAccount(account)
                        .fromAuthority(getAuthority())
                        .withCallback(
                                AcquireTokenTestHelper.failureSilentCallback(
                                        ErrorCodes.NO_ACCOUNT_FOUND_ERROR_CODE))
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentFailureNoAuthority() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .forAccount(account)
                        .withCallback(
                                AcquireTokenTestHelper.failureSilentCallback(
                                        ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE))
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentFailureNoAccount() {
        String noAccountErrorCode;

        if (mApplication instanceof ISingleAccountPublicClientApplication) {
            noAccountErrorCode = ErrorCodes.NO_CURRENT_ACCOUNT_ERROR_CODE;
        } else {
            noAccountErrorCode = ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE;
        }

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .fromAuthority(getAuthority())
                        .withCallback(
                                AcquireTokenTestHelper.failureSilentCallback(noAccountErrorCode))
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushSchedulerWithDelay(500);
    }

    @Test
    public void testAcquireTokenSilentFailureNoScopes() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .forAccount(account)
                        .forceRefresh(false)
                        .fromAuthority(getAuthority())
                        .withCallback(
                                AcquireTokenTestHelper.failureSilentCallback(
                                        ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE))
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test(expected = IllegalStateException.class)
    public void testAcquireTokenSilentFailureNoCallback() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .fromAuthority(getAuthority())
                        .forAccount(account)
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenInteractiveResultContainsSomeCorrelationId() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .fromAuthority(getAuthority())
                        .withCallback(
                                new AuthenticationCallback() {
                                    @Override
                                    public void onCancel() {
                                        Assert.fail("Unexpected cancel");
                                    }

                                    @Override
                                    public void onSuccess(
                                            IAuthenticationResult authenticationResult) {
                                        Assert.assertFalse(
                                                StringUtil.isEmpty(
                                                        authenticationResult.getAccessToken()));
                                        Assert.assertNotNull(
                                                authenticationResult.getCorrelationId());
                                        final String correlationId =
                                                authenticationResult.getCorrelationId().toString();
                                        Assert.assertFalse(StringUtil.isEmpty(correlationId));
                                    }

                                    @Override
                                    public void onError(MsalException exception) {
                                        Assert.fail(exception.getMessage());
                                    }
                                })
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentResultContainsSomeCorrelationId() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(true)
                        .forAccount(account)
                        .fromAuthority(getAuthority())
                        .withCallback(
                                new SilentAuthenticationCallback() {
                                    @Override
                                    public void onSuccess(
                                            IAuthenticationResult authenticationResult) {
                                        Assert.assertFalse(
                                                StringUtil.isEmpty(
                                                        authenticationResult.getAccessToken()));
                                        Assert.assertNotNull(
                                                authenticationResult.getCorrelationId());
                                        final String correlationId =
                                                authenticationResult.getCorrelationId().toString();
                                        Assert.assertFalse(StringUtil.isEmpty(correlationId));
                                    }

                                    @Override
                                    public void onError(MsalException exception) {
                                        Assert.fail(exception.getMessage());
                                    }
                                })
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenInteractiveResultContainsProvidedCorrelationId() {
        final String username = "fake@test.com";

        final UUID correlationId = UUID.randomUUID();

        final AcquireTokenParameters parameters =
                new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .fromAuthority(getAuthority())
                        .withCorrelationId(correlationId)
                        .withCallback(
                                new AuthenticationCallback() {
                                    @Override
                                    public void onCancel() {
                                        Assert.fail("Unexpected cancel");
                                    }

                                    @Override
                                    public void onSuccess(
                                            IAuthenticationResult authenticationResult) {
                                        Assert.assertFalse(
                                                StringUtil.isEmpty(
                                                        authenticationResult.getAccessToken()));
                                        Assert.assertNotNull(
                                                authenticationResult.getCorrelationId());
                                        Assert.assertEquals(
                                                correlationId,
                                                authenticationResult.getCorrelationId());
                                    }

                                    @Override
                                    public void onError(MsalException exception) {
                                        Assert.fail(exception.getMessage());
                                    }
                                })
                        .build();

        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentResultContainsProvidedCorrelationId() {
        final IAccount account = loadAccountForTest(mApplication);

        final UUID correlationId = UUID.randomUUID();

        final AcquireTokenSilentParameters silentParameters =
                new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(true)
                        .forAccount(account)
                        .fromAuthority(getAuthority())
                        .withCorrelationId(correlationId)
                        .withCallback(
                                new SilentAuthenticationCallback() {
                                    @Override
                                    public void onSuccess(
                                            IAuthenticationResult authenticationResult) {
                                        Assert.assertFalse(
                                                StringUtil.isEmpty(
                                                        authenticationResult.getAccessToken()));
                                        Assert.assertNotNull(
                                                authenticationResult.getCorrelationId());
                                        Assert.assertEquals(
                                                correlationId,
                                                authenticationResult.getCorrelationId());
                                    }

                                    @Override
                                    public void onError(MsalException exception) {
                                        Assert.fail(exception.getMessage());
                                    }
                                })
                        .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    abstract IAccount performGetAccount(
            IPublicClientApplication application, final String loginHint);

    private ICacheRecord createDataInCache(IPublicClientApplication application) {
        ICacheRecord cacheRecord = null;
        final TokenResponse tokenResponse = MockTokenResponse.getMockSuccessTokenResponse();

        try {
            cacheRecord = RoboTestCacheHelper.saveTokens(tokenResponse, application);
        } catch (ClientException e) {
            fail("Unable to save tokens to cache: " + e.getMessage());
        }

        return cacheRecord;
    }

    private ICacheRecord createDataInCacheWithExpiredAccessToken(
            IPublicClientApplication application) {
        ICacheRecord cacheRecord = null;
        final TokenResponse tokenResponse =
                MockTokenResponse.getMockTokenResponseWithExpiredAccessToken();

        try {
            cacheRecord = RoboTestCacheHelper.saveTokens(tokenResponse, application);
        } catch (ClientException e) {
            fail("Unable to save tokens to cache: " + e.getMessage());
        }

        return cacheRecord;
    }

    private IAccount loadAccountForTest(IPublicClientApplication application) {
        ICacheRecord cacheRecord = createDataInCache(application);
        final String loginHint = cacheRecord.getAccount().getUsername();
        final IAccount account = performGetAccount(application, loginHint);
        return account;
    }
}
