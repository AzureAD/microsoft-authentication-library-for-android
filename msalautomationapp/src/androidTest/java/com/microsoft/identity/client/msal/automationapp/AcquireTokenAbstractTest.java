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

import com.microsoft.identity.client.msal.automationapp.utils.CommonUtils;
import com.microsoft.identity.internal.testutils.TestUtils;

import org.junit.After;
import org.junit.Before;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.COMPANY_PORTAL_APP_SIGNATURE;

public abstract class AcquireTokenAbstractTest extends PublicClientApplicationAbstractTest implements IAcquireTokenTest {

    private static final String TAG = AcquireTokenAbstractTest.class.getSimpleName();

    protected String[] mScopes;

    @Before
    public void setup() {
        // remove existing authenticator and company portal apps - the test app is removed
        // by the Android Test Orchestrator. See build.gradle
        CommonUtils.removeApp(AZURE_AUTHENTICATOR_APP_PACKAGE_NAME);
        CommonUtils.removeApp(COMPANY_PORTAL_APP_SIGNATURE);

        mScopes = getScopes();
        super.setup();
    }

    @After
    public void cleanup() {
        super.cleanup();
        AcquireTokenTestHelper.setAccount(null);
        // remove everything from cache after test ends
        TestUtils.clearCache(SHARED_PREFERENCES_NAME);
        //CommonUtils.clearApp(mContext.getPackageName());
    }
}
