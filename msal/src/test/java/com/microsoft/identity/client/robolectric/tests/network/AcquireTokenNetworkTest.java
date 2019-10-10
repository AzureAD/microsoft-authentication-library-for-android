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
package com.microsoft.identity.client.robolectric.tests.network;

import android.app.Activity;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.robolectric.shadows.ShadowAuthority;
import com.microsoft.identity.client.robolectric.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.robolectric.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.robolectric.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.robolectric.utils.ErrorCodes;
import com.microsoft.identity.client.robolectric.utils.RoboTestUtils;
import com.microsoft.identity.common.internal.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static com.microsoft.identity.client.robolectric.utils.AcquireTokenTestHelper.failureSilentCallback;
import static com.microsoft.identity.client.robolectric.utils.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.client.robolectric.utils.AcquireTokenTestHelper.successfulInteractiveCallback;
import static com.microsoft.identity.client.robolectric.utils.AcquireTokenTestHelper.successfulSilentCallback;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowMsalUtils.class})
/**
 * This class contains PublicClientApplication acquire token tests that hit the network and
 * try to acquire a token. These test are parameterized and cannot be run individually,
 * the entire class must be run together for them to work.
 */
public class AcquireTokenNetworkTest {

    static final String TAG = AcquireTokenNetworkTest.class.getSimpleName();

    static final String[] AAD_SCOPES = {"user.read"};
    static final String[] B2C_SCOPES = {"https://msidlabb2c.onmicrosoft.com/msidlabb2capi/read"};

    static final String AAD_AUTHORITY_TYPE_STRING = "AAD";
    static final String B2C_AUTHORITY_TYPE_STRING = "B2C";

    String mAuthorityType = B2C_AUTHORITY_TYPE_STRING; //default
    String[] mScopes = B2C_SCOPES; //default

    @Before
    public void setup() {
        Logger.info(TAG, "Authority type = " + mAuthorityType);
        AcquireTokenTestHelper.setAccount(null);
    }

    @After
    public void cleanup() {
        AcquireTokenTestHelper.setAccount(null);
    }

    @Test
    public void testAcquireTokenSuccess() {
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

        }.instantiatePCAthenAcquireToken(mAuthorityType);
    }

    @Test
    public void testAcquireTokenSuccessFollowedBySilentSuccess() {
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

        }.instantiatePCAthenAcquireToken(mAuthorityType);
    }

    @Test
    public void testAcquireTokenSilentSuccessForceRefresh() {
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

        }.instantiatePCAthenAcquireToken(mAuthorityType);
    }

    @Test
    public void testAcquireTokenSilentFailureEmptyCache() {
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
                        .withCallback(failureSilentCallback(ErrorCodes.NO_ACCOUNT_FOUND_ERROR_CODE))
                        .build();

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.instantiatePCAthenAcquireToken(mAuthorityType);
    }

    @Test
    public void testAcquireTokenSilentSuccessCacheWithNoAccessToken() {
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

        }.instantiatePCAthenAcquireToken(mAuthorityType);
    }

}
