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
package com.microsoft.identity.client.internal;

import com.microsoft.identity.client.AcquireTokenParameters;

/**
 * Internal class specifically for the Microsoft Intune app (COBO) to app to enable System Browser support on Broker.
 */
public class IntuneAcquireTokenParameters extends AcquireTokenParameters {

    private boolean mBrokerBrowserSupportEnabled;

    private IntuneAcquireTokenParameters(final Builder builder) {
        super(builder);
        mBrokerBrowserSupportEnabled = builder.mBrokerBrowserSupportEnabled;
    }

    public boolean isBrokerBrowserSupportEnabled() {
        return mBrokerBrowserSupportEnabled;
    }

    public static class Builder extends AcquireTokenParameters.Builder {

        private boolean mBrokerBrowserSupportEnabled;

        public IntuneAcquireTokenParameters.Builder brokerBrowserSupportEnabled(
                boolean brokerBrowserSupportEnabled) {
            mBrokerBrowserSupportEnabled = brokerBrowserSupportEnabled;
            return this;
        }

        @Override
        public IntuneAcquireTokenParameters.Builder self() {
            return this;
        }

        @Override
        public IntuneAcquireTokenParameters build() {
            return new IntuneAcquireTokenParameters(this);
        }
    }
}
