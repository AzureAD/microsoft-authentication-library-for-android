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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.Account;
import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthorityForMockHttpResponse;
import com.microsoft.identity.internal.testutils.HttpRequestMatcher;
import com.microsoft.identity.internal.testutils.shadows.ShadowHttpClient;
import com.microsoft.identity.client.e2e.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.e2e.shadows.ShadowOpenIdProviderConfigurationClient;
import com.microsoft.identity.client.e2e.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.net.HttpClient;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.internal.testutils.MockHttpClient;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.internal.testutils.mocks.MockServerResponse;
import com.microsoft.identity.internal.testutils.mocks.MockTokenCreator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import edu.emory.mathcs.backport.java.util.Arrays;

import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;
import static com.microsoft.identity.internal.testutils.mocks.MockTokenCreator.CLOUD_DISCOVERY_ENDPOINT_REGEX;
import static com.microsoft.identity.internal.testutils.mocks.MockTokenCreator.MOCK_PREFERRED_USERNAME_VALUE;
import static com.microsoft.identity.internal.testutils.mocks.MockTokenCreator.MOCK_TOKEN_URL_REGEX;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowStorageHelper.class,
        ShadowAuthorityForMockHttpResponse.class,
        ShadowMsalUtils.class,
        ShadowHttpClient.class,
        ShadowOpenIdProviderConfigurationClient.class
})
public class SingleAccountOverloadsMockedTest extends AcquireTokenAbstractTest {

    private SingleAccountPublicClientApplication mSingleAccountPCA;
    private final String mUsername = MOCK_PREFERRED_USERNAME_VALUE;

    @Before
    public void setup() {
        super.setup();
        TestUtils.clearCache(SingleAccountPublicClientApplication.SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES);
        mSingleAccountPCA = (SingleAccountPublicClientApplication) mApplication;
        mockHttpClient.intercept(
                HttpRequestMatcher.builder()
                        .method(m -> m.equals(HttpClient.HttpMethod.POST))
                        .url(u -> u.toString().matches(MOCK_TOKEN_URL_REGEX))
                        .build(),
                MockServerResponse.getMockTokenSuccessResponse()
        );
        mockHttpClient.intercept(
                HttpRequestMatcher.builder()
                        .method(m -> m.equals(HttpClient.HttpMethod.GET))
                        .url(u -> u.toString().matches(CLOUD_DISCOVERY_ENDPOINT_REGEX))
                        .build(),
                MockServerResponse.getMockCloudDiscoveryResponse()
        );
    }

