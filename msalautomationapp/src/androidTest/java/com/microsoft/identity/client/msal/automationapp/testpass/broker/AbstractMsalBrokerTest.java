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

import androidx.annotation.NonNull;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.BrokerTestHelper;
import com.microsoft.identity.client.ui.automation.IBrokerTest;
import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.device.settings.ISettings;
import com.microsoft.identity.client.ui.automation.rules.RulesHelper;

import org.junit.rules.RuleChain;

/**
 * An MSAL test model that would leverage an {@link ITestBroker} installed on the device.
 */
public abstract class AbstractMsalBrokerTest extends AbstractMsalUiTest implements IBrokerTest {

    protected ITestBroker mBroker = getBroker();

    @NonNull
    @Override
    public ITestBroker getBroker() {
        // only initialize once....so calling getBroker from anywhere returns the same instance
        if (mBroker == null) {
            final SupportedBrokers supportedBrokersAnnotation = getClass().getAnnotation(SupportedBrokers.class);
            mBroker = BrokerTestHelper.createBrokerFromFlavor(supportedBrokersAnnotation);
        }
        return mBroker;
    }

    @Override
    public RuleChain getPrimaryRules() {
        return RulesHelper.getPrimaryRules(getBroker());
    }

    protected ISettings getSettingsScreen() {
        return TestContext.getTestContext().getTestDevice().getSettings();
    }
}
