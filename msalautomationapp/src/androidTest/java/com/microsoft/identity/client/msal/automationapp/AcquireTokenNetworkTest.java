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

import android.content.Intent;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;
import androidx.test.uiautomator.Until;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.internal.testutils.TestUtils;
import com.microsoft.identity.internal.testutils.labutils.LabConfig;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.microsoft.identity.client.msal.automationapp.AcquireTokenTestHelper.failureSilentCallback;
import static com.microsoft.identity.client.msal.automationapp.AcquireTokenTestHelper.getAccount;
import static com.microsoft.identity.client.msal.automationapp.AcquireTokenTestHelper.successfulInteractiveCallback;
import static com.microsoft.identity.client.msal.automationapp.AcquireTokenTestHelper.successfulSilentCallback;
import static com.microsoft.identity.internal.testutils.TestConstants.Scopes.USER_READ_SCOPE;

@RunWith(AndroidJUnit4.class)
public class AcquireTokenNetworkTest extends AcquireTokenNetworkAbstractTest implements IAcquireTokenNetworkTest {

//    @Parameterized.Parameter(0)
//    public LabUserQuery query;
//
//    // creates the test data
//    @Parameterized.Parameters(name = "{index}: Test with query={0} ")
//    public static Collection<Object[]> data() {
//        Object[][] data = new Object[][]{
//                {new AcquireTokenAADTest.AzureWorldWideCloudUser().getLabUserQuery()},
//                {new AcquireTokenAADTest.AzureGermanyCloudUser().getLabUserQuery()},
//                {new AcquireTokenAADTest.AzureUsGovCloudUser().getLabUserQuery()}
//        };
//        return Arrays.asList(data);
//    }


    @Override
    public void handleUserInteraction() {
        final UiDevice mDevice =
                UiDevice.getInstance(getInstrumentation());

        final int timeOut = 1000 * 60;

        // login webview
        mDevice.wait(Until.findObject(By.clazz(WebView.class)), timeOut);

        // Set Password
        UiObject passwordInput = mDevice.findObject(new UiSelector()
                .instance(0)
                .className(EditText.class));

        passwordInput.waitForExists(timeOut);
        try {
            passwordInput.setText(LabConfig.getCurrentLabConfig().getLabUserPassword());
        } catch (UiObjectNotFoundException e) {
            // may have webview cache
            //fail(e.getMessage());
        }

        // Confirm Button Click
        UiObject buttonLogin = mDevice.findObject(new UiSelector()
                .instance(1)
                .className(Button.class));

        buttonLogin.waitForExists(timeOut);
        try {
            buttonLogin.click();
        } catch (UiObjectNotFoundException e) {
            // may have webview cache
            //fail(e.getMessage());
        }
    }

    private void performAcquireTokenInteractive() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(mActivity)
                .withLoginHint(mLoginHint)
                .withScopes(Arrays.asList(mScopes))
                .withCallback(successfulInteractiveCallback(latch))
                .build();

        mApplication.acquireToken(parameters);
        handleUserInteraction();
        latch.await();
    }

    private void performAcquireTokenSilent(final boolean forceRefresh,
                                           final @Nullable String errorCode) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        SilentAuthenticationCallback callback = null;

        if (errorCode != null) {
            callback = failureSilentCallback(latch, errorCode);
        } else {
            callback = successfulSilentCallback(latch);
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
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        device.pressHome();

        // Bring up the default launcher by searching for a UI component
// that matches the content description for the launcher button.
        UiObject allAppsButton = device
                .findObject(new UiSelector().description("Apps"));

// Perform a click on the button to load the launcher.
        try {
            allAppsButton.clickAndWaitForNewWindow();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage("com.android.vending");  //sets the intent to start your app
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);  //clear out any previous task, i.e., make sure it starts on the initial screen
        mContext.startActivity(intent);

        UiObject searchButton = device.findObject(new UiSelector().resourceId("com.android.vending:id/search_bar_hint"));
        try {
            searchButton.waitForExists(1000 * 60);
            searchButton.click();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        UiObject searchTextField = device.findObject(new UiSelector().resourceId("com.android.vending:id/search_bar_text_input"));
        try {
            searchTextField.waitForExists(1000 * 60);
            searchTextField.setText("Authenticator");
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        device.pressEnter();

        UiObject appIconInSearchResult = device.findObject(new UiSelector().resourceId("com.android.vending:id/play_card").descriptionContains("Microsoft Authenticator"));
        try {
            appIconInSearchResult.waitForExists(1000 * 60);
            appIconInSearchResult.click();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        UiObject installButton = device.findObject(new UiSelector().resourceId("com.android.vending:id/right_button"));
        try {
            installButton.waitForExists(1000 * 60);
            installButton.click();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        //performAcquireTokenInteractive();
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

    @Test
    public void testAcquireTokenSilentFailureEmptyCache() throws InterruptedException {
        performAcquireTokenInteractive();

        // clear the cache now
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);

        performAcquireTokenSilent(false, ErrorCodes.NO_ACCOUNT_FOUND_ERROR_CODE);
    }

    @Test
    public void testAcquireTokenSilentSuccessCacheWithNoAccessToken() throws InterruptedException {
        performAcquireTokenInteractive();
        // remove the access token from cache
        TestUtils.removeAccessTokenFromCache(SHARED_PREFERENCES_NAME);

        performAcquireTokenSilent(false);
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
