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
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.RoboTestCacheHelper;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.robolectric.shadows.ShadowAuthority;
import com.microsoft.identity.client.robolectric.shadows.ShadowHttpRequest;
import com.microsoft.identity.client.robolectric.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.robolectric.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.robolectric.shadows.ShadowStrategyResultServerError;
import com.microsoft.identity.client.robolectric.shadows.ShadowStrategyResultUnsuccessful;
import com.microsoft.identity.client.robolectric.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.robolectric.utils.RoboTestUtils;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.internal.testutils.MockTokenResponse;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static junit.framework.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowHttpRequest.class, ShadowMsalUtils.class})
public final class AcquireTokenMockTest {

    private static final String[] SCOPES = {"user.read"};
    private static final String AAD_MOCK_AUTHORITY = "https://test.authority/mock";

    @Test
    public void testAcquireTokenSuccess() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

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
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    @Ignore // this tests is irrelevant
    public void testAcquireTokenFailureNoLoginHint() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failureInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenFailureNoScope() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failureInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenFailureNoActivity() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failureInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    @Ignore // onError won't be called as callback object is null
    public void testAcquireTokenFailureNoCallback() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withScopes(Arrays.asList(SCOPES))
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    @Config(shadows = {ShadowStrategyResultUnsuccessful.class})
    public void testAcquireTokenFailureUnsuccessfulTokenResult() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failureInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    @Config(shadows = {ShadowStrategyResultServerError.class})
    public void testAcquireTokenFailureServerError() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final String username = "fake@test.com";

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failureInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenSuccessFollowedBySilentSuccess() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

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
                        .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenSilentSuccessForceRefresh() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final IAccount account = loadAccountForTest(publicClientApplication);
                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(true)
                        .forAccount(account)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenSilentSuccessValidCache() {
        new AcquireTokenMockBaseTest() {
            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final IAccount account = loadAccountForTest(publicClientApplication);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .forAccount(account)
                        .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenSilentSuccessExpiredAccessToken() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {
                ICacheRecord cacheRecord = createDataInCacheWithExpiredAccessToken(publicClientApplication);
                final String loginHint = cacheRecord.getAccount().getUsername();
                final IAccount account = performGetAccount(publicClientApplication, loginHint);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .forAccount(account)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenSilentFailureEmptyCache() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final IAccount account = loadAccountForTest(publicClientApplication);
                RoboTestUtils.clearCache();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .forAccount(account)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failureSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenSilentFailureNoAuthority() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final IAccount account = loadAccountForTest(publicClientApplication);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .forAccount(account)
                        .withCallback(AcquireTokenTestHelper.failureSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenSilentFailureNoAccount() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failureSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    public void testAcquireTokenSilentFailureNoScopes() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {
                final IAccount account = loadAccountForTest(publicClientApplication);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(account)
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .withCallback(AcquireTokenTestHelper.failureSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    @Test
    @Ignore // callback will be null
    public void testAcquireTokenSilentFailureNoCallback() {
        new AcquireTokenMockBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final IAccount account = loadAccountForTest(publicClientApplication);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .forAccount(account)
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken();
    }

    private IAccount performGetAccount(IPublicClientApplication application, final String loginHint) {
        final IAccount[] requestedAccount = {null};
        final IMultipleAccountPublicClientApplication multipleAcctApp = (IMultipleAccountPublicClientApplication) application;
        multipleAcctApp.getAccount(
                loginHint.trim(),
                new IMultipleAccountPublicClientApplication.GetAccountCallback() {
                    @Override
                    public void onTaskCompleted(final IAccount account) {
                        if (account != null) {
                            requestedAccount[0] = account;
                        } else {
                            fail("No account found matching identifier");
                        }
                    }

                    @Override
                    public void onError(final MsalException exception) {
                        fail("No account found matching identifier");
                    }
                });
        RoboTestUtils.flushScheduler();
        return requestedAccount[0];
    }

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

    private ICacheRecord createDataInCacheWithExpiredAccessToken(IPublicClientApplication application) {
        ICacheRecord cacheRecord = null;
        final TokenResponse tokenResponse = MockTokenResponse.getMockTokenResponseWithExpiredAccessToken();

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
