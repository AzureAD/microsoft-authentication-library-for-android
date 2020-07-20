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
package com.microsoft.identity.client.msal.automationapp;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.ActivityTestRule;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.ui.automation.IBrokerTest;
import com.microsoft.identity.client.ui.automation.ILabTest;
import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.client.ui.automation.browser.IBrowser;
import com.microsoft.identity.client.ui.automation.testrules.InstallBrokerTestRule;
import com.microsoft.identity.client.ui.automation.testrules.LoadLabUserTestRule;
import com.microsoft.identity.client.ui.automation.testrules.RemoveBrokersBeforeTestRule;
import com.microsoft.identity.client.ui.automation.testrules.ResetAutomaticTimeZoneTestRule;
import com.microsoft.identity.client.ui.automation.testrules.UiAutomatorTestRule;
import com.microsoft.identity.common.internal.util.StringUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.fail;

public abstract class AbstractMsalUiTest implements IMsalTest, IBrokerTest, ILabTest {

    protected Context mContext;
    protected Activity mActivity;
    protected IPublicClientApplication mApplication;

    protected String[] mScopes;
    protected ITestBroker mBroker;
    protected IAccount mAccount;
    protected IBrowser mBrowser;
    protected String mLoginHint;

    @Rule(order = 0)
    public final TestRule uiAutomatorTestRule = new UiAutomatorTestRule();

    @Rule(order = 1)
    public final TestRule resetAutomaticTimeRule = new ResetAutomaticTimeZoneTestRule();

    @Rule(order = 2)
    public final TestRule loadLabUserRule = getLabUserQuery() != null
            ? new LoadLabUserTestRule(getLabUserQuery())
            : new LoadLabUserTestRule(getTempUserType());

    @Rule(order = 3)
    public final TestRule removeBrokersRule = new RemoveBrokersBeforeTestRule();

    @Rule(order = 4)
    public final TestRule installBrokerRule = new InstallBrokerTestRule(getBroker());

    @Rule(order = 5)
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule(MainActivity.class);

    @Before
    public void setup() {
        mScopes = getScopes();
        mBroker = getBroker();
        mBrowser = getBrowser();

        // clear all cookies in the browser
        ((IApp) mBrowser).clear();

        mLoginHint = ((LoadLabUserTestRule) loadLabUserRule).getLabUserUpn();

        mContext = ApplicationProvider.getApplicationContext();
        mActivity = mActivityRule.getActivity();
        setupPCA();
    }

    @After
    public void cleanup() {
        mAccount = null;
    }

    @Override
    public IBrowser getBrowser() {
        return new BrowserChrome();
    }

    private void setupPCA() {
        try {
            mApplication = PublicClientApplication.create(mContext, getConfigFileResourceId());
        } catch (InterruptedException e) {
            fail(e.getMessage());
        } catch (MsalException e) {
            fail(e.getMessage());
        }
    }

    protected IAccount getAccount() {
        return mAccount;
    }

    /**
     * A callback that can be used to perform assertions on completion of an interactive request
     * (success case) test.
     *
     * @param latch the latch associated to this request
     * @return an {@link AuthenticationCallback} object
     */
    protected AuthenticationCallback successfulInteractiveCallback(final CountDownLatch latch) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                mAccount = authenticationResult.getAccount();
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                junit.framework.Assert.fail(exception.getMessage());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                junit.framework.Assert.fail("User cancelled flow");
                latch.countDown();
            }
        };
    }

    /**
     * A callback that can be used to perform assertions on completion of an interactive request
     * (success case) test.
     *
     * @param latch the latch associated to this request
     * @return an {@link AuthenticationCallback} object
     */
    protected AuthenticationCallback successfulClaimsRequestInIdTokenInteractiveCallback(
            @NonNull final CountDownLatch latch,
            @NonNull final String requestedClaim,
            @Nullable final String expectedValue
    ) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                mAccount = authenticationResult.getAccount();
                final Map<String, ?> claims = authenticationResult.getAccount().getClaims();
                Assert.assertTrue(claims.containsKey(requestedClaim));
                if (!TextUtils.isEmpty(expectedValue)) {
                    final Object claimValue = claims.get(requestedClaim);
                    Assert.assertEquals(expectedValue, claimValue.toString());
                }
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                junit.framework.Assert.fail(exception.getMessage());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                junit.framework.Assert.fail("User cancelled flow");
                latch.countDown();
            }
        };
    }

    /**
     * A callback that can be used to perform assertions on completion of an interactive request
     * (cancel case) test.
     *
     * @param latch the latch associated to this request
     * @return an {@link AuthenticationCallback} object
     */
    protected AuthenticationCallback cancelInteractiveCallback(final CountDownLatch latch) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.fail("Unexpected Success!");
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                Assert.fail(exception.getMessage());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                latch.countDown();
            }
        };
    }

    /**
     * A callback that can be used to perform assertions on completion of an silent request
     * (success case) test.
     *
     * @param latch the latch associated to this request
     * @return an {@link AuthenticationCallback} object
     */
    protected SilentAuthenticationCallback successfulSilentCallback(final CountDownLatch latch) {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                Assert.assertFalse(StringUtil.isEmpty(authenticationResult.getAccessToken()));
                mAccount = authenticationResult.getAccount();
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                junit.framework.Assert.fail(exception.getMessage());
                latch.countDown();
            }
        };
    }

    /**
     * A callback that can be used to perform assertions on completion of an interactive request
     * (failure case) test.
     *
     * @param latch the latch associated to this request
     * @return an {@link AuthenticationCallback} object
     */
    protected AuthenticationCallback failureInteractiveCallback(final CountDownLatch latch, final String errorCode) {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                junit.framework.Assert.fail("Unexpected success");
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                Assert.assertEquals(errorCode, exception.getErrorCode());
                latch.countDown();
            }

            @Override
            public void onCancel() {
                junit.framework.Assert.fail("User cancelled flow");
                latch.countDown();
            }
        };
    }

    /**
     * A callback that can be used to perform assertions on completion of an silent request
     * (failure case) test.
     *
     * @param latch the latch associated to this request
     * @return an {@link AuthenticationCallback} object
     */
    protected SilentAuthenticationCallback failureSilentCallback(final CountDownLatch latch, final String errorCode) {
        return new SilentAuthenticationCallback() {
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                junit.framework.Assert.fail("Unexpected success");
                latch.countDown();
            }

            @Override
            public void onError(MsalException exception) {
                Assert.assertSame(errorCode, exception.getErrorCode());
                latch.countDown();
            }
        };
    }
}
