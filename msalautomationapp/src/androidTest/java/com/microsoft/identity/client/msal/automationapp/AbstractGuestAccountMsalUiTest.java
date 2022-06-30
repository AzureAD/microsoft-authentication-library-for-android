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
package com.microsoft.identity.client.msal.automationapp;

import android.app.Activity;

import androidx.test.rule.ActivityTestRule;

import com.microsoft.identity.client.ui.automation.ILabTest;
import com.microsoft.identity.client.ui.automation.IRuleBasedTest;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.rules.RulesHelper;
import com.microsoft.identity.client.ui.automation.rules.UncaughtExceptionHandlerRule;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.labapi.utilities.BuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.client.LabClient;
import com.microsoft.identity.labapi.utilities.client.LabGuestAccount;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.RuleChain;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGuestAccountMsalUiTest implements IMsalTest, ILabTest, IRuleBasedTest {
    @Rule(order = 0)
    public RuleChain primaryRules = getPrimaryRules();
    @Rule(order = 1)
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule(MainActivity.class);
    @Rule(order = 2)
    public UncaughtExceptionHandlerRule uncaughtExceptionHandlerRule = new UncaughtExceptionHandlerRule();
    protected Activity mActivity;
    protected IBrowser mBrowser;
    protected LabGuestAccount mGuestUser;
    protected LabClient mLabClient;

    @Before
    public void setup() throws LabApiException {
        mActivity = mActivityRule.getActivity();
        mBrowser = new BrowserChrome();

        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                BuildConfig.LAB_CLIENT_SECRET
        );
        mLabClient = new LabClient(authenticationClient);

        mGuestUser = mLabClient.loadGuestAccountFromLab(getLabQuery());
    }

    @After
    public void cleanup() {
        mBrowser.forceStop();
        mBrowser.clear();
    }

    /**
     * Calls "https://graph.microsoft.com/v1.0/me" endpoint with given accessToken
     * returns the response back on success
     * @param accessToken token to call the graph api
     * @return response of graph call
     */
    protected JSONObject getProfileObjectFromMSGraph(final String accessToken) {
        try {
            final URL profileApiUrl = new URL("https://graph.microsoft.com/v1.0/me");
            final Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Authorization", "Bearer " + accessToken);
            final HttpResponse response = UrlConnectionHttpClient.getDefaultInstance().get(profileApiUrl, requestHeaders, null);
            Assert.assertTrue(response.getStatusCode() == HttpURLConnection.HTTP_OK);
            return new JSONObject(response.getBody());
        } catch (final Exception exception) {
            throw new AssertionError(exception);
        }
    }

    @Override
    public RuleChain getPrimaryRules() {
        return RulesHelper.getPrimaryRules(null);
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public IBrowser getBrowser() {
        return mBrowser;
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}
