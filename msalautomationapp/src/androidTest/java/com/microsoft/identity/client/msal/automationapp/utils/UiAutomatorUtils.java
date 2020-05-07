package com.microsoft.identity.client.msal.automationapp.utils;

import android.view.accessibility.AccessibilityWindowInfo;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import static com.microsoft.identity.client.msal.automationapp.utils.CommonUtils.TIMEOUT;
import static org.junit.Assert.fail;

public class UiAutomatorUtils {

    public static UiObject obtainUiObjectWithResourceId(final String resourceId) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject uiObject = mDevice.findObject(new UiSelector()
                .resourceId(resourceId));

        uiObject.waitForExists(TIMEOUT);
        return uiObject;
    }

    public static UiObject obtainUiObjectWithText(final String text) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        UiObject uiObject = mDevice.findObject(new UiSelector()
                .textContains(text));

        uiObject.waitForExists(TIMEOUT);
        return uiObject;
    }

    public static UiObject obtainChildInScrollable(final String scrollableResourceId, final String childText) {
        final UiSelector scrollSelector = new UiSelector().resourceId(scrollableResourceId);

        final UiScrollable recyclerView = new UiScrollable(scrollSelector);

        final UiSelector childSelector = new UiSelector()
                .textContains(childText);

        try {
            final UiObject child = recyclerView.getChildByText(
                    childSelector,
                    childText
            );

            child.waitForExists(TIMEOUT);
            return child;
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        return null;
    }

    public static void handleInput(final String resourceId, final String inputText) {
        final UiObject inputField = obtainUiObjectWithResourceId(resourceId);

        try {
            inputField.setText(inputText);
            closeKeyboardIfNeeded();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }

    public static void handleButtonClick(final String resourceId) {
        final UiObject button = obtainUiObjectWithResourceId(resourceId);

        try {
            button.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }

    private static boolean isKeyboardOpen() {
        for (AccessibilityWindowInfo window : InstrumentationRegistry.getInstrumentation().getUiAutomation().getWindows()) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                return true;
            }
        }
        return false;
    }

    private static void closeKeyboardIfNeeded() {
        if (isKeyboardOpen()) {
            final UiDevice uiDevice =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            uiDevice.pressBack();
        }
    }
}
