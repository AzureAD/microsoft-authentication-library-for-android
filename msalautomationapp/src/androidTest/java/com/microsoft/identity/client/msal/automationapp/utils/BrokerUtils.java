package com.microsoft.identity.client.msal.automationapp.utils;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.Assert;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.microsoft.identity.client.msal.automationapp.utils.CommonUtils.TIMEOUT;
import static com.microsoft.identity.client.msal.automationapp.utils.CommonUtils.getResourceId;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;

public class BrokerUtils {

    public static void handleAccountPicker(final String username, final String brokerPackageName) {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        UiObject accountSelected = device.findObject(new UiSelector().resourceId(
                getResourceId(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME, "account_chooser_listView")
        ).childSelector(new UiSelector().textContains(
                username
        )));

        try {
            accountSelected.waitForExists(TIMEOUT);
            accountSelected.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }
}
