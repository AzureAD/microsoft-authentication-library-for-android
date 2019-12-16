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
package com.microsoft.identity.client.e2e.tests;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.internal.testutils.TestUtils;

import org.junit.Before;
import org.robolectric.annotation.Config;

import java.io.File;

import static com.microsoft.identity.client.e2e.utils.RoboTestUtils.flushScheduler;
import static org.junit.Assert.fail;

@Config(sdk = {
        Build.VERSION_CODES.JELLY_BEAN,
        Build.VERSION_CODES.JELLY_BEAN_MR1,
        Build.VERSION_CODES.JELLY_BEAN_MR2,
        Build.VERSION_CODES.KITKAT,
        //Build.VERSION_CODES.KITKAT_WATCH, // robolectric does not support API level 20
        Build.VERSION_CODES.LOLLIPOP,
        Build.VERSION_CODES.LOLLIPOP_MR1,
        Build.VERSION_CODES.M,
        Build.VERSION_CODES.N,
        Build.VERSION_CODES.N_MR1,
        Build.VERSION_CODES.O,
        Build.VERSION_CODES.O_MR1,
        Build.VERSION_CODES.P
})
public abstract class PublicClientApplicationAbstractTest implements IPublicClientApplicationTest {

    protected final String SHARED_PREFERENCES_NAME = "com.microsoft.identity.client.account_credential_cache";

    protected Context mContext;
    protected Activity mActivity;
    protected IPublicClientApplication mApplication;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActivity = TestUtils.getMockActivity(mContext);
        setupPCA();
    }

    private void setupPCA() {
        final File configFile = new File(getConfigFilePath());

        PublicClientApplication.create(mContext, configFile, new PublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                mApplication = application;
            }

            @Override
            public void onError(MsalException exception) {
                fail(exception.getMessage());
            }
        });

        flushScheduler();
    }

}
