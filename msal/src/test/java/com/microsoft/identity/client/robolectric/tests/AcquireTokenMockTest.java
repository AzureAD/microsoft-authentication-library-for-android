package com.microsoft.identity.client.robolectric.tests;

import android.app.Activity;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.robolectric.shadows.ShadowAuthority;
import com.microsoft.identity.client.robolectric.shadows.ShadowHttpRequest;
import com.microsoft.identity.client.robolectric.shadows.ShadowMsalUtils;
import com.microsoft.identity.client.robolectric.shadows.ShadowStorageHelper;
import com.microsoft.identity.client.robolectric.shadows.ShadowStrategyResultServerError;
import com.microsoft.identity.client.robolectric.shadows.ShadowStrategyResultUnsuccessful;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.internal.testutils.authorities.MockAuthority;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2TokenCache;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResponse;
import com.microsoft.identity.internal.testutils.MockTokenResponse;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static junit.framework.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowStorageHelper.class, ShadowAuthority.class, ShadowHttpRequest.class, ShadowMsalUtils.class})
public final class AcquireTokenMockTest {

    private static final String[] SCOPES = {"user.read"};
    private static final String AAD_MOCK_AUTHORITY = "https://test.authority/mock";

    @Test
    public void canAcquireToken() {
        new AcquireTokenBaseTest() {

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
        new AcquireTokenBaseTest() {

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
        new AcquireTokenBaseTest() {

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
        new AcquireTokenBaseTest() {
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
        new AcquireTokenBaseTest() {

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
        new AcquireTokenBaseTest() {

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
        new AcquireTokenBaseTest() {

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
    public void silentWorksWhenCacheHasExpiredAccessToken() {
        new AcquireTokenBaseTest() {

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

                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                RoboTestUtils.flushScheduler();
            }

        }.performTest();
    }

    @Test
    @Config(shadows = {ShadowStrategyResultUnsuccessful.class})
    public void acquireTokenFailsIfTokenRequestIsNotSuccessful() {
        new AcquireTokenBaseTest() {

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
    @Config(shadows = {ShadowStrategyResultServerError.class})
    public void acquireTokenFailsIfServerErrorOccurs() {
        new AcquireTokenBaseTest() {

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
    @Ignore
    public void acquireTokenFailsIfScopeNotProvided() {
        new AcquireTokenBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) {

                String username = "fake@test.com";

                AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(activity)
                        .withLoginHint(username)
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
