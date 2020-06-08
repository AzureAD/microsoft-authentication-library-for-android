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

import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.browser.BrowserChrome;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Before;

public abstract class AbstractAcquireTokenNetworkTest extends AbstractAcquireTokenTest implements IAcquireTokenNetworkTest {

    protected String mLoginHint;

    public static final int TEMP_USER_WAIT_TIME = 15000;

    @Before
    public void setup() {
        final LabUserQuery query = getLabUserQuery();
        final String tempUserType = getTempUserType();

        if (query != null) {
            mLoginHint = LabUserHelper.loadUserForTest(query);
        } else if (tempUserType != null) {
            mLoginHint = LabUserHelper.loadTempUser(tempUserType);
            try {
                // temp user takes some time to actually being created even though it may be
                // returned by the LAB API. Adding a wait here before we proceed with the test.
                Thread.sleep(TEMP_USER_WAIT_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalArgumentException("Both Lab User query and temp user type were null.");
        }

        super.setup();
    }

    @Override
    public IApp getBrowser() {
        return new BrowserChrome();
    }

}
