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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.foci;

import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LongUIAutomationTest;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.app.OutlookApp;
import com.microsoft.identity.client.ui.automation.app.WordApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

// [Non-joined][FoCl] FoCl (Multi-users) with Outlook and Word
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/833544
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class})
@RetryOnFailure
@LongUIAutomationTest
public class TestCase833544 extends AbstractMsalBrokerTest {

    @Test
    public void test_833544() throws LabApiException {
        // Recent build of authenticator seems to produce a notification popup on device, this blocks some ui we rely on to validate account presence. Disabling notifications will work.
        getSettingsScreen().toggleNotificationsThroughSettings(mBroker.getPackageName());

        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final OutlookApp outlook = new OutlookApp(new LocalApkInstaller());

        outlook.install();
        outlook.launch();
        outlook.handleFirstRun();

        final FirstPartyAppPromptHandlerParameters promptHandlerParameters = FirstPartyAppPromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .broker(mBroker)
                .registerPageExpected(false)
                .enrollPageExpected(false)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .sessionExpected(false)
                .expectingLoginPageAccountPicker(false)
                .expectingBrokerAccountChooserActivity(false)
                .build();

        // add first account to Outlook
        outlook.addFirstAccount(username, password, promptHandlerParameters);
        outlook.onAccountAdded();
        outlook.confirmAccount(username);

        final WordApp wordApp = new WordApp(new LocalApkInstaller());

        // open word
        wordApp.install();
        wordApp.launch();
        wordApp.handleFirstRun();

        // Word auto signs the user into with the account that was in Outlook
        // Sometimes, it might take a bit longer to see this UI page in word app
        final UiObject fileFetchScreen = UiAutomatorUtils.obtainUiObjectWithText("Fetching your files", TimeUnit.SECONDS.toMillis(45));
        Assert.assertTrue(fileFetchScreen.exists());

        // Make sure the account exists in Word
        wordApp.confirmAccount(username);

        // Steps from 833519
        // Make sure a Non-FOCI app (Azure sample in this case) can't see the account
        AzureSampleApp azureSample = new AzureSampleApp();
        azureSample.install();
        azureSample.launch();

        // sign in silently into Azure Sample App, should see account picker and not get signed in
        azureSample.signInSilentlyWithSingleAccountFragment(mBrowser, mBroker, false);

        // Confirm that the account picker did show up
        final UiObject accountPicker = UiAutomatorUtils.obtainUiObjectWithResourceId(CommonUtils.getResourceId(mBroker.getPackageName(), "account_chooser_listView"));
        Assert.assertTrue(accountPicker.exists());

        // Confirm that no account is logged in to AzureSampleApp
        azureSample.forceStop();
        azureSample.launch();
        azureSample.confirmSignedIn("None");

        // fetch another account from lab - someone from a different tenant
        final LabQuery govAccountQuery = LabQuery.builder()
                .userType(UserType.CLOUD)
                .azureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT)
                .build();

        final ILabAccount govAccount = mLabClient.getLabAccount(govAccountQuery);

        final String usernameGov = govAccount.getUsername();
        final String passwordGov = govAccount.getPassword();

        // relaunch Outlook
        outlook.forceStop();
        outlook.launch();

        final FirstPartyAppPromptHandlerParameters outlookPromptParameters =
                FirstPartyAppPromptHandlerParameters.builder()
                        .expectingNonZeroAccountsInTSL(true)
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .broker(mBroker)
                        .consentPageExpected(false)
                        .enrollPageExpected(false)
                        .registerPageExpected(false)
                        .isFederated(false)
                        .expectingBrokerAccountChooserActivity(true)
                        .expectingLoginPageAccountPicker(false)
                        .howWouldYouLikeToSignInExpected(true)
                        .loginHint(usernameGov)
                        .sessionExpected(false)
                        .speedBumpExpected(false)
                        .build();

        // add another account in Outlook
        outlook.addAnotherAccount(usernameGov, passwordGov, outlookPromptParameters);

        // Relaunching word right after outlook sign in is pressed leads to issues, sometimes the user is not signed in
        ThreadUtils.sleepSafely(5000, "Sleep failed", "Interrupted");

        // relaunch Word app
        wordApp.forceStop();
        wordApp.launch();

        // We used to check for a flag to expect what new, which occasionally appears in our testing based on word version
        // Let's just ignore any AssertionErrors that get thrown here, we don't know what to expect before hand anyway
        try {
            // Word shows a Whats New Dialog when the app is launched NEXT TIME after adding first account
            final UiObject whatsNewDialog = UiAutomatorUtils.obtainUiObjectWithResourceId(
                    "com.microsoft.office.word:id/WhatsNewDialogTitleTextView"
            );

            Assert.assertTrue(whatsNewDialog.exists());

            // Click the close btn to close this dialog
            UiAutomatorUtils.handleButtonClick("android:id/button2");
        } catch (AssertionError e){
            Logger.i(TestCase833544.class.getSimpleName(), "What's New Page did not appear: " + e.getMessage());
        }

        final FirstPartyAppPromptHandlerParameters wordPromptParameters =
                FirstPartyAppPromptHandlerParameters.builder()
                        .expectingNonZeroAccountsInTSL(true)
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .broker(mBroker)
                        .consentPageExpected(false)
                        .enrollPageExpected(false)
                        .registerPageExpected(false)
                        .isFederated(true)
                        .expectingBrokerAccountChooserActivity(true)
                        .expectingLoginPageAccountPicker(false)
                        .loginHint(usernameGov)
                        .sessionExpected(true)
                        .speedBumpExpected(false)
                        .build();

        // add another account in Word
        wordApp.addAnotherAccount(usernameGov, passwordGov, wordPromptParameters);

        // make sure this other account is in Word
        wordApp.confirmAccount(usernameGov);
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
