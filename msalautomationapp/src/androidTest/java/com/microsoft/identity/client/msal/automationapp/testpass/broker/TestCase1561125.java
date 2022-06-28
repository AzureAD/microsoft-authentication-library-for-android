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
package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.claims.ClaimsRequest;
import com.microsoft.identity.client.claims.RequestedClaimAdditionalInformation;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.common.java.providers.oauth2.IDToken;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;


import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

// [Joined][MSAL] In-line WPJ: Perform Device registration with deviceid claim
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1561125
public class TestCase1561125 extends AbstractMsalBrokerTest {
    @Test
    public void test_1561125() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final MsalSdk msalSdk = new MsalSdk();
        // create claims request object
        final ClaimsRequest claimsRequest = new ClaimsRequest();
        final RequestedClaimAdditionalInformation requestedClaimAdditionalInformation =
                new RequestedClaimAdditionalInformation();

        requestedClaimAdditionalInformation.setEssential(true);

        // request the deviceid claim in ID Token
        claimsRequest.requestClaimInIdToken("deviceid", requestedClaimAdditionalInformation);

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .claims(claimsRequest)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();


        // start interactive acquire token request in MSAL (should succeed)
        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.LOGIN)
                        .loginHint(username)
                        .sessionExpected(false)
                        .registerPageExpected(true)
                        .speedBumpExpected(false)
                        .broker(mBroker)
                        .expectingLoginPageAccountPicker(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult.assertSuccess();
        Map<String, ?> tokens = IDToken.parseJWT(authResult.getAccessToken());
        Assert.assertNotNull(tokens.get("deviceid"));
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


