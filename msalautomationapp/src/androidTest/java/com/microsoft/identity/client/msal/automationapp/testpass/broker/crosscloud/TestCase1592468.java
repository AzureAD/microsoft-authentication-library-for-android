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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.crosscloud;

import android.text.TextUtils;

import com.microsoft.identity.client.ITenantProfile;
import com.microsoft.identity.client.MultiTenantAccount;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractGuestAccountMsalBrokerUiTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.constants.GlobalConstants;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.GuestHomeAzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.GuestHomedIn;
import com.microsoft.identity.labapi.utilities.constants.SignInAudience;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.microsoft.identity.common.java.exception.ErrorStrings.AUTHORITY_URL_NOT_VALID;

import androidx.annotation.NonNull;

// Acquire token from cross cloud when Device CA is required (via PKeyAuth)
// https://identitydivision.visualstudio.com/DefaultCollection/IDDP/_workitems/edit/1592468
@RunWith(Parameterized.class)
public class TestCase1592468 extends AbstractGuestAccountMsalBrokerUiTest {

    private final GuestHomeAzureEnvironment mGuestHomeAzureEnvironment;
    private final String mHomeCloud;
    private final String mCrossCloud;

    public TestCase1592468(final String name, final @NonNull GuestHomeAzureEnvironment guestHomeAzureEnvironment, final String homeCloud, final String crossCloud) {
        mGuestHomeAzureEnvironment = guestHomeAzureEnvironment;
        mHomeCloud = homeCloud;
        mCrossCloud = crossCloud;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection guestHomeAzureEnvironment() {
        return Arrays.asList(new Object[][]{
                {"AZURE_US_GOV", GuestHomeAzureEnvironment.AZURE_US_GOVERNMENT, /*homeCloud*/"https://login.microsoftonline.us", /*crossCloud*/"https://login.microsoftonline.com"},
        });
    }

    @Test
    public void test_1592468() throws Throwable {
        final String userName = mGuestUser.getHomeUpn();
        final String password = mLabClient.getPasswordForGuestUser(mGuestUser);
        final MsalSdk msalSdk = new MsalSdk();

        //perform device registration for the account
        mBroker.performDeviceRegistration(userName, password);

        final OnInteractionRequired crossCloudInteractionHandler = () -> {
            final PromptHandlerParameters promptHandlerParameters =
                    PromptHandlerParameters.builder()
                    .prompt(PromptParameter.SELECT_ACCOUNT)
                    .loginHint(userName)
                    .broker(mBroker)
                    .staySignedInPageExpected(GlobalConstants.IS_STAY_SIGN_IN_PAGE_EXPECTED)
                    .build();
            final AadPromptHandler promptHandler = new AadPromptHandler(promptHandlerParameters);
            promptHandler.handlePrompt(userName, password);
        };

        final MsalAuthTestParams acquireTokenCrossCloudAuthParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(userName)
                .scopes(Arrays.asList(getScopes()))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .authority(getAuthority())
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        // Set EnableCrossCloudTokenRequests flight to true
        mBroker.setFlights("{'EnableCrossCloudTokenRequests' : 'true'}");

        Logger.setLogger("TEST_LOGGER", (tag, logLevel, message, containsPII) -> {
            // Not expecting the BrokerJoinedAccountController to be used for the cross cloud request
            if(tag.contains("BrokerJoinedAccountController")){
                Assert.fail("unexpected log statement tag: " + tag + " message: " + message);
            }
        });

        // Acquire token interactively from cross cloud
        final MsalAuthResult acquireTokenCrossCloudResult = msalSdk.acquireTokenInteractive(acquireTokenCrossCloudAuthParams, crossCloudInteractionHandler, TokenRequestTimeout.MEDIUM);
        Assert.assertFalse("Verify accessToken is not empty", TextUtils.isEmpty(acquireTokenCrossCloudResult.getAccessToken()));

        JSONObject profileObject = getProfileObjectFromMSGraph(acquireTokenCrossCloudResult.getAccessToken());
        Assert.assertEquals("Verify email returned from graph call matches the username", userName, profileObject.get("mail"));

        // Verify the account object contains tenant profile corresponding to the cross cloud guest account
        MultiTenantAccount account = (MultiTenantAccount) msalSdk.getAccount(mActivity, getConfigFileResourceId(), userName);
        ITenantProfile tenantProfile = account.getTenantProfiles().get(mGuestUser.getGuestLabTenants().get(0));
        Assert.assertNotNull(tenantProfile);
        Assert.assertTrue(tenantProfile.getClaims().get("iss").toString().contains(mCrossCloud));
        Assert.assertTrue(tenantProfile.getClaims().get("idp").toString().contains(mHomeCloud));
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.GUEST)
                .guestHomeAzureEnvironment(mGuestHomeAzureEnvironment)
                .guestHomedIn(GuestHomedIn.HOST_AZURE_AD)
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .build();
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mCrossCloud + "/" + mGuestUser.getGuestLabTenants().get(0);
    }
}
