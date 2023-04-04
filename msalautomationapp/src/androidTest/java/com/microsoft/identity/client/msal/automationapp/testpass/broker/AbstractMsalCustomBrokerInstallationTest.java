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
import com.microsoft.identity.client.ui.automation.ICustomBrokerInstallationTest;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;

/**
 * Msal UI Testing class that allows devs to easily install old/legacy and updated versions of brokers
 * quickly to test complex update scenarios.
 */
public abstract class AbstractMsalCustomBrokerInstallationTest extends AbstractMsalUiTest implements ICustomBrokerInstallationTest {

    // Fields to access installed brokers
    public BrokerHost mBrokerHost;
    public BrokerMicrosoftAuthenticator mAuthenticator;
    public BrokerCompanyPortal mCompanyPortal;
    public BrokerLTW mLtw;

    @Override
    public void installOldBrokerHost() {
        mBrokerHost = new BrokerHost(BrokerHost.OLD_BROKER_HOST_APK,
                BrokerHost.BROKER_HOST_APK);
        mBrokerHost.install();
    }

    @Override
    public void installBrokerHost() {
        mBrokerHost = new BrokerHost();
        mBrokerHost.install();
    }

    @Override
    public void installOldAuthenticator() {
        mAuthenticator = new BrokerMicrosoftAuthenticator(BrokerMicrosoftAuthenticator.OLD_AUTHENTICATOR_APK,
                BrokerMicrosoftAuthenticator.AUTHENTICATOR_APK);
        mAuthenticator.install();
    }

    @Override
    public void installAuthenticator() {
        mAuthenticator = new BrokerMicrosoftAuthenticator();
        mAuthenticator.install();
    }

    @Override
    public void installOldCompanyPortal() {
        mCompanyPortal = new BrokerCompanyPortal(BrokerCompanyPortal.OLD_COMPANY_PORTAL_APK,
                BrokerCompanyPortal.COMPANY_PORTAL_APK);
        mCompanyPortal.install();
    }

    @Override
    public void installCompanyPortal() {
        mCompanyPortal = new BrokerCompanyPortal();
        mCompanyPortal.install();
    }

    @Override
    public void installOldLtw() {
        mLtw = new BrokerLTW(BrokerLTW.OLD_BROKER_LTW_APK,
                BrokerLTW.BROKER_LTW_APK);
        mLtw.install();
    }

    @Override
    public void installLtw() {
        mLtw = new BrokerLTW();
        mLtw.install();
    }
}
