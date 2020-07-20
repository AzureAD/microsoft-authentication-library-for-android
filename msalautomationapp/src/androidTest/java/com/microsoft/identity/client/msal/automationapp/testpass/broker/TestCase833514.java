package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class TestCase833514 extends AbstractMsalUiTest {

    @Test
    public void test_833514() throws MsalException, InterruptedException {
        // pca should be in MULTIPLE account mode starting out
        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

        //we should NOT be in shared device mode
        Assert.assertFalse(mApplication.isSharedDevice());

        // perform shared device registration
        mBroker.performSharedDeviceRegistration(
                mLoginHint, LabConfig.getCurrentLabConfig().getLabUserPassword()
        );

        // re-create PCA after device registration
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(mApplication.isSharedDevice());

        // query to load a user from the same tenant that was used for WPJ
        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.CLOUD;

        // get username and password for this account
        final String username = LabUserHelper.loadUserForTest(query);
        String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        // use azure sample app and make sure we do a fresh install
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();

        azureSampleApp.install();
        azureSampleApp.launch();
        azureSampleApp.handleFirstRun();

        final MicrosoftStsPromptHandlerParameters microsoftStsPromptHandlerParameters =
                MicrosoftStsPromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .broker(getBroker())
                        .loginHint(null)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .enrollPageExpected(false)
                        .registerPageExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .expectingLoginPageAccountPicker(false)
                        .isFederated(false)
                        .sessionExpected(false)
                        .build();

        // sign in into Azure Sample App
        azureSampleApp.signIn(username, password, getBrowser(), false, microsoftStsPromptHandlerParameters);

        // make sure we have successfully signed in
        azureSampleApp.confirmSignedIn(username);

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final CountDownLatch getAccountLatch = new CountDownLatch(1);

        final IAccount[] accounts = new IAccount[1];

        // perform get account from MSAL Automation App
        ((SingleAccountPublicClientApplication) mApplication).getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                assert activeAccount != null;
                Assert.assertEquals(activeAccount.getUsername(), username);
                accounts[0] = activeAccount;
                getAccountLatch.countDown();
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                assert currentAccount != null;
                Assert.assertEquals(currentAccount.getUsername(), username);
                accounts[0] = currentAccount;
                getAccountLatch.countDown();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail(exception.getMessage());
                getAccountLatch.countDown();
            }
        });

        getAccountLatch.await();

        final CountDownLatch silentLatch = new CountDownLatch(1);

        // perform acquire token silent with account used for get account
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .fromAuthority(getAuthority())
                .forAccount(accounts[0])
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulSilentCallback(silentLatch))
                .build();

        singleAccountPCA.acquireTokenSilentAsync(silentParameters);

        silentLatch.await();
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.userRole = LabConstants.UserRole.CLOUD_DEVICE_ADMINISTRATOR;
        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public ITestBroker getBroker() {
        return new BrokerMicrosoftAuthenticator();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

}
