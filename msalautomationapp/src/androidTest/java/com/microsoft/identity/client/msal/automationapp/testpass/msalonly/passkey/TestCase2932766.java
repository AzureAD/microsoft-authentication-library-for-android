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

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

// Please note that this test is meant to be run locally.
// You must update the username, password, and systemPin variables to suit your own test account and device.
// Comment out @Ignore before running.
// Remember to delete your passkey in Password Manager and from your account at the conclusion of this test.
@Ignore("Ignoring as this test is not able to be run on the pipeline yet. This line can be commented out when running locally.")
public class TestCase2932766 extends AbstractMsalUiTest {
    String TAG = this.getClass().getSimpleName();

    @Test
    public void test_2932766() throws Throwable {
        // Update the username and password values.
        final String username = "PUT USERNAME HERE";
        final String password = "PUT PASSWORD HERE";
        // The systemPin variable should be updated to your device's PIN.
        // In the future, when this test gets added to the pipeline, the DevicePinSetupRule sets the PIN to a GlobalConstant.
        String systemPin = "1111";

        //Register passkey via Chrome
        final BrowserChrome chrome = new BrowserChrome();
        chrome.launch();
        UiAutomatorUtils.handleButtonClickForObjectWithText("Use without an account");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Got it");
        chrome.navigateTo("https://go.microsoft.com/fwlink/?linkid=2250685");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(username);
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Next");
        UiAutomatorUtils.pressBack();
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(password);
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        ThreadUtils.sleepSafely((int) TimeUnit.SECONDS.toMillis(1), TAG, "Wait some seconds for next screen.");
        device.click(850, 960);
        ThreadUtils.sleepSafely((int) TimeUnit.SECONDS.toMillis(10), TAG, "Wait some seconds for next screen.");
        device.click(450, 560);
        UiAutomatorUtils.handleButtonClickSafely("com.google.android.gms:id/continue_button");
        UiAutomatorUtils.handleInput("com.android.systemui:id/lockPassword", systemPin);
        UiAutomatorUtils.pressEnter();
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(10), TAG, "Sleeping while adding a passkey");
        device.click(750, 700);
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(2), TAG, "Wait some seconds for next screen.");
        device.click(750, 700);
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(2), TAG, "Wait some seconds for next screen.");

        // Now try to use the passkey to login.
        final MsalSdk msalSdk = new MsalSdk();



        // First, the UPN provided scenario. Vast majority of MSA passkey scenarios will be this one.
        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult;
        try {
            authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
                @Override
                public void handleUserInteraction() {
                    final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                            .prompt(PromptParameter.SELECT_ACCOUNT)
                            .loginHint(username)
                            .sessionExpected(false)
                            .consentPageExpected(false)
                            .speedBumpExpected(false)
                            .passwordPageExpected(false)
                            .choosePasskeyExpected(true)
                            .systemPin(systemPin)
                            .build();

                    new AadPromptHandler(promptHandlerParameters)
                            .handlePrompt(username, "");
                }
            }, TokenRequestTimeout.MEDIUM);
            authResult.assertSuccess();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // Second, the UPN-less scenario. Very few scenarios for this option.
//        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
//                .activity(mActivity)
//                .scopes(Arrays.asList(mScopes))
//                .promptParameter(Prompt.SELECT_ACCOUNT)
//                .msalConfigResourceId(getConfigFileResourceId())
//                .build();
//
//        final MsalAuthResult authResult;
//        try {
//            authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
//                @Override
//                public void handleUserInteraction() {
//                    final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
//                            .prompt(PromptParameter.SELECT_ACCOUNT)
//                            .sessionExpected(false)
//                            .consentPageExpected(false)
//                            .speedBumpExpected(false)
//                            .passwordPageExpected(false)
//                            .choosePasskeyExpected(true)
//                            .systemPin(systemPin)
//                            .build();
//
//                    new AadPromptHandler(promptHandlerParameters)
//                            .handlePrompt("", "");
//                }
//            }, TokenRequestTimeout.MEDIUM);
//            authResult.assertSuccess();
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
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
        return R.raw.msal_config_msa_webview;
    }
}
