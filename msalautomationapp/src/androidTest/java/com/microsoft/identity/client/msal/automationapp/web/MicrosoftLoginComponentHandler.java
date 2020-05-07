package com.microsoft.identity.client.msal.automationapp.web;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.msal.automationapp.utils.UiAutomatorUtils;

import org.junit.Assert;

import static com.microsoft.identity.client.msal.automationapp.utils.CommonUtils.TIMEOUT;
import static org.junit.Assert.fail;

public class MicrosoftLoginComponentHandler implements ILoginComponentHandler {

    @Override
    public void handleEmailField(final String username) {
        UiAutomatorUtils.handleInput("i0116", username);
        handleNextButton();
    }

    @Override
    public void handlePasswordField(final String password) {
        UiAutomatorUtils.handleInput("i0118", password);
        handleNextButton();
    }

    @Override
    public void handleBackButton() {
        UiAutomatorUtils.handleButtonClick("idBtn_Back");
    }

    @Override
    public void handleNextButton() {
        UiAutomatorUtils.handleButtonClick("idSIButton9");
    }

    @Override
    public void handleAccountPicker(final String username) {
        final UiDevice uiDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Confirm On Account Picker
        UiObject accountPicker = UiAutomatorUtils.obtainUiObjectWithResourceId("tilesHolder");

        if (!accountPicker.waitForExists(TIMEOUT)) {
            fail("Account picker screen did not show up");
        }

        UiObject account = uiDevice.findObject(new UiSelector()
                .text("Sign in with " + username + " work or school account.")
        );

        account.waitForExists(TIMEOUT);

        try {
            account.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }

    private UiObject getConsentScreen() {
        return UiAutomatorUtils.obtainUiObjectWithResourceId("consentHeader");
    }

    @Override
    public void confirmConsentPageReceived() {
        final UiObject consentScreen = getConsentScreen();
        Assert.assertTrue(consentScreen.waitForExists(TIMEOUT));
    }

    @Override
    public void acceptConsent() {
        confirmConsentPageReceived();
        handleNextButton();
    }

    public void declineConsent() {
        confirmConsentPageReceived();
        handleBackButton();
    }

    @Override
    public void handleSpeedBump() {
        // Confirm On Speed Bump Screen
        UiObject speedBump = UiAutomatorUtils.obtainUiObjectWithResourceId("appConfirmTitle");

        if (!speedBump.waitForExists(TIMEOUT)) {
            fail("Speed Bump screen did not show up");
        }

        handleNextButton();
    }
}
