package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
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
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(NetworkTestRunner.class)
public class TestCaseNetwork extends BaseMsalUiNetworkTest {

    @NetworkTest(inputFile = "input_acquireTokenWithoutBroker.csv", testTimeout = 120)
    @Test
    public void test_acquireTokenWithoutBroker() throws InterruptedException {
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
    }

    @NetworkTest(inputFile = "input_acquireTokenSilentlyWithoutBroker.csv", testTimeout = 120)
    @Test
    public void test_acquireTokenSilentWithoutBroker() throws InterruptedException {
        final TokenRequestLatch latch = new TokenRequestLatch(1);

        Timeline.start(
                NetworkTestConstants.TimelineEntities.ACQUIRE_TOKEN_SILENT_FLOW,
                "Acquiring token interactively.",
                "In order to initiate the silent flow, we need to run the interactive flow first."
        );

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(new AuthenticationCallback() {
                    @Override
                    public void onCancel() {
                        final Exception exception = new Exception("Interactive flow cancelled by user.");
                        Timeline.finish(NetworkTestConstants.TimelineEntities.ACQUIRE_TOKEN_SILENT_FLOW);

                        networkTestRule.setException(exception);
                        latch.countDown();
                    }

                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        Timeline.finish(
                                NetworkTestConstants.TimelineEntities.ACQUIRE_TOKEN_SILENT_FLOW,
                                "Access token: " + authenticationResult.getAccessToken()
                        );
                        mAccount = authenticationResult.getAccount();
                        latch.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Timeline.finish(NetworkTestConstants.TimelineEntities.ACQUIRE_TOKEN_SILENT_FLOW, exception);

                        networkTestRule.setException(exception);
                        latch.countDown();
                    }
                })
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
        latch.await();

        IAccount account = getAccount();

        final AtomicInteger numPassed = new AtomicInteger(0);
        final AtomicInteger numFailed = new AtomicInteger(0);

        final int numberOfTests = 10;
        for (int i = 0; i < numberOfTests; i++) {
            TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);
            Timeline.start(
                    NetworkTestConstants.TimelineEntities.ACQUIRE_TOKEN_SILENT_FLOW,
                    "Acquiring token silently (" + i + ")",
                    "Running acquire token silent with no cached tokens"
            );

            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .forAccount(account)
                    .fromAuthority(account.getAuthority())
                    .forceRefresh(true)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(new SilentAuthenticationCallback() {
                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            numPassed.incrementAndGet();

                            Timeline.finish(
                                    NetworkTestConstants.TimelineEntities.ACQUIRE_TOKEN_SILENT_FLOW,
                                    "Access token: " + authenticationResult.getAccessToken()
                            );
                            tokenRequestLatch.countDown();
                        }

                        @Override
                        public void onError(MsalException exception) {
                            numFailed.incrementAndGet();

                            Timeline.finish(
                                    NetworkTestConstants.TimelineEntities.ACQUIRE_TOKEN_SILENT_FLOW,
                                    exception
                            );
                            tokenRequestLatch.countDown();
                        }
                    })
                    .build();

            mApplication.acquireTokenSilentAsync(silentParameters);

            tokenRequestLatch.await(TokenRequestTimeout.SILENT);
        }

        if (numberOfTests == numPassed.get()) {
            networkTestRule.setResult(numberOfTests + " acquire token silent tests passed.");
        } else {
            networkTestRule.setException(new Exception(numFailed.get() + " out of " + numberOfTests + " acquire token silent tests failed."));
        }
    }
}
