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
