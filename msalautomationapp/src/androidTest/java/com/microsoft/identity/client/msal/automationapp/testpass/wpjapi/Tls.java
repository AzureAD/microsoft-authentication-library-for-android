package com.microsoft.identity.client.msal.automationapp.testpass.wpjapi;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

public class Tls {

    public void performTLSOperation(final String username, final String password) throws UiObjectNotFoundException {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final BrowserChrome chrome = new BrowserChrome();
        chrome.handleFirstRun();
        // click on Open up from chrome tabs to open in google chrome.
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/menu_button");
        final UiObject openTabs = UiAutomatorUtils.obtainUiObjectWithText("Open");
        Assert.assertTrue(openTabs.exists());
        openTabs.click();

        // in url removing x-client-SKU=MSAL.Android.
        final UiObject urlBar = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.chrome:id/url_bar");
        Assert.assertTrue(urlBar.exists());
        String url = urlBar.getText();
        url = url.replace("x-client-SKU=MSAL.Android", "");

        // entering the final url in google chrome.
        urlBar.click();
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/delete_button");
        urlBar.setText(url);
        device.pressEnter();

        // entering credentials.
        UiAutomatorUtils.handleInput("i0116", username);
        UiAutomatorUtils.handleButtonClick("idSIButton9");

        UiAutomatorUtils.handleInput("i0118", password);
        UiAutomatorUtils.handleButtonClick("idSIButton9");

        //installing certificate.
        UiAutomatorUtils.handleButtonClick("android:id/button1");
    }
}
