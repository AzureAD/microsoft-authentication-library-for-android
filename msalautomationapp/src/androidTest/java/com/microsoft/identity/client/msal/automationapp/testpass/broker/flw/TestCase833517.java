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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.constants.AuthScheme;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// End My Shift - In Shared device mode, MSAL should notify the app if the sign-out account is changed.
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/833517
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class})
@RetryOnFailure(retryCount = 2)
public class TestCase833517 extends AbstractMsalBrokerTest {
    final String TAG = TestCase833517.class.getSimpleName();
    @Test
    public void test_833517() throws Throwable{
        final String deviceAdminUsername = mLabAccount.getUsername();
        final String deviceAdminPassword = mLabAccount.getPassword();

        mBroker.performSharedDeviceRegistration(deviceAdminUsername, deviceAdminPassword);
        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) PublicClientApplication.create(mContext, getConfigFileResourceId());

        Logger.i(TAG, "Fetching another user from same tenant from lab account");
        final LabQuery labQuery = LabQuery.builder().userType(UserType.CLOUD).build();
        final ILabAccount labAccount = mLabClient.getLabAccount(labQuery);
        final String username = labAccount.getUsername();
        final String password = labAccount.getPassword();

        final MsalSdk msalSdk = new MsalSdk();
        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .authScheme(AuthScheme.BEARER)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.LOGIN)
                        .loginHint(username)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .expectingBrokerAccountChooserActivity(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();

        // uninstall the Azure Sample app to ensure clean state
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.uninstall();
        Logger.i(TAG, "Launching azure sample app and confirming user signed in or not.");

        // install and launch the Azure Sample app
        azureSampleApp.install();
        azureSampleApp.launch();
        azureSampleApp.confirmSignedIn(username);
        Logger.i(TAG, "Azure sample verified signed in account.");

        final TokenRequestLatch signOutLatch = new TokenRequestLatch(1);
        registerAccountChangeBroadcastReceiver(signOutLatch);

        // Remove account from account settings page
        getSettingsScreen().removeAccount(username);

        signOutLatch.await(TokenRequestTimeout.LONG);

        Logger.i(TAG, "Confirming getCurrentAccount returns null.");
        final IAccount account = singleAccountPCA.getCurrentAccount().getCurrentAccount();
        Assert.assertNull(account);

        Logger.i(TAG, "Confirming account is signed out in Azure Sample.");
        azureSampleApp.launch();
        azureSampleApp.confirmSignedIn("None");
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
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

    private void registerAccountChangeBroadcastReceiver(@NonNull final TokenRequestLatch signOutLatch){
        mContext.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Logger.i(TAG, "Received broadcast.");
                        signOutLatch.countDown();
                    }
                },
                new IntentFilter("android.accounts.LOGIN_ACCOUNTS_CHANGED")
                //[BUG] Removing account from settings page does not trigger the signout broadcast from broker.
                //new IntentFilter("com.microsoft.identity.client.sharedmode.CURRENT_ACCOUNT_CHANGED")
        );
    }
}
