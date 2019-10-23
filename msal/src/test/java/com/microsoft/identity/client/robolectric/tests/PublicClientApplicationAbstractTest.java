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
package com.microsoft.identity.client.robolectric.tests;

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.robolectric.utils.RoboTestUtils;

import org.junit.Before;

import java.io.File;

import static org.junit.Assert.fail;

public abstract class PublicClientApplicationAbstractTest {

    protected static final String MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH = "src/test/res/raw/aad_test_config.json";
    protected static final String SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH = "src/test/res/raw/single_account_aad_test_config.json";

    protected static final String SINGLE_ACCOUNT_APPLICATION_MODE = "single";
    protected static final String MULTIPLE_ACCOUNT_APPLICATION_MODE = "multiple";

    protected Context mContext;
    protected Activity mActivity;
    protected IPublicClientApplication mApplication;

    protected String mApplicationMode = SINGLE_ACCOUNT_APPLICATION_MODE; //Default

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mActivity = RoboTestUtils.getMockActivity(mContext);
        setupPCA();
    }

    void setupPCA() {
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

        RoboTestUtils.flushScheduler();
    }

    // can be overridden or more cases can be added here
    protected String getConfigFilePath() {
        if (mApplicationMode.equals(MULTIPLE_ACCOUNT_APPLICATION_MODE)) {
            return MULTIPLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
        } else {
            return SINGLE_ACCOUNT_MODE_AAD_CONFIG_FILE_PATH;
        }
    }

}
