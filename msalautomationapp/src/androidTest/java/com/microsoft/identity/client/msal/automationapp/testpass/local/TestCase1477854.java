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
package com.microsoft.identity.client.msal.automationapp.testpass.local;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

//https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1477854
public class TestCase1477854 extends AbstractMsalUiTest {

    @Ignore
    @Test
    public void test_1477854() throws Throwable {
        /*
            Summary: Execute 3 requests; 1 interactive, followed by 2 silent(which are refresh_in enabled).
            Verify:
            1) interactive access token == first silent access token
            2) second silent access token != first silent access token

            Steps
            a) Interactive returns access token A
            b) Fast forward device time to refresh expire interactively acquired access token but not cause access token regular expiry(in this case move by 1:35 min) - i.e. access token is now refresh expired
            c) Silent returns access token A (Refresh executed In Time(eager) causes background thread to retrieve NEW access token)
            d) Verify interactive and silent are both = to A
            e) Wait 1 minute (for background request to complete)
            f) Second Silent request returns access token B
            g) Verify that 2nd silent request... after refresh in expired... now returns a new AT. Silent 1 response not equal to silent 2 response.
         */

        //acquire token interactively - retain token with lifetime 3hr
        final String username = mLoginHint;
        final String password = LabConfig.getCurrentLabConfig().getLabUserPassword();

        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalAuthResult interactiveResult = msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                final PromptHandlerParameters promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.LOGIN)
                        .loginHint(mLoginHint)
                        .sessionExpected(false)
                        .consentPageExpected(false)
                        .speedBumpExpected(false)
                        .build();

                new AadPromptHandler(promptHandlerParameters)
                        .handlePrompt(username, password);
            }
        }, TokenRequestTimeout.MEDIUM);

        // a) Interactive returns access token A
        interactiveResult.assertSuccess();

        // b) Fast forward device time to refresh expire interactively acquired access token but not cause access token regular expiry(in this case move by 1:35 min) - i.e. access token is now refresh expired
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTime(5700);

        //acquire token silently - will start a background thread to refresh access token,
        //but in this thread the interactively acquired AT(i.e. belonging to authResult from above) will be returned.
        final MsalAuthTestParams authTestParams2 = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();
        final MsalAuthResult firstSilentResult = msalSdk.acquireTokenSilent(authTestParams2, TokenRequestTimeout.MEDIUM);

        // c) Silent returns access token A. Refresh executed In Time(eager) causes background thread to retrieve NEW access token)
        firstSilentResult.assertSuccess();

        // d) Verify interactive and silent are both = to A
        Assert.assertTrue(interactiveResult.isAccessTokenEqual(firstSilentResult.getAccessToken()));

        // e) Wait 1 minute (for background request to complete)
        Thread.sleep(TimeUnit.MINUTES.toMillis(1));

        //acquire token silently for second time, the purpose is to acquire the new AT fetched using the background thread
        //from the above silent call
        final MsalAuthTestParams secondSilentResult = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(mLoginHint)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.LOGIN)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();
        final MsalAuthResult authResult3 = msalSdk.acquireTokenSilent(authTestParams3, TokenRequestTimeout.MEDIUM);
        // f) Second Silent request returns access token B
        authResult3.assertSuccess();

        // g) Verify that 2nd silent request... after refresh in expired... now returns a new AT by verifying Silent 1 response not equal to silent 2 response.
        Assert.assertFalse(firstSilentResult.isAccessTokenEqual(secondSilentResult.getAccessToken()));

    }


    @Override
    public LabUserQuery getLabUserQuery() {
        return null;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"https://msidlab6-my.sharepoint.com/user.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

}
