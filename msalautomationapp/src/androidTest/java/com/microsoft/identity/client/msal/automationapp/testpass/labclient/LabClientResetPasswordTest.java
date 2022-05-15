package com.microsoft.identity.client.msal.automationapp.testpass.labclient;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class LabClientResetPasswordTest extends AbstractMsalBrokerTest {

    @Test
    public void testCannotSilentRequestAfterPasswordReset() throws Throwable {
        final String username = mLoginHint;
        final String password = mLabAccount.getPassword();

        final MsalSdk msalSdk = new MsalSdk();
        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult1 = msalSdk.acquireTokenInteractive(authTestParams, new com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(mLoginHint)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult1.assertSuccess();

        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        Assert.assertTrue(mLabClient.resetPassword(username));

        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();

        IAccount account = msalSdk.getAccount(mActivity,getConfigFileResourceId(),username);
        final MsalAuthTestParams silentParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .authority(account.getAuthority())
                .resource(mScopes[0])
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult2 = msalSdk.acquireTokenSilent(silentParams, TokenRequestTimeout.MEDIUM);

        // Should get a failure due to password reset.
        authResult2.assertFailure();
        Assert.assertTrue(authResult2.getException() instanceof MsalUiRequiredException);
    }

    @Override
    public LabUserQuery getLabQuery() {
        return null;
    }

    @Override
    public String getTempUserType() { return TempUserType.BASIC.name(); }

    @Override
    public String[] getScopes() {
        return new String[]{"https://graph.windows.net/user.read"};
    }

    @Override
    public String getAuthority() {
        return "https://login.microsoftonline.de/common";
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}
