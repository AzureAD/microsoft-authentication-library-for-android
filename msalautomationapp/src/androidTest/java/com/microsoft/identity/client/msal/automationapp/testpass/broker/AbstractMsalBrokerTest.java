package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.ui.automation.IBrokerTest;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.rules.InstallBrokerTestRule;

import org.junit.Rule;
import org.junit.rules.TestRule;

public abstract class AbstractMsalBrokerTest extends AbstractMsalUiTest implements IBrokerTest {

    protected ITestBroker mBroker = getBroker();

    @Rule
    public final TestRule installBrokerRule = new InstallBrokerTestRule(mBroker);
}
