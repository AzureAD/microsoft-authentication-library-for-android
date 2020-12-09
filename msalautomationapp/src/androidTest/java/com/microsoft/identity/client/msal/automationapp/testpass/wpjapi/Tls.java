package com.microsoft.identity.client.msal.automationapp.testpass.wpjapi;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class Tls {

    public void performTLSOperation(final String username, final String password) throws UiObjectNotFoundException, InterruptedException {

        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final BrowserChrome chrome = new BrowserChrome();
        chrome.handleFirstRun();

        // click on Open in chrome tabs.
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/menu_button");
        final UiObject openTabs = UiAutomatorUtils.obtainUiObjectWithText("Open");
        Assert.assertTrue(openTabs.exists());
        openTabs.click();

        // in url removing x-client-SKU=MSAL.Android.
        final UiObject urlBar = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.chrome:id/url_bar");
        Assert.assertTrue(urlBar.exists());
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        String url = urlBar.getText();
        url = url.replace("x-client-SKU=MSAL.Android", "");

        // entering the final url in urlbar.
        urlBar.click();
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/delete_button");
        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
        urlBar.setText(url);
        device.pressEnter();

        // entering credentials.
        UiAutomatorUtils.handleInput("i0116", username);
        UiAutomatorUtils.handleButtonClick("idSIButton9");

        UiAutomatorUtils.handleInput("i0118", password);
        UiAutomatorUtils.handleButtonClick("idSIButton9");

        // installing certificate.
        UiAutomatorUtils.handleButtonClick("android:id/button1");

        final UiObject appSelector = UiAutomatorUtils.obtainUiObjectWithText("MSAL");
        appSelector.click();
        final UiObject useSelector = UiAutomatorUtils.obtainUiObjectWithText("JUST ONCE");
        useSelector.click();
    }
}
