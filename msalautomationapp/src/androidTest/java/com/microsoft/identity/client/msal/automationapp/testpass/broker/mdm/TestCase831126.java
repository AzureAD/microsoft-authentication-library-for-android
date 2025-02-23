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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.mdm;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static org.junit.Assert.fail;

import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.OutlookApp;
import com.microsoft.identity.client.ui.automation.app.WordApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.IMdmAgent;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.ProtectionPolicy;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// [Joined][MDM] Device Admin MDM: MDM Account with Microsoft Outlook and Word
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/831126
@SupportedBrokers(brokers = {BrokerCompanyPortal.class})
@RetryOnFailure
public class TestCase831126 extends AbstractMsalBrokerTest {

    @Test
    public void test_831126() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final OutlookApp outlook = new OutlookApp(new LocalApkInstaller());

        outlook.install();

        // launch outlook and handle first run
        outlook.launch();
        outlook.handleFirstRun();

        // This went back and forth about needing an additional password prompt in browser, might need to revisit this one
        final FirstPartyAppPromptHandlerParameters promptHandlerParameters = FirstPartyAppPromptHandlerParameters.builder()
                .broker(mBroker)
                .prompt(PromptParameter.LOGIN)
                .loginHint(username)
                .consentPageExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .secondPasswordPageExpected(true)
                .expectingLoginPageAccountPicker(false)
                .enrollPageExpected(true)
                .build();

        // add first account in Outlook
        outlook.addFirstAccount(username, password, promptHandlerParameters);

        // verify go to playstore page to download CP
        mBrowser.handleFirstRun();
        final UiObject goToPlayStoreBtn = UiAutomatorUtils.obtainUiObjectWithText("Go to Google Play");
        if (!goToPlayStoreBtn.waitForExists(FIND_UI_ELEMENT_TIMEOUT)) {
            fail("Go to play store page did not show up");
        }

        // enroll device in MDM via the Company Portal app
        ((IMdmAgent) mBroker).enrollDevice(username, password);

        // re-launch outlook
        outlook.launch();

        final FirstPartyAppPromptHandlerParameters promptHandlerParamsPostEnroll = FirstPartyAppPromptHandlerParameters.builder()
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .sessionExpected(true)
                .build();

        outlook.addFirstAccount(username, password, promptHandlerParamsPostEnroll);
        outlook.onAccountAdded();

        // make sure our Account is in Outlook now
        outlook.confirmAccount(username);

        final WordApp wordApp = new WordApp(new LocalApkInstaller());
        wordApp.install();
        wordApp.launch();
        wordApp.handleFirstRun();

        // Word performs auto login using the account that was previously used in one of the other
        // microsoft apps.
        UiObject fileFetchScreen = UiAutomatorUtils.obtainUiObjectWithText("Fetching your files", CommonUtils.FIND_UI_ELEMENT_TIMEOUT_LONG);
        Assert.assertTrue(fileFetchScreen.exists());

        // confirm that the account appears in Word
        wordApp.confirmAccount(username);
        // advance clock by more than an hour to expire AT in cache
        getSettingsScreen().forwardDeviceTimeForOneDay();

        // again open outlook and confirm that there is no interactive prompt
        outlook.launch();
        outlook.confirmAccount(username);
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .protectionPolicy(ProtectionPolicy.MDM_CA)
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
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}

