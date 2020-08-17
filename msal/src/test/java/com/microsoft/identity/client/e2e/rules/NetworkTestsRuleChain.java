package com.microsoft.identity.client.e2e.rules;

import com.microsoft.identity.internal.testutils.BuildConfig;
import com.microsoft.identity.internal.testutils.kusto.CaptureKustoTestResultRule;

import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

public class NetworkTestsRuleChain {

    private final static String TAG = NetworkTestsRuleChain.class.getSimpleName();

    public static TestRule getRule() {
        System.out.println(TAG + ": Adding Robolectric Logging Rule");
        RuleChain ruleChain = RuleChain.outerRule(new RobolectricLoggingRule());

        if (BuildConfig.UPLOAD_TEST_RESULTS_TO_KUSTO) {
            System.out.println(TAG + ": Adding Rule to capture test results for Kusto");
            ruleChain = ruleChain.around(new CaptureKustoTestResultRule());
        }

        return ruleChain;
    }

}
