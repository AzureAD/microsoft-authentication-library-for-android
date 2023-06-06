package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.basic;

import android.util.Log;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.UiResponse;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

@RetryOnFailure(retryCount = 2)
@RunOnAPI29Minus
@SupportedBrokers(brokers = {BrokerLTW.class})
public class TestCaseForSupportMsalTestApp extends AbstractMsalBrokerTest {

    @Test
    public void TestCaseForSupportMsalTestApp () throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.uninstall();
        msalTestApp.install();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        // acquire toke interactively and validate the token
        final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .sessionExpected(false)
                .broker(mBroker)
                .expectingBrokerAccountChooserActivity(false)
                .expectingProvidedAccountInBroker(false)
                .expectingLoginPageAccountPicker(false)
                .expectingProvidedAccountInCookie(false)
                .consentPageExpected(false)
                .passwordPageExpected(true)
                .speedBumpExpected(false)
                .registerPageExpected(false)
                .enrollPageExpected(false)
                .staySignedInPageExpected(false)
                .verifyYourIdentityPageExpected(false)
                .howWouldYouLikeToSignInExpected(false)
                .build();

        String token = msalTestApp.acquireToken(username, password, promptHandlerParameters);
        Log.d("TestCaseForSupportMsalTestApp", "TestCaseForSupportMsalTestApp: tokenInteractive = " + token);

//        Map<String, ?> validatedToken = msalTestApp.validateToken(token);
//        Assert.assertTrue(validatedToken != null);

        // then acquire token silently and validate the token
        msalTestApp.forceStop();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();
        final MicrosoftStsPromptHandlerParameters promptHandlerParametersForSilentAcquireToken = MicrosoftStsPromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(null)
                .sessionExpected(false)
                .broker(null)
                .expectingBrokerAccountChooserActivity(false)
                .expectingProvidedAccountInBroker(false)
                .expectingLoginPageAccountPicker(false)
                .expectingProvidedAccountInCookie(false)
                .consentPageExpected(false)
                .passwordPageExpected(false)
                .speedBumpExpected(false)
                .registerPageExpected(false)
                .enrollPageExpected(false)
                .staySignedInPageExpected(false)
                .verifyYourIdentityPageExpected(false)
                .howWouldYouLikeToSignInExpected(false)
                .build();
        String silentToken = msalTestApp.acquireTokenSilent(username, password, promptHandlerParametersForSilentAcquireToken);
        Log.d("TestCaseForSupportMsalTestApp", "TestCaseForSupportMsalTestApp: tokenSilent = " + silentToken);
        Assert.assertTrue(silentToken != null);

        // finally get users and validate the users
        msalTestApp.forceStop();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();
        List<String> users = msalTestApp.getUsers();
        Log.d("TestCaseForSupportMsalTestApp", "TestCaseForSupportMsalTestApp: userList = " + users);
        Assert.assertTrue(users.size() == 1);
    }

    // if getLabQuery return null then will use getTempUserType to create account
    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.BASIC;
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
