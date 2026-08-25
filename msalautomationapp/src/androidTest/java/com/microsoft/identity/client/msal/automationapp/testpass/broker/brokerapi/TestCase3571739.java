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
import com.microsoft.identity.common.java.commands.webapps.WebAppsGetTokenSubOperationResponse;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

// WebApps API Tests via BrokerHost Broker API tab
// End-to-end coverage of the WebApps APIs using the BrokerHost app's Broker API tab, exercising the
// lookup-mode (Edge token binding) scenario end to end:
//   Step 1: an interactive GetToken sent as a lookup-mode request (nativebroker=1 +
//           nativebroker_mode=Lookup extra params, the values ESTS sends for a lookup request).
//           A lookup-mode request establishes the PRT-backed broker account and registers the
//           client, but is NOT persisted to the per-clientId MSAL token cache. Because the
//           lookup-mode response carries "none" for the tokens, we only assert that a successful
//           (non-error) response comes back, then read the homeAccountId from it.
//   Step 2: a silent MSAL JS GetToken supplying only that homeAccountId (no login hint), like the
//           real MSAL JS client. This is the request that used to fail with UiRequired: the broker
//           must resolve the account from the PRT/broker-account store keyed by homeAccountId
//           rather than from the (empty) per-clientId MSAL token cache. It must succeed without
//           falling back to an interactive account picker.
// GetCookies, GetAllSsoTokens, and SignOut are also exercised.
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3571739
@SupportedBrokers(brokers = BrokerHost.class)
@LocalBrokerHostDebugUiTest
@RetryOnFailure(retryCount = 2)
public class TestCase3571739 extends AbstractMsalBrokerTest {

    private static final String BROKER_HOST_PKG = "com.microsoft.identity.testuserapp";

    // WebApps UI resource IDs
    private static final String INPUT_SENDER_ORIGIN = BROKER_HOST_PKG + ":id/input_sender_origin";
    private static final String INPUT_HOME_ACCOUNT_ID = BROKER_HOST_PKG + ":id/input_home_account_id";
    private static final String INPUT_PROMPT = BROKER_HOST_PKG + ":id/input_prompt";
    private static final String INPUT_LOGIN_HINT = BROKER_HOST_PKG + ":id/input_login_hint";
    private static final String INPUT_EXTRA_PARAMS_KEYS = BROKER_HOST_PKG + ":id/input_extra_params_keys";
    private static final String INPUT_EXTRA_PARAMS_VALUES = BROKER_HOST_PKG + ":id/input_extra_params_values";
    private static final String CHECKBOX_CAN_SHOW_UI = BROKER_HOST_PKG + ":id/checkbox_can_show_ui";
    private static final String CHECKBOX_IS_STS = BROKER_HOST_PKG + ":id/checkbox_is_sts";
    private static final String BUTTON_EXECUTE_GET_TOKEN = BROKER_HOST_PKG + ":id/button_execute_get_token";
    private static final String BUTTON_EXECUTE_SIGN_OUT = BROKER_HOST_PKG + ":id/button_execute_sign_out";
    private static final String EDIT_TEXT_COOKIES_URL = BROKER_HOST_PKG + ":id/edit_text_webapps_cookie_url";
    private static final String BUTTON_GET_COOKIES = BROKER_HOST_PKG + ":id/button_get_webapp_cookies";
    private static final String BUTTON_GET_ALL_SSO_TOKENS = BROKER_HOST_PKG + ":id/button_get_sso_tokens";
    private static final String TEXT_GET_ALL_SSO_TOKENS = BROKER_HOST_PKG + ":id/edit_sso_token";
    private static final String DIALOG_MESSAGE = "android:id/message";
    private static final String DIALOG_OK_BUTTON = "android:id/button1";

    // Constants
    private static final String LEMON_GLACIER = "https://lemon-glacier-0fa89f11e.1.azurestaticapps.net/";
    private static final String MICROSOFT_ONLINE = "https://login.microsoftonline.com";
    private static final String PRT_COOKIE_NAME = "x-ms-RefreshTokenCredential";
    private static final String SCOPE = "User.Read";
    // Extra token-body params (semicolon-separated key/value lists for the BrokerHost form) that mark
    // the request as an ESTS lookup-mode request. These are the values ESTS sends for a lookup request
    // (AuthenticationConstants.Broker.NATIVEBROKER_KEY/VALUE and NATIVEBROKER_MODE_KEY/LOOKUP_MODE_VALUE).
    private static final String LOOKUP_MODE_EXTRA_PARAM_KEYS = "nativebroker;nativebroker_mode";
    private static final String LOOKUP_MODE_EXTRA_PARAM_VALUES = "1;Lookup";
    private static final String FAILURE_MESSAGE_PREFIX = "Failed to getToken";
    private static final String ACCESS_TOKEN_DESCRIPTION = "access_token";
    private static final long DIALOG_WAIT_TIMEOUT_MS = 5000L;

