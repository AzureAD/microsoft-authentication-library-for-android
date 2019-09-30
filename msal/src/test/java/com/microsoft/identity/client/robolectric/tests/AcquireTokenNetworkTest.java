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
package com.microsoft.identity.client.robolectric.tests;

import android.app.Activity;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.robolectric.shadows.ShadowAuthority;
import com.microsoft.identity.client.robolectric.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.robolectric.shadows.ShadowStorageHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collection;

import static com.microsoft.identity.client.robolectric.tests.AcquireTokenTestHelper.failureSilentCallback;
import static com.microsoft.identity.client.robolectric.tests.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.client.robolectric.tests.AcquireTokenTestHelper.successfulInteractiveCallback;
import static com.microsoft.identity.client.robolectric.tests.AcquireTokenTestHelper.successfulSilentCallback;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowMsalUtils.class})
public final class AcquireTokenNetworkTest {

    private static final String[] AAD_SCOPES = {"user.read"};
    private static final String[] B2C_SCOPES = {"https://msidlabb2c.onmicrosoft.com/msidlabb2capi/read"};

    private static final String AAD_AUTHORITY_TYPE_STRING = "AAD";
    private static final String B2C_AUTHORITY_TYPE_STRING = "B2C";

    private String mAuthorityType;
    private String[] mScopes;

    public AcquireTokenNetworkTest(String authorityType, String[] scopes) {
        mAuthorityType = authorityType;
        mScopes = scopes;
    }

    @ParameterizedRobolectricTestRunner.Parameters(name = "Authority Type = {0}")
    public static Collection data() {
        return Arrays.asList(new Object[][]{
                {AAD_AUTHORITY_TYPE_STRING, AAD_SCOPES},
                {B2C_AUTHORITY_TYPE_STRING, B2C_SCOPES}
        });
    }

    @Before
    public void setup() {
        AcquireTokenTestHelper.setAccount(null);
    }

    @After
    public void cleanup() {
        AcquireTokenTestHelper.setAccount(null);
    }

    @Test
    public void canPerformROPC() {
        new AcquireTokenNetworkBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final String username) {
                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .withCallback(successfulInteractiveCallback())
                        .build();


                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest(mAuthorityType);
    }

    @Test
    public void canAcquireSilentAfterGettingToken() {
        new AcquireTokenNetworkBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final String username) {
                final String authority = publicClientApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .withCallback(successfulInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(getAccount())
                        .fromAuthority(authority)
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .withCallback(successfulSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest(mAuthorityType);
    }

    @Test
    public void forceRefreshWorks() {
        new AcquireTokenNetworkBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final String username) {

                final String authority = publicClientApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .withCallback(successfulInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(getAccount())
                        .fromAuthority(authority)
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(true)
                        .withCallback(successfulSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest(mAuthorityType);
    }

    @Test
    public void silentCallFailsIfCacheIsEmpty() {
        new AcquireTokenNetworkBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final String username) {

                final String authority = publicClientApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .withCallback(successfulInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();

                // clear the cache now
                RoboTestUtils.clearCache();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(getAccount())
                        .fromAuthority(authority)
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .withCallback(failureSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest(mAuthorityType);
    }

    @Test
    public void silentWorksWhenCacheHasNoAccessToken() {
        new AcquireTokenNetworkBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity,
                                      final String username) {

                final String authority = publicClientApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(mScopes))
                        .withCallback(successfulInteractiveCallback())
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();

                // remove the access token from cache
                RoboTestUtils.removeAccessTokenFromCache();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .forAccount(getAccount())
                        .fromAuthority(authority)
                        .withScopes(Arrays.asList(mScopes))
                        .forceRefresh(false)
                        .withCallback(successfulSilentCallback())
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest(mAuthorityType);
    }

}
