package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.common.java.network.NetworkConstants;
import com.microsoft.identity.common.java.network.NetworkInterface;
import com.microsoft.identity.common.java.network.NetworkMarkerManager;
import com.microsoft.identity.common.java.network.NetworkState;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class TestCaseNetwork extends BaseMsalUiNetworkTest {

    @Test
    public void test_acquireTokenInteractiveWithNoInternetConnection() {
        NetworkMarkerManager.getInstance()
                .applyNetworkStates(
                        NetworkConstants.NetworkCodeMarkers.ACQUIRE_TOKEN_INTERACTIVE,
                        new NetworkState(NetworkInterface.NONE)
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
    public void test_acquireTokenSilentWithNoInternetConnection() throws Throwable {
        IAuthenticationResult result = runAcquireTokenSilent(true);

        assertNotNull(result);
    }
}
