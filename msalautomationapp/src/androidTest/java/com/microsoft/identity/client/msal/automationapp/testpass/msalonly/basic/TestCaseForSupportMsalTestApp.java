package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.basic;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RetryOnFailure
@RunOnAPI29Minus
@SupportedBrokers(brokers = {BrokerLTW.class})
public class TestCaseForSupportMsalTestApp extends AbstractMsalBrokerTest {

    @Test
    public void TestCaseForSupportMsalTestApp () throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        // test for installing msal test app crash error
//        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
//        String b = mDevice.executeShellCommand("ls /");
//        Log.d("TestCaseForSupportMsalTestApp", "installPackage: " + b);
//        String a = mDevice.executeShellCommand("pm install -t /data/local/tmp/MsalTestApp.apk");
//        Log.d("TestCaseForSupportMsalTestApp", "installPackage: " + a);

        MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.uninstall();
        msalTestApp.install();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        // acquire toke interactively and validate the token
        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
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

        // Small wait to allow getting token to complete
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));

        Map<String, ?> validatedToken = msalTestApp.validateToken(token);
        Assert.assertTrue(validatedToken != null);

        // then acquire token silently and validate the token

        // finally get users and validate the users

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
