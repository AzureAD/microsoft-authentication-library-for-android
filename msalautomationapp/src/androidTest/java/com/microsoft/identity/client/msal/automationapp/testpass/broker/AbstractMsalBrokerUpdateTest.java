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
import androidx.annotation.Nullable;

import com.microsoft.identity.client.msal.automationapp.AbstractMsalUiTest;
import com.microsoft.identity.client.msal.automationapp.BrokerTestHelper;
import com.microsoft.identity.client.msal.automationapp.BuildConfig;
import com.microsoft.identity.client.ui.automation.IBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.rules.RulesHelper;

import org.junit.rules.RuleChain;

import java.util.Arrays;
import java.util.List;

/**
 * An MSAL test model that would leverage an {@link ITestBroker} installed on the device.
 * This test model should be used only when update test scenarios need to be tested
 */
public abstract class AbstractMsalBrokerUpdateTest extends AbstractMsalUiTest implements IBrokerTest {

    protected ITestBroker mBroker = getBroker();

    @NonNull
    @Override
    public ITestBroker getBroker() {
        // only initialize once....so calling getBroker from anywhere returns the same instance
        if (mBroker == null) {
            final SupportedBrokers supportedBrokersAnnotation = getClass().getAnnotation(SupportedBrokers.class);
            mBroker = createBrokerFromFlavorAndApk(supportedBrokersAnnotation);
        }
        return mBroker;
    }

    @Override
    public RuleChain getPrimaryRules() {
        return RulesHelper.getPrimaryRules(getBroker());
    }

    private ITestBroker createBrokerFromFlavorAndApk(@Nullable final SupportedBrokers supportedBrokersAnnotation) {
        // In update scenarios, the default apk installed first is the old apk (apk with older version).
        switch (BuildConfig.SELECTED_BROKER) {
            case BuildConfig.BrokerHost:
                return new BrokerHost(BrokerHost.OLD_BROKER_HOST_APK,
                        BrokerHost.BROKER_HOST_APK);
            case BuildConfig.BrokerMicrosoftAuthenticator:
                return new BrokerMicrosoftAuthenticator(BrokerMicrosoftAuthenticator.OLD_AUTHENTICATOR_APK,
                        BrokerMicrosoftAuthenticator.AUTHENTICATOR_APK);
            case BuildConfig.BrokerCompanyPortal:
                return new BrokerCompanyPortal(BrokerCompanyPortal.OLD_COMPANY_PORTAL_APK,
                        BrokerCompanyPortal.COMPANY_PORTAL_APK);
            case BuildConfig.AutoBroker: {
                if (supportedBrokersAnnotation == null) {
                    return new BrokerMicrosoftAuthenticator(BrokerMicrosoftAuthenticator.OLD_AUTHENTICATOR_APK,
                            BrokerMicrosoftAuthenticator.AUTHENTICATOR_APK);
                }
                final List<Class<? extends ITestBroker>> supportedBrokerClasses =
                        Arrays.asList(supportedBrokersAnnotation.brokers());
                if (BuildConfig.FLAVOR_main.equals("dist") && supportedBrokerClasses.contains(BrokerCompanyPortal.class)) {
                    return new BrokerCompanyPortal(BrokerCompanyPortal.OLD_COMPANY_PORTAL_APK,
                            BrokerCompanyPortal.COMPANY_PORTAL_APK);
                } else {
                    return new BrokerMicrosoftAuthenticator(BrokerMicrosoftAuthenticator.OLD_AUTHENTICATOR_APK,
                            BrokerMicrosoftAuthenticator.AUTHENTICATOR_APK);
                }
            }
            default:
                throw new UnsupportedOperationException("Unsupported broker :(");
        }
    }
}
