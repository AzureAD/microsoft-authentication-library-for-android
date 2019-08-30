package com.microsoft.identity.client.robolectric.tests;

import android.app.Activity;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.robolectric.shadows.ShadowAuthority;
import com.microsoft.identity.client.robolectric.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.robolectric.shadows.ShadowStorageHelper;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.utilities.TestConfigurationHelper;
import com.microsoft.identity.common.utilities.TestConfigurationQuery;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static junit.framework.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowMsalUtils.class})
public final class AcquireTokenNetworkTest {

    private static final String[] SCOPES = {"user.read", "openid", "offline_access", "profile"};

    @Test
    public void canPerformROPC() {
        new AcquireTokenBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                String username = TestConfigurationHelper.getUpnForTest(query);

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void canAcquireSilentAfterGettingToken() {
        new AcquireTokenBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                final String username = TestConfigurationHelper.getUpnForTest(query);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                                IAccount account = authenticationResult.getAccount();
                                silentParameters.setAccount(account);
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void forceRefreshWorks() {
        new AcquireTokenBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                final String username = TestConfigurationHelper.getUpnForTest(query);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(true)
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                                IAccount account = authenticationResult.getAccount();
                                silentParameters.setAccount(account);
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void silentCallFailsIfCacheIsEmpty() {
        new AcquireTokenBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                final String username = TestConfigurationHelper.getUpnForTest(query);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                fail("Unexpected success");
                            }

                            @Override
                            public void onError(MsalException exception) {
                                Assert.assertTrue(exception instanceof MsalClientException);
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                                IAccount account = authenticationResult.getAccount();
                                silentParameters.setAccount(account);
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
                RoboTestUtils.clearCache();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void silentWorksWhenCacheHasNoAccessToken() {
        new AcquireTokenBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                final String username = TestConfigurationHelper.getUpnForTest(query);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                                IAccount account = authenticationResult.getAccount();
                                silentParameters.setAccount(account);
                            }

                            @Override
                            public void onError(MsalException exception) {
                                fail(exception.getMessage());
                            }

                            @Override
                            public void onCancel() {
                                fail("User cancelled flow");
                            }
                        })
                        .build();

                publicClientApplication.acquireToken(parameters);
                RoboTestUtils.flushScheduler();
                RoboTestUtils.removeAccessTokenFromCache();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }


}
