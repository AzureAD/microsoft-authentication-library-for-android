package com.microsoft.identity.client.msal.automationapp.testpass.broker.passkey;

import static androidx.core.content.ContextCompat.startActivity;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.browser.BrowserEdge;
import com.microsoft.identity.client.ui.automation.device.settings.GoogleSettings;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

public class testcase extends AbstractMsalBrokerTest {

    @Test
    public void test_case() throws Throwable {
        final String username = "passkeytest3@authapp2112.onmicrosoft.com";
        final String password = "Mikasa987!";

        //Enable Authenticator as a passkey provider.
        final GoogleSettings settings = new GoogleSettings();
        settings.launchAccountListPage();
        UiAutomatorUtils.handleButtonClickForObjectWithText("Authenticator");

        //Register passkey via Chrome
        final BrowserChrome chrome = new BrowserChrome();
        chrome.launch();
        UiAutomatorUtils.handleButtonClickForObjectWithText("Use without an account");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Got it");
        chrome.navigateTo("aka.ms/PasskeyPrivatePreviewMSI");
        //final UiObject usernameInput = UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().focused(true), FIND_UI_ELEMENT_TIMEOUT);
        //usernameInput.click();
        //usernameInput.setText(username);
        UiAutomatorUtils.handleInput("i0116", username);
        UiAutomatorUtils.handleButtonClick("idSIButton9");
       // final UiObject passwordInput = UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().focused(true), FIND_UI_ELEMENT_TIMEOUT);
        //passwordInput.click();
        //passwordInput.setText(password);
        UiAutomatorUtils.handleInput("i0118", password);
        //UiAutomatorUtils.handleButtonClickForObjectWithText("Sign in");
        UiAutomatorUtils.handleButtonClick("idSIButton9");

        UiAutomatorUtils.handleButtonClick("com.google.android.gms:id/more_options_button");
        UiAutomatorUtils.handleButtonClick("com.google.android.gms:id/transport_selection_cancel_button");
        UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().descriptionContains("Other ways to sign in").resourceId("idA_PWD_SwitchToCredPicker"), FIND_UI_ELEMENT_TIMEOUT).click();

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
        return R.raw.msal_config_webview;
    }
}
