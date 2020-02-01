package com.microsoft.identity.client.msal.automationapp.utils;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Assert;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.microsoft.identity.client.msal.automationapp.utils.CommonUtils.getResourceId;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;

public class BrokerUtils {

    public static void handleAccountPicker(final String username) {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final int timeOut = 1000 * 60;

        UiDevice device = UiDevice.getInstance(getInstrumentation());

        // we will just take the first app in the list
        UiObject accountSelected = device.findObject(new UiSelector().resourceId(
                getResourceId(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, "swipe_list")
        ).index(0).childSelector(new UiSelector().textContains(
                username
        )));

        try {
            accountSelected.waitForExists(timeOut);
            accountSelected.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    public static boolean isAuthenticatorOpen() {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        return mDevice.hasObject(By.res("com.azure.authenticator:id/swipe_list"));
    }

    public static boolean isCompanyPortalOpen() {
        final UiDevice mDevice =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        return mDevice.hasObject(By.res("com.microsoft.windowsintune.companyportal:id/account_list"));
    }
}
