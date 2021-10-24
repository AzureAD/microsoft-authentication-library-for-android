package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.NetworkOverrides;
import com.microsoft.identity.client.ui.automation.annotations.NetworkStateOverride;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.common.java.network.NetworkConstants;
import com.microsoft.identity.common.java.network.NetworkInterface;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCaseNetwork extends BaseMsalUiNetworkTest {

    @NetworkOverrides({
            @NetworkStateOverride(
                    marker = NetworkConstants.NetworkCodeMarkers.ACQUIRE_TOKEN_INTERACTIVE,
                    networkInterface = NetworkInterface.NONE
            )
    })
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

    @Test
    public void test_acquireTokenSilentWithoutBroker() throws InterruptedException {
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
        latch.await();

        IAccount account = getAccount();

        final AtomicInteger numPassed = new AtomicInteger(0);
        final AtomicInteger numFailed = new AtomicInteger(0);

        final int numberOfTests = 10;
        for (int i = 0; i < numberOfTests; i++) {
            TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);


            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .forAccount(account)
                    .fromAuthority(account.getAuthority())
                    .forceRefresh(true)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(new SilentAuthenticationCallback() {
                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            numPassed.incrementAndGet();

                            tokenRequestLatch.countDown();
                        }

                        @Override
                        public void onError(MsalException exception) {
                            numFailed.incrementAndGet();

                            tokenRequestLatch.countDown();
                        }
                    })
                    .build();

            mApplication.acquireTokenSilentAsync(silentParameters);

            tokenRequestLatch.await(TokenRequestTimeout.SILENT);
        }
    }
}
