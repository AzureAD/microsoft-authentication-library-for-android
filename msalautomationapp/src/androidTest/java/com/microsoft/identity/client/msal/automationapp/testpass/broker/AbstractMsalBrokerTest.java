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
package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.BuildConfig;
import com.microsoft.identity.client.ui.automation.IBrokerTest;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.rules.BrokerSupportRule;
import com.microsoft.identity.client.ui.automation.rules.DeviceEnrollmentFailureRecoveryRule;
import com.microsoft.identity.client.ui.automation.rules.InstallBrokerTestRule;
import com.microsoft.identity.client.ui.automation.rules.PowerLiftIncidentRule;

import org.junit.Rule;
import org.junit.rules.TestRule;

/**
 * An MSAL test model that would leverage an {@link ITestBroker} installed on the device.
 */
public abstract class AbstractMsalBrokerTest extends AbstractMsalUiTest implements IBrokerTest {

    protected ITestBroker mBroker = getBroker();

    @Rule(order = 5)
    public final TestRule brokerSupportRule = new BrokerSupportRule(mBroker);

    @Rule(order = 6)
    public final TestRule installBrokerRule = new InstallBrokerTestRule(mBroker);

    @Rule(order = 7)
    public final TestRule powerLiftIncidentRule = new PowerLiftIncidentRule(mBroker);

    @Rule(order = 8)
    public final TestRule deviceEnrollmentFailureRecoveryRule = new DeviceEnrollmentFailureRecoveryRule();

    @Override
    public ITestBroker getBroker() {
        switch (BuildConfig.FLAVOR_broker) {
            case "brokerHost":
                return new BrokerHost();
            case "authenticator":
                return new BrokerMicrosoftAuthenticator();
            case "companyPortal":
                return new BrokerCompanyPortal();
            default:
                throw new UnsupportedOperationException("Unsupported broker :(");
        }
    }
}
