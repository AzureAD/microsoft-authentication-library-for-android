package com.microsoft.identity.client.roboelectric;

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
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.dto.CredentialType;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.shadows.ShadowStorageHelper;
import com.microsoft.identity.common.utilities.TestConfigurationHelper;
import com.microsoft.identity.common.utilities.TestConfigurationQuery;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
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
@Config(shadows = {ShadowStorageHelper.class})
public final class PublicClientApplicationIntegrationTest {

    private static final String[] SCOPES = {"user.read", "openid", "offline_access", "profile"};
    private static final String AAD_ROPC_TEST_AUTHORITY = "https://test.authority/aad.ropc";

    private void flushScheduler() {
        final Scheduler scheduler = RuntimeEnvironment.getMasterScheduler();
        while (!scheduler.advanceToLastPostedRunnable());
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
                flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void canAcquireSilentAfterGettingToken() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) throws InterruptedException {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                final String username = TestConfigurationHelper.getUpnForTest(query);

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
                flushScheduler();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void forceRefreshWorks() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) throws InterruptedException {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                final String username = TestConfigurationHelper.getUpnForTest(query);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .fromAuthority(AAD_ROPC_TEST_AUTHORITY)
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
                        .fromAuthority(AAD_ROPC_TEST_AUTHORITY)
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
                flushScheduler();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void silentCallFailsIfCacheIsEmpty() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) throws InterruptedException {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                final String username = TestConfigurationHelper.getUpnForTest(query);

                final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                        .fromAuthority(AAD_ROPC_TEST_AUTHORITY)
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
                        .fromAuthority(AAD_ROPC_TEST_AUTHORITY)
                        .callback(new AuthenticationCallback() {
                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertTrue(!StringUtil.isEmpty(authenticationResult.getAccessToken()));
                                IAccount account = authenticationResult.getAccount();
                                silentParameters.setAccount(account);
                                final Context context = ApplicationProvider.getApplicationContext();
                                String prefName = "com.microsoft.identity.client.account_credential_cache";
                                SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.clear();
                                editor.commit();
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
                flushScheduler();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                flushScheduler();
            }

        }.performTest();
    }

    @Test
    public void silentWorksWhenCacheHasNoAccessToken() {
        new PublicClientApplicationBaseTest() {

            @Override
            void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                      final Activity activity) throws InterruptedException {

                TestConfigurationQuery query = new TestConfigurationQuery();
                query.userType = "Member";
                query.isFederated = false;
                query.federationProvider = "ADFSv4";

                final String username = TestConfigurationHelper.getUpnForTest(query);

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
                                IAccount account = authenticationResult.getAccount();
                                silentParameters.setAccount(account);
                                final Context context = ApplicationProvider.getApplicationContext();
                                String prefName = "com.microsoft.identity.client.account_credential_cache";
                                SharedPreferences sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                                final Map<String, ?> cacheValues = sharedPreferences.getAll();
                                final String keyToRemove = getKeyToBeRemoved(cacheValues);
                                if (keyToRemove != null) {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.remove(keyToRemove);
                                    editor.commit();
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
                flushScheduler();
                publicClientApplication.acquireTokenSilentAsync(silentParameters);
                flushScheduler();
            }

        }.performTest();
    }


    public String getKeyToBeRemoved(Map<String, ?> cacheValues) {
        for (Map.Entry<String, ?> cacheValue : cacheValues.entrySet()) {
            final String cacheKey = cacheValue.getKey();
            if (isAccessToken(cacheKey)) {
                return cacheKey;
            }
        }

        return null;
    }


    /**
     * Inspects the supplied cache key to determine the target CredentialType.
     *
     * @param cacheKey The cache key to inspect.
     * @return The CredentialType or null if a proper type cannot be resolved.
     */
    @Nullable
    private CredentialType getCredentialTypeForCredentialCacheKey(@NonNull final String cacheKey) {
        if (StringExtensions.isNullOrBlank(cacheKey)) {
            throw new IllegalArgumentException("Param [cacheKey] cannot be null.");
        }

        final Set<String> credentialTypesLowerCase = new HashSet<>();

        for (final String credentialTypeStr : CredentialType.valueSet()) {
            credentialTypesLowerCase.add(credentialTypeStr.toLowerCase(Locale.US));
        }

        CredentialType type = null;
        for (final String credentialTypeStr : credentialTypesLowerCase) {
            if (cacheKey.contains(CACHE_VALUE_SEPARATOR + credentialTypeStr + CACHE_VALUE_SEPARATOR)) {
                if (credentialTypeStr.equalsIgnoreCase(CredentialType.AccessToken.name())) {
                    type = CredentialType.AccessToken;
                    break;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.RefreshToken.name())) {
                    type = CredentialType.RefreshToken;
                    break;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.IdToken.name())) {
                    type = CredentialType.IdToken;
                    break;
                } else if (credentialTypeStr.equalsIgnoreCase(CredentialType.V1IdToken.name())) {
                    type = CredentialType.V1IdToken;
                    break;
                }
            }
        }

        return type;
    }

    private boolean isAccessToken(@NonNull final String cacheKey) {
        boolean isAccessToken = CredentialType.AccessToken == getCredentialTypeForCredentialCacheKey(cacheKey);;
        return isAccessToken;
    }


}
