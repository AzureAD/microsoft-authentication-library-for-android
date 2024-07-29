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
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

// If LTW is the active broker, and request is made through Authenticator from Legacy WorkplaceJoin API, nothing should break
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2582294
@SupportedBrokers(brokers = {BrokerLTW.class})
@LTWTests
public class TestCase2582294 extends AbstractMsalBrokerTest {

    @Test
    public void test_2582294() throws Throwable{
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();
        final String homeTenantId = mLabAccount.getHomeTenantId();

        // Shared Steps 2693823: Set up LTW with WPJ cert (non-joined)
        // Install new LTW with broker SDK changes of broker selection logic
        // installed LTW by SupportedBrokers annotation

        // Install BrokerHost app with broker selection logic
        final BrokerHost brokerHost = new BrokerHost();
        brokerHost.install();
        brokerHost.launch();

        // Under Multiple WPJ - Click on "Device Registration" button
        brokerHost.multipleWpjApiFragment.performDeviceRegistration(username, password);

        // Uninstall brokerHost
        brokerHost.uninstall();

        // Install new Authenticator with broker SDK changes of broker selection logic
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
        brokerMicrosoftAuthenticator.install();

        // Install BrokerHost with NO Broker Selection Logic. Navigate to "Single WPJ API" Tab.
        final BrokerHost brokerHostWithoutBrokerSelection = new BrokerHost(BrokerHost.BROKER_HOST_WITHOUT_BROKER_SELECTION_APK);
        brokerHostWithoutBrokerSelection.install();
        brokerHostWithoutBrokerSelection.launch();

        // click on "device id" button
        // You should see a popup with a deviceId
        final String deviceId = brokerHostWithoutBrokerSelection.obtainDeviceId();
        Assert.assertTrue(!deviceId.isEmpty());

        // click on "device state" button
        // You should see the "joined" state
        final String state = brokerHostWithoutBrokerSelection.getDeviceState();
        Assert.assertTrue("Assert that the device state is true", state.contains("true"));

        // click on "install cert" button
        // popup to select certificate type is shown
        // Choose "Vpn and app user cert" and click on ok
        // popup with name of cert is shown. let the default name be there. Upon clicking on ok, a toast message with "Certificate is installed" is shown
        brokerHostWithoutBrokerSelection.enableBrowserAccess(username);

        // click on "get wpj upn" button
        // You should see the upn with which we performed join
        final String upn = brokerHostWithoutBrokerSelection.getAccountUpn();
        Assert.assertEquals(username, upn);

        // click on "get device token"
        // You should see the popup with device token
        final String token = brokerHostWithoutBrokerSelection.getDeviceToken();
        Assert.assertTrue(!token.isEmpty());

        // Click on "wpj leave" button
        // Device should WPJ leave successfully. A popup with leave successful message should be shown
        brokerHostWithoutBrokerSelection.wpjLeave();

        // Enter tenantId in tenantId text box
        // Click on "Get preprovisioning blob" button
        // You should see a popup with the blob
        final String blob = brokerHostWithoutBrokerSelection.getBlob(homeTenantId);
        Assert.assertTrue(!blob.isEmpty());

        // Enter username in username textbox
        // Click on "User based join" button
        // Complete the device registration flow. See a popup message that the flow was successful
        brokerHostWithoutBrokerSelection.performDeviceRegistration(username, password);
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
