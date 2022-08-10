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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.joined;

import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

// [Joined][MSAL] Password Change
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1561151
public class TestCase1561151 extends AbstractMsalBrokerTest {
    @Test
    public void test_1561151() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final MsalSdk msalSdk = new MsalSdk();

        // perform device registration
        mBroker.performDeviceRegistration(username, password);

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        // start interactive acquire token request in MSAL (should succeed)
        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(username)
                        .sessionExpected(true)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .expectingBrokerAccountChooserActivity(true)
                        .expectingLoginPageAccountPicker(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();

        /*
            Note that password reset doesn't take effect by ESTS at least user being logged in for 1 min.
            Therefore we have a Thread.sleep after first successful token acquisition before resetting password.
         */
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));
        Assert.assertTrue(mLabClient.resetPassword(username, 3));

        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();

        // SILENT REQUEST - start a acquireTokenSilent request in MSAL
        final IAccount account = msalSdk.getAccount(mActivity, getConfigFileResourceId(), username);
        final MsalAuthTestParams silentParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .authority(account.getAuthority())
                .forceRefresh(true)
                .scopes(Arrays.asList(mScopes))
                .msalConfigResourceId(getConfigFileResourceId())
                .build();


        // get a token silently
        final MsalAuthResult silentAuthResult = msalSdk.acquireTokenSilent(silentParams, TokenRequestTimeout.SILENT);
        silentAuthResult.assertFailure();

        // fetch token in an interactive request
        final MsalAuthTestParams msalAuthTestParams2 = MsalAuthTestParams.builder()
                .loginHint(username)
                .activity(mActivity)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.WHEN_REQUIRED)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResultPostPwdChange = msalSdk.acquireTokenInteractive(msalAuthTestParams2, new com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.WHEN_REQUIRED)
                        .loginHint(username)
                        .sessionExpected(true)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .expectingBrokerAccountChooserActivity(false)
                        .passwordPageExpected(true)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResultPostPwdChange.assertSuccess();
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
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