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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.wpj;

import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadLoginComponentHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

// [WPJ] Get device state
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1561080
// TODO: Revisit once we can pull Prod/RC Brokerhost APKs from pipelines
@Ignore("Chrome Automation is inconsistent")
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class})
public class TestCase1561080 extends AbstractMsalBrokerTest {

    final String MY_ACCOUNT_MICROSOFT_URL = "https://myaccount.microsoft.com/";

    @Test
    public void test_1561080() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = (BrokerMicrosoftAuthenticator) mBroker;

        //perform device registration
        brokerMicrosoftAuthenticator.performDeviceRegistration(username, password);

        BrowserChrome chrome  = new BrowserChrome();
        chrome.clear();
        chrome.launch();
        chrome.handleFirstRun();

        //navigates to myaccount.microsoft.com
        chrome.navigateTo(MY_ACCOUNT_MICROSOFT_URL);

        //handle the login prompts
        final AadLoginComponentHandler aadLoginComponentHandler = new AadLoginComponentHandler();
        aadLoginComponentHandler.handleEmailField(username);
        aadLoginComponentHandler.handlePasswordField(password);

        ThreadUtils.sleepSafely(15000, "Sleep failed", "Interrupted");

        //Goto Manage devices
        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/button_secondary");

        //Click Manage devices
        UiAutomatorUtils.handleButtonClickForObjectWithText("MANAGE DEVICES");

        //open the collapsible container
        UiAutomatorUtils.handleButtonClickForObjectWithText("Collapsible item");

        //Click the Disable lost device button
        UiObject disable_lost_device_btn1 = UiAutomatorUtils.obtainUiObjectWithText("Disable lost device");
        disable_lost_device_btn1.click();

        //Confirm disabling of the device.
        UiObject disable_lost_device_btn2 = UiAutomatorUtils.obtainUiObjectWithText("Disable lost device");
        disable_lost_device_btn2.click();

        // Install BrokerHost.apk
        BrokerHost brokerHost = new BrokerHost(BrokerHost.BROKER_HOST_APK_RC);
        if(brokerHost.isInstalled()){
            brokerHost.uninstall();
        }

        brokerHost.install();

        //run get device state
        String deviceStateResponse = brokerHost.getDeviceState();

        //should be false as it should fail after disabling the device.
        Assert.assertEquals("false",deviceStateResponse);

        //Get the device ID direct from the brokerHost.
        String deviceId = brokerHost.obtainDeviceId();

        //launch msal test app.
        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams_firstTry = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        //AT interactive acquisition. - It should fail
        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams_firstTry, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                        .prompt(PromptParameter.LOGIN)
                        .loginHint(username)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .build();

                new MicrosoftStsPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        //test failure
        authResult.assertFailure();

        brokerHost.launch();

        final String deviceStateResponse2 = brokerHost.getDeviceState();

        // the devise response should be false.
        Assert.assertEquals("false", deviceStateResponse2);

        //Delete the device from idlabs.
        boolean isDeviceDeleted = mLabClient.deleteDevice(mLabAccount.getUsername(), deviceId);

        Assert.assertTrue(isDeviceDeleted);

        //acquire token the second time, this time it should work
        final MsalAuthTestParams authTestParams_SecondTry = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();


        //acquire the token interactively for a second time.
        final MsalAuthResult authResult2 = msalSdk.acquireTokenInteractive(authTestParams_SecondTry, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
                        .prompt(PromptParameter.LOGIN)
                        .loginHint(username)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .build();

                new MicrosoftStsPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult2.assertSuccess();
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
