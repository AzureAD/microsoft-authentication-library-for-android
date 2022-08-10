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
package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.basic;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// [MSAL-Only] A single-tenant app makes a silent request with common authority. It should fail..
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/2016158
public class TestCase2016158 extends AbstractMsalUiTest {

    @Test
    public void test_2016158() throws Throwable{
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final MsalSdk msalSdk = new MsalSdk();

        // Interactive call
        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult1 = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(username)
                        .sessionExpected(false)
                        .consentPageExpected(true)
                        .speedBumpExpected(false)
                        .expectingBrokerAccountChooserActivity(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult1.assertSuccess();

        // Silent with https://login.microsoftonline.com/common
        final MsalAuthTestParams silentParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .authority(getAuthority())
                .resource(mScopes[0])
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult authResult2 = msalSdk.acquireTokenSilent(silentParams, TokenRequestTimeout.MEDIUM);

        // Should fail with an MsalServiceException
        authResult2.assertFailure();
        Assert.assertTrue(authResult2.getException() instanceof MsalServiceException);
    }

    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.BASIC;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"https://graph.windows.net/user.read"};
    }

    @Override
    public String getAuthority() {
        return "https://login.microsoftonline.com/common";
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_no_admin_consent;
    }
}
