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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.passkey;

import android.widget.EditText;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

public class msaBrokerTestcase extends AbstractMsalBrokerTest {

    @Test
    public void msa_testcase() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        //Register passkey via Chrome
        final BrowserChrome chrome = new BrowserChrome();
        chrome.launch();
        UiAutomatorUtils.handleButtonClickForObjectWithText("Use without an account");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Got it");
        chrome.navigateTo("account.live.com");
        UiAutomatorUtils.handleButtonClickForObjectWithText("Sign in to your Microsoft account");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(username);
        UiAutomatorUtils.handleButtonClickForObjectWithText("Next");
        UiAutomatorUtils.obtainUiObjectWithClassAndIndex(EditText.class, 0).setText(password);
        UiAutomatorUtils.pressEnter();
        // TODO: navigate through registration.
        // TODO: sign in with passkey in broker, with UPN and without UPN.
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.MSA)
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
        return R.raw.msal_config_webview;
    }
}