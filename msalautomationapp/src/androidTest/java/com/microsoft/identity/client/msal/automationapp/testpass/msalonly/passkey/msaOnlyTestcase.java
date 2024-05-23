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
package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.passkey;

import android.widget.EditText;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.constants.GlobalConstants;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class msaOnlyTestcase extends AbstractMsalUiTest {
    String TAG = this.getClass().getSimpleName();
    // If running locally, the systemPin variable should be updated to your device's PIN.
    // Otherwise, the DevicePinSetupRule sets the PIN to a GlobalConstant.
    String systemPin = "2468";

    @Test
    public void msa_testcase() throws Throwable {
        final String username = "Username";
        final String password = "Password";

        //Register passkey via Chrome
        final BrowserChrome chrome = new BrowserChrome();
        chrome.launch();
        UiAutomatorUtils.handleButtonClickForObjectWithText("Use without an account");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Got it");
        chrome.navigateTo("https://go.microsoft.com/fwlink/?linkid=2250685");
        //UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Sign in to your Microsoft account");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(username);
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Next");
        UiAutomatorUtils.pressBack();
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(password);
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        ThreadUtils.sleepSafely((int) TimeUnit.SECONDS.toMillis(2), TAG, "Wait some seconds for next screen.");
        device.click(880, 1044);
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Email");
        ThreadUtils.sleepSafely((int) TimeUnit.SECONDS.toMillis(40), TAG, "Wait 30 seconds to finish email MFA.");
        //UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Sign in");
        //UiAutomatorUtils.pressEnter();
        //UiAutomatorUtils.handleButtonClickForObjectWithText("No");
        //ThreadUtils.sleepSafely((int) TimeUnit.SECONDS.toMillis(10), TAG, "Wait some seconds for next screen.");
        device.click(480, 577);
        UiAutomatorUtils.handleButtonClickSafely("com.google.android.gms:id/continue_button");
        UiAutomatorUtils.handleInput("com.android.systemui:id/lockPassword", systemPin);
        UiAutomatorUtils.pressEnter();
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(15), TAG, "Sleeping while adding a passkey");

    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.MSA)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_webview;
    }
}
