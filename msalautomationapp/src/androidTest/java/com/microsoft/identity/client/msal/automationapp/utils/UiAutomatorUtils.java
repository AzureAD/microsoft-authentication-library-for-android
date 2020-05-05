package com.microsoft.identity.client.msal.automationapp.utils;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import static com.microsoft.identity.client.msal.automationapp.utils.CommonUtils.TIMEOUT;
import static org.junit.Assert.fail;

public class UiAutomatorUtils {

    public static UiObject obtainUiObject(final String resourceId) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject uiObject = mDevice.findObject(new UiSelector()
                .resourceId(resourceId));

        uiObject.waitForExists(TIMEOUT);
        return uiObject;
    }

    public static void handleInput(final String resourceId, final String inputText) {
        final UiObject inputField = obtainUiObject(resourceId);

        try {
            inputField.setText(inputText);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }

    public static void handleButtonClick(final String resourceId) {
        final UiObject button = obtainUiObject(resourceId);

        try {
            button.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }
}
