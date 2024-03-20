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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.nestedAppAuth;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

// Nested app's fresh AT interactive succeeds but silent request fails
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2688462
@SupportedBrokers(brokers = BrokerHost.class)
@LocalBrokerHostDebugUiTest
@RunWith(Parameterized.class)
public class TestCase2688462 extends AbstractMsalBrokerTest {
    private final UserType mUserType;

    public TestCase2688462(@NonNull UserType userType) {
        mUserType = userType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<UserType> userType() {
        return Arrays.asList(
                UserType.MSA,
                UserType.CLOUD
        );
    }

    @Test
    public void test_2688462() {
        NestedAppHelper nestedAppHelper = new NestedAppHelper(mActivity, mLabAccount);
        // perform AT interactive request for nested app
        nestedAppHelper.performInteractiveATForNestedApp(false);
        // verify getAccounts gives 0 records
        nestedAppHelper.getAccountRecordAfterNestedAppAT();
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(mUserType)
                .build();
    }

    @Override
    public TempUserType getTempUserType() {
        return null;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}
