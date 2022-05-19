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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.blackforest;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestLatch;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import java.util.Arrays;

// Interactive token acquisition with instance_aware=true and with custom claims request requiring
// device auth {"access_token":{"deviceid":{"essential":true}}}
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1116132
public class TestCase1116132 extends AbstractMsalBrokerTest {

    public void test_1116132() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final TokenRequestLatch latch = new TokenRequestLatch(1);

        // create claims request object
        final ClaimsRequest claimsRequest = new ClaimsRequest();
        final RequestedClaimAdditionalInformation requestedClaimAdditionalInformation =
                new RequestedClaimAdditionalInformation();

        requestedClaimAdditionalInformation.setEssential(true);

        // request the deviceid claim in ID Token
        claimsRequest.requestClaimInIdToken("deviceid", requestedClaimAdditionalInformation);

        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .claims(claimsRequest)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        // start interactive acquire token request in MSAL (should succeed after device registration)
        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(username)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .expectingBrokerAccountChooserActivity(false)
                        .expectingLoginPageAccountPicker(false)
                        .registerPageExpected(true)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.LONG);

        authResult.assertSuccess();
    }


    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .azureEnvironment(AzureEnvironment.AZURE_GERMANY_CLOUD_MIGRATED)
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
        return R.raw.msal_config_instance_aware_common;
    }

}
