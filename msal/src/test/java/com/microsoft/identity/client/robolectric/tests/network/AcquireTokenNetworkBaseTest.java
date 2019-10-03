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
package com.microsoft.identity.client.robolectric.tests.network;

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.robolectric.utils.RoboTestUtils;
import com.microsoft.identity.internal.testutils.labutils.TestConfigurationHelper;
import com.microsoft.identity.internal.testutils.labutils.TestConfigurationQuery;

import java.io.File;

public abstract class AcquireTokenNetworkBaseTest {

    private static final String AAD_CONFIG_FILE_PATH = "src/test/res/raw/aad_test_config.json";
    private static final String B2C_CONFIG_FILE_PATH = "src/test/res/raw/b2c_test_config.json";

    private static final String AAD_AUTHORITY_TYPE_STRING = "AAD";
    private static final String B2C_AUTHORITY_TYPE_STRING = "B2C";

    /**
     * @param publicClientApplication instance of Public Client Application
     * @param activity                activity required for acquire token parameters
     * @param username                username needed to attach to token request for ROPC
     * @throws InterruptedException
     */
    abstract void makeAcquireTokenCall(final IPublicClientApplication publicClientApplication,
                                       final Activity activity,
                                       final String username) throws InterruptedException;


    void instantiatePCAthenAcquireToken(String authorityType) {
        final Context context = ApplicationProvider.getApplicationContext();
        final Activity testActivity = RoboTestUtils.getMockActivity(context);

        final String configFilePath = getConfigFilePath(authorityType);
        final File configFile = new File(configFilePath);

        final TestConfigurationQuery query = getTestConfigurationQuery(authorityType);
        final String username = TestConfigurationHelper.getUpnForTest(query);

        PublicClientApplication.create(context, configFile, new PublicClientApplication.ApplicationCreatedListener() {
            @Override
            public void onCreated(IPublicClientApplication application) {
                try {
                    makeAcquireTokenCall(application, testActivity, username);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(MsalException exception) {
                exception.printStackTrace();
            }
        });
    }

    private String getConfigFilePath(String authorityType) {
        return authorityType == B2C_AUTHORITY_TYPE_STRING
                ? B2C_CONFIG_FILE_PATH
                : AAD_CONFIG_FILE_PATH;
    }

    /**
     * @param authorityType can be either "AAD" or "B2C"
     * @return test configuration query to be used for pulling test accounts from Lab Api
     */
    private TestConfigurationQuery getTestConfigurationQuery(String authorityType) {
        return (authorityType == B2C_AUTHORITY_TYPE_STRING) ? getQueryForB2C() : getQueryForAAD();
    }

    private TestConfigurationQuery getQueryForAAD() {
        final TestConfigurationQuery query = new TestConfigurationQuery();
        query.userType = "Member";
        query.isFederated = false;
        query.federationProvider = "ADFSv4";
        return query;
    }

    private TestConfigurationQuery getQueryForB2C() {
        final TestConfigurationQuery query = new TestConfigurationQuery();
        query.b2cProvider = "Local";
        return query;
    }
}
