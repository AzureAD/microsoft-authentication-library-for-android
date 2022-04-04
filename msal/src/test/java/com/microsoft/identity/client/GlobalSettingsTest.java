package com.microsoft.identity.client;

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static org.junit.Assert.fail;

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
import java.util.concurrent.CountDownLatch;

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
    private Activity mActivity;
    private ISingleAccountPublicClientApplication mSingleAccountApplication;
    private IMultipleAccountPublicClientApplication mMultipleAccountApplication;

    public static final String TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH = "src/test/res/raw/test_split_config_global_config.json";

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActivity = Mockito.mock(Activity.class);
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
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getSuccessGlobalListener());
    }

    @Test
    public void testCannotInitializeGlobalTwice() {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getSuccessGlobalListener());

        final File anotherGlobalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(anotherGlobalConfigFile, getSecondInitFailureGlobalListener());
    }

    //region Single Account Tests
    @Test
    public void testCanCreateSingleAccountPcaWithoutGlobalInit() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        createSAPCAFromConfigPath(SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH, countDownLatch);
        countDownLatch.await();
        Assert.assertNotNull(mSingleAccountApplication);
    }

    @Test
    public void testCannotInitGlobalIfSingleAccountPcaHasBeenCreated() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        createSAPCAFromConfigPath(SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH, countDownLatch);
        countDownLatch.await();
        Assert.assertNotNull(mSingleAccountApplication);

        final File anotherGlobalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(anotherGlobalConfigFile, getGlobalAfterPcaFailureGlobalListener());
    }

    @Test
    public void testCanCreateSingleAccountPcaAfterGlobalInit() throws InterruptedException {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getSuccessGlobalListener());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        createSAPCAFromConfigPath(SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH, countDownLatch);
        countDownLatch.await();
        Assert.assertNotNull(mSingleAccountApplication);
    }

    @Test
    public void testGlobalFieldsOverrideSingleAccountPCAFields() throws InterruptedException {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getSuccessGlobalListener());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        createSAPCAFromConfigPath(SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH, countDownLatch);
        countDownLatch.await();
        Assert.assertNotNull(mSingleAccountApplication);

        final PublicClientApplicationConfiguration configuration = mSingleAccountApplication.getConfiguration();
        Assert.assertFalse(configuration.getMultipleCloudsSupported());
        Assert.assertFalse(configuration.getUseBroker());
        Assert.assertTrue(configuration.isWebViewZoomControlsEnabled());
        Assert.assertTrue(configuration.isWebViewZoomEnabled());
        Assert.assertTrue(configuration.getLoggerConfiguration().isPiiEnabled());
        Assert.assertFalse(configuration.getLoggerConfiguration().isLogcatEnabled());
    }
    //endregion

    //region Multiple Account Tests
    @Test
    public void testCanCreateMultipleAccountPcaWithoutGlobalInit() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        createMAPCAFromConfigPath(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH, countDownLatch);
        countDownLatch.await();
        Assert.assertNotNull(mMultipleAccountApplication);
    }

    @Test
    public void testCannotInitGlobalIfMultipleAccountPcaHasBeenCreated() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        createMAPCAFromConfigPath(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH, countDownLatch);
        countDownLatch.await();
        Assert.assertNotNull(mMultipleAccountApplication);

        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getGlobalAfterPcaFailureGlobalListener());
    }

    @Test
    public void testCanCreateMultipleAccountPcaAfterGlobalInit() throws InterruptedException {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getSuccessGlobalListener());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        createMAPCAFromConfigPath(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH, countDownLatch);
        countDownLatch.await();
        Assert.assertNotNull(mMultipleAccountApplication);
    }

    @Test
    public void testGlobalFieldsOverrideMultipleAccountPCAFields() throws InterruptedException {
        final File globalConfigFile = new File(TEST_SPLIT_CONFIG_GLOBAL_CONFIG_FILE_PATH);
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getSuccessGlobalListener());

        CountDownLatch countDownLatch = new CountDownLatch(1);
        createMAPCAFromConfigPath(MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH, countDownLatch);
        countDownLatch.await();
        Assert.assertNotNull(mMultipleAccountApplication);

        final PublicClientApplicationConfiguration configuration = mMultipleAccountApplication.getConfiguration();
        Assert.assertFalse(configuration.getMultipleCloudsSupported());
        Assert.assertFalse(configuration.getUseBroker());
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
                Assert.assertEquals(exception.getErrorCode(), GlobalSettings.GLOBAL_ALREADY_INITIALIZED_ERROR_CODE);
                Assert.assertEquals(exception.getMessage(), GlobalSettings.GLOBAL_ALREADY_INITIALIZED_ERROR_MESSAGE);
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
                Assert.assertEquals(exception.getErrorCode(), GlobalSettings.GLOBAL_INIT_AFTER_PCA_ERROR_CODE);
                Assert.assertEquals(exception.getMessage(), GlobalSettings.GLOBAL_INIT_AFTER_PCA_ERROR_MESSAGE);
            }
        };
    }

    private void createSAPCAFromConfigPath(final String configPath, final CountDownLatch countDownLatch){
        final File configFile = new File(configPath);
        PublicClientApplication.createSingleAccountPublicClientApplication(mContext, configFile, new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
            @Override
            public void onCreated(ISingleAccountPublicClientApplication application) {
                mSingleAccountApplication = application;
                countDownLatch.countDown();
            }
            @Override
            public void onError(MsalException exception) {
                Assert.fail(exception.getMessage());
            }
        });

        flushScheduler();
    }

    private void createMAPCAFromConfigPath(final String configPath, final CountDownLatch countDownLatch){
        final File configFile = new File(configPath);
        PublicClientApplication.createMultipleAccountPublicClientApplication(mContext, configFile, new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
            @Override
            public void onCreated(IMultipleAccountPublicClientApplication application) {
                mMultipleAccountApplication = application;
                countDownLatch.countDown();
            }
            @Override
            public void onError(MsalException exception) {
                Assert.fail(exception.getMessage());
            }
        });
        flushScheduler();
    }
}
