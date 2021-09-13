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
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.network.INetworkTestRunner;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import org.junit.Assert;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class TestCaseNetwork extends BaseMsalNetworkTest {


    @TestFactory
    public Collection<DynamicTest> test_acquireTokenSilentlyWithoutBroker() throws IOException, ClassNotFoundException {
        final int numberOfTests = 10;
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

        final IAccount account = getAccount();

        INetworkTestRunner acquireTokenSilentTest = new INetworkTestRunner() {
            @Override
            public void runTest(ResultFuture<String, Exception> resultFuture) {
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
        };

        return dynamicNetworkTests("input_acquireTokenSilentlyWithoutBroker.csv", "output_acquireTokenSilentlyWithoutBroker.csv", acquireTokenSilentTest);
    }

    @TestFactory
    public Collection<DynamicTest> test_acquireTokenWithoutBroker() throws ClassNotFoundException, IOException {
        INetworkTestRunner acquireTokenTest = new INetworkTestRunner() {
            @Override
            public void runTest(ResultFuture<String, Exception> resultFuture) {
                final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                        .startAuthorizationFromActivity(mActivity)
                        .withLoginHint(mLoginHint)
                        .withScopes(Arrays.asList(mScopes))
                        .withCallback(new AuthenticationCallback() {
                            @Override
                            public void onCancel() {
                                resultFuture.setResult(null);
                            }

                            @Override
                            public void onSuccess(IAuthenticationResult authenticationResult) {
                                Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                                mAccount = authenticationResult.getAccount();

                                resultFuture.setResult(authenticationResult.getAccessToken());
                            }

                            @Override
                            public void onError(MsalException exception) {
                                resultFuture.setException(exception);
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
            }
        };
        return dynamicNetworkTests("input_acquireTokenWithoutBroker.csv", "output_acquireTokenWithoutBroker.csv", acquireTokenTest);
    }
}
