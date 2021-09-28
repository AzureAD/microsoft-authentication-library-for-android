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
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.network.NetworkTestStateManager;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;

@RunWith(Parameterized.class)
public class TestNetworkAcquireTokenSilent extends AbstractMsalUiNetworkTest<IAccount> {

    @Parameterized.Parameters(name = "acquireTokenSilent_{1}")
    public static Iterable<Object[]> parameters() throws IOException, ClassNotFoundException {
        return generateTestParameters("input_acquireTokenSilentlyWithoutBroker.csv", "output_acquireTokenSilentlyWithoutBroker.csv");
    }

    @Override
    public IAccount prepare() {
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

        return getAccount();
    }

    @Override
    public void execute(IAccount account, ResultFuture<String, Exception> resultFuture) {
        final int numberOfTests = 10;
        for (int i = 0; i < numberOfTests; i++) {
            final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                    .forAccount(account)
                    .fromAuthority(account.getAuthority())
                    .forceRefresh(true)
                    .withScopes(Arrays.asList(mScopes))
                    .withCallback(new SilentAuthenticationCallback() {
                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            resultFuture.setResult(authenticationResult.getAccessToken());
                        }

                        @Override
                        public void onError(MsalException exception) {
                            resultFuture.setException(exception);
                        }
                    })
                    .build();

            mApplication.acquireTokenSilentAsync(silentParameters);
        }
    }
}
