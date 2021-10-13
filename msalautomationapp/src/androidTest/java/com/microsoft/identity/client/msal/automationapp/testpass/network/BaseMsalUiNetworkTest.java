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
package com.microsoft.identity.client.msal.automationapp.testpass.network;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.rules.NetworkTestRule;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Rule;

import java.util.concurrent.CountDownLatch;

/**
 * Sets up a base class for running MSAL network tests.
 */
public abstract class BaseMsalUiNetworkTest extends AbstractMsalUiTest {

    @Rule(order = 20)
    public NetworkTestRule networkTestRule = new NetworkTestRule();

    @Override
    public void setup() {
        super.setup();
    }

    @Override
    public void cleanup() {
        super.cleanup();
        final boolean mainActivityFocused = mActivity.hasWindowFocus();

        if (!mainActivityFocused) {
            UiAutomatorUtils.pressBack();
        }
    }

    @Override
    protected AuthenticationCallback successfulInteractiveCallback(CountDownLatch latch) {
        return new AuthenticationCallback() {
            @Override
            public void onCancel() {
                latch.countDown();
                networkTestRule.setException(new Exception("Interactive flow cancelled by user."));
            }

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                latch.countDown();
                networkTestRule.setResult("Access token: " + authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                latch.countDown();
                networkTestRule.setException(exception);
            }
        };
    }

    @Override
    protected SilentAuthenticationCallback successfulSilentCallback(CountDownLatch latch) {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                latch.countDown();
                networkTestRule.setResult("Access token: " + authenticationResult.getAccessToken());
            }

            @Override
            public void onError(MsalException exception) {
                latch.countDown();
                networkTestRule.setException(exception);
            }
        };
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        return query;
    }

    @Override
    public String getTempUserType() {
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
