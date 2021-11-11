package com.microsoft.identity.client.msal.automationapp.testpass.network;

import android.os.Debug;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.java.network.NetworkConstants;
import com.microsoft.identity.common.java.network.NetworkMarkerManager;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class TestCaseNetwork extends BaseMsalUiNetworkTest {

    @Test
    public void test_acquireTokenInteractiveWithReconnection() throws Throwable {
        Logger.setAllowLogcat(true);
        Logger.setAllowPii(true);

        UiAutomatorUtils.setUIElementTimeout(2, TimeUnit.HOURS);

        NetworkMarkerManager.getInstance()
                .applyNetworkStates(
                        NetworkConstants.NetworkCodeMarkers.ACQUIRE_TOKEN_INTERACTIVE,
                        "NONE 3, WIFI"
                );

        final IAuthenticationResult result = runAcquireTokenInteractive();

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
    }

    @Test
    public void test_acquireTokenInteractiveWithNoInternetConnection() {
        NetworkMarkerManager.getInstance()
                .applyNetworkStates(
                        NetworkConstants.NetworkCodeMarkers.ACQUIRE_TOKEN_INTERACTIVE,
                        "NONE"
                );

        AssertionError exception = assertThrows(AssertionError.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                runAcquireTokenInteractive();
            }
        });

        assertEquals("androidx.test.uiautomator.UiObjectNotFoundException: UiSelector[RESOURCE_ID=i0118]", exception.getMessage());
    }

    @Test
    public void test_acquireTokenInteractiveWithWIFI() throws Throwable {
        NetworkMarkerManager.getInstance()
                .applyNetworkStates(NetworkConstants.NetworkCodeMarkers.ACQUIRE_TOKEN_INTERACTIVE, "WIFI");

        final IAuthenticationResult result = runAcquireTokenInteractive();

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
    }

    @Test
    public void test_acquireTokenInteractiveWithMobileData() throws Throwable {
        NetworkMarkerManager.getInstance()
                .applyNetworkStates(NetworkConstants.NetworkCodeMarkers.ACQUIRE_TOKEN_INTERACTIVE, "CELLULAR");

        final IAuthenticationResult result = runAcquireTokenInteractive();

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
    }

    @Test
    @Ignore(value = "Some reason")
    public void test_acquireTokenSilentWithNoInternetConnection() throws Throwable {
        final IAuthenticationResult result = runAcquireTokenSilent(true);

        assertNotNull(result);
    }
}
