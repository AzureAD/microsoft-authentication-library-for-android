package com.microsoft.identity.client.msal.automationapp;

import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ForceRunRule implements TestRule {
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ForceRun forceRun = description.getAnnotation(ForceRun.class);

                if (forceRun == null) {
                    // if the test didn't have the SupportedBrokers annotation, then we see if the
                    // class had that annotation and we try to honor that
                    forceRun = description.getTestClass().getAnnotation(ForceRun.class);
                }

                Assume.assumeTrue(forceRun != null);

                base.evaluate();
            }
        };
    }
}
