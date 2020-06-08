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

import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.AbstractAcquireTokenNetworkTest;
import com.microsoft.identity.client.msal.automationapp.ErrorCodes;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.broker.BrokerAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

// End My Shift - In Shared device mode, there can be only one sign-in account.
public class TestCase833516 extends AbstractAcquireTokenNetworkTest {

    @Test
    public void test_833516() throws MsalException, InterruptedException {
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

        // query to load a user from a same tenant that was used for WPJ
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;

        final String username = LabUserHelper.loadUserForTest(query);
        String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final CountDownLatch latch = new CountDownLatch(1);

        // try sign in with an account from the same tenant
        singleAccountPCA.signIn(mActivity, username, mScopes, successfulInteractiveCallback(latch));

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHintProvided(true)
                .sessionExpected(false)
                .consentPageExpected(false)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .expectingNonZeroAccountsInBroker(false)
                .build();

        AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username, password);

        latch.await();

        // try sign in with a different account - it should fail

        // query to load another user from the same tenant
        final LabUserQuery query2 = new LabUserQuery();
        query2.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        query2.protectionPolicy = LabConstants.ProtectionPolicy.MAM_CA;

        final String anotherUserFromSameTenant = LabUserHelper.loadUserForTest(query2);
        password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final CountDownLatch latch2 = new CountDownLatch(1);

        // try sign in with an account from the same tenant
        singleAccountPCA.signIn(mActivity, anotherUserFromSameTenant, mScopes, failureInteractiveCallback(latch2, ErrorCodes.INVALID_PARAMETER));

        latch2.await();
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

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return null;
    }

    @Override
    public ITestBroker getBroker() {
        return new BrokerAuthenticator();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common;
    }

}
