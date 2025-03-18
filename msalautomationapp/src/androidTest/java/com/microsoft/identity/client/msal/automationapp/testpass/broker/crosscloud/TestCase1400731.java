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

import androidx.annotation.NonNull;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.labapi.utilities.client.LabGuestAccount;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.LabConstants;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// [Joined] Guest Support: Interactive and Silent Auth with MSAL Test app (Authenticator or Company Portal)
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1400731/
@RetryOnFailure(retryCount = 2)
public class TestCase1400731 extends AbstractMsalBrokerTest {

    @Test
    public void test_1400731() throws Throwable {
        // load a guest user account from the Lab
        final LabGuestAccount labGuest = mLabClient.loadGuestAccountFromLab(getLabQuery());

        final String username = "gcidlab@msidlab4.onmicrosoft.com";
        final String password = mLabClient.getPasswordForGuestUser(labGuest);

        //perform device registration
        mBroker.performDeviceRegistration(username, password);

        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .authority(LabConstants.MSID_LAB3)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        // start interactive acquire token request in MSAL (should succeed)
        final MsalAuthResult authResult = msalSdk.acquireTokenInteractive(authTestParams, new com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                // Should be silent
            }
        }, TokenRequestTimeout.MEDIUM);

        Assert.assertFalse(TextUtils.isEmpty(authResult.getAccessToken()));

        final MsalAuthTestParams authTestParams2 = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .authority(LabConstants.MSID_LAB4)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        // start interactive acquire token request in MSAL for msidlab4 (should succeed and be silent)
        final MsalAuthResult authResult2 = msalSdk.acquireTokenInteractive(authTestParams2, new com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                // Should be silent
            }
        }, TokenRequestTimeout.MEDIUM);

        authResult2.assertSuccess();

        // advance clock by more than an hour to expire AT in cache
        getSettingsScreen().forwardDeviceTimeForOneDay();

        final MsalAuthTestParams silentParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .authority(LabConstants.MSID_LAB3)
                .forceRefresh(true)
                .scopes(Arrays.asList(mScopes))
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        // get a token silently for msidlab3
        final MsalAuthResult silentAuthResult = msalSdk.acquireTokenSilent(silentParams, TokenRequestTimeout.SILENT);
        silentAuthResult.assertSuccess();

        final MsalAuthTestParams silentParams2 = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .authority(LabConstants.MSID_LAB4)
                .forceRefresh(true)
                .scopes(Arrays.asList(mScopes))
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        // get a token silently for msidlab4
        final MsalAuthResult silentAuthResult2 = msalSdk.acquireTokenSilent(silentParams2, TokenRequestTimeout.SILENT);
        silentAuthResult2.assertSuccess();
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.GUEST)
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
        return "https://login.microsoftonline.us/common";
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}