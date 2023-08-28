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

import android.text.TextUtils;

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

import java.util.List;
import java.util.Map;

// Test Case 2582296: If LTW is the active broker, and request is made through Authenticator from Multiple WorkplaceJoin API from a legacy broker test app, nothing should break.
//https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2582296
@LTWTests
@SupportedBrokers(brokers = {BrokerLTW.class})
public class TestCase2582296 extends AbstractMsalBrokerTest {
    @Test
    public void test_2582296() {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();
        final String tenantId = mLabAccount.getHomeTenantId();

        // Install new Authenticator with broker SDK changes of broker selection logic
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
        brokerMicrosoftAuthenticator.install();

        // Install old BrokerHost app with no broker SDK changes of broker selection logic
        final BrokerHost brokerHost = new BrokerHost(BrokerHost.BROKER_HOST_WITHOUT_BROKER_SELECTION_APK);
        brokerHost.install();
        brokerHost.launch();

        // Switch to Multiple WPJ API from hamburger menu on the top left
        // Enter username retrieved
        // Click on "Device Registration" button
        brokerHost.multipleWpjApiFragment.performDeviceRegistration(username, password);

        // Enter tenantId and Click on "Get Blob" button
        final String blob = brokerHost.multipleWpjApiFragment.getBlob(tenantId);
        Assert.assertTrue(!TextUtils.isEmpty(blob));

        // Click on "Get all records" button
        // A popup with all the registered entries is shown.
        final List<Map<String, String>> records = brokerHost.multipleWpjApiFragment.getAllRecords();
        Assert.assertEquals(1, records.size());
        final Map<String, String> record = records.get(0);
        Assert.assertEquals(username, record.get("Upn"));
        Assert.assertEquals(tenantId, record.get("TenantId"));

        // Click on "Get record by Upn" button
        final Map<String, String> recordByUpn = brokerHost.multipleWpjApiFragment.getRecordByUpn(username);
        Assert.assertEquals(record, recordByUpn);

        // Click on "Get State" button
        final String state = brokerHost.multipleWpjApiFragment.getDeviceState(username);
        Assert.assertTrue(state.contains("DEVICE_VALID"));

        // Click on "Install certificate" button
        brokerHost.multipleWpjApiFragment.installCertificate(username);

        // Click on "Get device token" button
        final String token = brokerHost.multipleWpjApiFragment.getDeviceToken(username);
        Assert.assertTrue(!TextUtils.isEmpty(token.replace("Device token:", "")));

        // Click on "Unregister" button
        brokerHost.multipleWpjApiFragment.unregister(username);
        final List<Map<String, String>> allRecords = brokerHost.multipleWpjApiFragment.getAllRecords();
        Assert.assertEquals(0, allRecords.size());
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
