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
package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.ltw;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

@LTWTests
@RunOnAPI29Minus
public class TestCase2582291 extends AbstractMsalBrokerTest {

    @Test
    public void test_2582291() throws Throwable{
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        mBroker.uninstall();

        // Install new LTW with broker SDK changes of broker selection logic
        final BrokerLTW brokerLTW = new BrokerLTW();
        brokerLTW.uninstall();
        brokerLTW.install();

        // Install new CP app with broker SDK changes of broker selection logic
        final BrokerCompanyPortal brokerCompanyPortal = new BrokerCompanyPortal();
        brokerCompanyPortal.uninstall();
        brokerCompanyPortal.install();

        // Install old MSALTestApp
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.uninstall();
        msalTestApp.installOldApk();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        // Click on "AcquireToken" button
        // User is Prompted for creds
        final MicrosoftStsPromptHandlerParameters promptHandlerParameters = MicrosoftStsPromptHandlerParameters.builder()
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

        // Enter username and pwd from step 2 to complete the acquire token flow
        // Token should be retrieved succeffully
        final String token = msalTestApp.acquireToken(username, password, promptHandlerParameters, true);
        Assert.assertNotNull(token);
        msalTestApp.handleBackButton();

        // Click on "Get Users" button
        // The user account should be shown in the UI
        final List<String> users = msalTestApp.getUsers();
        Assert.assertTrue(users.size() == 1);
        Assert.assertTrue(users.get(0).contains(username));
        msalTestApp.handleBackButton();

        // Click on "Acquire Token Silent" button
        // Token should be retrieved successfully
        final String silentToken = msalTestApp.acquireTokenSilent();
        Assert.assertNotNull(silentToken);
        msalTestApp.handleBackButton();

        // Select the Auth scheme as "POP"
        msalTestApp.selectFromAuthScheme("POP");

        // Click on "Generate SHR" button
        // UI should be updated with an SHR token
        final String shrToken = msalTestApp.generateSHR();
        Assert.assertNotNull(shrToken);
        msalTestApp.handleBackButton();

        // Click on "Remove User" button
        // UI updated with message "The account is successfully removed"
        final String removeUserMessage = msalTestApp.removeUser();
        Assert.assertEquals("The account is successfully removed.", removeUserMessage);

        // Install updated oneAuthTestApp
        final OneAuthTestApp oneAuthTestApp = new OneAuthTestApp();
        oneAuthTestApp.uninstall();
        oneAuthTestApp.install();
        oneAuthTestApp.launch();
        oneAuthTestApp.handleFirstRun();

        // Enter username in account name
        oneAuthTestApp.handleUserNameInput(username);

        // Click on getAccessToken
        // Accesstoken should be retrieved successully
        final String accessToken = oneAuthTestApp.acquireTokenSilent();
        Assert.assertNotNull(accessToken);
        oneAuthTestApp.assertSuccess();
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
