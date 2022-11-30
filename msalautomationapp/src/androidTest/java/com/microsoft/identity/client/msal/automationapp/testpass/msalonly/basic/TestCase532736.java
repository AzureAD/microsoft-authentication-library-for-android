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

import android.webkit.WebView;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.device.settings.ISettings;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

// MSAL Falls Back on WebView When All Browsers are Disabled
// https://identitydivision.visualstudio.com/DevEx/_workitems/edit/532736
public class TestCase532736 extends AbstractMsalUiTest {

    @After
    public void enableChrome() {
        final ISettings settings = getSettingsScreen();
        final BrowserChrome chrome = ((BrowserChrome) mBrowser);

        // Some cleanup after the test concludes
        settings.enableAppThroughSettings(chrome.getPackageName());
    }

    @Test
    public void test_532736() throws Throwable {
        final String username = mLabAccount.getUsername();

        // Disable Chrome
        final ISettings settings = getSettingsScreen();
        final BrowserChrome chrome = ((BrowserChrome) mBrowser);

        settings.disableAppThroughSettings(chrome.getPackageName());

        final MsalSdk msalSdk = new MsalSdk();

        final MsalAuthTestParams authTestParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(username)
                .scopes(Arrays.asList(mScopes))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        msalSdk.acquireTokenInteractive(authTestParams, new OnInteractionRequired() {
            @Override
            public void handleUserInteraction() {
                // Assert that webview UI object exists
                final UiObject webView = UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().className(WebView.class), CommonUtils.FIND_UI_ELEMENT_TIMEOUT);
                Assert.assertTrue(webView.exists());

                // Assert that no objects from Chrome package are present
                final UiObject chromeObject = UiAutomatorUtils.obtainUiObjectWithUiSelector(new UiSelector().packageName("com.android.chrome"), CommonUtils.FIND_UI_ELEMENT_TIMEOUT);
                Assert.assertFalse(chromeObject.exists());
            }
        }, TokenRequestTimeout.SILENT); // This isn't a silent request, but we want to complete it quickly since we're just checking for WebView
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
        return R.raw.msal_config_browser;
    }
}
