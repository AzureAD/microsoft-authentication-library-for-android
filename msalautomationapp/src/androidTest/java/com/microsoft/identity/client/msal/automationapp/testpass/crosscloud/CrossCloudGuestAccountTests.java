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

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabGuest;
import com.microsoft.identity.internal.testutils.labutils.LabGuestAccountHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Assert;

import java.util.Arrays;

public class CrossCloudGuestAccountTests {

    private static LabGuest guestUser = null;

    /**
     * Verifies acquiring access token for a cross cloud guest account
     * Steps:
     * 1. Get a cross cloud guest account using lab api
     * (usertype: guest, guestHomeAzureEnvironment: 'azureusgovernment' or 'azurechinacloud', guestHomedIn: "hostazuread", azureEnvironment: "azurecloud")
     * 2. Get password for the guest account using lab api, secret name is lab name part in the homeDomain from the lab api response in step 1 example: "arlmsidlab1"
     * 3. Launch the msal test app /msalautomation app
     * 4. Trigger acquire token interactive (loginhint: homeUpn from the lab api response in step 1, authority: https://login.microsoftonline.com/{tenantId from the lab api response in step1}
     * 5. Enter the password when prompted and proceed
     * 6. Click on "yes" in the "Stay signed in" page to proceed
     * 7. Accept speed bump if applicable
     * 8. Verify that accessToken is returned
     * 9. Advance the device time by 1 day
     * 10. TriggerAcquireToken Silent
     * 11. Verify that access token is returned
     *
     * @param activity        activity for Auth parameters
     * @param browser         browser instance to use
     * @param homeEnvironment Home AzureEnvironment for the account @link LabConstants.GuestHomeAzureEnvironment
     * @param broker          broker instance to use
     * @throws Throwable
     */
    public static void testAcquireToken(
            @NonNull Activity activity,
            @NonNull IBrowser browser,
            @NonNull final String homeEnvironment,
            @Nullable final ITestBroker broker
    ) throws Throwable {

        final LabUserQuery labUserQuery = buildCrossCloudLabGuestUserQuery(homeEnvironment);
        guestUser = LabGuestAccountHelper.loadGuestAccountFromLab(labUserQuery);

        final String userName = guestUser.getHomeUpn();
        final String password = LabGuestAccountHelper.getPasswordForGuestUser(guestUser);

        // Handler for Interactive auth call
        OnInteractionRequired interactionHandler = () -> {
            PromptHandlerParameters promptHandlerParameters = null;
            if (broker != null) {
                promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(userName)
                        .staySignedInPageExpected(true)
                        .broker(broker)
                        .build();
            } else {
                ((IApp) browser).handleFirstRun();
                promptHandlerParameters = PromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(userName)
                        .staySignedInPageExpected(true)
                        .speedBumpExpected(true)
                        .build();
            }

            AadPromptHandler promptHandler = new AadPromptHandler(promptHandlerParameters);
            promptHandler.handlePrompt(userName, password);
        };

        final String guestCloudUrl = LabConfig.getCurrentLabConfig().getAuthority() + guestUser.getGuestLabTenants().get(0);

        MsalAuthTestParams acquireTokenAuthParams = MsalAuthTestParams.builder()
                .activity(activity)
                .loginHint(userName)
                .scopes(Arrays.asList(getScopes()))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .authority(guestCloudUrl)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalSdk msalSdk = new MsalSdk();
        // Acquire token interactively
        MsalAuthResult acquireTokenResult = msalSdk.acquireTokenInteractive(acquireTokenAuthParams, interactionHandler, TokenRequestTimeout.SHORT);
        Assert.assertFalse("Verify accessToken is not empty", TextUtils.isEmpty(acquireTokenResult.getAccessToken()));

        // change the time on the device
        TestContext.getTestContext().getTestDevice().getSettings().forwardDeviceTimeForOneDay();

        // Acquire token silently
        MsalAuthResult acquireTokenSilentResult = msalSdk.acquireTokenSilent(acquireTokenAuthParams, TokenRequestTimeout.SHORT);
        //Assert.assertEquals("accessToken is same", acquireTokenSilentResult.getAccessToken(), acquireTokenResult.getAccessToken());
        Assert.assertFalse("Verify accessToken is not empty", TextUtils.isEmpty(acquireTokenSilentResult.getAccessToken()));
    }

    private static LabUserQuery buildCrossCloudLabGuestUserQuery(String guestHomeAzureEnvironment) {
        final LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.GUEST;
        query.guestHomeAzureEnvironment = guestHomeAzureEnvironment;
        query.guestHomedIn = LabConstants.GuestHomedIn.HOST_AZURE_AD;
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        return query;
    }

    public static String[] getScopes() {
        return new String[]{"https://graph.windows.net/.default"};
    }

    public static int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}
