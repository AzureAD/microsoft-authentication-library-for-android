package com.microsoft.identity.client.msal.automationapp.utils;

import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.microsoft.identity.internal.testutils.labutils.LabConfig;

import static com.microsoft.identity.client.msal.automationapp.utils.CommonUtils.TIMEOUT;
import static org.junit.Assert.fail;

public class WebViewUtils {

    public static void handleEmailField(final String username) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // login webview
        mDevice.wait(Until.findObject(By.clazz(WebView.class)), TIMEOUT);


        // Set username
        UiObject emailInput = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        emailInput.waitForExists(TIMEOUT);

        try {
            emailInput.setText(username);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        handleNextButton();
    }

    public static void handlePasswordField() {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Set Password
        UiObject passwordInput = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        passwordInput.waitForExists(TIMEOUT);
        try {
            passwordInput.setText(LabConfig.getCurrentLabConfig().getLabUserPassword());
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        handleNextButton();
    }

    public static void handleNextButton() {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Confirm Button Click
        UiObject buttonNext = mDevice.findObject(new UiSelector()
                .instance(1)
                .className(Button.class));

        buttonNext.waitForExists(TIMEOUT);
        try {
            buttonNext.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }

    public static void handleWebViewWithLoginHint() {
        handlePasswordField();
    }

    public static void handleWebViewWithoutLoginHint(final String username) {
        handleEmailField(username);
        handlePasswordField();
    }

}
