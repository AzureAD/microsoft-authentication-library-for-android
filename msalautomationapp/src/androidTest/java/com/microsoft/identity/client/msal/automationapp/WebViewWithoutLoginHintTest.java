package com.microsoft.identity.client.msal.automationapp;

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

import org.junit.Before;

import static org.junit.Assert.fail;

public class WebViewWithoutLoginHintTest extends AcquireTokenNetworkTest {

    @Before
    public void setup() {
        super.setup();
        mLoginHint = null;
    }

    @Override
    public void handleUserInteraction() {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        // login webview
        mDevice.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        // Set username
        UiObject emailInput = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        emailInput.waitForExists(timeOut);

        try {
            emailInput.setText(mUsername);
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        // Confirm Button Click
        UiObject buttonNext = mDevice.findObject(new UiSelector()
                .instance(1)
                .className(Button.class));

        buttonNext.waitForExists(timeOut);
        try {
            buttonNext.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        // Set Password
        UiObject passwordInput = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        passwordInput.waitForExists(timeOut);
        try {
            passwordInput.setText(LabConfig.getCurrentLabConfig().getLabUserPassword());
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }

        // Confirm Button Click
        UiObject buttonLogin = mDevice.findObject(new UiSelector()
                .instance(1)
                .className(Button.class));

        buttonLogin.waitForExists(timeOut);
        try {
            buttonLogin.click();
        } catch (UiObjectNotFoundException e) {
            fail(e.getMessage());
        }
    }
}
