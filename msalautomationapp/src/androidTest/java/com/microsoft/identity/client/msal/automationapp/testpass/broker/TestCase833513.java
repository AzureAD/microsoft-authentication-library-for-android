package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.AcquireTokenNetworkAbstractTest;
import com.microsoft.identity.client.msal.automationapp.ErrorCodes;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.broker.BrokerAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.MicrosoftPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

@Ignore
public class TestCase833513 extends AcquireTokenNetworkAbstractTest {

    @Test
    public void test_833513() throws MsalException, InterruptedException {
        // pca should be in MULTIPLE account mode starting
        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

        //we should NOT be in shared device mode
        Assert.assertFalse(mApplication.isSharedDevice());

        // perform shared device registration
        mBroker.performSharedDeviceRegistration(
                mUsername, LabConfig.getCurrentLabConfig().getLabUserPassword()
        );

        // re-create PCA after device registration
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(mApplication.isSharedDevice());

        // query to load a user from a different tenant (different from the one used to perform
        // shared device registration)
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_GERMANY_CLOUD;

        final String userFromDifferentTenant = LabUserHelper.loadUserForTest(query);
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final CountDownLatch latch = new CountDownLatch(1);

        // try sign in with an account from a different tenant
        singleAccountPCA.signIn(mActivity, userFromDifferentTenant, mScopes, failureInteractiveCallback(
                latch, ErrorCodes.UNKNOWN_ERROR_CODE, mContext
        ));

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHintProvided(true)
                .sessionExpected(false)
                .consentPageExpected(false)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .expectingNonZeroAccountsInBroker(false)
                .build();

        MicrosoftPromptHandler microsoftPromptHandler = new MicrosoftPromptHandler(promptHandlerParameters);
        microsoftPromptHandler.handlePrompt(userFromDifferentTenant, password);

        latch.await();
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
