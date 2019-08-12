package com.microsoft.identity.client.roboelectric;

import android.app.Activity;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.utilities.TestConfigurationHelper;
import com.microsoft.identity.common.utilities.TestConfigurationQuery;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.util.Scheduler;

import java.util.Arrays;

import static junit.framework.Assert.fail;

@RunWith(RobolectricTestRunner.class)
public final class PublicClientApplicationIntegrationTest {

    private static final String[] SCOPES = {"user.read", "openid", "offline_access", "profile"};
    private static final String AAD_ROPC_TEST_AUTHORITY = "https://test.authority/aad.ropc";

    private void flushScheduler() {
        Scheduler scheduler = ShadowLooper.getShadowMainLooper().getScheduler();
        while (!scheduler.advanceToLastPostedRunnable()) ;
    }

    @Test
    public void canPerformROPC() {
        new PublicClientApplicationBaseTest() {

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
                        .fromAuthority(AAD_ROPC_TEST_AUTHORITY)
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
            }

        }.performTest();
    }

    @Test
    public void acquireSilentAfterwards() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                String username = TestConfigurationHelper.getUpnForTest(query);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .fromAuthority(AAD_ROPC_TEST_AUTHORITY)
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
                        .fromAuthority(AAD_ROPC_TEST_AUTHORITY)
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                                try {
                                    silentParameters.setAccount(authenticationResult.getAccount());
                                    publicClientApplication.acquireTokenSilentAsync(silentParameters);
                                    flushScheduler();
                                } catch (Exception e) {
                                    fail(e.getMessage());
                                }

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
            }

        }.performTest();
    }
}
