package com.microsoft.identity.client.msal.automationapp.testpass.local;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.UiResponse;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestCase497038 extends BrokerLessMsalTest {

    @Test
    public void test_497038() throws InterruptedException {
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();

        azureSampleApp.install();
        azureSampleApp.launch();
        azureSampleApp.handleFirstRun();

        final MicrosoftStsPromptHandlerParameters microsoftStsPromptHandlerParameters =
                MicrosoftStsPromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .broker(getBroker())
                        .loginHint(null)
                        .consentPageExpected(true)
                        .consentPageResponse(UiResponse.ACCEPT)
                        .speedBumpExpected(false)
                        .enrollPageExpected(false)
                        .registerPageExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .expectingLoginPageAccountPicker(false)
                        .isFederated(false)
                        .sessionExpected(false)
                        .build();

        azureSampleApp.signIn(username, password, getBrowser(), true, microsoftStsPromptHandlerParameters);

        Thread.sleep(TimeUnit.SECONDS.toMillis(5));

        azureSampleApp.confirmSignedIn(username);

        // NOW LOGIN INTO MSAL AUTOMATION APP

        final CountDownLatch latch = new CountDownLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(username)
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
                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .broker(getBroker())
                                .loginHint(username)
                                .sessionExpected(true)
                                .consentPageExpected(false)
                                .speedBumpExpected(true)
                                .speedBumpResponse(UiResponse.ACCEPT)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await();
    }


    @Override
    public LabUserQuery getLabUserQuery() {
        return null;
    }

    @Override
    public String getTempUserType() {
        return LabConstants.TempUserType.BASIC;
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
    public int getConfigFileResourceId() {
        return R.raw.msal_config_browser;
    }

}
