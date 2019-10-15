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
package com.microsoft.identity.client.robolectric.tests.mocked;

import android.app.Activity;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.robolectric.shadows.ShadowAuthority;
import com.microsoft.identity.client.robolectric.shadows.ShadowHttpRequest;
import com.microsoft.identity.client.robolectric.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.robolectric.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.robolectric.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.robolectric.utils.CacheCountAuthenticationCallback;
import com.microsoft.identity.client.robolectric.utils.RoboTestUtils;
import com.microsoft.identity.common.internal.controllers.CommandDispatcherHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowHttpRequest.class, ShadowMsalUtils.class})
public final class CommandResultCachingTest {

    private static final String[] SCOPES = {"user.read"};
    private static final String AAD_MOCK_AUTHORITY = "https://test.authority/mock";
    private static final String AAD_MOCK_DELAYED_RESPONSE_AUTHORITY = "https://test.authority/mock_with_delays";

    @Before
    public void before(){
        CommandDispatcherHelper.clear();
    }

    /**
     * verifies that two different commands result in 2 cache entries
     */
    @Test
    public void testAcquireTokenCache2DifferentRequests() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) throws InterruptedException {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(AcquireTokenTestHelper.getAccount())
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(new CacheCountAuthenticationCallback(1))
                        .build();

                ClaimsRequest cr = new ClaimsRequest();
                cr.requestClaimInAccessToken("device_id", null);

                final AcquireTokenSilentParameters modifiedSilentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(AcquireTokenTestHelper.getAccount())
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withClaims(cr)
                        .withCallback(new CacheCountAuthenticationCallback(2))
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                Thread.sleep(500);
                publicClientApplication.acquireTokenSilentAsync(modifiedSilentParameters);

                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    /**
     * Second silent request is expected to be retrieved from the cache.
     */
    @Test
    public void testAcquireTokenCache2IdenticalRequests() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) throws InterruptedException {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(AcquireTokenTestHelper.getAccount())
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(new CacheCountAuthenticationCallback(1))
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                Thread.sleep(200);
                publicClientApplication.acquireTokenSilentAsync(silentParameters);

                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    /**
     * Second silent request is expected to fail with a duplicate command exception
     */
    @Test
    public void testAcquireTokenCache2IdenticalRequestsConcurrent() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) throws InterruptedException {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(AcquireTokenTestHelper.getAccount())
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(new CacheCountAuthenticationCallback(1))
                        .build();

                final AcquireTokenSilentParameters silentParameters1 = new AcquireTokenSilentParameters.Builder()
                        .forAccount(AcquireTokenTestHelper.getAccount())
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_DELAYED_RESPONSE_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failedSilentRequestDuplicateCommandCallback())
                        .build();


                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                publicClientApplication.acquireTokenSilentAsync(silentParameters1);

                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    /**
     * NOTE: This runs a bit longer
     */
    @Test
    public void testAcquireTokenExceedCacheMaxItems() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) throws InterruptedException {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();

                for (int i = 0; i < 250; i++) {

                    ClaimsRequest cr = new ClaimsRequest();
                    cr.requestClaimInAccessToken("device_" + Integer.toString(i), null );

                    final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                            .forAccount(AcquireTokenTestHelper.getAccount())
                            .withScopes(Arrays.asList(SCOPES))
                            .forceRefresh(false)
                            .fromAuthority(AAD_MOCK_AUTHORITY)
                            .withClaims(cr)
                            .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                            .build();

                    publicClientApplication.acquireTokenSilentAsync(silentParameters);
                    Thread.sleep(100);
                }

                Thread.sleep(1000);
                ClaimsRequest cr = new ClaimsRequest();
                cr.requestClaimInAccessToken("device_10", null);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(AcquireTokenTestHelper.getAccount())
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withClaims(cr)
                        .withCallback(new CacheCountAuthenticationCallback(250))
                        .build();

                RoboTestUtils.flushScheduler();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);

                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

}
