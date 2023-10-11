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
package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.ltw;

import android.text.TextUtils;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.RunOnAPI29Minus;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import org.junit.Assert;
import org.junit.Test;

// Add a UI testcase with update scenarios on OneAuthTest and MsalTest apps
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2517381
@LTWTests
@RunOnAPI29Minus
public class TestCase2517381 extends AbstractMsalBrokerTest {

    @Test
    public void test_2517381 () throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        mBroker.uninstall();

        // install old MsalTestApp then acquires token interactively and silently
        MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.uninstall();
        msalTestApp.installOldApk();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        final MicrosoftStsPromptHandlerParameters promptHandlerParametersMsal = MicrosoftStsPromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .sessionExpected(false)
                .broker(null)
                .expectingBrokerAccountChooserActivity(false)
                .expectingProvidedAccountInBroker(false)
                .expectingLoginPageAccountPicker(false)
                .expectingProvidedAccountInCookie(false)
                .consentPageExpected(false)
                .passwordPageExpected(true)
                .speedBumpExpected(false)
                .registerPageExpected(false)
                .enrollPageExpected(false)
                .staySignedInPageExpected(false)
                .verifyYourIdentityPageExpected(false)
                .howWouldYouLikeToSignInExpected(false)
                .build();

        String token = msalTestApp.acquireToken(username, password, promptHandlerParametersMsal, mBrowser, true, true);
        Assert.assertNotNull(token);

        // then acquire token silently and validate the token
        msalTestApp.handleBackButton();
        String silentToken = msalTestApp.acquireTokenSilent();
        Assert.assertNotNull(silentToken);

        // install old OneAuthTestApp then acquires token interactively and silently
        final OneAuthTestApp oneAuthApp = new OneAuthTestApp();
        oneAuthApp.uninstall();
        oneAuthApp.installOldApk();
        oneAuthApp.launch();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        handleOneAuthTestAppFirstRunCorrectly(oneAuthApp);

        final FirstPartyAppPromptHandlerParameters promptHandlerParametersOneAuth = FirstPartyAppPromptHandlerParameters.builder()
                .broker(null)
                .prompt(PromptParameter.LOGIN)
                .loginHint(username)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .sessionExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .enrollPageExpected(false)
                .build();
        oneAuthApp.addFirstAccount(username, password, promptHandlerParametersOneAuth);
        oneAuthApp.confirmAccount(username);

        // Hit back button to go on launch screen
        oneAuthApp.handleBackButton();

        final String silentTokenOneAuth = oneAuthApp.acquireTokenSilent();
        Assert.assertFalse(TextUtils.isEmpty(silentTokenOneAuth));
        oneAuthApp.assertSuccess();

        // update msal test app
        msalTestApp.update();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        // acquire token interactively and silently without prompting for creds
        final String tokenAfterUpdatedMsal = msalTestApp.acquireToken(username, password, promptHandlerParametersMsal, false);
        Assert.assertNotNull(tokenAfterUpdatedMsal);

        msalTestApp.handleBackButton();
        final String silentTokenAfterUpdatedMsal = msalTestApp.acquireTokenSilent();
        Assert.assertNotNull(silentTokenAfterUpdatedMsal);

        // update oneauth test app
        oneAuthApp.update();
        oneAuthApp.launch();

        // acquire token without prompting for creds
        oneAuthApp.handleUserNameInput(username);
        oneAuthApp.handlePreferBrokerSwitchButton();
        oneAuthApp.selectFromAppConfiguration("com.microsoft.identity.LabsApi.Guest");
        oneAuthApp.handleSignInWithoutPrompt();

        oneAuthApp.handleBackButton();

        // acquire token silently without prompting for creds
        final String tokenAfterUpdatedOneAuth = oneAuthApp.acquireTokenSilent();
        Assert.assertFalse(TextUtils.isEmpty(tokenAfterUpdatedOneAuth));
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
