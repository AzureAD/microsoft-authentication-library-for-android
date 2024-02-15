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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.dcf;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;

import org.junit.Test;

// Brokered Auth verify "Sign In from other device" option and remote login url.
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2828864
@RetryOnFailure(retryCount = 2)
public class TestCase2828864 extends AbstractSignInFromOtherDeviceTest {

    public TestCase2828864() {
        super(AzureEnvironment.AZURE_CLOUD);
    }

    @Test
    public void test_2828864() throws Throwable {
        this.testSignInFromOtherDevice();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

    @Override
    protected String getExpectedDeviceCodeUrl() {
        return "https://microsoft.com/devicelogin";
    }
}
