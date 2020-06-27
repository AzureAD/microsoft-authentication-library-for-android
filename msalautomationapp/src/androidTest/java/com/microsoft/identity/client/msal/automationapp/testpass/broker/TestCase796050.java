package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.AbstractAcquireTokenNetworkTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.broker.BrokerAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

// Multi-accounts for Broker - Add Account in Account Picker
public class TestCase796050 extends AbstractAcquireTokenNetworkTest {

    @Test
    public void test_796050() throws InterruptedException {

        // already created test user
        final String username1 = mLoginHint;
        final String password1 = LabConfig.getCurrentLabConfig().getLabUserPassword();

        // create another temp user
        final String username2 = LabUserHelper.loadTempUser(getTempUserType());
        final String password2 = LabConfig.getCurrentLabConfig().getLabUserPassword();

        Assert.assertNotEquals(username1, username2);

        // perform device registration
        mBroker.performDeviceRegistration(
                username1, password1
        );

        final CountDownLatch latch = new CountDownLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();

        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        mBroker.handleAccountPicker(null);

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHintProvided(false)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .broker(null) // already in webview and this would behave the same as no broker
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username2, password2);

                    }
                }
        );

        interactiveRequest.execute();
        latch.await();

        // Assert Authenticator Account screen has both accounts

        mBroker.launch();

        final UiObject account1 = UiAutomatorUtils.obtainUiObjectWithText(username1);
        Assert.assertTrue(account1.exists());

        final UiObject account2 = UiAutomatorUtils.obtainUiObjectWithText(username2);
        Assert.assertTrue(account2.exists());

        // SILENT REQUEST

        final IAccount account = getAccount();

        Assert.assertEquals(username2, account.getUsername());

        final CountDownLatch silentLatch = new CountDownLatch(1);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .forceRefresh(true)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulSilentCallback(silentLatch))
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        silentLatch.await();
    }


    @Override
    public LabUserQuery getLabUserQuery() {
        return null;
    }

    @Override
    public String getTempUserType() {
        return LabConstants.TempUserType.MAMCA;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public ITestBroker getBroker() {
        return new BrokerAuthenticator();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

}
