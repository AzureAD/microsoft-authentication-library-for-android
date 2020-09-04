package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.ui.automation.IBrokerTest;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
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
    public final TestRule installBrokerRule = new InstallBrokerTestRule(mBroker);

    @Rule(order = 6)
    public final TestRule powerLiftIncidentRule = new PowerLiftIncidentRule(mBroker);

    @Rule(order = 7)
    public final TestRule deviceEnrollmentFailureRecoveryRule = new DeviceEnrollmentFailureRecoveryRule();
}
