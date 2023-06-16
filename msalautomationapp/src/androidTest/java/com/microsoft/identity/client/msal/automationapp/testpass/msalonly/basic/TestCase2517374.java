package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.basic;

import android.util.Log;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import org.junit.Assert;
import org.junit.Test;
import java.util.List;


@RetryOnFailure(retryCount = 2)
@RunOnAPI29Minus
@SupportedBrokers(brokers = {BrokerCompanyPortal.class})
public class TestCase2517374 extends AbstractMsalBrokerTest {

    @Test
    public void test_2517374 () throws Throwable {
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

        String token = msalTestApp.acquireToken(username, password, promptHandlerParameters, true);
        Assert.assertNotNull(token);

        // then acquire token silently and validate the token
        msalTestApp.forceStop();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();
        String silentToken = msalTestApp.acquireTokenSilent();
        Assert.assertNotNull(silentToken);

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
