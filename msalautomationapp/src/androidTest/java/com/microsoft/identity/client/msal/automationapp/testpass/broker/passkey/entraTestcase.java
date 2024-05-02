package com.microsoft.identity.client.msal.automationapp.testpass.broker.passkey;

import android.widget.EditText;
import android.widget.Spinner;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.device.settings.GoogleSettings;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

import java.util.Arrays;

public class entraTestcase extends AbstractMsalBrokerTest {
    @Test
    public void entra_testcase() throws Throwable {
        final String username = mLabAccount.getUsername();
        //final String password = mLabAccount.getPassword();

        //Enable Authenticator as a passkey provider.
        final GoogleSettings settings = new GoogleSettings();
        settings.launchAccountListPage();
        UiAutomatorUtils.handleButtonClick("com.android.settings:id/switchWidget");

//        //Register passkey via Chrome
//        final BrowserChrome chrome = new BrowserChrome();
//        chrome.launch();
//        UiAutomatorUtils.handleButtonClickForObjectWithText("Use without an account");
//        UiAutomatorUtils.handleButtonClickForObjectWithText("Got it");
//        chrome.navigateTo("aka.ms/mysecurityinfo");
//        //UiAutomatorUtils.handleInput("i0116", username);
//        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(username);
//        UiAutomatorUtils.handleButtonClickForObjectWithText("Next");
//        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(password);
//        UiAutomatorUtils.pressEnter();
//        UiAutomatorUtils.handleButtonClickForObjectWithText("Add sign-in method");
//        UiAutomatorUtils.obtainUiObjectWithText("Choose a method").click();

        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(username)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .passwordPageExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .choosePasskeyExpected(true)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, "no password");
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_webview_skip_broker;
    }
}
