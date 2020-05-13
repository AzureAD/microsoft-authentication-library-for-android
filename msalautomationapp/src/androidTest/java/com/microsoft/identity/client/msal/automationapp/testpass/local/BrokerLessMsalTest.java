package com.microsoft.identity.client.msal.automationapp.testpass.local;

import com.microsoft.identity.client.msal.automationapp.AcquireTokenNetworkAbstractTest;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

public abstract class BrokerLessMsalTest extends AcquireTokenNetworkAbstractTest {

    @Override
    public ITestBroker getBroker() {
        return null;
    }
}
