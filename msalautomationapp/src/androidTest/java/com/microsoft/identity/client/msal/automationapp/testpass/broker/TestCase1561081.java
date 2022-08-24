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

package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.browser.BrowserEdge;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadLoginComponentHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.jwt.JWTParserFactory;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

// [WPJ] WPJ Leave
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1561081
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class})
public class TestCase1561081 extends AbstractMsalBrokerTest {

    final String MY_ACCOUNT_MICROSOFT_URL = "https://myaccount.microsoft.com/";

    @Test
    public void test_1561081() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = (BrokerMicrosoftAuthenticator) mBroker;

        //perform device registration
        brokerMicrosoftAuthenticator.performDeviceRegistration(username, password);

        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams_firstTry = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        //AT interactive acquisition.
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

        authResult.assertSuccess();

        //extract the device id claim from the access token.
        String deviceId =
                (String) IDToken.parseJWT(authResult.getAccessToken()).get("deviceid");

        //this gets the deviceId from the Ui and matches it to the deviceID obtained from the AT
        getAndConfirmDeviceIdFromMyAccount(deviceId, username, password, true);

        //init the release candidate BrokerHost in order to run wpj Leave
        BrokerHost brokerHost = new BrokerHost(BrokerHost.BROKER_HOST_APK_RC);
        if(brokerHost.isInstalled()){
            brokerHost.uninstall();
        }

        brokerHost.install();
        //run wpj leave
        brokerHost.wpjLeave();

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

        //wait for two minutes to ensure the device is unregistered.
        Thread.sleep(TimeUnit.MINUTES.toMinutes(2));

        getAndConfirmDeviceIdFromMyAccount(deviceId, username, password, false);

        deviceId =
                (String) IDToken.parseJWT(authResult.getAccessToken()).get("deviceid");

        //confirm that device ID is null.
        Assert.assertNull(deviceId);
    }

    /**
     * Get's the device registration details from <code>MY_ACCOUNT_MICROSOFT_URL</code> service
     * and confirms the deivce's registration / de-registration status.
     *
     * @param deviceID - The device Id obtained from performDeviceRegistration
     * @param username - LabApi username
     * @param password - LabApi password
     * @param isDeviceRegistered - toggle to enable re-usability of this method across the two states
     *                           this device maybe in, ie REGISTERED / UNREGISTERED.
     * @throws Throwable
     */
    private void getAndConfirmDeviceIdFromMyAccount(String deviceID, String username, String password, boolean isDeviceRegistered) throws  Throwable{
        BrowserChrome chrome  = new BrowserChrome();
        chrome.clear();
        chrome.launch();
        chrome.handleFirstRun();

        chrome.navigateTo(MY_ACCOUNT_MICROSOFT_URL);

        final AadLoginComponentHandler aadLoginComponentHandler = new AadLoginComponentHandler();
        aadLoginComponentHandler.handleEmailField(username);
        aadLoginComponentHandler.handlePasswordField(password);

        UiAutomatorUtils.handleButtonClick("com.android.chrome:id/button_secondary");

        //Click - MANAGE DEVICES
        UiAutomatorUtils.handleButtonClickForObjectWithText("MANAGE DEVICES");

        //Click - Collapsible item
        if(isDeviceRegistered){
            UiAutomatorUtils.handleButtonClickForObjectWithText("Collapsible item");
        }

        UiObject object  = null;

        if(isDeviceRegistered){
            object = UiAutomatorUtils.obtainUiObjectWithText(deviceID);
        } else {
            object = UiAutomatorUtils.obtainUiObjectWithText("No devices to display.");
        }

        Assert.assertTrue(object != null);
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
