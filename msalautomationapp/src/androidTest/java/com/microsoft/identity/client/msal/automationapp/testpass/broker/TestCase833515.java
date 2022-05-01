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

import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
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
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// End My Shift - In Shared device mode, global sign out should work.
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/833515
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class, BrokerHost.class})
public class TestCase833515 extends AbstractMsalBrokerTest {

    final static String MY_APPS_URL = "myapps.microsoft.com";

    @Test
    public void test_833515() throws MsalException, InterruptedException {
        // pca should be in MULTIPLE account mode starting out
        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

        //we should NOT be in shared device mode
        Assert.assertFalse(mApplication.isSharedDevice());

        // perform shared device registration
        mBroker.performSharedDeviceRegistration(
                mLoginHint, LabConfig.getCurrentLabConfig().getLabUserPassword()
        );

        // re-create PCA after device registration
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(mApplication.isSharedDevice());

        //creating a basic temp user account
        final String username = LabUserHelper.loadTempUser(LabConstants.TempUserType.BASIC);
        String password = LabConfig.getCurrentLabConfig().getLabUserPassword();
        Thread.sleep(TimeUnit.SECONDS.toMillis(30));


        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final CountDownLatch latch = new CountDownLatch(1);

        // try sign in with an account from the same tenant
        singleAccountPCA.signIn(mActivity, username, mScopes, successfulInteractiveCallback(latch));

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHint(username)
                .sessionExpected(false)
                .consentPageExpected(false)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .expectingBrokerAccountChooserActivity(false)
                .build();

        AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username, password);

        latch.await();

        //launching azure sample app and confirming user signed in or not.
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();
        azureSampleApp.install();
        azureSampleApp.launch();
        Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        azureSampleApp.confirmSignedIn(username);

        //clearing history of chrome.
        final IBrowser chrome = new BrowserChrome();
        chrome.clear();

        //relaunching chrome after clearing history of chrome.
        chrome.launch();
        chrome.handleFirstRun();
        chrome.navigateTo(MY_APPS_URL);

        // login into myapps from chrome
        final AadLoginComponentHandler aadLoginComponentHandler = new AadLoginComponentHandler();
        aadLoginComponentHandler.handleEmailField(username);
        aadLoginComponentHandler.handlePasswordField(password);

        //signing out from the application.
        ((SingleAccountPublicClientApplication) mApplication).signOut();

        //selecting which account should be logged out.
        aadLoginComponentHandler.handleAccountPicker(username);

        final UiObject signOutConfirmationUrl = UiAutomatorUtils.obtainUiObjectWithText(
                "login.microsoftonline.com/common/oauth2/v2.0/logoutsession"
        );

        Assert.assertTrue(signOutConfirmationUrl.exists());

        // can sometimes take a few seconds to actually be signed out
        Thread.sleep(TimeUnit.SECONDS.toMillis(8));

        //confirming account is signed out in google chrome.
        chrome.launch();
        chrome.navigateTo(MY_APPS_URL);
        aadLoginComponentHandler.handleAccountPicker(username);

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
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.userRole = LabConstants.UserRole.CLOUD_DEVICE_ADMINISTRATOR;
        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }
}
