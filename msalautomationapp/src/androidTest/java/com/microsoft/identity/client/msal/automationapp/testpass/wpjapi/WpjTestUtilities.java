//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.wpjapi;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadLoginComponentHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.internal.testutils.labutils.LabApiException;
import com.microsoft.identity.internal.testutils.labutils.LabDeviceHelper;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

public class WpjTestUtilities {

    public static void performTLSOperation(final String username, final String password) throws InterruptedException, UiObjectNotFoundException {
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final BrowserChrome chrome = new BrowserChrome();
        chrome.handleFirstRun();

        // click on Open in chrome tabs.
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/menu_button");
        final UiObject openInChrome = UiAutomatorUtils.obtainUiObjectWithText("Open in Chrome");
        Assert.assertTrue(openInChrome.exists());
        openInChrome.click();

        // in url removing x-client-SKU=MSAL.Android.
        final UiObject urlBar = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.chrome:id/url_bar");
        Assert.assertTrue(urlBar.exists());
        Thread.sleep(TimeUnit.SECONDS.toMillis(10));
        String url = urlBar.getText();
        url = url.replace("x-client-SKU=MSAL.Android", "");

        // entering the modified url in url bar.
        urlBar.click();
        urlBar.setText(url);
        device.pressEnter();

        // entering credentials.
        final AadLoginComponentHandler aadLoginComponentHandler = new AadLoginComponentHandler();
        aadLoginComponentHandler.handleEmailField(username);
        aadLoginComponentHandler.handlePasswordField(password);

        // installing certificate.
        UiAutomatorUtils.handleButtonClick("android:id/button1");

        final UiObject appSelector = UiAutomatorUtils.obtainUiObjectWithText("MSAL");
        appSelector.click();
        final UiObject useSelector = UiAutomatorUtils.obtainUiObjectWithText("JUST ONCE");
        useSelector.click();
    }

    public static void performWpjLeave(final ITestBroker broker) throws InterruptedException, UiObjectNotFoundException {
        broker.launch();
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonLeave");

        broker.launch();
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

    public static void installCertificate(final ITestBroker broker) {
        broker.launch();

        //installing certificate.
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonInstallCert");
        UiAutomatorUtils.handleButtonClick("android:id/button1");
    }

    public static String getUpn(final ITestBroker broker) throws UiObjectNotFoundException {
        broker.launch();
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

    public static void deleteDevice(final String upn, final String deviceId) {
        try {
            final boolean deviceDeleted = LabDeviceHelper.deleteDevice(upn, deviceId);
        } catch (LabApiException e) {
            Assert.assertTrue(e.getCode() == 400);
        }
    }
}

