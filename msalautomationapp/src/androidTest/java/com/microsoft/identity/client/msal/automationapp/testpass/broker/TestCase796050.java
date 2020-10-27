// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.interaction.InteractiveRequest;
import com.microsoft.identity.client.msal.automationapp.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// Multi-accounts for Broker - Add Account in Account Chooser Activity
// The goal of the test case is to ensure that we can add accounts in broker via the
// "Add another account" option in Account Chooser Activity
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/796050
public class TestCase796050 extends AbstractMsalBrokerTest {

    @Test
    public void test_796050() {

        // already created test user
        final String username1 = mLoginHint;
        final String password1 = LabConfig.getCurrentLabConfig().getLabUserPassword();

        // create another temp user
        final String username2 = LabUserHelper.loadTempUser(getTempUserType());
        final String password2 = LabConfig.getCurrentLabConfig().getLabUserPassword();

        Assert.assertNotEquals(username1, username2);

        // perform device registration with one of the accounts (account 1 here)
        mBroker.performDeviceRegistration(
                username1, password1
        );

        final TokenRequestLatch latch = new TokenRequestLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .withPrompt(Prompt.SELECT_ACCOUNT)
                .build();

        // Start interactive token request in MSAL (without login hint)
        final InteractiveRequest interactiveRequest = new InteractiveRequest(
                mApplication,
                parameters,
                new OnInteractionRequired() {
                    @Override
                    public void handleUserInteraction() {
                        // Account Chooser Activity should be displayed by broker after calling
                        // acquire token. In Account Choose Activity, click on "Add another account"
                        // When a username is not provided to the below method, it clicks on
                        // "Add another account"
                        mBroker.handleAccountPicker(null);

                        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                                .prompt(PromptParameter.SELECT_ACCOUNT)
                                .loginHint(null)
                                .sessionExpected(false)
                                .consentPageExpected(false)
                                .speedBumpExpected(false)
                                // already in webview as we handled account picker above
                                // and this would behave the same as no broker
                                .broker(null)
                                .build();

                        // In the WebView AAD login page, login with credentials for the other
                        // account aka Account 2 that we created earlier
                        new AadPromptHandler(promptHandlerParameters)
                                .handlePrompt(username2, password2);

                    }
                }
        );

        interactiveRequest.execute();
        latch.await(TokenRequestTimeout.MEDIUM);

        if (mBroker instanceof BrokerMicrosoftAuthenticator) {
            // Assert Authenticator Account screen has both accounts

            mBroker.launch(); // open Authenticator App

            final UiObject account1 = UiAutomatorUtils.obtainUiObjectWithText(username1);
            Assert.assertTrue(account1.exists()); // make sure account 1 is there

            final UiObject account2 = UiAutomatorUtils.obtainUiObjectWithText(username2);
            Assert.assertTrue(account2.exists()); // make sure account 2 is there
        }

        // NOW change device time (advance clock by more than an hour)

        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();

        // SILENT REQUEST - start a acquireTokenSilent request in MSAL with the Account 2

        final IAccount account = getAccount();

        // Make sure we have the most recent account aka Account 2
        Assert.assertEquals(username2, account.getUsername());

        final TokenRequestLatch silentLatch = new TokenRequestLatch(1);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(account.getAuthority())
                .forceRefresh(true)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulSilentCallback(silentLatch))
                .build();

        // get a token silently
        mApplication.acquireTokenSilentAsync(silentParameters);
        silentLatch.await(TokenRequestTimeout.SILENT);
    }


    @Override
    public LabUserQuery getLabUserQuery() {
        return null;
    }

    @Override
    public String getTempUserType() {
        return LabConstants.TempUserType.MAMCA;
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
