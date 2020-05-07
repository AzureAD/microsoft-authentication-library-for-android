package com.microsoft.identity.client.msal.automationapp.utils;

import static com.microsoft.identity.client.msal.automationapp.utils.CommonUtils.launchApp;

public class SettingsUtils {

    final static String SETTINGS_APP_PACKAGE_NAME = "com.android.settings";

    public static void clearCache() {
        launchApp(SETTINGS_APP_PACKAGE_NAME);
        //:TODO figure out how to clear cache in an app through settings screen
    }


}
