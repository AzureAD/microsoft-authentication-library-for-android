package com.microsoft.identity.client.msal.automationapp.broker;

import com.microsoft.identity.client.msal.automationapp.AcquireTokenNetworkTest;
import com.microsoft.identity.client.msal.automationapp.utils.PlayStoreUtils;

import org.junit.Before;
import org.junit.runners.Parameterized;

public abstract class BrokerMsalTest extends AcquireTokenNetworkTest {

    @Parameterized.Parameter(1)
    public ITestBroker mBroker;


    @Before
    public void setup() {
        super.setup();
        PlayStoreUtils.installApp(mBroker.brokerAppName());
    }



}
