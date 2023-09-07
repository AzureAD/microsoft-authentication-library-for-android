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
package com.microsoft.identity.client.msal.automationapp.testpass.broker;

import androidx.test.rule.ActivityTestRule;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.MainActivity;
import com.microsoft.identity.client.msal.automationapp.MsalLoggingRule;
import com.microsoft.identity.client.ui.automation.ICustomBrokerInstallationTest;

import org.junit.Rule;
import org.junit.rules.RuleChain;

/**
 * Msal UI Testing class that allows devs to easily install old/legacy and updated versions of brokers
 * quickly to test complex update scenarios.
 */
public abstract class AbstractMsalCustomBrokerInstallationTest extends AbstractMsalUiTest implements ICustomBrokerInstallationTest {

    @Rule(order = 0)
    public RuleChain primaryRules = getPrimaryRules();

    @Rule(order = 1)
    public ActivityTestRule<MainActivity> mActivityRule =
            new ActivityTestRule(MainActivity.class);

    @Rule(order = 2)
    public MsalLoggingRule msalLoggingRule = new MsalLoggingRule();

}
