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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

// Even Authenticator has the highest priority, if CP already has an artifact, CP will remain the broker.
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2579095
@LTWTests
@RunOnAPI29Minus
@RetryOnFailure
public class TestCase2579095 extends AbstractMsalBrokerTest {

        @Test
        public void test_2579095() throws Throwable {
            final String username = mLabAccount.getUsername();
            final String password = mLabAccount.getPassword();

            mBroker.uninstall();

            // install legacy company portal
            final BrokerCompanyPortal brokerCompanyPortal = new BrokerCompanyPortal(BrokerCompanyPortal.OLD_COMPANY_PORTAL_APK,
                    BrokerCompanyPortal.COMPANY_PORTAL_APK);
            brokerCompanyPortal.install();

            // install old OneAuthTestApp
            final OneAuthTestApp oldOneAuthTestApp = new OneAuthTestApp();
            oldOneAuthTestApp.installOldApk();
            oldOneAuthTestApp.launch();
            handleOneAuthTestAppFirstRunCorrectly(oldOneAuthTestApp);

            // acquire token interactively on OneAuthTestApp
            final FirstPartyAppPromptHandlerParameters promptHandlerParametersOneAuth = FirstPartyAppPromptHandlerParameters.builder()
                    .broker(mBroker)
                    .prompt(PromptParameter.LOGIN)
                    .loginHint(username)
                    .consentPageExpected(false)
                    .speedBumpExpected(false)
                    .sessionExpected(false)
                    .expectingBrokerAccountChooserActivity(false)
                    .expectingLoginPageAccountPicker(false)
                    .enrollPageExpected(false)
                    .build();
            oldOneAuthTestApp.addFirstAccount(username, password, promptHandlerParametersOneAuth);
            oldOneAuthTestApp.confirmAccount(username);

            // install new Authenticator
            final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
            brokerMicrosoftAuthenticator.install();

            // update Company Portal
            brokerCompanyPortal.update();

            // install new MsalTestApp
            final MsalTestApp msalTestApp = new MsalTestApp();
            msalTestApp.uninstall();
            msalTestApp.install();
            msalTestApp.launch();
            msalTestApp.handleFirstRun();

            // acquire token interactively on MsalTestApp and should not get prompt
            final MicrosoftStsPromptHandlerParameters promptHandlerParametersMsal = MicrosoftStsPromptHandlerParameters.builder()
                    .prompt(PromptParameter.SELECT_ACCOUNT)
                    .loginHint(username)
                    .sessionExpected(false)
                    .broker(mBroker)
                    .expectingBrokerAccountChooserActivity(false)
                    .expectingProvidedAccountInBroker(false)
                    .expectingLoginPageAccountPicker(false)
                    .expectingProvidedAccountInCookie(false)
                    .consentPageExpected(false)
                    .passwordPageExpected(false)
                    .speedBumpExpected(false)
                    .registerPageExpected(false)
                    .enrollPageExpected(false)
                    .staySignedInPageExpected(false)
                    .verifyYourIdentityPageExpected(false)
                    .howWouldYouLikeToSignInExpected(false)
                    .build();

            msalTestApp.handleUserNameInput(username);
            String tokenMsal = acquireTokenInMsalTestApp(msalTestApp, username, password, promptHandlerParametersMsal, null, false, false);
            Assert.assertNotNull(tokenMsal);

            // getPackageName on MsalTestApp and should be Company Portal
            msalTestApp.handleBackButton();
            final String activeBroker = msalTestApp.getActiveBrokerPackageName();
            Assert.assertEquals("Active broker pkg name : " + BrokerCompanyPortal.COMPANY_PORTAL_APP_PACKAGE_NAME, activeBroker);
        }

    public String acquireTokenInMsalTestApp(@NonNull final MsalTestApp app,
                                            @NonNull final String username,
                               @NonNull final String password,
                               @NonNull final PromptHandlerParameters promptHandlerParameters,
                               @Nullable final IBrowser browser,
                               final boolean shouldHandleBrowserFirstRun,
                               @NonNull final boolean shouldHandlePrompt) throws UiObjectNotFoundException, InterruptedException {

        final UiObject acquireTokenButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/btn_acquiretoken", 15000);
        scrollToElement(acquireTokenButton);
        acquireTokenButton.click();

        if (promptHandlerParameters.getBroker() == null && browser != null && shouldHandleBrowserFirstRun) {
            // handle browser first run as applicable
            browser.handleFirstRun();
        }
        // handle prompt if needed
        if (shouldHandlePrompt) {
            try {
                final UiObject emailField = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                        "", EditText.class);
                emailField.setText(username);
                final UiObject nextBtn = UiAutomatorUtils.obtainUiObjectWithTextAndClassType(
                        "Next", Button.class);
                nextBtn.click();
            } catch (final UiObjectNotFoundException e) {
                throw new AssertionError("Could not click on object with txt Next");
            }
            final MicrosoftStsPromptHandler microsoftStsPromptHandler = new MicrosoftStsPromptHandler((MicrosoftStsPromptHandlerParameters) promptHandlerParameters);
            microsoftStsPromptHandler.handlePrompt(username, password);
        }

        // get token and return
        final UiObject result = UiAutomatorUtils.obtainUiObjectWithResourceId("com.msft.identity.client.sample.local:id/txt_result", 15000);
        return result.getText();
    }

    private void scrollToElement(UiObject obj) throws UiObjectNotFoundException {
        UiScrollable scrollable = new UiScrollable(new UiSelector().scrollable(true));
        scrollable.scrollIntoView(obj);
        obj.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
    }

    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.BASIC;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}
