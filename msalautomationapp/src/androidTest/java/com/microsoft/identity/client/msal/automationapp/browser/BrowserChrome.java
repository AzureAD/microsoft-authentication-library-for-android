package com.microsoft.identity.client.msal.automationapp.browser;

import com.microsoft.identity.client.msal.automationapp.app.App;
import com.microsoft.identity.client.msal.automationapp.utils.UiAutomatorUtils;

public class BrowserChrome extends App {

    private static final String CHROME_PACKAGE_NAME = "com.android.chrome";
    private static final String CHROME_APP_NAME = "Google Chrome";

    public BrowserChrome() {
        super(CHROME_PACKAGE_NAME, CHROME_APP_NAME);
    }

    @Override
    public void handleFirstRun() {
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/terms_accept");
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/negative_button");
    }
}
