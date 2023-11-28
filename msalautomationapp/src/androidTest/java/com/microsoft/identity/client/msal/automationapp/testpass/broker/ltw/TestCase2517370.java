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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2517370
@LTWTests
//@RetryOnFailure
@RunOnAPI29Minus
@RunWith(Parameterized.class)
public class TestCase2517370 extends AbstractMsalBrokerTest {

    private final UserType mUserType;

    public TestCase2517370(@NonNull UserType userType) {
        mUserType = userType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<UserType> userType() {
        return Arrays.asList(
                UserType.MSA,
                UserType.CLOUD
        );
    }

    @Test
    public void test_2517370() throws UiObjectNotFoundException {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        // Install and launch One Auth test App
        final OneAuthTestApp oneAuthApp = new OneAuthTestApp();
        oneAuthApp.install();
        oneAuthApp.launch();
        oneAuthApp.handleFirstRun();

        if (mLabAccount.getUserType() == UserType.MSA) {
            oneAuthApp.selectFromAppConfiguration("com.microsoft.OneAuthTestApp");
            oneAuthApp.handleConfigureFlightsButton();
        }

        final FirstPartyAppPromptHandlerParameters promptHandlerParameters = FirstPartyAppPromptHandlerParameters.builder()
                .broker(mBroker)
                .prompt(PromptParameter.LOGIN)
                .loginHint(username)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .sessionExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .enrollPageExpected(false)
                .build();
        oneAuthApp.addFirstAccount(username, password, promptHandlerParameters);
        oneAuthApp.confirmAccount(username);

        // Hit back button to go on launch screen
        oneAuthApp.handleBackButton();

        final String silentToken = oneAuthApp.acquireTokenSilent();
        Assert.assertFalse(TextUtils.isEmpty(silentToken));
        oneAuthApp.assertSuccess();
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(mUserType)
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
