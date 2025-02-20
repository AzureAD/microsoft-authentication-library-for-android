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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.brokerapi;


import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LocalBrokerHostDebugUiTest;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Test;

// Check DCF Option UI (Join Tenant)
// Currently, other broker apps do not have a way to call AcquireToken with the "is_remote_login_allowed=true" parameter
// BrokerHost got a new button to facilitate this particular flow.
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2110359
@SupportedBrokers(brokers = BrokerHost.class)
@LocalBrokerHostDebugUiTest
public class TestCase2110359 extends AbstractMsalBrokerTest{
    // tenant id where lab api and key vault api is registered
    private final static String LAB_API_TENANT_ID = "72f988bf-86f1-41af-91ab-2d7cd011db47";

    @Test
    public void test_2110359() {
        checkForDcfOption(null);
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.CLOUD)
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
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }

    /**
     * Check if the Device Code Flow option shows up in sign in flow.
     *
     * @param tenantId tenant ID to use in Join Tenant
     */
    public void checkForDcfOption(@Nullable final String tenantId) {
        final String tenantIdToUse;

        // If no tenant ID is specified, default to microsoft tenant
        if (tenantId == null) {
            tenantIdToUse = LAB_API_TENANT_ID;
        } else {
            tenantIdToUse = tenantId;
        }
        BrokerHost brokerHost = (BrokerHost) mBroker;
        brokerHost.launch();
        brokerHost.clickJoinTenant(tenantIdToUse);

        // Apparently, there are two UI objects with exact text "Sign-in options", one is a button the other is a view
        // Have to specify the search to button class
        final UiDevice device =
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        final UiObject optionsObject = device.findObject(new UiSelector()
                .text("Sign-in options").className("android.widget.Button"));

        try {
            optionsObject.click();
        } catch (UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
        UiAutomatorUtils.handleButtonClickForObjectWithText("Sign in from another device");

        // Doesn't look like the page with the device code is readable to the UI automation,
        // this is a sufficient stopping point
    }

}
