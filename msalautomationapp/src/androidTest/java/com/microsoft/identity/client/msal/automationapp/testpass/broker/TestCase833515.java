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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

@Ignore
public class TestCase833515 extends AbstractMsalBrokerTest {

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

        // query to load a user from the same tenant that was used for WPJ
        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.CLOUD;

        // get username and password for this account
        final String username = LabUserHelper.loadUserForTest(query);
        String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final CountDownLatch interactiveLatch = new CountDownLatch(1);

        // Sign in into MSAL Automation App with this account we just pulled from LAB API
        singleAccountPCA.signIn(mActivity, username, mScopes, successfulInteractiveCallback(interactiveLatch));

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHint(username)
                .sessionExpected(false)
                .consentPageExpected(false)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .expectingBrokerAccountChooserActivity(false)
                .registerPageExpected(false)
                .expectingLoginPageAccountPicker(false)
                .build();

        AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username, password);

        // use azure sample app and make sure we do a fresh install
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();

        azureSampleApp.install();
        azureSampleApp.launch();
        azureSampleApp.handleFirstRun();

        // the account that was used with MSAL Automation App should already be there in Sample App
        azureSampleApp.confirmSignedIn(username);

        ((IApp) mBrowser).launch();
        ((IApp) mBrowser).handleFirstRun();
        mBrowser.navigateTo("https://myapps.microsoft.com");

        final CountDownLatch getAccountLatch = new CountDownLatch(1);

        final IAccount[] accounts = new IAccount[1];

        // perform get account from MSAL Automation App
        ((SingleAccountPublicClientApplication) mApplication).getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                assert activeAccount != null;
                Assert.assertEquals(activeAccount.getUsername(), username);
                accounts[0] = activeAccount;
                getAccountLatch.countDown();
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                assert currentAccount != null;
                Assert.assertEquals(currentAccount.getUsername(), username);
                accounts[0] = currentAccount;
                getAccountLatch.countDown();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail(exception.getMessage());
                getAccountLatch.countDown();
            }
        });

        getAccountLatch.await();

        final CountDownLatch signOutLatch = new CountDownLatch(1);

        singleAccountPCA.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                Assert.assertTrue(true);
                signOutLatch.countDown();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail(exception.getMessage());
                signOutLatch.countDown();
            }
        });

        signOutLatch.await();

        ((IApp) mBrowser).forceStop();
        ((IApp) mBrowser).launch();
        mBrowser.navigateTo("https://myapps.microsoft.com");

        azureSampleApp.forceStop();
        azureSampleApp.launch();
        azureSampleApp.confirmSignedIn("");
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
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public ITestBroker getBroker() {
        return new BrokerMicrosoftAuthenticator();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

}
