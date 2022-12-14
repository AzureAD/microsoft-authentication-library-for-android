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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.flw;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

// End My Shift - In Shared device mode, an account signed in through App A can be used by App B.
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/833514
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class, BrokerHost.class})
@RetryOnFailure
public class TestCase833514 extends AbstractMsalBrokerTest {

    @Test
    public void test_833514() throws MsalException, InterruptedException, LabApiException {
        final String username1 = mLabAccount.getUsername();
        final String password1 = mLabAccount.getPassword();

        // pca should be in MULTIPLE account mode starting out
        Assert.assertTrue(mApplication instanceof MultipleAccountPublicClientApplication);

        //we should NOT be in shared device mode
        Assert.assertFalse(mApplication.isSharedDevice());

        // perform shared device registration
        mBroker.performSharedDeviceRegistration(
                username1, password1
        );

        // re-create PCA after device registration
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(mApplication.isSharedDevice());

        // query to load a user from the same tenant that was used for WPJ
        final LabQuery query = LabQuery.builder()
                .userType(UserType.CLOUD)
                .build();

        // get username and password for this account
        final ILabAccount user2 = mLabClient.getLabAccount(query);
        final String username2 = user2.getUsername();
        final String password2 = user2.getPassword();

        // assert different accounts
        Assert.assertNotEquals(username1, username2);

        // use azure sample app and make sure we do a fresh install
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();

        azureSampleApp.install();
        azureSampleApp.launch();
        azureSampleApp.handleFirstRun();

        final MicrosoftStsPromptHandlerParameters microsoftStsPromptHandlerParameters =
                MicrosoftStsPromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .broker(mBroker)
                        .loginHint(null)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .enrollPageExpected(false)
                        .registerPageExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .expectingLoginPageAccountPicker(false)
                        .isFederated(false)
                        .sessionExpected(false)
                        .build();

        // sign in into Azure Sample App
        azureSampleApp.signInWithSingleAccountFragment(username2, password2, getBrowser(), false, microsoftStsPromptHandlerParameters);

        Thread.sleep(TimeUnit.SECONDS.toMillis(5));

        // make sure we have successfully signed in
        azureSampleApp.confirmSignedIn(username2);

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        final TokenRequestLatch getAccountLatch = new TokenRequestLatch(1);

        final IAccount[] accounts = new IAccount[1];

        // perform get account from MSAL Automation App
        ((SingleAccountPublicClientApplication) mApplication).getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount activeAccount) {
                assert activeAccount != null;
                Assert.assertEquals(activeAccount.getUsername(), username2);
                accounts[0] = activeAccount;
                getAccountLatch.countDown();
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                assert currentAccount != null;
                Assert.assertEquals(currentAccount.getUsername(), username2);
                accounts[0] = currentAccount;
                getAccountLatch.countDown();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail(exception.getMessage());
                getAccountLatch.countDown();
            }
        });

        getAccountLatch.await(TokenRequestTimeout.SILENT);

        Thread.sleep(TimeUnit.SECONDS.toMillis(8));

        final TokenRequestLatch silentLatch = new TokenRequestLatch(1);

        // perform acquire token silent with account used for get account
        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .fromAuthority(getAuthority())
                .forAccount(accounts[0])
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulSilentCallback(silentLatch))
                .build();

        singleAccountPCA.acquireTokenSilentAsync(silentParameters);

        silentLatch.await(TokenRequestTimeout.SILENT);
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
