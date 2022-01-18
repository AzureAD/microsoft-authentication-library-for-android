package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.network.NetworkCodeMarkers;
import com.microsoft.identity.common.java.network.NetworkMarkerManager;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class TestCaseNetwork extends BaseMsalUiNetworkTest {

    @Test
    @Ignore("It executes intermittently. For most of the cases, the WebViewAuthorizationFragment gets stuck in a progress bar, meaning the" +
            "WebView does not send back a result on the state of the loaded page.")
    public void test_acquireTokenInteractiveWithReconnection() throws Throwable {
        UiAutomatorUtils.setUIElementTimeout(2, TimeUnit.HOURS);

        NetworkMarkerManager
                .useNetworkCondition(NetworkCodeMarkers.ACQUIRE_TOKEN_INTERACTIVE, "NONE 3, WIFI");

        final IAuthenticationResult result = runAcquireTokenInteractive();

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
    }


    @Test
    public void test_acquireTokenInteractiveWithWIFI() throws Throwable {
        NetworkMarkerManager
                .useNetworkCondition(NetworkCodeMarkers.ACQUIRE_TOKEN_INTERACTIVE, "WIFI");

        final IAuthenticationResult result = runAcquireTokenInteractive();

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
    }

    @Test
    public void test_acquireTokenSilentWithNoInternetConnection() {
        NetworkMarkerManager
                .useNetworkCondition(NetworkCodeMarkers.ACQUIRE_TOKEN_SILENT, "NONE");

        assertThrows("An IO error occurred with message: Unable to resolve host \"login.microsoftonline.com\": No address associated with hostname", MsalClientException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                runAcquireTokenSilent(true);
            }
        });
    }

    @Test
    public void test_acquireTokenSilentWithCache() throws Throwable {
        NetworkMarkerManager.useNetworkCondition(NetworkCodeMarkers.ACQUIRE_TOKEN_SILENT, "NONE");

        final IAuthenticationResult result = runAcquireTokenSilent(false);

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
    }

    @Test
    public void test_acquireTokenSilentWithWIFI() throws Throwable {
        NetworkMarkerManager.useNetworkCondition(NetworkCodeMarkers.ACQUIRE_TOKEN_SILENT, "WIFI");

        final IAuthenticationResult result = runAcquireTokenSilent(true);

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
    }

    @Test
    public void test_acquireTokenSilentWithWifiAndMobileData() throws Throwable {
        NetworkMarkerManager.useNetworkCondition(NetworkCodeMarkers.ACQUIRE_TOKEN_SILENT, "WIFI_AND_CELLULAR");
        final IAuthenticationResult result = runAcquireTokenSilent(true);

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
    }
}
