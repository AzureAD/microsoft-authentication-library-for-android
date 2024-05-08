package com.microsoft.identity.client.msal.automationapp.testpass.broker.passkey;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.device.settings.GoogleSettings;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.rules.FactoryResetChromeRule;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.LabClient;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class entraTestcase extends AbstractMsalBrokerTest {
    String TAG = "entraTestcase";
    String systemPin = "PutPINHere";
    @Test
    public void entra_testcase() throws Throwable {
        final String username = mLabAccount.getUsername();
        // When using AuthApp test accounts, replace below with TAP, since that's what's needed to add an auth method.
        final String password = mLabAccount.getPassword();

        // Enable Authenticator as a passkey provider.
        final GoogleSettings settings = new GoogleSettings();
        settings.launchAccountListPage();
        UiAutomatorUtils.handleButtonClick("com.android.settings:id/switchWidget");

        // Will need to set PIN screen lock here, if not set already.
        //settings.launchScreenLockPage();
        //UiAutomatorUtils.obtainUiObjectWithTextAndClassType("Screen lock", TextView.class).click();

        // Add an account to AuthApp.
        mBroker.launch();
        UiAutomatorUtils.handleButtonClickSafely("com.android.permissioncontroller:id/permission_allow_button");
        mBroker.handleFirstRun();
        UiAutomatorUtils.handleButtonClickSafely("com.azure.authenticator:id/zero_accounts_add_account_button");
        UiAutomatorUtils.handleButtonClickSafely("com.azure.authenticator:id/add_account_work_btn");
        UiAutomatorUtils.obtainUiObjectWithClassAndDescription(LinearLayout.class, "Sign in 2 of 2").click();
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(username);
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Next");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(password);
        //UiAutomatorUtils.handleInput("accesspass", password);
        UiAutomatorUtils.pressEnter();
        //TODO: How can we select the "Next" button on the page that says registration is required? (and I'm guessing the "Next" on the following page will have the same issue)
        //UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().packageName("com.azure.authenticator").className(Button.class).clickable(true), 10000).click();
        //UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Next");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(Button.class, 0).click();
        UiAutomatorUtils.obtainUiObjectWithTextAndClassType("Next", Button.class).click();
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Pair your account to the app by clicking this link.");
        UiAutomatorUtils.handleButtonClickSafely("android:id/button1");

        // Register a passkey.
        UiAutomatorUtils.handleButtonClickSafely("com.azure.authenticator:id/account_list_row_account_name");
        //UiAutomatorUtils.obtainUiObjectWithTextAndClassType("Continue", TextView.class).click();
        //UiAutomatorUtils.handleButtonClickSafely("idSIButton9");
        UiAutomatorUtils.handleInput("com.android.systemui:id/lockPassword", systemPin);
        UiAutomatorUtils.pressEnter();
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(15), TAG, "Sleeping while registering");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Done");
        UiAutomatorUtils.handleInput("com.android.systemui:id/lockPassword", systemPin);
        UiAutomatorUtils.pressEnter();
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(15), TAG, "Sleeping while adding a passkey");

        // Now try to use the passkey to login.
        final MsalSdk msalSdk = new MsalSdk();
        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult;
        try {
            authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
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
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

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
    }

    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.GLOBAL_MFA;
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
