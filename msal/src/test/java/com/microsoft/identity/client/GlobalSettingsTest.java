package com.microsoft.identity.client;

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.internal.testutils.TestUtils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * Tests for {@link GlobalSettings}.
 */
@RunWith(RobolectricTestRunner.class)
public class GlobalSettingsTest {
    private Context mContext;
    private Activity mActivity;
    private ISingleAccountPublicClientApplication mSingleAccountApplication;
    private IMultipleAccountPublicClientApplication mMultipleAccountApplication;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActivity = Mockito.mock(Activity.class);
        Mockito.when(mActivity.getApplicationContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        TestUtils.clearCache("com.microsoft.identity.client.account_credential_cache");
    }

    @Test
    public void testCanInitializeGlobal() {
        final File globalConfigFile = new File("src/test/res/raw/test_split_config_global_config.json");
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getSuccessGlobalListener());
    }

    @Test
    public void testCannotInitializeGlobalTwice() {
        final File globalConfigFile = new File("src/test/res/raw/test_split_config_global_config.json");
        GlobalSettings.loadGlobalConfigurationFile(globalConfigFile, getSuccessGlobalListener());

        final File anotherGlobalConfigFile = new File("src/test/res/raw/test_split_config_global_config.json");
        GlobalSettings.loadGlobalConfigurationFile(anotherGlobalConfigFile, getSecondInitFailureGlobalListener());
    }

    //region Single Account Tests
    @Test
    public void testCanCreateSingleAccountPcaWithoutGlobalInit() throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        createSAPCAFromConfigPath("src/test/res/raw/single_account_aad_test_config.json", countDownLatch);
        countDownLatch.await();
    }

    @Test
    public void testCannotInitGlobalIfSingleAccountPcaHasBeenCreated() {

    }

    @Test
    public void testCanCreateSingleAccountPcaAfterGlobalInit() {

    }

    @Test
    public void testGlobalFieldsOverrideSingleAccountPCAFields() {

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
                fail(exception.getMessage());
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

    private void createMAPCAFromConfigPath(final String configPath, IPublicClientApplication.IMultipleAccountApplicationCreatedListener listener){
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
