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

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ICurrentAccountResult;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SignInParameters;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.SingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserRole;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

// End My Shift - In Shared device mode, global sign out should wait/cancel existing silent requests
// and clean all data.
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/2495140
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class, BrokerHost.class})
public class TestCase2495140 extends AbstractMsalBrokerTest {
    final String TAG = TestCase2495140.class.getSimpleName();

    @Test
    public void test_2495140() throws MsalException, InterruptedException, LabApiException {
        final String username1 = mLabAccount.getUsername();
        final String password1 = mLabAccount.getPassword();
        Logger.i(TAG, "Performing Shared Device Registration.");
        mBroker.performSharedDeviceRegistration(username1, password1);
        mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());

        // pca should now be in SINGLE account mode
        Assert.assertTrue(mApplication instanceof SingleAccountPublicClientApplication);

        // we should be in shared device mode
        Assert.assertTrue(mApplication.isSharedDevice());

        final SingleAccountPublicClientApplication singleAccountPCA =
                (SingleAccountPublicClientApplication) mApplication;

        // There should not be a signed in account at this time
        ICurrentAccountResult currentAccountResult = singleAccountPCA.getCurrentAccount();
        Assert.assertNull("There is already a signed in account...", currentAccountResult.getCurrentAccount());
        Logger.i(TAG, "Fetching another user from same tenant from lab account");
        final LabQuery labQuery = LabQuery.builder()
                .userType(UserType.CLOUD)
                .build();

        final ILabAccount labAccount = mLabClient.getLabAccount(labQuery);

        final String username2 = labAccount.getUsername();
        final String password2 = labAccount.getPassword();

        Assert.assertNotEquals(username1, username2);

        final TokenRequestLatch latch = new TokenRequestLatch(1);
        Logger.i(TAG, "Try sign in with an account from the same tenant");
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(mActivity)
                .withLoginHint(username2)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .build();
        singleAccountPCA.signIn(signInParameters);

        final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                .loginHint(username2)
                .sessionExpected(false)
                .consentPageExpected(false)
                .broker(mBroker)
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .expectingBrokerAccountChooserActivity(false)
                .build();

        AadPromptHandler aadPromptHandler = new AadPromptHandler(promptHandlerParameters);
        aadPromptHandler.handlePrompt(username2, password2);

        latch.await(TokenRequestTimeout.LONG);

        Logger.i(TAG, "Launching azure sample app and confirming user signed in or not.");
        final AzureSampleApp azureSampleApp = new AzureSampleApp();
        azureSampleApp.install();
        azureSampleApp.launch();
        azureSampleApp.confirmSignedIn(username2);
        Logger.i(TAG, "Azure sample verified signed in account.");
        final TokenRequestLatch silentTokenLatch = new TokenRequestLatch(1);

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(getAuthority())
                .withScopes(Collections.singletonList("User.read"))
                .forceRefresh(false)
                .withCallback(new SilentAuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                        silentTokenLatch.countDown();
                    }

                    @Override
                    public void onError(MsalException exception) {
                        Assert.assertEquals("thread interrupted", exception.getMessage());
                        silentTokenLatch.countDown();
                    }
                })
                .build();
        // Advance time by a day to force the silent request to do network call
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();
        Logger.i(TAG, "Performing a silent request from automation app.");
        mApplication.acquireTokenSilentAsync(silentParameters);

        // wait for sometime for the network requests to start from silent call.
        // This is to ensure that silent call reads the data from cache and makes network call
        // before its cleaned up by signout operation
        ThreadUtils.sleepSafely(600, TAG, "Sleep failed");

        final TokenRequestLatch signOutLatch = new TokenRequestLatch(1);
        Logger.i(TAG, "Triggering sign out from the application");
        ((SingleAccountPublicClientApplication) mApplication).signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                signOutLatch.countDown();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Assert.fail("Sign out failed: " + exception.getMessage());
            }
        });

        signOutLatch.await(TokenRequestTimeout.LONG);
        silentTokenLatch.await(TokenRequestTimeout.LONG);

        Logger.i(TAG, "Confirming account is signed out in Azure.");
        azureSampleApp.launch();
        azureSampleApp.confirmSignedIn("None");
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

}