    @Test
    public void test_3571739_webAppsOperations() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        final BrokerHost brokerHost = (BrokerHost) mBroker;

        // -------- Step 1: Interactive GetToken (lookup mode) --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the form fields and execute button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(INPUT_LOGIN_HINT));

        // Fill the WebApps GetToken form for interactive auth.
        // Send the lookup-mode extra params (nativebroker=1 + nativebroker_mode=Lookup) so the broker
        // treats this as an ESTS lookup-mode request: it establishes the PRT-backed broker account and
        // registers the client, but does NOT persist the token to the per-clientId MSAL token cache.
        // A lookup-mode request is ESTS-originated, so isSts is set true.
        setCheckbox(CHECKBOX_CAN_SHOW_UI, true);
        setCheckbox(CHECKBOX_IS_STS, true);
        UiAutomatorUtils.handleInput(INPUT_PROMPT, "select_account");
        UiAutomatorUtils.handleInput(INPUT_LOGIN_HINT, username);
        UiAutomatorUtils.handleInput(INPUT_EXTRA_PARAMS_KEYS, LOOKUP_MODE_EXTRA_PARAM_KEYS);
        UiAutomatorUtils.handleInput(INPUT_EXTRA_PARAMS_VALUES, LOOKUP_MODE_EXTRA_PARAM_VALUES);

        // Scroll down to make the execute button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(BUTTON_EXECUTE_GET_TOKEN));

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

        final String interactiveResult = dismissDialogAndGetText();

        // A lookup-mode response carries "none" for the tokens, so we only assert that a successful
        // (non-error) response came back -- not that it contains an access token.
        Assert.assertNotNull("Interactive lookup-mode GetToken result should not be null", interactiveResult);
        Assert.assertFalse(
                "Interactive lookup-mode GetToken should return a successful response, but failed: " + interactiveResult,
                interactiveResult.contains(FAILURE_MESSAGE_PREFIX)
        );

        // Extract the homeAccountId from the interactive result for the silent request. The
        // lookup-mode response still includes the account, so the homeAccountId is available here.
        final int jsonStart = interactiveResult.indexOf("{");
        if (jsonStart < 0) {
            throw new AssertionError("Interactive result does not contain JSON: " + interactiveResult);
        }
        final WebAppsGetTokenSubOperationResponse resultJson = ObjectMapper.deserializeJsonStringToObject(
                interactiveResult.substring(jsonStart),
                WebAppsGetTokenSubOperationResponse.class
        );
        Assert.assertNotNull(
                "Interactive lookup-mode response did not contain an account object: " + interactiveResult,
                resultJson.getAccount()
        );
        final String homeAccountId = resultJson.getAccount().getHomeAccountId();
        Assert.assertNotNull("homeAccountId should not be null", homeAccountId);

        // -------- Step 2: Silent GetToken (MSAL JS) --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the form fields visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(INPUT_LOGIN_HINT));

        // Set sender origin for MSAL JS
        UiAutomatorUtils.handleInput(INPUT_SENDER_ORIGIN, LEMON_GLACIER);

        // Fill the WebApps GetToken form for a silent MSAL JS request. This is the request that used
        // to fail with UiRequired after a lookup-mode establishing request: the per-clientId MSAL
        // token cache is empty (lookup mode never wrote to it), so the broker must resolve the account
        // from the PRT/broker-account store keyed by the homeAccountId. The real MSAL JS client sends
        // only the homeAccountId with no login hint, and its direct (non-ESTS) request is not lookup
        // mode, so the lookup-mode extra params are cleared here.
        setCheckbox(CHECKBOX_CAN_SHOW_UI, false);
        setCheckbox(CHECKBOX_IS_STS, false);
        UiAutomatorUtils.handleInput(INPUT_HOME_ACCOUNT_ID, homeAccountId);
        UiAutomatorUtils.handleInput(INPUT_PROMPT, "");
        UiAutomatorUtils.handleInput(INPUT_LOGIN_HINT, "");
        UiAutomatorUtils.handleInput(INPUT_EXTRA_PARAMS_KEYS, "");
        UiAutomatorUtils.handleInput(INPUT_EXTRA_PARAMS_VALUES, "");

        // Scroll down to make the execute button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(BUTTON_EXECUTE_GET_TOKEN));

        // Click Execute getToken
        UiAutomatorUtils.handleButtonClick(BUTTON_EXECUTE_GET_TOKEN);

        final String silentMsalJsResult = dismissDialogAndGetText();

