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

import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Test;

// Invoke each API from non-allowed apps. the request should be blocked.
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/1600567
@SupportedBrokers(brokers = {BrokerMicrosoftAuthenticator.class})
public class TestCase1600567 extends AbstractMsalBrokerTest {
    @Test
    public void test_1600567() throws Throwable {
        final BrokerHost brokerHost = new BrokerHost();
        brokerHost.install();
        brokerHost.launch();

        // verify getAccounts call gives calling app not verified
        UiAutomatorUtils.obtainChildInScrollable("Get Accounts");
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonGetAccounts");
        brokerHost.confirmCallingAppNotVerified();

        // verify removeAccount call gives calling app not verified
        final UiObject usernameTxt = UiAutomatorUtils.obtainChildInScrollable("someone@contoso.com");
        usernameTxt.setText("test@microsoft.com");
        final UiObject removeAccount = UiAutomatorUtils.obtainUiObjectWithResourceId(
                "com.microsoft.identity.testuserapp:id/buttonRemoveAccount"
        );
        removeAccount.click();
        brokerHost.confirmCallingAppNotVerified();

        // verify update BRT call gives calling app not verified
        // fill BRT
        final UiObject brokerRTTxt = UiAutomatorUtils.obtainChildInScrollable("Broker RT");
        brokerRTTxt.setText("5e0c0ce6-0f40-4738-b2d4-3d83a5a2b555");
        // fill home authority
        final UiObject homeAuthorityTxt = UiAutomatorUtils.obtainChildInScrollable("Home Authority");
        homeAuthorityTxt.setText("https://login.microsoftonline.com/common");
        // click on update BRT
        UiAutomatorUtils.handleButtonClick("com.microsoft.identity.testuserapp:id/buttonUpdateBRT");
        brokerHost.confirmCallingAppNotVerified();

        // verify setFlights call gives calling app not verified
        brokerHost.setFlights("{test : true}");
        brokerHost.confirmCallingAppNotVerified();

        // verify getFlights call gives calling app not verified
        final UiObject getFlightsBtn = UiAutomatorUtils.obtainChildInScrollable("Get Flights");
        getFlightsBtn.click();
        brokerHost.confirmCallingAppNotVerified();
    }


    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
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
}

