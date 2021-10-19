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
package com.microsoft.identity.client.msal.automationapp.testpass.stress;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.stress.rules.StressTestingRule;
import com.microsoft.identity.client.ui.automation.rules.RulesHelper;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;

import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public abstract class AbstractMsalUiStressTest<T, S> extends AbstractMsalUiTest {

    @Rule(order = 10)
    public final StressTestingRule stressTestingRule = new StressTestingRule(
            getOutputFileName(), getNumberOfThreads(), getTimeLimit()
    );


    /**
     * Run the stress tests
     */
    public void run() throws Exception {
        final T prerequisites = this.prepare();

        final ExecutorService executorService = Executors.newFixedThreadPool(getNumberOfThreads());

        final long timeLimit = TimeUnit.MINUTES.toMillis(getTimeLimit());


        for (int i = 0; i < getNumberOfThreads(); i++) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (!stressTestingRule.executionFailed()) {
                        try {
                            S result = execute(prerequisites);

                            boolean passed = isTestPassed(result);

                            stressTestingRule.updatePassRate(passed);

                        } catch (final Exception exception) {
                            stressTestingRule.updatePassRate(false);
                            stressTestingRule.setExecutionException(exception);
                        }
                    }
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(timeLimit, TimeUnit.MILLISECONDS);
        executorService.shutdownNow();
    }


    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_webview;
    }

    @Override
    public LabUserQuery getLabUserQuery() {
        final LabUserQuery query = new LabUserQuery();
        query.azureEnvironment = LabConstants.AzureEnvironment.AZURE_CLOUD;

        return query;
    }

    @Override
    public String getTempUserType() {
        return null;
    }

    @Override
    public RuleChain getPrimaryRules() {
        // Use the time limit as the test timeout rule and add 5 more minutes for cleaning up of the tests
        final Timeout timeout = new Timeout(getTimeLimit() + 5, TimeUnit.MINUTES);

        return RulesHelper
                .getPrimaryRules(null, timeout);
    }

    /**
     * Assert that the test passed
     *
     * @param result the test result
     * @return whether the test passed
     */
    public abstract boolean isTestPassed(S result);


    /**
     * Prepare all the tests for execution
     *
     * @return a result that will be used in the stress test execution
     */
    public abstract T prepare();

    /**
     * Run the stress test
     *
     * @param prerequisites result from the {@link AbstractMsalUiStressTest#prepare()}
     * @return the result of the test
     */
    public abstract S execute(T prerequisites) throws Exception;

    /**
     * Provide the number of threads that will be running
     *
     * @return the number of threads
     */
    public abstract int getNumberOfThreads();


    /**
     * Provide the time limit for the tests in minutes
     *
     * @return the time limit in minutes
     */
    public abstract long getTimeLimit();


    /**
     * Return the output file name
     *
     * @return the name of the output file
     */
    public abstract String getOutputFileName();


}
