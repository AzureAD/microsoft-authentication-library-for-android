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

import androidx.annotation.NonNull;

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
import com.microsoft.identity.client.ui.automation.interaction.UiResponse;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

// If LTW is the active broker, and request is made through Authenticator from MSAL in non-shared device mode, nothing should break
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2582290
@LTWTests
@RunOnAPI29Minus
@SupportedBrokers(brokers = {BrokerLTW.class})
@RunWith(Parameterized.class)
public class TestCase2582290 extends AbstractMsalBrokerTest {

    private final UserType mUserType;

    public TestCase2582290(@NonNull UserType userType) {
        mUserType = userType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<UserType> userType() {
        return Arrays.asList(
                UserType.MSA,
                UserType.CLOUD
        );
    }

    @Test
    public void test_2582290() throws Throwable{
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        //Install new LTW with broker SDK changes of broker selection logic
        // in supportedBrokers annotation

        // Install updated oneAuthTestApp
        final OneAuthTestApp oneAuthTestApp = new OneAuthTestApp();
        oneAuthTestApp.install();
        oneAuthTestApp.launch();
        oneAuthTestApp.handleFirstRunBasedOnUserType(mUserType);

        // Performs AcquireToken
        // User is Prompted for creds
        final FirstPartyAppPromptHandlerParameters promptHandlerParametersOneAuth = FirstPartyAppPromptHandlerParameters.builder()
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
        oneAuthTestApp.addFirstAccount(username, password, promptHandlerParametersOneAuth);
        oneAuthTestApp.confirmAccount(username);

        // Install new Authenticator with broker SDK changes of broker selection logic
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
        brokerMicrosoftAuthenticator.install();

        // Install old MSALTestApp
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.installOldApk();
        msalTestApp.launch();
        msalTestApp.handleFirstRunBasedOnUserType(mUserType);

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

        // Enter username and click on AcquireToken button
        // Token should be retrieved successfully
        msalTestApp.handleUserNameInput(username);
        final String token = msalTestApp.acquireToken(username, password, promptHandlerParameters, false);
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
        final String removeUserMessage = msalTestApp.removeUserLegacy();
        Assert.assertEquals("The account is successfully removed.", removeUserMessage);
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(mUserType)
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
