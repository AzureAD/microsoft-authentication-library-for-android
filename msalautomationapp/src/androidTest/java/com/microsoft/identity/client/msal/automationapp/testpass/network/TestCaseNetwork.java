package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.NetworkTest;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.network.NetworkTestConstants;
import com.microsoft.identity.client.ui.automation.network.runners.NetworkTestRunner;
import com.microsoft.identity.client.ui.automation.reporting.Timeline;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(NetworkTestRunner.class)
public class TestCaseNetwork extends BaseMsalUiNetworkTest {

    @Override
    public void setup() {
        Timeline.start(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, "Setting up test");
        super.setup();
        Timeline.finish(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE);
    }

    @NetworkTest(
            inputFile = "input_acquireTokenWithoutBroker.csv",
            testTimeout = 120
    )
    @Test
    public void test_acquireTokenWithoutBroker() throws InterruptedException {
        Timeline.start(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, "Test for acquire token");
        TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(tokenRequestLatch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();


        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();

        tokenRequestLatch.await();

        Timeline.finish(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE);
    }

    @NetworkTest(
            inputFile = "input_acquireTokenSilentWithoutBroker.csv",
            testTimeout = 120
    )
    @Test
    public void test_acquireTokenSilentWithoutBroker() {
        final TokenRequestLatch latch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
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
                        final String username = mLoginHint;
                        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(mLoginHint)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                .build();

                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username, password);
                    }
                }
        );

        interactiveRequest.execute();
        latch.await(TokenRequestTimeout.SHORT);

        IAccount account = getAccount();

        final int numberOfTests = 10;
        for (int i = 0; i < numberOfTests; i++) {
            TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);

            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .forAccount(account)
                    .fromAuthority(account.getAuthority())
                    .forceRefresh(true)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(successfulSilentCallback(tokenRequestLatch))
                    .build();

            mApplication.acquireTokenSilentAsync(silentParameters);

            tokenRequestLatch.await(TokenRequestTimeout.SILENT);
        }
    }
}
