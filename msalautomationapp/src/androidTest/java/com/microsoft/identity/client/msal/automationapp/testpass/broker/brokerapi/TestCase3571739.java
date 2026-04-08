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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.brokerapi;

import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandler;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.common.java.commands.webapps.WebAppsGetTokenSubOperationEnvelope;
import com.microsoft.identity.common.java.commands.webapps.WebAppsGetTokenSubOperationResponse;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.HomeUpn;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Test;

// WebApps API Tests via BrokerHost Broker API tab
// Tests GetToken (interactive and silent), GetCookies, and SignOut operations
// of the WebApps APIs using the BrokerHost app's Broker API tab.
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3571739
@SupportedBrokers(brokers = BrokerHost.class)
@LocalBrokerHostDebugUiTest
@RetryOnFailure(retryCount = 2)
public class TestCase3571739 extends AbstractMsalBrokerTest {

    private static final String TAG = TestCase3571739.class.getSimpleName();
    private static final String BROKER_HOST_PKG = "com.microsoft.identity.testuserapp";

    // WebApps UI resource IDs
    private static final String INPUT_SENDER_ORIGIN = BROKER_HOST_PKG + ":id/input_sender_origin";
    private static final String INPUT_HOME_ACCOUNT_ID = BROKER_HOST_PKG + ":id/input_home_account_id";
    private static final String INPUT_PROMPT = BROKER_HOST_PKG + ":id/input_prompt";
    private static final String INPUT_LOGIN_HINT = BROKER_HOST_PKG + ":id/input_login_hint";
    private static final String CHECKBOX_CAN_SHOW_UI = BROKER_HOST_PKG + ":id/checkbox_can_show_ui";
    private static final String CHECKBOX_IS_STS = BROKER_HOST_PKG + ":id/checkbox_is_sts";
    private static final String BUTTON_EXECUTE_GET_TOKEN = BROKER_HOST_PKG + ":id/button_execute_get_token";
    private static final String BUTTON_EXECUTE_SIGN_OUT = BROKER_HOST_PKG + ":id/button_execute_sign_out";
    private static final String EDIT_TEXT_COOKIES_URL = BROKER_HOST_PKG + ":id/edit_text_webapps_cookie_url";
    private static final String BUTTON_GET_COOKIES = BROKER_HOST_PKG + ":id/button_get_webapp_cookies";
    private static final String DIALOG_MESSAGE = "android:id/message";
    private static final String DIALOG_OK_BUTTON = "android:id/button1";

    // Constants
    private static final String LEMON_GLACIER = "https://lemon-glacier-0fa89f11e.1.azurestaticapps.net/";
    private static final String MICROSOFT_ONLINE = "https://login.microsoftonline.com";
    private static final String SCOPE = "User.read";
    private static final String ACCESS_TOKEN_DESCRIPTION = "access_token";

    @Test
    public void test_3571739_webAppsOperations() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final BrokerHost brokerHost = (BrokerHost) mBroker;

        // -------- Step 1: Interactive GetToken --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the form fields and execute button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();

        // Fill the WebApps GetToken form for interactive auth
        UiAutomatorUtils.handleInput(INPUT_PROMPT, "select_account");
        UiAutomatorUtils.handleInput(INPUT_LOGIN_HINT, username);
        setCheckbox(CHECKBOX_CAN_SHOW_UI, true);

        // Scroll down to make the execute button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();

        // Click Execute getToken
        UiAutomatorUtils.handleButtonClick(BUTTON_EXECUTE_GET_TOKEN);

        // Handle the interactive auth prompt
        final MicrosoftStsPromptHandlerParameters interactivePromptParams =
                MicrosoftStsPromptHandlerParameters.builder()
                        .prompt(PromptParameter.SELECT_ACCOUNT)
                        .loginHint(username)
                        .sessionExpected(true)
                        .passwordPageExpected(true)
                        .consentPageExpected(false)
                        .build();

        new MicrosoftStsPromptHandler(interactivePromptParams)
                .handlePrompt(username, password);

        // Wait for and read the result dialog
        ThreadUtils.sleepSafely(5000, TAG, "Waiting for interactive result");
        final String interactiveResult = dismissDialogAndGetText();

        Assert.assertNotNull("Interactive GetToken result should not be null", interactiveResult);
        Assert.assertTrue(
                "Interactive GetToken result should contain access_token",
                interactiveResult.contains(ACCESS_TOKEN_DESCRIPTION)
        );

        // Extract the homeAccountId from the interactive result for silent requests
        final int jsonStart = interactiveResult.indexOf("{");
        ObjectMapper.deserializeJsonStringToObject(
                interactiveResult.substring(jsonStart),
                WebAppsGetTokenSubOperationEnvelope.class
        );
        final WebAppsGetTokenSubOperationResponse resultJson = ObjectMapper.deserializeJsonStringToObject(
                interactiveResult.substring(jsonStart),
                WebAppsGetTokenSubOperationResponse.class
        );
        final String homeAccountId = resultJson.getAccount().getHomeAccountId();
        Assert.assertNotNull("homeAccountId should not be null", homeAccountId);

