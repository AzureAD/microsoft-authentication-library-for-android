// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.client.e2e.rules;

import com.microsoft.identity.internal.testutils.BuildConfig;
import com.microsoft.identity.internal.testutils.kusto.CaptureKustoTestResultRule;

import org.junit.Ignore;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * Utility file to obtain JUnit test rules that are created via a {@link RuleChain}.
 */
public class NetworkTestsRuleChain {

    private final static String TAG = NetworkTestsRuleChain.class.getSimpleName();

    public static TestRule getRule() {
        System.out.println(TAG + ": Adding Robolectric Logging Rule");
        RuleChain ruleChain = RuleChain.outerRule(new RobolectricLoggingRule());

        System.out.println(TAG + ": Should write test results to CSV: " +
                BuildConfig.SAVE_TEST_RESULTS_TO_CSV);

        if (BuildConfig.SAVE_TEST_RESULTS_TO_CSV) {
            System.out.println(TAG + ": Adding Rule to capture test results for Kusto");
            ruleChain = ruleChain.around(new CaptureKustoTestResultRule());
        }

        System.out.println(TAG + ": Adding Timeout Rule");
        ruleChain = ruleChain.around(new Timeout(30, TimeUnit.SECONDS));

        return ruleChain;
    }

}
