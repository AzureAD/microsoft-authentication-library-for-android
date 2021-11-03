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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.usgov.arlington;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;

import java.util.Arrays;

// Broker authentication with PRT with USGov account with instance_aware=true
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/948676
public class TestCase948676 extends AbstractMsalBrokerTest {

    @Test
    public void test_948676() throws Throwable {
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        // perform device registration (will obtain PRT in Broker for supplied account)
        mBroker.performDeviceRegistration(username, password);

        final MsalSdk msalSdk = new MsalSdk();

        // acquiring token
        final MsalAuthTestParams authTestParams =
                MsalAuthTestParams.builder()
                        .activity(mActivity)
                        .loginHint(mLoginHint)
                        .scopes(Arrays.asList(mScopes))
                        .msalConfigResourceId(getConfigFileResourceId())
                        .promptParameter(Prompt.SELECT_ACCOUNT)
                        .build();

        final MsalAuthResult authResult =
                msalSdk.acquireTokenInteractive(
                        authTestParams,
                        new com.microsoft.identity.client.ui.automation.interaction
                                .OnInteractionRequired() {
                            @Override
                            public void handleUserInteraction() {
                                final PromptHandlerParameters promptHandlerParameters =
                                        PromptHandlerParameters.builder()
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
                        },
                        TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.CLOUD;
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_US_GOVERNMENT;
        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[] {"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_instance_aware_common;
    }
}
