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

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowMockAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowOpenIdProviderConfigurationClient;
import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.internal.testutils.HttpRequestInterceptor;
import com.microsoft.identity.internal.testutils.HttpRequestMatcher;
import com.microsoft.identity.internal.testutils.shadows.ShadowHttpClient;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.e2e.utils.CacheCountAuthenticationCallback;
import com.microsoft.identity.common.internal.controllers.CommandDispatcherHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static com.microsoft.identity.internal.testutils.TestConstants.Authorities.AAD_MOCK_AUTHORITY;
import static com.microsoft.identity.internal.testutils.TestConstants.Authorities.AAD_MOCK_DELAYED_RESPONSE_AUTHORITY;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// TODO: re-enable this oncd we re-enable command caching.
@Ignore
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAndroidSdkStorageEncryptionManager.class,
        ShadowMockAuthority.class,
        ShadowHttpClient.class,
        ShadowPublicClientApplicationConfiguration.class,
        ShadowOpenIdProviderConfigurationClient.class
})
public final class CommandResultCachingTest extends AcquireTokenAbstractTest {

    @Before
    public void before() {
        super.setup();
        mockHttpClient.intercept(
                HttpRequestMatcher.builder().isPOST().build(), new HttpRequestInterceptor() {
                    @Override
                    public HttpResponse performIntercept(
                            @NonNull HttpClient.HttpMethod httpMethod,
                            @NonNull URL requestUrl,
                            @NonNull Map<String, String> requestHeaders,
                            @Nullable byte[] requestContent) throws IOException {
                        throw new IOException("Sending requests to server has been disabled for mocked unit tests");
                    }
                });

        ShadowLog.stream = System.out;
    }

    @Override
    public String getAuthority() {
        return AAD_MOCK_AUTHORITY;
    }

    /**
     * verifies that two different commands result in 2 cache entries
     */
    @Test
    public void testAcquireTokenCache2DifferentRequests() throws InterruptedException {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(new CacheCountAuthenticationCallback(1))
                .build();

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("device_id", null);

        final AcquireTokenSilentParameters modifiedSilentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withClaims(cr)
                .withCallback(new CacheCountAuthenticationCallback(2))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        Thread.sleep(500);
        mApplication.acquireTokenSilentAsync(modifiedSilentParameters);

        flushScheduler();
    }

    /**
     * Second silent request is expected to be retrieved from the cache.
     */
    @Test
    public void testAcquireTokenCache2IdenticalRequests() throws InterruptedException {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(new CacheCountAuthenticationCallback(1))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        Thread.sleep(200);
        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    /**
     * Second silent request is expected to fail with a duplicate command exception
     */
    @Test
    public void testAcquireTokenCache2IdenticalRequestsConcurrent() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(new CacheCountAuthenticationCallback(1))
                .build();

        final AcquireTokenSilentParameters silentParameters1 = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_DELAYED_RESPONSE_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.failedSilentRequestDuplicateCommandCallback())
                .build();


        mApplication.acquireTokenSilentAsync(silentParameters);
        mApplication.acquireTokenSilentAsync(silentParameters1);

        flushScheduler();
    }

    /**
     * NOTE: This runs a bit longer
     */
    @Test
    public void testAcquireTokenExceedCacheMaxItems() throws InterruptedException {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        Logger.getInstance().setLogLevel(Logger.LogLevel.VERBOSE);
        mApplication.acquireToken(parameters);

        flushScheduler();

        for (int i = 0; i < 250; i++) {

            ClaimsRequest cr = new ClaimsRequest();
            cr.requestClaimInAccessToken("device_" + Integer.toString(i), null);

            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .forAccount(AcquireTokenTestHelper.getAccount())
                    .withScopes(Arrays.asList(mScopes))
                    .forceRefresh(false)
                    .fromAuthority(AAD_MOCK_AUTHORITY)
                    .withClaims(cr)
                    .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                    .build();

            mApplication.acquireTokenSilentAsync(silentParameters);
            Thread.sleep(100);
        }

        Thread.sleep(1000);
        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("device_10", null);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withClaims(cr)
                .withCallback(new CacheCountAuthenticationCallback(250))
                .build();

        flushScheduler();

        mApplication.acquireTokenSilentAsync(silentParameters);

        flushScheduler();
    }

    @Override
    public String getConfigFilePath() {
        return MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
    }

    @Override
    public String[] getScopes() {
        return USER_READ_SCOPE;
    }
}
