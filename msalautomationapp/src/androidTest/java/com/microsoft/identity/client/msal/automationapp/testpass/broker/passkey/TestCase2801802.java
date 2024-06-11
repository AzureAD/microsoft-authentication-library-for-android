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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.passkey;

import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.constants.GlobalConstants;
import com.microsoft.identity.client.ui.automation.device.settings.GoogleSettings;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TestCase2801802 extends AbstractMsalBrokerTest {
    String TAG = this.getClass().getSimpleName();
    // If running locally, the systemPin variable should be updated to your device's PIN.
    // Otherwise, the DevicePinSetupRule sets the PIN to a GlobalConstant.
    String systemPin = GlobalConstants.PIN;
    @Test
    public void test_2801802() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        // Enable Authenticator as a passkey provider.
        final GoogleSettings settings = new GoogleSettings();
        settings.launchAccountListPage();
        UiAutomatorUtils.handleButtonClick("com.android.settings:id/switchWidget");

        // Add an account to AuthApp.
        mBroker.launch();
        UiAutomatorUtils.handleButtonClickSafely("com.android.permissioncontroller:id/permission_allow_button");
        mBroker.handleFirstRun();
        UiAutomatorUtils.handleButtonClickSafely("com.azure.authenticator:id/zero_accounts_add_account_button");
        UiAutomatorUtils.handleButtonClickSafely("com.azure.authenticator:id/add_account_work_btn");
        UiAutomatorUtils.obtainUiObjectWithClassAndDescription(LinearLayout.class, "Sign in 2 of 2").click();
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(username);
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Next");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(password);
        //UiAutomatorUtils.handleInput("accesspass", password);
        UiAutomatorUtils.pressEnter();
        // Adding account requires some extra linking steps.
        // Can't find the next few objects to click using conventional methods, so need to use coordinates.
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(5), TAG, "Wait a few seconds for next screen.");
        final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.click(800, 850);
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(10), TAG, "Wait a few seconds for next screen.");
        device.click(820, 1275);
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(5), TAG, "Wait a few seconds for next screen.");
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Pair your account to the app by clicking this link.");
        UiAutomatorUtils.handleButtonClickSafely("android:id/button1");

        // TODO: below should be steps to register a passkey.
        // Currently these lines are based on adding an AuthApp account, but they will need to be altered when lab accounts have passkeys enabled.
        UiAutomatorUtils.handleButtonClickSafely("com.azure.authenticator:id/account_list_row_account_name");
        //UiAutomatorUtils.obtainUiObjectWithTextAndClassType("Continue", TextView.class).click();
        //UiAutomatorUtils.handleButtonClickSafely("idSIButton9");
        UiAutomatorUtils.handleInput("com.android.systemui:id/lockPassword", systemPin);
        UiAutomatorUtils.pressEnter();
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(15), TAG, "Sleeping while registering");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Done");
        UiAutomatorUtils.handleInput("com.android.systemui:id/lockPassword", systemPin);
        UiAutomatorUtils.pressEnter();

        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(15), TAG, "Sleeping while adding a passkey");

        // Now try to use the passkey to login.
        // TODO: sign in with UPN and without UPN.
        final MsalSdk msalSdk = new MsalSdk();
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
                            .broker(mBroker)
                            .passwordPageExpected(false)
                            .expectingBrokerAccountChooserActivity(false)
                            .choosePasskeyExpected(true)
                            .build();

                    new AadPromptHandler(promptHandlerParameters)
                            .handlePrompt(username, "no password");
                }
            }, TokenRequestTimeout.MEDIUM);
            authResult.assertSuccess();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void test_2801802_tap() throws Throwable {
        final String username = "username";
        final String tap = "tap";

        // Enable Authenticator as a passkey provider.
        final GoogleSettings settings = new GoogleSettings();
        settings.launchAccountListPage();
        UiAutomatorUtils.handleButtonClick("com.android.settings:id/switchWidget");

        // Add an account to AuthApp.
        mBroker.launch();
        UiAutomatorUtils.handleButtonClickSafely("com.android.permissioncontroller:id/permission_allow_button");
        mBroker.handleFirstRun();
        UiAutomatorUtils.handleButtonClickSafely("com.azure.authenticator:id/zero_accounts_add_account_button");
        UiAutomatorUtils.handleButtonClickSafely("com.azure.authenticator:id/add_account_work_btn");
        UiAutomatorUtils.obtainUiObjectWithClassAndDescription(LinearLayout.class, "Sign in 2 of 2").click();
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(username);
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Next");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(tap);
        UiAutomatorUtils.pressEnter();

        // Passkey registration via AuthApp.
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(10), TAG, "Sleeping while waiting for auth.");
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Skip");
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(10), TAG, "Need some time here for loading.");
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Done");
        UiAutomatorUtils.handleInput("com.android.systemui:id/lockPassword", systemPin);
        UiAutomatorUtils.pressEnter();
        ThreadUtils.sleepSafely((int)TimeUnit.SECONDS.toMillis(5), TAG, "Sleeping while adding a passkey");
        UiAutomatorUtils.handleButtonClickForObjectWithTextSafely("Continue");

        // Now try to use the passkey to login.
        final MsalSdk msalSdk = new MsalSdk();
        // With UPN.
        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult;
        try {
            authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
                @Override
                public void handleUserInteraction() {
                    final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                            .prompt(PromptParameter.LOGIN)
                            .loginHint(username)
                            .sessionExpected(false)
                            .consentPageExpected(false)
                            .speedBumpExpected(false)
                            .broker(mBroker)
                            .passwordPageExpected(true)
                            .expectingBrokerAccountChooserActivity(false)
                            .choosePasskeyExpected(true)
                            .systemPin(systemPin)
                            .build();

                    new AadPromptHandler(promptHandlerParameters)
                            .handlePrompt(username, "N/A");
                }
            }, TokenRequestTimeout.MEDIUM);
            authResult.assertSuccess();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        // Without UPN
        final MsalAuthTestParams authTestParams2 = MsalAuthTestParams.builder()
                .activity(mActivity)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult2;
        try {
            authResult2 = msalSdk.acquireTokenInteractive(authTestParams2, new OnInteractionRequired() {
                @Override
                public void handleUserInteraction() {
                    final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                            .prompt(PromptParameter.LOGIN)
                            .sessionExpected(false)
                            .consentPageExpected(false)
                            .speedBumpExpected(false)
                            .broker(mBroker)
                            .passwordPageExpected(true)
                            .expectingBrokerAccountChooserActivity(false)
                            .choosePasskeyExpected(true)
                            .systemPin(systemPin)
                            .build();

                    new AadPromptHandler(promptHandlerParameters)
                            .handlePrompt(username, "N/A");
                }
            }, TokenRequestTimeout.MEDIUM);
            authResult2.assertSuccess();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
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
        return R.raw.msal_config_default;
    }
}
