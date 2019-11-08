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
package com.microsoft.identity.client.e2e.tests.network;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.e2e.utils.ErrorCodes;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.failureSilentCallback;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.successfulInteractiveCallback;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.successfulSilentCallback;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowMsalUtils.class})
public abstract class AcquireTokenNetworkTest extends AcquireTokenAbstractTest implements IAcquireTokenNetworkTest {

    private String mUsername;

    @Before
    public void setup() {
        AcquireTokenTestHelper.setAccount(null);
        final LabUserQuery query = getLabUserQuery();
        mUsername = LabUserHelper.getUpnForTest(query);
        super.setup();
    }

    @After
    public void cleanup() {
        AcquireTokenTestHelper.setAccount(null);
    }

    @Test
    public void testAcquireTokenSuccess() {
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();


        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSuccessFollowedBySilentSuccess() {
        final String authority = mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(authority)
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .withCallback(successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessForceRefresh() {
        final String authority = mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(authority)
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .withCallback(successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentFailureEmptyCache() {
        final String authority = mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
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

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessCacheWithNoAccessToken() {
        final String authority = mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
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

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }
}
