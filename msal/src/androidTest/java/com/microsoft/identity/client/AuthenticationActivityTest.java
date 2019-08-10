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
package com.microsoft.identity.client;

import android.content.Context;
import android.content.Intent;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.common.internal.net.HttpUrlConnectionFactory;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link AuthenticationActivity}.
 */
@RunWith(AndroidJUnit4.class)
public final class AuthenticationActivityTest {
    private static final int REQUEST_ID = 1234;
    private Context mAppContext;
    private String mRedirectUri;

    private final ActivityTestRule mTestActivityRule = new ActivityTestRule<>(TestActivity.class,
            true, false);

    @Before
    public void setUp() {
        mAppContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mRedirectUri = "msauth-client-id://" + mAppContext.getPackageName();
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
    }

    @Test
    public void testIntentWithNoRequestUri() {
        final Intent intent = new Intent(mAppContext, AuthenticationActivity.class);
        intent.putExtra(Constants.REQUEST_ID, REQUEST_ID);

        mTestActivityRule.launchActivity(TestActivity.createIntent(mAppContext, intent));

        Assert.assertTrue(TestActivity.getResultCode() == Constants.UIResponse.AUTH_CODE_ERROR);
        final Intent resultData = TestActivity.getResultData();
        Assert.assertNotNull(resultData);
        Assert.assertTrue(resultData.getStringExtra(Constants.UIResponse.ERROR_CODE).equals(
                MsalClientException.UNRESOLVABLE_INTENT));
    }

    private String getRequestUri() {
        return mRedirectUri + "?grant_type=code&";
    }
}
