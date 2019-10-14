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
package com.microsoft.identity.client.robolectric.tests.mocked;

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.robolectric.utils.RoboTestUtils;

import java.io.File;

public abstract class AcquireTokenMockBaseTest {

    private static final String AAD_CONFIG_FILE_PATH = "src/test/res/raw/aad_test_config.json";

    abstract void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                       final Activity activity) throws InterruptedException;


    void instantiatePCAthenAcquireToken() {
        final Context context = ApplicationProvider.getApplicationContext();
        final Activity testActivity = RoboTestUtils.getMockActivity(context);

        final File configFile = new File(AAD_CONFIG_FILE_PATH);

        final IPublicClientApplication[] applications = new IPublicClientApplication[1];

        PublicClientApplication.create(context, configFile, new PublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                applications[0] = application;
            }

            @Override
            public void onError(MsalException exception) {
                exception.printStackTrace();
            }
        });

        RoboTestUtils.flushScheduler();

        // TODO: This is a temporary change that is needed as create() is now using command.
        //       Will need a proper refactor at some point.
        try {
            makeAcquireTokenCall(applications[0], testActivity);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
