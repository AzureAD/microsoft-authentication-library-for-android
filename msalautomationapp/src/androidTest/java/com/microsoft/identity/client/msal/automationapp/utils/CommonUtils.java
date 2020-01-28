package com.microsoft.identity.client.msal.automationapp.utils;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.uiautomator.UiDevice;

import org.junit.Assert;

import java.io.IOException;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class CommonUtils {

    final static long TIMEOUT = 1000 * 60;

    public static void launchApp(final String packageName) {
        final Context context = ApplicationProvider.getApplicationContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);  //sets the intent to start your app
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);  //clear out any previous task, i.e., make sure it starts on the initial screen
        context.startActivity(intent);
    }

    public void removeApp(final String packageName) {
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            String output = mDevice.executeShellCommand("pm uninstall " + packageName);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    static String getResourceId(final String appPackageName, final String internalResourceId) {
        return appPackageName + ":id/" + internalResourceId;
    }

    static boolean isStringPackageName(final String hint) {
        return hint.contains("."); // best guess
    }
}
