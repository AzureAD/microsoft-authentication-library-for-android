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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.flw;

import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// End My Shift - In Shared device mode, only account from the same tenant should be able to acquire token.
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/833513
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class, BrokerHost.class})
public class TestCase833513 extends AbstractMsalBrokerTest {

    @Test
    public void test_833513() throws MsalException, InterruptedException, LabApiException, UiObjectNotFoundException {
        final String username1 = mLabAccount.getUsername();
        final String password1 = mLabAccount.getPassword();

        // query to load a user from a different tenant that is used for WPJ
        final LabQuery query = LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_US_GOVERNMENT)
                .build();

        final ILabAccount difTenantAccount = mLabClient.getLabAccount(query);
        final String username2 = difTenantAccount.getUsername();
        final String password2 = difTenantAccount.getPassword();

        // pca should be in MULTIPLE account mode starting out
        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

        // perform shared device registration
        mBroker.performSharedDeviceRegistration(
                username1, password1
        );

        // re-create PCA after device registration
        final IPublicClientApplication publicClientApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(publicClientApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(publicClientApplication.isSharedDevice());

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) publicClientApplication;

        // try sign in with an account from a different tenant
        // expect failure result from Interactive call
        final TokenRequestLatch tokenRequestLatch = new TokenRequestLatch(1);
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(username2)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(failureInteractiveCallback(tokenRequestLatch, ClientException.BRT_TENANT_MISMATCH))
                .build();
        singleAccountPCA.signIn(signInParameters);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHint(username2)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .build();

        final AadPromptHandler promptHandler = new AadPromptHandler(promptHandlerParameters);
        promptHandler.handlePrompt(username2, password2);

        tokenRequestLatch.await();
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

    @Override
    public String[] getScopes() {
        return new String[]{"user.read"};
    }

    @Override
    public String getAuthority() {
        return null;
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common;
    }
}
