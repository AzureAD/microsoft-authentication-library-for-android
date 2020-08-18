package com.microsoft.identity.client.e2e.rules;

import android.util.Log;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.shadows.ShadowLog;

/**
 * A JUnit rule to enable logging during a Robolectric Test.
 */
public class RobolectricLoggingRule implements TestRule {

    private final static String TAG = RobolectricLoggingRule.class.getSimpleName();

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ShadowLog.stream = System.out;
                Log.i(TAG, "Enabled logging in sandbox");
                base.evaluate();
            }
        };
    }
}
