package com.microsoft.identity.client.robolectric;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.shadows.ShadowAuthority;
import com.microsoft.identity.client.shadows.ShadowHttpRequest;
import com.microsoft.identity.client.shadows.ShadowStorageHelper;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.MockAuthority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.common.internal.testutils.MockTokenResponse;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.msal.R;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.Scheduler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.internal.cache.CacheKeyValueDelegate.CACHE_VALUE_SEPARATOR;
import static junit.framework.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowHttpRequest.class})
public final class PublicClientApplicationMockTest {

    private static final String[] SCOPES = {"user.read", "openid", "offline_access", "profile"};
    private static final String AAD_MOCK_AUTHORITY = "https://test.authority/aad.mock";

    @Test
    public void canGetAccount() {
        final Context context = ApplicationProvider.getApplicationContext();

        PublicClientApplication.create(context, R.raw.test_msal_config_multiple_account, new PublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                final IAccount account = loadAccountForTest(application);
                Assert.assertTrue(account != null);
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        });
    }

    @Test
    public void canPerformROPC() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                String username = "fake@test.com";

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
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
    public void acquireTokenFailsIfLoginHintNotProvidedForRopc() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withScopes(Arrays.asList(SCOPES))
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                fail("Unexpected Success");
                            }

                            @Override
                            public void onError(MsalException exception) {
                                Assert.assertTrue(true);
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
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                String username = "fake@test.com";

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
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
                        .fromAuthority(AAD_MOCK_AUTHORITY)
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
    public void canAcquireSilentIfValidTokensAvailableInCache() {
        new PublicClientApplicationBaseTest() {
            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {
                final IAccount account = loadAccountForTest(publicClientApplication);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
                        .forAccount(account)
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

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void forceRefreshWorks() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {
                final IAccount account = loadAccountForTest(publicClientApplication);
                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(true)
                        .forAccount(account)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
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

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void silentCallFailsIfAccountNotProvided() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
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

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void silentCallFailsIfCacheIsEmpty() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {
                final IAccount account = loadAccountForTest(publicClientApplication);
                RoboTestUtils.clearCache();

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .forAccount(account)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
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

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void silentWorksWhenCacheHasNoAccessToken() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {
                final IAccount account = loadAccountForTest(publicClientApplication);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .forAccount(account)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
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

                RoboTestUtils.removeAccessTokenFromCache();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void silentWorksWhenCacheHasExpiredAccessToken() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {
                ICacheRecord cacheRecord = createDataInCacheWithExpiredAccessToken(publicClientApplication);
                final String loginHint = cacheRecord.getAccount().getUsername();
                final IAccount account = performGetAccount(publicClientApplication, loginHint);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .withScopes(Arrays.asList(SCOPES))
                        .forceRefresh(false)
                        .forAccount(account)
                        .fromAuthority(AAD_MOCK_AUTHORITY)
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

                RoboTestUtils.removeAccessTokenFromCache();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }


    private ICacheRecord saveTokens(TokenResponse tokenResponse, IPublicClientApplication application) throws ClientException {
        OAuth2TokenCache tokenCache = application.getConfiguration().getOAuth2TokenCache();
        String clientId = application.getConfiguration().getClientId();
        Authority authority = new MockAuthority();
        OAuth2Strategy strategy = authority.createOAuth2Strategy();
        MicrosoftStsAuthorizationRequest fakeAuthRequest = Mockito.mock(MicrosoftStsAuthorizationRequest.class);
        Mockito.when(fakeAuthRequest.getAuthority()).thenReturn(authority.getAuthorityURL());
        Mockito.when(fakeAuthRequest.getClientId()).thenReturn(clientId);
        return tokenCache.save(strategy, fakeAuthRequest, tokenResponse);
    }

    private IAccount performGetAccount(IPublicClientApplication application, final String loginHint) {
        final IAccount[] requestedAccount = {null};
        final IMultipleAccountPublicClientApplication multipleAcctApp = (IMultipleAccountPublicClientApplication) application;
        multipleAcctApp.getAccount(
                loginHint.trim(),
                new IMultipleAccountPublicClientApplication.GetAccountCallback() {
                    @Override
                    public void onTaskCompleted(final IAccount account) {
                        if (account != null) {
                            requestedAccount[0] = account;
                        } else {
                            fail("No account found matching identifier");
                        }
                    }

                    @Override
                    public void onError(final MsalException exception) {
                        fail("No account found matching identifier");
                    }
                });
        RoboTestUtils.flushScheduler();
        return requestedAccount[0];
    }

    private ICacheRecord createDataInCache(IPublicClientApplication application) {
        ICacheRecord cacheRecord = null;
        final TokenResponse tokenResponse = MockTokenResponse.getTokenResponse();

        try {
            cacheRecord = saveTokens(tokenResponse, application);
        } catch (ClientException e) {
            fail("Unable to save tokens to cache: " + e.getMessage());
        }

        return cacheRecord;
    }

    private ICacheRecord createDataInCacheWithExpiredAccessToken(IPublicClientApplication application) {
        ICacheRecord cacheRecord = null;
        final TokenResponse tokenResponse = MockTokenResponse.getTokenResponseWithExpiredAccessToken();

        try {
            cacheRecord = saveTokens(tokenResponse, application);
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
