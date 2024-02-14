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
package com.microsoft.identity.client.e2e.tests;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.http.MockHttpClient;
import com.microsoft.identity.internal.testutils.TestUtils;

import org.junit.After;
import org.junit.Before;

import java.util.Arrays;

import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.successfulInteractiveCallback;
import static com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper.successfulSilentCallback;
import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;

public abstract class AcquireTokenAbstractTest extends PublicClientApplicationAbstractTest implements IAcquireTokenTest {

    protected String[] mScopes;
    protected final MockHttpClient mockHttpClient = MockHttpClient.install();

    @Before
    public void setup() {
        mScopes = getScopes();
        super.setup();
    }

    @After
    public void cleanup() {
        AcquireTokenTestHelper.setAccount(null);
        // remove everything from cache after test ends
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);

        Authority.clearKnownAuthorities();

        mockHttpClient.uninstall();
    }

    public void performInteractiveAcquireTokenCall(final String username) {
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback())
                .build();


        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    public void performInteractiveAcquireTokenCall(final String username, final String authority) {
        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(authority)
                .withCallback(successfulInteractiveCallback())
                .build();


        mApplication.acquireToken(parameters);
        flushScheduler();
    }

    public void performSilentAcquireTokenCall(final String[] scopes) {
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(getAuthority())
                .withScopes(Arrays.asList(scopes))
                .forceRefresh(false)
                .withCallback(successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    public void performSilentAcquireTokenCall(final IAccount account) {
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .withCallback(successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }

    public void performSilentAcquireTokenCall(final IAccount account, final String authority) {
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(authority)
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .withCallback(successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        flushScheduler();
    }
}
