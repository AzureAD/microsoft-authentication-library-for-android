package com.microsoft.identity.client.e2e.tests.mocked;

import android.text.TextUtils;

import com.microsoft.identity.client.Account;
import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthorityForMockHttpResponse;
import com.microsoft.identity.client.e2e.shadows.ShadowOpenIdProviderConfigurationClient;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.e2e.tests.AcquireTokenAbstractTest;
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.internal.testutils.HttpRequestMatcher;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.internal.testutils.mocks.MockServerResponse;
import com.microsoft.identity.internal.testutils.mocks.MockTokenCreator;
import com.microsoft.identity.internal.testutils.shadows.ShadowHttpClient;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;
import static com.microsoft.identity.internal.testutils.mocks.MockTokenCreator.CLOUD_DISCOVERY_ENDPOINT_REGEX;
import static com.microsoft.identity.internal.testutils.mocks.MockTokenCreator.MOCK_PREFERRED_USERNAME_VALUE;
import static com.microsoft.identity.internal.testutils.mocks.MockTokenCreator.MOCK_TOKEN_URL_REGEX;

import static org.junit.Assert.fail;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAndroidSdkStorageEncryptionManager.class,
        ShadowAuthorityForMockHttpResponse.class,
        ShadowPublicClientApplicationConfiguration.class,
        ShadowHttpClient.class,
        ShadowOpenIdProviderConfigurationClient.class
})
public class SingleAccountOverloadsWithParametersMockedTest extends AcquireTokenAbstractTest {

    private SingleAccountPublicClientApplication mSingleAccountPCA;
    private final String mUsername = MOCK_PREFERRED_USERNAME_VALUE;

    @Before
    public void setup() {
        super.setup();
        TestUtils.clearCache(SingleAccountPublicClientApplication.SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES);
        mSingleAccountPCA = (SingleAccountPublicClientApplication) mApplication;
        mockHttpClient.intercept(
                HttpRequestMatcher.builder()
                        .isPOST()
                        .urlPattern(MOCK_TOKEN_URL_REGEX)
                        .build(),
                MockServerResponse.getMockTokenSuccessResponse()
        );
        mockHttpClient.intercept(
                HttpRequestMatcher.builder()
                        .isGET()
                        .urlPattern(CLOUD_DISCOVERY_ENDPOINT_REGEX)
                        .build(),
                MockServerResponse.getMockCloudDiscoveryResponse()
        );
    }

