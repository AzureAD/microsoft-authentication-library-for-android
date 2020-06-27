package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.AbstractAcquireTokenNetworkTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.broker.BrokerAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AdfsPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class TestCase833513 extends AbstractAcquireTokenNetworkTest {

    @Test
    public void test_833513() throws MsalException, InterruptedException {
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

        // query to load a user from a different tenant that was used for WPJ
        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.FEDERATED;
        query.federationProvider = LabConstants.FederationProvider.ADFS_V3;

        final String username = LabUserHelper.loadUserForTest(query);
        String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        // try sign in with an account from a different tenant
        // passing null for latch as we don't need to receive the result from this call
        // we just want to get into the webview and look for the error in AAD page
        singleAccountPCA.signIn(mActivity, username, mScopes, successfulInteractiveCallback(null));

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHintProvided(true)
                .sessionExpected(false)
                .consentPageExpected(false)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .expectingNonZeroAccountsInBroker(false)
                .build();

        AdfsPromptHandler adfsPromptHandler = new AdfsPromptHandler(promptHandlerParameters);
        adfsPromptHandler.handlePrompt(username, password);

        // expecting error in WebView now

        final UiObject errMsg = UiAutomatorUtils.obtainUiObjectWithText("AADSTS50020");
        Assert.assertTrue(errMsg.exists());
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
        return null;
    }

    @Override
    public ITestBroker getBroker() {
        return new BrokerAuthenticator();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common;
    }


}
