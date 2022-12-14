package com.microsoft.identity.client.msal.automationapp.testpass.broker.cba;

import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.ICbaTest;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.rules.RulesHelper;
import com.microsoft.identity.client.ui.automation.rules.UserCertificateRemovalRule;

import org.junit.rules.RuleChain;

public abstract class AbstractMsalBrokerCbaTest extends AbstractMsalBrokerTest implements ICbaTest {

    private final static String TAG = AbstractMsalBrokerCbaTest.class.getSimpleName();

    @Override
    public RuleChain getPrimaryRules() {
        RuleChain ruleChain = RulesHelper.getPrimaryRules(getBroker());
        Logger.i(TAG, "Adding UserCertificateRemovalRule");
        ruleChain = ruleChain.around(new UserCertificateRemovalRule(getSettingsScreen()));
        return ruleChain;
    }
}
