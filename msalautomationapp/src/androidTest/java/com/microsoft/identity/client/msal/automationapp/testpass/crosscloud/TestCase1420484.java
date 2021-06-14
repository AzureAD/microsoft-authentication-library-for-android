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
//  FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.crosscloud;

import android.text.TextUtils;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.AbstractGuestAccountMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabGuestAccountHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

// Acquiring token for cross cloud guest account (Msal Only)
// https://identitydivision.visualstudio.com/DefaultCollection/IDDP/_workitems/edit/1420484
@RunWith(Parameterized.class)
public class TestCase1420484 extends AbstractGuestAccountMsalUiTest {

    private final String mGuestHomeAzureEnvironment;

    public TestCase1420484(final String name, final String guestHomeAzureEnvironment) {
        mGuestHomeAzureEnvironment = guestHomeAzureEnvironment;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection guestHomeAzureEnvironment() {
        return Arrays.asList(new Object[][]{
                {"AZURE_US_GOV", LabConstants.GuestHomeAzureEnvironment.AZURE_US_GOV},
                {"AZURE_CHINA_CLOUD", LabConstants.GuestHomeAzureEnvironment.AZURE_CHINA_CLOUD},
        });
    }

    /**
     * Tests Acquiring token for Cross cloud Guest account without broker.
     */
    @Test
    public void test_1420484() throws Throwable {
        final String userName = mGuestUser.getHomeUpn();
        final String password = LabGuestAccountHelper.getPasswordForGuestUser(mGuestUser);

        // Handler for Interactive auth call
        final OnInteractionRequired interactionHandler = () -> {
            ((IApp) mBrowser).handleFirstRun();
            final PromptHandlerParameters promptHandlerParameters =
                    PromptHandlerParameters.builder()
                    .prompt(PromptParameter.SELECT_ACCOUNT)
                    .loginHint(userName)
                    .staySignedInPageExpected(true)
                    .speedBumpExpected(true)
                    .build();
            final AadPromptHandler promptHandler = new AadPromptHandler(promptHandlerParameters);
            promptHandler.handlePrompt(userName, password);
        };

        final MsalAuthTestParams acquireTokenAuthParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(userName)
                .scopes(Arrays.asList(getScopes()))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .authority(getAuthority())
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalSdk msalSdk = new MsalSdk();
        // Acquire token interactively
        final MsalAuthResult acquireTokenResult = msalSdk.acquireTokenInteractive(acquireTokenAuthParams, interactionHandler, TokenRequestTimeout.SHORT);
        Assert.assertFalse("Verify accessToken is not empty", TextUtils.isEmpty(acquireTokenResult.getAccessToken()));

        // change the time on the device
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();

        // Acquire token silently
        final MsalAuthResult acquireTokenSilentResult = msalSdk.acquireTokenSilent(acquireTokenAuthParams, TokenRequestTimeout.SHORT);
        Assert.assertFalse("Verify accessToken is not empty", TextUtils.isEmpty(acquireTokenSilentResult.getAccessToken()));

        Assert.assertNotEquals("Silent request gets new access token", acquireTokenSilentResult.getAccessToken(), acquireTokenResult.getAccessToken());
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.GUEST;
        query.guestHomeAzureEnvironment = mGuestHomeAzureEnvironment;
        query.guestHomedIn = LabConstants.GuestHomedIn.HOST_AZURE_AD;
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        return query;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"https://graph.windows.net/.default"};
    }

    @Override
    public String getAuthority() {
        return "https://login.microsoftonline.com/" + mGuestUser.getGuestLabTenants().get(0);
    }
}
