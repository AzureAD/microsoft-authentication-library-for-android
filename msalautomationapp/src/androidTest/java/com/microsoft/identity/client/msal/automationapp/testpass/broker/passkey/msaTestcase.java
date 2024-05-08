package com.microsoft.identity.client.msal.automationapp.testpass.broker.passkey;

import android.widget.EditText;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

public class msaTestcase extends AbstractMsalBrokerTest {

    @Test
    public void msa_testcase() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        //Enable Authenticator as a passkey provider.
//        final GoogleSettings settings = new GoogleSettings();
//        settings.launchAccountListPage();
//        UiAutomatorUtils.handleButtonClickForObjectWithText("Authenticator");

        //Register passkey via Chrome
        final BrowserChrome chrome = new BrowserChrome();
        chrome.launch();
        UiAutomatorUtils.handleButtonClickForObjectWithText("Use without an account");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Got it");
        chrome.navigateTo("account.live.com");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Sign in to your Microsoft account");
        //UiAutomatorUtils.handleInput("i0116", username);
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(username);
        UiAutomatorUtils.handleButtonClickForObjectWithText("Next");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(password);
        UiAutomatorUtils.pressEnter();
        //UiAutomatorUtils.handleButtonClickForObjectWithText("No");
        //UiAutomatorUtils.obtainUiObjectWithText("Get started").click();
        //UiAutomatorUtils.handleButtonClick("idSIButton9");
        // final UiObject passwordInput = UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().focused(true), FIND_UI_ELEMENT_TIMEOUT);
        //passwordInput.click();
        //passwordInput.setText(password);
        //UiAutomatorUtils.handleInput("i0118", password);
        //UiAutomatorUtils.handleButtonClickForObjectWithText("Sign in");
        //UiAutomatorUtils.handleButtonClick("idSIButton9");

//        UiAutomatorUtils.handleButtonClick("com.google.android.gms:id/more_options_button");
//        UiAutomatorUtils.handleButtonClick("com.google.android.gms:id/transport_selection_cancel_button");
//        UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().descriptionContains("Other ways to sign in").resourceId("idA_PWD_SwitchToCredPicker"), FIND_UI_ELEMENT_TIMEOUT).click();

        //UiAutomatorUtils.obtainUiObjectWithDescription("Other ways to sign in").click();

        UiAutomatorUtils.handleButtonClickForObjectWithText("Add sign-in method");
        UiAutomatorUtils.handleButtonClick("Dropdown65");
        //UiAutomatorUtils.pressBack();
        //UiObject clickable = UiAutomatorUtils.obtainClickableUiObjects("hello");
        //clickable.click();
        //UiAutomatorUtils.handleButtonClickForObjectWithText("More options");
        //UiAutomatorUtils.handleButtonClickForObjectWithText("Cancel");
        //UiAutomatorUtils.handleButtonClickForObjectWithText("Other ways to sign in"); //UiAutomatorUtils.handleButtonClick("idA_PWD_SwitchToCredPicker");
        //UiAutomatorUtils.handleButtonClickForObjectWithText("Approve a request on my Microsoft Authenticator app");
        //UiAutomatorUtils.handleButtonClick("Add method");
        //UiAutomatorUtils.handleButtonClick("Dropdown132-option");
        //UiAutomatorUtils.handleButtonClick("Dropdown132-list2");
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.MSA)
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
        return R.raw.msal_config_webview;
    }
}