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

import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.FederationProvider;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

@LTWTests
@SupportedBrokers(brokers = {BrokerLTW.class})
public class TestCase2582292 extends AbstractMsalBrokerTest {

    @Test
    public void test_2582292() throws LabApiException, InterruptedException, UiObjectNotFoundException {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        // Install new Auth app with broker SDK changes of broker selection logic
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
        brokerMicrosoftAuthenticator.uninstall();
        brokerMicrosoftAuthenticator.install();

        // Install legacy MSAL Test app (Msal test app with no broker selection logic)
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.installOldApk();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        // Set up shared device mode
        // Open Authenticator app -> ... -> Settings
        brokerMicrosoftAuthenticator.performSharedDeviceRegistration(username, password);

        // Check mode in MSAL test app
        final String mode = msalTestApp.checkMode();
        Assert.assertEquals("Single Account", mode);

        // performs AcquireToken with an account from the a same tenant with the WPJed account.
        final LabQuery query = LabQuery.builder()
                .userType(UserType.CLOUD)
                .build();

        final ILabAccount difAccount = mLabClient.getLabAccount(query);
        final String usernameDif = difAccount.getUsername();
        final String passwordDif = difAccount.getPassword();

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

        String token = msalTestApp.acquireToken(usernameDif, passwordDif, promptHandlerParameters, true);
        Assert.assertNotNull(token);

        // Click on "GetUsers" button
        // You should see the signed in user
        final List<String> users = msalTestApp.getUsers();
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(usernameDif, users.get(0));

        // Click on "RemoveUsers" button
        // Account should be removed from MSAL
        final String msg = msalTestApp.removeUser();
        Assert.assertEquals("The account is successfully removed.", msg);
        Assert.assertEquals(0, msalTestApp.getUsers().size());
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .userRole(UserRole.CLOUD_DEVICE_ADMINISTRATOR)
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
