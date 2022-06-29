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

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

import androidx.test.uiautomator.UiObject;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadLoginComponentHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// End My Shift - In Shared device mode, global sign out should work.
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/833515
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class, BrokerHost.class})
public class TestCase833515 extends AbstractMsalBrokerTest {

    final static String MY_APPS_URL = "myapps.microsoft.com";

    @Test
    public void test_833515() throws MsalException, InterruptedException, LabApiException {
        final String username1 = mLabAccount.getUsername();
        final String password1 = mLabAccount.getPassword();

        // pca should be in MULTIPLE account mode starting out
        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

        //we should NOT be in shared device mode
        Assert.assertFalse(mApplication.isSharedDevice());

        // perform shared device registration
        mBroker.performSharedDeviceRegistration(
                username1, password1
        );

        // re-create PCA after device registration
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(mApplication.isSharedDevice());

        // fetching a different user from Lab
        final ILabAccount labAccount = mLabClient.createTempAccount(TempUserType.BASIC);
        final String username2 = labAccount.getUsername();
        final String password2 = labAccount.getPassword();
        Thread.sleep(TimeUnit.SECONDS.toMillis(30));

        Assert.assertNotEquals(username1, username2);

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final CountDownLatch latch = new CountDownLatch(1);

        // try sign in with an account from the same tenant
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(username2)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .build();
        singleAccountPCA.signIn(signInParameters);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHint(username2)
                .sessionExpected(false)
                .consentPageExpected(false)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .expectingBrokerAccountChooserActivity(false)
                .build();

        AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username2, password2);

        latch.await();

        //launching azure sample app and confirming user signed in or not.
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();
        azureSampleApp.install();
        azureSampleApp.launch();
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        azureSampleApp.confirmSignedIn(username2);

        // clearing history of chrome.
        final IBrowser chrome = new BrowserChrome();
        chrome.clear();

        // relaunching chrome after clearing history of chrome.
        chrome.launch();
        chrome.handleFirstRun();
        chrome.navigateTo(MY_APPS_URL);

        // login into myapps from chrome
        final AadLoginComponentHandler aadLoginComponentHandler = new AadLoginComponentHandler();
        aadLoginComponentHandler.handleEmailField(username2);
        aadLoginComponentHandler.handlePasswordField(password2);

        //signing out from the application.
        ((SingleAccountPublicClientApplication) mApplication).signOut();

        //selecting which account should be logged out.
        aadLoginComponentHandler.handleAccountPicker(username2);

        final UiObject signOutConfirmationUrl = UiAutomatorUtils.obtainUiObjectWithText(
                "login.microsoftonline.com/common/oauth2/v2.0/logoutsession"
        );

        Assert.assertTrue(signOutConfirmationUrl.exists());

        // can sometimes take a few seconds to actually be signed out
        Thread.sleep(TimeUnit.SECONDS.toMillis(8));

        //confirming account is signed out in google chrome.
        chrome.launch();
        chrome.navigateTo(MY_APPS_URL);

        // TODO: Anyway to make sure account picker appears? Account picker is not always showing up (sometimes
        //  it just goes to the password screen), but the user is signed out.

        // Attempt to handle Account Picker. If account picker does not show up, do not fail yet,
        // since we could be at the password page, which would also confirm a sign out.
        try {
            aadLoginComponentHandler.handleAccountPicker(username2);
        } catch (AssertionError e) {
            Assert.assertEquals(AadLoginComponentHandler.ACCOUNT_PICKER_DID_NOT_APPEAR_ERROR, e.getMessage());
        }

        // we must see password prompt after sign out
        final UiObject passwordField = UiAutomatorUtils.obtainUiObjectWithResourceId("i0118");
        Assert.assertTrue(passwordField.exists());

        //Confirming account is signed out in Azure.
        azureSampleApp.launch();
        azureSampleApp.confirmSignedIn("None");
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

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userRole(UserRole.CLOUD_DEVICE_ADMINISTRATOR)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }
}
