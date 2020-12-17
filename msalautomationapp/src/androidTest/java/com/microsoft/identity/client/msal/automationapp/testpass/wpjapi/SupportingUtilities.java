package com.microsoft.identity.client.msal.automationapp.testpass.wpjapi;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.sql.Time;
import java.util.concurrent.TimeUnit;

public class SupportingUtilities {

    public static void performTLSOperation(final String username, final String password) throws UiObjectNotFoundException, InterruptedException {

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
        device.pressEnter();

        // installing certificate.
        UiAutomatorUtils.handleButtonClick("android:id/button1");

        final UiObject appSelector = UiAutomatorUtils.obtainUiObjectWithText("MSAL");
        appSelector.click();
        final UiObject useSelector = UiAutomatorUtils.obtainUiObjectWithText("JUST ONCE");
        useSelector.click();
    }

    public static void performWpjLeave(final ITestBroker sBroker) throws InterruptedException, UiObjectNotFoundException {
        sBroker.launch();
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonLeave");

        sBroker.launch();
        Thread.sleep(TimeUnit.SECONDS.toMillis(15));
        // getting wpj upn which should be error.
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonGetWpjUpn");

        // Look for the UPN dialog box
        final UiObject showUpnDialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );

        Assert.assertTrue(showUpnDialogBox.exists());

        final String newUpn = showUpnDialogBox.getText().split(":")[0];

        // dismiss dialog
        UiAutomatorUtils.handleButtonClick("android:id/button1");
        Assert.assertEquals(newUpn, "Error");
    }

    public static void confirmSignInAzure(final String username) throws InterruptedException {
        // installing Azure Sample App.
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();
        azureSampleApp.install();
        azureSampleApp.launch();
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        azureSampleApp.confirmSignedIn(username);
    }

    public static void installCertificate(final ITestBroker sBroker) {
        sBroker.launch();
        //installing certificate.
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonInstallCert");
        UiAutomatorUtils.handleButtonClick("android:id/button1");
    }

    public static String getUpn(final ITestBroker sBroker) throws UiObjectNotFoundException {
        sBroker.launch();
        // obtaining wpj upn.
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonGetWpjUpn");

        // Look for the UPN dialog box
        final UiObject showUpnDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "android:id/message"
        );

        Assert.assertTrue(showUpnDialog.exists());

        final String upn = showUpnDialog.getText().split(":")[1];

        // dismiss dialog
        UiAutomatorUtils.handleButtonClick("android:id/button1");
        return upn;
    }
}
