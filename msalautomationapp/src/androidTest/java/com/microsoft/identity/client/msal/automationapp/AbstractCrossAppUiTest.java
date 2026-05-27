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
package com.microsoft.identity.client.msal.automationapp;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.microsoft.identity.client.ui.automation.app.NativeAuthSampleApp;
import com.microsoft.identity.client.ui.automation.rules.CopyFileRule;
import com.microsoft.identity.labapi.utilities.BuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.client.LabClient;

import org.junit.Before;
import org.junit.Rule;

/**
 * A lightweight base class for cross-app UI automation tests that need Lab API access
 * and UiDevice, but do NOT need MSAL's PublicClientApplication, or msalautomationapp's
 * MainActivity.
 *
 * Includes CopyFileRule to copy APKs from /sdcard/ to /data/local/tmp/ (for Firebase pipeline).
 * Uninstalls and reinstalls NativeAuthSampleApp before each test for a clean state.
 *
 * Use this instead of {@link AbstractMsalUiTest} when your test launches a separate app
 * (e.g., NativeAuthSampleApp) rather than testing MSAL in-process.
 */
public abstract class AbstractCrossAppUiTest {

    protected UiDevice mDevice;
    protected LabClient mLabClient;

    @Rule
    public CopyFileRule mCopyFileRule = new CopyFileRule();

    @Before
    public void baseSetup() {
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                BuildConfig.LAB_CLIENT_SECRET
        );
        mLabClient = new LabClient(authenticationClient);

        // Uninstall and reinstall NativeAuthSampleApp to ensure clean state
        final NativeAuthSampleApp nativeAuthApp = new NativeAuthSampleApp();
        if (nativeAuthApp.isInstalled()) {
            nativeAuthApp.uninstall();
        }
        nativeAuthApp.install();
    }
}