    @Test
    public void testSignInOnlyAllowedOnce() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getInvalidParameterExpectedCallback());
    }

    @Test
    public void testSignInWithPromptOnlyAllowedOnce() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, Prompt.LOGIN, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, Prompt.LOGIN, getInvalidParameterExpectedCallback());
    }

    @Test
    public void testSignInAgainAllowsSignInAgain() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.signInAgain(mActivity, mScopes, Prompt.LOGIN, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotSignInAgainIfNeverSignedInBefore() {
        mSingleAccountPCA.signInAgain(mActivity, mScopes, Prompt.LOGIN, getNoCurrentAccountExpectedCallback());
    }

    @Test
    public void testCanSignOutIfAlreadySignedIn() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                Assert.assertTrue("Successfully signed out", true);
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                fail(exception.getMessage());
            }
        });

        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotSignOutIfNotSignedIn() {
        mSingleAccountPCA.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                fail("Unexpected sign out");
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.assertTrue(exception instanceof MsalClientException);
                Assert.assertEquals(exception.getErrorCode(), MsalClientException.NO_CURRENT_ACCOUNT);
            }
        });
    }

    @Test
    public void testCanAcquireTokenIfAlreadySignIn() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.acquireToken(mActivity, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotAcquireTokenIfNotSignedIn() {
        mSingleAccountPCA.acquireToken(mActivity, mScopes, getNoCurrentAccountExpectedCallback());
    }

    public void testCannotAcquireTokenWithParametersIfNotSignedIn() {
        //todo implement this test. Blocked by MSAL Issue #1032
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfNoLoginHintNoAccountProvided() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getAccountMismatchExpectedCallback())
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfLoginHintDoesNotMatch() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withLoginHint("someOtherAccount@test.com")
                .withCallback(getAccountMismatchExpectedCallback())
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfAccountDoesNotMatch() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final IAccount fakeOtherAccount = getFakeOtherAccount();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .forAccount(fakeOtherAccount)
                .withCallback(getAccountMismatchExpectedCallback())
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
    }

    @Test
    public void testCanAcquireTokenWithParametersIfLoginHintMatches() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withLoginHint(mUsername)
                .withCallback(getSuccessExpectedCallback())
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCanAcquireTokenWithParametersIfAccountMatches() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withCallback(getSuccessExpectedCallback())
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotAcquireTokenSilentlyIfNotSignedIn() {
        mSingleAccountPCA.acquireTokenSilentAsync(mScopes, getAuthority(), getNoCurrentAccountExpectedCallback());
    }

    @Test
    public void testCanAcquireTokenSilentlyIfAlreadySignedIn() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.acquireTokenSilentAsync(mScopes, getAuthority(), getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotAcquireTokenSilentlyWithParametersIfNotSignedIn() {
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .fromAuthority(getAuthority())
                .withCallback(getNoCurrentAccountExpectedCallback())
                .build();

        mSingleAccountPCA.acquireTokenSilentAsync(silentParameters);
    }

    @Test
    public void testCanAcquireTokenSilentlyWithParametersIfAlreadySignedIn() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .fromAuthority(getAuthority())
                .withCallback(getSuccessExpectedCallback())
                .build();

        mSingleAccountPCA.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotAcquireTokenSilentlyWithParametersIfAccountDoesNotMatch() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final IAccount fakeOtherAccount = getFakeOtherAccount();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(fakeOtherAccount)
                .fromAuthority(getAuthority())
                .withCallback(getAccountMismatchExpectedCallback())
                .build();

        mSingleAccountPCA.acquireTokenSilentAsync(silentParameters);
    }

    @Test
    public void testCanGetCurrentAccountIfAlreadySignedIn() {
        mSingleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                assert activeAccount != null;
                Assert.assertEquals(activeAccount.getId(), AcquireTokenTestHelper.getAccount().getId());
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                Assert.fail();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail(exception.getMessage());
            }
        });

        RoboTestUtils.flushScheduler();
    }

    public void testCannotGetCurrentAccountIfNotSignedIn() {
        // todo need to improve the behaviour around this before a test should be written
    }

    private AuthenticationCallback getSuccessExpectedCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onCancel() {
                fail("Unexpected cancel");
            }

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                AcquireTokenTestHelper.setAccount(authenticationResult.getAccount());
                Assert.assertFalse(TextUtils.isEmpty(authenticationResult.getAccessToken()));
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        };
    }

    private AuthenticationCallback getNoCurrentAccountExpectedCallback() {
        return getClientExceptionFailureCallback(MsalClientException.NO_CURRENT_ACCOUNT);
    }

    private AuthenticationCallback getInvalidParameterExpectedCallback() {
        return getClientExceptionFailureCallback(MsalClientException.INVALID_PARAMETER);
    }

    private AuthenticationCallback getAccountMismatchExpectedCallback() {
        return getClientExceptionFailureCallback(MsalClientException.CURRENT_ACCOUNT_MISMATCH);
    }

    private AuthenticationCallback getClientExceptionFailureCallback(final String expectedErrorCode) {
        return new AuthenticationCallback() {
            @Override
            public void onCancel() {
                fail("Unexpected cancel");
            }

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                fail("Unexpected success");
            }

            @Override
            public void onError(MsalException exception) {
                Assert.assertTrue(exception instanceof MsalClientException);
                Assert.assertEquals(expectedErrorCode, exception.getErrorCode());
            }
        };
    }

    private IAccount getFakeOtherAccount() {
        IDToken mockIdToken = null;
        final String mockClientInfo = MockTokenCreator.createMockRawClientInfo();
        try {
            mockIdToken = new IDToken(MockTokenCreator.createMockIdToken());
        } catch (ServiceException e) {
            fail(e.getMessage());
        }
        return new Account(mockClientInfo, mockIdToken);
    }

    @Override
    public String[] getScopes() {
        return USER_READ_SCOPE;
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public String getConfigFilePath() {
        return TestConstants.Configurations.SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
    }
}
