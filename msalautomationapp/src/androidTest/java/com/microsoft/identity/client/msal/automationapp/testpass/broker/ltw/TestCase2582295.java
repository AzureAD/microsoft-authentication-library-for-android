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
package com.microsoft.identity.client.msal.automationapp.testpass.broker.ltw;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

// Test Case 2582295: If LTW is the active broker, and request is made through CP from Legacy WorkplaceJoin API, nothing should break
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2582295
@LTWTests
@SupportedBrokers(brokers = {BrokerLTW.class})
public class TestCase2582295 extends AbstractMsalBrokerTest {

    @Test
    public void test_2582295() throws Throwable{
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();
        final String homeTenantId = mLabAccount.getHomeTenantId();

        // Install new CP app with broker SDK changes of broker selection logic
        final BrokerCompanyPortal brokerCompanyPortal = new BrokerCompanyPortal();
        brokerCompanyPortal.install();

        // Install old BrokerHost app with no broker SDK changes of broker selection logic
        final BrokerHost brokerHost = new BrokerHost(BrokerHost.BROKER_HOST_WITHOUT_BROKER_SELECTION_APK);
        brokerHost.install();
        brokerHost.launch();

        // Enter tenantId in tenantId text box
        // Click on "Get preprovisioning blob" button
        // You should see a popup with the blob
        final String blob = brokerHost.getBlob(homeTenantId);
        Assert.assertTrue(!blob.isEmpty());

        // Enter username in username textbox
        // Click on "User based join" button
        // Complete the device registration flow. See a popup message that the flow was successful
        brokerHost.performDeviceRegistration(username, password);

        // click on "device id" button
        // You should see a popup with a deviceId
        final String deviceId = brokerHost.obtainDeviceId();
        Assert.assertTrue(!deviceId.isEmpty());

        // click on "device state" button
        // You should see the "joined" state
        final String state = brokerHost.getDeviceState();
        Assert.assertTrue("Assert that the device state is true", state.contains("true"));

        // click on "install cert" button
        // popup to select certificate type is shown
        // Choose "Vpn and app user cert" and click on ok
        // popup with name of cert is shown. let the default name be there. Upon clicking on ok, a toast message with "Certificate is installed" is shown
        brokerHost.enableBrowserAccess();

        // click on "get wpj upn" button
        // You should see the upn with which we performed join
        final String upn = brokerHost.getAccountUpn();
        Assert.assertEquals(username, upn);

        // click on "get device token"
        // You should see the popup with device token
        final String token = brokerHost.getDeviceToken();
        Assert.assertTrue(!token.isEmpty());

        // Click on "wpj leave" button
        // Device should WPJ leave successfully. A popup with leave successful message should be shown
        brokerHost.wpjLeave();
    }

    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.BASIC;
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
