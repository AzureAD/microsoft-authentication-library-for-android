package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.rules.RulesHelper;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import org.junit.Assert;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
public class TestNetworkAcquireToken extends AbstractMsalUiNetworkTest<Void> {


    @Parameterized.Parameters(name = "acquireToken_{1}")
    public static Iterable<Object[]> parameters() throws IOException, ClassNotFoundException {
        return generateTestParameters(
                "input_acquireTokenWithoutBroker.csv",
                "output_acquireTokenWithoutBroker.csv");
    }

    @Override
    public Void prepare() {
        return null;
    }


    @Override
    public void execute(Void prerequisites, ResultFuture<String, Exception> resultFuture) {
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

    @Override
    public long timeoutSeconds() {
        return TimeUnit.MINUTES.toSeconds(2);
    }
}