        Assert.assertNotNull("Silent MSAL JS GetToken result should not be null", silentMsalJsResult);
        // The silent MSAL JS request must succeed with an access token (resolved from the PRT-backed
        // account) and must NOT fall back to an interactive account picker.
        Assert.assertTrue(
                "Silent MSAL JS GetToken result should contain access_token, but was: " + silentMsalJsResult,
                silentMsalJsResult.contains(ACCESS_TOKEN_DESCRIPTION)
        );

        // -------- Step 3: GetCookies --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the cookies URL field and button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(BUTTON_GET_COOKIES));

        UiAutomatorUtils.handleInput(EDIT_TEXT_COOKIES_URL, MICROSOFT_ONLINE);
        UiAutomatorUtils.handleButtonClick(BUTTON_GET_COOKIES);

        final String cookiesResult = dismissDialogAndGetText();

        Assert.assertNotNull("GetCookies result should not be null", cookiesResult);
        Assert.assertFalse(
                "GetCookies result should not be empty",
                cookiesResult.isEmpty()
        );
        Assert.assertFalse(
                "GetCookies should be supported",
                cookiesResult.contains("Unsupported contract")
        );
        final int cookiesJsonStart = cookiesResult.indexOf("{");
        Assert.assertTrue(
                "GetCookies result should contain a JSON response",
                cookiesJsonStart >= 0
        );
        final GetCookiesResponse cookiesResponse = ObjectMapper.deserializeJsonStringToObject(
                cookiesResult.substring(cookiesJsonStart),
                GetCookiesResponse.class
        );
        Assert.assertNotNull(
                "GetCookies JSON response should not be null",
                cookiesResponse
        );
        Assert.assertNotNull(
                "GetCookies response should contain a cookie list",
                cookiesResponse.response
        );
        Assert.assertFalse(
                "GetCookies should return at least one cookie",
                cookiesResponse.response.isEmpty()
        );
        for (int i = 0; i < cookiesResponse.response.size(); i++) {
            final CookieItem cookie = cookiesResponse.response.get(i);
            Assert.assertNotNull("GetCookies should not return a null cookie", cookie);
            final String expectedName = i == 0 ? PRT_COOKIE_NAME : PRT_COOKIE_NAME + i;
            Assert.assertEquals(
                    "GetCookies should enumerate cookie names",
                    expectedName,
                    cookie.name
            );
            Assert.assertFalse(
                    "GetCookies should return nonblank cookie data",
                    isBlank(cookie.data)
            );
        }

        // -------- Step 4: GetAllSsoTokens --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the GetAllSsoTokens button visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(BUTTON_GET_ALL_SSO_TOKENS));
        UiAutomatorUtils.handleButtonClick(BUTTON_GET_ALL_SSO_TOKENS);

        // The result is shown in the textbox next to the button (not a dialog)
        final UiObject allSsoTokensResultView = UiAutomatorUtils.obtainUiObjectWithResourceId(TEXT_GET_ALL_SSO_TOKENS);
        Assert.assertTrue(
                "GetAllSsoTokens result text view should be present",
                allSsoTokensResultView.waitForExists(DIALOG_WAIT_TIMEOUT_MS)
        );
        String allSsoTokensResult = "";
        final long waitUntil = System.currentTimeMillis() + DIALOG_WAIT_TIMEOUT_MS;
        while (allSsoTokensResult.isEmpty() && System.currentTimeMillis() < waitUntil) {
            try {
                allSsoTokensResult = allSsoTokensResultView.getText();
            } catch (final UiObjectNotFoundException e) {
                throw new AssertionError("Could not read GetAllSsoTokens result text", e);
            }
            if (allSsoTokensResult.isEmpty()) {
                ThreadUtils.sleepSafely(250, "Waiting for GetAllSsoTokens result text", "Interrupted");
            }
        }

        Assert.assertNotNull("GetAllSsoTokens result should not be null", allSsoTokensResult);
        Assert.assertFalse(
                "GetAllSsoTokens result should not be empty",
                allSsoTokensResult.isEmpty()
        );

        // -------- Step 5: SignOut --------
        // Navigate to the Broker API tab
        brokerHost.brokerApiFragment.launch();

        // Scroll down to make the form fields visible
        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(INPUT_HOME_ACCOUNT_ID));

        UiAutomatorUtils.handleInput(INPUT_HOME_ACCOUNT_ID, homeAccountId);

        new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId(BUTTON_EXECUTE_SIGN_OUT));
        UiAutomatorUtils.handleButtonClick(BUTTON_EXECUTE_SIGN_OUT);

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
        Assert.assertTrue(
                "Dialog box should be present",
                dialogBox.waitForExists(DIALOG_WAIT_TIMEOUT_MS)
        );
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

    private static boolean isBlank(final String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class GetCookiesResponse {
        private List<CookieItem> response;
    }

    private static final class CookieItem {
        private String name;
        private String data;
    }

    @Override
    public UserType getJsonUserType() {
        return UserType.BASIC;
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
