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
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.RoboTestCacheHelper;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthority;
import com.microsoft.identity.client.e2e.shadows.ShadowHttpRequest;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.e2e.shadows.ShadowStrategyResultServerError;
import com.microsoft.identity.client.e2e.shadows.ShadowStrategyResultUnsuccessful;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.e2e.utils.ErrorCodes;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.e2e.utils.TestConstants;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.internal.testutils.MockTokenResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static com.microsoft.identity.client.e2e.utils.TestConstants.Authorities.AAD_MOCK_AUTHORITY;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowHttpRequest.class, ShadowMsalUtils.class})
public abstract class AcquireTokenMockTest extends AcquireTokenAbstractTest {

    @Override
    public String[] getScopes() {
        return TestConstants.Scopes.USER_READ_SCOPE;
    }

    @Test
    public void testAcquireTokenSuccess() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();
    }


    @Test
    public void testAcquireTokenFailureNoScope() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.failureInteractiveCallback(ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE))
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenFailureNoActivity() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.failureInteractiveCallback(ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE))
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();
    }

    @Test(expected = IllegalStateException.class)
    public void testAcquireTokenFailureNoCallback() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withScopes(Arrays.asList(mScopes))
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowStrategyResultUnsuccessful.class})
    public void testAcquireTokenFailureUnsuccessfulTokenResult() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.failureInteractiveCallback(ErrorCodes.UNKNOWN_ERROR_CODE))
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    @Config(shadows = {ShadowStrategyResultServerError.class})
    public void testAcquireTokenFailureServerError() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.failureInteractiveCallback(ErrorCodes.INTERNAL_SERVER_ERROR_CODE))
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSuccessFollowedBySilentSuccess() {
        final String username = "fake@test.com";

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
                .withScopes(Arrays.asList(mScopes))
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulInteractiveCallback())
                .build();

        mApplication.acquireToken(parameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessForceRefresh() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(true)
                .forAccount(account)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessValidCache() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .forAccount(account)
                .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentSuccessExpiredAccessToken() {
        ICacheRecord cacheRecord = createDataInCacheWithExpiredAccessToken(mApplication);
        final String loginHint = cacheRecord.getAccount().getUsername();
        final IAccount account = performGetAccount(mApplication, loginHint);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .forAccount(account)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.successfulSilentCallback())
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentFailureEmptyCache() {
        final IAccount account = loadAccountForTest(mApplication);
        RoboTestUtils.clearCache();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .forAccount(account)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.failureSilentCallback(ErrorCodes.NO_ACCOUNT_FOUND_ERROR_CODE))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentFailureNoAuthority() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .forAccount(account)
                .withCallback(AcquireTokenTestHelper.failureSilentCallback(ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testAcquireTokenSilentFailureNoAccount() {
        String noAccountErrorCode;

        if (mApplication instanceof ISingleAccountPublicClientApplication) {
            noAccountErrorCode = ErrorCodes.NO_CURRENT_ACCOUNT_ERROR_CODE;
        } else {
            noAccountErrorCode = ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE;
        }

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.failureSilentCallback(noAccountErrorCode))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushSchedulerWithDelay(500);
    }

    @Test
    public void testAcquireTokenSilentFailureNoScopes() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .withCallback(AcquireTokenTestHelper.failureSilentCallback(ErrorCodes.ILLEGAL_ARGUMENT_ERROR_CODE))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test(expected = IllegalStateException.class)
    public void testAcquireTokenSilentFailureNoCallback() {
        final IAccount account = loadAccountForTest(mApplication);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(false)
                .fromAuthority(AAD_MOCK_AUTHORITY)
                .forAccount(account)
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    abstract IAccount performGetAccount(IPublicClientApplication application, final String loginHint);

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
