// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp;

import androidx.annotation.Nullable;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static com.microsoft.identity.client.msal.automationapp.AcquireTokenTestHelper.failureSilentCallback;
import static com.microsoft.identity.client.msal.automationapp.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.client.msal.automationapp.AcquireTokenTestHelper.successfulInteractiveCallback;
import static com.microsoft.identity.client.msal.automationapp.AcquireTokenTestHelper.successfulSilentCallback;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;

public abstract class AcquireTokenNetworkTest extends AcquireTokenNetworkAbstractTest implements IAcquireTokenNetworkTest {

//    @Override
//    public void handleUserInteraction() {
//        final UiDevice mDevice =
//                UiDevice.getInstance(getInstrumentation());
//
//        final int timeOut = 1000 * 60;
//
//        // login webview
//        mDevice.wait(Until.findObject(By.clazz(WebView.class)), timeOut);
//
//        // Set Password
//        UiObject passwordInput = mDevice.findObject(new UiSelector()
//                .instance(0)
//                .className(EditText.class));
//
//        passwordInput.waitForExists(timeOut);
//        try {
//            passwordInput.setText(LabConfig.getCurrentLabConfig().getLabUserPassword());
//        } catch (UiObjectNotFoundException e) {
//            // may have webview cache
//            //fail(e.getMessage());
//        }
//
//        // Confirm Button Click
//        UiObject buttonLogin = mDevice.findObject(new UiSelector()
//                .instance(1)
//                .className(Button.class));
//
//        buttonLogin.waitForExists(timeOut);
//        try {
//            buttonLogin.click();
//        } catch (UiObjectNotFoundException e) {
//            // may have webview cache
//            //fail(e.getMessage());
//        }
//    }

    protected void performAcquireTokenInteractive() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch, mContext))
                .build();

        mApplication.acquireToken(parameters);
        handleUserInteraction();
        latch.await();
    }

    protected void performAcquireTokenSilent(final boolean forceRefresh,
                                             final @Nullable String errorCode) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        SilentAuthenticationCallback callback = null;

        if (errorCode != null) {
            callback = failureSilentCallback(latch, errorCode, mContext);
        } else {
            callback = successfulSilentCallback(latch, mContext);
        }

        final AcquireTokenSilentParameters silentParameters = new AcquireTokenSilentParameters.Builder()
                .forAccount(getAccount())
                .fromAuthority(getAuthority())
                .withScopes(Arrays.asList(mScopes))
                .forceRefresh(forceRefresh)
                .withCallback(callback)
                .build();

        mApplication.acquireTokenSilentAsync(silentParameters);
        latch.await();
    }

    public void performAcquireTokenSilent(final boolean forceRefresh) throws InterruptedException {
        performAcquireTokenSilent(forceRefresh, null);
    }

    @Test
    public void testAcquireTokenSuccess() throws InterruptedException {
        performAcquireTokenInteractive();
    }

    @Test
    public void testAbc() throws InterruptedException {
        performAcquireTokenInteractive();
        performAcquireTokenInteractive();
    }

    @Test
    public void testAcquireTokenSuccessFollowedBySilentSuccess() throws InterruptedException {
        performAcquireTokenInteractive();
        performAcquireTokenSilent(false);
    }

    @Test
    public void testAcquireTokenSilentSuccessForceRefresh() throws InterruptedException {
        performAcquireTokenInteractive();
        performAcquireTokenSilent(true);
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;
        return query;
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_automation_config;
    }

    @Override
    public String[] getScopes() {
        return USER_READ_SCOPE;
    }

    @Override
    public String getAuthority() {
        return getAccount().getAuthority();
    }
}
