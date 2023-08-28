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
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

// Uninstall scenario - with old Auth app new LTW
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2571565
@LTWTests
@RunOnAPI29Minus
public class TestCase2571565 extends AbstractMsalBrokerTest {

    @Test
    public void test_2571565() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        mBroker.uninstall();

        // Install legacy authenticator app
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator(BrokerMicrosoftAuthenticator.OLD_AUTHENTICATOR_APK, BrokerMicrosoftAuthenticator.AUTHENTICATOR_APK);
        brokerMicrosoftAuthenticator.uninstall();
        brokerMicrosoftAuthenticator.install();

        // Install new LTW
        final BrokerLTW brokerLTW = new BrokerLTW();
        brokerLTW.uninstall();
        brokerLTW.install();

        // AcquireToken Interactively in MsalTestApp
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.uninstall();
        msalTestApp.install();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

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
                .passwordPageExpected(true)
                .speedBumpExpected(false)
                .registerPageExpected(false)
                .enrollPageExpected(false)
                .staySignedInPageExpected(false)
                .verifyYourIdentityPageExpected(false)
                .howWouldYouLikeToSignInExpected(false)
                .build();

        String tokenMsal = msalTestApp.acquireToken(username, password, promptHandlerParametersMsal, true);
        Assert.assertNotNull(tokenMsal);

        // Uninstall legacy authenticator app
        brokerMicrosoftAuthenticator.uninstall();

        // Install new oneAuthTestApp
        final OneAuthTestApp oneAuthTestApp = new OneAuthTestApp();
        oneAuthTestApp.uninstall();
        oneAuthTestApp.install();
        oneAuthTestApp.launch();
        oneAuthTestApp.handleFirstRun();

        // AcquireToken Interactively in OneAuthTestApp, should prompt for password
        final FirstPartyAppPromptHandlerParameters promptHandlerParametersOneAuth = FirstPartyAppPromptHandlerParameters.builder()
                .broker(mBroker)
                .prompt(PromptParameter.LOGIN)
                .loginHint(username)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .sessionExpected(false)
                .passwordPageExpected(true)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .enrollPageExpected(false)
                .build();
        oneAuthTestApp.addFirstAccount(username, password, promptHandlerParametersOneAuth);
        oneAuthTestApp.confirmAccount(username);
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
