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
package com.microsoft.identity.client.msal.automationapp.testpass.crosscloud;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

// Acquiring token for cross cloud guest account (Msal Only)
// https://identitydivision.visualstudio.com/DefaultCollection/IDDP/_workitems/edit/1420484
public class TestCase1420484 extends AbstractMsalUiTest {

    @Before
    public void setup() {
        mActivity = mActivityRule.getActivity();
        mBrowser = new BrowserChrome();
    }

    @After
    public void cleanup() {
        mBrowser.clear();
    }

    /**
     * Tests Acquiring token for Cross cloud Guest account without broker.
     */
    @Test
    public void test_1420484() throws Throwable {
        CrossCloudGuestAccountTests.testAcquireToken(
                mActivity,
                mBrowser,
                LabConstants.GuestHomeAzureEnvironment.AZURE_US_GOV,
                null);

        mBrowser.clear();

        CrossCloudGuestAccountTests.testAcquireToken(
                mActivity,
                mBrowser,
                LabConstants.GuestHomeAzureEnvironment.AZURE_CHINA_CLOUD,
                null);
    }

    @Override
    public String[] getScopes() {
        return null;
    }

    @Override
    public int getConfigFileResourceId() {
        return 0;
    }

    @Override
    public String getAuthority() {
        return null;
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        return null;
    }

    @Override
    public String getTempUserType() {
        return null;
    }
}
