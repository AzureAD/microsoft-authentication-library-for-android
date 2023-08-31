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

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

// If LTW is the active broker, and request is made through Authenticator from OneAuth, nothing should break
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2582288
@LTWTests
@RunOnAPI29Minus("OneAuth test app dependency")
@SupportedBrokers(brokers = {BrokerLTW.class})
public class TestCase2582288 extends AbstractMsalBrokerTest {
    @Test
    public void test_2582288() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        // Install new Authenticator app with broker SDK changes of broker selection logic
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
        brokerMicrosoftAuthenticator.install();

        // Install old MsalTestApp
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.installOldApk();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        // AcquireToken Interactively in MsalTestApp
        final FirstPartyAppPromptHandlerParameters promptHandlerParameters = FirstPartyAppPromptHandlerParameters.builder()
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

        final String tokenMsal = msalTestApp.acquireToken(username, password, promptHandlerParameters, true);
        Assert.assertNotNull(tokenMsal);

        // Install old OneAuthTestApp
        final OneAuthTestApp oneAuthTestApp = new OneAuthTestApp();
        oneAuthTestApp.installOldApk();
        oneAuthTestApp.launch();
        oneAuthTestApp.handleFirstRun();

        // Enter username in AccountName textbox
        oneAuthTestApp.handleUserNameInput(username);

        // Click on getAccessToken
        // User should not be prompted for credentials and token is returned
        oneAuthTestApp.handleSignInWithoutPrompt();
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