    @Test
    public void testSignInOnlyAllowedOnceWithParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(getScopesList())
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final SignInParameters secondSignInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getInvalidParameterExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(secondSignInParameters);
        countDownLatch.await();
    }

    @Test
    public void testSignInWithPromptOnlyAllowedOnceWithParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withPrompt(Prompt.LOGIN)
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final SignInParameters secondSignInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withPrompt(Prompt.LOGIN)
                .withCallback(getInvalidParameterExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(secondSignInParameters);
        countDownLatch.await();
    }

    @Test
    public void testSignInAgainAllowsSignInAgainWithParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final SignInParameters secondSignInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withPrompt(Prompt.LOGIN)
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signInAgain(secondSignInParameters);
        RoboTestUtils.flushScheduler();
        countDownLatch.await();
    }

    @Test
    public void testCannotSignInAgainIfNeverSignedInBeforeWithParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withPrompt(Prompt.LOGIN)
                .withCallback(getNoCurrentAccountExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signInAgain(signInParameters);
        countDownLatch.await();
    }

    @Test
    public void testCanSignOutIfAlreadySignedInWithParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                Assert.assertTrue("Successfully signed out", true);
                countDownLatch.countDown();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                fail(exception.getMessage());
            }
        });
        RoboTestUtils.flushScheduler();
        countDownLatch.await();
    }

    @Test
    public void testCanAcquireTokenIfAlreadySignInWithParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(getScopesList())
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(getScopesList())
                .withLoginHint(mUsername)
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushScheduler();
        countDownLatch.await();
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfNoLoginHintNoAccountProvidedWithSignInParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getAccountMismatchExpectedCallback(countDownLatch))
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
        countDownLatch.await();
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfLoginHintDoesNotMatchWithSignInParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withLoginHint("someOtherAccount@test.com")
                .withCallback(getAccountMismatchExpectedCallback(countDownLatch))
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
        countDownLatch.await();
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfAccountDoesNotMatchWithSignInParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final IAccount fakeOtherAccount = getFakeOtherAccount();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .forAccount(fakeOtherAccount)
                .withCallback(getAccountMismatchExpectedCallback(countDownLatch))
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
        countDownLatch.await();
    }

    @Test
    public void testCanAcquireTokenWithParametersIfLoginHintMatchesWithSignInParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withLoginHint(mUsername)
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushScheduler();
        countDownLatch.await();
    }

    @Test
    public void testCanAcquireTokenWithParametersIfAccountMatchesWithSignInParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();

        mSingleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushScheduler();
        countDownLatch.await();
    }

    @Test
    public void testCannotAcquireTokenSilentlyWithParametersIfNotSignedIn() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .fromAuthority(getAuthority())
                .withCallback(getNoCurrentAccountExpectedCallback(countDownLatch))
                .build();

        mSingleAccountPCA.acquireTokenSilentAsync(silentParameters);
        countDownLatch.await();
    }

    @Test
    public void testCanAcquireTokenSilentlyIfAlreadySignedInWithParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .fromAuthority(getAuthority())
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
        countDownLatch.await();
    }

    @Test
    public void testCanAcquireTokenSilentlyWithParametersIfAlreadySignedInWithSignInParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .fromAuthority(getAuthority())
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();

        mSingleAccountPCA.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
        countDownLatch.await();
    }

    @Test
    public void testCannotAcquireTokenSilentlyWithParametersIfAccountDoesNotMatchWithSignInParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        final IAccount fakeOtherAccount = getFakeOtherAccount();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(fakeOtherAccount)
                .fromAuthority(getAuthority())
                .withCallback(getAccountMismatchExpectedCallback(countDownLatch))
                .build();

        mSingleAccountPCA.acquireTokenSilentAsync(silentParameters);
        countDownLatch.await();
    }

    @Test
    public void testCanGetCurrentAccountIfAlreadySignedInWithParameters() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getSuccessExpectedCallback(countDownLatch))
                .build();
        mSingleAccountPCA.signIn(signInParameters);
        RoboTestUtils.flushScheduler();

        mSingleAccountPCA.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                assert activeAccount != null;
                Assert.assertEquals(activeAccount.getId(), AcquireTokenTestHelper.getAccount().getId());
                countDownLatch.countDown();
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
        countDownLatch.await();
    }

    private AuthenticationCallback getSuccessExpectedCallback(final CountDownLatch countDownLatch) {
        return new AuthenticationCallback() {
            @Override
            public void onCancel() {
                fail("Unexpected cancel");
            }

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                AcquireTokenTestHelper.setAccount(authenticationResult.getAccount());
                Assert.assertFalse(TextUtils.isEmpty(authenticationResult.getAccessToken()));
                countDownLatch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                throw new AssertionError(exception);
            }
        };
    }

    private AuthenticationCallback getNoCurrentAccountExpectedCallback(final CountDownLatch countDownLatch) {
        return getClientExceptionFailureCallback(MsalClientException.NO_CURRENT_ACCOUNT, countDownLatch);
    }

    private AuthenticationCallback getInvalidParameterExpectedCallback(final CountDownLatch countDownLatch) {
        return getClientExceptionFailureCallback(MsalClientException.INVALID_PARAMETER, countDownLatch);
    }

    private AuthenticationCallback getAccountMismatchExpectedCallback(final CountDownLatch countDownLatch) {
        return getClientExceptionFailureCallback(MsalClientException.CURRENT_ACCOUNT_MISMATCH, countDownLatch);
    }

    private AuthenticationCallback getClientExceptionFailureCallback(final String expectedErrorCode, final CountDownLatch countDownLatch) {
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
                countDownLatch.countDown();
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

    public List<String> getScopesList() {
        return Arrays.asList(getScopes());
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
