package com.microsoft.identity.client.e2e.tests.network;

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
import com.microsoft.identity.client.e2e.utils.AcquireTokenTestHelper;
import com.microsoft.identity.client.e2e.utils.RoboTestUtils;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.internal.providers.oauth2.IDToken;
import com.microsoft.identity.internal.testutils.TestConstants;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;
import com.microsoft.identity.internal.testutils.mocks.MockTokenCreator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.emory.mathcs.backport.java.util.Arrays;

import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;
import static org.junit.Assert.fail;

public class SingleAccountAcquireTokenNetworkTest extends AcquireTokenNetworkAbstractTest {

    private SingleAccountPublicClientApplication singleAccountPCA;

    @Before
    public void setup() {
        super.setup();
        TestUtils.clearCache(SingleAccountPublicClientApplication.SINGLE_ACCOUNT_CREDENTIAL_SHARED_PREFERENCES);
        singleAccountPCA = (SingleAccountPublicClientApplication) mApplication;
    }

    @Test
    public void testSignInOnlyAllowedOnce() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getInvalidParameterExpectedCallback());
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testSignInWithPromptOnlyAllowedOnce() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, Prompt.LOGIN, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        singleAccountPCA.signIn(mActivity, mUsername, mScopes, Prompt.LOGIN, getInvalidParameterExpectedCallback());
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testSignInAgainAllowsSignInAgain() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        singleAccountPCA.signInAgain(mActivity, mScopes, Prompt.LOGIN, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotSignInAgainIfNeverSignedInBefore() {
        singleAccountPCA.signInAgain(mActivity, mScopes, Prompt.LOGIN, getNoCurrentAccountExpectedCallback());
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCanSignOutIfAlreadySignedIn() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        singleAccountPCA.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
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
        singleAccountPCA.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
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

        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCanAcquireTokenIfAlreadySignIn() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        singleAccountPCA.acquireToken(mActivity, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotAcquireTokenIfNotSignedIn() {
        singleAccountPCA.acquireToken(mActivity, mScopes, getNoCurrentAccountExpectedCallback());
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    @Ignore // failing - we need to throw an exception if there is no current account
    public void testCannotAcquireTokenWithParametersIfNotSignedIn() {
        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mUsername)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getNoCurrentAccountExpectedCallback())
                .build();

        singleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfNoLoginHintNoAccountProvided() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(getAccountMismatchExpectedCallback())
                .build();

        singleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfLoginHintDoesNotMatch() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withLoginHint("someOtherAccount@test.com")
                .withCallback(getAccountMismatchExpectedCallback())
                .build();

        singleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCannotAcquireTokenWithParametersIfAccountDoesNotMatch() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final IAccount fakeOtherAccount = getFakeOtherAccount();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .forAccount(fakeOtherAccount)
                .withCallback(getAccountMismatchExpectedCallback())
                .build();

        singleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCanAcquireTokenWithParametersIfLoginHintMatches() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withLoginHint(mUsername)
                .withCallback(getSuccessExpectedCallback())
                .build();

        singleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCanAcquireTokenWithParametersIfAccountMatches() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenParameters acquireTokenParameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .withCallback(getSuccessExpectedCallback())
                .build();

        singleAccountPCA.acquireToken(acquireTokenParameters);
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCannotAcquireTokenSilentlyIfNotSignedIn() {
        singleAccountPCA.acquireTokenSilentAsync(mScopes, getAuthority(), getNoCurrentAccountExpectedCallback());
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCanAcquireTokenSilentlyIfAlreadySignedIn() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        singleAccountPCA.acquireTokenSilentAsync(mScopes, getAuthority(), getSuccessExpectedCallback());
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

        singleAccountPCA.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCanAcquireTokenSilentlyWithParametersIfAlreadySignedIn() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(AcquireTokenTestHelper.getAccount())
                .fromAuthority(getAuthority())
                .withCallback(getSuccessExpectedCallback())
                .build();

        singleAccountPCA.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushScheduler();
    }

    @Test
    public void testCannotAcquireTokenSilentlyWithParametersIfAccountDoesNotMatch() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        final IAccount fakeOtherAccount = getFakeOtherAccount();

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .withScopes(Arrays.asList(mScopes))
                .forAccount(fakeOtherAccount)
                .fromAuthority(getAuthority())
                .withCallback(getAccountMismatchExpectedCallback())
                .build();

        singleAccountPCA.acquireTokenSilentAsync(silentParameters);
        RoboTestUtils.flushSchedulerWithDelay(1000);
    }

    @Test
    public void testCanGetCurrentAccountIfAlreadySignedIn() {
        singleAccountPCA.signIn(mActivity, mUsername, mScopes, getSuccessExpectedCallback());
        RoboTestUtils.flushScheduler();

        singleAccountPCA.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {

            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {

            }

            @Override
            public void onError(@NonNull MsalException exception) {

            }
        });

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
                Assert.assertEquals(exception.getErrorCode(), MsalClientException.NO_CURRENT_ACCOUNT);
            }
        };
    }

    private AuthenticationCallback getInvalidParameterExpectedCallback() {
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
                Assert.assertEquals(exception.getErrorCode(), MsalClientException.INVALID_PARAMETER);
            }
        };
    }

    private AuthenticationCallback getAccountMismatchExpectedCallback() {
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
                Assert.assertEquals(exception.getErrorCode(), MsalClientException.CURRENT_ACCOUNT_MISMATCH);
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
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        return query;
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
