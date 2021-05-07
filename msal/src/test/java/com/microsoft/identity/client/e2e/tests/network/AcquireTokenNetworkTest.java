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
import com.microsoft.identity.client.e2e.rules.NetworkTestsRuleChain;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.e2e.utils.ErrorCodes;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.failureSilentCallback;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.successfulInteractiveCallback;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.successfulSilentCallback;
import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowPublicClientApplicationConfiguration.class})
public abstract class AcquireTokenNetworkTest extends AcquireTokenAbstractTest implements IAcquireTokenNetworkTest {

    private String mUsername;

    @Rule
    public TestRule rule = NetworkTestsRuleChain.getRule();

    @Before
    public void setup() {
        AcquireTokenTestHelper.setAccount(null);
        final LabUserQuery query = getLabUserQuery();
        mUsername = LabUserHelper.loadUserForTest(query);
        super.setup();
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
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSuccessFollowedBySilentSuccess() {
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(getAuthority())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .withCallback(successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessForceRefresh() {
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(getAuthority())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .withCallback(successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentFailureEmptyCache() {
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        // clear the cache now
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(getAuthority())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .withCallback(failureSilentCallback(ErrorCodes.NO_ACCOUNT_FOUND_ERROR_CODE))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessCacheWithNoAccessToken() {
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        flushScheduler();

        // remove the access token from cache
        TestUtils.removeAccessTokenFromCache(SHARED_PREFERENCES_NAME);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(getAuthority())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .withCallback(successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

}
