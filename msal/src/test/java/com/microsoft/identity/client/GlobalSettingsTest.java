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

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.e2e.shadows.ShadowAndroidSdkStorageEncryptionManager;
import com.microsoft.identity.client.e2e.shadows.ShadowAuthorityForMockHttpResponse;
import com.microsoft.identity.client.e2e.shadows.ShadowOpenIdProviderConfigurationClient;
import com.microsoft.identity.client.e2e.shadows.ShadowPublicClientApplicationConfiguration;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.internal.testutils.shadows.ShadowHttpClient;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
import static com.microsoft.identity.internal.testutils.TestConstants.Configurations.MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.File;

/**
 * Tests for {@link GlobalSettings}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAndroidSdkStorageEncryptionManager.class,
        ShadowAuthorityForMockHttpResponse.class,
        ShadowPublicClientApplicationConfiguration.class,
        ShadowHttpClient.class,
        ShadowOpenIdProviderConfigurationClient.class
})
@LooperMode(LooperMode.Mode.LEGACY)
public class GlobalSettingsTest {
    private Context mContext;
    private ISingleAccountPublicClientApplication mSingleAccountApplication;
    private IMultipleAccountPublicClientApplication mMultipleAccountApplication;

    public static final String TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH = "src/test/res/raw/test_split_config_global_config.json";

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        final Activity mActivity = Mockito.mock(Activity.class);
        Mockito.when(mActivity.getApplicationContext()).thenReturn(mContext);
        mSingleAccountApplication = null;
        mMultipleAccountApplication = null;
    }

    @After
    public void tearDown() {
        GlobalSettings.resetInstance();
    }

    @Test
    public void testCanInitializeGlobal() {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getSuccessGlobalListener());
    }

    @Test
    public void testCannotInitializeGlobalTwice() {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getSuccessGlobalListener());

        final File anotherGlobalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, anotherGlobalConfigFile, getSecondInitFailureGlobalListener());
    }

    //region Single Account Tests
    @Test
    public void testCanCreateSingleAccountPcaWithoutGlobalInit() {
        createSAPCAFromConfigPath(SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH);
        Assert.assertNotNull(mSingleAccountApplication);
    }

    @Test
    public void testCannotInitGlobalIfSingleAccountPcaHasBeenCreated() {
        createSAPCAFromConfigPath(SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH);
        Assert.assertNotNull(mSingleAccountApplication);

        final File anotherGlobalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, anotherGlobalConfigFile, getGlobalAfterPcaFailureGlobalListener());
    }

    @Test
    public void testCanCreateSingleAccountPcaAfterGlobalInit() {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getSuccessGlobalListener());

        createSAPCAFromConfigPath(SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH);
        Assert.assertNotNull(mSingleAccountApplication);
    }

    @Test
    public void testGlobalFieldsOverrideSingleAccountPCAFields() {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getSuccessGlobalListener());

        createSAPCAFromConfigPath(SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH);
        Assert.assertNotNull(mSingleAccountApplication);

        final PublicClientApplicationConfiguration configuration = mSingleAccountApplication.getConfiguration();
        Assert.assertTrue(configuration.isWebViewZoomControlsEnabled());
        Assert.assertTrue(configuration.isWebViewZoomEnabled());
        Assert.assertTrue(configuration.getLoggerConfiguration().isPiiEnabled());
        Assert.assertFalse(configuration.getLoggerConfiguration().isLogcatEnabled());
    }
    //endregion

    //region Multiple Account Tests
    @Test
    public void testCanCreateMultipleAccountPcaWithoutGlobalInit() {
        createMAPCAFromConfigPath(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH);
        Assert.assertNotNull(mMultipleAccountApplication);
    }

    @Test
    public void testCannotInitGlobalIfMultipleAccountPcaHasBeenCreated() {
        createMAPCAFromConfigPath(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH);
        Assert.assertNotNull(mMultipleAccountApplication);

        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getGlobalAfterPcaFailureGlobalListener());
    }

    @Test
    public void testCanCreateMultipleAccountPcaAfterGlobalInit() {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getSuccessGlobalListener());

        createMAPCAFromConfigPath(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH);
        Assert.assertNotNull(mMultipleAccountApplication);
    }

    @Test
    public void testGlobalFieldsOverrideMultipleAccountPCAFields() {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(mContext, globalConfigFile, getSuccessGlobalListener());

        createMAPCAFromConfigPath(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH);
        Assert.assertNotNull(mMultipleAccountApplication);

        final PublicClientApplicationConfiguration configuration = mMultipleAccountApplication.getConfiguration();
        Assert.assertTrue(configuration.isWebViewZoomControlsEnabled());
        Assert.assertTrue(configuration.isWebViewZoomEnabled());
        Assert.assertTrue(configuration.getLoggerConfiguration().isPiiEnabled());
        Assert.assertFalse(configuration.getLoggerConfiguration().isLogcatEnabled());
    }
    //endregion

    // TODO: Once we figure out which fields should be global-only or pca-specific-only, add tests to make sure they can't be read in through other methods.

    private GlobalSettings.GlobalSettingsListener getSuccessGlobalListener() {
        return new GlobalSettings.GlobalSettingsListener() {
            @Override
            public void onSuccess(@NonNull String message) {
                // Nothing
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail(exception.getMessage());
            }
        };
    }

    private GlobalSettings.GlobalSettingsListener getSecondInitFailureGlobalListener() {
        return new GlobalSettings.GlobalSettingsListener() {
            @Override
            public void onSuccess(@NonNull String message) {
                Assert.fail("Second initialization was allowed.");
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.assertEquals(GlobalSettings.GLOBAL_ALREADY_INITIALIZED_ERROR_CODE, exception.getErrorCode());
                Assert.assertEquals(GlobalSettings.GLOBAL_ALREADY_INITIALIZED_ERROR_MESSAGE, exception.getMessage());
            }
        };
    }

    private GlobalSettings.GlobalSettingsListener getGlobalAfterPcaFailureGlobalListener() {
        return new GlobalSettings.GlobalSettingsListener() {
            @Override
            public void onSuccess(@NonNull String message) {
                Assert.fail("Second initialization was allowed.");
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.assertEquals(GlobalSettings.GLOBAL_INIT_AFTER_PCA_ERROR_CODE, exception.getErrorCode());
                Assert.assertEquals(GlobalSettings.GLOBAL_INIT_AFTER_PCA_ERROR_MESSAGE, exception.getMessage());
            }
        };
    }

    private void createSAPCAFromConfigPath(final String configPath){
        final File configFile = new File(configPath);
        PublicClientApplication.createSingleAccountPublicClientApplication(mContext, configFile, new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
            @Override
            public void onCreated(ISingleAccountPublicClientApplication application) {
                mSingleAccountApplication = application;
            }
            @Override
            public void onError(MsalException exception) {
                Assert.fail(exception.getMessage());
            }
        });

        flushScheduler();
    }

    private void createMAPCAFromConfigPath(final String configPath){
        final File configFile = new File(configPath);
        PublicClientApplication.createMultipleAccountPublicClientApplication(mContext, configFile, new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
            @Override
            public void onCreated(IMultipleAccountPublicClientApplication application) {
                mMultipleAccountApplication = application;
            }
            @Override
            public void onError(MsalException exception) {
                Assert.fail(exception.getMessage());
            }
        });
        flushScheduler();
    }
}