        // -------- Step 2a: Silent GetToken (eSTS) --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the form fields visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();

        // Fill the WebApps GetToken form for silent eSTS auth (isSts defaults to true)
        UiAutomatorUtils.handleInput(INPUT_HOME_ACCOUNT_ID, homeAccountId);
        UiAutomatorUtils.handleInput(INPUT_LOGIN_HINT, username);
        UiAutomatorUtils.handleInput(INPUT_PROMPT, "");
        setCheckbox(CHECKBOX_CAN_SHOW_UI, false);
        setCheckbox(CHECKBOX_IS_STS, true);

        // Scroll down to make the execute button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();

        // Click Execute getToken
        UiAutomatorUtils.handleButtonClick(BUTTON_EXECUTE_GET_TOKEN);

        // Wait for the result dialog (no interactive prompt expected)
        ThreadUtils.sleepSafely(5000, TAG, "Waiting for silent eSTS result");
        final String silentEstsResult = dismissDialogAndGetText();

        Assert.assertNotNull("Silent eSTS GetToken result should not be null", silentEstsResult);
        Assert.assertTrue(
                "Silent eSTS GetToken result should contain access_token",
                silentEstsResult.contains(ACCESS_TOKEN_DESCRIPTION)
        );

        // -------- Step 2b: Silent GetToken (MSAL JS) --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the form fields visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();

        // Set sender origin for MSAL JS
        UiAutomatorUtils.handleInput(INPUT_SENDER_ORIGIN, LEMON_GLACIER);

        // Fill the WebApps GetToken form for silent MSAL JS auth
        UiAutomatorUtils.handleInput(INPUT_HOME_ACCOUNT_ID, homeAccountId);
        UiAutomatorUtils.handleInput(INPUT_LOGIN_HINT, username);
        UiAutomatorUtils.handleInput(INPUT_PROMPT, "");
        setCheckbox(CHECKBOX_CAN_SHOW_UI, false);
        setCheckbox(CHECKBOX_IS_STS, false);

        // Scroll down to make the execute button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();

        // Click Execute getToken
        UiAutomatorUtils.handleButtonClick(BUTTON_EXECUTE_GET_TOKEN);

        // Wait for the result dialog (no interactive prompt expected)
        ThreadUtils.sleepSafely(5000, TAG, "Waiting for silent MSAL JS result");
        final String silentMsalJsResult = dismissDialogAndGetText();

        Assert.assertNotNull("Silent MSAL JS GetToken result should not be null", silentMsalJsResult);
        Assert.assertTrue(
                "Silent MSAL JS GetToken result should contain access_token",
                silentMsalJsResult.contains(ACCESS_TOKEN_DESCRIPTION)
        );

        // -------- Step 3: GetCookies --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the cookies URL field and button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();

        UiAutomatorUtils.handleInput(EDIT_TEXT_COOKIES_URL, MICROSOFT_ONLINE);
        UiAutomatorUtils.handleButtonClick(BUTTON_GET_COOKIES);

        ThreadUtils.sleepSafely(5000, TAG, "Waiting for cookies result");
        final String cookiesResult = dismissDialogAndGetText();

        Assert.assertNotNull("GetCookies result should not be null", cookiesResult);
        Assert.assertFalse(
                "GetCookies result should not be empty",
                cookiesResult.isEmpty()
        );

        // -------- Step 4: SignOut --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the form fields visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();

        UiAutomatorUtils.handleInput(INPUT_HOME_ACCOUNT_ID, homeAccountId);

        new UiScrollable(new UiSelector().scrollable(true)).scrollForward();
        UiAutomatorUtils.handleButtonClick(BUTTON_EXECUTE_SIGN_OUT);

        ThreadUtils.sleepSafely(5000, TAG, "Waiting for sign out result");
        final String signOutResult = dismissDialogAndGetText();

        Assert.assertNotNull("SignOut result should not be null", signOutResult);
        Assert.assertFalse(
                "SignOut result should not indicate failure",
                signOutResult.contains("Failed to sign out")
        );
    }

    /**
     * Reads the text from the dialog box, dismisses it, and returns the text.
     */
    private String dismissDialogAndGetText() {
        final UiObject dialogBox = UiAutomatorUtils.obtainUiObjectWithResourceId(DIALOG_MESSAGE);
        Assert.assertTrue("Dialog box should be present", dialogBox.exists());
        try {
            final String text = dialogBox.getText();
            UiAutomatorUtils.handleButtonClick(DIALOG_OK_BUTTON);
            return text;
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError("Could not read dialog text", e);
        }
    }

    /**
     * Sets a checkbox to the desired state by clicking it if needed.
     */
    private void setCheckbox(final String resourceId, final boolean checked) {
        final UiObject checkbox = UiAutomatorUtils.obtainUiObjectWithResourceId(resourceId);
        try {
            if (checkbox.isChecked() != checked) {
                checkbox.click();
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError("Could not find checkbox: " + resourceId, e);
        }
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
                .homeUpn(HomeUpn.MSIDLAB4)
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{SCOPE};
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
