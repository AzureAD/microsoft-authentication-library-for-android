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
//  FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp;

import androidx.annotation.Nullable;

import com.microsoft.identity.client.msal.automationapp.BuildConfig;
import com.microsoft.identity.client.ui.automation.annotations.SupportedBrokers;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import java.util.Arrays;
import java.util.List;

public class BrokerTestHelper {
    public static ITestBroker createBrokerFromFlavor(@Nullable final SupportedBrokers supportedBrokersAnnotation) {
        switch (BuildConfig.SELECTED_BROKER) {
            case BuildConfig.BrokerHost:
                return new BrokerHost();
            case BuildConfig.BrokerMicrosoftAuthenticator:
                return new BrokerMicrosoftAuthenticator();
            case BuildConfig.BrokerCompanyPortal:
                return new BrokerCompanyPortal();

            case BuildConfig.BrokerLTW:
                return new BrokerLTW();
            case BuildConfig.AutoBroker: {
                if (supportedBrokersAnnotation == null) {
                    return new BrokerMicrosoftAuthenticator();
                }
                final List<Class<? extends ITestBroker>> supportedBrokerClasses =
                        Arrays.asList(supportedBrokersAnnotation.brokers());
                if (supportedBrokerClasses.contains(BrokerCompanyPortal.class)) {
                    return new BrokerCompanyPortal();
                } else if (supportedBrokerClasses.contains(BrokerLTW.class)) {
                    return new BrokerLTW();
                } else {
                    return new BrokerMicrosoftAuthenticator();
                }
            }
            default:
                throw new UnsupportedOperationException("Unsupported broker :(");
        }
    }
}
