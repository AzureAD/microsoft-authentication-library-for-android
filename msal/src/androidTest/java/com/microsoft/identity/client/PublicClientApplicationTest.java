//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.internal.MsalUtils;
import com.microsoft.identity.common.adal.internal.AuthenticationSettings;
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory;
import com.microsoft.identity.msal.test.R;
import com.microsoft.identity.nativeauth.INativeAuthPublicClientApplication;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

/**
 * Tests for {@link PublicClientApplication}.
 */
@RunWith(AndroidJUnit4.class)
public final class PublicClientApplicationTest {
    private Context mAppContext;
    private static final String CLIENT_ID = "client-id";
    private static final String[] SCOPE = {"scope1", "scope2"};
    private static final String CIAM_AUTHORITY = "https://msidlabciam1.ciamlogin.com/msidlabciam1.onmicrosoft.com";
    public static final String TEST_REDIRECT_URI = "msauth://com.microsoft.identity.client.sample.local/signature";
    private final List<String> CHALLENGE_TYPES = Arrays.asList("oob", "password");

    @Before
    public void setUp() {
        System.setProperty(
                "dexmaker.dexcache",
                androidx.test.platform.app.InstrumentationRegistry
                        .getInstrumentation()
                        .getTargetContext()
                        .getCacheDir()
                        .getPath()
        );

        System.setProperty(
                "org.mockito.android.target",
                ApplicationProvider
                        .getApplicationContext()
                        .getCacheDir()
                        .getPath()
        );

        mAppContext = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getApplicationContext();
    }

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        AndroidTestUtil.removeAllTokens(mAppContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigValidationFailsOnEmptyRedirect() throws MsalException, InterruptedException {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        final IMultipleAccountPublicClientApplication app = PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                R.raw.test_pcaconfig_empty_redirect
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConfigValidationFailsOnEmptyClientId() throws MsalException, InterruptedException {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        final IMultipleAccountPublicClientApplication app = PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                R.raw.test_pcaconfig_empty_clientid
        );
    }

    @Test
    public void testSingleAccountConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        try {
            final ISingleAccountPublicClientApplication app = PublicClientApplication.createSingleAccountPublicClientApplication(
                    context,
                    R.raw.test_msal_config_single_account
            );
            Assert.assertTrue(app instanceof ISingleAccountPublicClientApplication);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MsalException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultipleAccountConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        try {
            final IMultipleAccountPublicClientApplication app = PublicClientApplication.createMultipleAccountPublicClientApplication(
                    context,
                    R.raw.test_msal_config_multiple_account
            );
            Assert.assertTrue(app instanceof IMultipleAccountPublicClientApplication);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (MsalException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMultipleAccountAsyncConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                R.raw.test_msal_config_multiple_account,
                new PublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(IMultipleAccountPublicClientApplication application) {
                        Assert.assertTrue(application instanceof IMultipleAccountPublicClientApplication);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Assert.assertTrue(false);
                    }
                });
    }

    @Test
    public void testFailingMultipleAccountAsyncConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                R.raw.test_msal_config_single_account,
                new PublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(IMultipleAccountPublicClientApplication application) {
                        Assert.assertTrue(false);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Assert.assertTrue("Expecting error.", true);
                    }
                });
    }

    @Test
    public void testSingleAccountAsyncConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.test_msal_config_single_account,
                new PublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        Assert.assertTrue(application instanceof ISingleAccountPublicClientApplication);
                    }

                    @Override
                    public void onError(MsalException exception) {
                    }
                });
    }

    @Test
    public void testFailingSingleAccountAsyncConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.test_msal_config_multiple_account,
                new PublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        Assert.assertTrue(false);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Assert.assertTrue("Expecting error.", true);
                    }
                });
    }

    @Test
    public void testMultipleAccountCIAMAuthorityAsyncConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        try {
            final IMultipleAccountPublicClientApplication app = PublicClientApplication.createMultipleAccountPublicClientApplication(
                    context,
                    R.raw.test_msal_config_ciam_multiple_account
            );
            Assert.assertTrue(app instanceof IMultipleAccountPublicClientApplication);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (MsalException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNativeAuthConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        try {
            final INativeAuthPublicClientApplication app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    R.raw.test_msal_config_native_auth
            );
            Assert.assertNotNull(app);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (MsalException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNativeAuthConstructorNoMetadata() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        try {
            final INativeAuthPublicClientApplication app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    CLIENT_ID,
                    CIAM_AUTHORITY,
                    null,
                    CHALLENGE_TYPES
            );
            Assert.assertNotNull(app);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (MsalException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testFailingNativeAuthConstructor() {
        final Context context = new PublicClientApplicationTest.MockContext(mAppContext);
        mockPackageManagerWithDefaultFlag(context);
        mockHasCustomTabRedirect(context);

        // Should fail, as we are attempting to create a multiple account application
        try {
            final INativeAuthPublicClientApplication app = PublicClientApplication.createNativeAuthPublicClientApplication(
                    context,
                    R.raw.test_msal_config_multiple_account
            );
            Assert.fail("Unintentionally created app");
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        } catch (MsalException e) {
            Assert.assertEquals("Expecting error.",
                    e.getErrorCode(),
                    MsalClientException.NATIVE_AUTH_INVALID_ACCOUNT_MODE_CONFIG_ERROR_CODE);
        }
    }

    /**
     * Verify correct exception is thrown if callback is not provided.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCallBackEmpty() throws PackageManager.NameNotFoundException, MsalClientException {
        final Context context = new MockContext(mAppContext);
        mockPackageManagerWithClientId(context, null, CLIENT_ID);
        mockHasCustomTabRedirect(context);
        mockPackageManagerWithDefaultFlag(context);

        final PublicClientApplicationConfiguration config = PublicClientApplicationConfigurationFactory.initializeConfiguration(context);
        config.setRedirectUri(TEST_REDIRECT_URI);
        config.setClientId(CLIENT_ID);
        final PublicClientApplication application = new PublicClientApplication(config);
        application.acquireToken(getActivity(context), SCOPE, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testInternetPermissionMissing() throws PackageManager.NameNotFoundException, MsalClientException {
        final Context context = new MockContext(mAppContext);
        final PackageManager packageManager = context.getPackageManager();
        mockPackageManagerWithClientId(context, null, CLIENT_ID);
        mockHasCustomTabRedirect(context);
        mockPackageManagerWithDefaultFlag(context);

        Mockito.when(packageManager.checkPermission(Mockito.refEq("android.permission.INTERNET"),
                Mockito.refEq(mAppContext.getPackageName()))).thenReturn(PackageManager.PERMISSION_DENIED);

        PublicClientApplicationConfiguration config = PublicClientApplicationConfigurationFactory.initializeConfiguration(context);
        config.setRedirectUri(TEST_REDIRECT_URI);
        config.setClientId(CLIENT_ID);

        new PublicClientApplication(config);
    }


    @Test
    public void testSecretKeysAreSet() throws NoSuchAlgorithmException, InvalidKeySpecException, MsalClientException {
        final Context context = new MockContext(mAppContext);
        mockHasCustomTabRedirect(context);
        mockPackageManagerWithDefaultFlag(context);

        final PublicClientApplicationConfiguration config = PublicClientApplicationConfigurationFactory.initializeConfiguration(context);
        config.setRedirectUri(TEST_REDIRECT_URI);
        config.setClientId(CLIENT_ID);
        final PublicClientApplication pca = new PublicClientApplication(config);
        final PublicClientApplicationConfiguration appConfig = pca.getConfiguration();

        SecretKeyFactory keyFactory = SecretKeyFactory
                .getInstance("PBEWithSHA256And256BitAES-CBC-BC");
        SecretKey generatedSecretKey = keyFactory.generateSecret(
                new PBEKeySpec(
                        "test_password".toCharArray(),
                        "byte-code-for-your-salt".getBytes(),
                        100,
                        256
                )
        );
        SecretKey secretKey = new SecretKeySpec(generatedSecretKey.getEncoded(), "AES");
        final byte[] encodedSecretKey = secretKey.getEncoded();

        appConfig.setTokenCacheSecretKeys(encodedSecretKey);

        // Check that the AuthenticationSettings.INSTANCE.secretKey matches the value configured
        assertEquals(
                encodedSecretKey,
                AuthenticationSettings.INSTANCE.getSecretKeyData()
        );
    }

    private void mockPackageManagerWithClientId(final Context context, final String alternateAuthorityInManifest,
                                                final String clientId)
            throws PackageManager.NameNotFoundException {
        final PackageManager mockedPackageManager = context.getPackageManager();
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        // meta data is empty, no client id there.
        applicationInfo.metaData = new Bundle();
        if (!MsalUtils.isEmpty(clientId)) {
            applicationInfo.metaData.putString("com.microsoft.identity.client.ClientId", clientId);
        }

        if (!MsalUtils.isEmpty(alternateAuthorityInManifest)) {
            applicationInfo.metaData.putString("com.microsoft.identity.client.AuthorityMetadata", alternateAuthorityInManifest);
        }

        Mockito.when(mockedPackageManager.getApplicationInfo(
                Mockito.refEq(mAppContext.getPackageName()), Mockito.eq(
                        PackageManager.GET_META_DATA))).thenReturn(applicationInfo);

        final PackageInfo mockedPackageInfo = Mockito.mock(PackageInfo.class);
        Mockito.when(mockedPackageManager.getPackageInfo(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockedPackageInfo);
    }

    private void mockPackageManagerWithDefaultFlag(final Context context) {
        // This is to bypass telemetry initialization code.
        try {
            final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
            Mockito.when(context.getPackageManager().getApplicationInfo(mAppContext.getPackageName(), 0))
                    .thenReturn(applicationInfo);

            final PackageInfo packageInfo = Mockito.mock(PackageInfo.class);
            Mockito.when(context.getPackageManager().getPackageInfo(mAppContext.getPackageName(), 0))
                    .thenReturn(packageInfo);

        } catch (Exception e) {
            fail();
        }
    }

    private void mockHasCustomTabRedirect(final Context context) {
        final PackageManager packageManager = context.getPackageManager();

        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        Mockito.when(packageManager.queryIntentActivities(Matchers.any(Intent.class),
                Matchers.eq(PackageManager.GET_RESOLVED_FILTER))).thenReturn(resolveInfos);

        final ResolveInfo mockedResolveInfo1 = Mockito.mock(ResolveInfo.class);
        final ActivityInfo mockedActivityInfo1 = Mockito.mock(ActivityInfo.class);
        mockedActivityInfo1.name = BrowserTabActivity.class.getName();
        mockedActivityInfo1.packageName = context.getPackageName();
        mockedResolveInfo1.activityInfo = mockedActivityInfo1;
        resolveInfos.add(mockedResolveInfo1);
    }

    private Activity getActivity(final Context context) {
        final Activity mockedActivity = Mockito.mock(Activity.class);
        Mockito.when(mockedActivity.getApplicationContext()).thenReturn(context);

        return mockedActivity;
    }

    public static class MockContext extends ContextWrapper {
        private final PackageManager mPackageManager;
        private final ConnectivityManager mConnectivityManager;

        MockContext(final Context context) {
            super(context);
            mPackageManager = Mockito.mock(PackageManager.class);
            mConnectivityManager = Mockito.mock(ConnectivityManager.class);
        }

        @Override
        public PackageManager getPackageManager() {
            return mPackageManager;
        }

        @Override
        public Object getSystemService(String name) {
            if (Context.CONNECTIVITY_SERVICE.equals(name)) {
                return mConnectivityManager;
            }

            return super.getSystemService(name);
        }
    }
}